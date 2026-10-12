## Context

- **ledger17 holds the domain knowledge.**
  - ledger17 persists `assetType`, `partyRef`, `valuations[{year, wealthValue | wealthValuePerShare}]` and
    `disposedAt` as JSON. This vocabulary keeps those keys as local names.
  - Its six asset type codes: `INTANGIBLE` (1000-1099), `BUILDING` (1100-1199), `EQUIPMENT` (1200-1299,
    "Utstyr/kjøretøy"), `FINANCIAL_FIXED_ASSET` (1300-1399, `partyRef` names the investee),
    `FINANCIAL_INSTRUMENTS` (1800-1899, mark-to-market, no wealthValue) and `BANK_ACCOUNT` (1920/1950/1960,
    the balance is the value).
  - A valuation row holds exactly one of `wealthValue` and `wealthValuePerShare`; the per-share total is
    perShare × the year-end share count derived from the ledger. `disposedAt` drops the asset from every
    fiscal year ending on or after that date.
  - ledger17 has an internal ontology. The terms this vocabulary needs are carried over (D3). No persisted
    data uses its IRIs, so moving the terms breaks no data.
- **ledger17 account numbers are 4 digits, no sub-accounts.** Sub-ledger detail lives elsewhere, not in
  longer account numbers. Class 2 as a whole is treated as the financing side (see D3).
- **"Kontonummer" usually means a bank account number** (11 digits). The `@no` label of `ledgerAccount` is
  therefore "Regnskapskonto", never "Kontonummer".
- **Skatteetaten sources (verified 2026-10-12):**
  - `github.com/Skatteetaten/skattemeldingen` (Apache-2.0), `src/resources/kodeliste/<year>/`:
    - `<year>_saldogruppe.xml` exists for 2021–2026. 2025 and 2026 both have codes a–j plus `acd` (a, c
      and d for hydropower, treated together).
    - The list gives labels (nb, nn, en, se) and `skattemessigLevetid`, but no rates. Rates come from the
      statute (skatteloven § 14-43).
    - `<year>_formuesobjekttypeUpersonligSkattepliktig.xml` exists for 2021–2027.
  - The XSDs (`naeringsspesifikasjon_v6`, `skattemeldingUpersonlig_v5`) reference the code lists via
    `eksternKodeliste="/formuesOgInntektsskatt/skattemelding/2025_saldogruppe.xml"`.
    - They carry `skatt:begrepsreferanse` datakatalog UUIDs for fast eiendom (`9c33fd0b-2964-11e6-b2bc-96405985e0fa`),
      fast eiendom som formuesobjekt (`41b9c8b8-973b-11e9-bed2-005056821322`) and eiendomstype
      (`9c33fd09-2964-11e6-b2bc-96405985e0fa`).
    - They carry none for formuesverdi, saldogruppe, avskrivningssats or skattemessig verdi. Those fields
      are typed `Beløp`/`Prosent` or by code list only.
  - `github.com/Skatteetaten/saf-t`:
    - `General Ledger Standard Accounts` (2- and 4-character) are Copyright Regnskap Norge AS 2018 and
      "may only be used in connection with SAF-T mapping". They are **not used** here; the 2-character
      group equals the first two digits of the NS 4102 range we already carry.
    - `Grouping Category Code 2025-2026/CSV/naeringsspesifikasjon.csv` holds category
      `balanseverdiForAnleggsmiddel` (codes 1000–1395) and `balanseverdiForOmloepsmiddel` (1400–1950).
- **Sirk** (its shared base taxonomy) has a divergent copy
  (`AssetValueEntry`, `wealthTaxValue`, `ledgerAccount`, `bookValue`, `taxValue`, `depreciationGroup`,
  `DepreciationGroup` A–J, `jurisdiction`). It opts these into domain classes via `schema:domainIncludes`,
  which Sirk renders generically; that is the marketplace/DPP leak path. Sirk prod stores nothing under
  these terms.

## Goals / Non-Goals

**Goals:**
- Publish ledger17's asset vocabulary so that ledger17 adopts it with no data migration and asset
  management systems receive the same terms in the sync payload.
- Split it into a jurisdiction-neutral core and a Norway module that only Norwegian consumers load.
- Fail closed: Sirk-style generic loaders and non-NO consumers never see Norwegian tax terms.
- Minimal Java API, Java 17.

**Non-Goals:**
- Producing values (depreciation runs, formuesverdi calculation), the sync protocol, persistence.
- VAT/MVA and non-asset transaction types (invoices, salary, bank transactions, transfers). They stay
  internal to ledger17.
- Other countries' modules. The loading model makes them additive.
- IFRS links. A later item: `skos:closeMatch` from core terms to IFRS taxonomy elements (IAS 16, IAS 38,
  IFRS 9).
- Code changes in ledger17 or Sirk (follow-ups in tasks.md).

## Decisions

### D1 Artifact, repo, release
Coordinates and package are ledger17's: `com.ledger17:accounting-taxonomy`, package
`com.ledger17.taxonomy.accounting`. Repo `sirktek/ledger17-accounting-taxonomy` (public, like the other
taxonomy repos), MIT, `<revision>1.0-SNAPSHOT`, CI releases `1.${run_number}`.

Publishing goes to GitHub Packages only. There is no Maven Central publishing, because sirktek's Central
organization has hit its monthly release limit. From `/home/mkf/dev/sirk-logistics-taxonomy` copy:
- mvnw and `.mvn`;
- the pom's `github-packages` profile, with `distributionManagement` →
  `https://maven.pkg.github.com/sirktek/ledger17-accounting-taxonomy` (repository id `github`);
- from `.github/workflows/build_tag_and_release.yml`: checkout, JDK 21 setup with `server-id: github`,
  test, git tag, set and verify the release version, and the "Publish to GitHub Packages" step
  (`./mvnw deploy -P github-packages`, `GITHUB_TOKEN`);
- `build_only.yml` as the PR workflow.

Do not copy:
- the `deployment` profile (javadoc, source, gpg, central-publishing);
- the Central/GPG secrets or the Central steps;
- the `taxonomy-commons` dependency and its GitHub Packages `<repositories>` entry;
- Lombok.

`maven.compiler.release` 17, CI on JDK 21. Compile dependency `org.apache.jena:jena-arq:5.5.0`. Test
dependencies: JUnit 5.11.3, `jena-shacl` 5.5.0, slf4j-simple.

### D2 Modules, files, namespaces
| Module | File (`src/main/resources/ontologies/`) | Namespace | Prefix |
|---|---|---|---|
| core | `accounting-core.ttl` | `http://ledger17.com/ontology/accounting#` | `acct:` |
| Norway | `accounting-no.ttl` | `http://ledger17.com/ontology/accounting/no#` | `acctno:` |

The NO module imports nothing at runtime; the loader always adds core. The NO module may assert
statements about core terms (for example an `acctno:accountRange` on `acct:Building`), and those
statements exist only when NO is loaded. A future country `xx` gets `accounting-xx.ttl` /
`.../accounting/xx#` / `acctxx:`.

**Why modules, not per-term jurisdiction tags:**
- Tags fail open. Every consumer must remember to filter, and a generic loader that forgets (Sirk's
  `domainIncludes` renderer is exactly that) shows Norwegian tax terms everywhere.
- Tags put different countries' statutory values (rates, ranges) on the same resource, so a reader must
  disambiguate every triple.
- One country's statute changes would churn a shared file.

With modules, a module that is not loaded is not in the graph, the IRI shows the origin, and countries
change independently. Sirk's existing `jurisdiction` property is dropped in its cleanup follow-up.

**Naming:**
- No third-party trademark appears in any IRI, prefix, file name, Java name or label. In particular, no
  name refers to the Norwegian accounting standards foundation's mark.
- Statutes and standards (regnskapsloven, skatteloven § 4-17/§ 14-40/§ 14-41/§ 14-43, NS 4102 (Standard Norge),
  SAF-T) are cited factually in `rdfs:comment` and `dct:source` only.
- `acctno:accountRange` holds NS 4102 account numbers only; labels are our own, and no NS 4102 or SAF-T
  description texts are shipped.

**Isolation (tests):**
- No `sirktek` IRI.
- No `schema:` namespace and no `schema:domainIncludes`.
- Core uses only the prefixes `rdf rdfs xsd skos sh dct acct`; NO additionally uses `acctno`.
- Field domains use `rdfs:domain`; applicability uses `acct:appliesToAssetType`.

### D3 Carry-over mapping (ledger17's internal ontology → this vocabulary)
| ledger17's internal term | New term | Note |
|---|---|---|
| `AssetPurchase` | `acct:AssetPurchase` | Acquisition event; cost basis of `bookValue` |
| `Depreciation` | `acct:Depreciation` | Depreciation event |
| `FiscalYear` | `acct:FiscalYear` | NO: the calendar year (regnskapsloven § 1-7), stated in `accounting-no.ttl` |
| `AccountType` | `acct:AccountType` | — |
| ledger17's `Asset` (account type, 1000-1999) | `acct:AssetAccount` + `acctno:accountRange` 1000-1999 | Renamed because `acct:Asset` is the asset entity |
| ledger17's `Liability` (2000-2999) | `acct:EquityAndLiabilitiesAccount` + 2000-2999 | ledger17's whole-class-2 "liability" maps to the parent, so ledger17 logic that reads all of class 2 needs no change |
| `Equity` | `acct:EquityAccount` + 2000-2099, `skos:broader` the parent | — |
| — | `acct:LiabilityAccount` + 2100-2999, `skos:broader` the parent | New, narrower: liabilities proper |
| `Revenue` | `acct:RevenueAccount` + 3000-3999 | — |
| `Expense` | `acct:ExpenseAccount` + 4000-8999 | As ledger17; class 8 also holds financial income (later refinement) |
| `AccountShape`, `accountNumber` `^[1-8][0-9]{3}$` | `acct:ledgerAccount` (core `^[0-9]{4}$`) + `acctno:LedgerAccountShape` (`^[1-8][0-9]{3}$`) | Classes 1-8 are NS 4102-specific, so that rule lives in NO |
| `SalesInvoice`, `PurchaseInvoice`, `BankTransaction`, `SalaryPayment`, `InterAccountTransfer`, `SalesInvoiceShape` | — | Out of scope; stay internal to ledger17 |
| `VatSettlement`, `VatTreatment`/`InputVat`/`OutputVat`/`NoVat`, `MvaPeriod`, `MvaFrequency` + values | — | VAT out of scope; stays internal |
| ledger17's asset type codes and asset/valuation JSON keys | D4/D5 terms | Codes and local names identical |

Here the account ranges are data in the NO module.

### D4 Core vocabulary (`acct:`)
**Classes:**
- `acct:Asset`: an asset as an accounting system records it.
- `acct:AssetType`.
- `acct:AssetValuation`: the values of one asset at the end of one fiscal year.
- `acct:DepreciationGroup`: a tax depreciation pool defined by a jurisdiction module. Core has no values.
- `acct:AccountType`, with values `acct:AssetAccount`, `acct:EquityAndLiabilitiesAccount` (en "Equity and
  liabilities", no "Egenkapital og gjeld"), `acct:EquityAccount`, `acct:LiabilityAccount`,
  `acct:RevenueAccount` and `acct:ExpenseAccount`.
  - `EquityAccount` and `LiabilityAccount` have `skos:broader acct:EquityAndLiabilitiesAccount`. The
    parent is the financing side of the balance sheet.
  - Equity is not a liability: it is the residual interest (IAS 32; regnskapsloven separates egenkapital
    from gjeld). For formuesskatt, gjeld is deductible and egenkapital is not.
  - No asset-side parent is added. `acct:AssetAccount` already is the whole of class 1, so there is no gap.
    A later split (fixed 10-13 / current 14-19) would add children with `skos:broader acct:AssetAccount`.
  - The type names keep the `Account` suffix, because `acct:Asset` is the asset entity.
- `acct:AssetPurchase`, `acct:Depreciation`, `acct:FiscalYear`.

**Account numbers** (core, chart-neutral 4-digit hierarchy: class, group, account):
- `acct:ledgerAccount` is an `xsd:string` matching `^[0-9]{4}$` (four digits).
- `acct:accountGroup` (`@no` "Kontogruppe") is the first two digits; `acct:accountClass` (`@no`
  "Kontoklasse") is the first digit. Both are `xsd:string`.
- Derivation is positional, so no lookup table is needed:
  - SHACL messages: one `sh:message`@en per constraint plus `acct:messageNo`@no (annotation property).
    Jena 5.5's SHACL parser throws "More than one match" for two `sh:message` values on an `sh:sparql`
    constraint (property shapes accept them); the same rule applies to every constraint so a UI reads
    the Norwegian text from one place.
  - In RDF, `acct:AssetShape` uses `sh:sparql` to check that, when present, `accountGroup` =
    `SUBSTR(ledgerAccount, 1, 2)` and `accountClass` = `SUBSTR(ledgerAccount, 1, 1)`.
  - In Java, `AccountNumbers.accountGroup("1205") == "12"` and `accountClass("1205") == "1"`.
- 5+ digit sub-accounts are not allowed, because ledger17 rejects them. If a future consumer needs them,
  that is a new property, not a looser pattern.

**Annotations:**
- `acct:accountType` (AssetType → AccountType; all six → `acct:AssetAccount`).
- `acct:requiresCounterparty` (`xsd:boolean`).
- `acct:appliesToAssetType` (on a property; repeatable; absent means all types).
- `acct:defaultAssetType` (a consumer annotates its own `rdfs:Class`; unused inside this repo).

**Asset types:** `skos:notation` is ledger17's exact persisted code.

| Instance | notation | Label en / no | Counterparty |
|---|---|---|---|
| `acct:Intangible` | INTANGIBLE | Intangible asset / Immateriell eiendel | no |
| `acct:Building` | BUILDING | Building / Bygning | no |
| `acct:Equipment` | EQUIPMENT | Equipment and vehicles / Utstyr og kjøretøy | no |
| `acct:FinancialFixedAsset` | FINANCIAL_FIXED_ASSET | Financial fixed asset / Finansielt anleggsmiddel | yes |
| `acct:FinancialInstruments` | FINANCIAL_INSTRUMENTS | Securities / Verdipapir | no |
| `acct:BankAccount` | BANK_ACCOUNT | Bank account / Bankkonto | no |

A later refinement (Land, Vehicle, …) is backwards compatible: new instances with `skos:broader` pointing
at the v1 value. The v1 codes never change.

### D5 Properties; local names = ledger17 JSON keys
**Core, asset-level** (`rdfs:domain acct:Asset`):

| Property | Range | Applies to | Origin |
|---|---|---|---|
| `acct:assetType` | `acct:AssetType` (by notation in JSON) | all | ledger17 |
| `acct:partyRef` | `xsd:string` (the investee party's id) | FinancialFixedAsset | ledger17 |
| `acct:disposedAt` | `xsd:date` | all | ledger17 |
| `acct:valuations` | `acct:AssetValuation`, one per year | all | ledger17 |
| `acct:ledgerAccount` | `xsd:string` `^[0-9]{4}$`, an account in the chart a jurisdiction module defines | all | new |
| `acct:accountGroup`, `acct:accountClass` | `xsd:string`, derived from `ledgerAccount` (D4) | all | new |
| `acct:depreciationGroup` | `acct:DepreciationGroup` | Intangible, Building, Equipment | new |

`name` and `identifiers` are excluded. They are generic registry fields mastered by the asset system, not
accounting vocabulary.

**Core, `acct:AssetValuation` fields:**

| Field | Label no | Applies to | Semantics |
|---|---|---|---|
| `acct:year` | År | all | `xsd:integer`; identifies the `acct:FiscalYear` (its end year) |
| `acct:bookValue` | Bokført verdi | all | Carrying amount: cost from `acct:AssetPurchase` less accumulated `acct:Depreciation` and write-downs |
| `acct:taxValue` | Skattemessig verdi | all except BankAccount | Tax written-down value that the depreciation rate applies to |

**Norway module** (`rdfs:domain acct:AssetValuation`, ledger17 JSON keys):

| Field | Label no | Applies to | Semantics |
|---|---|---|---|
| `acctno:wealthValue` | Formuesverdi | Intangible, Building, Equipment, FinancialFixedAsset | Total formuesverdi at 31.12, entered manually. Mutually exclusive with `wealthValuePerShare`. |
| `acctno:wealthValuePerShare` | Formuesverdi per aksje | FinancialFixedAsset | Per share; total = per-share value × year-end holding |

Amounts are decimals in the reporting currency (NOK in NO). Comments say what a value means, never
where it is produced.

### D6 Norway module (`acctno:`)
**Annotations on core asset types:**

| Instance | `acctno:accountRange` (NS 4102) | `acctno:defaultDepreciationGroup` | `acctno:wealthValueDefault` | SAF-T grouping category |
|---|---|---|---|---|
| Intangible | 1000-1099 | — | `acct:taxValue` | balanseverdiForAnleggsmiddel |
| Building | 1100-1199 | h | — | balanseverdiForAnleggsmiddel |
| Equipment | 1200-1299 | d | `acct:taxValue` | balanseverdiForAnleggsmiddel |
| FinancialFixedAsset | 1300-1399 | — | — | balanseverdiForAnleggsmiddel |
| FinancialInstruments | 1800-1899 | — | `acct:bookValue` | balanseverdiForOmloepsmiddel |
| BankAccount | 1920, 1950, 1960 | — | `acct:bookValue` | balanseverdiForOmloepsmiddel |

`acctno:accountRange` points to an `acctno:AccountRange` node:
- `acctno:fromAccount` and `acctno:toAccount` are 4-digit `xsd:string`s (`"1000"`, `"1099"`; a single
  account has from = to).
- `acctno:coversGroup` names each 2-digit group the range touches (Intangible `10`; Equipment `12`; each
  BankAccount range `19`).
- `acctno:wholeGroups true` is set when the range is exactly whole groups (false for the BankAccount
  accounts).

Account types carry ranges the same way:
- AssetAccount 1000-1999 (class 1);
- EquityAndLiabilitiesAccount 2000-2999 (class 2), with EquityAccount 2000-2099 (group 20) and
  LiabilityAccount 2100-2999 (groups 21-29);
- RevenueAccount 3000-3999;
- ExpenseAccount 4000-8999.

Sibling ranges do not overlap, and every child range lies inside its parent's.

Ranges are ledger17's: deferred tax 1070 sits inside Intangible, land 1140-1159 inside Building, vehicles
1220-1249 inside Equipment.

**Formuesverdi rules** (in the comments on `acctno:wealthValue` and `acctno:wealthValueDefault`):
- `acct:appliesToAssetType` on `acctno:wealthValue` and `wealthValuePerShare` states where manual input
  is accepted.
- `acctno:wealthValueDefault` states which value counts when none is given:
  - driftsmidler (Intangible, Equipment): `taxValue` at 31.12 (skatteloven § 4-17);
  - Building: no default, because Skatteetaten values property under skatteloven chapter 4;
  - FinancialFixedAsset: no default (manual per share or total);
  - FinancialInstruments and BankAccount: `bookValue`; no manual formuesverdi.

**Depreciation groups:**
- `acctno:DepreciationGroupA` … `acctno:DepreciationGroupJ` are `a acct:DepreciationGroup`, with
  `skos:notation` "a"–"j" equal to the code list's `tekniskNavn`.
- Labels follow the code list's nb/en texts.
- `acctno:depreciationRate` gives the statutory maximum declining-balance rate in percent per skatteloven
  § 14-43: 30/20/24/20/14/12/5/4/2/10. `dct:source` cites § 14-41/§ 14-43. Checked on 2026-10-12 against
  Skatteetaten's current rate page: all ten match.
- Group h also has higher statutory rates for some buildings (livestock buildings in agriculture, and
  simple buildings and structures with a short expected life). v1 does not model them; the comment on
  group h says so in our own words and points at the rate page.

**Capitalisation threshold** (skatteloven § 14-40, per income year, on each `saldogruppe` `CodeList`):
- `acctno:capitalisationThreshold` (`xsd:integer`, NOK) and `acctno:minimumLifetimeYears`
  (`xsd:integer`). An asset is a driftsmiddel that must be capitalised when its cost is at least the
  threshold and its expected life is at least the minimum; below either it is expensed.
- 2025 and 2026: 30 000 and 3. The threshold was 15 000 up to income year 2023 and 30 000 from 2024;
  earlier years are not covered (v1 starts at 2025).
- `dct:source` cites § 14-40; `rdfs:seeAlso` Skatteetaten's page on buying assets for the business.
- Per year, not a constant, because the amount changes by statute (as it did for 2024).

**Code lists (year-versioned):**
- `acctno:CodeList` instances, e.g. `acctno:SaldogruppeCodeList2025`, carry:
  - `acctno:codeListName "saldogruppe"`;
  - `acctno:incomeYear 2025`;
  - `acctno:codeListPath "/formuesOgInntektsskatt/skattemelding/2025_saldogruppe.xml"` (as the XSD's
    `eksternKodeliste` names it);
  - `acctno:codeListIdentifier` (the list's `identifikator` UUID) and `acctno:codeListVersion`
    (`versjonsnummer`);
  - `rdfs:seeAlso` the file in Skatteetaten/skattemeldingen;
  - for 2025 also `rdfs:seeAlso` Skatte-ABC 2025 chapter D-2 (driftsmidler, avskrivning), whose URL is
    year-specific. A later year gets that year's Skatte-ABC link;
  - `acctno:capitalisationThreshold` and `acctno:minimumLifetimeYears` (above).
- Each group value has `acctno:inCodeList` for every year it appears in.
- Codes deliberately not modeled are listed with `acctno:excludedCode` on the code list (`acd`).
- v1 covers 2025 and 2026. Adding a year adds one `CodeList` instance plus `inCodeList` triples, never a
  new term.
- The SAF-T grouping list is modeled the same way as `acctno:GroupingCodeList2025` (name
  `naeringsspesifikasjon`, `incomeYear` 2025 and 2026, from Skatteetaten/saf-t
  `Grouping Category Code 2025-2026`).

**SAF-T grouping codes** (Skatteetaten's næringsspesifikasjon list; the SAF-T standard accounts are not
referenced):
- `acctno:saftGroupingCategory` on each asset type.
- `acctno:saftGroupingCode` (repeatable literals) lists every `naeringsspesifikasjon` code within the
  type's range, except 1296/1298/1299 (negative gain/loss, timber and agriculture accounts, not assets).
  The codes are literals, so `acctno:GroupingCodeList2025` (incomeYear 2025, 2026) carries the year coverage.
- Codes only, no description texts.
- Later item: grouping code → saldogruppe defaults (1205→d, 1238→c, 1280→a, …).

**Datakatalog links** (SKOS, asserted in the NO file):
- `acct:Building skos:closeMatch <https://data.skatteetaten.no/web/datakatalog/begrep/9c33fd0b-2964-11e6-b2bc-96405985e0fa>`
  (fast eiendom).
- `acct:Building skos:closeMatch <…/41b9c8b8-973b-11e9-bed2-005056821322>` (fast eiendom som
  formuesobjekt).
- Use `skos:exactMatch` only when Skatteetaten's definition and ours coincide in scope; otherwise use
  `closeMatch`. SKOS mapping targets are only concept URIs that appear in the XSDs'
  `skatt:begrepsreferanse` attributes; today that is the two above.
- formuesverdi, saldogruppe, avskrivningssats and skattemessig verdi have no datakatalog concept (none in
  the XSDs, and Skatteetaten has no single catalogue entry for them). They get `rdfs:seeAlso` to
  Skatteetaten web pages instead. Web pages are not concept identifiers and are never `closeMatch` or
  `exactMatch` targets.
- `rdfs:seeAlso` targets (current-year pages, so attached to the term, not to a year):
  - `acctno:wealthValue` and `acctno:wealthValueDefault` → `https://www.skatteetaten.no/satser/formuesskatt/`;
  - `acct:DepreciationGroup` (asserted in the NO file) and `acctno:depreciationRate` →
    `https://www.skatteetaten.no/satser/avskrivningssatser/`;
  - `acct:taxValue` (asserted in the NO file), `acctno:capitalisationThreshold` and
    `acctno:minimumLifetimeYears` →
    `https://www.skatteetaten.no/bedrift-og-organisasjon/skatt/skattemelding-naringsdrivende/fradrag/eiendeler-utstyr-eiendom/bedriftens-eiendeler/kjope-eiendeler-til-bedriften/`.
  - The year-specific Skatte-ABC page sits on `acctno:SaldogruppeCodeList2025` (code lists above).
- Comments paraphrase these sources in our own words; no text is copied.

**Schema element mappings** (in addition to the `rdfs:seeAlso` pages; asserted in the NO file):
- Skatteetaten's XSDs in `github.com/Skatteetaten/skattemeldingen` (`src/resources/xsd/`) give stable,
  versioned element names. A mapping to them is a mapping to a data element, not to a concept, so it is
  never a SKOS match.
- `acctno:SchemaVersion` nodes, one per schema and version:
  - `acctno:schemaName`, `acctno:schemaVersion`, `acctno:schemaNamespace` (the XSD's target namespace),
    `acctno:incomeYear` (same property as on `CodeList`);
  - `dct:source` the XSD at a release tag:
    `https://github.com/Skatteetaten/skattemeldingen/blob/v1.62.115/src/resources/xsd/<file>.xsd`
    (tag commit `9784809838c8c910b494f91f03d25a7c4fe44a36`, 2026-10-10).
  - v1 instances:

    | Node | File | Income year | Code lists it names |
    |---|---|---|---|
    | `acctno:NaeringsspesifikasjonV6` | `naeringsspesifikasjon_v6_ekstern.xsd` | 2025 | `2025_*` |
    | `acctno:NaeringsspesifikasjonV7` | `naeringsspesifikasjon_v7_ekstern.xsd` | 2026 | `2026_*` |
    | `acctno:SkattemeldingUpersonligV5` | `skattemeldingUpersonlig_v5_ekstern.xsd` | 2025 | `2025_*` |
    | `acctno:SkattemeldingUpersonligV6` | `skattemeldingUpersonlig_v6_ekstern.xsd` | 2026 | `2026_*` |

    The income year is read from the year prefix of the code lists each XSD names in
    `skatt:eksternKodeliste` (v6/v5 name only `2025_` lists, v7/v6 only `2026_`).
- `acctno:SchemaElementMapping` nodes: `acctno:mapsTerm` (the term), `acctno:inSchema` (a
  `SchemaVersion`), `acctno:schemaElement` (path literal `<complexType>/<element>`; the element may be
  inherited through an `xsd:extension` base) and optionally `acctno:openingSchemaElement`.
- Mapping table (identical paths in both versions of each schema):

  | Term | Schema | `schemaElement` | Note |
  |---|---|---|---|
  | `acct:depreciationGroup` | naeringsspesifikasjon v6, v7 | `SaldoavskrevetAnleggsmiddel/saldogruppe` | value is a `saldogruppe` code (`<year>_saldogruppe.xml`) |
  | `acctno:depreciationRate` | naeringsspesifikasjon v6, v7 | `SaldoavskrevetAnleggsmiddel/avskrivningssats` | the rate used; ours is the statutory maximum |
  | `acct:taxValue` | naeringsspesifikasjon v6, v7 | `SaldoavskrevetAnleggsmiddel/utgaaendeVerdi` | inherited from `AnleggsmiddelFelles`; `openingSchemaElement` `SaldoavskrevetAnleggsmiddel/inngaaendeVerdi` = the previous year's `taxValue` |
  | `acctno:wealthValue` | skattemeldingUpersonlig v5, v6 | `Formuesobjekt/formuesverdi` | in the formuesobjekt context; the object's kind is `Formuesobjekt/formuesobjekttype` |

- Not mapped, recorded here only: the other saldo components of `SaldoavskrevetAnleggsmiddel`
  (`nyanskaffelse`, `paakostning`, `offentligTilskudd`, `justeringAvInngaaendeMva`,
  `vederlagVedRealisasjonOgUttak`, `nedskrevetVerdiAvUtskiltAnleggsmiddel`,
  `grunnlagForAvskrivningOgInntektsfoering`, `aaretsAvskrivning`, `aaretsInntektsfoeringAvNegativSaldo`,
  `gevinstOverfoertTilGevinstOgTapskonto`, `tapOverfoertTilGevinstOgTapskonto`). The closing balance moves
  with all of them, so this vocabulary states no formula linking opening value, depreciation and closing
  value. `formuesverdi` in naeringsspesifikasjon occurs only in a power-plant calculation and is not
  mapped.
- None of the mapped elements carries `skatt:begrepsreferanse`. `avskrivningssats`'s wrapper type points
  at the generic "prosent" concept, which is not a concept for the depreciation rate and gets no link.
- No per-group rate cap is modeled: the XSDs constrain `avskrivningssats` only as a generic percentage
  (v7 bounds `Prosent` to 0–100, v6 not at all). The per-group maxima come from skatteloven § 14-43 and
  are already `acctno:depreciationRate`.
- Fixtures: the four XSDs with every `xsd:annotation` removed (element and type names, attributes and code
  list references only, no documentation texts), under `src/test/resources/xsd/` with a NOTICE (Apache-2.0,
  tag, commit SHA and the original files' SHA-256).

**NO SHACL:**
- `acctno:LedgerAccountShape` (`sh:targetSubjectsOf acct:ledgerAccount`): pattern `^[1-8][0-9]{3}$`.
- `acctno:WealthValueShape` (`sh:targetClass acct:AssetValuation`): not both `wealthValue` and
  `wealthValuePerShare`.

Applicability and range membership are API checks, not SHACL, because consumer data may be partial.

### D7 Consumer mapping (README; implemented elsewhere)
**ledger17:**
- Loads core + NO. Stored data needs no mapping: enum code = `skos:notation`, JSON keys = local names.
- A JSON key resolves via `property(localName)` across the loaded modules; local names are unique across
  modules (tested).

**Asset management systems (first: Sirk):**
- Consume the artifact through typed fields, not generic properties. Modules are picked by the
  organization's country (`NO` → `load(Set.of("NO"))`, others → core only).
- Domain taxonomies annotate their own classes: `logistics:RealEstate acct:defaultAssetType
  acct:Building`; `machine:Machine`, `ict:Hardware`, `furniture:Furniture` and vehicles →
  `acct:Equipment`.
- This is a class annotation, not a property declaration, so Sirk's category-property inheritance never
  surfaces accounting terms.

**Resolving the artifact:**
- GitHub Packages Maven requires authentication even for public packages. Sirk services already read
  sirktek GitHub Packages.
- ledger17's build (lab17tech org: CI and developer `settings.xml`) needs a token with `read:packages`
  on sirktek packages and the repository
  `https://maven.pkg.github.com/sirktek/ledger17-accounting-taxonomy`. This is a prerequisite for
  ledger17's adoption.

### D8 Java API (`com.ledger17.taxonomy.accounting`)
```java
public final class AccountingTaxonomy {
  public static final String CORE_NAMESPACE = "http://ledger17.com/ontology/accounting#";
  public static final String NO_NAMESPACE = "http://ledger17.com/ontology/accounting/no#";
  public static final String CORE_RESOURCE = "/ontologies/accounting-core.ttl";
  public static final String NO_RESOURCE = "/ontologies/accounting-no.ttl";
  public static final Set<String> SUPPORTED_JURISDICTIONS = Set.of("NO");   // ISO 3166-1 alpha-2
  public static AccountingTaxonomy load(Set<String> jurisdictions); // core + modules; Set.of() = core only;
      // unknown/lowercase code -> IllegalArgumentException naming the supported set;
      // missing/invalid resource -> IllegalStateException
  public Set<String> jurisdictions();
  public Model model();                                 // union incl. shapes; callers must not mutate
  public List<AssetTypeDef> assetTypes();               // D4 order
  public List<AccountTypeDef> accountTypes();           // incl. skos:broader
  public Optional<AssetTypeDef> assetType(String code); // by notation
  public List<PropertyDef> properties();                // asset-level + valuation fields of loaded modules
  public Optional<PropertyDef> property(String localName);
  public boolean appliesTo(String propertyLocalName, String assetTypeCode);
  public NorwegianRules norway();                       // IllegalStateException if NO not loaded
}
public final class NorwegianRules {
  public List<AccountRange> accountRanges(String assetTypeCode);
  public Optional<AssetTypeDef> assetTypeForAccount(String account);
  public List<AccountTypeDef> accountTypesForAccount(String account);     // most specific first, then broader
  public Optional<String> defaultDepreciationGroup(String assetTypeCode); // notation
  public Optional<String> wealthValueDefault(String assetTypeCode);       // property local name
  public SortedSet<Integer> coveredYears(String codeListName);
  public List<DepreciationGroupDef> depreciationGroups(int incomeYear);   // IllegalArgumentException if not covered
  public SaftReference saft(String assetTypeCode);
}
public final class AccountNumbers {                    // core, no lookup tables
  public static boolean isValid(String account);       // ^[0-9]{4}$
  public static String accountGroup(String account);   // first two digits; IllegalArgumentException if invalid
  public static String accountClass(String account);   // first digit
}
public record AccountRange(String from, String to, Set<String> groups, boolean wholeGroups) {
  public boolean contains(String account); }
public record AccountTypeDef(String uri, String labelEn, String labelNo, String broader /*uri or null*/) {}
public record AssetTypeDef(String uri, String code, String labelEn, String labelNo, String comment,
    boolean requiresCounterparty) {}
public record PropertyDef(String uri, String name, String labelEn, String labelNo, String comment,
    String domain, String range, Set<String> appliesTo /*codes; empty = all*/, String jurisdiction /*null = core*/) {}
public record DepreciationGroupDef(String uri, String notation, String labelEn, String labelNo, BigDecimal rate) {}
public record SaftReference(String groupingCategory, List<String> groupingCodes) {}
```
Nothing else: no service layer and no Sirk-style generic `PropertyDefinition`.

### D9 Tests
- `ModuleLoadingTest`:
  - `load(Set.of())` contains no `acctno:` subject, predicate or object, no datakatalog IRI and no SAF-T
    grouping annotation; `property("wealthValue")` is empty and `norway()` throws.
  - `load(Set.of("NO"))` contains all of these.
  - `"SE"`, `"no"` and `null` are rejected.
- `NamingTest`:
  - no IRI, prefix, label, resource path or Java source name matches a forbidden token: the name of ledger17's internal ontology, `(?i)\bnrs\b`;
  - no `sirktek` and no `schema:`;
  - namespaces are as in D2;
  - no property named `jurisdiction`.
- `LabelsTest`: every non-shape subject in each file has exactly one `@en` and one `@no` label and an
  `@en` comment.
- `CarryOverTest`: every D3 target term exists; no VAT/MVA or invoice terms exist.
- `AssetTypeTest`: six values, notations = ledger17 enum names, D4 table.
- `NorwayRangeTest`:
  - D6 table;
  - `assetTypeForAccount`: 1080→INTANGIBLE, 1150→BUILDING, 1230→EQUIPMENT, 1350→FINANCIAL_FIXED_ASSET,
    1810→FINANCIAL_INSTRUMENTS, 1950→BANK_ACCOUNT, 1930→empty;
  - `accountTypesForAccount("2050")` = [Equity, EquityAndLiabilities], `("2400")` = [Liability,
    EquityAndLiabilities];
  - sibling ranges do not overlap, child ranges lie inside the parent's, and each range's `coversGroup`
    equals the groups derived from its bounds.
- `DepreciationGroupTest`: a–j with rates 30/20/24/20/14/12/5/4/2/10.
- `CodeListCoverageTest`. Test fixtures are the fetched 2025 and 2026 `saldogruppe.xml` (Apache-2.0,
  attributed in `src/test/resources/kodeliste/NOTICE`) and a codes-only extract of the grouping list.
  The test checks:
  - every `acctno:CodeList` has a fixture, and its identifier and version match;
  - every fixture code is a group notation with `inCodeList` for that year, or an `excludedCode`, and
    the reverse;
  - **year coverage:** when income year `Year.now(Europe/Oslo) - 1` (the year being filed) has no
    `saldogruppe` or grouping code list, the test logs a WARNING naming the missing year and passes.
    A missing year must never fail the build: from 1 January every unrelated build and hotfix release
    would be blocked until the new lists are added. `depreciationGroups(2019)` throws (that part is
    not time-dependent and does fail).
- `SaftReferenceTest`:
  - every grouping code on a type is in the fixture and inside that type's range;
  - every fixture code in a balance category inside a range is listed, except 1296/1298/1299.
- `DatakatalogLinkTest`: `skos:closeMatch`/`exactMatch` targets match
  `^https://data\.skatteetaten\.no/web/datakatalog/begrep/[0-9a-f-]{36}$` (so no `www.skatteetaten.no` page is
  ever a match target); Building has the two closeMatches; the D6 `rdfs:seeAlso` links exist; core-only
  loads have none of them. No network access.
- `SchemaElementTest` (no network): parses each fixture XSD; every `SchemaVersion` has a fixture whose
  target namespace equals `schemaNamespace`; every `schemaElement` and `openingSchemaElement` resolves (the
  complex type exists and declares the element directly or through an `xsd:extension` base); every
  mapped term has a mapping for each version listed in the D6 table; no mapping object is a
  `skos:closeMatch`/`exactMatch` and no mapped term has a SKOS match to a GitHub URL; filing-year coverage
  as in `CodeListCoverageTest` (both schemas need a `SchemaVersion` for income year `Year.now - 1`).
- `CapitalisationThresholdTest`: every `saldogruppe` `CodeList` has exactly one
  `capitalisationThreshold` and one `minimumLifetimeYears`; 2025 and 2026 are 30000 and 3; both cite
  § 14-40.
- `ApplicabilityTest`: every row of the D5 tables via `appliesTo`; every ledger17 JSON key resolves via
  `property()`.
- `ShaclTest`:
  - a ledger17-shaped asset and valuation conforms;
  - both wealth fields → violation (NO);
  - `ledgerAccount` "123" or "12050" → violation (core);
  - `ledgerAccount` "1205" with `accountGroup` "13" → violation (core);
  - "0123" → accepted core-only, violation with NO.
- `AccountNumbersTest`: `accountGroup("1205")` = "12" and `accountClass("1205")` = "1"; "123", "12050"
  and "12a5" → IllegalArgumentException.

### D10 Planned later step (not part of this change)
The repo may move to the lab17tech org, and Maven Central publishing may be added under a separate
`com.ledger17` Sonatype organization. Coordinates, Java package and IRIs are already ledger17's, so
neither step changes them:
- a move only updates the repo URL, SCM and `distributionManagement`;
- Central adds a publishing profile (sources, javadoc, signing) and secrets.

## Risks / Trade-offs

- [Consumers on an older Jena] → the API uses only stable Model/RDFDataMgr calls.
- [Repo lives in the sirktek org while names are ledger17's] → intentional; a move (D10) changes no
  names.
- [GitHub Packages needs auth] → consumer prerequisite in D7.
- [The time-dependent coverage test turns red in January] → intended. The fix is a new `CodeList` year
  once Skatteetaten publishes the list (historically mid-year for the following filing). Until then a
  maintainer can add the year with the previous list's codes and a comment, then verify later.
- [Statutory rates change] → rates are documentation with `dct:source`; the statute is the source of
  truth.
- [Third-party code lists have use terms] → only Skatteetaten codes and NS 4102 numbers, no texts; the
  Regnskap Norge standard accounts are not used.
- ledger17's own use of SAF-T reference data is its own responsibility.
- [Coarse v1 types] → refinement via `skos:broader` is additive.
- [Renaming a term breaks consumers and ledger17's stored JSON] → terms frozen per major version.

## Migration Plan

New artifact; nothing to migrate (no persisted IRIs, Context). Release 1.x from main. ledger17 then
adopts with no data migration. Sirk removes its copies only after adopting this artifact.

## Open Questions

None. Resolved: the Equipment label is `@no` "Utstyr og kjøretøy" / `@en` "Equipment and vehicles";
a missing filing year is a warning, not a build failure.
