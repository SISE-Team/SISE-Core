"""Install the exact verified Core artifact, preserving the previous Core JAR."""
from pathlib import Path
import hashlib
import json
import re
import shutil
from audit_localization import ROOT, OUT
from build_localization import PROJECT

fixture = ROOT / '.work/test-sise-localization-client-20261010'
assert (fixture / 'SISE_AUTOTEST_ONLY').is_file()
result = json.loads((fixture / 'localization-result.json').read_text('utf-8'))
assert result['passed'], result
validation = json.loads((OUT / 'validation.json').read_text('utf-8'))
reports = {}
for stage in ('startup', 'reload'):
    report = json.loads((fixture / ('localization-' + stage + '.json')).read_text('utf-8'))
    assert report['required'] and report['compatible'] and report['language'] == 'zh_cn'
    assert report['checkedTranslations'] == validation['supplementEntries']
    assert not report['mismatches']
    assert all(not value for key, value in report.items() if key.startswith('missing'))
    reports[stage] = report
    shutil.copy2(fixture / ('localization-' + stage + '.json'), OUT / ('client-' + stage + '.json'))
shutil.copy2(fixture / 'localization-result.json', OUT / 'client-result.json')

before = json.loads((OUT / 'minecraft-hashes-before.json').read_text('utf-8'))
# The original launcher sets jna.tmpdir to natives. JNA creates random-named
# extraction files and deletion markers there, then cleans them at JVM exit.
temporary = {name: sha for name, sha in before.items() if re.fullmatch(r'natives[/\\]jna\d+\.dll(?:\.x)?', name)}
if temporary:
    (OUT / 'jna-runtime-temporary-hashes.json').write_text(json.dumps(temporary, indent=2), 'utf-8')
    before = {name: sha for name, sha in before.items() if name not in temporary}
    (OUT / 'minecraft-hashes-before.json').write_text(json.dumps(before, indent=2), 'utf-8')
after = {name: hashlib.sha256((ROOT / name).read_bytes()).hexdigest() for name in before}
assert before == after, 'Original Minecraft files changed'
(OUT / 'minecraft-hashes-after.json').write_text(json.dumps(after, indent=2), 'utf-8')
jar = PROJECT / 'build/libs/SISE-Core-0.1.1+26.1.2.jar'
sha = hashlib.sha256(jar.read_bytes()).hexdigest()
process = json.loads((OUT / 'client-process.json').read_text('utf-8'))
assert process['coreSha256'] == sha, 'Built Core differs from tested Core'
assert hashlib.sha256((fixture / 'mods' / jar.name).read_bytes()).hexdigest() == sha

active = list((ROOT / 'mods').glob('SISE-Core-*.jar'))
assert len(active) == 1 and active[0].name == 'SISE-Core-0.1.0+26.1.2.jar', active
old = active[0]
backup = OUT / 'backup' / old.name
backup.parent.mkdir(exist_ok=True)
old_sha = hashlib.sha256(old.read_bytes()).hexdigest()
if backup.exists():
    assert hashlib.sha256(backup.read_bytes()).hexdigest() == old_sha
else:
    shutil.copy2(old, backup)
target = ROOT / 'mods' / jar.name
assert not target.exists(), target
shutil.copy2(jar, target)
assert hashlib.sha256(target.read_bytes()).hexdigest() == sha
old.unlink()  # The verified identical backup is outside the active mods directory.
assert len(list((ROOT / 'mods').glob('SISE-Core-*.jar'))) == 1
summary = dict(validation)
summary.update({'installedJar': str(target), 'sha256': sha, 'previousCoreBackup': str(backup),
                'previousCoreSha256': old_sha, 'protectedMinecraftFiles': len(after), 'minecraftFilesUnchanged': True,
                'clientStartupPassed': True, 'clientReloadPassed': True,
                'missingRegisteredNames': 0, 'missingDeclaredConfigurationTexts': 0})
(OUT / 'installation.json').write_text(json.dumps(summary, ensure_ascii=False, indent=2), 'utf-8')
report = f'''# Core 全模组汉化补全验证

- 已安装 Core：`{target.name}`，替换前的 Core 已备份。
- 扫描当前实例 {validation['installedModJars']} 个模组 JAR，包含嵌套依赖和 TACZ 枪包的语言资源。
- 审核 {validation['languageNamespacesAudited']} 个语言命名空间；内置补全包覆盖 {validation['supplementNamespaces']} 个命名空间，共 {validation['supplementEntries']} 条文本。
- 非空英文语言键的缺失项、占位符错误均为 0。
- 实际物品、方块、实体、状态效果、属性名称，以及模组已声明的配置翻译键缺失项均为 0。
- 首次启动无需选择资源包，Core 汉化包自动启用；清空玩家资源包选择并重载后仍自动启用，全部补全文本与实际加载值一致。
- 原始 Minecraft JAR、版本元数据、natives 和公共 libraries 共 {len(after)} 个受检文件哈希一致。
- JNA 按原始启动参数自动创建、清理的随机名临时 DLL 和删除标记单独记录，不作为原始原生库计数。

汉化包位于 Core 源码的 `src/main/resources/localization/`，新文本及修订保存在
`localization/overrides/`，新增名称与配置键的英文回退文本保存在
`localization/fallback-en/`。原作者名称、技术标识、单位和有意留空的说明按原用途保留。
资源包覆盖语言键；模组直接生成的无翻译键文字不纳入语言键覆盖统计。

使用游戏的简体中文语言，重启后生效，无需安装独立 ZIP 或手动启用资源包。
翻译来源和原许可证随 JAR 一起保存在 `META-INF/sise-localization/`。

## 产物与证据

- 新 Core SHA-256：`{sha}`
- 旧 Core SHA-256：`{old_sha}`
- `validation.json`：语言键与占位符检查。
- `client-startup.json`、`client-reload.json`、`client-result.json`：真实客户端验证。
- `minecraft-hashes-before.json`、`minecraft-hashes-after.json`：原始文件保护校验。
- `installation.json`：安装位置、版本、备份及哈希。
'''
(OUT / 'REPORT.md').write_text(report, 'utf-8')
print(json.dumps(summary, ensure_ascii=False))
