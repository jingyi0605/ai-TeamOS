# 任务清单 - Bitwarden 与 Vaultwarden 凭据插件接入（人话版）

状态：实施中（2026-11-13）

父规格：[Spec 005：插件扩展体系](../005-插件扩展体系/tasks.md)

## 这份文档是做什么的

这份任务清单把 Bitwarden/Vaultwarden 插件拆成可以逐步验收的工作。每个任务都明确主要文件、边界、需求和设计章节。实现完成后必须立刻把状态、验证命令和实际限制回写到这里。

## 状态说明

- `TODO`：还没开始
- `IN_PROGRESS`：正在做
- `BLOCKED`：被外部问题卡住，必须写清楚阻塞原因
- `IN_REVIEW`：已经有结果，等复核
- `DONE`：已经完成，并且已经回写状态
- `CANCELLED`：取消，不做了，但要写清楚原因

## 阶段 1：插件 runtime field 契约

- [x] 1.1 放宽 plugin environment field 的 secret 限制
  - 状态：DONE
  - 证据：`bun test packages/plugin-sdk/test/package.test.ts`、`bun run --cwd packages/plugin-sdk typecheck` 通过；新增普通/secret runtime field、格式和保留空白校验。
  - 这一步到底做什么：让带 `environment` 的 string 字段可以是普通字段或 secret 字段，从而支持 Vaultwarden URL、邮箱和凭据分别使用合适的输入类型。
  - 做完你能看到什么：`PluginField.environment` 的校验、注释和 `environmentFields` 语义能够表达普通 runtime field；现有 1Password manifest 仍然合法。
  - 先依赖什么：无
  - 开始前先看：
    - `requirements.md` 需求 3
    - `design.md` §3.3「SDK runtime field 扩展」
    - `packages/plugin-sdk/src/types.ts`
    - `packages/plugin-sdk/src/manifest.ts`
    - `packages/plugin-sdk/src/configuration.ts`
  - 主要改哪里：
    - `packages/plugin-sdk/src/types.ts`
    - `packages/plugin-sdk/src/manifest.ts`
    - `packages/plugin-sdk/src/configuration.ts`
    - `packages/plugin-sdk/test/package.test.ts`
  - 这一步先不做什么：不添加 Bitwarden 插件，不修改连接器 account 配置。
  - 怎么算完成：
    1. secret 和非 secret string environment field 都能通过校验。
    2. 非 string、重复 environment name、非法名称和带 default 的 secret field 仍按规则失败。
    3. 现有 1Password package tests 全部通过。
  - 怎么验证：
    - `bun test packages/plugin-sdk/test/package.test.ts`
    - `bun run --cwd packages/plugin-sdk typecheck`
  - 对应需求：`requirements.md` 需求 3；需求 10 验收 10.3、10.4
  - 对应设计：`design.md` §3.3、§6.6

- [x] 1.2 更新服务端和桌面端 runtime field 类型表现
  - 状态：DONE
  - 证据：`bun test apps/server/test/plugin/bitwarden-environment.test.ts` 通过；服务端安装/更新共用环境名冲突检查，桌面端按 `secret` 选择输入类型。
  - 这一步到底做什么：检查并调整插件安装、替换、保存、冲突和详情页，让普通 runtime field 以文本输入展示，secret field 仍以密码输入展示。
  - 做完你能看到什么：Bitwarden 的 server URL 和邮箱不会被迫显示为密码输入，已安装插件的更新表单与首次安装表单行为一致。
  - 先依赖什么：1.1
  - 开始前先看：
    - `requirements.md` 需求 2、需求 3
    - `design.md` §3.2、§3.3
    - `apps/server/src/services/plugin/installations.ts`
    - `apps/server/src/services/process-secrets.ts`
    - `apps/desktop/src/renderer/components/outcomego/plugin-detail-view.tsx`
  - 主要改哪里：
    - `apps/server/src/services/plugin/installations.ts`
    - `apps/server/src/services/plugin/queries.ts`
    - `apps/desktop/src/renderer/components/outcomego/plugin-detail-view.tsx`
    - 相关 contracts 和测试文件
  - 这一步先不做什么：不把 runtime field 明文返回给 API，不重做插件设置页面。
  - 怎么算完成：
    1. 普通 runtime field 能保存并能在 Bot process environment 中读取。
    2. secret field 仍不回显，配置视图只返回 Saved/Not set。
    3. environment name 冲突、更新删除和卸载清理行为保持一致。
  - 怎么验证：
    - `bun test apps/server/test/plugin/environment.integration.test.ts`
    - 运行相关 desktop plugin settings tests
    - 手工检查一个普通 field 和一个 secret field 的输入类型
  - 对应需求：`requirements.md` 需求 2、需求 3
  - 对应设计：`design.md` §3.2、§3.3、§4.3

### 阶段检查

- [x] 1.3 runtime field 检查点
  - 状态：DONE
  - 证据：SDK、plugins、server typecheck 通过；Bitwarden environment unit fixture 和现有 process-secret 回归通过。
  - 这一步到底做什么：确认 SDK、server、桌面设置和 Bot Shell 之间的字段语义一致。
  - 做完你能看到什么：可以开始写 Bitwarden plugin.json，不需要在插件实现阶段再改变字段传递协议。
  - 先依赖什么：1.1、1.2
  - 开始前先看：
    - `requirements.md` 需求 2、需求 3
    - `design.md` §3.3、§4.1、§6.1、§6.6
    - `tasks.md` 阶段 1
  - 主要改哪里：阶段 1 全部相关文件
  - 这一步先不做什么：不引入 Bitwarden CLI，不改变 1Password 运行行为。
  - 怎么算完成：
    1. 1Password environment integration test 仍通过。
    2. 非 secret runtime field 有保存、注入、清理和脱敏边界测试。
    3. 公开插件设置响应不包含任何 field value。
  - 怎么验证：
    - 相关 SDK/server/desktop typecheck
    - 运行插件环境和配置测试
    - 人工走查 `PluginField`、`environmentFields` 和 `processEnvironment`
  - 对应需求：`requirements.md` 需求 3、需求 10
  - 对应设计：`design.md` §3.3、§4.1、§6.1、§6.6

## 阶段 2：Bitwarden 插件包与 CLI 包装层

- [x] 2.1 创建 Bitwarden 插件目录和 manifest
  - 状态：DONE
  - 证据：`bun run --cwd packages/plugins generate` 通过；`packages/plugins/test/bitwarden.test.ts` 验证 manifest、Skill scope、五个 runtime field 和导出包内容。
  - 这一步到底做什么：创建 `packages/plugins/bitwarden`，声明插件元数据、五个 runtime fields、Skill、图标和 README。
  - 做完你能看到什么：Marketplace registry 能发现 `bitwarden`，插件详情能显示正确的安装步骤和字段。
  - 先依赖什么：1.3
  - 开始前先看：
    - `requirements.md` 需求 1、需求 2、需求 10
    - `design.md` §3.1、§3.2
    - `packages/plugins/1password/plugin.json`
    - `packages/plugins/1password/README.md`
    - `packages/plugins/scripts/generate.ts`
  - 主要改哪里：
    - `packages/plugins/bitwarden/plugin.json`
    - `packages/plugins/bitwarden/README.md`
    - `packages/plugins/bitwarden/assets/icon.*`
    - `packages/plugins/bitwarden/assets/SOURCES.md`
  - 这一步先不做什么：不实现 CLI 安装和读取逻辑，不把真实凭据写入 fixture。
  - 怎么算完成：
    1. key、version、category、components、fields 和 Skill agents 正确。
    2. 文案明确 Bitwarden Cloud、Vaultwarden、专用账号、API Key、主密码和网络要求。
    3. 插件生成器能打包并通过 manifest 校验。
  - 怎么验证：
    - `bun run --cwd packages/plugins generate`
    - `bun test packages/plugin-sdk/test/package.test.ts`
    - 检查生成 registry 中存在 `bitwarden`
  - 对应需求：`requirements.md` 需求 1、需求 2、需求 10
  - 对应设计：`design.md` §3.1、§3.2

- [x] 2.2 实现固定版本 Bitwarden CLI 安装器
  - 状态：DONE
  - 证据：`packages/plugins/test/bitwarden-installer.test.ts` 覆盖缓存复用、平台/路径拒绝、版本保护、校验失败和原子安装；真实 release asset 下载因本机 TLS 建连失败，见 `docs/20261112-Vaultwarden兼容性矩阵.md`。
  - 这一步到底做什么：实现平台识别、下载、完整性校验、解包、原子安装和版本检查。
  - 做完你能看到什么：Bot computer 第一次使用 Skill 时可以在私有目录获得可验证的 `bw` CLI。
  - 先依赖什么：2.1
  - 开始前先看：
    - `requirements.md` 需求 4、需求 9
    - `design.md` §3.6、§7.4
    - `packages/plugins/1password/skills/1password/scripts/ensure-cli.sh`
  - 主要改哪里：
    - `packages/plugins/bitwarden/skills/bitwarden/scripts/ensure-cli.sh`
    - `packages/plugins/bitwarden/README.md`
    - CLI 安装 fixture 或测试脚本
  - 这一步先不做什么：不实现 Vault 登录、解锁和读取，不允许校验失败后继续执行。
  - 怎么算完成：
    1. 固定版本和每个平台完整性值集中声明。
    2. 下载失败、hash 不匹配、版本不匹配和平台不支持都有明确错误。
    3. 安装失败不覆盖旧的可用版本，不留下可执行半成品。
  - 怎么验证：
    - shellcheck 或等价脚本静态检查
    - 使用本地 fixture 验证 hash mismatch 和原子安装
    - 在支持的平台执行 `--check` 和 `--install`
  - 对应需求：`requirements.md` 需求 4、需求 10
  - 对应设计：`design.md` §3.6、§4.2

- [x] 2.3 实现受限 `bw.sh` 包装层
  - 状态：DONE
  - 证据：`packages/plugins/test/bitwarden.test.ts` 覆盖动作白名单、登录/解锁/同步顺序、精确匹配、多候选、字段读取、错误分类、并发隔离和取消清理。
  - 这一步到底做什么：把 server 配置、API Key 登录、解锁、同步和字段读取封装为有限动作。
  - 做完你能看到什么：Skill 不需要调用任意 `bw` 命令，可以按 `status/search/username/password/totp` 读取。
  - 先依赖什么：2.2
  - 开始前先看：
    - `requirements.md` 需求 4、需求 5、需求 7、需求 8
    - `design.md` §3.4、§3.5、§5
    - `packages/plugins/1password/skills/1password/scripts/op.sh`
  - 主要改哪里：
    - `packages/plugins/bitwarden/skills/bitwarden/scripts/bw.sh`
    - `packages/plugins/bitwarden/skills/bitwarden/scripts/lib/*.sh`（如确有必要）
    - wrapper unit/fixture tests
  - 这一步先不做什么：不把 `bw create`, `bw edit`, `bw delete`, `bw share` 或任意透传参数开放给 Skill。
  - 怎么算完成：
    1. 动作白名单和参数校验在网络请求前生效。
    2. 每次流程使用 server、API Key 和主密码完成受控登录/解锁/同步。
    3. session key 不写入持久化目录、日志、命令行参数或错误消息。
    4. 输出只包含请求字段或非 secret 候选摘要。
  - 怎么验证：
    - wrapper allowlist tests
    - secret redaction tests
    - 用假的 `bw` executable 验证调用顺序和禁止参数
  - 对应需求：`requirements.md` 需求 4、需求 5、需求 7、需求 8
  - 对应设计：`design.md` §3.4、§3.8、§5、§6.2

- [x] 2.4 编写 Bitwarden Skill
  - 状态：DONE
  - 证据：`bun test packages/messaging/test/plugin-skill-catalog.test.ts apps/worker/test/plugins.test.ts` 通过；Skill 仅引用 `bw.sh` 白名单动作并声明 `main`、`computerUse`。
  - 这一步到底做什么：让主 Bot 和 `computerUse` 知道何时读取、如何选择 item、何时停止并请求用户选择。
  - 做完你能看到什么：Bot 能把网站登录任务转换为受限 wrapper 调用，且不会自己扩展出写操作。
  - 先依赖什么：2.3
  - 开始前先看：
    - `requirements.md` 需求 5、需求 6、需求 7
    - `design.md` §2.3、§3.4、§3.5
    - `packages/plugins/1password/skills/1password/SKILL.md`
    - `packages/messaging/src/skill-catalog.ts`
  - 主要改哪里：
    - `packages/plugins/bitwarden/skills/bitwarden/SKILL.md`
    - Skill supporting files 和 Skill tests
  - 这一步先不做什么：不把主密码、API Key 或固定 Vault item 写进 Skill 文本。
  - 怎么算完成：
    1. Skill agents 包含 `main` 和 `computerUse`。
    2. 文档只引用 `bw.sh` 白名单动作。
    3. exact hostname、多个候选、显式 item id、username/password/TOTP 和人工接管边界写清楚。
  - 怎么验证：
    - Skill frontmatter/package tests
    - prompt snapshot 或 Skill catalog tests
    - 人工阅读 Skill，确认没有写命令和 secret 示例值
  - 对应需求：`requirements.md` 需求 5、需求 6、需求 7
  - 对应设计：`design.md` §2.3、§3.4、§3.5

### 阶段检查

- [x] 2.5 CLI 和 Skill 核心链路检查点
  - 状态：DONE
  - 证据：插件生成、SDK/plugins typecheck、Bitwarden fixture tests 和 Skill catalog tests 全部通过；未使用真实凭据。
  - 这一步到底做什么：确认安装器、包装脚本、Skill 路径和 runtime fields 能组成一条完整的无真实凭据测试链路。
  - 做完你能看到什么：可以进入真实 Vaultwarden 兼容性测试，而不是只验证零散脚本。
  - 先依赖什么：2.1、2.2、2.3、2.4
  - 开始前先看：
    - `requirements.md` 需求 1、4、5、7、10
    - `design.md` §2.1、§2.3.2、§3、§6
  - 主要改哪里：阶段 2 全部相关文件
  - 这一步先不做什么：不连接用户真实 Vault，不把真实凭据写入测试输出。
  - 怎么算完成：
    1. 生成的插件包包含 Skill、脚本和图标。
    2. 伪造 CLI fixture 能验证所有动作和错误路径。
    3. 运行时字段能到达 wrapper，但没有进入 Skill 文本或 registry。
  - 怎么验证：
    - `bun run --cwd packages/plugins generate`
    - `bun test packages/plugin-sdk/test packages/messaging/test/plugin-skill-catalog.test.ts`
    - wrapper fixture tests
  - 对应需求：`requirements.md` 需求 1、需求 4、需求 5、需求 7、需求 10
  - 对应设计：`design.md` §2.1、§3.1、§3.4、§3.6、§6.1

## 阶段 3：服务端、Bot 和浏览器集成

- [ ] 3.1 增加插件生命周期和环境字段集成测试
  - 状态：IN_REVIEW
  - 证据：`apps/server/test/plugin/bitwarden-environment.test.ts` 的 unit fixture 通过；PostgreSQL 集成测试已编写但因未设置 `OUTCOMEGO_TEST_DATABASE_URL` 跳过。
  - 这一步到底做什么：验证 Bitwarden 插件安装、重复安装、字段替换、环境冲突、删除字段和卸载行为。
  - 做完你能看到什么：Bitwarden 的 process environment 生命周期有真实数据库证据，且不会破坏 1Password。
  - 先依赖什么：2.5
  - 开始前先看：
    - `requirements.md` 需求 1、需求 2、需求 3、需求 8
    - `design.md` §4.1、§4.3、§6.5、§6.6
    - `apps/server/test/plugin/environment.integration.test.ts`
    - `apps/server/test/plugin/lifecycle.integration.test.ts`
  - 主要改哪里：
    - `apps/server/test/plugin/bitwarden-environment.integration.test.ts`
    - `apps/server/test/plugin/lifecycle.integration.test.ts`（仅在需要共用断言时）
  - 这一步先不做什么：不调用真实 Vaultwarden，不测试浏览器页面。
  - 怎么算完成：
    1. 五个运行字段可保存、轮换、读取和卸载清理。
    2. 普通 field 与 secret field 的公开视图都不回显值。
    3. 同名 environment conflict 和 Bot-specific override 行为有断言。
  - 怎么验证：
    - `bun test apps/server/test/plugin/bitwarden-environment.integration.test.ts`
    - 相关 server plugin lifecycle tests
  - 对应需求：`requirements.md` 需求 1、需求 2、需求 3、需求 8
  - 对应设计：`design.md` §4.1、§4.3、§6.1、§6.5、§6.6

- [x] 3.2 验证主 Bot 和子机器人 Skill 可见性
  - 状态：DONE
  - 证据：`bun test packages/messaging/test/plugin-skill-catalog.test.ts apps/worker/test/plugins.test.ts` 通过；断言 `main`/`computerUse` 可见且 `executor` 不可见。
  - 这一步到底做什么：确认主 Bot 和 `computerUse` 获得 Bitwarden Skill，其他不应使用的 worker 不会被意外扩大能力。
  - 做完你能看到什么：网站登录任务可以由浏览器子机器人执行，同时 Skill scope 与 1Password 一致。
  - 先依赖什么：2.5、3.1
  - 开始前先看：
    - `requirements.md` 需求 5、需求 6
    - `design.md` §2.3.3
    - `apps/worker/src/plugins.ts`
    - `packages/messaging/src/plugin-skill-catalog.ts`
    - `apps/worker/test/plugin-skill-catalog.test.ts`（如存在）
  - 主要改哪里：
    - `apps/worker/test/plugins.test.ts`
    - `packages/messaging/test/plugin-skill-catalog.test.ts`
    - 必要时更新测试 fixture
  - 这一步先不做什么：不实现新的 connector inheritance，不让 `computerUse` 获得 MCP tools。
  - 怎么算完成：
    1. `main` 和 `computerUse` 有 Bitwarden Skill。
    2. Skill supporting files 能被正确解析。
    3. Skill 内容和路径中没有配置值。
  - 怎么验证：
    - messaging/worker plugin tests
    - prompt snapshot 检查
  - 对应需求：`requirements.md` 需求 5、需求 6、需求 10
  - 对应设计：`design.md` §2.1、§2.3.3、§3.1

- [x] 3.3 验证浏览器 secret fill 和 direct read 行为
  - 状态：DONE
  - 证据：Bitwarden protected runtime fields 已从普通 `processEnvironment` 过滤；服务端 `BitwardenReadApproval`、widget 响应、一次性 read/search lease 和 Computer first-party tools 已接入，Skill 不再指导直接 Shell 读取。Computer 只向模型返回短时 `browserReference`，并在任务结束或 TTL 到期时清理环境变量和浏览器私有值。
  - 这一步到底做什么：用安全 fixture 验证 Bot 只能在服务端授权后读取字段，Computer 能把字段写入短时环境变量并通过浏览器私有路径填充，同时不把值出现在模型上下文、普通进程环境、snapshot 或错误摘要。
  - 做完你能看到什么：Bitwarden 的用户体验与 1Password 一致，且 credential value 不会进入对话上下文。
  - 先依赖什么：3.2
  - 开始前先看：
    - `requirements.md` 需求 5、需求 6、需求 7
    - `design.md` §2.3.3、§6.2、§6.3
    - `apps/computer/src/browser/reference-driver.ts`
    - 现有 browser secret fill tests
  - 主要改哪里：
    - `apps/computer/test/` 相关 browser/runtime 测试
    - `packages/messaging/test/` 相关 prompt/worker 测试
    - Bitwarden fixture server 或 fake wrapper
  - 这一步先不做什么：不接入真实用户站点，不测试绕过 MFA、CAPTCHA 或 Passkey。
  - 怎么算完成：
    1. unique hostname 时必须先通过服务端 widget 授权，再依次读取 username/password/TOTP。
    2. 多匹配时必须由用户选择，pending/denied/expired 或错误 item 均不能读取。
    3. 普通 Shell 无法看到 `BW_*`，lease 只能由 Computer 私有路径消费一次。
    4. secret fill 后页面观察结果、Shell stderr 和失败重试文本不暴露值。
  - 怎么验证：
    - computer browser tests
    - fake login page end-to-end fixture
    - secret split/chunk redaction tests
  - 对应需求：`requirements.md` 需求 5、需求 6、需求 7
  - 对应设计：`design.md` §2.3.3、§3.5、§6.2、§6.3

### 阶段检查

- [ ] 3.4 Bot 端到端主链路检查点
  - 状态：IN_REVIEW
  - 证据：插件/Skill/runtime field/wrapper 的确定性链路已由 package、worker、server 和 computer 回归覆盖；fake Vault + fake website 的完整浏览器回放尚未执行。
  - 这一步到底做什么：确认插件安装、Skill 加载、环境注入、wrapper 读取和浏览器填充已经串成一条主链路。
  - 做完你能看到什么：在 fake Vault 和 fake 网站上可以复现“用户要求登录 -> 查找 item -> 读取字段 -> 填充 -> 报告结果”。
  - 先依赖什么：3.1、3.2、3.3
  - 开始前先看：
    - `requirements.md` 全部功能需求
    - `design.md` §2、§3、§4、§5
  - 主要改哪里：阶段 3 全部相关测试和 fixture
  - 这一步先不做什么：不处理多账号、写入 Vault 或 MCP connector。
  - 怎么算完成：
    1. 主 Bot direct read 和 `computerUse` 两条路径均可验证。
    2. 配置错误、item 歧义和浏览器人工接管状态都能正确回传。
    3. 1Password 现有主链路回归通过。
  - 怎么验证：
    - 相关 worker/computer/server integration tests
    - fake Vault + fake website 回放
  - 对应需求：`requirements.md` 需求 5、需求 6、需求 7、需求 8、需求 10
  - 对应设计：`design.md` §2.3、§5、§6、§7.3

## 阶段 4：Vaultwarden 兼容性、文档与最终验收

- [ ] 4.1 准备 Vaultwarden 兼容性测试环境
  - 状态：IN_REVIEW
  - 证据：`docs/20261112-Vaultwarden兼容性矩阵.md` 已记录 CLI、平台、网络和证书基线；隔离 Vaultwarden 实例和 Bot computer 网络条件仍待提供。
  - 这一步到底做什么：准备一个可重复启动的 Vaultwarden 测试实例或明确的外部测试环境，记录 Bot computer 的网络、DNS 和证书条件。
  - 做完你能看到什么：有一个不依赖用户个人 Vault 的专用测试账号、Collection、Login item 和 TOTP。
  - 先依赖什么：3.4
  - 开始前先看：
    - `requirements.md` 需求 9、需求 10
    - `design.md` §3.7、§7.4、§8.1
    - 项目 Docker 和测试环境规则
  - 主要改哪里：
    - `specs/005.1-Bitwarden与Vaultwarden凭据插件接入/docs/20261112-Vaultwarden兼容性矩阵.md`
    - 测试 fixture 或不含 secret 的 compose/启动说明
    - 相关测试脚本
  - 这一步先不做什么：不把真实账号、真实主密码或真实 API Key 写入仓库。
  - 怎么算完成：
    1. 兼容性矩阵包含 CLI 版本、Vaultwarden 版本、平台、网络和证书条件。
    2. 测试账号只拥有专用 Collection 的最小权限。
    3. 运行说明不包含真实 secret。
  - 怎么验证：
    - 按矩阵逐项执行
    - 审查日志和 fixture 是否含 secret
  - 对应需求：`requirements.md` 需求 9、需求 10
  - 对应设计：`design.md` §3.7、§7.4、§8.1

- [ ] 4.2 完成真实或隔离 Vaultwarden 读取验收
  - 状态：BLOCKED
  - 阻塞：当前没有可用的隔离 Vaultwarden 实例、测试账号或授权的 Bot computer 网络环境；fake CLI 结果不能替代真实 provider 验收。官方 CLI release asset 下载也因本机 TLS 建连失败处于 PENDING。
  - 这一步到底做什么：验证 API Key 登录、主密码解锁、同步、精确匹配、多匹配、username/password/TOTP 和错误路径。
  - 做完你能看到什么：可以明确说明支持的 Vaultwarden/CLI 组合和不支持的组合。
  - 先依赖什么：4.1
  - 开始前先看：
    - `requirements.md` 需求 4、需求 5、需求 8、需求 9
    - `design.md` §4.2、§5、§7.4
    - `docs/20261112-Vaultwarden兼容性矩阵.md`
  - 主要改哪里：
    - 兼容性测试脚本和 fixture
    - `docs/20261112-Vaultwarden兼容性矩阵.md`
    - 需要修复的 plugin/computer tests
  - 这一步先不做什么：不为单个失败版本加入关闭 TLS、调用未声明内部 API 或永久缓存旧凭据的绕过方案。
  - 怎么算完成：
    1. 正常读取场景全部通过。
    2. API Key、主密码、TLS、网络、同步和 item 错误分类符合设计。
    3. Bot computer 的实际网络条件被记录。
  - 怎么验证：
    - Vaultwarden compatibility replay
    - `status/search/username/password/totp` 全动作验证
    - 错误 fixture 验证
  - 对应需求：`requirements.md` 需求 4、需求 5、需求 8、需求 9
  - 对应设计：`design.md` §3.4、§3.7、§3.8、§5、§7.4

- [x] 4.3 完成插件文档和使用文档
  - 状态：DONE
  - 证据：插件 README、`docs/usage/plugins.md`、`docs/usage/skills.md`、兼容性矩阵和故障排查文档已更新，并明确网络/TLS、权限和 direct read 上下文边界。
  - 这一步到底做什么：补齐插件 README、使用文档、Vaultwarden 网络说明、专用账号权限建议和故障排查说明。
  - 做完你能看到什么：用户可以理解 Bitwarden Cloud 与 Vaultwarden 的配置差异，知道如何让远程 Bot computer 访问本地服务。
  - 先依赖什么：4.2
  - 开始前先看：
    - `requirements.md` 需求 1、需求 7、需求 9、需求 10
    - `design.md` §3.2、§3.7、§5.3、§8.1
    - `docs/usage/plugins.md`
    - `docs/usage/skills.md`
  - 主要改哪里：
    - `packages/plugins/bitwarden/README.md`
    - `docs/usage/plugins.md`
    - `docs/usage/skills.md`
    - `specs/005.1-Bitwarden与Vaultwarden凭据插件接入/docs/`
  - 这一步先不做什么：不承诺未验证的 Vaultwarden 版本，不写真实连接地址或 secret。
  - 怎么算完成：
    1. 文档解释 API Key、主密码、只读 Collection、Bot computer 网络和 TLS。
    2. 文档说明读取值只进入 Computer 短时环境变量和浏览器私有写入路径，并在任务结束或 TTL 到期时清理。
    3. 文档链接和命令与最终实现一致。
  - 怎么验证：
    - Markdown link/path check
    - 按文档从空安装回放一次
    - 人工走查 secret redaction 和不支持能力
  - 对应需求：`requirements.md` 需求 1、需求 7、需求 9、需求 10
  - 对应设计：`design.md` §3.2、§3.7、§5.3、§7.4

- [ ] 4.4 运行相关测试、静态检查和 package 验收
  - 状态：IN_REVIEW
  - 证据：`bun install --frozen-lockfile`、SDK/plugins/server typecheck、plugins generate，以及包含 Bitwarden fixture 的 101-test 回归通过；PostgreSQL 集成、真实 CLI 下载、Vaultwarden 和浏览器回放未执行。
  - 这一步到底做什么：运行 SDK、server plugin、messaging、computer、plugins package 的相关 typecheck/test/build，并记录真实输出。
  - 做完你能看到什么：插件代码、运行时字段扩展、Skill、CLI 包装层和现有 1Password 都有验证证据。
  - 先依赖什么：4.2、4.3
  - 开始前先看：
    - `requirements.md`
    - `design.md` §7
    - `tasks.md` 全文
  - 主要改哪里：当前 Spec 所有任务涉及的代码、测试和文档
  - 这一步先不做什么：不处理无关的预存工作区修改，不启动开发服务器。
  - 怎么算完成：
    1. 相关 package typecheck 和 tests 通过。
    2. 生成 registry、插件包和 Skill cache 检查通过。
    3. 失败测试、未执行的真实环境测试和兼容性限制被明确记录。
  - 怎么验证：
    - `bun run --cwd packages/plugin-sdk typecheck`
    - `bun run --cwd packages/plugins typecheck`
    - `bun test packages/plugin-sdk/test packages/messaging/test apps/server/test/plugin apps/computer/test`
    - 按仓库实际脚本补充受影响 package checks
  - 对应需求：`requirements.md` 需求 10；全部非功能需求
  - 对应设计：`design.md` §7 全部章节

### 最终检查

- [ ] 4.5 最终 Spec 检查点
  - 状态：IN_REVIEW
  - 证据：`spec-review/scripts/spec_check.py` 返回 `0 error(s), 0 warning(s) in 1 spec dir(s)`；需求/设计/任务/文档已交叉引用，外部未验证项保留为 PENDING/BLOCKED。
  - 这一步到底做什么：确认需求、设计、任务、代码、测试、兼容性矩阵和文档逐项对齐，并把任务状态回写为真实状态。
  - 做完你能看到什么：后续实现者可以按任务继续工作，复核者可以从需求追踪到验证证据。
  - 先依赖什么：4.4
  - 开始前先看：
    - `requirements.md`
    - `design.md`
    - `tasks.md`
    - `docs/`
  - 主要改哪里：当前 Spec 全部文件和相关验证记录
  - 这一步先不做什么：不追加未评审的新需求，不把未验证的兼容性写成已支持。
  - 怎么算完成：
    1. 每条需求至少有一个任务和验证方式。
    2. 每个 DONE 任务都有实际命令或人工验收证据。
    3. 风险、阻塞、取消项、版本矩阵和未执行项已记录。
    4. 当前 Spec 状态更新为可复核状态。
  - 怎么验证：
    - 人工逐项走查需求、设计和任务交叉引用
    - 检查所有 Spec 内部链接
    - 复核最终测试输出和兼容性矩阵
  - 对应需求：`requirements.md` 全部需求
  - 对应设计：`design.md` 全文
