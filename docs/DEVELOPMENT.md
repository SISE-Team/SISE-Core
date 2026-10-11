# 开发与验证

主分支：**`26.1.2`**。构建环境：**JDK 25**，Gradle Wrapper **9.2.1**。汉化检查使用 Python **3.10 或更高版本**，无需额外 Python 依赖。

## 构建

在仓库根目录执行：

```powershell
python tools/check_localization.py
.\gradlew.bat build
.\gradlew.bat runGameTestServer
```

Linux / macOS 将 `.\gradlew.bat` 替换为 `./gradlew`。首次构建需要联网；依赖缓存齐全后可加 `--offline`。CI 检查汉化并构建模组；游戏行为的验证结果请写进 Pull Request。

产物位于 `build/libs/`。发布 JAR 内包含代码许可、总声明和第三方汉化来源记录。

## 找到相关代码

| 位置 | 职责 |
| --- | --- |
| `src/main/java/local/sise/core/SiseCore.java` | 注册材料、入口与其他基础内容。 |
| `src/main/java/local/sise/core/space/` | 个人空间、权限、传送、菜单与持久化。 |
| `src/main/java/local/sise/core/localization/` | 内置汉化资源包的注册与自动启用。 |
| `src/main/java/local/sise/core/mixin/` | 资源索引与粒子优化。 |
| `src/main/java/local/sise/core/verification/` | GameTest 与显式启用的客户端验证。 |
| `localization/` | 汉化维护源文件与英文核对快照。 |
| `src/main/resources/localization/assets/` | 随模组打包的语言资源。 |

## 使用隔离实例

Gradle 运行目录位于仓库的相邻文件夹：

| 任务 | 目录 |
| --- | --- |
| `runClient` | `../test-sise-core-client` |
| `runServer` | `../test-sise-core-server` |
| `runGameTestServer` | `../test-sise-core-gametest` |

这些实例只用于测试。首次运行服务端时，按其提示处理 EULA。不要指向正在使用的正式实例或正式存档。

GameTest 覆盖材料与配方、传送器绑定、权限与返回位置、菜单约束、两种地形和资源索引。改动相应功能时，运行对应检查。涉及持久化时，再验证创建、进入、返回、重启与重新进入；涉及界面时，确认菜单操作与资源重载后的行为。

## 客户端验证探针

探针默认关闭，只供独立测试实例使用。启用前，在实例目录放置名为 `SISE_AUTOTEST_ONLY` 的标记文件，并传入完全一致的实例绝对路径。

| 探针 | JVM 参数 |
| --- | --- |
| 个人空间与菜单 | `-Dsise.core.verify=true -Dsise.core.fixture=<实例绝对路径>` |
| 重新打开测试存档 | 追加 `-Dsise.core.reopen=true` |
| 汉化与自动加载 | `-Dsise.localization.verify=true -Dsise.localization.fixture=<实例绝对路径>` |

不要同时启用两个探针。探针会执行测试操作、写出结果并退出游戏；绝不在正式实例中使用。结果直接写入实例目录；个人空间探针还会在 `screenshots/` 保存截图。具体参数和输出以 `verification` 包中的实现为准。

## 修改边界

在模组内实现修复，不修改 Minecraft JAR、原始 GLFW / LWJGL DLL、公共游戏资源或版本元数据。空间权限由服务端决定；注册 ID 与持久化数据修改需要考虑现有存档。

语言资源只能替换语言键对应的文本。其他模组中的硬编码文字，需要修改对应模组或提供适配代码。汉化维护流程见 [LOCALIZATION.md](LOCALIZATION.md)。
