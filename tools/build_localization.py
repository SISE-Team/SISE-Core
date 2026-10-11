"""Assemble the Core-only Simplified Chinese supplement and report coverage."""
from pathlib import Path
from collections import defaultdict
import hashlib
import json
import re
import zipfile
from audit_localization import ROOT, OUT

PROJECT = Path(__file__).resolve().parents[1]
PACK = PROJECT / 'src/main/resources/localization'
OLD = Path('D:/MC/.minecraft/versions/project01/mods')
HAN = re.compile(r'[\u3400-\u9fff]')
FORMAT = re.compile(r'%(?:\d+\$)?[-#+0,(]*\d*(?:\.\d+)?[sdf%]')


def signature(text):
    # Minecraft converts numeric printf formats to %s; preserve the original here.
    formats = FORMAT.findall(text)
    return (sorted(formats) if any('$' in token for token in formats) else formats), sorted(re.findall(r'\{\d+(?:,[^}]+)?\}', text))


def usable(en, zh):
    return isinstance(zh, str) and bool(zh.strip()) and zh != en and bool(HAN.search(zh)) and signature(en) == signature(zh)


def main():
    baseline = json.loads((OUT / 'baseline.json').read_text('utf-8'))
    missing = json.loads((OUT / 'missing.json').read_text('utf-8'))
    additional_en = defaultdict(dict)
    supplements = defaultdict(dict)
    provenance = defaultdict(dict)
    candidates = defaultdict(list)
    dictionary = defaultdict(set)
    for namespace, langs in baseline.items():
        for key, en in langs['en_us'].items():
            zh = langs.get('zh_cn', {}).get(key)
            if isinstance(en, str) and usable(en, zh):
                dictionary[en].add(zh)
    for jar in sorted(OLD.glob('*.jar')):
        with zipfile.ZipFile(jar) as z:
            names = set(z.namelist())
            for name in names:
                match = re.fullmatch(r'(?:localization/)?assets/([^/]+)/lang/zh_cn.json', name)
                if not match or match[1] not in baseline:
                    continue
                ns = match[1]
                try:
                    zh = json.loads(z.read(name).decode('utf-8-sig'))
                    en_name = name.replace('zh_cn.json', 'en_us.json')
                    en = json.loads(z.read(en_name).decode('utf-8-sig')) if en_name in names else {}
                    priority = 20 if jar.name.startswith('SISE-Core') else 10
                    candidates[ns].append((priority, str(jar) + '!' + name, zh, en))
                    for key, value in en.items():
                        if isinstance(value, str) and usable(value, zh.get(key)):
                            dictionary[value].add(zh[key])
                except (ValueError, UnicodeError):
                    pass
    for path in (OUT / 'community').rglob('zh_cn.json'):
        ns = path.parent.parent.name
        if ns not in baseline:
            continue
        rank = 5
        for version, priority in [('26.1', 19), ('1.21', 18), ('1.20', 15), ('1.19', 12), ('1.18', 8)]:
            if '/' + version in path.as_posix():
                rank = priority
                break
        try:
            zh = json.loads(path.read_text('utf-8-sig'))
            candidates[ns].append((rank, 'CFPA:' + str(path.relative_to(OUT / 'community')), zh, {}))
        except ValueError:
            pass
    manual_dir = PROJECT / 'localization/overrides'
    for path in manual_dir.glob('*.json'):
        candidates[path.stem].append((100, 'Core:' + path.name, json.loads(path.read_text('utf-8')), {}))
    remaining = {}
    retained = defaultdict(dict)
    for ns, data in missing.items():
        needed = dict(data['missing'])
        needed.update(data['english'])
        for key, en in needed.items():
            if not isinstance(en, str) or not en.strip() or key.startswith('_'):
                continue
            for rank, source, zh, old_en in sorted(candidates[ns], key=lambda c: (-c[0], c[1])):
                value = zh.get(key)
                reviewed = rank == 100 and isinstance(value, str) and bool(value.strip()) and signature(en) == signature(value)
                if (usable(en, value) or reviewed) and (key not in old_en or old_en[key] == en or rank == 100):
                    supplements[ns][key] = value
                    provenance[ns][key] = source
                    break
            else:
                if len(dictionary[en]) == 1:
                    value = next(iter(dictionary[en]))
                    if usable(en, value):
                        supplements[ns][key] = value
                        provenance[ns][key] = 'identical English phrase in bilingual mod resources'
            # Missing values that are identifiers, units and names still need a key.
            if key not in supplements[ns]:
                technical = (key.endswith('.color') and bool(re.fullmatch(r'[0-9a-fA-F]{6,8}', en))) or not re.search(r'[A-Za-z\u3400-\u9fff]', FORMAT.sub('', en)) or en in ('SHIFT', 'CTRL', 'CMD', 'ALT', 'X', 'Y', 'Z') or (key.startswith('enchantment.level.') and bool(re.fullmatch(r'[IVXLCDM]+', en)))
                if technical:
                    supplements[ns][key] = en
                    retained[ns][key] = en
                    provenance[ns][key] = 'unchanged color, control key, or formatting token'
                else:
                    remaining.setdefault(ns, {})[key] = en
    for path in (PROJECT / 'localization/fallback-en').glob('*.json'):
        ns = path.stem
        en = json.loads(path.read_text('utf-8'))
        zh = json.loads((manual_dir / path.name).read_text('utf-8'))
        additional_en[ns].update(en)
        for key, value in en.items():
            assert key in zh and signature(value) == signature(zh[key]), (ns, key)
            supplements[ns][key] = zh[key]
            provenance[ns][key] = 'Core: missing registered name or declared configuration language key'
    for ns, en in additional_en.items():
        target = PACK / 'assets' / ns / 'lang/en_us.json'
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps(dict(sorted(en.items())), ensure_ascii=False, indent=2) + '\n', 'utf-8')
    PACK.mkdir(parents=True, exist_ok=True)
    for old_file in (PACK / 'assets').glob('*/lang/zh_cn.json'):
        if not supplements.get(old_file.parent.parent.name):
            assert old_file.resolve().is_relative_to(PACK.resolve())
            old_file.unlink()
    for old_file in (PACK / 'assets').glob('*/lang/en_us.json'):
        if old_file.parent.parent.name not in additional_en:
            assert old_file.resolve().is_relative_to(PACK.resolve())
            old_file.unlink()
    (PACK / 'pack.mcmeta').write_text(json.dumps({'pack': {'description': 'SISE Core · 全模组简体中文补全', 'min_format': [84, 0], 'max_format': [84, 0]}}, ensure_ascii=False, indent=2) + '\n', 'utf-8')
    for ns, entries in supplements.items():
        if not entries:
            continue
        target = PACK / 'assets' / ns / 'lang/zh_cn.json'
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps(dict(sorted(entries.items())), ensure_ascii=False, indent=2) + '\n', 'utf-8')
    (OUT / 'remaining.json').write_text(json.dumps(remaining, ensure_ascii=False, indent=2), 'utf-8')
    (OUT / 'provenance.json').write_text(json.dumps(provenance, ensure_ascii=False, indent=2), 'utf-8')
    (OUT / 'retained-technical-values.json').write_text(json.dumps(retained, ensure_ascii=False, indent=2), 'utf-8')
    print('Supplement:', sum(map(len, supplements.values())), 'entries in', sum(bool(x) for x in supplements.values()), 'namespaces')
    print('Remaining:', sum(map(len, remaining.values())))
    for ns, entries in sorted(remaining.items(), key=lambda c: -len(c[1])):
        print(ns, len(entries))


if __name__ == '__main__':
    main()
