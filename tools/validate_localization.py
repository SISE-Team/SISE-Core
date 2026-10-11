"""Validate complete installed English-key coverage and package source notices."""
from pathlib import Path
import hashlib
import json
import re
import tomllib
import zipfile
from build_localization import PROJECT, PACK, signature
from audit_localization import ROOT, OUT

baseline = json.loads((OUT / 'baseline.json').read_text('utf-8'))
provenance = json.loads((OUT / 'provenance.json').read_text('utf-8'))
namespaces = []
missing = []
errors = []
added = 0
for path in sorted((PACK / 'assets').glob('*/lang/zh_cn.json')):
    ns = path.parent.parent.name
    namespaces.append(ns)
    data = json.loads(path.read_text('utf-8'))
    added += len(data)
    for key, zh in data.items():
        fallback = PROJECT / 'localization/fallback-en' / (ns + '.json')
        new_en = json.loads(fallback.read_text('utf-8')) if fallback.exists() else {}
        en = baseline.get(ns, {}).get('en_us', {}).get(key, new_en.get(key))
        if not isinstance(zh, str) or not zh.strip() or '\ufffd' in zh or signature(en) != signature(zh):
            errors.append({'namespace': ns, 'key': key, 'english': en, 'chinese': zh})
for ns, langs in baseline.items():
    supplement = PACK / 'assets' / ns / 'lang/zh_cn.json'
    data = json.loads(supplement.read_text('utf-8')) if supplement.exists() else {}
    merged = dict(langs.get('zh_cn', {})); merged.update(data)
    for key, en in langs['en_us'].items():
        if not isinstance(en, str) or not en.strip() or key.startswith('_'):
            continue
        if key not in merged or not str(merged[key]).strip():
            missing.append(ns + ':' + key)
assert not missing and not errors, (missing, errors)
meta = PROJECT / 'src/main/resources/META-INF/sise-localization'
meta.mkdir(parents=True, exist_ok=True)
(meta / 'namespaces.json').write_text(json.dumps(namespaces, ensure_ascii=False, indent=2) + '\n', 'utf-8')
source_meta = json.loads((OUT / 'community-sources.json').read_text('utf-8'))
notice = '''# SISE Core localization sources

The Java implementation remains MIT licensed. The embedded translation resources
retain their respective original licenses, independently of the Java implementation.

- CFPAOrg/Minecraft-Mod-Language-Package contributors: CC BY-NC-SA 4.0.
  https://github.com/CFPAOrg/Minecraft-Mod-Language-Package
  Pinned revision: ''' + source_meta['commit'] + '''
  This supplement selects translations by matching installed language keys and
  preserves formatting parameters. New and revised translations are stored in
  localization/overrides and localization/reviewed in the Core source repository.
- The user's existing SISE Core 0.4.45-unify1-client translation supplement and
  installed project01 bilingual mod resources supplied additional matching keys.
  Original bundled notices and licenses are preserved under legacy-notices/.
  Additional source JAR attribution is recorded in mod-resource-sources.json,
  with available original license documents under mod-resource-licenses/.

No mod textures, models, executables or Minecraft files are copied into this pack.
The pack is bundled at localization/assets and automatically required by Core.
'''
(meta / 'NOTICE.md').write_text(notice, 'utf-8')
(meta / 'CFPA-CC-BY-NC-SA-4.0.txt').write_bytes((OUT / 'community/LICENSE').read_bytes())
old_jar = Path('D:/MC/.minecraft/versions/project01/mods/SISE-Core-0.4.45-unify1-client.jar')
with zipfile.ZipFile(old_jar) as z:
    for name in z.namelist():
        if name.startswith('META-INF/sise-localization/sources/'):
            target = meta / 'legacy-notices' / name.removeprefix('META-INF/sise-localization/sources/')
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(z.read(name))
mod_sources = {}
for entries in provenance.values():
    for key, source in entries.items():
        if '!assets/' in source and not Path(source.split('!')[0]).name.startswith('SISE-Core'):
            mod_sources.setdefault(source.split('!')[0], []).append(key)
source_records = []
source_notes = json.loads((PROJECT / 'localization/source-notes.json').read_text('utf-8'))
for source, keys in sorted(mod_sources.items()):
    jar = Path(source)
    record = {'sourceJar': jar.name, 'sha256': hashlib.sha256(jar.read_bytes()).hexdigest(), 'languageKeys': sorted(keys)}
    with zipfile.ZipFile(jar) as z:
        for name in ('META-INF/neoforge.mods.toml', 'META-INF/mods.toml'):
            if name in z.namelist():
                metadata = tomllib.loads(z.read(name).decode('utf-8'))
                record['declaredLicense'] = metadata.get('license', '')
                record['mods'] = [{k: m[k] for k in ('modId', 'displayName', 'authors', 'displayURL') if k in m} for m in metadata.get('mods', [])]
                break
        for name in z.namelist():
            if Path(name).suffix.lower() not in {'', '.txt', '.md', '.rst', '.license'}:
                continue
            if name.endswith('/') or not re.fullmatch(r'(?:licen[sc]e|notice|copying)(?:[ _-].*)?(?:\.(?:txt|md|rst))?', Path(name).name, re.I):
                continue
            target = meta / 'mod-resource-licenses' / re.sub(r'[^a-zA-Z0-9_.-]', '_', jar.name) / name
            assert target.resolve().is_relative_to(meta.resolve())
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(z.read(name))
    record.update(source_notes.get(jar.name, {}))
    source_records.append(record)
(meta / 'mod-resource-sources.json').write_text(json.dumps(source_records, ensure_ascii=False, indent=2) + '\n', 'utf-8')
public_provenance = {
    namespace: {
        key: Path(source.split('!', 1)[0]).name + '!' + source.split('!', 1)[1]
        if '!assets/' in source or '!localization/assets/' in source else source
        for key, source in entries.items()
    }
    for namespace, entries in provenance.items()
}
(meta / 'provenance.json').write_text(json.dumps(public_provenance, ensure_ascii=False, indent=2) + '\n', 'utf-8')
summary = {'installedModJars': json.loads((OUT / 'inventory.json').read_text('utf-8'))['mods'], 'languageNamespacesAudited': len(baseline), 'supplementNamespaces': len(namespaces), 'supplementEntries': added,
           'missingEnglishKeys': missing, 'formatErrors': errors, 'communityCommit': source_meta['commit']}
(OUT / 'validation.json').write_text(json.dumps(summary, ensure_ascii=False, indent=2), 'utf-8')
print(json.dumps(summary, ensure_ascii=False))
