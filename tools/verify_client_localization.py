"""Launch the installed mod set in a disposable, marked localization fixture."""
from pathlib import Path
import hashlib
import json
import re
import shutil
import subprocess
import sys
import uuid
from audit_localization import ROOT, OUT

sys.path.insert(0, str(ROOT / '.work/SlashBlade_2/tools'))
from launch_instance import arguments
from initialize_instance import allowed

fixture = ROOT / '.work/test-sise-localization-client-20261010'
fixture.mkdir(exist_ok=True)
(fixture / 'SISE_AUTOTEST_ONLY').write_text('Disposable localization verification fixture\n', 'utf-8')
for stage in ('startup', 'reload', 'result'):
    previous = fixture / ('localization-' + stage + '.json')
    if previous.exists():
        history = OUT / 'client-history'
        history.mkdir(exist_ok=True)
        shutil.copy2(previous, history / (str(previous.stat().st_mtime_ns) + '-' + previous.name))
        previous.unlink()
mods = fixture / 'mods'
mods.mkdir(exist_ok=True)
for jar in (ROOT / 'mods').glob('*.jar'):
    if not jar.name.startswith('SISE-Core-'):
        shutil.copy2(jar, mods / jar.name)
core = ROOT / '.work/SISE-Core-26/build/libs/SISE-Core-0.1.1+26.1.2.jar'
shutil.copy2(core, mods / core.name)
for folder in ('config', 'tacz', 'CustomSkinLoader'):
    shutil.copytree(ROOT / folder, fixture / folder, dirs_exist_ok=True)
(fixture / 'config/dynamic_fps.json').write_text('{"enabled": false}\n', 'utf-8')
(fixture / 'options.txt').write_text('version:4790\nlang:zh_cn\nguiScale:2\nresourcePacks:[]\nfullscreen:false\nsoundCategory_master:0.0\nrenderDistance:2\nmaxFps:60\npauseOnLostFocus:false\n', 'utf-8')
metadata = json.loads((ROOT / 'project02.json').read_text('utf-8'))
classpath = [str(ROOT / 'libraries' / entry['downloads']['artifact']['path']) for entry in metadata['libraries'] if allowed(entry)]
classpath.append(str(ROOT / 'project02.jar'))
replacements = {'auth_player_name': 'LocalizationProbe', 'auth_uuid': uuid.UUID(bytes=hashlib.md5(b'OfflinePlayer:LocalizationProbe').digest(), version=3).hex,
                'auth_access_token': '0', 'auth_xuid': '', 'clientid': '', 'user_type': 'legacy', 'version_name': 'project02', 'version_type': 'release',
                'game_directory': str(fixture), 'assets_root': str(ROOT / 'assets'), 'assets_index_name': metadata['assetIndex']['id'],
                'natives_directory': str(ROOT / 'natives'), 'library_directory': str(ROOT / 'libraries'), 'launcher_name': 'localization-verification',
                'launcher_version': '1', 'classpath': ';'.join(classpath)}
command = [str(ROOT / '.work/devtools/jdk-25/bin/javaw.exe'), '-Xms1G', '-Xmx6G', '-Dsise.localization.verify=true', '-Dsise.localization.fixture=' + str(fixture)]
command += arguments(metadata['arguments']['jvm'], replacements)
native_temp = fixture / 'jna-tmp'
native_temp.mkdir(exist_ok=True)
command += ['-Djna.tmpdir=' + str(native_temp)]
command += [metadata['mainClass']] + arguments(metadata['arguments']['game'], replacements)
command += ['--width', '960', '--height', '540']
protected = [ROOT / 'project02.jar', ROOT / 'project02.json'] + sorted((ROOT / 'natives').glob('*')) + sorted((ROOT / 'libraries').rglob('*.jar'))
hashes = {str(p.relative_to(ROOT)): hashlib.sha256(p.read_bytes()).hexdigest() for p in protected if p.is_file() and not re.fullmatch(r'jna\d+\.dll(?:\.x)?', p.name)}
(OUT / 'minecraft-hashes-before.json').write_text(json.dumps(hashes, indent=2), 'utf-8')
with (OUT / 'client-launch.log').open('w', encoding='utf-8') as log:
    process = subprocess.Popen(command, cwd=fixture, stdout=log, stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW)
(OUT / 'client-process.json').write_text(json.dumps({'pid': process.pid, 'fixture': str(fixture), 'coreSha256': hashlib.sha256(core.read_bytes()).hexdigest()}, indent=2), 'utf-8')
print(json.dumps({'pid': process.pid, 'fixture': str(fixture), 'protectedFiles': len(hashes)}))
