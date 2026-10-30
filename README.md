# 拾页 ShiYe 1.1.0

> 1.1.0 (versionCode 2) 更新，签名与 1.0.0 相同（证书 SHA-256 `4e81d1cf…b93d26`）。

## 1.1.0 新增
- **编辑模式**：阅读页右上角「⋯」→「编辑原文」→ 纯文本源码编辑器（MD 源码 / HTML 源码），撤销 / 重做（连续输入 700ms 合并，≤200 步，有内存预算）。保存会覆盖原文件，并只保留 **一份** 上一版备份，可在编辑页「恢复上一版」（当前版与备份互换，可反悔）。文档上限 600KB。
- **排版面板**（「⋯」→「排版」，一级栏不放按钮）：字号、行距 1.2–2.2×、段间距 0–1.5em（默认 0.6em），全局记忆；段间距同时作用于阅读页、长图、小红书图文。
- **分享栏四个独立入口**：小红书 / 长图 / Notion / 文件。旧的合并「导出」页与页签已移除。
  - **小红书**：只做分页卡片，仅竖版，导出时二选一 3:4（1080×1440）或 9:16（1080×1920）；封面标题可改；共 7 套风格，一条横向列表，由简到花：留白杂志、极细线框、报纸排版、日系便签、渐变弥散、手账贴纸、撞色波普。每种风格封面与正文版式各不相同（页码在右下 n/N）。
  - **长图**：独立页面，永远只有 **一张** 连续长图（不分页），4 种底色风格。
  - **文件**：原文件分享，MD→`.md`、HTML→`.html`，文件名=标题，经 FileProvider + 系统分享面板。
- **首页**：去掉巨大的「拾页」标题，改为小图标字标 + 问候/日期/篇数 + 胶囊搜索 + 筛选 chips。
- 所有导出共用一个 Canvas/StaticLayout 排版引擎（CJK 两端对齐、加粗/引用/标题/表格/代码/提示块）。

截图：`screenshots/v1.1/`（`xhs/` 各风格×两种比例的封面与正文、`long/` 长图、`contact_*.png` 总览图、`home_before/after.png` 等）。

## 1.0.0 基础说明

把散落在微信、浏览器、文件夹里的 `.md` / `.html` 收进一个干净的本地书库，读、转发、导出。
包名 `com.scarlett.shiye` · minSdk 29 · target/compileSdk 35 · Kotlin 2.0 + Jetpack Compose。

## 功能
1. **阅读**：书库（双列瀑布流 / 列表，搜索、筛选、排序、收藏、重命名、删除）；Markdown 自带渲染器（标题、列表、任务列表、引用、提示块、表格、代码块、图片、Front-matter）；HTML 用 WebView 阅读。
2. **接收分享**：`ACTION_SEND`（text/plain、text/markdown、text/html、任意文件，含多文件）与 `ACTION_VIEW`（md / html）；微信等应用「用其他应用打开 / 分享」选拾页，自动存进书库。网页 ZIP 包会整体解开保留相对资源。
3. **Notion 同步**：写入子页面或数据库条目；图片自动上传（最多 8 张、单张 ≤ 5 MB）；同一篇再次同步会替换上一次的页面；长文本自动拆分、限流重试；Token 用 Android Keystore 加密保存。
4. **一键发小红书**：生成 1242×1656 图文卡片（封面 + 正文页，最多 18 张）→ 直接拉起小红书（未安装则走系统分享面板），文案自动复制到剪贴板。
5. **长图**：1080 宽，超长自动分段（单张 ≤ 12000 px）；可分享、发微信、存到相册 `Pictures/拾页`。
6. 四套全新导出样式：冷白、雾蓝（玻璃面板）、电光（整页蓝封面）、石墨（深色）。

## 设计
冷白 `#F5F7FA` 底 + 唯一强调色电光蓝 `#2F6BFF`；线性矢量图标（vector drawable）；顶栏/底栏/浮动工具条为轻量「液态玻璃」（Haze 实时模糊，API 31+；更低版本退化为高不透明纯色）；跟随系统深色模式。

### 借鉴 Glimpse（glimpse.cards）
借鉴：超大粗体页面标题；圆形白底图标按钮；胶囊搜索框；双列错落卡片 + 右上角书签角标；大圆角 + 柔和阴影；吸顶半透明玻璃顶栏；三步引导页；「分享即收藏」的收件理念；阅读页底部浮动动作坞。
未借鉴：账号 / 云同步 / 订阅体系、iOS 专属组件（Share Extension、Widget、Siri）、AI 摘要与社交功能。

## 构建
```
cd app-src && ./gradlew :app:assembleRelease     # 需联网拉依赖
./gradlew :app:testDebugUnitTest                 # 单测 + Robolectric 截图
```
签名读取 `signing/signing.properties`（权限 600，不进源码、不上传）。`signing/shiye-release.p12` 为拾页专用新密钥，请自行备份；丢失后无法覆盖安装升级。
截图在 `screenshots/`：`ui_*.png` 为 Robolectric 渲染的界面（软件渲染，看不到真实模糊），`card_*` / `long_*` 为导出样张。

## Notion 配置
1. 打开 notion.so/my-integrations，新建「内部集成」，复制 Token。
2. 在 Notion 打开要存放的页面或数据库，右上角「…」→「连接」→ 选刚建的集成。
3. 复制该页面 / 数据库链接。
4. 拾页 → 设置 → Notion 同步：选「子页面」或「数据库条目」，填 Token 和链接，点「保存并测试连接」。
数据库模式默认自动识别标题列；标签列名可在高级项里填（Front-matter 的 `tags` 会写入）。API 版本 2025-09-03。

## 安全与规范（android-skills）
- **edge-to-edge**：`enableEdgeToEdge`（随深浅色重设）、insets 统一处理、`adjustResize`。
- **Intent 安全**：`ImportActivity` 只读 data/extras，绝不转发收到的 Intent；只接受 `content://`；跳转 `MainActivity` 用自建显式 Intent，`OPEN_ID` 额外校验 `[0-9a-f]{16}`；`FileProvider` 不导出。
- **权限**：仅 `INTERNET`；无存储权限，文件选择走 SAF，存相册走 MediaStore。
- **R8**：release 开启 minify + shrinkResources；`allowBackup=false`，禁用明文流量。
- **预测性返回**：`enableOnBackInvokedCallback` + `BackHandler`。
- **ANR**：所有读写、解析、渲染、网络都在后台线程。
- **WebView**：默认关闭 JS（每篇可单独开启并有确认）；禁用 file/content 访问、禁止混合内容、无 JS 接口；内容经 `WebViewAssetLoader` 的 https 虚拟域加载（含路径越界校验），拦截非 https 请求；外链须确认后才交给系统浏览器；可在设置里禁止网页联网。

## 1.1.0 对照 android-skills
- edge-to-edge：编辑器 / 设置页 `imePadding()`，Activity 为 `adjustResize` + `enableEdgeToEdge`。
- intent / 权限安全：FileProvider `exported=false`，只授予单次 `FLAG_GRANT_READ_URI_PERMISSION`，cache-path 限定 `export/`，经 `createChooser` 隐式分享；不申请新权限。
- compose 性能：预览用 `LazyRow` + 缩略图缓存，输入防抖 450ms，渲染在 `Dispatchers.Default`，页面销毁时清缓存。
- R8：`minify + shrinkResources` 保持开启，无反射 / 序列化，无需新增 keep 规则。
- ANR：所有文件读写、位图渲染均不在主线程；超大长图有 24000px 上限 + OOM 降级。
- testing：Robolectric + Compose UI 测试（见下）。

## 1.1.0 已知限制
- 无真机 / 模拟器：小红书直达、微信、MediaStore 保存、WebView 渲染、R8 运行时均未在真机验证。
- Robolectric 截图无真实模糊；衬线 / CJK 字体依赖设备，部分风格在无衬线字体设备上标题会回退为黑体。
- 长图超过 24000px 会截断；HTML 文档导出的是提取后的文本；编辑器上限 600KB。

## 已知限制（1.0）
- HTML 文档同步 Notion / 导出小红书 / 长图时按「提取文本」处理，复杂排版不会保留（阅读时是完整网页）。
- 没有真机 / 模拟器：Notion 实机联网、小红书 / 微信拉起、MediaStore 存图、Keystore、WebView 实际渲染、R8 混淆后的运行均未在设备上验证。已验证：编译、签名、单元测试（Markdown 解析、Notion 请求构造）、Robolectric 界面与导出渲染。
- 小红书 App 版本不同，对「外部分享图片」的接收情况可能不同；无法直达时会走系统分享面板，文案已复制。
- 同步 Notion 为手动单向，不做自动同步。
