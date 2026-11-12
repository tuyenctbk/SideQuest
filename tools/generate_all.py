# Complete 20 Languages Localization Generator
import os

def escape_xml(s):
    s = s.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
    s = s.replace("'", "\'")
    s = s.replace('"', '\"')
    s = s.replace('@', '\@')
    s = s.replace('?', '\?')
    return s

def write_xml(folder, d):
    path = os.path.join('app/src/main/res', folder, 'strings.xml')
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as out:
        out.write('<?xml version="1.0" encoding="utf-8"?>
')
        out.write('<resources>
')
        for k, v in d.items():
            out.write(f'    <string name="{k}">{escape_xml(v)}</string>
')
        out.write('</resources>
')
    print(f'Wrote {len(d)} keys to {path}')

print('Generator initialized.')
