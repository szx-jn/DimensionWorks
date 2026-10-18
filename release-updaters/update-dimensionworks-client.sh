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

download() {
    local url="$1" destination="$2"
    printf '下载 %s\n' "$url" >&2
    if command -v curl >/dev/null 2>&1; then
        curl --fail --location --progress-bar --show-error --retry 3 --output "$destination" "$url" ||
            die "下载失败：$url"
    elif command -v wget >/dev/null 2>&1; then
        wget --progress=bar:force --output-document="$destination" "$url" ||
            die "下载失败：$url"
    else
        die "系统缺少 curl 或 wget"
    fi
}

try_download() {
    local url="$1" destination="$2"
    printf '下载 %s\n' "$url" >&2
    if command -v curl >/dev/null 2>&1; then
        curl --fail --location --progress-bar --show-error --retry 2 --output "$destination" "$url"
    elif command -v wget >/dev/null 2>&1; then
        wget --progress=bar:force --output-document="$destination" "$url"
    else
        return 1
    fi
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
            update-dimensionworks-client.sh|update-dimensionworks-client.bat|\
            update-dimensionworks-server.sh|update-dimensionworks-server.bat)
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
        rmdir -- "$LOCK_DIR" 2>/dev/null || true
    fi
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
        local latest
        latest="$(download_to_stdout "https://api.github.com/repos/$REPO/releases/latest")" ||
            die "无法查询 GitHub Latest Release"
        TARGET_RELEASE="$(printf '%s' "$latest" | sed -n 's/^[[:space:]]*"tag_name":[[:space:]]*"\([^"]*\)".*/\1/p' | head -n 1)"
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
        curl --fail --location --silent --show-error --retry 3 "$1"
    elif command -v wget >/dev/null 2>&1; then
        wget -qO- "$1"
    else
        return 1
    fi
}

stop_related_processes() {
    printf '[第零步] 检查并关闭当前整合包相关进程...\n' >&2
    local table targets protected changed pid ppid cmd cursor remaining=0
    table="$(mktemp "${TMPDIR:-/tmp}/dw-processes.XXXXXX")" || die "无法创建进程检查临时文件"
    ps -axo pid=,ppid=,command= > "$table" || { rm -f "$table"; die "无法枚举进程"; }
    targets="$table.targets"; protected="$table.protected"; : > "$targets"; : > "$protected"
    cursor="$"
    while [[ "$cursor" =~ ^[0-9]+$ ]] && [[ "$cursor" -gt 1 ]]; do
        printf '%s\n' "$cursor" >> "$protected"
        cursor="$(awk -v id="$cursor" '$1==id {print $2; exit}' "$table")"
        [[ -n "$cursor" ]] || break
    done
    while read -r pid ppid cmd; do
        [[ "$pid" =~ ^[0-9]+$ ]] || continue
        grep -qx "$pid" "$protected" && continue
        case "$cmd" in *java*|*minecraft*|*Minecraft*) [[ "$cmd" == *"$ROOT_DIR"* ]] && printf '%s\n' "$pid" >> "$targets" ;; esac
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
    stop_related_processes
    [[ -d "$ROOT_DIR" ]] || die "实例根目录不存在"
    mkdir -p "$UPDATE_DIR" || die "无法创建更新目录"
    mkdir "$LOCK_DIR" 2>/dev/null || die "已有更新任务正在运行"
    LOCK_HELD=1
    WORK_DIR="$UPDATE_DIR/work.$$"
    mkdir "$WORK_DIR" || die "无法创建临时目录"
    PACKAGE_ROOT="$WORK_DIR/package"
    TARGET_MANIFEST="$WORK_DIR/target.tsv"
    mkdir "$PACKAGE_ROOT"

    resolve_sources
    if [[ -n "$MANIFEST_URL" ]]; then
        if ! try_download "$MANIFEST_URL" "$TARGET_MANIFEST"; then
            MANIFEST_URL=""
            case "$PACKAGE_URL" in
                http://*|https://*)
                    PACKAGE_LOCAL="$WORK_DIR/package.zip"
                    download "$PACKAGE_URL" "$PACKAGE_LOCAL" || die "下载完整客户端包失败"
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
        if [[ ! -f "$ROOT_DIR/$path" ]] || [[ "$(sha_file 256 "$ROOT_DIR/$path")" != "$value" ]]; then
            archive_needed=1
            break
        fi
    done < <(awk -F '\t' 'NR > 6' "$TARGET_MANIFEST")

    local delta_root="$WORK_DIR/delta"
    local delta_ready=0
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
                        if [[ ! -f "$delta_root/$source" ]] ||
                           [[ "$(sha_file 256 "$delta_root/$source")" != "$value" ]]; then
                            delta_ready=0
                            break
                        fi
                    done < <(awk -F '\t' 'NR > 6' "$TARGET_MANIFEST")
                    if [[ "$delta_ready" -eq 1 ]]; then
                        PACKAGE_ROOT="$delta_root"
                    fi
                fi
            fi
        fi
    fi
    if [[ "$archive_needed" -eq 1 && "$delta_ready" -eq 0 ]]; then
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
