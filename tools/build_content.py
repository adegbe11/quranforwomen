"""Builds the bundled content for Quran for Women.

Usage:
    python -I tools/build_content.py <raw_dir> <resources_dir>

<raw_dir> must contain:
    uthmani.json   https://api.alquran.cloud/v1/quran/quran-uthmani  (Tanzil Uthmani text)
    meta.json      https://api.alquran.cloud/v1/meta

Quran text is never edited. The only transformations are:
    * removing a stray byte order mark
    * moving the leading Basmala of surahs 2 to 114 (except 9) out of verse 1,
      because the reader shows it as a separate heading (verse numbering is unchanged)

Quranic duas are sliced from the verified verse text by locating a start word, so no
Quranic Arabic is typed by hand. Hadith duas are typed and must be reviewed by a
qualified scholar before release (see CONTENT_REVIEW.md).
"""

import json
import os
import sys
import unicodedata

sys.stdout.reconfigure(encoding="utf-8")

RAW, OUT = sys.argv[1], sys.argv[2]
os.makedirs(OUT, exist_ok=True)

ENGLISH_NAMES = [
    "Al-Fatihah", "Al-Baqarah", "Ali 'Imran", "An-Nisa", "Al-Ma'idah", "Al-An'am", "Al-A'raf",
    "Al-Anfal", "At-Tawbah", "Yunus", "Hud", "Yusuf", "Ar-Ra'd", "Ibrahim", "Al-Hijr", "An-Nahl",
    "Al-Isra", "Al-Kahf", "Maryam", "Ta-Ha", "Al-Anbiya", "Al-Hajj", "Al-Mu'minun", "An-Nur",
    "Al-Furqan", "Ash-Shu'ara", "An-Naml", "Al-Qasas", "Al-'Ankabut", "Ar-Rum", "Luqman",
    "As-Sajdah", "Al-Ahzab", "Saba", "Fatir", "Ya-Sin", "As-Saffat", "Sad", "Az-Zumar", "Ghafir",
    "Fussilat", "Ash-Shura", "Az-Zukhruf", "Ad-Dukhan", "Al-Jathiyah", "Al-Ahqaf", "Muhammad",
    "Al-Fath", "Al-Hujurat", "Qaf", "Adh-Dhariyat", "At-Tur", "An-Najm", "Al-Qamar", "Ar-Rahman",
    "Al-Waqi'ah", "Al-Hadid", "Al-Mujadilah", "Al-Hashr", "Al-Mumtahanah", "As-Saff",
    "Al-Jumu'ah", "Al-Munafiqun", "At-Taghabun", "At-Talaq", "At-Tahrim", "Al-Mulk", "Al-Qalam",
    "Al-Haqqah", "Al-Ma'arij", "Nuh", "Al-Jinn", "Al-Muzzammil", "Al-Muddaththir", "Al-Qiyamah",
    "Al-Insan", "Al-Mursalat", "An-Naba", "An-Nazi'at", "'Abasa", "At-Takwir", "Al-Infitar",
    "Al-Mutaffifin", "Al-Inshiqaq", "Al-Buruj", "At-Tariq", "Al-A'la", "Al-Ghashiyah", "Al-Fajr",
    "Al-Balad", "Ash-Shams", "Al-Layl", "Ad-Duha", "Ash-Sharh", "At-Tin", "Al-'Alaq", "Al-Qadr",
    "Al-Bayyinah", "Az-Zalzalah", "Al-'Adiyat", "Al-Qari'ah", "At-Takathur", "Al-'Asr",
    "Al-Humazah", "Al-Fil", "Quraysh", "Al-Ma'un", "Al-Kawthar", "Al-Kafirun", "An-Nasr",
    "Al-Masad", "Al-Ikhlas", "Al-Falaq", "An-Nas",
]
assert len(ENGLISH_NAMES) == 114

quran = json.load(open(os.path.join(RAW, "uthmani.json"), encoding="utf-8"))["data"]
assert quran["edition"]["identifier"] == "quran-uthmani"
meta = json.load(open(os.path.join(RAW, "meta.json"), encoding="utf-8"))["data"]
sajdas = {(s["surah"], s["ayah"]) for s in meta["sajdas"]["references"]}

surahs = quran["surahs"]
assert len(surahs) == 114

basmala = surahs[0]["ayahs"][0]["text"].lstrip("﻿").strip()
verses = {}  # (surah, ayah) -> text, used for dua slicing
out_surahs = []
global_number = 0
for s in surahs:
    n = s["number"]
    ayahs = []
    pages = []
    for a in s["ayahs"]:
        t = a["text"].lstrip("﻿").strip()
        if n not in (1, 9) and a["numberInSurah"] == 1:
            # Surahs 95 and 97 encode the first letter with an extra shadda in this edition.
            variants = [basmala, basmala[0] + "ّ" + basmala[1:]]
            prefix = next((v for v in variants if t.startswith(v)), None)
            assert prefix, f"surah {n} verse 1 has no basmala prefix"
            t = t[len(prefix):].strip()
        assert t, f"empty verse {n}:{a['numberInSurah']}"
        ayahs.append(t)
        pages.append(a["page"])
        verses[(n, a["numberInSurah"])] = t
        global_number += 1
        assert a["number"] == global_number
    arabic_name = s["name"]
    for prefix in ("سُورَةُ ",):
        if arabic_name.startswith(prefix):
            arabic_name = arabic_name[len(prefix):]
    out_surahs.append({
        "id": n,
        "arabicName": arabic_name,
        "name": ENGLISH_NAMES[n - 1],
        "meaning": s["englishNameTranslation"],
        "revelation": s["revelationType"],
        "sajdas": sorted(a for (sn, a) in sajdas if sn == n),
        "pages": pages,
        "ayahs": ayahs,
    })

assert global_number == 6236
juz_starts = [{"surah": j["surah"], "ayah": j["ayah"]} for j in meta["juzs"]["references"]]
assert len(juz_starts) == 30
hizb_quarter_starts = [{"surah": r["surah"], "ayah": r["ayah"]} for r in meta["hizbQuarters"]["references"]]
assert len(hizb_quarter_starts) == 240
json.dump({
    "source": "Tanzil Project Uthmani text (tanzil.net), via api.alquran.cloud edition quran-uthmani. Unmodified.",
    "basmala": basmala,
    "juzStarts": juz_starts,
    "hizbQuarterStarts": hizb_quarter_starts,
    "surahs": out_surahs,
}, open(os.path.join(OUT, "quran-uthmani.json"), "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
print("quran: 114 surahs, 6236 verses")

# ---------- Dua slicing helpers ----------

def is_mark(ch):
    cp = ord(ch)
    return (0x0610 <= cp <= 0x061A or 0x064B <= cp <= 0x065F or cp == 0x0670
            or 0x06D6 <= cp <= 0x06ED or cp == 0x0640 or unicodedata.category(ch) == "Mn")

FOLD = {"ٱ": "ا", "أ": "ا", "إ": "ا", "آ": "ا", "ى": "ي", "ة": "ه"}

def stripped_with_map(text):
    chars, idx = [], []
    for i, ch in enumerate(text):
        if is_mark(ch):
            continue
        chars.append(FOLD.get(ch, ch))
        idx.append(i)
    return "".join(chars), idx

def slice_from(text, marker, occurrence=1):
    plain, idx = stripped_with_map(text)
    pos = -1
    for _ in range(occurrence):
        pos = plain.find(marker, pos + 1)
        assert pos >= 0, f"marker {marker!r} not found"
    return text[idx[pos]:]

ARABIC_DIGITS = str.maketrans("0123456789", "٠١٢٣٤٥٦٧٨٩")

LAST_SLICED = [False]

def quran_text(surah, start, end=None, marker=None, occurrence=1):
    end = end or start
    LAST_SLICED[0] = marker is not None
    parts = []
    for a in range(start, end + 1):
        t = verses[(surah, a)]
        if a == start and marker:
            t = slice_from(t, marker, occurrence)
        parts.append(f"{t} ﴿{str(a).translate(ARABIC_DIGITS)}﴾")
    return " ".join(parts)

# ---------- Duas ----------
# kind: "quran" (text taken from verified Quran text) or "hadith" (typed, needs review)

CATEGORIES = [
    {"id": "morning-evening", "title": "Morning & Evening", "symbol": "sun.horizon"},
    {"id": "peace-patience", "title": "Peace & Patience", "symbol": "leaf"},
    {"id": "worry-hardship", "title": "Worry & Hardship", "symbol": "cloud.rain"},
    {"id": "gratitude", "title": "Gratitude", "symbol": "heart"},
    {"id": "marriage-family", "title": "Marriage & Family", "symbol": "house"},
    {"id": "motherhood", "title": "Motherhood", "symbol": "figure.and.child.holdinghands"},
    {"id": "forgiveness", "title": "Forgiveness", "symbol": "drop"},
    {"id": "protection", "title": "Protection", "symbol": "shield"},
    {"id": "sleep", "title": "Sleep", "symbol": "moon.stars"},
]

def q(id, cat, title, ref, arabic, translit, meaning, note=None):
    surah, verses = ref.split(":")
    first, _, last = verses.partition("-")
    return {"id": id, "category": cat, "title": title, "kind": "quran", "source": f"Quran {ref}",
            "arabic": arabic, "transliteration": translit, "translation": meaning, "note": note,
            "verses": {"surah": int(surah), "from": int(first), "to": int(last or first)},
            # Audio is only offered when the dua is made of whole verses, so the
            # recitation never includes words that are not shown on screen.
            "wholeVerses": not LAST_SLICED[0]}

def h(id, cat, title, source, arabic, translit, meaning, note=None):
    return {"id": id, "category": cat, "title": title, "kind": "hadith", "source": source,
            "arabic": arabic, "transliteration": translit, "translation": meaning, "note": note}

duas = [
    # Morning & Evening
    h("waking", "morning-evening", "On waking up", "Sahih al-Bukhari 6324",
      "الْحَمْدُ لِلَّهِ الَّذِي أَحْيَانَا بَعْدَ مَا أَمَاتَنَا وَإِلَيْهِ النُّشُورُ",
      "Alhamdu lillahil-ladhi ahyana ba'da ma amatana wa ilayhin-nushur",
      "All praise is for Allah, who gave us life after He caused us to die, and to Him is the return."),
    h("morning-bika", "morning-evening", "In the morning", "Sunan Abi Dawud 5068 · Jami' at-Tirmidhi 3391",
      "اللَّهُمَّ بِكَ أَصْبَحْنَا، وَبِكَ أَمْسَيْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ، وَإِلَيْكَ النُّشُورُ",
      "Allahumma bika asbahna, wa bika amsayna, wa bika nahya, wa bika namutu, wa ilaykan-nushur",
      "O Allah, by You we enter the morning and by You we enter the evening. By You we live and by You we die, and to You is the return."),
    h("no-harm", "morning-evening", "For protection, morning and evening", "Sunan Abi Dawud 5088 · Jami' at-Tirmidhi 3388",
      "بِسْمِ اللَّهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الْأَرْضِ وَلَا فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ",
      "Bismillahil-ladhi la yadurru ma'asmihi shay'un fil-ardi wa la fis-sama'i wa huwas-sami'ul-'alim",
      "In the name of Allah, with whose name nothing on earth or in the heaven can cause harm, and He is the All Hearing, the All Knowing.",
      "Said three times in the morning and three times in the evening."),

    # Peace & Patience
    q("patience-2-250", "peace-patience", "For patience and steady feet", "2:250",
      quran_text(2, 250, marker="ربنا"),
      "Rabbana afrigh 'alayna sabran wa thabbit aqdamana wansurna 'alal-qawmil-kafirin",
      "Our Lord, pour patience upon us, make our steps firm, and help us against the disbelieving people."),
    h("heart-firm", "peace-patience", "For a steady heart", "Jami' at-Tirmidhi 2140",
      "يَا مُقَلِّبَ الْقُلُوبِ ثَبِّتْ قَلْبِي عَلَىٰ دِينِكَ",
      "Ya muqallibal-qulub, thabbit qalbi 'ala dinik",
      "O Turner of hearts, keep my heart firm on Your religion."),
    q("guidance-3-8", "peace-patience", "To stay guided", "3:8",
      quran_text(3, 8),
      "Rabbana la tuzigh qulubana ba'da idh hadaytana wa hab lana min ladunka rahmah, innaka antal-Wahhab",
      "Our Lord, do not let our hearts turn away after You have guided us, and grant us mercy from You. You are the Giver."),
    h("huda", "peace-patience", "For guidance and contentment", "Sahih Muslim 2721",
      "اللَّهُمَّ إِنِّي أَسْأَلُكَ الْهُدَىٰ وَالتُّقَىٰ وَالْعَفَافَ وَالْغِنَىٰ",
      "Allahumma inni as'alukal-huda wat-tuqa wal-'afafa wal-ghina",
      "O Allah, I ask You for guidance, piety, chastity, and freedom from need."),
    q("chest-20-25", "peace-patience", "For ease and clear speech", "20:25-28",
      quran_text(20, 25, 28, marker="رب"),
      "Rabbish-rah li sadri, wa yassir li amri, wahlul 'uqdatan min lisani, yafqahu qawli",
      "My Lord, open my heart for me, make my task easy for me, and untie the knot from my tongue, so they understand my words.",
      "The dua of Prophet Musa."),
    q("knowledge-20-114", "peace-patience", "For knowledge", "20:114",
      quran_text(20, 114, marker="رب زدني"),
      "Rabbi zidni 'ilma",
      "My Lord, increase me in knowledge."),

    # Worry & Hardship
    h("hamm-hazan", "worry-hardship", "For worry and sadness", "Sahih al-Bukhari 6369",
      "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْهَمِّ وَالْحَزَنِ، وَالْعَجْزِ وَالْكَسَلِ، وَالْجُبْنِ وَالْبُخْلِ، وَضَلَعِ الدَّيْنِ، وَغَلَبَةِ الرِّجَالِ",
      "Allahumma inni a'udhu bika minal-hammi wal-hazan, wal-'ajzi wal-kasal, wal-jubni wal-bukhl, wa dala'id-dayn, wa ghalabatir-rijal",
      "O Allah, I seek refuge in You from worry and sadness, from weakness and laziness, from cowardice and miserliness, from the burden of debt, and from being overpowered by people."),
    h("distress", "worry-hardship", "In times of distress", "Sahih al-Bukhari 6346 · Sahih Muslim 2730",
      "لَا إِلَٰهَ إِلَّا اللَّهُ الْعَظِيمُ الْحَلِيمُ، لَا إِلَٰهَ إِلَّا اللَّهُ رَبُّ الْعَرْشِ الْعَظِيمِ، لَا إِلَٰهَ إِلَّا اللَّهُ رَبُّ السَّمَاوَاتِ وَرَبُّ الْأَرْضِ وَرَبُّ الْعَرْشِ الْكَرِيمِ",
      "La ilaha illallahul-'Azimul-Halim, la ilaha illallahu Rabbul-'arshil-'azim, la ilaha illallahu Rabbus-samawati wa Rabbul-ardi wa Rabbul-'arshil-karim",
      "There is no god but Allah, the Great, the Forbearing. There is no god but Allah, Lord of the Great Throne. There is no god but Allah, Lord of the heavens, Lord of the earth, and Lord of the Noble Throne."),
    q("yunus-21-87", "worry-hardship", "The dua of Prophet Yunus", "21:87",
      quran_text(21, 87, marker="لا اله الا انت"),
      "La ilaha illa anta subhanaka inni kuntu minaz-zalimin",
      "There is no god but You. Glory be to You. I have been one of the wrongdoers."),
    h("ya-hayyu", "worry-hardship", "Asking for His mercy", "Jami' at-Tirmidhi 3524",
      "يَا حَيُّ يَا قَيُّومُ بِرَحْمَتِكَ أَسْتَغِيثُ",
      "Ya Hayyu ya Qayyum, bi rahmatika astaghith",
      "O Ever Living, O Sustainer of all, by Your mercy I call for help."),
    h("musibah", "worry-hardship", "After a loss", "Sahih Muslim 918",
      "إِنَّا لِلَّهِ وَإِنَّا إِلَيْهِ رَاجِعُونَ، اللَّهُمَّ أْجُرْنِي فِي مُصِيبَتِي، وَأَخْلِفْ لِي خَيْرًا مِنْهَا",
      "Inna lillahi wa inna ilayhi raji'un. Allahumma'jurni fi musibati wa akhlif li khayran minha",
      "We belong to Allah and to Him we return. O Allah, reward me in my loss and replace it with something better.",
      "Narrated by Umm Salamah, who said this after losing her husband."),
    q("faqir-28-24", "worry-hardship", "When you are in need", "28:24",
      quran_text(28, 24, marker="رب اني"),
      "Rabbi inni lima anzalta ilayya min khayrin faqir",
      "My Lord, I am truly in need of whatever good You send down to me.",
      "The dua of Prophet Musa."),
    q("hasbuna-3-173", "worry-hardship", "Trusting Allah", "3:173",
      quran_text(3, 173, marker="حسبنا الله"),
      "Hasbunallahu wa ni'mal-wakil",
      "Allah is enough for us, and He is the best Guardian."),

    # Gratitude
    q("gratitude-27-19", "gratitude", "To be grateful", "27:19",
      quran_text(27, 19, marker="رب اوزعني"),
      "Rabbi awzi'ni an ashkura ni'matakal-lati an'amta 'alayya wa 'ala walidayya wa an a'mala salihan tardahu wa adkhilni birahmatika fi 'ibadikas-salihin",
      "My Lord, help me be grateful for Your favor which You gave to me and to my parents, and to do good that pleases You. Admit me by Your mercy among Your righteous servants.",
      "The dua of Prophet Sulayman."),
    h("a-inni", "gratitude", "Help me remember You", "Sunan Abi Dawud 1522",
      "اللَّهُمَّ أَعِنِّي عَلَىٰ ذِكْرِكَ وَشُكْرِكَ وَحُسْنِ عِبَادَتِكَ",
      "Allahumma a'inni 'ala dhikrika wa shukrika wa husni 'ibadatik",
      "O Allah, help me to remember You, to thank You, and to worship You well."),

    # Marriage & Family
    q("family-25-74", "marriage-family", "For a loving family", "25:74",
      quran_text(25, 74, marker="ربنا"),
      "Rabbana hab lana min azwajina wa dhurriyyatina qurrata a'yunin waj'alna lil-muttaqina imama",
      "Our Lord, give us joy in our spouses and children, and make us an example for those who are mindful of You."),
    h("barakallah", "marriage-family", "For a newly married couple", "Sunan Abi Dawud 2130 · Jami' at-Tirmidhi 1091",
      "بَارَكَ اللَّهُ لَكَ، وَبَارَكَ عَلَيْكَ، وَجَمَعَ بَيْنَكُمَا فِي خَيْرٍ",
      "Barakallahu laka, wa baraka 'alayka, wa jama'a baynakuma fi khayr",
      "May Allah bless you, send blessings upon you, and bring you both together in goodness."),
    q("parents-17-24", "marriage-family", "For your parents", "17:24",
      quran_text(17, 24, marker="رب ارحمهما"),
      "Rabbir-hamhuma kama rabbayani saghira",
      "My Lord, have mercy on them as they raised me when I was small."),
    q("prayer-14-40", "marriage-family", "For you and your children to pray", "14:40",
      quran_text(14, 40),
      "Rabbij-'alni muqimas-salati wa min dhurriyyati, Rabbana wa taqabbal du'a",
      "My Lord, make me one who keeps up the prayer, and my children too. Our Lord, accept my prayer.",
      "The dua of Prophet Ibrahim."),
    q("forgive-14-41", "marriage-family", "Forgiveness for your family", "14:41",
      quran_text(14, 41),
      "Rabbanagh-fir li wa liwalidayya wa lil-mu'minina yawma yaqumul-hisab",
      "Our Lord, forgive me, my parents, and the believers on the Day the account is taken.",
      "The dua of Prophet Ibrahim."),

    # Motherhood
    q("imran-3-35", "motherhood", "The dua of Maryam's mother", "3:35",
      quran_text(3, 35, marker="رب اني"),
      "Rabbi inni nadhartu laka ma fi batni muharraran fataqabbal minni, innaka antas-Sami'ul-'Alim",
      "My Lord, I have dedicated to You what is in my womb, devoted to Your service, so accept this from me. You are the All Hearing, the All Knowing.",
      "Said by the wife of 'Imran while she was pregnant with Maryam."),
    q("zakariya-3-38", "motherhood", "For righteous children", "3:38",
      quran_text(3, 38, marker="رب هب"),
      "Rabbi hab li min ladunka dhurriyyatan tayyibah, innaka sami'ud-du'a",
      "My Lord, grant me good children from You. You hear every prayer.",
      "The dua of Prophet Zakariya."),
    q("ahqaf-46-15", "motherhood", "Gratitude for your family", "46:15",
      quran_text(46, 15, marker="رب اوزعني"),
      "Rabbi awzi'ni an ashkura ni'matakal-lati an'amta 'alayya wa 'ala walidayya wa an a'mala salihan tardahu wa aslih li fi dhurriyyati, inni tubtu ilayka wa inni minal-muslimin",
      "My Lord, help me be grateful for Your favor upon me and my parents, and to do good that pleases You. Make my children good. I turn to You, and I am one of those who submit to You.",
      "This verse begins by describing a mother carrying and nursing her child."),

    # Forgiveness
    h("sayyid-istighfar", "forgiveness", "The best way to ask forgiveness", "Sahih al-Bukhari 6306",
      "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَٰهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَىٰ عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ لَكَ بِذَنْبِي فَاغْفِرْ لِي، فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ",
      "Allahumma anta Rabbi la ilaha illa ant, khalaqtani wa ana 'abduk, wa ana 'ala 'ahdika wa wa'dika mastata't, a'udhu bika min sharri ma sana't, abu'u laka bini'matika 'alayya, wa abu'u laka bidhanbi faghfir li, fa innahu la yaghfirudh-dhunuba illa ant",
      "O Allah, You are my Lord. There is no god but You. You created me and I am Your servant. I keep Your covenant and promise as much as I can. I seek refuge in You from the evil I have done. I admit Your favors upon me and I admit my sins, so forgive me, for no one forgives sins except You.",
      "Known as Sayyid al-Istighfar, the best way to ask forgiveness."),
    q("adam-7-23", "forgiveness", "The dua of Adam and Hawwa", "7:23",
      quran_text(7, 23, marker="ربنا"),
      "Rabbana zalamna anfusana wa in lam taghfir lana wa tarhamna lanakunanna minal-khasirin",
      "Our Lord, we have wronged ourselves. If You do not forgive us and have mercy on us, we will surely be among the losers."),
    h("afuw", "forgiveness", "In the last nights of Ramadan", "Jami' at-Tirmidhi 3513",
      "اللَّهُمَّ إِنَّكَ عَفُوٌّ تُحِبُّ الْعَفْوَ فَاعْفُ عَنِّي",
      "Allahumma innaka 'afuwwun tuhibbul-'afwa fa'fu 'anni",
      "O Allah, You are the One who pardons, and You love to pardon, so pardon me.",
      "Taught by the Prophet to 'Aishah for Laylat al-Qadr."),
    q("mercy-23-118", "forgiveness", "For forgiveness and mercy", "23:118",
      quran_text(23, 118, marker="رب اغفر"),
      "Rabbigh-fir warham wa anta khayrur-rahimin",
      "My Lord, forgive and have mercy, and You are the best of those who show mercy."),

    # Protection
    q("ayat-al-kursi", "protection", "Ayat al-Kursi", "2:255",
      quran_text(2, 255),
      None,
      "Allah, there is no god but Him, the Ever Living, the Sustainer of all. Neither drowsiness nor sleep overtakes Him. To Him belongs whatever is in the heavens and the earth. Who can intercede with Him except by His permission? He knows what is before them and what is behind them, and they grasp nothing of His knowledge except what He wills. His Throne extends over the heavens and the earth, and guarding them does not tire Him. He is the Most High, the Most Great."),
    h("kalimat", "protection", "When you stop at a place", "Sahih Muslim 2708",
      "أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ",
      "A'udhu bikalimatillahit-tammati min sharri ma khalaq",
      "I seek refuge in the perfect words of Allah from the evil of what He has created.",
      "Narrated by Khawlah bint Hakim."),
    q("al-falaq", "protection", "Surah Al-Falaq", "113:1-5",
      quran_text(113, 1, 5),
      None,
      "Say, I seek refuge in the Lord of daybreak, from the evil of what He created, from the evil of darkness when it settles, from the evil of those who blow on knots, and from the evil of an envier when he envies."),
    q("an-nas", "protection", "Surah An-Nas", "114:1-6",
      quran_text(114, 1, 6),
      None,
      "Say, I seek refuge in the Lord of people, the King of people, the God of people, from the evil of the whisperer who withdraws, who whispers in the hearts of people, from among jinn and people."),

    # Sleep
    h("sleep-bismika", "sleep", "Before sleeping", "Sahih al-Bukhari 6324",
      "بِاسْمِكَ اللَّهُمَّ أَمُوتُ وَأَحْيَا",
      "Bismika Allahumma amutu wa ahya",
      "In Your name, O Allah, I die and I live."),
    h("sleep-qini", "sleep", "Lying down to sleep", "Sunan Abi Dawud 5045 · Jami' at-Tirmidhi 3398",
      "اللَّهُمَّ قِنِي عَذَابَكَ يَوْمَ تَبْعَثُ عِبَادَكَ",
      "Allahumma qini 'adhabaka yawma tab'athu 'ibadak",
      "O Allah, protect me from Your punishment on the Day You raise Your servants.",
      "The Prophet would place his right hand under his cheek and say this."),
    q("al-ikhlas", "sleep", "Surah Al-Ikhlas", "112:1-4",
      quran_text(112, 1, 4),
      None,
      "Say, He is Allah, the One. Allah, the Eternal Refuge. He neither begets nor was born, and there is none comparable to Him.",
      "The Prophet would recite Al-Ikhlas, Al-Falaq and An-Nas before sleeping (Sahih al-Bukhari 5017)."),

    # Also good for patience
    q("asiya-66-11", "peace-patience", "The dua of Asiyah", "66:11",
      quran_text(66, 11, marker="رب ابن"),
      "Rabbibni li 'indaka baytan fil-jannah, wa najjini min Fir'awna wa 'amalih, wa najjini minal-qawmiz-zalimin",
      "My Lord, build for me a home near You in Paradise, and save me from Pharaoh and his deeds, and save me from the wrongdoing people.",
      "Said by Asiyah, the wife of Pharaoh, whom Allah gives as an example for the believers."),
    q("dunya-akhirah-2-201", "peace-patience", "For good in both lives", "2:201",
      quran_text(2, 201, marker="ربنا"),
      "Rabbana atina fid-dunya hasanatan wa fil-akhirati hasanatan wa qina 'adhaban-nar",
      "Our Lord, give us good in this world and good in the Hereafter, and protect us from the punishment of the Fire."),
]

ids = [d["id"] for d in duas]
assert len(ids) == len(set(ids)), "duplicate dua id"
cat_ids = {c["id"] for c in CATEGORIES}
for d in duas:
    assert d["category"] in cat_ids, d["id"]
    for field in ("translation", "transliteration", "note"):
        if d[field]:
            assert not any(ch in d[field] for ch in (chr(0x2014), chr(0x2013))), d["id"]

DHIKR = [
    {"id": "subhanallah", "arabic": "سُبْحَانَ اللَّهِ", "transliteration": "SubhanAllah",
     "translation": "Glory be to Allah", "target": 33,
     "source": "Taught to Fatimah · Sahih al-Bukhari 3705"},
    {"id": "alhamdulillah", "arabic": "الْحَمْدُ لِلَّهِ", "transliteration": "Alhamdulillah",
     "translation": "All praise is for Allah", "target": 33,
     "source": "Taught to Fatimah · Sahih al-Bukhari 3705"},
    {"id": "allahu-akbar", "arabic": "اللَّهُ أَكْبَرُ", "transliteration": "Allahu Akbar",
     "translation": "Allah is the Greatest", "target": 34,
     "source": "Taught to Fatimah · Sahih al-Bukhari 3705"},
    {"id": "tahlil", "arabic": "لَا إِلَٰهَ إِلَّا اللَّهُ", "transliteration": "La ilaha illallah",
     "translation": "There is no god but Allah", "target": 100,
     "source": "Jami' at-Tirmidhi 3383"},
    {"id": "astaghfirullah", "arabic": "أَسْتَغْفِرُ اللَّهَ", "transliteration": "Astaghfirullah",
     "translation": "I ask Allah for forgiveness", "target": 100,
     "source": "Sahih Muslim 2702"},
    {"id": "subhanallahi-wa-bihamdihi", "arabic": "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ",
     "transliteration": "SubhanAllahi wa bihamdihi", "translation": "Glory be to Allah and all praise is His",
     "target": 100, "source": "Sahih al-Bukhari 6405"},
    {"id": "hawqala", "arabic": "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ",
     "transliteration": "La hawla wa la quwwata illa billah",
     "translation": "There is no power and no strength except with Allah", "target": 33,
     "source": "Sahih al-Bukhari 6384"},
    {"id": "salawat", "arabic": "اللَّهُمَّ صَلِّ عَلَىٰ مُحَمَّدٍ", "transliteration": "Allahumma salli 'ala Muhammad",
     "translation": "O Allah, send blessings upon Muhammad", "target": 10,
     "source": "Sahih Muslim 408"},
]

# Daily verses: whole verses only, chosen for comfort and encouragement.
DAILY = ["2:152", "2:153", "2:186", "20:25", "2:286", "3:139", "13:28", "14:7", "16:97",
         "29:69", "33:35", "39:53", "40:60", "50:16", "64:11", "65:3", "93:3", "93:5",
         "94:5", "94:6", "2:45", "11:115", "3:200", "20:46"]
for ref in DAILY:
    s, a = map(int, ref.split(":"))
    assert (s, a) in verses, ref

# Verse collections. Whole verses only, chosen for comfort. Titles and intros are plain and factual.
FEELINGS = [
    {"id": "anxious", "title": "Anxious", "symbol": "wind",
     "verses": ["13:28", "2:286", "65:3", "9:51", "94:5", "94:6", "3:139", "2:153"]},
    {"id": "sad", "title": "Sad", "symbol": "cloud.rain",
     "verses": ["93:3", "93:4", "93:5", "12:86", "2:155", "2:156", "2:157", "39:53"]},
    {"id": "lonely", "title": "Lonely", "symbol": "person",
     "verses": ["50:16", "2:186", "57:4", "58:7", "20:46", "2:152"]},
    {"id": "overwhelmed", "title": "Overwhelmed", "symbol": "water.waves",
     "verses": ["2:286", "65:7", "4:28", "94:5", "94:6", "3:173"]},
    {"id": "grateful", "title": "Grateful", "symbol": "heart",
     "verses": ["14:7", "2:152", "16:18", "55:13", "31:12", "93:11"]},
    {"id": "hopeful", "title": "In need of hope", "symbol": "sunrise",
     "verses": ["39:53", "12:87", "15:56", "2:214", "40:60", "2:186"]},
    {"id": "regretful", "title": "Regretful", "symbol": "drop",
     "verses": ["39:53", "4:110", "3:135", "25:70", "11:114", "66:8"]},
    {"id": "tired", "title": "Tired", "symbol": "moon.zzz",
     "verses": ["2:153", "2:45", "3:200", "16:127", "39:10", "70:5"]},
    {"id": "angry", "title": "Angry", "symbol": "flame",
     "verses": ["3:134", "42:37", "41:34", "7:199"]},
]

TOPICS = [
    {"id": "women", "title": "Women in the Quran", "symbol": "sparkles",
     "intro": "Verses about women the Quran honors by name or story, and Allah's promise that no good deed of a woman is ever lost.",
     "verses": ["33:35", "3:195", "4:124", "16:97", "9:71", "3:42", "3:43", "19:23", "19:24", "19:25", "19:26",
                "28:7", "66:11", "66:12", "58:1", "27:44"]},
    {"id": "mercy", "title": "Allah's Mercy", "symbol": "hands.and.sparkles",
     "intro": "Verses that describe how wide Allah's mercy is.",
     "verses": ["7:156", "6:54", "39:53", "21:107", "1:3"]},
    {"id": "patience", "title": "Patience", "symbol": "leaf",
     "intro": "Verses about patience and its reward.",
     "verses": ["2:153", "2:155", "2:156", "2:157", "39:10", "16:127", "70:5", "103:3"]},
    {"id": "family", "title": "Family & Parents", "symbol": "house",
     "intro": "Verses about spouses, children and honoring parents.",
     "verses": ["4:1", "30:21", "17:23", "17:24", "31:14", "46:15", "25:74"]},
    {"id": "trust", "title": "Trust in Allah", "symbol": "shield",
     "intro": "Verses about relying on Allah in every matter.",
     "verses": ["65:3", "3:159", "9:51", "3:173", "11:123"]},
    {"id": "prayer", "title": "Prayer", "symbol": "moon.stars",
     "intro": "Verses about salah and keeping it.",
     "verses": ["2:45", "20:14", "29:45", "2:238", "11:114", "17:78"]},
]

for group in FEELINGS + TOPICS:
    for ref in group["verses"]:
        sn, an = map(int, ref.split(":"))
        assert (sn, an) in verses, ref

# Arabic for the daily verses, so the home screen widget can show verified text on its own.
widget = [{"ref": ref, "surahName": ENGLISH_NAMES[int(ref.split(":")[0]) - 1], "arabic": verses[tuple(map(int, ref.split(":")))]} for ref in DAILY]
json.dump(widget, open(os.path.join(OUT, "widget-verses.json"), "w", encoding="utf-8"), ensure_ascii=False)

json.dump({"categories": CATEGORIES, "duas": duas, "dhikr": DHIKR, "dailyVerses": DAILY,
           "feelings": FEELINGS, "topics": TOPICS},
          open(os.path.join(OUT, "content.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1)
print(f"duas: {len(duas)} in {len(CATEGORIES)} categories, dhikr: {len(DHIKR)}, daily verses: {len(DAILY)}")

# Print sliced Quranic duas so a human can check every start point.
for d in duas:
    if d["kind"] == "quran":
        print(f"\n[{d['id']}] {d['source']}\n{d['arabic']}")
