import os

def escape_xml(s):
    s = s.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
    s = s.replace("'", "\'")
    s = s.replace('"', '\"')
    s = s.replace('@', '\@')
    s = s.replace('?', '\?')
    return s

def save_strings(folder, data):
    target_dir = os.path.join('app/src/main/res', folder)
    os.makedirs(target_dir, exist_ok=True)
    target_file = os.path.join(target_dir, 'strings.xml')
    with open(target_file, 'w', encoding='utf-8') as out:
        out.write('<?xml version="1.0" encoding="utf-8"?>
')
        out.write('<resources>
')
        for k, v in data.items():
            out.write(f'    <string name="{k}">{escape_xml(v)}</string>
')
        out.write('</resources>
')
    print(f'Wrote {len(data)} strings to {target_file}')
