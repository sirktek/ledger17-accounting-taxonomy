Each group is one implementer run. Read design.md (D1–D10) first; touch only the files a group names.
Run Maven via `mvnq` (`~/.local/bin/mvnq`, same args as `./mvnw`). Do not commit or create remotes.
Append a short "Handoff" note to this change directory when a group is done. Naming rule (D2): no
third-party trademark in any name; `NamingTest`'s forbidden tokens must not match anything in `src/`.

## 1. Maven skeleton (template: /home/mkf/dev/sirk-logistics-taxonomy)

- [ ] 1.1 Copy `mvnw`, `mvnw.cmd`, `.mvn/`, `.gitignore` from the template; add `LICENSE.md` (MIT, same holder as the template)
- [ ] 1.2 Write `pom.xml` per D1: `com.ledger17:accounting-taxonomy:${revision}` (`1.0-SNAPSHOT`), `maven.compiler.release` 17, `jena-arq` 5.5.0 compile; test: JUnit 5.11.3, `jena-shacl` 5.5.0, slf4j-simple; copy only the `github-packages` profile with `distributionManagement` → `https://maven.pkg.github.com/sirktek/ledger17-accounting-taxonomy`; no `deployment` profile, no `taxonomy-commons` or its `<repositories>` entry, no Lombok; SCM/url `sirktek/ledger17-accounting-taxonomy`
- [ ] 1.3 Copy `.github/workflows/build_only.yml`; copy `build_tag_and_release.yml` keeping only checkout, JDK setup (`server-id: github`), test, tag, release-version steps and "Publish to GitHub Packages"; remove the Central/GPG steps and secrets; revision `1.${{github.run_number}}`
- [ ] 1.4 Stub `README.md`; create `src/main/resources/ontologies/` and `src/main/java/com/ledger17/taxonomy/accounting/`
- [ ] 1.5 `mvnq -q verify` passes; `mvnq dependency:tree` shows only Jena (+ transitive) at compile scope

## 2. Core module (`src/main/resources/ontologies/accounting-core.ttl`)

- [ ] 2.1 Header comment (jurisdiction-neutral core, mirrors ledger17's ASSET model, isolation and naming rules, "values say what, never where produced"); prefixes `rdf rdfs xsd skos sh dct acct` only
- [ ] 2.2 Carried-over classes per D3/D4: `AssetPurchase`, `Depreciation`, `FiscalYear`, `AccountType` + six account type values incl. `EquityAndLiabilitiesAccount` with `skos:broader` from Equity/Liability (labels/comments written fresh; do not copy ledger17's file)
- [ ] 2.3 `acct:Asset`, `acct:AssetValuation`, `acct:DepreciationGroup` (no instances), annotations `accountType`, `requiresCounterparty`, `appliesToAssetType`, `defaultAssetType`
- [ ] 2.3a Account numbers per D4: `ledgerAccount` (`^[0-9]{4}$`, `@no` "Regnskapskonto"), `accountGroup`, `accountClass`; `sh:sparql` consistency check in `acct:AssetShape`
- [ ] 2.4 `acct:AssetType` + six instances per D4 (notation = ledger17 code)
- [ ] 2.5 Core properties per D5 with applicability; `bookValue`/`taxValue` comments (own wording, no Sirk IRIs)
- [ ] 2.6 `acct:AssetShape`, `acct:AssetValuationShape` per the accounting-core spec, messages `@en` + `@no`
- [ ] 2.7 Syntax check (`riot --validate` if available, else a throwaway load); `grep` shows no `sirktek`, no `schema:`, no `NamingTest` forbidden token

## 3. Norway module (`src/main/resources/ontologies/accounting-no.ttl`)

- [ ] 3.1 Header comment (extends core; statutes cited: regnskapsloven § 1-7, skatteloven § 4-17, § 14-41, § 14-43; NS 4102; SAF-T; codes only, no publisher texts)
- [ ] 3.2 Annotation properties `accountRange`, `defaultDepreciationGroup`, `wealthValueDefault`, `depreciationRate`, `inCodeList`, `excludedCode`, code-list fields, `saftGroupingCategory`/`saftGroupingCode` (D6), each with range, en/no labels, en comment
- [ ] 3.3 `acctno:AccountRange` nodes (`fromAccount`, `toAccount`, `coversGroup`, `wholeGroups`) on asset types and account types per D3/D6; `acct:FiscalYear` comment statement that it is the calendar year in NO
- [ ] 3.4 `acctno:wealthValue`, `acctno:wealthValuePerShare` per D5 with applicability; `wealthValueDefault` per D6 with the § 4-17 rule in comments
- [ ] 3.5 `acctno:DepreciationGroupA`–`J` (notation a–j, labels from the code list nb/en texts, rates, `dct:source`), `defaultDepreciationGroup` h/d
- [ ] 3.6 `acctno:LedgerAccountShape`, `acctno:WealthValueShape`

## 4. Java loader API (`src/main/java/com/ledger17/taxonomy/accounting/`)

- [ ] 4.1 `AccountNumbers` (isValid, accountGroup, accountClass); records `AccountRange` (`contains`), `AccountTypeDef`, `AssetTypeDef`, `PropertyDef`, `DepreciationGroupDef`, `SaftReference` per D8
- [ ] 4.2 `AccountingTaxonomy`: constants, `load(Set<String>)` with module registry `{NO → NO_RESOURCE}`, IllegalArgumentException/IllegalStateException per spec, `jurisdictions()`, `model()`
- [ ] 4.3 `assetTypes()`, `assetType(code)`, `properties()` (excludes annotations), `property(localName)`, `appliesTo`
- [ ] 4.4 `NorwegianRules` per D8 (`depreciationGroups(year)` filters by `inCodeList`; uncovered year → IllegalArgumentException); javadoc on public types; `mvnq -q compile` passes

## 5. Core and API tests (`src/test/java/com/ledger17/taxonomy/accounting/`)

- [ ] 5.1 `ModuleLoadingTest`, `NamingTest`, `LabelsTest`, `CarryOverTest` per D9
- [ ] 5.2 `AssetTypeTest`, `ApplicabilityTest`, `ShaclTest`, `AccountNumbersTest` (pattern + group/class derivation, 5-digit rejected) per D9
- [ ] 5.3 `NorwayRangeTest`, `DepreciationGroupTest` per D9
- [ ] 5.4 Affected test classes green, output clean

## 6. Skatteetaten links: code lists, SAF-T, datakatalog (Norway module + tests)

- [ ] 6.1 Code lists: fetch `src/resources/kodeliste/{2025,2026}/{year}_saldogruppe.xml` from Skatteetaten/skattemeldingen (pin the commit SHA in the NOTICE), store under `src/test/resources/kodeliste/<year>/` with `NOTICE` (Apache-2.0, source URL, SHA); add `acctno:SaldogruppeCodeList2025`/`2026` (identifier, version, path, `rdfs:seeAlso`), `inCodeList` on a–j, `excludedCode "acd"`
- [ ] 6.2 SAF-T: from Skatteetaten/saf-t `Grouping Category Code 2025-2026/CSV/naeringsspesifikasjon.csv` write a codes-only fixture (`GroupingCategory;GroupingCode` for the two balance categories, no descriptions) + NOTICE (source URL, SHA); add `acctno:GroupingCodeList2025` (covers 2025, 2026) and per asset type `saftGroupingCategory`, `saftGroupingCode` per D6; do not fetch or reference the General Ledger Standard Accounts (exclude 1296/1298/1299)
- [ ] 6.3 Datakatalog and reference pages: add the two `acct:Building skos:closeMatch` links (D6), and the D6 `rdfs:seeAlso` links to Skatteetaten pages (formuesskatt and avskrivningssatser rate pages, the buying-assets page) on the terms, plus Skatte-ABC 2025 D-2 on `acctno:SaldogruppeCodeList2025`; never `closeMatch`/`exactMatch` to a web page; comments in our own words
- [ ] 6.3a Capitalisation threshold (sktl § 14-40): `acctno:capitalisationThreshold` and `acctno:minimumLifetimeYears` (range `xsd:integer`, en/no labels, `dct:source` § 14-40) on `SaldogruppeCodeList2025`/`2026` = 30000 / 3; `CapitalisationThresholdTest` per D9
- [ ] 6.4 Tests: `CodeListCoverageTest` (fixture agreement both ways; filing-year coverage `Year.now(ZoneId.of("Europe/Oslo")).getValue() - 1` for `saldogruppe` and `naeringsspesifikasjon`; `depreciationGroups(2019)` throws), `SaftReferenceTest`, `DatakatalogLinkTest` (no network, seeAlso links present, no web page as match target)
- [ ] 6.5 `mvnq verify` green (full suite), test output clean

## 7. Docs and release

- [ ] 7.1 README: purpose and ownership (ledger17 domain knowledge, hosted in the sirktek org), modules and namespaces (D2) with the modules-vs-tags rationale, term tables (D4–D6), value semantics, code-list year policy (how to add a year when the coverage test fails), Maven coordinates and the GitHub Packages repository + `read:packages` token requirement, Java API example (`load(Set.of("NO"))`), consumer mapping (D7), refinement policy (`skos:broader`, codes frozen), not-in-scope list (VAT, non-asset transactions, IFRS links later), attribution for Skatteetaten sources; no third-party trademarks as names
- [ ] 7.2 README "Adopting": Maven dependency, `AccountingTaxonomy.load(Set.of("NO")).model()` example, Jena 5.5 requirement
- [ ] 7.3 Resolve design.md open questions before the first release

## Follow-up changes (other repos, not tasks here)

- **ledger17:** depend on the artifact and map asset types to tax-return object types per year.
- **Sirk:** consumes the artifact through typed fields, not generic properties.
