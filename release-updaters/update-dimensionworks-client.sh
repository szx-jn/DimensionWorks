#!/usr/bin/env bash
# DimensionWorks standalone client updater.
# This file is a release asset. It is intentionally never embedded in a package.

set -euo pipefail

KIND="client"
REPO="szx-jn/DimensionWorks"
SCRIPT_NAME="update-dimensionworks-client.sh"
ASSET_PREFIX="DimensionWorks"
UPDATE_DIR=".dimensionworks-update"
STATE_REL="$UPDATE_DIR/state.tsv"
MANIFEST_NAME="dimensionworks-update-manifest.tsv"
LOCK_DIR="$UPDATE_DIR/lock"
TRANSACTION_LOG="transaction.tsv"
BACKUP_DIR="$UPDATE_DIR/backups"
RELEASE_ARG=""
PACKAGE_ARG=""
URL_ARG=""
TARGET_VERSION=""
TARGET_RELEASE=""
MANIFEST_URL=""
PACKAGE_URL=""
WORK_DIR=""
PACKAGE_ROOT=""
TARGET_MANIFEST=""
PACKAGE_LOCAL=""
OLD_VERSION=""
LOCK_HELD=0
    TX_ACTIVE=0
DELTA_USED=0
ROOT_DIR=""
PROGRESS_TOTAL=0
PROGRESS_DONE=0

usage() {
    printf '%s\n' \
        "DimensionWorks 客户端增量更新器" \
        "" \
        "用法:" \
        "  $SCRIPT_NAME [--release <vX.Y.Z> | --package <本地 .mrpack> | --url <完整包 URL>]" \
        "  $SCRIPT_NAME --help" \
        "" \
        "不带参数时从 GitHub Latest Release 更新。更新前请退出 Minecraft 和启动器。"
}

die() {
    printf '错误：%s\n' "$*" >&2
    exit 1
}

sha_file() {
    local algorithm="$1" path="$2"
    if command -v shasum >/dev/null 2>&1; then
        shasum -a "$algorithm" "$path" | awk '{print tolower($1)}'
    elif command -v sha256sum >/dev/null 2>&1; then
        case "$algorithm" in
            256) sha256sum "$path" | awk '{print tolower($1)}' ;;
            512) sha512sum "$path" | awk '{print tolower($1)}' ;;
            *) die "不支持的摘要算法：$algorithm" ;;
        esac
    else
        die "系统缺少 shasum 或 sha256sum"
    fi
}

download_once() {
    local url="$1" destination="$2" timeout_seconds="$3"
    rm -f -- "$destination"
    printf '[下载] 本源限时 %s 秒：%s\n' "$timeout_seconds" "$url" >&2
    # 优先使用 aria2（Motrix 同系下载引擎），以分段并发方式下载单个大文件。
    if command -v aria2c >/dev/null 2>&1; then
        aria2c --no-conf --allow-overwrite=true --auto-file-renaming=false \\
            --max-connection-per-server=8 --split=8 --min-split-size=1M \\
            --file-allocation=none --connect-timeout=10 --timeout="$timeout_seconds" \\
            --max-tries=1 --summary-interval=1 --console-log-level=notice \\
            --dir="$(dirname "$destination")" --out="$(basename "$destination")" "$url"
    elif command -v curl >/dev/null 2>&1; then
        curl --fail --location --progress-bar --show-error \
            --connect-timeout 10 --max-time "$timeout_seconds" \
            --output "$destination" "$url"
    elif command -v wget >/dev/null 2>&1; then
        wget --timeout="$timeout_seconds" --tries=1 --progress=bar:force \
            --output-document="$destination" "$url"
    else
        return 127
    fi
}

download_forever() {
    local url="$1" destination="$2"
    printf '[下载] 人工续传，不限制总时长：%s\n' "$url" >&2
    if command -v curl >/dev/null 2>&1; then
        curl --fail --location --progress-bar --show-error --continue-at - --output "$destination" "$url"
    elif command -v wget >/dev/null 2>&1; then
        wget --tries=1 --continue --output-document="$destination" "$url"
    else
        return 127
    fi
}

download_forever_full() {
    local url="$1" destination="$2"
    printf '[下载] 人工重下，不限制总时长：%s\n' "$url" >&2
    if command -v curl >/dev/null 2>&1; then
        curl --fail --location --progress-bar --show-error --output "$destination" "$url"
    elif command -v wget >/dev/null 2>&1; then
        wget --tries=1 --progress=bar:force --output-document="$destination" "$url"
    else
        return 127
    fi
}

find_local_release_asset() {
    local url="$1" filename path
    case "$url" in
        https://github.com/*/releases/download/*) ;;
        *) return 1 ;;
    esac
    filename="${url##*/}"
    case "$filename" in
        *.mrpack|*.zip|*-update-manifest.tsv) ;;
        *) return 1 ;;
    esac
    for path in "$ROOT_DIR/$filename" "$ROOT_DIR/updates/$filename" "$ROOT_DIR/downloads/$filename" "$ROOT_DIR/$UPDATE_DIR/$filename"; do
        [[ -f "$path" && -r "$path" ]] || continue
        printf '%s\n' "$path"
        return 0
    done
    return 1
}

try_download() {
    local url="$1" destination="$2" candidate elapsed remaining attempts_left slice index=0 size choice local_path
    local start_seconds="$SECONDS" best_index=0 best_size=-1
    local -a candidates=("$url") partials=() sizes=()
    local local_asset=""
    if local_asset="$(find_local_release_asset "$url")"; then
        printf '[本地包] 使用已有更新资源：%s\n' "$local_asset" >&2
        cp -- "$local_asset" "$destination" || return 1
        return 0
    fi
    case "$url" in
        https://github.com/*/releases/download/*) candidates+=("https://ghproxy.net/$url" "https://gh-proxy.com/$url") ;;
    esac
    for index in "${!candidates[@]}"; do
        candidate="${candidates[$index]}"
        partials[$index]="${destination}.source${index}.part"
        elapsed=$((SECONDS - start_seconds))
        remaining=$((120 - elapsed))
        if [[ "$remaining" -le 0 ]]; then printf '[下载] 首轮总预算 120 秒已用完。\n' >&2; break; fi
        attempts_left=$(( ${#candidates[@]} - index ))
        slice=$((remaining / attempts_left))
        [[ "$slice" -ge 1 ]] || slice=1
        printf '[下载] 来源 %d/%d：%s\n' "$((index + 1))" "${#candidates[@]}" "$candidate" >&2
        if download_once "$candidate" "${partials[$index]}" "$slice"; then
            mv -f -- "${partials[$index]}" "$destination"
            rm -f -- "${partials[@]}"
            return 0
        fi
        size=0
        [[ -f "${partials[$index]}" ]] && size=$(wc -c < "${partials[$index]}" | tr -d ' ')
        sizes[$index]="$size"
        printf '[下载] 来源失败，保留部分文件：%s 字节。\n' "$size" >&2
    done
    case "$url" in *-delta.zip|*-update-manifest.tsv) rm -f -- "${partials[@]}"; return 1 ;; esac
    [[ "${#candidates[@]}" -gt 1 ]] || { rm -f -- "${partials[@]}"; return 1; }
    best_index=0
    best_size=-1
    for index in "${!candidates[@]}"; do
        size="${sizes[$index]:-0}"
        if [[ "$size" -gt "$best_size" ]]; then best_size="$size"; best_index="$index"; fi
    done
    printf '\n所有下载源均失败。\n'
    printf '本次需要的完整更新包：%s\n' "${url##*/}"
    printf '已自动搜索实例根目录、updates/、downloads/ 和 %s/，未找到可用的同名文件。\n' "$UPDATE_DIR"
    printf '请下载该文件并上传到实例目录，或输入服务器/客户端上已有文件的绝对路径。\n'
    printf '1) 使用已下载字节数最多的来源：%s（已下载 %s 字节），尝试续传\n' "${candidates[$best_index]}" "$best_size"
    printf '2) 手动指定已上传的完整更新包路径\n'
    while true; do
        read -r -p '请输入 1 或 2：' choice || die "无法读取输入，更新已中止"
        case "$choice" in
            1)
                candidate="${candidates[$best_index]}"
                [[ -f "${partials[$best_index]}" ]] || : > "${partials[$best_index]}"
                while true; do
                    if download_forever "$candidate" "${partials[$best_index]}"; then
                        mv -f -- "${partials[$best_index]}" "$destination"
                        rm -f -- "${partials[@]}"
                        return 0
                    fi
                    # Some mirrors ignore HTTP Range. Retry from zero on the same chosen source.
                    if download_forever_full "$candidate" "${partials[$best_index]}"; then
                        mv -f -- "${partials[$best_index]}" "$destination"
                        rm -f -- "${partials[@]}"
                        return 0
                    fi
                    size=0
                    [[ -f "${partials[$best_index]}" ]] && size=$(wc -c < "${partials[$best_index]}" | tr -d ' ')
                    printf '[下载] 续传/重下失败，当前源已保留 %s 字节；5 秒后重试同一来源。按 Ctrl+C 可中止。\n' "$size" >&2
                    sleep 5
                done
                ;;
            2)
                read -r -p '请输入完整更新包在本机的绝对路径：' local_path || die "无法读取文件路径"
                if [[ -f "$local_path" && -r "$local_path" ]]; then
                    cp -- "$local_path" "$destination" || die "无法复制手动提供的更新包"
                    rm -f -- "${partials[@]}"
                    return 0
                fi
                printf '文件不存在或不可读：%s\n请重新输入。\n' "$local_path" >&2
                ;;
            *) printf '输入无效，请输入 1 或 2。\n' >&2 ;;
        esac
    done
}

download() {
    local url="$1" destination="$2"
    try_download "$url" "$destination" || die "下载失败：$url"
}
progress_step() {
    local label="$1"
    PROGRESS_DONE=$((PROGRESS_DONE + 1))
    local percent=0
    if [[ "$PROGRESS_TOTAL" -gt 0 ]]; then
        percent=$((PROGRESS_DONE * 100 / PROGRESS_TOTAL))
    fi
    printf '\r[进度] %d/%d (%d%%) %s' "$PROGRESS_DONE" "$PROGRESS_TOTAL" "$percent" "$label" >&2
    if [[ "$PROGRESS_DONE" -ge "$PROGRESS_TOTAL" ]]; then
        printf '\n' >&2
    fi
}

validate_relative_path() {
    local path="$1"
    [[ -n "$path" && "$path" != "." && "$path" != ".." ]] || die "清单路径为空或不安全：$path"
    [[ "$path" != /* && "$path" != *"\\"* && "$path" != *$'\r'* ]] || die "清单路径不安全：$path"
    [[ "$path" != *$'\t'* && "$path" != *$'\001'* ]] || die "清单包含控制字符"
    case ":$path:" in
        *:/*|*:*:*) die "清单路径不是规范相对路径：$path" ;;
    esac
    local remaining="$path" segment
    while [[ -n "$remaining" ]]; do
        segment="${remaining%%/*}"
        if [[ "$segment" == "" || "$segment" == "." || "$segment" == ".." ]]; then
            die "清单路径包含不安全片段：$path"
        fi
        if [[ "$remaining" == */* ]]; then
            remaining="${remaining#*/}"
        else
            remaining=""
        fi
    done
}

validate_zip() {
    local archive="$1" entries="$2"
    unzip -tq "$archive" >/dev/null || die "压缩包完整性检查失败：$archive"
    unzip -Z1 "$archive" > "$entries" || die "无法读取压缩包目录：$archive"
    while IFS= read -r member; do
        [[ -n "$member" ]] || continue
        [[ "$member" != /* && "$member" != *"\\"* ]] || die "压缩包包含不安全路径：$member"
        case "$member" in
            *../*|*/../*|*/..|../*|..) die "压缩包包含路径穿越：$member" ;;
        esac
        local name="${member%/}"
        name="${name##*/}"
        case "$(printf '%s' "$name" | tr '[:upper:]' '[:lower:]')" in
            update-dimensionworks-client.sh|update-dimensionworks-client.bat|update-dimensionworks-client.exe|\
            update-dimensionworks-server.sh|update-dimensionworks-server.bat|update-dimensionworks-server.exe)
                die "更新脚本不得打入整合包：$member"
                ;;
        esac
    done < "$entries"
    if unzip -Z -l "$archive" 2>/dev/null | awk 'substr($0, 1, 1) == "l" { found=1 } END { exit !found }'; then
        die "压缩包不得包含符号链接：$archive"
    fi
}

manifest_is_valid() {
    local path="$1"
    [[ -f "$path" ]] || return 1
    LC_ALL=C grep -q $'\r' "$path" && return 1
    LC_ALL=C awk -F '\t' '
        NR == 1 && $0 != "# DimensionWorks update manifest 1" { exit 1 }
        NR == 2 && $0 != "# kind client" { exit 1 }
        NR == 3 && $0 !~ /^# version [0-9]+\.[0-9]+\.[0-9]+$/ { exit 1 }
        NR == 4 && $0 !~ /^# release v[0-9]+\.[0-9]+\.[0-9]+$/ { exit 1 }
        NR == 5 && $0 !~ /^# base (none|v[0-9]+\.[0-9]+\.[0-9]+)$/ { exit 1 }
        NR == 6 && $0 != "action\tpath\talgorithm\tvalue\tsource" { exit 1 }
        NR > 6 {
            if (NF != 5 || seen[tolower($2)]++) exit 1
            if ($1 != "remote" && $1 != "archive") exit 1
            if ($2 == "" || $2 ~ /^\// || $2 ~ /\\/ || $2 ~ /:/ || $2 ~ /(^|\/)\.\.?(\/|$)/) exit 1
            expected = ($1 == "remote" ? 128 : 64)
            if (($1 == "remote" && $3 != "sha512") || ($1 == "archive" && $3 != "sha256")) exit 1
            if (length($4) != expected || $4 !~ /^[0-9a-f]+$/) exit 1
            if ($5 == "" || ($1 == "remote" && $5 ~ /[[:space:]]/)) exit 1
            if ($1 == "archive" && ($5 ~ /^\// || $5 ~ /\\/ || $5 ~ /:/ || $5 ~ /(^|\/)\.\.?(\/|$)/)) exit 1
            if ($1 == "remote" && $5 !~ /^https?:\/\/[^[:space:]]+$/) exit 1
        }
        END { if (NR < 6) exit 1 }
    ' "$path"
}

validate_manifest() {
    manifest_is_valid "$1" || die "更新清单格式无效：$1"
}

state_digest_for_path() {
    local state_file="$1" wanted_path="$2"
    [[ -f "$state_file" ]] || return 0
    awk -F '\t' -v wanted="$wanted_path" 'NR > 6 && $2 == wanted { print $4; exit }' "$state_file"
}

is_preserved_client() {
    case "$1" in
        saves/*|logs/*|crash-reports/*|screenshots/*|world/*|world_nether/*|world_the_end/*)
            return 0
            ;;
    esac
    return 1
}

bootstrap_state() {
    local state_tmp="$UPDATE_DIR/bootstrap.tsv" root relative digest
    {
        printf '%s\n' \
            '# DimensionWorks update manifest 1' \
            '# kind client' \
            '# version 0.0.0' \
            '# release v0.0.0' \
            '# base none'
        printf 'action\tpath\talgorithm\tvalue\tsource\n'
        for root in mods shaderpacks config defaultconfigs kubejs resourcepacks local; do
            [[ -d "$ROOT_DIR/$root" ]] || continue
            while IFS= read -r -d '' file; do
                relative="${file#"$ROOT_DIR/"}"
                is_preserved_client "$relative" && continue
                [[ ! -L "$file" ]] || continue
                digest="$(sha_file 256 "$file")"
                printf 'archive\t%s\tsha256\t%s\t%s\n' "$relative" "$digest" "$relative"
            done < <(find "$ROOT_DIR/$root" -type f -print0)
        done
        for relative in \
            modrinth.index.json options.txt log4j2.xml rhino.local.properties \
            CustomSkinLoader/CustomSkinLoader.json minemenu/menu.json; do
            [[ -f "$ROOT_DIR/$relative" && ! -L "$ROOT_DIR/$relative" ]] || continue
            digest="$(sha_file 256 "$ROOT_DIR/$relative")"
            printf 'archive\t%s\tsha256\t%s\t%s\n' "$relative" "$digest" "$relative"
        done
    } > "$state_tmp"
    mv -f "$state_tmp" "$ROOT_DIR/$STATE_REL"
}

ensure_no_symlink() {
    local relative="$1"
    local current="$ROOT_DIR" part path="$relative"
    [[ ! -L "$current" ]] || die "实例根目录不得是符号链接"
    while [[ -n "$path" ]]; do
        part="${path%%/*}"
        current="$current/$part"
        if [[ -L "$current" ]]; then
            die "受管路径包含符号链接：$relative"
        fi
        if [[ "$path" == */* ]]; then
            path="${path#*/}"
        else
            path=""
        fi
    done
}

ensure_parent_dirs() {
    local relative="$1" parent="${1%/*}" path
    [[ "$parent" != "$relative" ]] || parent=""
    path="$ROOT_DIR"
    if [[ -n "$parent" ]]; then
        local old_ifs="$IFS"
        IFS='/'
        # Paths may contain spaces; split only on the POSIX separator.
        set -f
        local -a segments=($parent)
        set +f
        IFS="$old_ifs"
        local segment
        for segment in "${segments[@]}"; do
            path="$path/$segment"
            if [[ -e "$path" && ! -d "$path" ]]; then
                die "目标父路径被文件占用：${path#$ROOT_DIR/}"
            fi
            if [[ ! -e "$path" ]]; then
                mkdir "$path" || die "无法创建目录：${path#$ROOT_DIR/}"
                printf 'D\t%s\n' "$path" >> "$UPDATE_DIR/$TRANSACTION_LOG"
            fi
        done
    fi
}

rollback() {
    local line kind target backup reverse="$UPDATE_DIR/rollback.lines"
    [[ -f "$UPDATE_DIR/$TRANSACTION_LOG" ]] || return 0
    awk '{ lines[NR]=$0 } END { for (i=NR; i>=1; i--) print lines[i] }' \
        "$UPDATE_DIR/$TRANSACTION_LOG" > "$reverse" || return 1
    while IFS=$'\t' read -r kind target backup; do
        case "$kind" in
            N)
                if [[ -e "$target" || -L "$target" ]]; then
                    rm -f -- "$target" || return 1
                fi
                ;;
            B)
                if [[ "$backup" != "-" ]]; then
                    rm -f -- "$target" || return 1
                    cp -p -- "$backup" "$target" || return 1
                fi
                ;;
            D)
                rmdir -- "$target" 2>/dev/null || true
                ;;
        esac
    done < "$reverse"
}

cleanup() {
    if [[ -n "$WORK_DIR" && -d "$WORK_DIR" ]]; then
        rm -rf -- "$WORK_DIR"
    fi
    if [[ "$LOCK_HELD" -eq 1 ]]; then
        rm -f -- "$LOCK_DIR/pid"
        rmdir -- "$LOCK_DIR" 2>/dev/null || true
    fi
}

process_matches_updater() {
    local pid="$1" command_line
    [[ "$pid" =~ ^[0-9]+$ ]] || return 1
    kill -0 "$pid" 2>/dev/null || return 1
    command_line="$(ps -p "$pid" -o command= 2>/dev/null || true)"
    [[ "$command_line" == *"$SCRIPT_NAME"* ]]
}

lock_has_live_updater() {
    local owner_pid="" pid command_line
    if [[ -r "$LOCK_DIR/pid" ]]; then
        IFS= read -r owner_pid < "$LOCK_DIR/pid" || true
        if process_matches_updater "$owner_pid"; then
            return 0
        fi
    fi

    # Legacy locks have no PID file. Check for another instance of this updater
    # before removing the lock, so an old but active updater is never interrupted.
    while read -r pid command_line; do
        [[ "$pid" =~ ^[0-9]+$ && "$pid" != "$" ]] || continue
        [[ "$command_line" == *"$SCRIPT_NAME"* ]] || continue
        return 0
    done < <(ps -axo pid=,command= 2>/dev/null)
    return 1
}

acquire_lock() {
    mkdir -p "$UPDATE_DIR" || die "无法创建更新目录"
    if ! mkdir "$LOCK_DIR" 2>/dev/null; then
        if lock_has_live_updater; then
            die "检测到另一个更新任务仍在运行，拒绝删除锁文件"
        fi
        printf '[锁检查] 未发现仍在运行的上次更新进程，清理残留锁。\n' >&2
        rm -rf -- "$LOCK_DIR" || die "无法清理残留锁：$LOCK_DIR"
        mkdir "$LOCK_DIR" 2>/dev/null || die "另一个更新任务刚刚取得锁，请稍后重试"
    fi
    printf '%s\n' "$" > "$LOCK_DIR/pid" || {
        rm -rf -- "$LOCK_DIR"
        die "无法写入更新锁 PID"
    }
    LOCK_HELD=1
}

on_exit() {
    local status=$?
    trap - EXIT INT TERM
    if [[ "$status" -ne 0 && "$TX_ACTIVE" -eq 1 ]]; then
        if rollback; then
            printf '更新失败，已恢复事务前状态。\n' >&2
        else
            printf '更新失败，自动恢复未完成；请检查 %s 中的备份。\n' "$ROOT_DIR/$BACKUP_DIR" >&2
        fi
    fi
    cleanup || true
    if [[ "$status" -eq 0 ]]; then
        rm -rf -- "$ROOT_DIR/$BACKUP_DIR" 2>/dev/null || true
    fi
    exit "$status"
}

log_change() {
    local target="$1"
    local source="$2"
    local backup_path="$ROOT_DIR/$BACKUP_DIR/${target#$ROOT_DIR/}"
    if [[ -e "$target" || -L "$target" ]]; then
        mkdir -p "${backup_path%/*}"
        cp -p -- "$target" "$backup_path" || die "无法备份：${target#$ROOT_DIR/}"
        printf 'B\t%s\t%s\n' "$target" "$backup_path" >> "$UPDATE_DIR/$TRANSACTION_LOG"
    else
        printf 'N\t%s\t-\n' "$target" >> "$UPDATE_DIR/$TRANSACTION_LOG"
    fi
    mkdir -p "${backup_path%/*}"
    cp -- "$source" "$backup_path.tmp" || die "无法暂存变更：${target#$ROOT_DIR/}"
    mv -f -- "$backup_path.tmp" "$target" || die "无法应用变更：${target#$ROOT_DIR/}"
}

parse_args() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --help|-h)
                usage
                exit 0
                ;;
            --release)
                [[ $# -ge 2 ]] || die "--release 缺少参数"
                RELEASE_ARG="$2"
                shift 2
                ;;
            --package)
                [[ $# -ge 2 ]] || die "--package 缺少参数"
                PACKAGE_ARG="$2"
                shift 2
                ;;
            --url)
                [[ $# -ge 2 ]] || die "--url 缺少参数"
                URL_ARG="$2"
                shift 2
                ;;
            *)
                die "未知参数：$1（使用 --help 查看用法）"
                ;;
        esac
    done
    local sources=0
    [[ -n "$RELEASE_ARG" ]] && sources=$((sources + 1))
    [[ -n "$PACKAGE_ARG" ]] && sources=$((sources + 1))
    [[ -n "$URL_ARG" ]] && sources=$((sources + 1))
    [[ "$sources" -le 1 ]] || die "--release、--package 和 --url 只能选择一个"
}

resolve_sources() {
    local package_name manifest_name
    if [[ -n "$PACKAGE_ARG" ]]; then
        [[ -f "$PACKAGE_ARG" ]] || die "本地包不存在：$PACKAGE_ARG"
        PACKAGE_URL="$PACKAGE_ARG"
        return
    fi
    if [[ -n "$URL_ARG" ]]; then
        PACKAGE_URL="$URL_ARG"
        case "$PACKAGE_URL" in
            *.mrpack)
                MANIFEST_URL="${PACKAGE_URL%.mrpack}-update-manifest.tsv"
                ;;
            *) die "--url 必须指向 .mrpack 完整包" ;;
        esac
        return
    fi
    if [[ -n "$RELEASE_ARG" ]]; then
        TARGET_RELEASE="$RELEASE_ARG"
    else
        local latest latest_url
        latest="$(download_to_stdout "https://api.github.com/repos/$REPO/releases/latest" 2>/dev/null)" || latest=""
        TARGET_RELEASE="$(printf '%s' "$latest" | sed -n 's/^[[:space:]]*"tag_name":[[:space:]]*"\([^"]*\)".*/\1/p' | head -n 1)"
        if [[ -z "$TARGET_RELEASE" ]]; then
            # GitHub API may be blocked/reset by some server networks; fall back to the browser redirect.
            latest_url="$(resolve_latest_release_url)" ||
                die "无法查询 GitHub Latest Release（API 与网页入口均连接失败）；可使用 --release vX.Y.Z 指定版本"
            TARGET_RELEASE="${latest_url##*/tag/}"
        fi
        [[ -n "$TARGET_RELEASE" ]] || die "GitHub Latest Release 没有 tag_name"
    fi
    [[ "$TARGET_RELEASE" =~ ^v[0-9]+\.[0-9]+\.[0-9]+$ ]] || die "Release 标签格式无效：$TARGET_RELEASE"
    TARGET_VERSION="${TARGET_RELEASE#v}"
    package_name="$ASSET_PREFIX-$TARGET_VERSION.mrpack"
    manifest_name="$ASSET_PREFIX-$TARGET_VERSION-update-manifest.tsv"
    PACKAGE_URL="https://github.com/$REPO/releases/download/$TARGET_RELEASE/$package_name"
    MANIFEST_URL="https://github.com/$REPO/releases/download/$TARGET_RELEASE/$manifest_name"
}

download_to_stdout() {
    if command -v curl >/dev/null 2>&1; then
        curl --fail --location --silent --show-error --retry 3 --connect-timeout 10 --max-time 45 "$1"
    elif command -v wget >/dev/null 2>&1; then
        wget -qO- "$1"
    else
        return 1
    fi
}

resolve_latest_release_url() {
    command -v curl >/dev/null 2>&1 || return 1
    curl --fail --location --silent --show-error --retry 2 \
        --connect-timeout 10 --max-time 45 --output /dev/null --write-out '%{url_effective}' \
        "https://github.com/$REPO/releases/latest"
}

stop_related_processes() {
    printf '[第零步] 检查并关闭当前整合包相关进程...\n' >&2
    local table targets protected changed pid ppid cmd cursor remaining=0
    table="$(mktemp "${TMPDIR:-/tmp}/dw-processes.XXXXXX")" || die "无法创建进程检查临时文件"
    ps -axo pid=,ppid=,command= > "$table" || { rm -f "$table"; die "无法枚举进程"; }
    targets="$table.targets"; protected="$table.protected"; : > "$targets"; : > "$protected"
    cursor="$$"
    while [[ "$cursor" =~ ^[0-9]+$ ]] && [[ "$cursor" -gt 1 ]]; do
        local parent_line parent_cmd
        parent_line="$(awk -v id="$cursor" '$1==id {sub(/^[[:space:]]*[0-9]+[[:space:]]+[0-9]+[[:space:]]+/, ""); print; exit}' "$table")"
        case "$parent_line" in *java*|*javaw*) break ;; esac
        printf '%s\n' "$cursor" >> "$protected"
        cursor="$(awk -v id="$cursor" '$1==id {print $2; exit}' "$table")"
        [[ -n "$cursor" ]] || break
    done
    while read -r pid ppid cmd; do
        [[ "$pid" =~ ^[0-9]+$ ]] || continue
        grep -qx "$pid" "$protected" && continue
        case "$cmd" in
            *java*|*minecraft*|*Minecraft*)
                if [[ "$cmd" == *"$ROOT_DIR"* ]]; then
                    printf '%s\n' "$pid" >> "$targets"
                elif [[ "$KIND" == "server" && "$cmd" == *"@libraries"* && "$cmd" == *"forge"* && "$cmd" == *"nogui"* ]] && command -v jcmd >/dev/null 2>&1; then
                    local working
                    working="$(jcmd "$pid" VM.system_properties 2>/dev/null | sed -n 's/^user.dir=//p' | head -n 1)"
                    if [[ -n "$working" && "$(cd "$working" 2>/dev/null && pwd -P)" == "$ROOT_DIR" ]]; then
                        printf '%s\n' "$pid" >> "$targets"
                    fi
                fi
                ;;
        esac
    done < "$table"
    changed=1
    while [[ "$changed" -eq 1 ]]; do
        changed=0
        while read -r pid ppid cmd; do
            [[ "$pid" =~ ^[0-9]+$ ]] || continue
            grep -qx "$pid" "$protected" && continue
            grep -qx "$pid" "$targets" && continue
            if grep -qx "$ppid" "$targets"; then printf '%s\n' "$pid" >> "$targets"; changed=1; fi
        done < "$table"
    done
    if [[ ! -s "$targets" ]]; then printf '[第零步] 未发现可确认属于当前目录的 Minecraft/Java 进程。\n' >&2; rm -f "$table" "$targets" "$protected"; return; fi
    while read -r pid; do kill -TERM "$pid" 2>/dev/null || true; done < "$targets"
    sleep 2
    while read -r pid; do kill -0 "$pid" 2>/dev/null && kill -KILL "$pid" 2>/dev/null || true; done < "$targets"
    while read -r pid; do kill -0 "$pid" 2>/dev/null && remaining=1 || true; done < "$targets"
    rm -f "$table" "$targets" "$protected"
    [[ "$remaining" -eq 0 ]] || die "仍有相关进程无法关闭，停止更新"
    printf '[第零步] 相关进程已关闭。\n' >&2
}

main() {
    parse_args "$@"
    ROOT_DIR="$(cd "$(dirname "$0")" && pwd -P)" || die "无法确定实例根目录"
    cd "$ROOT_DIR" || die "无法进入实例根目录"
    [[ -d "$ROOT_DIR" ]] || die "实例根目录不存在"
    acquire_lock
    WORK_DIR="$UPDATE_DIR/work.$$"
    mkdir "$WORK_DIR" || die "无法创建临时目录"
    PACKAGE_ROOT="$WORK_DIR/package"
    TARGET_MANIFEST="$WORK_DIR/target.tsv"
    mkdir "$PACKAGE_ROOT"
    resolve_sources
    local local_package=""
    if [[ -n "$MANIFEST_URL" ]]; then
        local_package="$(find_local_release_asset "$PACKAGE_URL" || true)"
    fi
    if [[ -n "$local_package" ]]; then
        PACKAGE_LOCAL="$WORK_DIR/package.zip"
        printf '[本地包] 找到完整更新包：%s\n' "$local_package" >&2
        cp -- "$local_package" "$PACKAGE_LOCAL" || die "无法复制本地完整更新包"
        MANIFEST_URL=""
        validate_zip "$PACKAGE_LOCAL" "$WORK_DIR/package.entries"
        unzip -p "$PACKAGE_LOCAL" "$MANIFEST_NAME" > "$TARGET_MANIFEST" ||
            die "完整包缺少 $MANIFEST_NAME"
    elif [[ -n "$MANIFEST_URL" ]]; then
        if ! try_download "$MANIFEST_URL" "$TARGET_MANIFEST"; then
            MANIFEST_URL=""
            case "$PACKAGE_URL" in
                http://*|https://*)
                    PACKAGE_LOCAL="$WORK_DIR/package.zip"
                    download "$PACKAGE_URL" "$PACKAGE_LOCAL" || die "下载完整更新包失败"
                    ;;
                *)
                    PACKAGE_LOCAL="$PACKAGE_URL"
                    ;;
            esac
            validate_zip "$PACKAGE_LOCAL" "$WORK_DIR/package.entries"
            unzip -p "$PACKAGE_LOCAL" "$MANIFEST_NAME" > "$TARGET_MANIFEST" ||
                die "完整包缺少 $MANIFEST_NAME"
        fi
    else
        PACKAGE_LOCAL="$PACKAGE_URL"
        validate_zip "$PACKAGE_LOCAL" "$WORK_DIR/package.entries"
        unzip -p "$PACKAGE_LOCAL" "$MANIFEST_NAME" > "$TARGET_MANIFEST" ||
            die "完整包缺少 $MANIFEST_NAME"
    fi
    validate_manifest "$TARGET_MANIFEST" "$KIND"
    TARGET_VERSION="$(sed -n 's/^# version //p' "$TARGET_MANIFEST")"
    TARGET_RELEASE="$(sed -n 's/^# release //p' "$TARGET_MANIFEST")"
    local target_base
    target_base="$(sed -n 's/^# base //p' "$TARGET_MANIFEST")"
    [[ "$TARGET_RELEASE" == "v$TARGET_VERSION" ]] || die "Release 与版本不一致"

    : > "$UPDATE_DIR/$TRANSACTION_LOG"
    if manifest_is_valid "$ROOT_DIR/$STATE_REL"; then
        OLD_VERSION="$(sed -n 's/^# version //p' "$STATE_REL")"
    else
        bootstrap_state
        OLD_VERSION=""
    fi
    printf 'DimensionWorks 更新：%s -> %s\n' "${OLD_VERSION:-未安装}" "$TARGET_RELEASE"
    PROGRESS_TOTAL="$(awk 'NR > 6 { count++ } END { print count + 0 }' "$TARGET_MANIFEST")"
    printf '[进度] 检查 %d 个受管文件\n' "$PROGRESS_TOTAL" >&2

    local archive_needed=0
    while IFS=$'\t' read -r action path algorithm value source; do
        [[ "$action" == "archive" ]] || continue
        if is_preserved_client "$path"; then continue; fi
        if [[ ! -f "$ROOT_DIR/$path" ]]; then
            archive_needed=1
            break
        fi
        if [[ "$(sha_file 256 "$ROOT_DIR/$path")" != "$value" ]]; then
            local old_digest
            old_digest="$(state_digest_for_path "$ROOT_DIR/$STATE_REL" "$path")"
            [[ "$old_digest" == "$value" ]] && continue
            archive_needed=1
            break
        fi
    done < <(awk -F '\t' 'NR > 6' "$TARGET_MANIFEST")

    local delta_root="$WORK_DIR/delta"
    local delta_ready=0
    if [[ "$archive_needed" -eq 1 && ( -z "$MANIFEST_URL" || -z "$OLD_VERSION" || "v$OLD_VERSION" != "$target_base" ) ]]; then
        printf '[差量] 跳过：当前版本状态与差量包基准不匹配（当前=%s，目标基准=%s）。\\n' "${OLD_VERSION:-未知}" "$target_base" >&2
    fi
    if [[ "$archive_needed" -eq 1 && -n "$MANIFEST_URL" && -n "$OLD_VERSION" && "v$OLD_VERSION" == "$target_base" ]]; then
        local delta_url="${PACKAGE_URL%.mrpack}-delta.zip"
        if try_download "$delta_url" "$WORK_DIR/delta.zip"; then
            if (validate_zip "$WORK_DIR/delta.zip" "$WORK_DIR/delta.entries") 2>/dev/null; then
                mkdir -p "$delta_root"
                unzip -q "$WORK_DIR/delta.zip" -d "$delta_root" 2>/dev/null || true
                if [[ -d "$delta_root" ]]; then
                    delta_ready=1
                    while IFS=$'\t' read -r action path algorithm value source; do
                        [[ "$action" == "archive" ]] || continue
                        if is_preserved_client "$path"; then continue; fi
                        if [[ -f "$ROOT_DIR/$path" ]] && [[ "$(sha_file 256 "$ROOT_DIR/$path")" == "$value" ]]; then
                            continue
                        fi
                        if [[ -f "$ROOT_DIR/$path" ]]; then
                            local old_digest
                            old_digest="$(state_digest_for_path "$ROOT_DIR/$STATE_REL" "$path")"
                            [[ "$old_digest" == "$value" ]] && continue
                        fi
                        if [[ ! -f "$delta_root/$source" ]]; then
                            printf '[差量校验] 本地文件与目标版本不同，但差量包不包含：%s（源：%s）。需要完整包恢复该文件。\\n' "$path" "$source" >&2
                            delta_ready=0
                            break
                        fi
                        if [[ "$(sha_file 256 "$delta_root/$source")" != "$value" ]]; then
                            printf '[差量校验] 差量包文件摘要与目标清单不符：%s（源：%s）。\\n' "$path" "$source" >&2
                            delta_ready=0
                            break
                        fi
                    done < <(awk -F '\t' 'NR > 6' "$TARGET_MANIFEST")
                    if [[ "$delta_ready" -eq 1 ]]; then
                        PACKAGE_ROOT="$delta_root"
                        DELTA_USED=1
                    fi
                fi
            fi
        fi
    fi
    if [[ "$archive_needed" -eq 1 && "$delta_ready" -eq 0 ]]; then
        printf '[差量] 无法覆盖本地所有需要修复的受管文件，安全回退到完整客户端包。\\n' >&2
        if [[ ! -f "$PACKAGE_LOCAL" ]]; then
            case "$PACKAGE_URL" in
                http://*|https://*)
                    PACKAGE_LOCAL="$WORK_DIR/package.zip"
                    download "$PACKAGE_URL" "$PACKAGE_LOCAL" || die "下载完整客户端包失败"
                    ;;
                *)
                    PACKAGE_LOCAL="$PACKAGE_URL"
                    ;;
            esac
        fi
        validate_zip "$PACKAGE_LOCAL" "$WORK_DIR/package.entries"
        unzip -q "$PACKAGE_LOCAL" -d "$PACKAGE_ROOT" || die "解压完整包失败"
    fi
    local manifest_entries="$WORK_DIR/entries.tsv"
    awk -F '\t' 'NR > 6' "$TARGET_MANIFEST" > "$manifest_entries"
    local action path algorithm value source
    while IFS=$'\t' read -r action path algorithm value source; do
        progress_step "$path"
        if is_preserved_client "$path"; then
            continue
        fi
        ensure_no_symlink "$path"
        local staged="$WORK_DIR/staged/$path"
        mkdir -p "${staged%/*}"
        case "$action" in
            remote)
                if [[ -f "$ROOT_DIR/$path" ]] &&
                   [[ "$(sha_file 512 "$ROOT_DIR/$path")" == "$value" ]]; then
                    continue
                fi
                download "$source" "$staged" || die "下载受管文件失败：$source"
                [[ "$(sha_file 512 "$staged")" == "$value" ]] ||
                    die "远程文件摘要不匹配：$path"
                ;;
            archive)
                if [[ -f "$ROOT_DIR/$path" ]] &&
                   [[ "$(sha_file 256 "$ROOT_DIR/$path")" == "$value" ]]; then
                    continue
                fi
                if [[ -f "$ROOT_DIR/$path" ]]; then
                    local old_digest
                    old_digest="$(state_digest_for_path "$ROOT_DIR/$STATE_REL" "$path")"
                    [[ "$old_digest" == "$value" ]] && continue
                fi
                local archive_source="$PACKAGE_ROOT/$source"
                [[ -f "$archive_source" && ! -L "$archive_source" ]] ||
                    die "完整包缺少受管文件：$source"
                [[ "$(sha_file 256 "$archive_source")" == "$value" ]] ||
                    die "完整包文件摘要不匹配：$source"
                cp -p -- "$archive_source" "$staged"
                ;;
            *) die "未知清单操作：$action" ;;
        esac
        printf '%s\t%s\n' "$path" "$staged" >> "$WORK_DIR/needed"
    done < "$manifest_entries"

    if manifest_is_valid "$ROOT_DIR/$STATE_REL"; then
        local seen_paths="$WORK_DIR/seen.paths"
        awk -F '\t' 'NR > 6 { print $2 }' "$TARGET_MANIFEST" | sort > "$seen_paths"
        awk -F '\t' 'NR > 6 { print $2 "\t" $4 }' "$STATE_REL" |
        while IFS=$'\t' read -r path value; do
            if ! grep -Fqx "$path" "$seen_paths"; then
                printf 'delete\t%s\t%s\n' "$path" "$value" >> "$WORK_DIR/deletes"
            fi
        done
    fi

    if [[ ! -s "$WORK_DIR/needed" ]] &&
        [[ ! -s "$WORK_DIR/deletes" ]] &&
        [[ -f "$ROOT_DIR/VERSION" ]] &&
        [[ "$(tr -d '\r\n' < "$ROOT_DIR/VERSION")" == "$TARGET_VERSION" ]] &&
        manifest_is_valid "$ROOT_DIR/$STATE_REL" &&
        [[ "$(sed -n 's/^# release //p' "$ROOT_DIR/$STATE_REL")" == "$TARGET_RELEASE" ]]; then
        printf 'DimensionWorks 已是最新版本：%s\n' "$TARGET_RELEASE"
        return 0
    fi

    stop_related_processes
    TX_ACTIVE=1
    if [[ -f "$WORK_DIR/needed" ]]; then
        while IFS=$'\t' read -r path staged; do
            printf '[应用] %s\n' "$path" >&2
            ensure_no_symlink "$path"
            ensure_parent_dirs "$path"
            log_change "$ROOT_DIR/$path" "$staged"
        done < "$WORK_DIR/needed"
    fi
    if [[ -f "$WORK_DIR/deletes" ]]; then
        while IFS=$'\t' read -r _ path value; do
            if is_preserved_client "$path"; then
                continue
            fi
            local current="$ROOT_DIR/$path"
            [[ -e "$current" && ! -L "$current" ]] || continue
            printf '[删除] %s\n' "$path" >&2
            log_change "$current" /dev/null
            rm -f -- "$current" || die "无法删除旧受管文件：$path"
        done < "$WORK_DIR/deletes"
    fi

    if [[ ! -f "$ROOT_DIR/VERSION" ]] || [[ "$(tr -d '\r\n' < "$ROOT_DIR/VERSION")" != "$TARGET_VERSION" ]]; then
        local version_tmp="$UPDATE_DIR/VERSION.tmp.$$"
        printf '%s\n' "$TARGET_VERSION" > "$version_tmp"
        if [[ -e "$ROOT_DIR/VERSION" ]]; then
            local version_backup="$BACKUP_DIR/VERSION"
            mkdir -p "${version_backup%/*}"
            cp -p "$ROOT_DIR/VERSION" "$version_backup"
            printf 'B\t%s\t%s\n' "$ROOT_DIR/VERSION" "$ROOT_DIR/$version_backup" >> "$UPDATE_DIR/$TRANSACTION_LOG"
        else
            printf 'N\t%s\t-\n' "$ROOT_DIR/VERSION" >> "$UPDATE_DIR/$TRANSACTION_LOG"
        fi
        mv -f "$version_tmp" "$ROOT_DIR/VERSION"
    fi
    [[ "$(tr -d '\r\n' < "$ROOT_DIR/VERSION")" == "$TARGET_VERSION" ]] || die "版本号写入后校验失败：$ROOT_DIR/VERSION"

    local state_tmp="$UPDATE_DIR/state.tsv.tmp.$$"
    if [[ "$KIND" == "client" ]]; then
        awk 'NR <= 6' "$TARGET_MANIFEST" > "$state_tmp"
        while IFS= read -r line; do
            path="${line#*$'\t'}"
            path="${path%%$'\t'*}"
            if ! is_preserved_client "$path"; then
                printf '%s\n' "$line" >> "$state_tmp"
            fi
        done < <(awk 'NR > 6' "$TARGET_MANIFEST")
    else
        cp "$TARGET_MANIFEST" "$state_tmp"
    fi
    if [[ -e "$ROOT_DIR/$STATE_REL" || -L "$ROOT_DIR/$STATE_REL" ]]; then
        local state_backup="$BACKUP_DIR/state.tsv"
        mkdir -p "${state_backup%/*}"
        cp -p "$ROOT_DIR/$STATE_REL" "$state_backup"
        printf 'B\t%s\t%s\n' "$ROOT_DIR/$STATE_REL" "$ROOT_DIR/$state_backup" >> "$UPDATE_DIR/$TRANSACTION_LOG"
    else
        printf 'N\t%s\t-\n' "$ROOT_DIR/$STATE_REL" >> "$UPDATE_DIR/$TRANSACTION_LOG"
    fi
    mv -f "$state_tmp" "$ROOT_DIR/$STATE_REL"
    TX_ACTIVE=0
    printf 'DimensionWorks 更新完成：%s -> %s\n' "${OLD_VERSION:-未安装}" "$TARGET_RELEASE"
}

trap on_exit EXIT
trap 'exit 130' INT TERM
main "$@"
