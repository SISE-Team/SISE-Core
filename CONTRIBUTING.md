# 参与贡献

**从一个清楚的问题开始，交付一个容易验证的改进。**

开发主分支是 **`26.1.2`**。新分支从这里检出，Pull Request 也提交到这里。

## 选择入口

| 你想做什么 | 从这里开始 |
| --- | --- |
| 报告故障 | [报告故障](https://github.com/SISE-Team/SISE-Core/issues/new?template=bug_report.yml)，提供版本和复现步骤。 |
| 补充或修正汉化 | [报告汉化问题](https://github.com/SISE-Team/SISE-Core/issues/new?template=translation.yml)，或阅读[汉化维护](docs/LOCALIZATION.md)后直接提交。 |
| 修正文档或小问题 | 直接提交 Pull Request，无需先开 Issue。 |
| 新增功能或改变现有行为 | 先[说明使用场景](https://github.com/SISE-Team/SISE-Core/issues/new?template=feature_request.md)，讨论范围后再实现。 |

## 把问题说清楚

提供 Minecraft、NeoForge 和 Core 的版本，以及单人或服务端环境。用最少的步骤说明「做了什么」「预期发生什么」「实际发生什么」。与问题有关的其他模组，也请写明名称和版本。

日志请保留报错前后的上下文；界面问题请附截图。发布前删除令牌、私人地址和个人信息。无法复现也可以反馈，请明确哪些情况已经确认。

## 提交一个改动

1. Fork 仓库，从 `26.1.2` 创建工作分支。
2. 完成一个明确的改动，运行与它相关的检查。
3. 向 `SISE-Team/SISE-Core` 的 `26.1.2` 提交 Pull Request。

```sh
git clone --branch 26.1.2 https://github.com/<你的用户名>/SISE-Core.git
cd SISE-Core
git switch -c fix/private-space-return
```

分支名可以使用 `fix/…`、`l10n/…` 或 `docs/…`。提交说明描述实际改动，例如 `fix: restore the player's return position`。避免在功能修改中顺带格式化整个项目。

## 该验证什么

| 改动 | 验证方式 |
| --- | --- |
| 文档 | 检查链接、命令、版本与 GitHub 显示效果。 |
| 汉化 | 运行 `python tools/check_localization.py`，并在对应游戏界面确认文本与排版。 |
| Java、数据或构建配置 | 运行 `build`；涉及游戏行为时运行相关 GameTest。涉及界面、资源加载或跨维度移动时，再做客户端验证。 |

Windows PowerShell：

```powershell
.\gradlew.bat build
.\gradlew.bat runGameTestServer
```

Linux / macOS 使用 `./gradlew`。纯文档改动无需启动游戏。详细环境与隔离测试方法见[开发与验证](docs/DEVELOPMENT.md)。

Pull Request 提交前确认：

- [ ] 目标分支为 `26.1.2`，改动范围集中。
- [ ] 写清改动后的行为、检查结果，以及未验证的部分和原因。
- [ ] 同步更新相关文档和语言文本。
- [ ] 没有提交正式存档、日志、凭据、私人路径或构建产物。
- [ ] 新增资源具有可追溯的来源、署名与许可。

## 共同遵守的约定

**保护玩家存档。** 空间所有权和访问权限由服务端验证。修改持久化数据、注册 ID 或配方时，说明兼容影响与迁移方法。

**在模组范围内实现。** 不修改 Minecraft 本体 JAR、原始原生库或版本元数据。使用独立测试实例和可丢弃存档验证。

**让文字帮助玩家完成操作。** 先说明动作与结果，使用常见名称；相关内容放在一起，同级标题与术语保持一致。实现细节写进开发文档。

**让翻译可以维护。** 保留语言键、注册 ID、格式参数、单位、颜色码和有意留空的内容。汉化源文件与生成资源一起提交，具体流程见[汉化维护](docs/LOCALIZATION.md)。

讨论时针对问题与方案，尊重贡献者。维护者会根据版本兼容、使用场景与验证结果审阅，不承诺固定合并时间。

## 贡献的许可

提交原创代码或原创文档，表示你同意它们按仓库的 [MIT](LICENSE) 协议发布。引入第三方内容时，保留其原协议、署名和来源；第三方汉化不得直接改标为 MIT。详情见 [NOTICE.md](NOTICE.md)。
