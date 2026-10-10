# SISE Core

A fresh Minecraft **26.1.2 / NeoForge** foundation for Steelbound Idyll: Starforge Era. Requires Java 25 and NeoForge 26.1.2.114 or later in the 26.1.2 series. MIT licensed; original notices are preserved in [LICENSE](LICENSE).

This first stage includes:

- **11 original progression materials**, with unchanged IDs, textures, stack sizes and English / Simplified Chinese names.
- **Private Space Teleporter**, including its original crafting recipe and placed entrance model. Each player owns a separate dimension, bound to their UUID.
- **Two terrain choices:** a void with a 16 × 16 starting platform, or a grass superflat world. Creation locks the terrain choice. Player builds persist across visits and restarts.
- **Server-controlled access:** owner only, invited members, or public. Return positions persist in the save. Arrival checks avoid blocked positions and hazardous floors.
- **ZIP resource directory indexing** and a **64-particle cap per damage-indicator packet**. Other particle types are unchanged. The index can be disabled with `-Dsise.reload.disableZipIndex=true`.

The source baseline is the local SISE Core `0.4.47-ifgui1` code for Minecraft 1.21.1. This repository retains its material assets and standalone optimization logic, with a new 26.1.2 build and private-space implementation.

## Use

Craft the teleporter with seven obsidian, one eye of ender and one diamond:

```text
Obsidian  Eye of Ender  Obsidian
Obsidian  Diamond      Obsidian
Obsidian  Obsidian     Obsidian
```

Right-click to choose and create your space; later uses enter or return. Sneak and right-click a block to place an entrance, or sneak and right-click air to open settings. The settings screen uses the standard Minecraft container interface. Click the access icon to change visitor permissions.

| Command | Action |
| --- | --- |
| `/sise space leave` | Return without needing the item |
| `/sise space settings` | Open settings while holding your teleporter or inside your space |
| `/sise space set_spawn` | Set a safe arrival point inside your own space |
| `/sise space invite <online player>` | Invite a player and enable members-only access |
| `/sise space revoke <UUID>` | Remove a member, including an offline member |

Install the same JAR on the client and server. Space data is stored under `data/sise_core/private_spaces.dat`; world chunks use `dimensions/sise_core/personal_<UUID>/`. Back up the complete save, not only the item or index file.

## Scope

This is a new 26.1.2 port, not a replacement for the complete 1.21.1 integration pack. The 11 materials retain their original role as recipe ingredients. Their pack-wide production chains are not included in the old Core's recipe resources and are not invented here.

Gun talents, the old guide hub, L2 overclock symbols, cleanup tools, advanced terrain / sky editors and mod-specific compatibility patches are outside this first stage. Existing 1.21.1 private-space saves are not automatically converted. Use this build in a 26.1.2 world.

## Build and test

```shell
./gradlew build
./gradlew runGameTestServer
```

Development runs use separate directories beside this project. GameTests cover registration, recipe loading, ownership, persistence codecs, both generators, return stacks, menu safety and ZIP index equivalence. The optional client probe requires `-Dsise.core.verify=true`, an exact `-Dsise.core.fixture=<directory>` and a `SISE_AUTOTEST_ONLY` marker; it must only be used in disposable test instances.

---

## 中文

这是面向 **Minecraft 26.1.2 / NeoForge** 重新建立的 SISE Core。首批提供 **11 种原有进度材料、可用的私有空间传送器、资源包目录索引和伤害粒子数量限制**。

传送器沿用原配方：7 个黑曜石、1 个末影之眼、1 个钻石，摆放见上图。首次右键选择虚空平台或超平坦空间；之后右键进入／返回。潜行右键方块放置入口，潜行右键空气打开设置。空间按玩家 UUID 绑定，入口和物品副本不会创建新空间。支持仅主人、指定成员和公开访问；地形创建后固定，建筑和返回位置会保存。

本版先交付基础物品与空间功能。完整整合包进度链、枪械天赋、清扫工具和旧版高级空间编辑器尚未迁入；不要把它视为旧版 Core 的全部功能或旧存档转换器。客户端与服务端使用同一个 JAR。
