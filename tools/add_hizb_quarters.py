"""Adds `hizbQuarterStarts` (240 verse refs) to an existing quran-uthmani.json.

Usage:
    python -I tools/add_hizb_quarters.py <meta.json> <quran-uthmani.json> [<more quran-uthmani.json> ...]

<meta.json> is https://api.alquran.cloud/v1/meta. The field is inserted right after
`juzStarts` as text, so every other byte of the file (all Quran text) stays exactly the same.
Running it again on a file that already has the field does nothing.
"""

import json
import sys

meta = json.load(open(sys.argv[1], encoding="utf-8"))["data"]
quarters = [{"surah": r["surah"], "ayah": r["ayah"]} for r in meta["hizbQuarters"]["references"]]
assert len(quarters) == 240
assert quarters[0] == {"surah": 1, "ayah": 1}
for a, b in zip(quarters, quarters[1:]):
    assert (a["surah"], a["ayah"]) < (b["surah"], b["ayah"]), "hizb quarters out of order"

field = ',"hizbQuarterStarts":' + json.dumps(quarters, separators=(",", ":"))

for path in sys.argv[2:]:
    raw = open(path, "rb").read()
    text = raw.decode("utf-8")
    if '"hizbQuarterStarts":' in text:
        print(f"{path}: already has hizbQuarterStarts")
        continue
    marker = '"juzStarts":['
    start = text.index(marker)
    end = text.index("]", start) + 1
    updated = text[:end] + field + text[end:]
    # Check: removing the new field gives back the original bytes exactly.
    assert updated.replace(field, "", 1).encode("utf-8") == raw
    data = json.loads(updated)
    assert data["hizbQuarterStarts"] == quarters
    open(path, "wb").write(updated.encode("utf-8"))
    print(f"{path}: added {len(quarters)} hizb quarter starts")
