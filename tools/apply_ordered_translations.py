"""Apply a complete reviewed list, asserting the source list size and formats."""
import json
from pathlib import Path
from audit_localization import OUT
from apply_manual_translations import apply
from build_localization import signature

def ordered(namespace, translations):
    remaining = json.loads((OUT / 'remaining.json').read_text('utf-8'))
    values = list(dict.fromkeys(remaining[namespace].values()))
    assert len(values) == len(translations), (namespace, len(values), len(translations))
    for en, zh in zip(values, translations):
        assert signature(en) == signature(zh), (namespace, en, zh)
    pairs = dict(zip(values, translations))
    review = Path(__file__).resolve().parents[1] / 'localization/reviewed' / (namespace + '.json')
    review.write_text(json.dumps(pairs, ensure_ascii=False, indent=2) + '\n', 'utf-8')
    apply(pairs)

if __name__ == '__main__':
    import sys
    if sys.argv[1] == '--batch':
        for namespace, translations in json.loads(Path(sys.argv[2]).read_text('utf-8')).items():
            ordered(namespace, translations)
    else:
        ordered(sys.argv[1], json.loads(Path(sys.argv[2]).read_text('utf-8')))
