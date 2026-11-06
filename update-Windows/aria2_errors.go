package main

import (
	"errors"
	"fmt"
	"os"
	"regexp"
	"strings"
)

type aria2Error struct {
	stage  string
	code   string
	cause  error
	detail string
}

func newAria2Error(stage, code string, cause error, detail string) *aria2Error {
	if code == "" {
		match := regexp.MustCompile(`(?:errorCode=|code=)([0-9]+)`).FindStringSubmatch(detail)
		if len(match) > 1 { code = match[1] }
	}
	return &aria2Error{stage: stage, code: code, cause: cause, detail: detail}
}

func (e *aria2Error) Unwrap() error { return e.cause }

func (e *aria2Error) localFailure() bool {
	return e.stage != "下载任务" || e.code == "9" || e.code == "16" || e.code == "17" || e.code == "18"
}

func (e *aria2Error) retrySingleConnection() bool {
	switch e.code {
	case "1", "8", "19", "22", "29":
		return e.stage == "下载任务"
	}
	return false
}

func (e *aria2Error) Error() string {
	reason, advice := e.explanation()
	label := "aria2c " + e.stage + "失败"
	if e.code != "" { label += "（错误码 " + e.code + "）" }
	detail := strings.TrimSpace(e.detail)
	if e.cause != nil { detail += " " + e.cause.Error() }
	detail = safeErrorForLog(errors.New(strings.TrimSpace(detail)))
	// Engine output can contain ANSI controls and RPC secrets; never echo them.
	detail = regexp.MustCompile(`token:[A-Za-z0-9]+|--rpc-secret=[^\s]+`).ReplaceAllString(detail, "<redacted>")
	detail = regexp.MustCompile(`\x1b\[[0-?]*[ -/]*[@-~]`).ReplaceAllString(detail, "")
	detail = strings.Map(func(r rune) rune {
		if r < 32 || r == 127 { return ' ' }
		return r
	}, detail)
	if len([]rune(detail)) > 2048 { detail = string([]rune(detail)[len([]rune(detail))-2048:]) }
	message := fmt.Sprintf("%s：%s。%s", label, reason, advice)
	if detail != "" { message += "\n原始信息：" + detail }
	return message
}

func (e *aria2Error) explanation() (string, string) {
	detail := strings.ToLower(e.detail)
	if e.cause != nil { detail += " " + strings.ToLower(e.cause.Error()) }
	switch {
	case errors.Is(e.cause, os.ErrPermission) || strings.Contains(detail, "permission denied") || strings.Contains(detail, "operation not permitted"):
		return "系统拒绝执行引擎或写入文件", "检查目录权限、noexec 挂载和安全软件；可将更新器移到可写且允许执行的目录后重试。"
	case strings.Contains(detail, "no space left") || e.code == "9":
		return "磁盘空间不足", "释放实例与临时目录所在磁盘的空间后重试。"
	case strings.Contains(detail, "exec format") || strings.Contains(detail, "bad cpu"):
		return "引擎与操作系统或 CPU 架构不匹配", "请使用对应 Windows amd64、Linux amd64 或 macOS arm64 的正式更新器。"
	case strings.Contains(detail, "shared librar") || strings.Contains(detail, "dyld") || strings.Contains(detail, "dll"):
		return "系统无法加载引擎的运行依赖", "检查原始信息中缺少的系统组件，或换用受支持系统上的对应更新器。"
	case strings.Contains(detail, "certificate") || strings.Contains(detail, "ssl") || strings.Contains(detail, "tls"):
		return "HTTPS 握手或证书校验失败", "检查系统时间、受信任根证书和代理证书；更新器保持证书校验，并尝试下一个来源。"
	case strings.Contains(detail, "sha-256"):
		return "内嵌引擎完整性校验失败", "重新下载正式发布的更新器；损坏的引擎不会执行。"
	}
	switch e.code {
	case "2":
		return "连接或传输超时", "当前来源未及时响应，将尝试下一个来源；检查网络或代理连通性。"
	case "3", "4":
		return "来源没有提供请求的文件", "将尝试下一个来源；全部失败时请核对发行版本和资产是否仍存在。"
	case "5":
		return "下载速度低于来源要求", "将尝试下一个来源。"
	case "6":
		return "域名解析失败", "将尝试下一个来源；检查 DNS 与网络连接。"
	case "7":
		return "无法连接来源服务器", "将尝试下一个来源；检查防火墙和代理设置。"
	case "8", "19", "22", "29":
		return "来源拒绝分段、续传或返回了不兼容的 HTTP 响应", "使用 aria2c 单连接重新下载，仍失败时切换来源。"
	case "16", "17", "18":
		return "无法创建目录、文件或写入磁盘", "检查实例目录的写入权限、文件占用和磁盘状态。"
	case "24":
		return "来源要求登录或拒绝认证", "将尝试下一个来源；核对下载地址和访问权限。"
	case "32":
		return "下载摘要校验失败", "当前文件不能用于更新，请重新下载并核对发布摘要。"
	}
	if e.stage == "RPC 接口" {
		return "本机下载进度接口未响应或连接中断", "引擎已回收；检查安全软件是否阻止回环连接，以及原始引擎输出中的启动错误。"
	}
	if e.stage == "启动引擎" || e.stage == "准备引擎" {
		return "内嵌引擎未能完成释放或启动检查", "请根据原始信息检查目录权限、系统兼容性，或重新下载正式单文件更新器。"
	}
	return "引擎报告了下载异常", "将尝试下一个来源；下面保留了引擎原始信息用于定位原因。"
}
