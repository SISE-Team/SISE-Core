"""Reviewed names for TofuCraft candle cake variants absent from both languages."""
from add_registry_names import add

colors = {
    'white': '白色', 'orange': '橙色', 'magenta': '品红色', 'light_blue': '淡蓝色',
    'yellow': '黄色', 'lime': '黄绿色', 'pink': '粉红色', 'gray': '灰色',
    'light_gray': '淡灰色', 'cyan': '青色', 'purple': '紫色', 'blue': '蓝色',
    'brown': '棕色', 'green': '绿色', 'red': '红色', 'black': '黑色',
}
cakes = {
    'soycheese_candle_tart': ('Soycheese Tart', '豆腐奶酪蛋挞'),
    'tofu_cake': ('Tofu Cake', '豆腐蛋糕'),
    'zundatofu_cake': ('Zunda Tofu Cake', '毛豆豆腐蛋糕'),
}
entries = {}
for cake, (en, zh) in cakes.items():
    entries['block.tofucraft.candle_' + cake] = [en + ' with Candle', '插上蜡烛的' + zh]
    for color, chinese in colors.items():
        entries['block.tofucraft.' + color + '_candle_' + cake] = [en + ' with ' + color.replace('_', ' ').title() + ' Candle', '插上' + chinese + '蜡烛的' + zh]
add(entries)
