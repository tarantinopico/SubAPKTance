# Optimalizace prostoru, velikosti prvků a odstranění hluchých mezer v kartách

Cílem této úpravy je odstranit prázdné černé mezery uvnitř karet na **Přehledu (Dashboard)**, na stránce **Přidat (Nový záznam)** i v **Analytice a Katalogu látek**, opravit přetékání štítků v hlavní kartě aktivních látek a zvětšit grafy, tlačítka, posuvníky i písmo tak, aby každá karta využívala svůj prostor naplno a byla perfektně čitelná.

### User Review & Critical Decisions

> [!IMPORTANT]
> Níže jsou potvrzená rozhodnutí z vašich odpovědí, podle kterých bude upraveno rozvržení a velikost prvků:

- **Potvrzená volba 1 (Přehled / Dashboard):** Zvětšit grafy, písmo, ukazatele a položky v kartách tak, aby beze zbytku vyplnily celou plochu karet bez prázdných mezer.
- **Potvrzená volba 2 (Stránka Přidat / Nový záznam):** Výrazně zvětšit tlačítka dávkování (`-5x`, `-`, `+`, `+5x`), posuvník prahových zón, předvolby dávek i přepínače času a kontextu a odstranit hluchá místa mezi řádky.
- **Potvrzená volba 3 (Sjednocení v celé aplikaci):** Stejný princip plného využití plochy karet a větší čitelnosti prvků sjednotit i v Analytice a Katalogu látek.

---

### 1. Overview & Core Concept

- **Co úprava řeší:**
  1. **Oprava zúženého grafu na Dashboardu:** V hlavní kartě aktivních látek docházelo při více aktivních látkách k vertikálnímu zalomení posledního štítku (*Všechny aktivní*), což roztáhlo horní lištu přes polovinu karty a zmáčklo farmakokinetickou křivku do úzkého pruhu. Přidáním vodorovného posouvání lišty štítků se křivka okamžitě roztáhne na plnou výšku karty.
  2. **Konec hluchých mezer (`SpaceBetween`) uvnitř karet:** Karty s pevnou výškou dosud odsouvaly malé prvky k hornímu a dolnímu okraji a uprostřed nechávaly prázdné černé plochy. Místo prázdného místa se samotné položky, grafy, tlačítka a ukazatele proporcionálně roztáhnou a zvětší.
  3. **Ergonomické ovládání na stránce Přidat:** Přepínače látek, poddruhů, krokovací tlačítka, posuvník prahových zón, časové pilulky (`Teď`, `-10m`, `-30m`…) i hlavní tlačítko `+ Zaznamenat užití` budou výrazně větší a vyplní karty bez zbytečných mezer.

---

### 2. User Experience & Visual Design

- **Přehled (Dashboard):**
  - **Horní Bio-Cockpit karta:** Větší kruhový indikátor (52 dp) a vyšší, plně vyplněné statistické boxy (*SPEND*, *DOSES*, *STATUS*) s větším písmem (`12.5 sp` hodnoty, `9.5 sp` popisky).
  - **Hlavní karta aktivních látek (Hero PK Monitor):** Vodorovně posuvná lišta aktivních látek s většími pilulkami (`12 sp`, ikony `14 dp`) bez zalamování; farmakokinetická křivka zabírá celý střed karty; spodní 4fázová lišta (*Užití — Nástup — Vrchol — Konec*) má větší štítek fáze, silnější 8dp progres bar a čitelnější odpočet (`11.5 sp`).
  - **Střední dvojice karet (`TIME SINCE LAST` a `TODAY UNITS / COST`):** Tři řádky položek v každé kartě se rovnoměrně rozdělí přes celou výšku karty pomocí vnitřních podkaret s většími ikonami (`28 dp`), větším písmem (`12.5 sp` název, `11.5 sp` hodnota) a silnějšími `6 dp` indikačními lištami — žádné prázdné místo pod nadpisem.
  - **Spodní karta (`24H CIRCADIAN TIMELINE` & Rychlé dávky):** Dvojnásobná výška 24h časové osy (`26 dp`) s popisky hodin (`00h`, `06h`, `12h`, `18h`, `24h`) a většími body dávek; výrazně vyšší tlačítka rychlého zápisu (`38 dp` výška, `11.5 sp` písmo).
- **Stránka Přidat (Nový záznam):**
  - **Karta 1 (Látka & Poddruh):** Upravený poměr výšky (`0.15f`) a výrazně větší pilulky látek i poddruhů (`7 dp` vertikální padding, `12 sp` text, `15 dp` ikony) těsně a přehledně pod sebou bez prázdného pruhu uprostřed.
  - **Karta 2 (Dávkování):** Větší krokovací tlačítka (`46 dp` pro `-5x`/`+5x` a `56 dp` pro `-`/`+`), dominantní středová hodnota dávky (`42 sp`), vyšší 5zónový prahový posuvník (`28 dp` s čitelnými popisky zón `10 sp`) a vysoká tlačítka předvoleb dávek (`40 mg`, `80 mg`… s výškou `34 dp` a písmem `11.5 sp`).
  - **Karta 3 (Simulace PK křivky & Fáze):** Větší prostor pro živou křivku (`0.22f` výšky obrazovky) a větší popisky fází (`10.5 sp`).
  - **Karta 4 (Kontext, Čas a Uložení):** Vyšší pilulky kontextu a vlastních polí, větší časové přepínače (`Teď`, `-10m`, `-30m`, `-1h`, `-2h`, `-4h`) a plné `48 dp` vysoké indigové tlačítko `+ Zaznamenat užití`.
- **Analytika & Katalog látek:**
  - Vyšší KPI karty a statistické lišty (`MIN | PRŮMĚR | MEDIÁN | P90 | MAX`) s větším monospace písmem (`11 sp`) a kompaktnějšími vnějšími okraji.

---

### 3. Key Product Decisions & Trade-Offs

- **Rozhodnutí 1: Vnitřní vyplnění karet pomocí proporcionální výšky (`weight(1f)`) místo prázdných mezer (`SpaceBetween`)**
  - *Zvolený přístup:* Uvnitř všech Bento karet nahradíme volné rozestupy `Arrangement.SpaceBetween` proporcionálním rozložením řádků a grafů (`Modifier.weight(1f)` + `fillMaxHeight()`), takže samotné prvky (řádky látek, tlačítka, posuvníky a grafy) se zvětší a vyplní plochu karty.
  - *Proč:* Zachovává přehlednost celé obrazovky bez nutnosti neustálého skrolování, ale eliminuje hluchá černá místa.
- **Rozhodnutí 2: Horizontálně posuvná lišta aktivních látek v Hero kartě**
  - *Zvolený přístup:* Lišta aktivních látek v horní části Hero PK karty bude vodorovně posuvná (`horizontalScroll`) s pevnou výškou jednoho řádku.
  - *Proč:* Zabraňuje vertikálnímu zalamování textu při 4 a více aktivních látkách a garantuje maximální výšku pro samotný graf.

---

### 4. Technical Architecture & Data Strategy *(Technical Reference)*

#### Architecture & Component Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    Full-Viewport Adaptive Bento Layout                  │
├───────────────────────────────────┬─────────────────────────────────────┤
│        Přehled (Dashboard)        │        Přidat (LogDoseScreen)       │
├───────────────────────────────────┼─────────────────────────────────────┤
│ ┌───────────────────────────────┐ │ ┌─────────────────────────────────┐ │
│ │ Bio-Cockpit (Weight 0.12f)    │ │ │ Látka & Poddruh (Weight 0.15f)  │ │
│ │ 52dp Ring + 3 Full-Height KPI │ │ │ 2x Large Scrollable Pill Rows   │ │
│ └───────────────────────────────┘ │ └─────────────────────────────────┘ │
│ ┌───────────────────────────────┐ │ ┌─────────────────────────────────┐ │
│ │ Hero PK Monitor (Weight 0.40f)│ │ │ Dávkování & Zóny (Weight 0.37f) │ │
│ │ 1-Line Scroll Pills           │ │ │ 56dp Steppers + 42sp Readout    │ │
│ │ Full-Bleed PK Canvas (weight) │ │ │ 28dp Threshold Bar + 34dp Chips │ │
│ │ 4-Phase Strip (8dp bar)       │ │ └─────────────────────────────────┘ │
│ └───────────────────────────────┘ │ ┌─────────────────────────────────┐ │
│ ┌──────────────┐ ┌──────────────┐ │ │ PK Simulace (Weight 0.22f)      │ │
│ │ Od poslední  │ │ Dnes užito   │ │ │ Full-Height Curve + 4-Phase Row │ │
│ │ 3x Weighted  │ │ 3x Weighted  │ │ └─────────────────────────────────┘ │
│ │ Item Cards   │ │ Progress Rows│ │ ┌─────────────────────────────────┐ │
│ └──────────────┘ └──────────────┘ │ │ Kontext, Čas & CTA (Weight 0.26f│ │
│ ┌───────────────────────────────┐ │ │ Tall Context & Time Pills       │ │
│ │ 24h Osa (26dp) + 38dp Dock    │ │ │ 48dp Solid Indigo Commit Button │ │
│ └───────────────────────────────┘ │ └─────────────────────────────────┘ │
└───────────────────────────────────┴─────────────────────────────────────┘
```

#### Interactive Component & State Mapping
- **Přepínání aktivních látek na Dashboardu:** Vodorovný posun lišty štítků umožní plynulé klepnutí na libovolnou aktivní látku nebo *Všechny aktivní*, zatímco klepnutí do plochy grafu přepne mezi živou PK křivkou a rozpisem jednotlivých dávek.
- **Karty `TIME SINCE LAST` a `TODAY UNITS`:** Každá ze tří položek uvnitř karty je vykreslena jako vyplněný řádek s `Modifier.weight(1f)`, který reaguje na klepnutí (stránkování oblíbených látek `1/2` nebo přepnutí `UNITS <-> COST`).
- **Krokování a předvolby na stránce Přidat:** Zvětšená tlačítka `-5x`, `-`, `+`, `+5x`, klikací středová hodnota pro ruční zadání, rozšířený posuvník prahových zón i vysoká tlačítka předvoleb okamžitě přepočítávají živou farmakokinetickou křivku a náklady.
