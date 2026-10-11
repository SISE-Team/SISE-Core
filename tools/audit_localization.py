"""Inventory installed language resources, including nested mod JARs."""
from pathlib import Path
from collections import defaultdict
import io
import json
import re
import zipfile

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / '.work/verification/core-localization-20261010'


def inventory():
    languages = defaultdict(lambda: defaultdict(dict))
    sources = defaultdict(set)
    errors = []
    jars = sorted((ROOT / 'mods').glob('*.jar'))

    def scan(z, label, depth=0):
        for name in z.namelist():
            match = re.fullmatch(r'assets/([^/]+)/lang/(en_us|zh_cn)\.json', name)
            if match:
                namespace, language = match.groups()
                try:
                    data = json.loads(z.read(name).decode('utf-8-sig'))
                    languages[namespace][language].update(data)
                    sources[namespace].add(label)
                except Exception as error:
                    errors.append({'source': label, 'path': name, 'error': str(error)})
            elif depth < 4 and name.endswith('.jar') and 'jarjar/' in name:
                with zipfile.ZipFile(io.BytesIO(z.read(name))) as nested:
                    scan(nested, label + '!' + name, depth + 1)
    for jar in jars:
        with zipfile.ZipFile(jar) as z:
            scan(z, jar.name)
    for path in (ROOT / 'tacz').glob('*/assets/*/lang/*.json'):
        if path.stem in ('en_us', 'zh_cn'):
            namespace = path.parent.parent.name
            languages[namespace][path.stem].update(json.loads(path.read_text('utf-8-sig')))
            sources[namespace].add(str(path.relative_to(ROOT)))
    return languages, sources, errors, jars


if __name__ == '__main__':
    languages, sources, errors, jars = inventory()
    OUT.mkdir(parents=True, exist_ok=True)
    records = {}
    missing = {}
    for namespace, langs in sorted(languages.items()):
        en, zh = langs['en_us'], langs['zh_cn']
        absent = {k: v for k, v in en.items() if k not in zh or not str(zh[k]).strip()}
        english = {k: v for k, v in en.items() if k in zh and v == zh[k] and re.search(r'[A-Za-z]{3}', str(v)) and not re.search(r'[\u4e00-\u9fff]', str(v))}
        records[namespace] = {'english': len(en), 'chinese': len(zh), 'missing': len(absent), 'same_as_english': len(english), 'sources': sorted(sources[namespace])}
        missing[namespace] = {'missing': absent, 'english': english}
    (OUT / 'inventory.json').write_text(json.dumps({'mods': len(jars), 'namespaces': records, 'errors': errors}, ensure_ascii=False, indent=2), 'utf-8')
    (OUT / 'baseline.json').write_text(json.dumps(languages, ensure_ascii=False, indent=2), 'utf-8')
    (OUT / 'missing.json').write_text(json.dumps(missing, ensure_ascii=False, indent=2), 'utf-8')
    print('Mods:', len(jars), 'namespaces:', len(records), 'parse errors:', len(errors))
    print('Missing:', sum(r['missing'] for r in records.values()), 'unchanged English:', sum(r['same_as_english'] for r in records.values()))
    for ns, record in sorted(records.items(), key=lambda item: item[1]['missing'], reverse=True):
        if record['missing'] or record['same_as_english']:
            print(ns, record['english'], record['missing'], record['same_as_english'])
