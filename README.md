# SISE Core

**中文补全、个人空间与进度材料。整合包的共同基础。**

SISE Core 是面向 Minecraft **26.1.2** 的 NeoForge 核心模组。把常用基础功能放在一起，安装后即可使用。

[下载模组](https://github.com/SISE-Team/SISE-Core/releases/tag/v0.1.1%2B26.1.2) · [使用个人空间](#使用个人空间) · [参与贡献](CONTRIBUTING.md) · [报告问题](https://github.com/SISE-Team/SISE-Core/issues/new/choose)

## 你会得到什么

| 功能 | 使用体验 |
| --- | --- |
| 简体中文补全 | 物品、菜单与配置项的汉化随 Core 自动加载。无需另装汉化资源包。 |
| 个人空间 | 每位玩家拥有独立维度，可选择虚空或草地，也可邀请成员或开放访问。 |
| 进度材料 | 提供 11 种材料，供整合包连接配方与进度。 |
| 资源与粒子优化 | 为 ZIP 资源查询建立目录索引，将单个伤害粒子数据包限制为最多 64 个粒子。 |

本版汉化补充 **5,909 条文本，覆盖 134 个命名空间**。这些数据来自目标整合包的 206 个模组 JAR 审核，含内嵌依赖。升级或新增模组后，需要重新核对语言键；硬编码文本也需要对应模组适配。

## 下载与安装

| 项目 | 要求 |
| --- | --- |
| Minecraft | **26.1.2** |
| NeoForge | **26.1.2.114** 或更高的 26.1.2 系列版本 |
| Java | **25** |
| Core | **0.1.1+26.1.2** |
| 开发主分支 | [`26.1.2`](https://github.com/SISE-Team/SISE-Core/tree/26.1.2) |

1. 从[发布页](https://github.com/SISE-Team/SISE-Core/releases/tag/v0.1.1%2B26.1.2)下载 `SISE-Core-0.1.1+26.1.2.jar`。
2. 放入实例的 `mods` 文件夹。同一实例只保留一个 Core 版本。
3. 启动游戏。联机时，客户端与服务端安装相同版本的 Core。

使用汉化时，在游戏语言设置中选择「简体中文」。Core 会在启动和资源重载时自动启用内置汉化包。游戏运行时无需联网下载翻译。

## 使用个人空间

用 **7 个黑曜石、1 个末影之眼、1 个钻石**合成个人空间传送器：

| | | |
| --- | --- | --- |
| 黑曜石 | 末影之眼 | 黑曜石 |
| 黑曜石 | 钻石 | 黑曜石 |
| 黑曜石 | 黑曜石 | 黑曜石 |

| 操作 | 结果 |
| --- | --- |
| 首次右键传送器 | 选择并创建虚空平台或草地超平坦空间。 |
| 创建后右键传送器 | 进入空间，或返回此前的位置。 |
| 潜行并右键方块 | 放置空间入口。 |
| 潜行并在空中右键传送器 | 打开空间设置。 |
| 潜行并右键自己的空间入口 | 打开空间设置。 |
| 点击设置中的访问权限 | 在仅所有者、成员、公开之间切换。 |

空间绑定玩家 UUID，复制传送器不会复制空间。地形在创建时确定；之后的建筑会保留，重启后仍可继续使用。

| 命令 | 用途 |
| --- | --- |
| `/sise space leave` | 离开个人空间。 |
| `/sise space settings` | 打开空间设置。 |
| `/sise space set_spawn` | 在自己的空间中设置到达位置。 |
| `/sise space invite <在线玩家>` | 添加成员。 |
| `/sise space revoke <成员 UUID>` | 移除成员。 |

**备份时请保存整个世界文件夹。** 空间权限与返回位置位于 `data/sise_core/private_spaces.dat`，空间区块位于 `dimensions/sise_core/personal_<无连字符的 UUID>/`。

## 本版的范围

本版完成材料、个人空间、资源优化和汉化补全。旧版的枪械天赋、指南枢纽、L2 超频联动、清理功能及高级地形编辑尚未迁移。材料的生产链由整合包配方决定；旧版 1.21.1 空间没有自动转换工具。

小地图位置与快捷键由整合包实例配置管理。Core 不会在启动时覆盖玩家的按键或小地图设置。

## 从源码构建

准备 JDK 25，检出 `26.1.2` 分支：

```sh
git clone --branch 26.1.2 https://github.com/SISE-Team/SISE-Core.git
cd SISE-Core
```

Windows PowerShell：

```powershell
.\gradlew.bat build
```

Linux / macOS：

```sh
./gradlew build
```

产物位于 `build/libs/`。首次构建需要联网下载依赖。

[开发与验证](docs/DEVELOPMENT.md) · [汉化维护](docs/LOCALIZATION.md)

## 一起完善 Core

发现故障、漏译或更好的使用方式，都可以从[贡献指南](CONTRIBUTING.md)开始。一次改动解决一个清楚的问题，并说明如何验证，让下一位贡献者更容易接手。

## 许可与致谢

代码与原创文档采用 [MIT](LICENSE)。内置第三方汉化保留各自的原协议与署名，其中社区汉化包含 **CC BY-NC-SA 4.0** 内容。详见[许可范围与来源](NOTICE.md)和[内置汉化声明](src/main/resources/META-INF/sise-localization/NOTICE.md)。

感谢 Project01、viking602、CFPA 社区，以及提供原始翻译的模组作者与贡献者。
