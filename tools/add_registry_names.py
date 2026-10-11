"""Add reviewed fallback names for registry keys absent from mod language files."""
from collections import defaultdict
import json
from build_localization import PROJECT, PACK
from audit_localization import OUT

def add(entries):
    directory = PROJECT / 'localization/fallback-en'
    overrides = PROJECT / 'localization/overrides'
    directory.mkdir(exist_ok=True)
    grouped = defaultdict(dict)
    for key, pair in entries.items():
        namespace = pair[2] if len(pair) > 2 else key.split('.')[1]
        grouped[namespace][key] = pair[:2]
    for ns, data in grouped.items():
        en_path = directory / (ns + '.json')
        zh_path = overrides / (ns + '.json')
        en = json.loads(en_path.read_text('utf-8')) if en_path.exists() else {}
        zh = json.loads(zh_path.read_text('utf-8')) if zh_path.exists() else {}
        for key, (english, chinese) in data.items():
            en[key] = english
            zh[key] = chinese
        en_path.write_text(json.dumps(dict(sorted(en.items())), ensure_ascii=False, indent=2) + '\n', 'utf-8')
        zh_path.write_text(json.dumps(dict(sorted(zh.items())), ensure_ascii=False, indent=2) + '\n', 'utf-8')
    print('Added', len(entries), 'fallback registry names')

if __name__ == '__main__':
    import sys
    if sys.argv[1] == '--report':
        report = json.loads(__import__('pathlib').Path(sys.argv[2]).read_text('utf-8'))
        baseline = json.loads((OUT / 'baseline.json').read_text('utf-8'))
        pairs = {}
        unresolved = {}
        missing = {item: key for category, entries in report.items() if category.startswith('missing') for item, key in entries.items()}
        for item, key in missing.items():
            ns = item.split(':')[0]
            en = baseline.get(ns, {}).get('en_us', {})
            zh = dict(baseline.get(ns, {}).get('zh_cn', {}))
            supplement = PACK / 'assets' / ns / 'lang/zh_cn.json'
            if supplement.exists():
                zh.update(json.loads(supplement.read_text('utf-8')))
            candidates = [key.replace('item.', 'block.', 1), key.replace('block.', 'item.', 1), key.replace('item.enderio_evolution.', 'item.enderio_evolution.conduit.'), key.removesuffix('_bush'), key.replace('.wall_', '.'), key.replace('block.tofucraft.', 'block.tofucraft.', 1).replace('wall_', '')]
            for alias in candidates:
                if alias != key and alias in en and alias in zh:
                    pairs[key] = [en[alias], zh[alias], ns]
                    break
            else:
                unresolved[item] = key
        add(pairs)
        (OUT / 'unresolved-registry-names.json').write_text(json.dumps(unresolved, indent=2), 'utf-8')
        print(json.dumps(unresolved, ensure_ascii=False, indent=2))
    else:
        add(json.loads(__import__('pathlib').Path(sys.argv[1]).read_text('utf-8')))
