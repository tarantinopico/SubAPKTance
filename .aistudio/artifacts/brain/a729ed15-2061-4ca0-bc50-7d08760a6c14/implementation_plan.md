# Substance — Profesionální Bio-Analytická & Farmakokinetická Platforma (Finální Specifikace)

Kompletní rozšíření a finální vylepšení aplikace **Substance** na čistý, profesionální analytický nástroj s podporou poddruhů (variant) látek, interaktivním zkoumáním konkrétních bodů v grafech (včetně zobrazení Min, Max, Průměru, Mediánu a směrodatných odchylek), víceúrovňovými analytickými podstránkami pro korelace a rozestupy, volitelnou dobou účinku vs. dobou setrvání v těle, plně upravitelnými výpočetními koeficienty, plnohodnotným **Light Mode** a **Lightweight režimem** (vypnutí efektů) a automatickou kompilací přes **GitHub Actions workflow**.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> Níže jsou potvrzená rozhodnutí z úvodního dotazníku a klíčové architektonické volby před samotnou implementací.

- **Potvrzené rozhodnutí 1 — Poddruhy (Subtypy / Varianty) látek**: Každá látka může mít libovolný počet poddruhů (např. *Kofein → Espresso, V60 Filtr, Matcha, Energy Drink*; *Kratom → Green Maeng Da, White Borneo, Red Bali, Extrakt*). Každý poddruh má vlastní název, barvu/odstín, modifikátor síly/potence (např. `1.25×`), vlastní cenu za jednotku a volitelnou úpravu rychlosti nástupu. V analytice jsou samostatné grafy porovnávající poddruhy mezi sebou.
- **Potvrzené rozhodnutí 2 — Hlavní doba účinku vs. volitelná doba v těle**: Doba trvání účinků (`Active Effect Duration`) a doba setrvání látky v těle (`Body Elimination / Presence Duration`) jsou dvě oddělené hodnoty. Hlavní metrikou na Dashboardu i v grafech je **doba účinku**. Časové parametry nejsou povinné (lze zapnout zjednodušený režim pouze s celkovou dobou účinku, nebo časování u dané látky zcela vypnout).
- **Potvrzené rozhodnutí 3 — 5 podstránek v Analytice (Globálně i Per-Substance)**: Analytické centrum obsahuje v obou režimech (**Celkový přehled** i **Detail konkrétní látky**) 5 přehledných podstránek:
  1. **Přehled & Spotřeba** (denní/týdenní/měsíční trendy, finance, kumulativní vývoj)
  2. **Druhy & Varianty** (srovnání poddruhů látky, podíl spotřeby, cena a účinek podle druhu, cesty podání)
  3. **Korelace & Závislosti** (korelace velikosti dávky na rozestupu od minulé dávky, korelace mezi různými látkami v témže dni, vliv denní doby prvního užití na celkovou denní spotřebu, korelace s vlastním hodnocením/poznámkami)
  4. **Rozestupy & Frekvence** (distribuce rozestupů mezi dávkami, vývoj zkracování/prodlužování rozestupů v čase, kompulzivní redosing index, cirkadiánní hodiny, dny v týdnu, abstinence)
  5. **Farmakokinetika & Tolerance** (aktivní křivky účinku vs. hladina v těle, multi-dose stacking amplifikace, index tolerance a saturace receptorů, spánková hygiena)
- **Potvrzené rozhodnutí 4 — Profesionální vzhled (Dark + Light Mode) & Lightweight přepínač**: Design je přepracován z futuristického stylu na čistý, precizní, vysoce profesionální analytický vzhled (inspirovaný špičkovými zdravotními a finančními analytickými nástroji). Podporuje **Dark Mode**, **Light Mode** a **Lightweight Mode** (1-tap vypnutí všech glow efektů, animací, průhledností a stínů pro maximální plynulost a čistotu).

---

## 1. Overview & Core Concept

- **Co aplikace dělá**: Slouží k přesnému zaznamenávání příjmu látek a doplňků (včetně konkrétního poddruhu/varianty), výpočtu aktivního účinku i zbytkové přítomnosti v těle a hloubkové statistické a korelační analýze.
- **Hlavní přidaná hodnota nové verze**:
  - **Maximální informační hodnota grafů**: Každý graf automaticky vykresluje a vypisuje **Minimum, Maximum, Průměr (Mean), Medián a P90 / Trend**, umožňuje **kliknutím/posouváním prstu (Point Inspector) zobrazit přesnou hodnotu, datum a detail konkrétního bodu** na křivce či sloupci.
  - **Otevřený matematický engine**: Žádná konstanta není schovaná „natvrdo“ v kódu — jak globální algoritmické parametry (rychlost růstu a poklesu tolerance, váha superpozice, prahy pro spánek, rozlišení křivek), tak parametry jednotlivých látek a jejich poddruhů lze upravit v přehledně strukturovaných podstránkách.

---

## 2. User Experience & Visual Design

### Vizuální identita: Čistý Profesionální Design (Dark Mode, Light Mode & Lightweight Mode)
- **Estetický směr**: Čistý, klinicky přesný a přehledný profesionální design s vysokým kontrastem typografie, jasnou hierarchií dat a nulovým vizuálním šumem.
- **Barevná schémata**:
  - **Professional Dark (`--background: #0B0F17`, `--surface: #131B2E`, `--border: #23304A`)**: Neutrální břidlicové pozadí šetrné k očím s ostrými datovými křivkami.
  - **Professional Light (`--background: #F8FAFC`, `--surface: #FFFFFF`, `--border: #E2E8F0`, `--text-primary: #0F172A`)**: Světlý kancelářský/klinický režim s vysokou čitelností na přímém světle.
  - **Lightweight Mode (Úsporný / Rychlý režim)**: Okamžitě vypne veškeré poloprůhledné vrstvy, barevné aury pozadí, pulzující animace a přechody — nahradí je pevnými, okamžitě vykreslovanými kartami s nulovou zátěží GPU.
- **Typografie & Čitelnost**:
  - Nadpisy a popisky: *Space Grotesk* s čistými proporcemi.
  - Číselné hodnoty, osy grafů, statistické ukazatele (Min / Avg / Max) a odpočty: *Roboto Mono* (tabulkové zarovnání číslic).

### Interaktivní Grafy s Point Inspectorem a Statistikami
Každá analytická karta s grafem obsahuje:
1. **Statistickou lištu nad grafem**: Okamžitý přehled `MIN`, `AVG (Průměr)`, `MED (Medián)`, `MAX` a celkového součtu nebo směru trendu (`% změna`).
2. **Vykreslené referenční hladiny přímo v plátně grafu**: Čárkovaná linie průměru (`AVG`), vyznačený bod globálního maxima (`MAX ▲`) a minima (`MIN ▼`), případně bezpečný denní limit.
3. **Dotykový Inspektor Bodu (Interactive Touch Scrubber)**: Kliknutím nebo tažením prstu po libovolném grafu se zobrazí svislý zaměřovač (crosshair) a informační bublina s přesnými daty daného bodu (datum/čas, přesná hodnota v jednotkách, odchylka od průměru, poddruh látky a zúčastněné dávky).

---

## 3. Key Product Decisions & Trade-Offs

- **Rozhodnutí 1: Oddělení Doby účinku (`Active Effect`) od Doby v těle (`Body Presence`) + Nepovinné časování**
  - *Zvolený přístup*: U každé látky lze zvolit režim časování:
    1. **Vypnuto (Bez časového modelování)** — sleduje se pouze množství, cena, rozestupy a statistiky.
    2. **Zjednodušený režim** — zadává se pouze **Hlavní doba účinku** (např. 4 hodiny) a volitelně **Celková doba v těle** (např. 12 hodin), náběh a dojezd se dopočítají automaticky podle globálních poměrů.
    3. **Detailní farmakokinetický režim** — samostatné nastavení Onset, Come-up, Peak, Offset + nezávislá doba eliminace z těla (`Body Presence Hours` / `Half-Life`).
  - *Proč*: Uživatel nemusí znát přesnou farmakokinetiku u každého doplňku, ale u hlavních látek má plnou kontrolu nad tím, jak dlouho látka reálně účinkuje vs. jak dlouho zůstává v organismu.

- **Rozhodnutí 2: Rozdělení Editoru látky a Nastavení do přehledných podstránek (Sub-tabs)**
  - *Zvolený přístup*:
    - **Editor látky (Substance Studio)** je rozdělen do 5 čistých karet/podstránek:
      1. *Základní info & Vzhled* (název, kategorie, jednotka, krok `+/-`, výchozí dávka, rychlé předvolby, cena a kalkulačka balení)
      2. *Druhy / Poddruhy látky* (správa variant dané látky — název druhu, modifikátor síly v %, vlastní cena za jednotku, modifikátor nástupu)
      3. *Hranice dávek (Thresholds)* (5 úrovní intenzity, bezpečný denní limit, doporučený minimální rozestup)
      4. *Doba účinku & Doba v těle* (přepínač povinnosti časování, hlavní doba účinku, volitelná doba v těle, detailní fáze, chování při vrstvení dávek)
      5. *Vlastní pole & Výpočetní koeficienty látky* (vlastní sledované parametry + citlivost tolerance a váha bio-zátěže pro tuto látku)
    - **Nastavení (Control Center)** je rozděleno do 5 samostatných podstránek:
      1. *Vzhled & Výkon* (Light / Dark Mode, Lightweight Mode — vypnutí efektů, animací a průhlednosti, hustota zobrazení)
      2. *Výpočetní Engine & Konstanty* (globální parametry všech výpočtů: koeficient akumulace tolerance, rychlost regenerace receptorů za čistý den, váha překryvu při stackingu, výchozí poměry fází účinku, spánková hodina, délka časových oken)
      3. *Jednotky, Měna & Cesty podání (RoA)* (měna, zaokrouhlování, vlastní jednotky, kontextové štítky, cesty podání a jejich násobitele)
      4. *Historie záznamů (Log CRUD Manager)* (vyhledávání, filtrace podle látky i poddruhu, hromadné i jednotlivé úpravy, duplikace a mazání)
      5. *Data, Zálohy & Demo* (JSON/CSV export a import, nahrání či vymazání demo dat)

- **Rozhodnutí 3: Hloubková korelační a intervalová analytika**
  - *Zvolený přístup*: Přidání specializovaných matematických výpočtů (Pearsonův korelační koeficient $r$, lineární regresní přímka v bodových grafech, matice závislostí) pro zkoumání:
    - **Závislosti velikosti dávky na rozestupu od předchozí dávky** (např. zda kratší rozestup vede k vyšším či nižším dávkám, včetně regresní přímky).
    - **Korelace dávek mezi různými látkami** (zda vyšší příjem látky A v daný den koreluje s vyšším/nižším příjmem látky B).
    - **Vývoje rozestupů v čase** (zda se průměrný rozestup mezi dávkami v posledních týdnech zkracuje nebo prodlužuje).

---

## 4. Technical Architecture & Data Strategy

### Architektura & Hierarchie Modulů

```
┌────────────────────────────────────────────────────────────────────────────┐
│                   SUBSTANCE APPLICATION (Dark / Light / Lightweight)       │
├──────────────┬──────────────┬────────────────────┬────────────┬────────────┤
│  1. Cockpit  │ 2. Log Dose  │  3. Analytics Hub  │ 4. Studio  │ 5. Control │
│  Dashboard   │    Studio    │ (Global & Per-Sub) │ (Sub CRUD) │  Settings  │
└──────┬───────┴──────┬───────┴─────────┬──────────┴─────┬──────┴─────┬──────┘
       │              │                 │                │            │
       ▼              ▼                 ▼                ▼            ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                 5-SUBPAGE ANALYTICS & INTERACTIVE CHART ENGINE             │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────┐ ┌──────┐  │
│  │ A. Přehled & │ │ B. Druhy &   │ │ C. Korelace  │ │D.Rozestup│ │E. PK │  │
│  │   Spotřeba   │ │   Varianty   │ │ & Závislosti │ │&Frekvence│ │& Tělo│  │
│  └──────────────┘ └──────────────┘ └──────────────┘ └──────────┘ └──────┘  │
│  • Point Inspector (Tap/Drag Crosshair) • Min / Avg / Med / P90 / Max Bar  │
└───────────────────────────────────────┬────────────────────────────────────┘
                                        │
                                        ▼
┌────────────────────────────────────────────────────────────────────────────┐
│            CONFIGURABLE COMPUTATIONAL & PHARMACOKINETIC ENGINE             │
│  • Dual-Timeline Curve Solver: Active Effect Duration vs. Body Presence    │
│  • Subtype Potency & Cost Multiplier • Pearson Correlation & Linear Reg.   │
│  • User-Tunable Global & Per-Substance Mathematical Constants              │
└───────────────────────────────────────┬────────────────────────────────────┘
                                        │
                                        ▼
┌────────────────────────────────────────────────────────────────────────────┐
│             ROOM SQLITE DATABASE + DATASTORE PREFERENCES + CI              │
│  • Substances + SubstanceSubtypes + IntakeLogs + CustomFields + RoA        │
│  • GitHub Actions Workflow (.github/workflows/android.yml -> APK artifact) │
└────────────────────────────────────────────────────────────────────────────┘
```

### Datový Model & Rozšíření Schématu
1. **Látka (`SubstanceEntity`)**:
   - Rozšířeno o nastavení časování: `isTimingEnabled` (Boolean — zda vůbec počítat křivky), `useSimpleTiming` (Boolean — zda zadávat jen hlavní dobu účinku), `mainEffectDurationMinutes` (Hlavní doba účinku), `bodyPresenceHours` (Volitelná celková doba setrvání v těle, nezávislá na době účinku), plus individuální koeficienty pro výpočet tolerance (`toleranceBuildRate`, `toleranceDecayRate`) a bio-zátěže (`bioLoadWeight`).
2. **Poddruh / Varianta látky (`SubstanceSubtypeEntity`)**:
   - Vazba na `substanceId`, `name` (např. *Double Espresso*, *Green Maeng Da*), `colorHex`, `potencyMultiplier` (síla účinku oproti základu, např. `1.20`), `onsetMultiplier` (rychlost nástupu), `durationMultiplier` (délka účinku), `customCostPerUnit` (volitelná specifická cena za jednotku tohoto druhu) a `notes`.
3. **Záznam užití (`IntakeLogEntity`)**:
   - Uchovává `subtypeId` a `subtypeName`, aplikovaný `potencyMultiplier`, vyhodnocenou efektivní dávku (`effectiveDoseAmount = doseAmount * bioavailability * potencyMultiplier`), skutečnou dobu účinku a dobu eliminace z těla.
4. **Globální výpočetní konstanty (`UserSettings`)**:
   - Všechny matematické parametry jsou plně editovatelné v Nastavení:
     - Výchozí poměry fází při jednoduchém zadání doby účinku (Onset %, Come-up %, Peak %, Offset %).
     - Koeficient růstu tolerance (`globalToleranceGainFactor`) a denní rychlost poklesu tolerance (`globalToleranceDecayPerDay`).
     - Prahy pro kompulzivní redosing (`compulsiveRedoseThresholdHours`) a váhy pro výpočet celkového Safety Skóre.
     - Přepínače vzhledu: `isLightMode` (Světlý / Tmavý režim), `isLightweightMode` (vypnutí efektů, glow a animací), `showChartMinMaxAvgLines` (vykreslování Min/Max/Avg přímek v grafech).

### Přehled všech Grafů a Vizualizací v 5 Podstránkách Analytiky

#### Režim 1: Celkový Přehled (Global Analytics — 5 podstránek, 22+ detailních grafů)
- **Podstránka 1: Přehled & Finance**
  1. Souhrnná matice KPI (Celková útrata, denní průměr, počet záznamů, aktivní hodiny účinku vs. v těle, Safety skóre).
  2. Skládaná časová osa denní spotřeby (s vyznačeným Min, Max, Průměrem a dotykovým inspektorem každého dne).
  3. Finanční rozpad podle látek (Donut + kumulativní křivka výdajů s 30denní predikcí).
  4. 84denní kalendářová mapa aktivity (s detailem konkrétního dne po kliknutí).
  5. Dynamika 7D vs. 30D (rychlost růstu nebo klesání spotřeby a výdajů u všech látek).
- **Podstránka 2: Druhy & Varianty (Subtypes)**
  6. Rozpad spotřeby podle všech poddruhů napříč látkami (Top 10 nejčastějších druhů, např. *V60 Filtr*, *Green Maeng Da*).
  7. Finanční nákladovost jednotlivých druhů (průměrná cena za dávku a celková útrata podle druhu).
  8. Průměrná efektivní síla a hodnocení podle druhu látky.
  9. Srovnání cest podání (RoA) a kontextových štítků napříč druhy.
- **Podstránka 3: Korelace & Závislosti (Cross-Substance & Interval Correlations)**
  10. Matice souběhu a Pearsonovy korelace dávek mezi látkami ($r \in [-1, +1]$ — zda vyšší dávka látky A vede k vyšší dávce látky B).
  11. Globální bodový graf (Scatter Plot + regresní přímka): **Závislost relativní velikosti dávky na rozestupu od předchozího užití**.
  12. Korelace času prvního ranního užití vs. celkové denní bio-zátěže (s regresní křivkou).
  13. Korelace subjektivního zaměření/nálady v závislosti na celkové denní zátěži a kombinacích látek.
- **Podstránka 4: Rozestupy & Rytmus**
  14. Globální histogram rozestupů mezi dávkami (s Min, Mediánem, Průměrem a Max rozestupem).
  15. Vývoj průměrného rozestupu v čase (Trend zkracování vs. prodlužování pauz mezi dávkami).
  16. 24hodinový cirkadiánní hodinový profil (s vyznačením špiček v průběhu dne).
  17. Profil dnů v týdnu (Pondělí–Neděle: srovnání pracovních dnů a víkendů v dávkách, ceně i zátěži).
  18. Žebříček abstinence a čistých dnů (aktuální pauza vs. historický rekord u každé látky).
- **Podstránka 5: Farmakokinetika & Tělo**
  19. Kompozitní 48hodinová vlna: **Aktivní účinek vs. Zbytková hladina v těle** napříč látkami.
  20. Rozložení všech dávek do 5 prahových zón (Zanedbatelná až Nebezpečná) — jednotlivé dávky vs. navrstvené špičky.
  21. Sledování dodržování bezpečných denních limitů a spánkové hygieny.

#### Režim 2: Detail Konkrétní Látky (Per-Substance Deep Dive — 5 podstránek, 24+ detailních grafů)
- **Podstránka 1: Spotřeba & Jednotky**
  1. Kvarteto hlavních metrik v jednotkách (Průměr na kalendářní den, Průměr na aktivní den, Min/Max/Medián dávky, Celkem jednotek a cena).
  2. Denní spotřeba v jednotkách (Sloupcový graf + 7denní klouzavý průměr + linie Min / Avg / Max / Safe Limit + dotykový inspektor dne).
  3. Kumulativní křivka spotřebovaných jednotek a peněz + 30denní predikční kužel.
  4. Profil spotřeby v jednotkách podle dnů v týdnu (Pondělí–Neděle s Min/Avg/Max).
  5. Statistické rozložení velikosti jednotlivých dávek (Histogram + kvantily Min, P25, Medián, Průměr, P90, Max).
- **Podstránka 2: Druhy této látky (Subtypes Deep Dive)**
  6. Podíl jednotlivých druhů této látky na celkové spotřebě v jednotkách (Donut + tabulka Min/Avg/Max dávky pro každý druh).
  7. Vývoj obliby druhů v čase (Skládaný graf spotřeby podle druhu po dnech).
  8. Srovnání průměrné velikosti dávky, efektivní síly a ceny za dávku mezi jednotlivými druhy této látky.
  9. Korelace druhů látky s kontextem užití a subjektivním hodnocením účinku.
  10. Rozpad cest podání (RoA) pro tuto látku a její druhy.
- **Podstránka 3: Korelace & Závislosti této látky**
  11. **Závislost velikosti dávky na délce předchozího rozestupu** (Bodový Scatter graf + lineární regresní přímka + korelační koeficient $r$ — ukazuje, zda po delší pauze následuje menší či větší dávka, nebo zda dochází k rychlému navyšování při krátkých rozestupech).
  12. **Závislost velikosti dalšího rozestupu na velikosti předchozí dávky** (ukazuje, zda větší dávka skutečně prodlouží dobu do dalšího užití).
  13. **Vliv času první ranní dávky na celkovou denní spotřebu této látky** (Scatter graf + regresní přímka).
  14. **Korelace této látky s ostatními látkami** (se kterými látkami se nejčastěji kombinuje a jak se vzájemně ovlivňují velikosti dávek).
  15. Analýza vlastních polí (Custom Fields — např. vliv plnosti žaludku nebo šarže na hodnocení účinku a velikost dávky).
- **Podstránka 4: Rozestupy & Rytmus této látky**
  16. Detailní distribuce rozestupů mezi dávkami (`<1h`, `1–2h`, `2–4h`, `4–8h`, `8–16h`, `16–24h`, `>24h`) s vyznačeným doporučeným minimálním rozestupem.
  17. **Křivka vývoje rozestupů mezi dávkami v čase** (ukazuje každou mezeru mezi dávkami chronologicky včetně klouzavého průměru, minima a maxima).
  18. 24hodinový cirkadiánní profil této látky (přesné hodiny užití + průměrný čas první a poslední dávky).
  19. Historie abstinenčních pauz (srovnání posledních 10 nejdelších pauz bez této látky).
- **Podstránka 5: Účinek, Tělo & Historie**
  20. **Dvouvrstvá 7denní simulace: Křivka aktivního účinku vs. Křivka přítomnosti v těle** (s dotykovým inspektorem libovolné hodiny).
  21. Analýza vrstvení dávek (Multi-Dose Stacking — kolikrát redosing navýšil špičku do vyššího prahového pásma a o kolik minut posunul peak).
  22. Křivka odhadované tolerance a saturace receptorů (s výpočtem dnů do plného resetu podle upravitelných koeficientů).
  23. Dodržování večerního limitu pro čistý spánek (zbytkový účinek vs. zbytková látka v těle v čase večerky).
  24. Kompletní filtrovatelná tabulka záznamů této látky a jejích druhů s okamžitou úpravou.

### GitHub Actions CI/CD Workflow & Testy
- V kořenovém adresáři projektu bude vytvořen workflow soubor `.github/workflows/android-build.yml`, který při každém pushi nebo pull requestu (i při ručním spuštění `workflow_dispatch`):
  1. Nastaví JDK 17 a Android SDK.
  2. Spustí jednotkové a Robolectric testy (`gradle :app:testDebugUnitTest`).
  3. Zkompiluje Debug APK (`gradle :app:assembleDebug`).
  4. Nahraje hotový instalační balíček `Substance-debug.apk` jako stažitelný GitHub Actions Artifact.
- Všechny nové výpočty (poddruhy, oddělená doba účinku a doba v těle, korelace velikosti dávky na rozestupu, statistiky Min/Max/Avg/Median, přepínání Light/Dark/Lightweight režimu) budou pokryty automatickými testy.
