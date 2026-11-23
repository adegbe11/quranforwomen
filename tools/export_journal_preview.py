"""Exports JournalContent.swift templates and prompt packs to preview/journal-data.js for the HTML preview.

Usage: python -I tools/export_journal_preview.py
"""
import json
import re
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
src = open(os.path.join(ROOT, "QuranForWomen", "Data", "JournalContent.swift"), encoding="utf-8").read()

questions = re.findall(r'^\s+"([^"]+\?)",$', src.split("static func question")[0], re.M)

collections = [dict(zip(("id", "title", "summary", "symbol"), m))
               for m in re.findall(r'\.init\(id: "([^"]+)", title: "([^"]+)", summary: "([^"]+)", symbol: "([^"]+)"\)', src)]

templates = []
template_block = src.split("static let templates: [JournalTemplate] = [")[1].split("static func templates")[0]
for m in re.finditer(r'\.init\(id: "([^"]+)", collection: "([^"]+)", title: "([^"]+)", summary: "([^"]+)", prompts: \[(.*?)\](.*?)\),\n', template_block, re.S):
    tid, col, title, summary, prompts_src, tail = m.groups()
    prompts = [{"heading": h, "question": q, "style": (s or "lines")}
               for h, q, s in re.findall(r'\.init\(heading: "([^"]+)", question: "([^"]+)"(?:, style: \.(\w+))?\)', prompts_src)]
    verse = re.search(r'VerseRef\(surah: (\d+), ayah: (\d+)\)', tail)
    templates.append({"id": tid, "collection": col, "title": title, "summary": summary, "prompts": prompts,
                      "isDua": "isDua: true" in tail, "verse": f"{verse.group(1)}:{verse.group(2)}" if verse else None})

packs = []
pack_block = src.split("static let packs: [PromptPack] = [")[1]
for m in re.finditer(r'\.init\(id: "([^"]+)", title: "([^"]+)", symbol: "([^"]+)", prompts: \[(.*?)\](, isDua: true)?\)', pack_block, re.S):
    pid, title, symbol, body, dua = m.groups()
    packs.append({"id": pid, "title": title, "prompts": re.findall(r'"([^"]+)"', body), "isDua": bool(dua)})

data = {"questions": questions, "collections": collections, "templates": templates, "packs": packs}
out = os.path.join(ROOT, "preview", "journal-data.js")
open(out, "w", encoding="utf-8").write("window.JOURNAL = " + json.dumps(data, ensure_ascii=False) + ";\n")
print(f"{len(questions)} questions, {len(collections)} collections, {len(templates)} templates, {len(packs)} packs")
