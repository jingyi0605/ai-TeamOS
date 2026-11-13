# 任务清单 - Windows 与移动端网络出口及电脑操作界面选择（人话版）

状态：IN_PROGRESS

## 这份文档是干什么的

这份任务清单把 `spec006` 拆成可验证的实现步骤。Windows 网络出口和电脑操作界面是本次实现范围；Android/iOS 只完成可行性评估与边界记录。

每个任务都引用 `requirements.md` 和 `design.md`，完成后立即回写状态。

## 阶段 1：Spec 与现状基线

- [x] 1.1 建立 Spec 目录和主文档
  - 状态：DONE
  - 这一步到底做什么：复制 Spec 模板，建立需求、设计、任务和补充文档入口。
  - 做完你能看到什么：`specs/006-Windows与移动端网络出口及电脑操作界面选择/` 下有完整主文档结构。
  - 先依赖什么：无
  - 开始前先看：`specs/000-Spec规范/Spec模板/`
  - 主要改哪里：当前 Spec 目录
  - 这一步先不做什么：不修改业务代码。
  - 怎么算完成：主文档已创建，并且互相引用。
  - 怎么验证：检查目录结构和文档链接。
  - 对应需求：全部需求
  - 对应设计：`design.md` §1

- [x] 1.2 完成 Android/iOS 可行性评估
  - 状态：DONE
  - 这一步到底做什么：基于现有移动端工程、权限模型和生命周期，记录协议复用、实现前提、限制和下一 Spec 边界。
  - 做完你能看到什么：`docs/20261113-移动端网络出口可行性评估.md`，明确 Android、iOS、前台和后台形态的结论。
  - 先依赖什么：1.1
  - 开始前先看：
    - `requirements.md` 需求 4
    - `design.md` §2.3.4、§6.4
    - `apps/android/README.md`
    - `apps/ios/README.md`
  - 主要改哪里：
    - `specs/006-Windows与移动端网络出口及电脑操作界面选择/docs/20261113-移动端网络出口可行性评估.md`
    - 相关评估引用
  - 这一步先不做什么：不添加 Android/iOS provider 代码。
  - 怎么算完成：
    1. Android 和 iOS 都有协议、权限、生命周期、网络能力和风险结论。
    2. 文档明确哪些结论需要真机验证。
  - 怎么验证：文档走查和现有工程路径核对。
  - 对应需求：`requirements.md` 需求 4
  - 对应设计：`design.md` §2.3.4、§6.4

### 阶段检查

- [x] 1.3 Spec 基线检查
  - 状态：DONE
  - 这一步到底做什么：确认需求、设计、任务和移动端评估已经形成可执行边界。
  - 做完你能看到什么：可以开始代码实现，不会把移动端评估误做成移动端实现。
  - 先依赖什么：1.1、1.2
  - 开始前先看：当前 Spec 全部文件
  - 主要改哪里：Spec 文档
  - 这一步先不做什么：不加新功能。
  - 怎么算完成：
    1. 每条需求都有设计和任务入口。
    2. Windows 实现范围和移动端评估范围清楚。
  - 怎么验证：人工走查。
  - 对应需求：全部需求
  - 对应设计：`design.md` 全文

## 阶段 2：Windows Desktop Provider

- [x] 2.1 开放 Windows Provider 平台能力
  - 状态：DONE
  - 这一步到底做什么：让 `DesktopBrowserNetwork` 在 `win32` 上执行现有 identity、consent 和 provider client 流程。
  - 做完你能看到什么：Windows 的网络共享开关可以显示为支持状态，并能建立 device WebSocket。
  - 先依赖什么：1.3
  - 开始前先看：
    - `requirements.md` 需求 1
    - `design.md` §2.3.1、§3.1、§3.3.3
    - `apps/desktop/src/main/browser-network.ts`
  - 主要改哪里：
    - `apps/desktop/src/main/browser-network.ts`
    - `apps/desktop/test/network/consent.test.ts`
    - `apps/desktop/test/network/recovery.test.ts`
  - 这一步先不做什么：不修改 server protocol，不实现移动端。
  - 怎么算完成：
    1. win32 status 报告 supported。
    2. Windows provider 使用与其他 Desktop 相同的 credential、deployment ID 和 WebSocket protocol。
  - 怎么验证：Desktop network unit tests 和 typecheck。
  - 对应需求：`requirements.md` 需求 1.1、1.2、1.4
  - 对应设计：`design.md` §2.3.1、§3.1、§6.1

- [ ] 2.2 验证 Windows 公共 TCP 出站边界
  - 状态：TODO
  - 这一步到底做什么：验证共享 `dialTarget`、public-only policy、DNS 和 multiplexed stream 在 Windows provider 角色下的行为。
  - 做完你能看到什么：Windows provider 可以提供公共 TCP；私有/local/不支持目标仍被拒绝。
  - 先依赖什么：2.1
  - 开始前先看：
    - `requirements.md` 需求 1
    - `design.md` §2.3.1、§5.1、§7.2
    - `packages/plugin-sdk/src/browser-network/policy.ts`
    - `packages/plugin-sdk/src/browser-network/client.ts`
  - 主要改哪里：
    - `packages/plugin-sdk/test/browser-network/policy.test.ts`
    - `packages/plugin-sdk/test/browser-network/client.test.ts`
    - Windows integration fixture 或 workflow
  - 这一步先不做什么：不允许 Windows 访问私有网络，不扩展 UDP。
  - 怎么算完成：
    1. public-only policy 在 Windows runner 上通过。
    2. relay round-trip、lease expiry 和 route fencing 有证据。
  - 怎么验证：共享协议测试、Windows runner integration test。
  - 对应需求：`requirements.md` 需求 1.3、1.5
  - 对应设计：`design.md` §2.3.1、§2.3.3、§6.1、§6.3

- [ ] 2.3 阶段检查：Windows Provider 主链路
  - 状态：TODO
  - 这一步到底做什么：检查 Windows enrollment、consent、lease、公共 TCP 和断线 failover 是否形成完整链路。
  - 做完你能看到什么：Windows provider 可以被 Computer 使用，并能在断线后切换到下一个来源。
  - 先依赖什么：2.1、2.2
  - 开始前先看：`requirements.md`、`design.md`、`tasks.md`
  - 主要改哪里：本阶段相关文件
  - 这一步先不做什么：不开始 UI 选择器。
  - 怎么算完成：关键链路和已知 Windows 风险都有证据或记录。
  - 怎么验证：Windows 实机/CI 回放、Server browser-network tests。
  - 对应需求：`requirements.md` 需求 1
  - 对应设计：`design.md` §2.3.1、§2.3.3、§7.2、§7.3

## 阶段 3：电脑操作界面手动选择

- [x] 3.1 扩展电脑操作界面出口状态控件
  - 状态：DONE
  - 这一步到底做什么：将当前只读的 `BrowserNetworkIndicator` 扩展为可展开的出口状态和来源列表。
  - 做完你能看到什么：打开 bot computer 时可以看到当前出口、来源顺序、可用状态和手动选择入口。
  - 先依赖什么：1.3
  - 开始前先看：
    - `requirements.md` 需求 2、需求 3
    - `design.md` §2.3.2、§3.1、§3.2.1
    - `apps/desktop/src/renderer/components/outcomego/browser-network-indicator.tsx`
    - `apps/desktop/src/renderer/components/outcomego/vnc-computer.tsx`
  - 主要改哪里：
    - `apps/desktop/src/renderer/components/outcomego/browser-network-indicator.tsx`
    - `apps/desktop/src/renderer/components/outcomego/vnc-computer.tsx`
    - `apps/desktop/src/renderer/lib/i18n-catalog.ts`
  - 这一步先不做什么：不复制 Server 设置页面的排序逻辑，不新建后端状态。
  - 怎么算完成：
    1. 控件使用现有 `BrowserNetworkView`。
    2. 异步刷新不会覆盖新 revision/epoch 的状态。
  - 怎么验证：Desktop renderer test 和手工打开 VNC computer 检查。
  - 对应需求：`requirements.md` 需求 2.1、2.2、2.3、3.1
  - 对应设计：`design.md` §2.3.2、§3.1、§3.2.1

- [x] 3.2 实现手动提升出口并处理冲突
  - 状态：DONE
  - 这一步到底做什么：选择一个可用来源后，将它提升到 `sourceOrder` 第一项并调用已有 update API。
  - 做完你能看到什么：用户在电脑操作界面确认后，活动出口进入 applying，随后显示新的活动来源。
  - 先依赖什么：3.1
  - 开始前先看：
    - `requirements.md` 需求 3
    - `design.md` §2.3.2、§3.3.2、§5.3、§6.2、§6.3
    - `packages/client-core/src/client.ts`
    - `apps/server/src/services/browser-network-service.ts`
  - 主要改哪里：
    - `apps/desktop/src/renderer/components/outcomego/browser-network-indicator.tsx`
    - `apps/desktop/test/browser/browser-network.tsx`
    - 必要时补充 `packages/client-core` 类型或测试
  - 这一步先不做什么：不新增临时 override、不改变 server failover 算法。
  - 怎么算完成：
    1. 提交携带当前 revision。
    2. 目标来源被提升到第一项。
    3. 409 或来源不可用时刷新并提示，不显示假成功。
  - 怎么验证：renderer browser-network test、server revision/fencing tests。
  - 对应需求：`requirements.md` 需求 3.1 至 3.6
  - 对应设计：`design.md` §2.3.2、§3.3.2、§5.3、§6.2、§6.3

- [ ] 3.3 阶段检查：操作界面手动切换
  - 状态：TODO
  - 这一步到底做什么：走通查看、确认、切换、applying、成功、冲突和断线几条界面路径。
  - 做完你能看到什么：电脑操作界面可以安全地切换出口，Server 设置看到相同顺序。
  - 先依赖什么：3.1、3.2
  - 开始前先看：当前 Spec 全部文件
  - 主要改哪里：本阶段相关文件
  - 这一步先不做什么：不追加 Android/iOS provider 实现。
  - 怎么算完成：关键路径有测试，旧连接中断提示明确。
  - 怎么验证：renderer test、Server integration test、人工 VNC 操作。
  - 对应需求：`requirements.md` 需求 2、需求 3
  - 对应设计：`design.md` §2.3.2、§5、§7.2、§7.3

## 阶段 4：移动端评估与最终验收

- [x] 4.1 回写移动端评估结论
  - 状态：DONE
  - 这一步到底做什么：根据 Android/iOS 工程和移动操作系统限制，回写可行、不可行、前台限定和下一步 Spec 建议。
  - 做完你能看到什么：维护者可以据此决定是否启动移动端 Provider Spec。
  - 先依赖什么：1.2
  - 开始前先看：`docs/20261113-移动端网络出口可行性评估.md`
  - 主要改哪里：评估文档、`requirements.md`、`design.md`
  - 这一步先不做什么：不把移动端加入 `sourceOrder`，不修改 Android/iOS 网络代码。
  - 怎么算完成：Android/iOS 结论和验证前提清楚。
  - 怎么验证：文档审查。
  - 对应需求：`requirements.md` 需求 4
  - 对应设计：`design.md` §2.3.4、§6.4

- [ ] 4.2 最终检查点
  - 状态：TODO
  - 这一步到底做什么：确认需求、设计、任务、代码和验证证据逐项对应。
  - 做完你能看到什么：`spec006` 可交付，后续工作边界明确。
  - 先依赖什么：2.3、3.3、4.1
  - 开始前先看：
    - `requirements.md`
    - `design.md`
    - `tasks.md`
    - `docs/`
  - 主要改哪里：当前 Spec 全部文件和相关实现文件
  - 这一步先不做什么：不追加移动端 provider 实现。
  - 怎么算完成：
    1. Windows Provider 通过目标验证。
    2. 电脑操作界面手动选择通过目标验证。
    3. Android/iOS 评估完成并明确后续边界。
  - 怎么验证：完整测试集、Windows integration evidence、人工验收清单。
  - 对应需求：全部需求
  - 对应设计：`design.md` 全文
