"""Check the committed translation pack without an installed game or downloads."""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

PROJECT = Path(__file__).resolve().parents[1]
PACK = PROJECT / "src/main/resources/localization/assets"
SOURCE = PROJECT / "localization"
META = PROJECT / "src/main/resources/META-INF/sise-localization"
FORMAT = re.compile(r"%(?:\d+\$)?[-#+0,(]*\d*(?:\.\d+)?[sdf%]")


def unique_object(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError(f"duplicate JSON key: {key}")
        result[key] = value
    return result


def read_json(path):
    try:
        return json.loads(path.read_text(encoding="utf-8"), object_pairs_hook=unique_object)
    except (OSError, ValueError) as error:
        raise ValueError(f"{path.relative_to(PROJECT)}: {error}") from error


def text_map(data, label):
    if not isinstance(data, dict):
        raise ValueError(f"{label}: expected a JSON object")
    for key, value in data.items():
        if not isinstance(key, str) or not key or not isinstance(value, str):
            raise ValueError(f"{label}: {key!r} must map to a string")
        if "\ufffd" in value:
            raise ValueError(f"{label}: {key} contains a replacement character")
    return data


def namespace_maps(path):
    result = {}
    for file in sorted(path.glob("*.json")):
        result[file.stem] = text_map(read_json(file), file.relative_to(PROJECT))
    return result


def signature(text):
    tokens = FORMAT.findall(text)
    printf = sorted(tokens) if any("$" in token for token in tokens) else tokens
    return printf, sorted(re.findall(r"\{\d+(?:,[^}]+)?\}", text))


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")


def check(sync=False):
    reference = read_json(SOURCE / "english-reference.json")
    if not isinstance(reference, dict) or not reference:
        raise ValueError("english-reference.json: expected namespace objects")
    for namespace, data in reference.items():
        if not re.fullmatch(r"[a-z0-9_.-]+", namespace) or namespace in {".", ".."}:
            raise ValueError(f"invalid namespace: {namespace}")
        text_map(data, f"english-reference.json:{namespace}")
        if not data or any(not value.strip() for value in data.values()):
            raise ValueError(f"english-reference.json:{namespace}: empty reference text")
    overrides = namespace_maps(SOURCE / "overrides")
    fallback = namespace_maps(SOURCE / "fallback-en")
    provenance = read_json(META / "provenance.json")
    namespace_list = read_json(META / "namespaces.json")
    if not isinstance(provenance, dict):
        raise ValueError("provenance.json: expected namespace objects")
    actual = {}
    for file in sorted(PACK.glob("*/lang/zh_cn.json")):
        actual[file.parent.parent.name] = text_map(read_json(file), file.relative_to(PROJECT))
    proposed = {namespace: dict(data) for namespace, data in actual.items()}
    if sync:
        for namespace, english in reference.items():
            target = proposed.setdefault(namespace, {})
            target.update({key: value for key, value in overrides.get(namespace, {}).items() if key in english})
    errors = []
    if set(proposed) != set(reference):
        errors.append("pack namespaces differ from english-reference.json")
    for namespace, english in reference.items():
        chinese = proposed.get(namespace, {})
        if set(chinese) != set(english):
            missing = sorted(set(english) - set(chinese))
            extra = sorted(set(chinese) - set(english))
            errors.append(f"{namespace}: missing keys {missing}; extra keys {extra}")
        sources = text_map(provenance.get(namespace, {}), f"provenance.json:{namespace}")
        for key, original in english.items():
            translation = chinese.get(key)
            if translation is None:
                continue
            if not translation.strip() or signature(original) != signature(translation):
                errors.append(f"{namespace}:{key}: empty translation or changed format arguments")
            if key in overrides.get(namespace, {}) and translation != overrides[namespace][key]:
                errors.append(f"{namespace}:{key}: override differs from pack; run with --sync")
            if not sources.get(key, "").strip():
                errors.append(f"{namespace}:{key}: missing provenance")
    for namespace, data in fallback.items():
        for key, value in data.items():
            if reference.get(namespace, {}).get(key) != value:
                errors.append(f"{namespace}:{key}: fallback differs from English reference")
    packed_en = {}
    for file in sorted(PACK.glob("*/lang/en_us.json")):
        packed_en[file.parent.parent.name] = text_map(read_json(file), file.relative_to(PROJECT))
    if not sync and packed_en != fallback:
        errors.append("English fallback resources differ from source; run with --sync")
    if sync and set(packed_en) - set(fallback):
        errors.append("obsolete English fallback namespaces: remove their pack files explicitly")
    namespaces = sorted(reference)
    if not sync and namespace_list != namespaces:
        errors.append("namespaces.json differs from pack; run with --sync")
    own = PROJECT / "src/main/resources/assets/sise_core/lang"
    english = text_map(read_json(own / "en_us.json"), "Core en_us.json")
    chinese = text_map(read_json(own / "zh_cn.json"), "Core zh_cn.json")
    if set(english) != set(chinese):
        errors.append("Core English and Chinese language keys differ")
    for key in set(english) & set(chinese):
        if signature(english[key]) != signature(chinese[key]) or (english[key].strip() and not chinese[key].strip()):
            errors.append(f"sise_core:{key}: empty translation or changed format arguments")
    if errors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
        return 1
    if sync:
        for namespace, data in proposed.items():
            if data != actual.get(namespace):
                write_json(PACK / namespace / "lang/zh_cn.json", data)
        for namespace, data in fallback.items():
            if data != packed_en.get(namespace):
                write_json(PACK / namespace / "lang/en_us.json", data)
        if namespace_list != namespaces:
            write_json(META / "namespaces.json", namespaces)
    entries = sum(len(data) for data in reference.values())
    print(f"Localization OK: {len(reference)} namespaces, {entries} supplement entries, {len(english)} Core entries.")
    return 0


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--sync", action="store_true", help="apply reviewed overrides and fallback resources before checking")
    args = parser.parse_args()
    try:
        return check(args.sync)
    except ValueError as error:
        print(f"ERROR: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
