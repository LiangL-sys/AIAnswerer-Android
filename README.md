# AI答题助手 (AIAnswerer)

[中文](#中文使用指南) | [English](README_EN.md)

## 中文使用指南

### 应用简介
AI答题助手是一款基于 OCR 与大语言模型的安卓答题工具。通过悬浮窗截图识别题目，并调用 DeepSeek AI 等兼容 OpenAI 接口的模型为你快速给出答案，适用于练习、查缺补漏或自测场景。

<img src="./image/main.png" width="300px"> <img src="./image/ai_setting.jpg" width="300px"> <img src="./image/answerer.jpg" width="300px"> <img src="./image/crop_demo.jpg" width="300px">


### 功能亮点
- 🖼️ 屏幕快速截取：一键截取当前屏幕，自动聚焦题目区域
- 📝 智能文字识别：支持中英文识别，可在提交前编辑校正
- 🤖 AI 实时答题：根据题型生成解析，并自动复制答案
- 📋 批量答题：截图包含多题时一次性返回所有答案
- 💬 悬浮窗操作：无需切换应用即可完成截屏、预览、提交
- 🔒 本地可控：自定义 API Key，随时启停网络请求
- 🌐 中英双语：支持中文和英文界面切换

### 技术栈
| 类别 | 技术 |
|------|------|
| 语言 | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material3 |
| OCR | Google ML Kit (中文+拉丁文) |
| 网络 | OkHttp 4.12.0 |
| 存储 | MMKV + EncryptedSharedPreferences |
| 构建 | Gradle (AGP 8.13.0) |

### 安装与准备
1. 使用 Android 11 及以上系统的设备，并保持网络通畅。
2. 安装提供的 APK 文件；首次安装需按照系统提示允许来自未知来源的应用。
3. 设置 LLM 模型信息。
4. 首次启动时，按照屏幕提示授予悬浮窗、截屏和通知等必要权限。

### 快速上手
1. 参考应用内说明

### 支持的题型
- 选择题：识别题干与选项，标记推荐答案并给出理由
- 填空题：生成精炼答案，适用于多空位题目
- 问答题：提供结构化解答或要点式分析

### 使用小贴士
- 保持截图清晰、居中，避免复杂背景，以提升 OCR 准确率。
- 如需暂停网络请求，可暂时断网或在设置页关闭 AI 回答。
- 答案生成后可再次点击悬浮按钮刷新题目，便于连续练习。

### 常见问题
- **提示缺少权限？** 前往系统设置搜索"悬浮窗""屏幕录制"等选项，手动开启相关权限。
- **识别不准确？** 在确认页手动修正文本，或重新截图后再提交。
- **AI 没有回应？** 检查网络、确认 API Key 有效，并确保 DeepSeek 账户余额充足。

### 待完成
- ~~精准框选答题区域（微信读书等）~~
- 自定义题库/知识库（内部文档、技术文档、网站等）
- github action 自动打包
- ~~优化prompt~~、自定义prompt

### 隐私与免责声明
- 应用会将识别出的文字发送至所选 AI 服务，请避免上传敏感或受限内容。
- DeepSeek API 请求可能产生费用，请留意使用频率。
- 本应用仅用于学习与研究，请遵守考试纪律和法律法规，任何违规使用后果自负。

### 项目结构

```
com.hwb.aianswerer/
├── BaseActivity.kt           # 统一语言配置基类
├── MyApplication.kt          # Application 初始化
├── MainActivity.kt           # 主界面（权限管理、答题设置）
├── FloatingWindowService.kt  # 悬浮窗核心服务
├── ScreenCaptureManager.kt   # 截图管理（MediaProjection）
├── TextRecognitionManager.kt # OCR 文字识别
├── ConfirmTextActivity.kt    # 识别文本确认/编辑
├── ImageCropActivity.kt      # 图片裁剪（四角拖拽）
├── SettingsActivity.kt       # 通用设置
├── ModelSettingsActivity.kt  # API 模型配置
├── AboutActivity.kt          # 关于页面
├── Constants.kt              # 常量与系统提示词
├── api/
│   └── OpenAIClient.kt       # OpenAI 兼容 API 客户端
├── config/
│   └── AppConfig.kt          # 配置管理（MMKV + 加密存储）
├── models/                   # 数据模型
├── ui/
│   ├── components/           # 共享 Compose 组件
│   ├── dialogs/              # 对话框
│   ├── icons/                # 本地图标定义
│   └── theme/                # Material3 主题
└── utils/
    ├── AppLog.kt             # 统一日志工具
    ├── ClipboardUtil.kt      # 剪贴板工具
    ├── ImageCropUtil.kt      # 图片裁剪工具
    └── LanguageUtil.kt       # 语言切换工具
```

### 更新说明

#### v0.6 (联网搜索增强 & 悬浮窗优化)
* **Tavily 联网搜索**
  - 集成 Tavily 搜索引擎 API，单题模式下自动搜索相关资料并注入 LLM 上下文
  - 搜索结果作为答题参考，提升冷门题目的准确率
  - 设置页可配置 Tavily API Key（加密存储）和启用开关
  - 支持测试 Tavily 连接
  - 智能提取搜索关键词：从 OCR 文本中提取题干和选项，过滤 UI 噪音
  - 多题模式自动跳过搜索，避免无效 API 调用
* **悬浮窗交互重构**
  - 悬浮按钮支持自由拖拽移动，可吸附到屏幕左/右边缘
  - 点击截图，拖拽移动，一个按钮两个功能
  - 卡片从按钮正下方出现，不遮挡按钮位置
  - 按钮位置不受卡片显隐影响，窗口宽度变化时按钮稳定
  - 修复悬浮窗遮挡下层应用触摸的问题（WRAP_CONTENT 窗口）
* **悬浮窗外观自定义**
  - 设置页新增悬浮窗外观配置：按钮大小（32~80dp）、按钮透明度、卡片透明度
  - 实时生效，无需重启 Service
* **Bug 修复**
  - 修复第二次截图输出第一次答案的问题（等待 Compose 重组完成 + 清除 ImageReader 旧帧）
* **UI 优化**
  - 设置页支持上下滚动
  - 关于页面更新 GitHub 地址，移除邮箱卡片
* **版本号** v0.0.6

#### v0.5 (代码质量优化)
* **安全增强**
  - API Key 使用 EncryptedSharedPreferences 加密存储，不再明文保存
  - Release 构建移除 HTTP 日志，防止 API Key 泄露到 logcat
  - 添加 OkHttp CertificatePinner 证书固定，防止中间人攻击
  - 即使 Debug 模式也对 Authorization 头脱敏
* **架构优化**
  - 抽取 BaseActivity 统一语言配置，消除 6 处重复代码
  - 悬浮窗 Composable 组件独立文件，Service 职责更清晰
  - 统一协程作用域，修复 CancellationException 被吞噬的问题
* **国际化完善**
  - 所有悬浮窗状态消息支持中英文切换
  - 通知渠道名、剪贴板标签等均使用字符串资源
* **网络增强**
  - 添加网络连接预检，无网络时快速提示
  - API 请求支持自动重试（指数退避）
  - Service 销毁时自动取消进行中的网络请求
* **构建优化**
  - 移除冗余 ML Kit 依赖（-10MB 包体积）
  - 所有依赖版本统一到 Version Catalog
  - 收紧 ProGuard 规则，提升 R8 混淆效果
* **代码质量**
  - 统一日志工具 AppLog，Release 构建静默
  - 消除所有 `e.printStackTrace()` 调用
  - 修复 `savedCropRect!!` 空安全风险
  - 补充核心单元测试（extractJsonPayload、isApiConfigValid）
* **JSON 解析优化**
  - 支持批量答题：截图包含多题时返回所有答案
  - 5 级降级解析策略：直接解析 → 提取修复 → 正则数组 → 正则对象 → 文本提取
  - 修复中文引号导致 JSON 截断的问题
  - 系统提示词优化：强制 AI 填写 answer 字段，不得留空

#### v0.4
* 优化了prompt
* 兼容了GPT-5传回的markdown 格式

#### v0.3
* 加入COR 前裁剪功能，提高题目识别能力

#### v0.2
* 修复release 包无法请求ai api 的问题

#### v0.1
* 初次发版

### License
This project is released under the [MIT License](/LICENSE)
