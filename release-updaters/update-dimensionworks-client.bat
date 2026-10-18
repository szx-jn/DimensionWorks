@echo off
setlocal EnableExtensions DisableDelayedExpansion
set "DW_KIND=client"
set "DW_SCRIPT_NAME=update-dimensionworks-client.bat"

if defined DW_UPDATE_WRAPPER goto payload
set "DW_SOURCE=%~f0"
set "DW_COPY=%TEMP%\DimensionWorks-%DW_KIND%-%RANDOM%-%RANDOM%.bat"
copy /y "%DW_SOURCE%" "%DW_COPY%" >nul || (
    echo [ERROR] Cannot create the temporary updater wrapper.
    exit /b 1
)
set "DW_UPDATE_WRAPPER=1"
call "%DW_COPY%" %*
set "DW_EXIT=%ERRORLEVEL%"
del /f /q "%DW_COPY%" >nul 2>&1
exit /b %DW_EXIT%

:payload
set "DW_PAYLOAD=%TEMP%\DimensionWorks-%DW_KIND%-%RANDOM%-%RANDOM%.ps1"
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -Command "$p='::PS '; $l=[IO.File]::ReadAllLines($env:DW_SOURCE,[Text.Encoding]::UTF8); $o=@($l | Where-Object { $_.StartsWith($p) } | ForEach-Object { $_.Substring($p.Length) }); [IO.File]::WriteAllLines($env:DW_PAYLOAD,$o,[Text.UTF8Encoding]::new($false))" || (
    echo [ERROR] Cannot prepare the Windows updater.
    exit /b 1
)
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%DW_PAYLOAD%" %*
set "DW_EXIT=%ERRORLEVEL%"
del /f /q "%DW_PAYLOAD%" >nul 2>&1
exit /b %DW_EXIT%

::PS $ErrorActionPreference = 'Stop'
::PS $ProgressPreference = 'Continue'
::PS [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
::PS Add-Type -AssemblyName System.IO.Compression
::PS Add-Type -AssemblyName System.IO.Compression.FileSystem
::PS
::PS $Kind = $env:DW_KIND
::PS $Repo = 'szx-jn/DimensionWorks'
::PS $ManifestName = 'dimensionworks-update-manifest.tsv'
::PS $StateName = '.dimensionworks-update/state.tsv'
::PS $Root = [IO.Path]::GetDirectoryName([IO.Path]::GetFullPath($env:DW_SOURCE).TrimEnd('\'))
::PS $UpdateRoot = Join-Path $Root '.dimensionworks-update'
::PS $Lock = Join-Path $UpdateRoot 'lock'
::PS $Work = Join-Path $UpdateRoot ('work-' + $PID + '-' + [Guid]::NewGuid().ToString('N'))
::PS $Updaters = @(
::PS     'update-dimensionworks-client.bat',
::PS     'update-dimensionworks-client.sh',
::PS     'update-dimensionworks-server.bat',
::PS     'update-dimensionworks-server.sh'
::PS )
::PS $Packages = [Collections.Generic.List[object]]::new()
::PS $Applied = [Collections.Generic.List[object]]::new()
::PS $CreatedDirs = [Collections.Generic.List[string]]::new()
::PS $TempFiles = [Collections.Generic.List[string]]::new()
::PS $VersionRecord = $null
::PS $StateRecord = $null
::PS $RollbackOk = $true
::PS $KeepWork = $false
::PS
::PS function Fail([string]$Message) { throw $Message }
::PS
::PS function Show-Help {
::PS     Write-Host ('DimensionWorks ' + $Kind + ' updater for Windows')
::PS     Write-Host 'Usage:'
::PS     Write-Host ('  ' + $env:DW_SCRIPT_NAME + ' [--release <tag>]')
::PS     Write-Host ('  ' + $env:DW_SCRIPT_NAME + ' --package <path>')
::PS     Write-Host ('  ' + $env:DW_SCRIPT_NAME + ' --url <package-url>')
::PS     Write-Host ('  ' + $env:DW_SCRIPT_NAME + ' --help')
::PS     Write-Host 'With no arguments, the latest GitHub Release is used.'
::PS }
::PS
::PS function Assert-Relative([string]$Value, [string]$Label) {
::PS     if ([string]::IsNullOrWhiteSpace($Value) -or $Value -ne $Value.Trim()) {
::PS         Fail ($Label + ' must be a trimmed relative path')
::PS     }
::PS     if ($Value.StartsWith('/') -or $Value.Contains('\') -or $Value.Contains(':')) {
::PS         Fail ($Label + ' must be a canonical relative POSIX path')
::PS     }
::PS     if ($Value.IndexOfAny([char[]](0..31 + 127)) -ge 0) {
::PS         Fail ($Label + ' contains a control character')
::PS     }
::PS     $parts = $Value.Split('/')
::PS     if (@($parts | Where-Object { $_ -eq '' -or $_ -eq '.' -or $_ -eq '..' }).Count -gt 0) {
::PS         Fail ($Label + ' contains an unsafe path segment')
::PS     }
::PS     if (($parts -join '/') -ne $Value) { Fail ($Label + ' is not canonical') }
::PS }
::PS
::PS function Assert-Http([string]$Value) {
::PS     $uri = $null
::PS     if (-not [Uri]::TryCreate($Value, [UriKind]::Absolute, [ref]$uri)) {
::PS         Fail ('invalid HTTP URL: ' + $Value)
::PS     }
::PS     if ($uri.Scheme -ne 'http' -and $uri.Scheme -ne 'https') {
::PS         Fail ('URL must use HTTP or HTTPS: ' + $Value)
::PS     }
::PS }
::PS
::PS function Test-Hash([string]$Value, [int]$Length) {
::PS     $Value.Length -eq $Length -and $Value -match '^[0-9a-f]+$'
::PS }
::PS
::PS function Parse-Manifest([byte[]]$Bytes) {
::PS     if ($Bytes.Length -ge 3 -and $Bytes[0] -eq 0xEF -and $Bytes[1] -eq 0xBB -and $Bytes[2] -eq 0xBF) {
::PS         Fail 'manifest must not contain a UTF-8 BOM'
::PS     }
::PS     $text = [Text.Encoding]::UTF8.GetString($Bytes)
::PS     if ($text.Contains("`r") -or -not $text.EndsWith("`n")) {
::PS         Fail 'manifest must use LF and end with a newline'
::PS     }
::PS     $lines = $text.Substring(0, $text.Length - 1).Split([char]"`n")
::PS     if ($lines.Count -lt 6 -or $lines[0] -ne '# DimensionWorks update manifest 1') {
::PS         Fail 'invalid manifest signature'
::PS     }
::PS     $header = @{}
::PS     for ($i = 1; $i -le 4; $i++) {
::PS         if (-not $lines[$i].StartsWith('# ')) { Fail 'invalid manifest header' }
::PS         $pair = $lines[$i].Substring(2).Split([char]' ', 2)
::PS         if ($pair.Count -ne 2 -or $pair[0] -notin @('kind','version','release','base')) {
::PS             Fail 'invalid manifest header'
::PS         }
::PS         if ($header.ContainsKey($pair[0])) { Fail 'duplicate manifest header' }
::PS         $header[$pair[0]] = $pair[1]
::PS     }
::PS     if ($header.Count -ne 4) { Fail 'manifest must have four headers' }
::PS     if ($header.kind -ne $Kind) { Fail ('expected ' + $Kind + ' manifest') }
::PS     if ($header.version -notmatch '^\d+\.\d+\.\d+$') { Fail 'invalid version' }
::PS     if ($header.release -ne ('v' + $header.version)) { Fail 'release does not match version' }
::PS     if ($header.base -ne 'none' -and $header.base -notmatch '^v\d+\.\d+\.\d+$') { Fail 'invalid base release' }
::PS     if ($lines[5] -ne "action`tpath`talgorithm`tvalue`tsource") { Fail 'invalid column header' }
::PS     $entries = [Collections.Generic.List[object]]::new()
::PS     $seen = @{}
::PS     for ($i = 6; $i -lt $lines.Count; $i++) {
::PS         $f = $lines[$i].Split([char]"`t")
::PS         if ($f.Count -ne 5) { Fail ('invalid manifest line ' + ($i + 1)) }
::PS         Assert-Relative $f[1] 'manifest path'
::PS         $key = $f[1].ToLowerInvariant()
::PS         if ($seen.ContainsKey($key)) { Fail ('duplicate path: ' + $f[1]) }
::PS         $seen[$key] = $true
::PS         if ($f[0] -eq 'remote') {
::PS             if ($f[2] -ne 'sha512' -or -not (Test-Hash $f[3] 128)) { Fail ($f[1] + ': invalid SHA-512') }
::PS             Assert-Http $f[4]
::PS         } elseif ($f[0] -eq 'archive') {
::PS             if ($f[2] -ne 'sha256' -or -not (Test-Hash $f[3] 64)) { Fail ($f[1] + ': invalid SHA-256') }
::PS             Assert-Relative $f[4] 'archive source'
::PS         } else {
::PS             Fail ($f[1] + ': unknown action')
::PS         }
::PS         $entries.Add([pscustomobject]@{
::PS             Action=$f[0]; Path=$f[1]; Algorithm=$f[2]; Digest=$f[3]; Source=$f[4]
::PS         })
::PS     }
::PS     [pscustomobject]@{
::PS         Kind=$header.kind; Version=$header.version; Release=$header.release;
::PS         Base=$header.base; Entries=$entries; Bytes=$Bytes
::PS     }
::PS }
::PS
::PS function Read-State {
::PS     $path = Join-Path $Root $StateName
::PS     if (-not [IO.File]::Exists($path)) { return $null }
::PS     if ((Get-Item -LiteralPath $path -Force).Attributes -band [IO.FileAttributes]::ReparsePoint) {
::PS         Fail 'state file must not be a symbolic link'
::PS     }
::PS     $parsed = Parse-Manifest ([IO.File]::ReadAllBytes($path))
::PS     if ($parsed.Kind -ne $Kind) { Fail ('state manifest kind mismatch: ' + $parsed.Kind) }
::PS     $parsed
::PS }
::PS
::PS function New-BootstrapState {
::PS     $lines = [Collections.Generic.List[string]]::new()
::PS     $lines.Add('# DimensionWorks update manifest 1')
::PS     $lines.Add('# kind ' + $Kind)
::PS     $lines.Add('# version 0.0.0')
::PS     $lines.Add('# release v0.0.0')
::PS     $lines.Add('# base none')
::PS     $lines.Add("action`tpath`talgorithm`tvalue`tsource")
::PS     $roots = if ($Kind -eq 'client') {
::PS         @('mods','shaderpacks','config','defaultconfigs','kubejs','resourcepacks','local')
::PS     } else {
::PS         @('mods','config','defaultconfigs','kubejs','libraries')
::PS     }
::PS     foreach ($rootName in $roots) {
::PS         $rootPath = Join-Path $Root $rootName
::PS         if (-not [IO.Directory]::Exists($rootPath)) { continue }
::PS         foreach ($file in Get-ChildItem -LiteralPath $rootPath -File -Recurse -Force -ErrorAction SilentlyContinue) {
::PS             $relative = $file.FullName.Substring($Root.Length).TrimStart([char]'\', [char]'/') -replace '\\','/'
::PS             $first = $relative.Split('/')[0]
::PS             if ($first -in @('saves','logs','crash-reports','screenshots','world','world_nether','world_the_end','.dimensionworks-update')) { continue }
::PS             if ($relative -in $Updaters -or $relative -eq 'VERSION' -or $relative -eq $StateName) { continue }
::PS             $digest = (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
::PS             $lines.Add("archive`t$relative`tsha256`t$digest`t$relative")
::PS         }
::PS     }
::PS     $rootFiles = if ($Kind -eq 'client') {
::PS         @('modrinth.index.json','options.txt','log4j2.xml','rhino.local.properties',
::PS             'CustomSkinLoader/CustomSkinLoader.json','minemenu/menu.json')
::PS     } else {
::PS         @('README.md','start.sh','start.bat','run.sh','run.bat')
::PS     }
::PS     foreach ($relative in $rootFiles) {
::PS         $path = Join-Path $Root ($relative -replace '/', [IO.Path]::DirectorySeparatorChar)
::PS         if (-not [IO.File]::Exists($path) -or (Get-Item -LiteralPath $path -Force).Attributes -band [IO.FileAttributes]::ReparsePoint) { continue }
::PS         $digest = (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant()
::PS         $lines.Add("archive`t$relative`tsha256`t$digest`t$relative")
::PS     }
::PS     $bytes = [Text.Encoding]::UTF8.GetBytes(($lines -join "`n") + "`n")
::PS     Parse-Manifest $bytes
::PS }
::PS
::PS function Get-Digest([string]$Path, [string]$Algorithm) {
::PS     if (-not [IO.File]::Exists($Path)) { return $null }
::PS     (Get-FileHash -LiteralPath $Path -Algorithm $Algorithm).Hash.ToLowerInvariant()
::PS }
::PS
::PS function Get-Target([string]$Relative) {
::PS     Assert-Relative $Relative 'target path'
::PS     $first = $Relative.Split('/')[0]
::PS     if ($Kind -eq 'client') {
::PS         if ($first -in @('saves','logs','crash-reports','screenshots','.dimensionworks-update')) {
::PS             Fail ('preserved client path: ' + $Relative)
::PS         }
::PS         if ($Relative -in @('DimensionWorks.jar','DimensionWorks.json') -or $Relative -in $Updaters) {
::PS             Fail ('preserved launcher/updater path: ' + $Relative)
::PS         }
::PS     } else {
::PS         $serverRuntimeNames = @('eula.txt','server.properties','user_jvm_args.txt','usercache.json',
::PS             'ops.json','whitelist.json','banned-ips.json','banned-players.json')
::PS         if ($Relative -in $serverRuntimeNames -and
::PS             [IO.File]::Exists((Join-Path $Root ($Relative -replace '/', [IO.Path]::DirectorySeparatorChar)))) {
::PS             return [IO.Path]::GetFullPath((Join-Path $Root ($Relative -replace '/', [IO.Path]::DirectorySeparatorChar)))
::PS         }
::PS         if ($first -in @('logs','crash-reports','debug','screenshots','world','world_nether','world_the_end','.dimensionworks-update')) {
::PS             Fail ('preserved server path: ' + $Relative)
::PS         }
::PS         if ($Relative -in $Updaters) { Fail ('updater path cannot be replaced: ' + $Relative) }
::PS     }
::PS     $path = [IO.Path]::GetFullPath((Join-Path $Root ($Relative -replace '/', [IO.Path]::DirectorySeparatorChar)))
::PS     $prefix = $Root.TrimEnd([IO.Path]::DirectorySeparatorChar) + [IO.Path]::DirectorySeparatorChar
::PS     if (-not $path.StartsWith($prefix, [StringComparison]::OrdinalIgnoreCase)) {
::PS         Fail ('path escapes instance root: ' + $Relative)
::PS     }
::PS     $path
::PS }
::PS
::PS function Test-PreserveExisting([string]$Relative) {
::PS     if ($Kind -ne 'server') { return $false }
::PS     $name = $Relative.ToLowerInvariant()
::PS     $names = @('eula.txt','server.properties','user_jvm_args.txt','usercache.json',
::PS         'ops.json','whitelist.json','banned-ips.json','banned-players.json')
::PS     if ($name -notin $names) { return $false }
::PS     [IO.File]::Exists((Join-Path $Root ($Relative -replace '/', [IO.Path]::DirectorySeparatorChar)))
::PS }
::PS
::PS function Test-SafeExisting([string]$Path) {
::PS     $current = $Path
::PS     while ($current.Length -gt $Root.Length) {
::PS         if (-not (Test-Path -LiteralPath $current)) {
::PS             $current = [IO.Path]::GetDirectoryName($current)
::PS             continue
::PS         }
::PS         if ((Get-Item -LiteralPath $current -Force).Attributes -band [IO.FileAttributes]::ReparsePoint) {
::PS             Fail ('symbolic link is not allowed: ' + $current)
::PS         }
::PS         if ($current -eq $Path) { break }
::PS         $current = [IO.Path]::GetDirectoryName($current)
::PS     }
::PS     [IO.File]::Exists($Path) -or [IO.Directory]::Exists($Path)
::PS }
::PS
::PS function Invoke-Download([string]$Uri, [string]$Destination) {
::PS     Assert-Http $Uri
::PS     [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($Destination)) | Out-Null
::PS     for ($attempt = 1; $attempt -le 3; $attempt++) {
::PS         try {
::PS             Invoke-WebRequest -UseBasicParsing -UserAgent 'DimensionWorks-Windows-Updater/1.0' -Uri $Uri -OutFile $Destination
::PS             return
::PS         } catch {
::PS             if ($attempt -eq 3) { Fail ('download failed: ' + $Uri + ': ' + $_.Exception.Message) }
::PS             Start-Sleep -Seconds $attempt
::PS         }
::PS     }
::PS }
::PS
::PS function Open-Zip([string]$Path) {
::PS     if (-not [IO.File]::Exists($Path)) { Fail ('package does not exist: ' + $Path) }
::PS     if ((Get-Item -LiteralPath $Path -Force).Attributes -band [IO.FileAttributes]::ReparsePoint) {
::PS         Fail 'package must not be a symbolic link'
::PS     }
::PS     try {
::PS         $archive = [IO.Compression.ZipFile]::OpenRead($Path)
::PS         $map = @{}
::PS         foreach ($entry in $archive.Entries) {
::PS             Assert-Relative $entry.FullName.TrimEnd('/') 'archive member'
::PS             if ($entry.FullName.EndsWith('/')) { continue }
::PS             if ((($entry.ExternalAttributes -shr 12) -band 0xA000) -eq 0xA000) {
::PS                 Fail ('archive contains a symbolic link: ' + $entry.FullName)
::PS             }
::PS             $name = [IO.Path]::GetFileName($entry.FullName)
::PS             if ($name.ToLowerInvariant() -in @($Updaters | ForEach-Object { $_.ToLowerInvariant() })) {
::PS                 Fail ('package contains an updater script: ' + $entry.FullName)
::PS             }
::PS             $key = $entry.FullName.ToLowerInvariant()
::PS             if ($map.ContainsKey($key)) { Fail ('case-colliding archive path: ' + $entry.FullName) }
::PS             $map[$key] = $entry
::PS         }
::PS         $package = [pscustomobject]@{Archive=$archive; Entries=$map; Path=$Path}
::PS         $Packages.Add($package)
::PS         $package
::PS     } catch {
::PS         if ($archive) { $archive.Dispose() }
::PS         Fail ('cannot open package: ' + $_.Exception.Message)
::PS     }
::PS }
::PS
::PS function Read-Embedded($Package) {
::PS     $key = $ManifestName.ToLowerInvariant()
::PS     if (-not $Package.Entries.ContainsKey($key)) { Fail 'package has no update manifest' }
::PS     $stream = $Package.Entries[$key].Open()
::PS     $memory = [IO.MemoryStream]::new()
::PS     try {
::PS         $stream.CopyTo($memory)
::PS         $parsed = Parse-Manifest $memory.ToArray()
::PS         if ($parsed.Kind -ne $Kind) { Fail ('manifest kind mismatch: ' + $parsed.Kind) }
::PS         $parsed
::PS     }
::PS     finally { $stream.Dispose(); $memory.Dispose() }
::PS }
::PS
::PS function Copy-Entry($Entry, [string]$Destination) {
::PS     [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($Destination)) | Out-Null
::PS     $input = $Entry.Open()
::PS     $output = [IO.File]::Create($Destination)
::PS     try { $input.CopyTo($output) } finally { $input.Dispose(); $output.Dispose() }
::PS }
::PS
::PS function Get-Latest([string]$Pattern) {
::PS     $api = 'https://api.github.com/repos/' + $Repo + '/releases/latest'
::PS     $release = Invoke-RestMethod -UserAgent 'DimensionWorks-Windows-Updater/1.0' -Uri $api
::PS     if ($release.tag_name -notmatch '^v\d+\.\d+\.\d+$') { Fail 'latest release has an invalid tag' }
::PS     $asset = @($release.assets) | Where-Object { $_.name -match $Pattern } | Select-Object -First 1
::PS     if (-not $asset) { Fail ('latest release lacks a required asset: ' + $Pattern) }
::PS     [pscustomobject]@{
::PS         Release=$release.tag_name; Version=$release.tag_name.Substring(1);
::PS         Url=$asset.browser_download_url; Name=$asset.name
::PS     }
::PS }
::PS
::PS function AssetUrl([string]$Release, [string]$Name) {
::PS     'https://github.com/' + $Repo + '/releases/download/' + $Release + '/' + $Name
::PS }
::PS
::PS function Select-Changed($Manifest) {
::PS     $result = [Collections.Generic.List[object]]::new()
::PS     $index = 0
::PS     Write-Host ('[进度] 检查 ' + $Manifest.Entries.Count + ' 个受管文件')
::PS     foreach ($entry in $Manifest.Entries) {
::PS         $index++
::PS         Write-Progress -Activity '检查受管文件' -Status $entry.Path -PercentComplete ([int](100 * $index / $Manifest.Entries.Count))
::PS         $target = Get-Target $entry.Path
::PS         if (Test-PreserveExisting $entry.Path) { continue }
::PS         $exists = Test-SafeExisting $target
::PS         if ($exists -and [IO.Directory]::Exists($target)) { Fail ('target is a directory: ' + $entry.Path) }
::PS         if ($exists -and (Get-Digest $target $entry.Algorithm) -eq $entry.Digest) { continue }
::PS         $result.Add($entry)
::PS     }
::PS     Write-Progress -Activity '检查受管文件' -Completed
::PS     $result
::PS }
::PS
::PS function Stage-Entries($Manifest, $Selected, $Package) {
::PS     $index = 0
::PS     foreach ($entry in $Selected) {
::PS         $index++
::PS         Write-Host ('[进度] 暂存 ' + $index + '/' + $Selected.Count + ' ' + $entry.Path)
::PS         Write-Progress -Activity '暂存更新文件' -Status $entry.Path -PercentComplete ([int](100 * $index / $Selected.Count))
::PS         $destination = Join-Path $Work ('stage/' + $entry.Path)
::PS         if ($entry.Action -eq 'remote') {
::PS             Invoke-Download $entry.Source $destination
::PS         } else {
::PS             $key = $entry.Source.ToLowerInvariant()
::PS             if (-not $Package -or -not $Package.Entries.ContainsKey($key)) {
::PS                  Fail ('package lacks ' + $entry.Source + ' for ' + $entry.Path)
::PS              }
::PS              Copy-Entry $Package.Entries[$key] $destination
::PS          }
::PS          if ((Get-Digest $destination $entry.Algorithm) -ne $entry.Digest) {
::PS              Fail ('staged file failed verification: ' + $entry.Path)
::PS          }
::PS      }
::PS      Write-Progress -Activity '暂存更新文件' -Completed
::PS  }
::PS
::PS function Test-Staged($Manifest, $Selected) {
::PS      $wanted = @{}
::PS      foreach ($entry in $Selected) { $wanted[$entry.Path.ToLowerInvariant()] = $true }
::PS      foreach ($entry in $Manifest.Entries) {
::PS          if (Test-PreserveExisting $entry.Path) { continue }
::PS          if ($wanted.ContainsKey($entry.Path.ToLowerInvariant())) { continue }
::PS          $target = Get-Target $entry.Path
::PS          if ((Get-Digest $target $entry.Algorithm) -ne $entry.Digest) {
::PS              Fail ('package lacks required unchanged file: ' + $entry.Path)
::PS          }
::PS      }
::PS  }
::PS
::PS function Clear-Stage {
::PS      $stage = Join-Path $Work 'stage'
::PS      if ([IO.Directory]::Exists($stage)) { [IO.Directory]::Delete($stage, $true) }
::PS  }
::PS
::PS function Make-Operations($Manifest, $Selected) {
::PS      $result = [Collections.Generic.List[object]]::new()
::PS      $number = 0
::PS      foreach ($entry in $Selected) {
::PS          $number++
::PS          $target = Get-Target $entry.Path
::PS          $backup = $null
::PS          if ([IO.File]::Exists($target)) {
::PS              $backup = Join-Path $Work ('backup/' + $number.ToString('D6') + '.bak')
::PS              [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($backup)) | Out-Null
::PS              [IO.File]::Copy($target, $backup, $true)
::PS          }
::PS          $result.Add([pscustomobject]@{
::PS              Entry=$entry; Target=$target; Backup=$backup; Delete=$false
::PS          })
::PS      }
::PS      $state = $Script:CurrentState
::PS      if ($state) {
::PS          $targets = @{}
::PS          foreach ($entry in $Manifest.Entries) { $targets[$entry.Path.ToLowerInvariant()] = $true }
::PS          $number = 0
::PS          foreach ($old in $state.Entries) {
::PS              $number++
::PS              if ($targets.ContainsKey($old.Path.ToLowerInvariant())) { continue }
::PS              if (Test-PreserveExisting $old.Path) { continue }
::PS              $target = Get-Target $old.Path
::PS              if (-not (Test-SafeExisting $target)) { continue }
::PS              if ([IO.Directory]::Exists($target)) { Fail ('cannot remove directory from old state: ' + $old.Path) }
::PS              $backup = Join-Path $Work ('backup/delete-' + $number.ToString('D6') + '.bak')
::PS              [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($backup)) | Out-Null
::PS              $result.Add([pscustomobject]@{
::PS                  Entry=$old; Target=$target; Backup=$backup; Delete=$true
::PS              })
::PS          }
::PS      }
::PS      $result
::PS  }
::PS
::PS function Ensure-Parent([string]$Path) {
::PS      $parent = [IO.Path]::GetDirectoryName($Path)
::PS      if ([IO.Directory]::Exists($parent)) { return }
::PS      $missing = [Collections.Generic.List[string]]::new()
::PS      $current = $parent
::PS      while (-not [IO.Directory]::Exists($current)) {
::PS          $missing.Add($current)
::PS          $current = [IO.Path]::GetDirectoryName($current)
::PS      }
::PS      for ($i = $missing.Count - 1; $i -ge 0; $i--) {
::PS          [IO.Directory]::CreateDirectory($missing[$i]) | Out-Null
::PS          $CreatedDirs.Add($missing[$i])
::PS      }
::PS  }
::PS
::PS function Apply-Operations($Operations) {
::PS      $index = 0
::PS      foreach ($op in $Operations) {
::PS          $index++
::PS          Write-Host ('[进度] 应用 ' + $index + '/' + $Operations.Count + ' ' + $op.Entry.Path)
::PS          Write-Progress -Activity '应用更新文件' -Status $op.Entry.Path -PercentComplete ([int](100 * $index / $Operations.Count))
::PS          Ensure-Parent $op.Target
::PS          if ($op.Delete) {
::PS              [IO.File]::Move($op.Target, $op.Backup)
::PS              $Applied.Add($op)
::PS              continue
::PS          }
::PS          $temporary = $op.Target + '.dw-new-' + $PID
::PS          $TempFiles.Add($temporary)
::PS          Copy-Item -LiteralPath (Join-Path $Work ('stage/' + $op.Entry.Path)) -Destination $temporary
::PS          if ((Get-Digest $temporary $op.Entry.Algorithm) -ne $op.Entry.Digest) {
::PS              Fail ('temporary file failed verification: ' + $op.Entry.Path)
::PS          }
::PS          if ([IO.File]::Exists($op.Target)) {
::PS              [IO.File]::Replace($temporary, $op.Target, $null, $true)
::PS          } else {
::PS              [IO.File]::Move($temporary, $op.Target)
::PS          }
::PS          $Applied.Add($op)
::PS      }
::PS      Write-Progress -Activity '应用更新文件' -Completed
::PS  }
::PS
::PS function Write-Metadata([string]$Path, [byte[]]$Bytes, [string]$BackupName) {
::PS      $ancestor = $Path
::PS      while ($ancestor.Length -gt $Root.Length) {
::PS          if ((Test-Path -LiteralPath $ancestor) -and
::PS              ((Get-Item -LiteralPath $ancestor -Force).Attributes -band [IO.FileAttributes]::ReparsePoint)) {
::PS              Fail ('symbolic link is not allowed: ' + $ancestor)
::PS          }
::PS          $ancestor = [IO.Path]::GetDirectoryName($ancestor)
::PS      }
::PS      Ensure-Parent $Path
::PS      $existed = [IO.File]::Exists($Path)
::PS      $backup = $null
::PS      if ($existed) {
::PS          $backup = Join-Path $Work ('backup/' + $BackupName)
::PS          [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($backup)) | Out-Null
::PS          [IO.File]::Copy($Path, $backup, $true)
::PS      }
::PS      $temporary = $Path + '.dw-new-' + $PID
::PS      $TempFiles.Add($temporary)
::PS      [IO.File]::WriteAllBytes($temporary, $Bytes)
::PS      if ($existed) { [IO.File]::Replace($temporary, $Path, $null, $true) }
::PS      else { [IO.File]::Move($temporary, $Path) }
::PS      [pscustomobject]@{Path=$Path; Backup=$backup; Existed=$existed}
::PS  }
::PS
::PS function Restore-Metadata($Record) {
::PS      if (-not $Record) { return }
::PS      if ([IO.File]::Exists($Record.Path)) { [IO.File]::Delete($Record.Path) }
::PS      if ($Record.Existed -and [IO.File]::Exists($Record.Backup)) {
::PS          [IO.File]::Move($Record.Backup, $Record.Path)
::PS      }
::PS  }
::PS
::PS function Rollback {
::PS      $RollbackOk = $true
::PS      try {
::PS          for ($i = $Applied.Count - 1; $i -ge 0; $i--) {
::PS              $op = $Applied[$i]
::PS              if ([IO.File]::Exists($op.Target)) { [IO.File]::Delete($op.Target) }
::PS              if ($op.Backup -and [IO.File]::Exists($op.Backup)) {
::PS                  [IO.File]::Move($op.Backup, $op.Target)
::PS              }
::PS          }
::PS          Restore-Metadata $StateRecord
::PS          Restore-Metadata $VersionRecord
::PS          foreach ($path in $TempFiles) {
::PS              if ([IO.File]::Exists($path)) { [IO.File]::Delete($path) }
::PS          }
::PS          for ($i = $CreatedDirs.Count - 1; $i -ge 0; $i--) {
::PS              $dir = $CreatedDirs[$i]
::PS              if ([IO.Directory]::Exists($dir) -and
::PS                  -not [IO.Directory]::EnumerateFileSystemEntries($dir).GetEnumerator().MoveNext()) {
::PS                  [IO.Directory]::Delete($dir)
::PS              }
::PS          }
::PS      } catch {
::PS          $RollbackOk = $false
::PS          $KeepWork = $true
::PS          Write-Warning ('rollback incomplete: ' + $_.Exception.Message)
::PS      }
::PS  }
::PS
::PS function Verify-Manifest($Manifest) {
::PS      foreach ($entry in $Manifest.Entries) {
::PS          if (Test-PreserveExisting $entry.Path) { continue }
::PS          $target = Get-Target $entry.Path
::PS          if ((Get-Digest $target $entry.Algorithm) -ne $entry.Digest) {
::PS              Fail ('final verification failed: ' + $entry.Path)
::PS          }
::PS      }
::PS  }
::PS
::PS function Stop-RelatedProcesses {
::PS     Write-Host '[第零步] 检查并关闭当前整合包相关进程...'
::PS     $all = @(Get-CimInstance Win32_Process -ErrorAction Stop)
::PS     $protected = [Collections.Generic.HashSet[int]]::new()
::PS     $cursor = $PID
::PS     while ($cursor -gt 0) {
::PS         $parent = $all | Where-Object { $_.ProcessId -eq $cursor } | Select-Object -First 1
::PS         if (-not $parent) { break }
::PS         if ($parent.Name -in @('java.exe','javaw.exe')) { break }
::PS         if (-not $protected.Add([int]$cursor)) { break }
::PS         $cursor = [int]$parent.ParentProcessId
::PS     }
::PS     $root = [IO.Path]::GetFullPath($Root).TrimEnd('\')
::PS     $targets = [Collections.Generic.HashSet[int]]::new()
::PS     foreach ($p in $all) {
::PS         if ($protected.Contains([int]$p.ProcessId)) { continue }
::PS         if ($p.Name -notin @('java.exe','javaw.exe')) { continue }
::PS         $cmd = [string]$p.CommandLine
::PS         if ($cmd -and $cmd.IndexOf($root, [StringComparison]::OrdinalIgnoreCase) -ge 0) { [void]$targets.Add([int]$p.ProcessId) }
::PS     }
::PS     $changed = $true
::PS     while ($changed) {
::PS         $changed = $false
::PS         foreach ($p in $all) {
::PS             if ($protected.Contains([int]$p.ProcessId) -or $targets.Contains([int]$p.ProcessId)) { continue }
::PS             if ($targets.Contains([int]$p.ParentProcessId)) { [void]$targets.Add([int]$p.ProcessId); $changed = $true }
::PS         }
::PS     }
::PS     if ($targets.Count -eq 0) { Write-Host '[第零步] 未发现可确认属于当前目录的 Java 游戏进程。'; return }
::PS     foreach ($id in $targets) { try { Stop-Process -Id $id -ErrorAction SilentlyContinue } catch {} }
::PS     Start-Sleep -Seconds 2
::PS     foreach ($id in @($targets)) { if (Get-Process -Id $id -ErrorAction SilentlyContinue) { Stop-Process -Id $id -Force -ErrorAction SilentlyContinue } }
::PS     Start-Sleep -Milliseconds 500
::PS     $remaining = @($targets | Where-Object { Get-Process -Id $_ -ErrorAction SilentlyContinue })
::PS     if ($remaining.Count -gt 0) { Fail ('仍有相关进程无法关闭，PID: ' + ($remaining -join ', ')) }
::PS     Write-Host '[第零步] 相关进程已关闭。'
::PS }
::PS
::PS function Invoke-Update {
::PS      $release = $null
::PS      $packageOption = $null
::PS      $urlOption = $null
::PS      for ($i = 0; $i -lt $args.Count; $i++) {
::PS          switch ($args[$i]) {
::PS              '--help' { Show-Help; return }
::PS              '-h' { Show-Help; return }
::PS              '/?' { Show-Help; return }
::PS              '--release' {
::PS                  if (++$i -ge $args.Count) { Fail '--release requires a tag' }
::PS                  $release = $args[$i]
::PS              }
::PS              '--package' {
::PS                  if (++$i -ge $args.Count) { Fail '--package requires a path' }
::PS                  $packageOption = $args[$i]
::PS              }
::PS              '--url' {
::PS                  if (++$i -ge $args.Count) { Fail '--url requires a URL' }
::PS                  $urlOption = $args[$i]
::PS              }
::PS              default { Fail ('unknown option: ' + $args[$i]) }
::PS          }
::PS      }
::PS      if ((@($release,$packageOption,$urlOption) | Where-Object { $_ }).Count -gt 1) {
::PS          Fail '--release, --package and --url are mutually exclusive'
::PS      }
::PS      if ($release -and $release -notmatch '^v\d+\.\d+\.\d+$') {
::PS          Fail '--release must be a v-prefixed x.x.x tag'
::PS      }
::PS 
::PS      [IO.Directory]::CreateDirectory($UpdateRoot) | Out-Null
::PS      if ((Get-Item -LiteralPath $UpdateRoot -Force).Attributes -band [IO.FileAttributes]::ReparsePoint) {
::PS          Fail '.dimensionworks-update must not be a symbolic link'
::PS      }
::PS      if ([IO.Directory]::Exists($Lock)) { Fail 'another update is already running' }
::PS      try { New-Item -ItemType Directory -Path $Lock -ErrorAction Stop | Out-Null }
::PS      catch { Fail 'could not acquire the update lock' }
::PS      [IO.Directory]::CreateDirectory($Work) | Out-Null
::PS 
::PS      try {
::PS          Stop-RelatedProcesses
::PS          $Script:CurrentState = Read-State
::PS          if (-not $Script:CurrentState) { $Script:CurrentState = New-BootstrapState }
::PS          $oldVersion = if ($Script:CurrentState) { $Script:CurrentState.Version } else { 'first installation' }
::PS          $manifest = $null
::PS          $manifestUrl = $null
::PS          $package = $null
::PS          $releaseTag = $null
::PS 
::PS          if ($packageOption) {
::PS              $packagePath = [IO.Path]::GetFullPath($packageOption)
::PS              $package = Open-Zip $packagePath
::PS              $manifest = Read-Embedded $package
::PS              $releaseTag = $manifest.Release
::PS          } elseif ($urlOption) {
::PS              $manifestUrl = $urlOption -replace '\.mrpack$', '-update-manifest.tsv'
::PS              $manifestPath = Join-Path $Work 'manifest.tsv'
::PS              try {
::PS                  Invoke-Download $manifestUrl $manifestPath
::PS                  $manifest = Parse-Manifest ([IO.File]::ReadAllBytes($manifestPath))
::PS                  $releaseTag = $manifest.Release
::PS              } catch {
::PS                  $download = Join-Path $Work 'package.download'
::PS                  Invoke-Download $urlOption $download
::PS                  $package = Open-Zip $download
::PS                  $manifest = Read-Embedded $package
::PS                  $releaseTag = $manifest.Release
::PS              }
::PS          } else {
::PS              if ($release) {
::PS                  $releaseTag = $release
::PS                  $version = $release.Substring(1)
::PS                  if ($Kind -eq 'client') {
::PS                      $manifestName = 'DimensionWorks-' + $version + '-update-manifest.tsv'
::PS                  } else {
::PS                      $manifestName = 'DimensionWorks-Server-' + $version + '-update-manifest.tsv'
::PS                  }
::PS                  $manifestUrl = AssetUrl $releaseTag $manifestName
::PS              } else {
::PS                  if ($Kind -eq 'client') {
::PS                      $pattern = '^DimensionWorks-\d+\.\d+\.\d+-update-manifest\.tsv$'
::PS                  } else {
::PS                      $pattern = '^DimensionWorks-Server-\d+\.\d+\.\d+-update-manifest\.tsv$'
::PS                  }
::PS                  $latest = Get-Latest $pattern
::PS                  $releaseTag = $latest.Release
::PS                  $manifestUrl = $latest.Url
::PS              }
::PS              $manifestPath = Join-Path $Work 'manifest.tsv'
::PS              Invoke-Download $manifestUrl $manifestPath
::PS              $manifest = Parse-Manifest ([IO.File]::ReadAllBytes($manifestPath))
::PS              if ($manifest.Kind -ne $Kind) { Fail ('manifest kind mismatch: ' + $manifest.Kind) }
::PS          }
::PS 
::PS          Write-Host ('DimensionWorks update: ' + $oldVersion + ' -> ' + $manifest.Version)
::PS          $selected = @(Select-Changed $manifest)
::PS          $deltaUsed = $false
::PS 
::PS          if ($Kind -eq 'client') {
::PS              $needsPackage = @($selected | Where-Object { $_.Action -eq 'archive' }).Count -gt 0
::PS              $compatible = $Script:CurrentState -and
::PS                  $Script:CurrentState.Release -eq $manifest.Base
::PS              if ($needsPackage -and $compatible -and $releaseTag -and (-not $package)) {
::PS                  try {
::PS                      $deltaPath = Join-Path $Work 'client-delta.zip'
::PS                      Invoke-Download (AssetUrl $releaseTag ('DimensionWorks-' + $manifest.Version + '-delta.zip')) $deltaPath
::PS                      $delta = Open-Zip $deltaPath
::PS                      Stage-Entries $manifest $selected $delta
::PS                      Test-Staged $manifest $selected
::PS                      $deltaUsed = $true
::PS                  } catch {
::PS                      Write-Warning ('delta unavailable, using full package: ' + $_.Exception.Message)
::PS                      Clear-Stage
::PS                  }
::PS              }
::PS              if (-not $deltaUsed -and $needsPackage) {
::PS                  if (-not $package) {
::PS                      if ($releaseTag) {
::PS                          $path = Join-Path $Work 'package.mrpack'
::PS                          Invoke-Download (AssetUrl $releaseTag ('DimensionWorks-' + $manifest.Version + '.mrpack')) $path
::PS                          $package = Open-Zip $path
::PS                      } else {
::PS                          Fail 'cannot resolve a client package'
::PS                      }
::PS                  }
::PS              }
::PS              if (-not $deltaUsed -or -not $needsPackage) {
::PS                  Stage-Entries $manifest $selected $package
::PS                  Test-Staged $manifest $selected
::PS              }
::PS          } else {
::PS              $compatible = $Script:CurrentState -and
::PS                  $Script:CurrentState.Release -eq $manifest.Base -and
::PS                  $Script:CurrentState.Base -eq $manifest.Base
::PS              if ($compatible -and $selected.Count -gt 0 -and $releaseTag) {
::PS                  try {
::PS                      $deltaPath = Join-Path $Work 'server-delta.zip'
::PS                      Invoke-Download (AssetUrl $releaseTag ('DimensionWorks-Server-' + $manifest.Version + '-delta.zip')) $deltaPath
::PS                      $delta = Open-Zip $deltaPath
::PS                      Stage-Entries $manifest $selected $delta
::PS                      Test-Staged $manifest $selected
::PS                      $deltaUsed = $true
::PS                  } catch {
::PS                      Write-Warning ('delta unavailable, using full package: ' + $_.Exception.Message)
::PS                      Clear-Stage
::PS                  }
::PS              }
::PS              if (-not $deltaUsed -and $selected.Count -gt 0) {
::PS                  if (-not $package) {
::PS                      if ($releaseTag) {
::PS                          $path = Join-Path $Work 'server-full.zip'
::PS                          Invoke-Download (AssetUrl $releaseTag ('DimensionWorks-Server-' + $manifest.Version + '.zip')) $path
::PS                          $package = Open-Zip $path
::PS                      } else {
::PS                          Fail 'cannot resolve a server package'
::PS                      }
::PS                  }
::PS                  Stage-Entries $manifest $selected $package
::PS                  Test-Staged $manifest $selected
::PS             }
::PS         }
::PS 
::PS         $operations = @(Make-Operations $manifest $selected)
::PS         try {
::PS             Apply-Operations $operations
::PS             Verify-Manifest $manifest
::PS             $versionBytes = [Text.Encoding]::UTF8.GetBytes($manifest.Version + "`n")
::PS             $Script:VersionRecord = Write-Metadata (Join-Path $Root 'VERSION') $versionBytes 'version.bak'
::PS             $Script:StateRecord = Write-Metadata (Join-Path $Root $StateName) $manifest.Bytes 'state.bak'
::PS         } catch {
::PS             Rollback
::PS             throw
::PS         }
::PS 
::PS         Write-Host ('DimensionWorks update complete: ' + $oldVersion + ' -> ' + $manifest.Version)
::PS         if ($deltaUsed) { Write-Host 'Update source: verified client delta package' }
::PS     } catch {
::PS         $message = 'DimensionWorks update failed: ' + $_.Exception.Message
::PS         Write-Host $message
::PS         $status = if ($RollbackOk) { 'rollback-complete' } else { 'rollback-incomplete' }
::PS         $log = Join-Path $UpdateRoot ('failed-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.log')
::PS         [IO.File]::WriteAllText($log, $status + "`n" + $message + "`n", [Text.UTF8Encoding]::new($false))
::PS         if ($KeepWork) { Write-Host ('Recovery files: ' + $Work) }
::PS         exit 1
::PS     } finally {
::PS         foreach ($package in $Packages) { if ($package.Archive) { $package.Archive.Dispose() } }
::PS         if (-not $KeepWork -and [IO.Directory]::Exists($Work)) {
::PS             try { [IO.Directory]::Delete($Work, $true) }
::PS             catch { Write-Warning ('temporary cleanup failed: ' + $_.Exception.Message) }
::PS         }
::PS         if ([IO.Directory]::Exists($Lock)) { [IO.Directory]::Delete($Lock, $false) }
::PS     }
::PS }
::PS 
::PS Invoke-Update @args
