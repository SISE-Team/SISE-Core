"""Apply reviewed English-to-Chinese pairs to every identical missing phrase."""
import json
from pathlib import Path
from audit_localization import OUT

PROJECT = Path(__file__).resolve().parents[1]

def apply(pairs):
    remaining = json.loads((OUT / 'remaining.json').read_text('utf-8'))
    directory = PROJECT / 'localization/overrides'
    directory.mkdir(parents=True, exist_ok=True)
    changed = 0
    for ns, entries in remaining.items():
        target = directory / (ns + '.json')
        result = json.loads(target.read_text('utf-8')) if target.exists() else {}
        for key, en in entries.items():
            if en in pairs:
                result[key] = pairs[en]
                changed += 1
        if result:
            target.write_text(json.dumps(dict(sorted(result.items())), ensure_ascii=False, indent=2) + '\n', 'utf-8')
    print('Applied', changed, 'reviewed translations')

if __name__ == '__main__':
    import sys
    pairs = {}
    for file in sys.argv[1:]:
        for line in Path(file).read_text('utf-8').splitlines():
            if '\t' in line:
                en, zh = line.split('\t', 1)
                pairs[en.replace('\\n', '\n')] = zh.replace('\\n', '\n')
    apply(pairs)
