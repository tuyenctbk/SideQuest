# Auto-generated localization script for 20 languages
import os

def escape_xml(s):
    s = s.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
    s = s.replace("'", "'")
    s = s.replace('"', '"')
    s = s.replace('@', '\@')
    s = s.replace('?', '\?')
    return s

def write_lang(folder, data):
    out_dir = os.path.join('app/src/main/res', folder)
    os.makedirs(out_dir, exist_ok=True)
    out_file = os.path.join(out_dir, 'strings.xml')
    with open(out_file, 'w', encoding='utf-8') as f:
        f.write('<?xml version="1.0" encoding="utf-8"?>
')
        f.write('<resources>
')
        for k, v in data.items():
            f.write(f'    <string name="{k}">{escape_xml(v)}</string>
')
        f.write('</resources>
')
    print(f'Wrote {len(data)} strings to {out_file}')

print('Script skeleton created')
