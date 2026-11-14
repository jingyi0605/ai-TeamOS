# 需求文档 - Bitwarden 与 Vaultwarden 凭据插件接入

状态：实施中（2026-11-13 复核修订）

父规格：[Spec 005：插件扩展体系](../005-插件扩展体系/requirements.md)

## 简介

当前项目已经通过 `packages/plugins/1password` 提供登录项查询和浏览器代填能力。该插件依赖 1Password Service Account，适合 1Password Cloud 或组织环境，但不能覆盖希望在本地部署密码库的用户。

Vaultwarden 提供与 Bitwarden 客户端生态兼容的自托管密码库。用户希望在不把 Vault 内容迁移到 OpenTeam 的前提下，让 Bot 直接使用本地 Vaultwarden 中的登录项完成网站登录、读取用户名、密码和一次性验证码。

本功能的目标是提供一个与 1Password 使用方式一致的 Bitwarden 插件，同时把本地部署场景中的 server URL、网络可达性、CLI 登录解锁和版本兼容问题纳入正式的安装、诊断和验收流程。

## 术语表

- **System**：OpenTeam 服务端、Worker、computer runtime、桌面端插件管理页面和 Bot 运行时。
- **Bitwarden CLI**：用于配置 server、登录、解锁、同步和读取 Vault 项目的命令行客户端，命令名为 `bw`。
- **Vaultwarden**：用户自托管的 Bitwarden 兼容密码库服务。
- **Server URL**：Bitwarden CLI 访问的密码库服务根地址，例如 `https://vault.example.test`。
- **Login item**：Bitwarden/Vaultwarden 中的登录类型项目，包含用户名、密码、URI 和可选 TOTP。
- **Credential value**：Bot 从 Login item 读取的 username、password 或 TOTP。
- **Plugin runtime field**：插件 setup field 中带有 `environment` 的字段。它在运行时注入 Bot computer，但不出现在公开配置值中。
- **受限 CLI 包装层**：插件提供的 `bw.sh` 脚本，只允许执行本 Spec 定义的读取流程，不把完整 `bw` 命令集暴露给 Skill。
- **Dedicated account**：专门用于 Bot 的 Bitwarden/Vaultwarden 用户账号，只授权所需组织、Collection 或登录项。

## 范围说明

### In Scope

- 新增稳定插件 key 为 `bitwarden` 的 Skills 插件包。
- 支持 Bitwarden Cloud 和本地 Vaultwarden server URL。
- 支持 API Key 登录、主密码解锁和同步。
- 支持按 URI hostname、标题和 item id 选择 Login item。
- 支持读取 username、password 和 TOTP。
- 支持主 Bot 和 `computerUse` 子机器人使用插件 Skill。
- 支持凭据配置轮换、插件卸载和运行时缓存清理。
- 扩展插件字段，使普通文本 runtime field 能安全传递给 Bot computer。
- 增加 CLI 安装、完整性校验、错误分类和相关自动化测试。

### Out of Scope

- Vault 写操作以及任何组织管理能力。
- 通过 MCP 暴露任意 Bitwarden 工具。
- 多个独立 Bitwarden/Vaultwarden 账户的账号路由。
- SSO、Passkey、交互式 MFA、设备批准和人工登录流程自动化。
- 通过非 TLS HTTP 或忽略 TLS 错误连接生产 Vaultwarden。
- 在 OpenTeam 内部保存或索引完整 Vault。

## 需求

### 需求 1：插件发现与安装

**用户故事：** 作为 OpenTeam 管理员，我希望在插件市场看到 Bitwarden 插件，以便为 Bot 配置 Bitwarden Cloud 或自托管 Vaultwarden。

#### 验收标准

1. WHEN 插件目录生成 THEN System SHALL 发现 `packages/plugins/bitwarden/plugin.json` 并生成稳定 key 为 `bitwarden` 的插件定义。
2. WHEN 用户打开插件详情 THEN System SHALL 显示 Bitwarden/Vaultwarden 的用途、支持的凭据类型、安装步骤和本地部署网络要求。
3. WHEN 用户安装插件 THEN System SHALL 校验必填字段、保存插件运行配置并把 Skill 文件同步到 Bot computer。
4. WHEN 用户重复安装已安装的插件 THEN System SHALL 保持幂等，不创建重复的插件安装记录或重复的环境变量。
5. WHEN 用户卸载插件 THEN System SHALL 删除插件运行配置、停止相关运行时、删除插件 Skill 缓存，并保留与其他插件无关的用户文件。

### 需求 2：Bitwarden 与 Vaultwarden 配置

**用户故事：** 作为管理员，我希望输入 Vaultwarden 地址和专用账号配置，以便 Bot 连接到正确的本地密码库。

#### 验收标准

1. WHEN 用户配置插件 THEN System SHALL 支持 `BW_SERVER_URL`、`BW_EMAIL`、`BW_CLIENT_ID`、`BW_CLIENT_SECRET` 和 `BW_MASTER_PASSWORD` 五个运行字段。
2. WHEN 用户使用 Vaultwarden THEN System SHALL 允许配置自托管 HTTPS URL，不得把 server URL 固定为 Bitwarden Cloud。
3. WHEN 用户输入 server URL THEN System SHALL 要求 HTTPS origin（根 `/` 可选），拒绝 path、query、fragment、用户名、密码或控制字符。
4. WHEN 用户输入 API Key 或主密码 THEN System SHALL 以密码输入控件提交，数据库和公开插件视图不得返回明文。
5. WHEN 用户替换任一运行字段 THEN System SHALL 只更新对应字段，不清空其他已保存字段。
6. WHEN 用户打开已安装插件详情 THEN System SHALL 只显示每个字段的 Saved/Not set 状态，所有 runtime field 均不回显其值；普通 runtime field 可显示为文本配置但不得出现在公开 Bot prompt 中。
7. WHEN 插件安装配置不完整 THEN System SHALL 阻止声称插件已准备好，并在 Bot 使用时报告缺失字段名称而不是尝试空值登录。

### 需求 3：插件运行时字段传递

**用户故事：** 作为插件开发者，我希望把普通配置和 secret 配置都安全地传递给 Skill 进程，以便自托管服务地址不必伪装成密码字段。

#### 验收标准

1. WHEN 插件字段声明 `environment` 且类型为 string THEN SDK SHALL 允许其 `secret` 为 true 或 false，但同一环境变量名仍必须在插件目录内唯一。
2. WHEN server 保存插件运行字段 THEN System SHALL 继续使用现有插件级 process secret 存储和 owner key，不新增凭据表。
3. WHEN Bot 或受支持的子机器人执行普通 Shell THEN System SHALL 注入非 protected runtime fields，并按现有个人、插件、Bot 优先级处理同名变量；Bitwarden protected fields SHALL NOT be included.
4. WHEN Shell 或 Read 返回内容 THEN System SHALL 继续对实际注入的插件 secret 值做流式脱敏；Bitwarden protected fields 只进入服务端授权的 Computer 私有 wrapper 进程。
5. WHEN 插件卸载、更新删除字段或替换字段 THEN System SHALL 清理已经不再声明的环境变量。
6. WHEN 两个插件声明相同的 runtime environment name THEN System SHALL 拒绝后安装或更新，并返回可理解的冲突错误。

### 需求 4：CLI 安装与 Vault 生命周期

**用户故事：** 作为 Bot，我希望在需要时准备 Bitwarden CLI 并完成登录、解锁和同步，以便读取 Vaultwarden 中的最新登录项。

#### 验收标准

1. WHEN Bitwarden Skill 第一次运行 THEN System SHALL 使用插件内脚本安装固定版本的 CLI，并校验下载内容的 SHA-256 或等价完整性证明。
2. WHEN 当前平台不受支持、安装器依赖缺失或校验失败 THEN System SHALL 停止执行，不运行未经验证的 CLI，并返回可操作错误。
3. WHEN Bot 请求读取凭据 THEN Skill SHALL 为本次读取准备 server 配置、Personal API Key 登录、Vault 解锁和同步流程，并将 `BW_CLIENT_ID` / `BW_CLIENT_SECRET` 映射到 CLI 的 `BW_CLIENTID` / `BW_CLIENTSECRET`；System SHALL 明确说明账号邮箱和主密码不能推导或替代 Personal API Key 字段。
4. WHEN 登录或解锁成功 THEN Skill SHALL 只在当前命令链路中使用 session key，不保存到插件包、工作区或 OpenTeam 数据库；CLI 必需的 token 和加密 Vault 只在独立临时目录中存在，结束后清理。
5. WHEN CLI 需要写入配置文件 THEN System SHALL 使用 Bot 私有目录或等价隔离机制，不复用其他 Bot 或桌面用户的 Bitwarden CLI 配置。
6. WHEN 用户轮换 API Key 或主密码 THEN System SHALL 允许覆盖对应字段，并在下一次读取时使用新值。
7. WHEN Vaultwarden 服务不可达或同步失败 THEN System SHALL 不使用旧的、不确定新鲜度的凭据结果冒充本次成功读取。

### 需求 5：登录项查找与直接凭据调用

**用户故事：** 作为 Bot，我希望直接读取匹配网站的登录项，以便完成用户明确要求的网站登录或凭据查询。

#### 验收标准

1. WHEN 用户要求登录某个网站 THEN Bot SHALL 先使用 Skill 查找与目标 hostname 匹配的 Login item。
2. WHEN 存在一个精确 hostname 匹配项 THEN Skill SHALL 选择该 item，并只读取完成任务所需的 username、password 或 TOTP 字段。
3. WHEN 存在多个候选项 THEN Skill SHALL 优先使用完整 URI 和 hostname 匹配；仍然不唯一时只返回不含 secret 的标题、hostname 和 item 标识摘要，要求用户明确选择或报告歧义。
4. WHEN 用户提供显式 item id THEN Skill SHALL 读取该 item，但仍需校验其类型为 Login 并拒绝读取不支持的项目类型。
5. WHEN 用户要求直接查看密码、用户名或 TOTP THEN Bot SHALL 先发送聊天问题 widget 请求读取所选 item 的授权，只有获得批准后才能调用包装脚本并在当前任务中使用结果；credential value SHALL 只进入 Computer 私有环境和浏览器写入路径，不得进入 Bot 模型上下文、聊天消息或终端输出。
6. WHEN 本次任务首次读取某个 item 的字段 THEN 授权 SHALL 在本次任务剩余时间内适用于该 item；更换 item 必须重新授权，已授权 item 的后续字段不重复询问。
7. WHEN `ReadBitwardenCredential` is called without a matching server-side approved `BitwardenReadApproval` and current-task reference THEN Server SHALL reject the call before exposing protected runtime fields, regardless of Skill instructions or model arguments.
8. WHEN the user dismisses, denies, or an approval lease expires THEN Server SHALL reject every credential read for that approval and SHALL NOT inject `BW_*` into a normal Shell. Any approved field value held by Computer SHALL be removed when its credential scope ends or its short TTL expires; a delegated `computerUse` child SHALL be allowed to use the parent scope only while that task scope is active.
9. WHEN 匹配项不存在、类型不支持或字段为空 THEN Skill SHALL 返回明确缺失原因，不猜测其他项目，也不暴露其他搜索结果中的密码。
10. WHEN 读取完成 THEN Skill SHALL 不把完整 item JSON、未请求字段、credential value 或 Vault 中其他项目写入用户消息、模型上下文、插件活动日志或诊断日志。

### 需求 6：浏览器与桌面登录工作流

**用户故事：** 作为用户，我希望 Bot 读取 Vaultwarden 登录项并填入网站，以便不需要手工复制密码和验证码。

#### 验收标准

1. WHEN `computerUse` 子机器人处理网站登录 THEN System SHALL 向其提供 Bitwarden Skill 和支持文件路径。
2. WHEN 页面需要用户名、密码或 TOTP THEN Bot SHALL 读取对应字段并通过现有浏览器安全填充路径输入，而不是把凭据写入普通页面观察结果。
3. WHEN 页面出现多个账号、登录项选择或无法确认目标站点 THEN Bot SHALL 停止自动选择并向用户说明需要的选择。
4. WHEN 页面出现 SSO、Passkey、交互式 MFA、CAPTCHA 或 Bitwarden 本身需要人工批准 THEN Bot SHALL 通过现有人工接管流程处理，不伪造已完成登录。
5. WHEN 浏览器任务完成 THEN System SHALL 继续使用现有浏览器 secret fill 标记和结果脱敏逻辑。
6. WHEN 浏览器任务失败或中断 THEN System SHALL 报告实际状态，不重复提交密码或 OTP，不自动重试可能导致锁定的登录流程。
7. WHEN 用户明确打开并授权 HTTP 登录页面 THEN System SHALL 允许在同一目标 hostname 内使用浏览器安全填充；HTTP 跨站 frame 仍 SHALL 被拒绝。

### 需求 7：只读边界与凭据安全

**用户故事：** 作为管理员，我希望这个插件只用于登录项读取，以便降低 Bot 获得 Vault 写权限后的影响范围。

#### 验收标准

1. WHEN Skill 调用 Bitwarden CLI THEN SHALL 只能通过插件包装脚本提供的 `status`、`search`、`username`、`password` 和 `totp` 读取动作。
2. WHEN Bot 请求创建、修改、删除、移动、共享或导出 Vault 项目 THEN Skill SHALL 拒绝该操作并说明第一版只支持读取。
3. WHEN 插件脚本、CLI stderr、Shell 标题或错误消息产生文本 THEN SHALL 不包含 API Client Secret、主密码、session key 或完整 credential value。
4. WHEN 用户保存插件配置 THEN System SHALL 将 secret 字段保存为插件级 process secret，并且公开设置接口只返回字段是否已配置。
5. WHEN 插件被多个 Bot 使用 THEN System SHALL 遵守现有插件级环境语义，所有 Bot 可使用同一组配置；安装说明 SHALL 要求使用专用账号和最小 Collection 权限。
6. WHEN 用户直接要求输出凭据 THEN Bot SHALL 遵守当前任务授权和现有安全提示；插件本身不得主动追加凭据日志或发送到任务外部目标；现有 Shell 和会话历史可能保存 direct read 值，与 1Password 一致。
7. WHEN server 使用自签名或不受信任 TLS 证书 THEN System SHALL 报告证书错误并指导用户配置受信任证书，不提供跳过证书验证开关。

### 需求 8：错误分类、诊断与恢复

**用户故事：** 作为管理员，我希望知道连接失败发生在哪一步，以便修复 Vaultwarden URL、账号、证书或网络配置。

#### 验收标准

1. WHEN 配置缺失 THEN System SHALL 指出缺失的字段名，不回显字段值。
2. WHEN server URL 无效 THEN System SHALL 在配置保存阶段拒绝，并返回字段级错误。
3. WHEN Bot computer 无法解析或连接 server THEN System SHALL 区分 DNS、连接拒绝、超时和 TLS 错误。
4. WHEN Bitwarden CLI 登录失败 THEN System SHALL 返回 login 阶段错误，仅在 CLI 明确返回时细分交互认证或网络原因，不推断账号存在性。
5. WHEN Vault 解锁失败 THEN System SHALL 返回 unlock 阶段错误，不把未知 CLI 故障断言为主密码错误。
6. WHEN 同步失败 THEN System SHALL 不返回本次未验证的新凭据，并保留可重试的错误状态。
7. WHEN 读取项失败 THEN System SHALL 区分未找到、多个匹配、项目类型不支持、字段缺失和权限不足。
8. WHEN 错误被写入日志或插件活动 THEN System SHALL 只写错误类别、目标 server origin、CLI 退出状态和安全摘要，不写完整 URL path、Authorization header 或凭据值。
9. WHEN 用户修正配置后重试 THEN System SHALL 重新建立隔离的 CLI 流程，不复用可能已经失效的 session key。

### 需求 9：Vaultwarden 兼容性与部署约束

**用户故事：** 作为 Vaultwarden 用户，我希望插件在本地部署、反向代理和容器网络中可预测地工作，以便不需要把密码库迁移到云端。

#### 验收标准

1. WHEN 用户配置 Vaultwarden server URL THEN System SHALL 从 Bot computer 的网络视角验证可达性；`localhost` 不得被解释为宿主机地址。
2. WHEN Vaultwarden 通过反向代理提供 HTTPS THEN System SHALL 使用标准证书校验和 server root URL，不依赖特定代理实现。
3. WHEN 目标 Vaultwarden 版本与固定 CLI 组合通过兼容性测试 THEN System SHALL 在文档中记录该组合和验证日期。
4. WHEN CLI 或 Vaultwarden API 返回未知字段、分页或错误格式 THEN System SHALL 保留安全错误摘要，不把未知响应当作成功凭据。
5. WHEN 目标组合不兼容 THEN System SHALL 在兼容性矩阵中标记失败原因，不通过关闭校验或调用未声明的内部 API 绕过问题。
6. WHEN Bot computer 位于远程容器 THEN 安装文档 SHALL 说明需要为该容器提供 DNS、路由和证书信任链；不得只验证桌面主机可以访问 server。

### 需求 10：文档、测试和发布验收

**用户故事：** 作为维护者，我希望新插件有清晰的安装、测试和兼容性证据，以便后续升级不会破坏现有 1Password 行为。

#### 验收标准

1. WHEN 插件代码提交 THEN SHALL 包含 `plugin.json`、README、Skill、CLI 安装脚本、CLI 包装脚本和图标来源说明。
2. WHEN 插件包生成 THEN SHALL 通过插件 SDK manifest 校验、包生成和 registry 生成。
3. WHEN 运行插件相关测试 THEN SHALL 覆盖字段校验、插件安装/卸载、环境变量传递、CLI 参数脱敏、读取动作白名单和错误分类。
4. WHEN 运行现有 1Password 测试 THEN SHALL 保持原有 Service Account Token 注入、轮换、卸载和 Skill 可见性行为。
5. WHEN 使用 Vaultwarden fixture 或真实测试实例 THEN SHALL 验证配置、登录、解锁、同步、单匹配、多匹配、字段读取、TLS 错误和网络错误。
6. WHEN 最终验收完成 THEN SHALL 在本 Spec 的任务清单和兼容性补充文档中记录命令、结果、限制和未执行项。

## 非功能需求

### 非功能需求 1：性能

1. WHEN 读取单个 Login item THEN System SHALL 在 CLI 已安装且 server 正常的情况下完成一次受控的登录/解锁/同步/读取流程，不重复执行无关的全 Vault 查询。
2. WHEN 多个 Bot 同时读取 THEN System SHALL 不共享可导致 session 混淆的可变配置目录，并且单次失败不会污染另一个 Bot 的进程状态。
3. WHEN 命令输出超过预设限制 THEN System SHALL 截断安全摘要并停止继续读取，不把完整 Vault 导出到模型上下文。

### 非功能需求 2：可靠性

1. CLI 下载、解包和安装必须是原子操作，校验失败不得替换已有可用版本。
2. 任何失败的读取流程都必须可重新开始，不依赖上一次残留 session key 或半写入配置文件。
3. 插件配置更新、环境变量清理和插件卸载必须保持事务性或具备明确的失败恢复路径。
4. 取消 Bot 任务时，CLI 子进程和其子进程组必须被回收。

### 非功能需求 3：安全

1. 主密码、Client Secret、session key 和真实凭据值不得进入插件包、生成 registry、环境示例文件、Git 记录或公开配置响应。
2. SecretRedactor 必须覆盖完整值、跨 chunk 值和命令 stderr；测试必须覆盖包含换行和 UTF-8 字符的 secret。
3. server URL 和 email 虽然可以是非 secret runtime field，但必须与其它 Bitwarden protected runtime field 一样，不得进入普通 Shell、MCP process、主模型系统 prompt 或用户信息；它们只在服务端授权的 Computer 内部 CLI 进程环境中出现。
4. 插件文档必须建议专用 Vaultwarden 用户和最小 Collection 权限，并说明 Bot 读取的凭据只进入 Computer 短时环境变量和浏览器私有写入路径。

### 非功能需求 4：可维护性

1. 新增 Bitwarden CLI 版本必须同时更新版本、完整性值、兼容性说明和 CLI 回放测试。
2. 读取动作必须由包装脚本集中定义，Skill 文本不得散落任意 CLI 写命令。
3. 不得修改 1Password 插件的行为来适配 Bitwarden；共享能力必须通过 SDK 或通用 runtime 层扩展。
4. 未来增加多账户或 MCP connector 时，必须保留当前 Skills 浏览器登录路径或明确记录行为变化。

## 成功定义

- `bitwarden` 插件可以在 Marketplace 安装并配置 Bitwarden Cloud 或 Vaultwarden URL。
- Bot 可以通过 Skill 直接查找和读取 username、password、TOTP。
- `computerUse` 可以使用同一插件完成网页登录，并继续使用浏览器 secret fill 脱敏。
- CLI 只通过受限读取动作暴露，插件不会执行 Vault 写入操作。
- Vaultwarden 真实实例或等价 fixture 的登录、解锁、同步和读取流程通过验收。
- 现有 1Password 相关测试和行为保持不变。
- requirements、design、tasks 和兼容性补充文档可以逐项追踪。

## 实施约定

主密码前后空白原样保留；CLI 输出统一 JSON；网站只按完整 URI/exact hostname 匹配，标题查询显式使用 `--title`；不自动回退域名。只读与脱敏实际边界、默认决策及验证映射见 [design.md](design.md) §8.2 和 [tasks.md](tasks.md)。
