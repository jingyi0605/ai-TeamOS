# Bitwarden / Vaultwarden

本插件通过官方 Bitwarden CLI 读取 Login 项目的 username、password 和 TOTP，支持主 Bot 与 `computerUse`。使用 Skills-only 模型，不创建 MCP connector，不复制 Vault 到 OutcomeGo 数据库。

## 配置

使用专用个人账号，只授予所需组织或 Collection 的读取权限。所有 Bot 共享一组配置；API Key 不是天然只读凭据，包装层白名单也不是主机沙箱。不要使用组织管理 API Key。

在 web vault 的 Settings → Security → Keys → View API key 获取 personal API client ID 和 client secret，然后在 Marketplace 配置：

| 字段 | 环境变量 | 输入与用途 |
| --- | --- | --- |
| Server URL | `BW_SERVER_URL` | 受保护运行字段，HTTPS origin；美国 Cloud 为 `https://vault.bitwarden.com`，欧洲为 `https://vault.bitwarden.eu` |
| Account email | `BW_EMAIL` | 受保护运行字段，与 API Key 对应账号核对 |
| Personal API client ID | `BW_CLIENT_ID` | 受保护密码字段；内部映射到官方 CLI `BW_CLIENTID` |
| Personal API client secret | `BW_CLIENT_SECRET` | 受保护密码字段；内部映射到 `BW_CLIENTSECRET` |
| Master password | `BW_MASTER_PASSWORD` | 受保护密码字段，保留前后空格，仅用于解锁 |

字段没有默认值。公开设置仅返回 Saved/Not set，不回显 runtime 值；单独替换某个字段不会清空其他字段。沿用插件的渐进配置行为，可先安装再配置；缺失配置时 wrapper 立即返回字段名，不尝试空值登录。卸载删除配置与 Skill 缓存，撤销 provider 访问还需要在 Bitwarden/Vaultwarden 轮换 API Key 或删除账号。

## 运行与网络

固定 CLI **2026.10.1**，支持 Linux/macOS 的 x64/ARM64。需要 Bash、Python 3、curl、unzip；不依赖插件内的 node_modules。安装器只使用固定 HTTPS release asset，先校验 SHA-256，再验证版本并原子安装。缓存位于 `~/.local/share/outcomego/bitwarden-cli/2026.10.1`；computer 的 HOME 不可写时使用可写 `/workspace`。运维可通过绝对路径 `OUTCOMEGO_BW_CLI_DIR` 指定位置。版本异常不会自动覆盖。

自托管服务器必须是 Bot computer 能解析、路由可达且信任证书的 HTTPS origin，不支持路径前缀、query、fragment 或 URL 内嵌凭据。远程 Docker 中的 `localhost` 指向容器自身，桌面浏览器能访问不能证明 Bot 能访问。内网 CA 应加入 computer 信任链；Node CLI 也可通过运维设置 `NODE_EXTRA_CA_CERTS` 使用 PEM CA。插件没有跳过 TLS 的选项，清除继承的关闭校验选项。

每次动作使用独立 0700 临时目录，按配置 server、API Key 登录、核对账号、主密码解锁、同步、读取的顺序执行；不缓存 session，不在失败后返回旧值。Linux 优先使用 `/dev/shm`；其他平台使用系统临时目录。CLI 必需的认证 token 和加密 Vault 可能短暂写入该目录，正常完成、失败、SIGTERM/SIGINT 后清理。SIGKILL 或主机崩溃无法保证即时清理，应由部署侧清理临时目录。

## 使用与边界

Skill 通过 `SearchBitwardenItems` 获取非 secret metadata；首次读取某个 item 前必须调用 `RequestBitwardenReadApproval`，由服务端创建聊天 widget。用户选择或批准后，调用 `ReadBitwardenCredential` 获取单个字段。普通 Shell、MCP process 和其它子机器人不会收到 `BW_*`，wrapper 只在 Computer 私有的一次性 lease 路径执行。

返回 JSON，成功退出码 0，失败非零。搜索只返回标题、hostname、UUID 和 `ambiguous` 标志；多个候选需要用户选择。仅 exact hostname 或完整 URI 匹配，字段读取只接受显式 UUID 并校验 Login 类型。标题查询不会按用户名搜索。CLI 输出上限 8 MiB，候选上限 50，单命令超时 90 秒，不自动重试认证或网站登录。

凭据值只在 Computer 私有运行时短暂保留为环境变量；`ReadBitwardenCredential` 只向模型返回不含值的浏览器引用，浏览器写入路径解析引用后自动脱敏并在任务结束或 TTL 到期时清理。读取某个 item 的第一个字段前，服务端 widget 同时承担 item 选择和授权，用户拒绝、关闭或未回答时 lease 不会发放。授权只对当前任务中的该 item 有效。本插件不会另外创建凭据日志，不提供 Vault 写入、导出、多账号路由、SSO/邮箱密码备用认证或交互 MFA 自动化。

Bitwarden 服务端仍必须使用 HTTPS；登录目标页面可以是用户明确打开并授权的 HTTP 页面。Computer 只允许在已确认目标 hostname 内写入 HTTP 页面，拒绝把凭据写入其他 HTTP frame。

## 故障排查与验收

| 错误 | 操作 |
| --- | --- |
| `plugin_setup_invalid` | 查看 `detail.field`，补齐字段；URL 使用 HTTPS origin |
| `cli_unavailable` / `cli_integrity_failed` | 检查平台、依赖、下载网络和固定安装目录；不要跳过校验 |
| `server_unreachable` | 查看安全 `reason`：DNS、连接拒绝、超时；从 computer 容器排查 |
| `tls_untrusted` | 修复证书、主机名、过期时间或 CA 信任 |
| `login_failed` | 检查 personal API Key 与 email；服务端未区分时不推断账号存在性 |
| `vault_locked` | 检查主密码；未知 unlock 故障不等于密码错误 |
| `sync_failed` | 修复同步后重新开始，不读取旧缓存 |
| `item_not_found` / `field_missing` | 确认站点、Collection 权限和请求字段 |
| `interactive_auth_required` | 人工处理账号策略；不自动降级认证方式 |
| `provider_incompatible` | 核对 CLI/provider 版本，不把未知 schema 当成功 |

确定性测试：`bun test packages/plugins/test/bitwarden*.test.ts`。

真实实例回放（读取环境中已配置的五个字段，不打印凭据）：`python3 -I packages/plugins/test/fixtures/bitwarden-replay.py <hostname> <item-uuid>`。此命令不会启动任何服务；测试环境需要专用 Login 和 TOTP，凭据由外部注入。

真实 Vaultwarden、远程 computer 与网页登录验收状态见 [兼容性矩阵](../../../specs/005.1-Bitwarden与Vaultwarden凭据插件接入/docs/20261112-Vaultwarden兼容性矩阵.md)。未实测版本不声明支持范围。

升级 CLI 时同时更新安装器四平台 SHA-256、版本、测试和矩阵。下载来源：[官方 release](https://github.com/bitwarden/clients/releases/tag/cli-v2026.10.1)，摘要来源：[release API](https://api.github.com/repos/bitwarden/clients/releases/tags/cli-v2026.10.1)。
