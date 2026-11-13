# Bitwarden 运行故障排查

1. `plugin_setup_invalid`：检查五个 runtime field；Server URL 必须是 HTTPS origin，主密码前后空白按原值保存。
2. `cli_unavailable` / `cli_integrity_failed`：检查 computer 的 Python 3、curl、unzip、架构和固定安装目录；不要跳过 SHA-256 或版本校验。
3. `server_unreachable`：从 Bot computer 检查 DNS、路由、端口和超时；容器中的 `localhost` 不是桌面主机。
4. `tls_untrusted`：为 computer 配置受信任 CA 和正确 hostname；插件没有关闭 TLS 的开关。
5. `login_failed`：确认 personal API client ID/secret 与 `BW_EMAIL` 属于同一账号；不根据模糊 CLI 文本推断账号是否存在。
6. `vault_locked`：确认主密码；失败后重新开始完整流程，不复用旧 session。
7. `sync_failed`：修复 provider 网络后重试；同步失败不会返回旧凭据。
8. `item_not_found`、`item_ambiguous`、`field_missing`：确认 exact hostname、完整 URI、显式 item UUID、Login 类型和 Collection 权限。

日志和活动只保留阶段、错误类别、退出状态和 server origin。direct read 值可能进入当前模型上下文及现有 Shell 会话历史，行为与 1Password 一致。
