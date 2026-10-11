"""Fetch pinned CFPA translations; only import needed keys with matching formats."""
from pathlib import Path
import concurrent.futures
import json
import re
import urllib.request
from audit_localization import OUT


def download(url):
    request = urllib.request.Request(url, headers={'User-Agent': 'SISE-Core-localization'})
    with urllib.request.urlopen(request, timeout=60) as response:
        return response.read()


if __name__ == '__main__':
    tree = json.loads((OUT / 'cfpa-tree.json').read_text('utf-8'))
    commit = tree['sha']
    baseline = json.loads((OUT / 'baseline.json').read_text('utf-8'))
    cache = OUT / 'community'
    cache.mkdir(exist_ok=True)
    candidates = []
    for node in tree['tree']:
        match = re.search(r'/([^/]+)/lang/zh_cn.json$', node['path'])
        if match and match[1] in baseline:
            candidates.append(node['path'])
    # The GitHub recursive root tree can truncate alphabetically; retrieve the tail.
    for project in ('waystones', 'xaeros-minimap', 'xaeros-world-map'):
        url = f'https://api.github.com/repos/CFPAOrg/Minecraft-Mod-Language-Package/contents/projects/assets/{project}?ref={commit}'
        try:
            nodes = json.loads(download(url))
            for node in nodes:
                if node['type'] != 'dir':
                    continue
                subtree = json.loads(download(f'https://api.github.com/repos/CFPAOrg/Minecraft-Mod-Language-Package/git/trees/{node["sha"]}?recursive=1'))
                for entry in subtree['tree']:
                    match = re.search(r'(^|/)([^/]+)/lang/zh_cn.json$', entry['path'])
                    if match and match[2] in baseline:
                        candidates.append(node['path'] + '/' + entry['path'])
        except Exception as error:
            print(project, str(error))
    def fetch(path):
        target = cache / path
        if not target.exists():
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(download(f'https://raw.githubusercontent.com/CFPAOrg/Minecraft-Mod-Language-Package/{commit}/{path}'))
        return path
    errors = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=10) as executor:
        futures = {executor.submit(fetch, path): path for path in set(candidates)}
        for future in concurrent.futures.as_completed(futures):
            try:
                future.result()
            except Exception as error:
                errors.append({'path': futures[future], 'error': str(error)})
    (cache / 'LICENSE').write_bytes(download(f'https://raw.githubusercontent.com/CFPAOrg/Minecraft-Mod-Language-Package/{commit}/LICENSE'))
    (OUT / 'community-sources.json').write_text(json.dumps({'repository': 'https://github.com/CFPAOrg/Minecraft-Mod-Language-Package', 'commit': commit, 'files': sorted(set(candidates)), 'errors': errors}, ensure_ascii=False, indent=2), 'utf-8')
    print('Fetched', len(set(candidates)), 'files;', len(errors), 'errors')
