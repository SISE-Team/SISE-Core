# 许可范围与来源

## 代码与原创文档

SISE Core 的代码与原创文档采用 [MIT](LICENSE)。保留 Project01 的原始版权声明，并记录 SISE Team 与贡献者的后续贡献。项目由 viking602 维护。

## 内置汉化

内置汉化资源独立于代码许可，保留来源中的协议与署名。MIT 不改变这些资源的授权条件。

| 来源 | 许可与记录 |
| --- | --- |
| CFPAOrg / Minecraft-Mod-Language-Package | CC BY-NC-SA 4.0；保留协议全文与固定版本记录。 |
| 旧版 SISE Core 汉化补充 | 保留 `legacy-notices/` 中的原始来源与许可。 |
| 模组自带双语资源 | 保留各模组声明的许可、作者和可取得的协议文件。 |
| 补充与修订的翻译 | 在汉化源文件及 `provenance.json` 中记录来源，遵循相应资源的许可。 |

来源记录位于 [`src/main/resources/META-INF/sise-localization/`](src/main/resources/META-INF/sise-localization/)，发布的 JAR 中也保留同一目录。英文核对快照 `localization/english-reference.json` 取自对应模组语言资源，适用相同的来源许可。

社区来源固定为 [CFPA 提交 7196729](https://github.com/CFPAOrg/Minecraft-Mod-Language-Package/tree/719672954d9df4ea51877e3d5ce2912148273682)。详细说明见[内置汉化声明](src/main/resources/META-INF/sise-localization/NOTICE.md)。

[Re-Avaritia 的项目声明](https://www.curseforge.com/minecraft/mc-mods/re-avaritia)区分代码的 MIT 与资源的 CC BY-NC-SA 4.0。来源清单保留 JAR 中的代码许可声明，并另行记录资源许可。

## 名称与依赖

Minecraft、NeoForge 及其他模组名称与商标归其各自权利人所有。本仓库没有打包 Minecraft 或其他依赖模组的可执行文件。构建使用的 Gradle Wrapper 保留其原有声明。
