"""Reviewed missing declared config keys; never invent keys for literal UI text."""
from pathlib import Path
import json
import re
from add_registry_names import add
from build_localization import PROJECT, PACK, signature
from audit_localization import OUT

audit = json.loads((OUT / 'config-text-audit-declared.json').read_text('utf-8'))
baseline = json.loads((OUT / 'baseline.json').read_text('utf-8'))
pairs = {}
unresolved = {}

def metadata(text):
    return text.replace('Default:', '默认值：').replace('Range:', '范围：').replace('Allowed Values:', '可选值：')

def chinese(ns, key):
    path = PACK / 'assets' / ns / 'lang/zh_cn.json'
    data = dict(baseline.get(ns, {}).get('zh_cn', {}))
    if path.exists(): data.update(json.loads(path.read_text('utf-8')))
    return data.get(key)

modules = {
    'activator': '执行模块', 'breaker': '破坏模块', 'detector': '检测模块',
    'distributor': '分配模块', 'dropper': '投掷器模块', 'energydistributor': '能量分配模块',
    'energyoutput': '能量输出模块', 'flinger': '投掷模块', 'placer': '放置模块',
    'player': '玩家模块', 'vacuum': '虚空模块', 'void': '销毁模块',
    'extruder': '造桥模块', 'fluid': '流体模块', 'puller': '抽取模块', 'sender': '发送模块',
}
descriptions = {
    'bagus_lib.config.aprilFeature': '启用愚人节功能。[true / false]',
    'bagus_lib.config.aprilFeatureAlway': '始终启用愚人节功能。[true / false]',
    'bagus_lib.config.playableFirstPerson': '启用第一人称动画；使用玩家模型修改模组时应关闭。[true / false]',
    'bagus_lib.config.shakeCameraForVanilla': '为原版生物启用镜头震动。[true / false]',
    'modularrouters.gui.config.alwaysShowSettings': '始终在模块提示中显示设置，无需按住 Shift。',
    'modularrouters.gui.config.baseTickRate': '路由器在每个方向上的最大流体传输速率（mB/刻）。',
    'modularrouters.gui.config.blockBreakXPDrops': '破坏方块的模块是否在适用时掉落经验，例如采矿。',
    'modularrouters.gui.config.breakerHarvestLevelLimit': '破坏模块和造桥模块 Mk1 是否受合成时所用镐的采掘等级限制。例如用铁镐合成的模块无法破坏黑曜石。',
    'modularrouters.gui.config.breakerParticles': '破坏模块工作时显示粒子效果。',
    'modularrouters.gui.config.dimensionBlacklist': '禁止发送模块 Mk3 跨越、以及禁止玩家模块运行的维度 ID。会检查路由器与玩家所在的维度。支持通配符，例如 somemod:* 可禁用该模组的全部维度。',
    'modularrouters.gui.config.ecoTimeout': '启用节能模式的路由器闲置达到此游戏刻数后进入低功耗模式。',
    'modularrouters.gui.config.extruderPushEntities': '造桥模块 Mk1/2 伸出方块时是否推动实体。',
    'modularrouters.gui.config.extruderSound': '造桥模块 Mk1/2 放置或移除方块时是否播放声音。',
    'modularrouters.gui.config.fePerEnergyUpgrade': '每个能量升级提供的储能容量（FE）。',
    'modularrouters.gui.config.feXferPerEnergyUpgrade': '每个能量升级提供的能量传输速率（FE/刻）。',
    'modularrouters.gui.config.flingerEffects': '投掷模块工作时显示烟雾并播放声音。',
    'modularrouters.gui.config.fluidBaseTransferRate': '路由器在每个方向上的基础流体传输速率（mB/刻）。',
    'modularrouters.gui.config.hardMinTicks': '无论安装多少速度升级，路由器运行间隔都不能低于此游戏刻数。',
    'modularrouters.gui.config.heldRouterShowsCamoRouters': '手持物品路由器时高亮附近伪装的路由器。',
    'modularrouters.gui.config.lowPowerTickRate': '节能模式路由器进入低功耗状态后的运行间隔（游戏刻）。',
    'modularrouters.gui.config.mBperFluidUpgrade': '每个流体传输升级增加的流体传输速率。',
    'modularrouters.gui.config.moduleBindVolume': '绑定容器、设置伪装等模块或升级操作所播放提示音的音量。',
    'modularrouters.gui.config.moduleGuiBackgroundTint': '根据模块物品的颜色为模块界面背景着色。',
    'modularrouters.gui.config.placerParticles': '放置模块工作时显示粒子效果。',
    'modularrouters.gui.config.pullerParticles': '抽取模块工作时显示粒子效果。',
    'modularrouters.gui.config.renderFlyingItems': '在世界中渲染正在传输的物品。显示效果较好，但大量物品可能降低帧率。',
    'modularrouters.gui.config.senderParticles': '发送模块工作时显示粒子效果。',
    'modularrouters.gui.config.ticksPerUpgrade': '每个速度升级减少的路由器运行间隔（游戏刻）。',
    'modularrouters.gui.config.vacuumParticles': '虚空模块工作时显示粒子效果。',
    'text.autoconfig.torohealth.option.enabled': '启用 ToroHealth。',
    'text.autoconfig.torohealth.option.hudOptions.showEntity': '显示实体。',
    'text.autoconfig.torohealth.option.hudOptions.showHUD': '显示 HUD。',
    'tofucraft.config.enable_extra': '强制启用额外功能，例如进入豆腐世界。',
    'tofucraft.config.travelerTofunianPlayBugle': '豆腐旅行者生成时播放号角声。',
    'tofucraft.config.travelerTofunianSpawn': '生成豆腐旅行者。',
}
descriptions.update(json.loads((PROJECT / 'localization/reviewed/config-briefs.json').read_text('utf-8')))
labels = {
    'breakerHarvestLevelLimit': '破坏模块采掘等级限制', 'dimensionBlacklist': '维度黑名单',
    'buttonCorner': '按钮所在角落', 'buttonXOffset': '按钮 X 偏移', 'buttonYOffset': '按钮 Y 偏移',
    'creativeButtonXOffset': '创造模式按钮 X 偏移', 'creativeButtonYOffset': '创造模式按钮 Y 偏移',
    'enableButton': '启用饰品按钮', 'keepCurios': '死亡时保留饰品', 'maxSlotsPerPage': '每页最大槽位数',
    'minimumColumns': '最少列数', 'renderCurios': '渲染饰品', 'slots': '饰品槽位',
}
curios = {
    'buttonCorner': '饰品界面按钮所在的角落。\n可选值：TOP_LEFT（左上）、TOP_RIGHT（右上）、BOTTOM_LEFT（左下）、BOTTOM_RIGHT（右下）',
    'buttonXOffset': '饰品界面按钮的 X 偏移。', 'buttonYOffset': '饰品界面按钮的 Y 偏移。',
    'creativeButtonXOffset': '创造模式饰品界面按钮的 X 偏移。', 'creativeButtonYOffset': '创造模式饰品界面按钮的 Y 偏移。',
    'enableButton': '设为 true 启用饰品界面按钮。',
    'keepCurios': '控制死亡时的饰品保留方式。\nON：死亡时保留饰品。\nDEFAULT：遵循 keepInventory 游戏规则。\nOFF：死亡时掉落饰品。\n可选值：ON、DEFAULT、OFF',
    'maxSlotsPerPage': '饰品菜单每页的最大槽位数。', 'minimumColumns': '饰品菜单的最少列数。',
    'renderCurios': '设为 true 启用饰品渲染。',
    'slots': '需要创建或修改的槽位列表。\n语法说明：https://docs.illusivesoulworks.com/curios/configuration#slot-configuration\n',
}
solar_labels = {'solar_heat': '太阳热能发电机', 'critical_generation_temperature': '发电临界温度',
    'gen': '发电量', 'heat_gain_per_reflector': '每个反射器的热量增益', 'max_temperature': '最高温度',
    'optimal_generation_temperature': '最佳发电温度', 'target_conversion_temperature': '目标转换温度',
    'wind': '大型风力发电机', 'max': '最大发电量', 'min': '最小发电量'}
solar_descriptions = {
    'solar_heat': '太阳热能发电机设置。',
    'critical_generation_temperature': '反射器损坏开始大幅加速时的温度（开尔文）。',
    'gen': '太阳热能发电机能够产生的最大发电量（焦耳/刻）。',
    'heat_gain_per_reflector': '日照充足时，每个反射器每刻提供的热量。',
    'max_temperature': '反射器能够将太阳热能发电机加热到的最高温度（开尔文）。',
    'optimal_generation_temperature': '发电温度系数达到峰值时的温度（开尔文）。',
    'target_conversion_temperature': '冷却剂开始传热时的温度（开尔文）。',
    'wind': '大型风力发电机设置。',
    'max': '大型风力发电机能够产生的最大发电量（焦耳/刻）。',
    'min': '大型风力发电机能够产生的最小发电量（焦耳/刻）。',
}
for key, record in audit.items():
    ns, en = record['namespace'], record['english']
    tooltip = key.endswith('.tooltip')
    base = key.removesuffix('.tooltip')
    field = base.rsplit('.', 1)[-1]
    # NeoForge appends defaults/ranges to comments; preserve their values exactly.
    split = re.search(r'\n\s*(?:Default:|Range:|Allowed Values:)', en)
    tail = metadata(en[split.start():]) if split else ''
    zh = None
    if base in descriptions and tooltip:
        zh = descriptions[base] + tail
    elif base.startswith('gui.curios.config.'):
        zh = (curios.get(field) + tail) if tooltip and field in curios else labels.get(field)
    elif base.startswith('configuration.mekmm.capacity.'):
        machine = base.split('.')[3]
        name = chinese('mekmm', 'block.mekmm.' + machine) or {'item_replicator': '物品复制机', 'wireless_transmitter_station': '无线传输站'}.get(machine)
        if name:
            zh = ('基础储能容量：' + name + tail) if tooltip else name
    elif base.startswith('configuration.mekmm.server.generator.'):
        zh = (solar_descriptions.get(field) + tail) if tooltip and field in solar_descriptions else solar_labels.get(field)
    elif ns == 'sophisticatedstorage' and tooltip:
        name = chinese(ns, base)
        if name and re.search(r'[\u3400-\u9fff]', name):
            zh = name + '设置。'
    elif base.startswith('modularrouters.gui.config.'):
        cost = re.fullmatch(r'([a-z]+)Module([123]?)EnergyCost(Attack)?', field)
        span = re.fullmatch(r'(extruder|fluid|puller|sender|vacuum)([123]?)(Base|Max)Range', field)
        if cost and cost[1] in modules:
            name = modules[cost[1]] + (' Mk' + cost[2] if cost[2] else '')
            operation = '左键攻击' if cost[3] else ('右键操作' if cost[1] == 'activator' else '操作')
            zh = name + '每次' + operation + '消耗的能量（FE）。' + tail if tooltip else name + ('攻击' if cost[3] else '') + '能耗'
        elif span:
            name = modules[span[1]] + (' Mk' + span[2] if span[2] else '')
            scope = '基础范围（不含范围升级）' if span[3] == 'Base' else '最大范围'
            zh = name + scope + '。' + tail if tooltip else name + scope
        elif not tooltip:
            zh = labels.get(field)
    if zh is None:
        unresolved[key] = record
        continue
    assert signature(en) == signature(zh), (key, en, zh)
    pairs[key] = [en, zh, ns]
add(pairs)
(OUT / 'unresolved-config-keys.json').write_text(json.dumps(unresolved, ensure_ascii=False, indent=2), 'utf-8')
print('Unresolved declared config keys:', len(unresolved))
