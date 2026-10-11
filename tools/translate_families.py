"""Reviewed compositional translations for consistently named label families."""
import json
from audit_localization import OUT
from apply_manual_translations import apply

r = json.loads((OUT / 'remaining.json').read_text('utf-8'))
pairs = {}

# Vanilla loot-table labels share an explicit, stable naming scheme.
terms = dict(line.split('=', 1) for line in '''Desert Pyramid=沙漠神殿
Ocean Ruin=海底废墟
Cold=寒冷
Warm=温暖
Trail Ruins=古迹废墟
Common=普通
Rare=稀有
Abandoned Mineshaft=废弃矿井
Ancient City Ice Box=远古城市冰箱
Bastion=堡垒遗迹
Bridge=桥梁
Hoglin Stable=疣猪兽棚
Other=其他
Treasure=宝藏
End City Treasure=末地城宝藏
Jungle Temple=丛林神庙
Chest=箱子
Dispenser=发射器
Nether Bridge=下界要塞
Shipwreck Map=沉船地图
Shipwreck=沉船
Supply=补给
Dungeon=地牢
Stronghold=要塞
Corridor=走廊
Crossing=交叉路口
Library=图书馆
Trial Chamber=试炼密室
Entrance=入口
Intersection=交叉口
Intersection Barrel=交叉口木桶
Reward=奖励
Common Reward=普通奖励
Ominous Reward=不祥奖励
Unique=独特
Rare Reward=稀有奖励
Unique Reward=独特奖励
Underwater Ruin=水下废墟
Big=大型
Small=小型
Village=村庄
Armorer=盔甲匠
Butcher=屠夫
Cartographer=制图师
Desert House=沙漠民居
Fisher=渔夫
Fisherman=渔夫
Fletcher=制箭师
Mason=石匠
Plains House=平原民居
Savanna House=热带草原民居
Shepherd=牧羊人
Snowy House=雪原民居
Taiga House=针叶林民居
Tannery=皮匠屋
Temple=教堂
Toolsmith=工具匠
Weaponsmith=武器匠
Chamber=密室
Water=水
Empty=空
Melee=近战
Ranged=远程
Cat=猫
Morning Gift=晨间赠礼
Fishing=钓鱼
Fish=鱼
Junk=垃圾
Hero Of The Village=村庄英雄赠礼
Cleric=牧师
Farmer=农民
Leatherworker=皮匠
Librarian=图书管理员
Corridor Pot=走廊陶罐
Bogged=沼骸
Shearing=剪毛
Ominous Consumables=不祥消耗品
Ominous Key=不祥钥匙
Consumables=消耗品
Ominous Drops=不祥掉落物
Key=钥匙'''.splitlines())
for value in r.get('bookshelf', {}).values():
    parts = value.split(' - ')
    if all(p in terms for p in parts):
        pairs[value] = ' · '.join(terms[p] for p in parts)

tests = dict(line.split('=', 1) for line in '''Category=分类
Bool=布尔开关
Plain Bool=普通布尔值
World Reload=世界重载
Game Restart=游戏重启
Int=整数
Int Slider=整数滑块
Double=双精度数
Double Slider=双精度数滑块
Percent=百分比
Item=物品
Range=范围
Vec3=三维向量
Vec3i=三维整数向量
Enum=枚举
Dropdown=下拉选项
String=字符串
Regex=正则表达式
Color=颜色
Color No Alpha=无透明度颜色
After Comment=后置注释
List=列表
Dropdown List=下拉列表
Item List=物品列表
Json=JSON
Bean=对象
Record Bean=记录对象
Schema=结构表单
Schema List=结构表单列表
Feature=功能
Sub Feature=子功能
Date=日期
Time=时间
Schema Advanced=高级结构表单
Schema Map=映射结构表单'''.splitlines())
for label, zh in tests.items():
    pairs['Test ' + label] = '测试' + zh
renderers = dict(line.split('=', 1) for line in '''Pathfinding Debug Renderers=寻路调试渲染器
Goal Selector Debug Renderers=目标选择器调试渲染器
Structures Bounds Debug Renderers=结构边界调试渲染器
Neighbor Updates Renderer=相邻方块更新渲染器
Section paths=区段路径
Smart culling=智能剔除
Section visibility=区段可见性
Wireframe=线框
Water debug renderer=水体调试渲染器
Heightmap renderer=高度图渲染器
Collision renderer=碰撞渲染器
Support shape renderer=支撑形状渲染器
Light debug renderer=光照调试渲染器
World generation attempts renderer=世界生成尝试渲染器
Solid faces renderer=实体表面渲染器
Game events renderer=游戏事件渲染器
Sky light sections renderer=天空光照区段渲染器
Breeze debug renderer=旋风人调试渲染器'''.splitlines())
for en, zh in renderers.items():
    pairs['Enabled ' + en] = '已启用' + zh
    pairs['Disabled ' + en] = '已禁用' + zh
    pairs['Enabled ' + en + ' for %s'] = '已为 %s 启用' + zh
    pairs['Disabled ' + en + ' for %s'] = '已为 %s 禁用' + zh
    for word, prefix in [('enabled', '已启用'), ('disabled', '已禁用'), ('shown', '已显示'), ('hidden', '已隐藏')]:
        pairs[en + ' ' + word] = prefix + zh

materials = dict(line.split('=', 1) for line in '''Advanced Alloy=高级合金
Wrought Iron=锻铁
Glue=胶水
Honey=蜂蜜
Latex=乳胶
Liquid Glass=液态玻璃
Molten Ancient Debris=熔融远古残骸
Molten Blazing=熔融烈焰
Molten Copper=熔融铜
Molten Glowstone=熔融荧石
Molten Gold=熔融金
Molten Iron=熔融铁
Molten Netherite=熔融下界合金
Molten Steel=熔融钢
Plastic=塑料
Resin=树脂
Sap=树液
Amethyst=紫水晶
Andesite=安山岩
Argentite=辉银矿
Auricupride=铜金矿
Bauxite=铝土矿
Blackstone=黑石
Carbon=碳
Cassiterite=锡石
Chalcopyrite=黄铜矿
Copper=铜
Crude Oil=原油
Cylindrite=圆柱锡矿
Deepslate=深板岩
Diorite=闪长岩
Dripstone=滴水石
Endstone=末地石
Galena=方铅矿
Glass=玻璃
Gold=金
Granite=花岗岩
Hematite=赤铁矿
Iron=铁
Kaolin=高岭土
Lapis=青金石
Lignite=褐煤
Malachite=孔雀石
Mixed=混合
Netherrack=下界岩
Osmiridium=铱锇矿
Pentlandite=镍黄铁矿
Pyrolite=火成岩
Quartzite=石英岩
Silicon=硅
Steel=钢
Stone=石头
Sulfuric Acid=硫酸
Tuff=凝灰岩
Uraninite=沥青铀矿
Xenothite=磷钇矿'''.splitlines())
suffixes = {'Block': '块', 'Ingot': '锭', 'Nugget': '粒', 'Plate': '板', 'Bucket': '桶', 'Dust': '粉', 'Chunk': '碎块', 'Deposit': '矿团', 'Coil': '线圈', 'Foil': '箔', 'Pebble': '卵石', 'Gem': '晶体', 'Shard': '碎片', 'Gear': '齿轮'}
for en, zh in materials.items():
    pairs[en] = zh
    for suffix, end in suffixes.items():
        pairs[en + ' ' + suffix] = zh + end
for en, zh in {'Bar': '条', 'Blank': '空白', 'Block': '块', 'Foil': '箔', 'Gear': '齿轮', 'Ingot': '锭', 'Nugget': '粒', 'Plate': '板', 'Rod': '杆'}.items():
    pairs[en + ' Mold'] = zh + '模具'
for en, zh in {'Blue': '蓝色', 'Green': '绿色'}.items():
    pairs[en + ' Battery'] = zh + '电池'
for en, zh in {'Eject': '输出', 'Fluid': '流体', 'Luck': '幸运'}.items():
    pairs[en + ' Upgrade'] = zh + '升级'

# Just Dyna Things consistently composes machine names and config labels.
prefixes = {'Advanced Light wand': '高级光照法杖', 'Advanced Time wand': '高级时间法杖', 'BlackHole': '黑洞', 'BlazeGoldAnvil': '烈焰金砧', 'BLazeGoldSolarPanel': '烈焰金太阳能板', 'CelestiGemAnvil': '苍穹晶砧', 'CelestigemSolarPanel': '苍穹晶太阳能板', 'Charged BlazeBloom Goo': '充能烈焰花黏液', 'Charged Primogel Goo': '充能原初黏液', 'Charged ShadowPulse Goo': '充能暗影脉冲黏液', 'Charged VoidShimmer Goo': '充能虚空微光黏液', 'Creative Goo': '创造黏液', 'EclipseAlloyAnvil': '蚀空合金砧', 'EclipseAlloySolarPanel': '蚀空合金太阳能板', 'FerricoreAnvil': '核源铁砧', 'Ferricore Clock': '核源铁时钟', 'FerricoreSolarPanel': '核源铁太阳能板', 'Generic energy goo': '通用能量黏液', 'Light wand': '光照法杖', 'Light Wand Block': '光照法杖方块', 'Picker wand': '拾取法杖', 'Swapper wand': '交换法杖', 'Stabilizer': '稳定器', 'Ticker': '刻加速器', 'PhaseBox': '相位箱'}
labels = dict(line.split('=', 1) for line in '''glowing duration=发光持续时间
allow Fakeplayers=允许模拟玩家
max energy storage=最大储能
max fluid storage=最大储液量
max multiplier=最大倍率
mode max=最大模式
max mode normal=普通模式上限
max mode 2x=双倍模式上限
max mode 4x=四倍模式上限
energy consumed every tick=每刻能量消耗
fluid consumed every tick=每刻流体消耗
keep content when redstone off=红石关闭时保留内容
emit sounds=播放声音
ignore repair delay=忽略修复延迟
biomelist whitelist=生物群系白名单
energy every tick=每刻能量
reduce production at y level=减产 Y 高度
when placed alone=单独放置时
require daytime=需要白天
require to see sky=需要露天
energy usage=能量消耗
counter reducer=计数器减量
tier=等级
sound on change state=状态变化声音
sound on ececution recipe=执行配方声音
require wrench to change face=需要扳手改变朝向
extra sound on execution=执行时额外声音
sound on execution recipe=执行配方声音
tier multiply energy capacity=等级储能倍率
tier multiply energy cost=等级能耗倍率
emit Particles=显示粒子
FE cost=FE 消耗
entity glowing=实体发光
light level change=亮度变化
energy storage=储能
placing light blocks=放置光源方块
sounds=声音
require wrench to change state=需要扳手改变状态
emit sound=播放声音
fluid every tick=每刻流体
tick limiter=刻速率限制'''.splitlines())
for prefix, name in prefixes.items():
    for suffix, label in labels.items():
        pairs[prefix + ' ' + suffix] = name + '：' + label
apply(pairs)
