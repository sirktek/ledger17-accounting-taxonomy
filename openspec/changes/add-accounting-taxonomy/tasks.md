Each group is one implementer run. Read design.md (D1–D10) and the delta specs first; touch only the files a group names.
Run Maven via `mvnq` (`~/.local/bin/mvnq`, same args as `./mvnw`). Do not push; the coordinator commits per group.
Append a short "Handoff" note (what changed, what is unverified) to `handoff.md` in this change directory when a group
is done. Naming rule (D2): no third-party trademark in any name; `NamingTest`'s forbidden tokens must not match anything
in `src/`. Paths below are relative to the repo root; `<pkg>` is `com/ledger17/taxonomy/accounting`.

### Group 1: Maven skeleton

Files: `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/`, `.gitignore`, `LICENSE.md`, `README.md`, `.github/workflows/`,
`src/main/resources/ontologies/`, `src/main/java/<pkg>/`, `src/test/java/<pkg>/OntologySyntaxTest.java`.
Template (read only): `/home/mkf/dev/sirk-logistics-taxonomy`.
Tests: `mvnq -q verify`; `mvnq dependency:tree`.

- [x] 1.1 Copy `mvnw`, `mvnw.cmd`, `.mvn/`, `.gitignore` from the template; add `LICENSE.md` (MIT, same holder as the template)
- [x] 1.2 Write `pom.xml` per D1: `com.ledger17:accounting-taxonomy:${revision}` (`1.0-SNAPSHOT`), `maven.compiler.release` 17, `jena-arq` 5.5.0 compile; test: JUnit 5.11.3, `jena-shacl` 5.5.0, slf4j-simple; copy only the `github-packages` profile with `distributionManagement` → `https://maven.pkg.github.com/sirktek/ledger17-accounting-taxonomy`; no `deployment` profile, no `taxonomy-commons` or its `<repositories>` entry, no Lombok; SCM/url `sirktek/ledger17-accounting-taxonomy`
- [x] 1.3 Copy `.github/workflows/build_only.yml`; copy `build_tag_and_release.yml` keeping only checkout, JDK setup (`server-id: github`), test, tag, release-version steps and "Publish to GitHub Packages"; remove the Central/GPG steps and secrets; reword the "Verify Release Version" message (no Maven Central); revision `1.${{github.run_number}}`
- [x] 1.4 Stub `README.md`; create `src/main/resources/ontologies/` and `src/main/java/<pkg>/`
- [x] 1.5 `OntologySyntaxTest`: parses every `*.ttl` under `src/main/resources/ontologies/` with Jena RIOT and fails on any syntax error (passes with zero files); later groups use it as their syntax gate
- [x] 1.6 `mvnq -q verify` passes; `mvnq dependency:tree` shows only Jena (+ transitive) at compile scope

### Group 2: Core module

Files: `src/main/resources/ontologies/accounting-core.ttl`.
Tests: `mvnq -q test -Dtest=OntologySyntaxTest`.

- [x] 2.1 Header comment (jurisdiction-neutral core, mirrors ledger17's ASSET model, isolation and naming rules, "values say what, never where produced"); prefixes `rdf rdfs xsd skos sh dct acct` only
- [x] 2.2 Carried-over classes per D3/D4: `AssetPurchase`, `Depreciation`, `FiscalYear`, `AccountType` + six account type values incl. `EquityAndLiabilitiesAccount` with `skos:broader` from Equity/Liability (labels/comments written fresh; do not copy ledger17's file)
- [x] 2.3 `acct:Asset`, `acct:AssetValuation`, `acct:DepreciationGroup` (no instances), annotations `accountType`, `requiresCounterparty`, `appliesToAssetType`, `defaultAssetType`
- [x] 2.4 Account numbers per D4: `ledgerAccount` (`^[0-9]{4}$`, `@no` "Regnskapskonto"), `accountGroup`, `accountClass`; `sh:sparql` consistency check in `acct:AssetShape`
- [x] 2.5 `acct:AssetType` + six instances per D4 (notation = ledger17 code; `acct:Equipment` labels "Equipment and vehicles" / "Utstyr og kjøretøy")
- [x] 2.6 Core properties per D5 with applicability; `bookValue`/`taxValue` comments (own wording, no Sirk IRIs)
- [x] 2.7 `acct:AssetShape`, `acct:AssetValuationShape` per the accounting-core spec, messages `@en` + `@no`
- [x] 2.8 `OntologySyntaxTest` green; `grep` shows no `sirktek`, no `schema:`, no `NamingTest` forbidden token

### Group 3: Norway module

Files: `src/main/resources/ontologies/accounting-no.ttl`.
Tests: `mvnq -q test -Dtest=OntologySyntaxTest`.

- [x] 3.1 Header comment (extends core; statutes cited: regnskapsloven § 1-7, skatteloven § 4-17, § 14-40, § 14-41, § 14-43; NS 4102; SAF-T grouping codes; codes only, no publisher texts)
- [x] 3.2 Annotation properties `accountRange`, `defaultDepreciationGroup`, `wealthValueDefault`, `depreciationRate`, `inCodeList`, `excludedCode`, code-list fields, `saftGroupingCategory`/`saftGroupingCode` (D6), each with range, en/no labels, en comment
- [x] 3.3 `acctno:AccountRange` nodes (`fromAccount`, `toAccount`, `coversGroup`, `wholeGroups`) on asset types and account types per D3/D6; `acct:FiscalYear` comment statement that it is the calendar year in NO
- [x] 3.4 `acctno:wealthValue`, `acctno:wealthValuePerShare` per D5 with applicability; `wealthValueDefault` per D6 with the § 4-17 rule in comments
- [x] 3.5 `acctno:DepreciationGroupA`–`J` (notation a–j, own labels, rates, `dct:source`; group h comment paraphrases the higher special rates that are not modelled), `defaultDepreciationGroup` h/d
- [x] 3.6 `acctno:LedgerAccountShape`, `acctno:WealthValueShape`
- [x] 3.7 `OntologySyntaxTest` green; no `NamingTest` forbidden token

### Group 4: Java loader API

Files: `src/main/java/<pkg>/` (`AccountNumbers`, `AccountRange`, `AccountTypeDef`, `AssetTypeDef`, `PropertyDef`,
`DepreciationGroupDef`, `SaftReference`, `AccountingTaxonomy`, `NorwegianRules`, `package-info.java`).
Tests: `mvnq -q compile`; `mvnq -q test -Dtest=OntologySyntaxTest`.

- [x] 4.1 `AccountNumbers` (isValid, accountGroup, accountClass); records `AccountRange` (`contains`), `AccountTypeDef`, `AssetTypeDef`, `PropertyDef`, `DepreciationGroupDef`, `SaftReference` per D8
- [x] 4.2 `AccountingTaxonomy`: constants, `load(Set<String>)` with module registry `{NO → NO_RESOURCE}`, IllegalArgumentException/IllegalStateException per spec, `jurisdictions()`, `model()`
- [x] 4.3 `assetTypes()`, `assetType(code)`, `accountTypes()`, `accountTypesForAccount(String)`, `assetTypeForAccount(String)`, `properties()` (excludes annotations), `property(localName)`, `appliesTo`
- [x] 4.4 `NorwegianRules` per D8 (`depreciationGroups(year)` filters by `inCodeList`; uncovered year → IllegalArgumentException); javadoc on public types; compile passes

### Group 5: Core and API tests

Files: `src/test/java/<pkg>/` (the test classes below); fixes only, if a test exposes a defect, in
`src/main/resources/ontologies/*.ttl` and `src/main/java/<pkg>/` (name each fix in the handoff).
Tests: `mvnq -q test -Dtest='OntologySyntaxTest,ModuleLoadingTest,NamingTest,LabelsTest,CarryOverTest,AssetTypeTest,ApplicabilityTest,ShaclTest,AccountNumbersTest,NorwayRangeTest,DepreciationGroupTest'`.

- [ ] 5.1 `ModuleLoadingTest`, `NamingTest`, `LabelsTest`, `CarryOverTest` per D9
- [ ] 5.2 `AssetTypeTest`, `ApplicabilityTest`, `ShaclTest`, `AccountNumbersTest` (pattern + group/class derivation, 5-digit rejected) per D9
- [ ] 5.3 `NorwayRangeTest`, `DepreciationGroupTest` per D9
- [ ] 5.4 The group's tests green, output clean (no stderr noise)

### Group 6: Skatteetaten code lists, thresholds and reference links

Files: `src/main/resources/ontologies/accounting-no.ttl`, `src/main/java/<pkg>/NorwegianRules.java`,
`src/test/resources/kodeliste/`, `src/test/java/<pkg>/{CodeListCoverageTest,CapitalisationThresholdTest,DatakatalogLinkTest}.java`.
Tests: `mvnq -q test -Dtest='OntologySyntaxTest,DepreciationGroupTest,CodeListCoverageTest,CapitalisationThresholdTest,DatakatalogLinkTest'`.

- [ ] 6.1 Code lists: fetch `src/resources/kodeliste/{2025,2026}/{year}_saldogruppe.xml` from Skatteetaten/skattemeldingen (pin the commit SHA in the NOTICE), store under `src/test/resources/kodeliste/<year>/` with `NOTICE` (Apache-2.0, source URL, SHA); add `acctno:SaldogruppeCodeList2025`/`2026` (identifier, version, path, `rdfs:seeAlso`), `inCodeList` on a–j, `excludedCode "acd"`
- [ ] 6.2 Datakatalog and reference pages: add the two `acct:Building skos:closeMatch` links (D6), and the D6 `rdfs:seeAlso` links to Skatteetaten pages (formuesskatt and avskrivningssatser rate pages, the buying-assets page) on the terms, plus Skatte-ABC 2025 D-2 on `acctno:SaldogruppeCodeList2025`; never `closeMatch`/`exactMatch` to a web page; comments in our own words
- [ ] 6.3 Capitalisation threshold (sktl § 14-40): `acctno:capitalisationThreshold` and `acctno:minimumLifetimeYears` (range `xsd:integer`, en/no labels, `dct:source` § 14-40) on `SaldogruppeCodeList2025`/`2026` = 30000 / 3
- [ ] 6.4 Tests: `CodeListCoverageTest` for `saldogruppe` (fixture agreement both ways; filing-year coverage `Year.now(ZoneId.of("Europe/Oslo")).getValue() - 1` logs a WARNING and passes when missing; `depreciationGroups(2019)` throws), `CapitalisationThresholdTest`, `DatakatalogLinkTest` (no network, seeAlso links present, no web page as match target) per D9
- [ ] 6.5 The group's tests green, output clean (a coverage WARNING is the only allowed log line)

### Group 7: SAF-T grouping codes and schema element mappings

Files: `src/main/resources/ontologies/accounting-no.ttl`, `src/main/java/<pkg>/{SaftReference,NorwegianRules}.java`,
`src/test/resources/saft/`, `src/test/resources/xsd/`,
`src/test/java/<pkg>/{SaftReferenceTest,SchemaElementTest,CodeListCoverageTest}.java`.
Tests: `mvnq -q test -Dtest='OntologySyntaxTest,CodeListCoverageTest,SaftReferenceTest,SchemaElementTest'`, then the
full suite once: `mvnq verify`.

- [ ] 7.1 SAF-T: from Skatteetaten/saf-t `Grouping Category Code 2025-2026/CSV/naeringsspesifikasjon.csv` write a codes-only fixture (`GroupingCategory;GroupingCode` for the two balance categories, no descriptions) to `src/test/resources/saft/` + NOTICE (source URL, SHA); add `acctno:GroupingCodeList2025` (covers 2025, 2026) and per asset type `saftGroupingCategory`, `saftGroupingCode` per D6; do not fetch or reference the General Ledger Standard Accounts (exclude 1296/1298/1299)
- [ ] 7.2 Schema element mappings (D6): copy `naeringsspesifikasjon_v6/v7_ekstern.xsd` and `skattemeldingUpersonlig_v5/v6_ekstern.xsd` from Skatteetaten/skattemeldingen tag `v1.62.115` with all `xsd:annotation` stripped to `src/test/resources/xsd/` + NOTICE (Apache-2.0, tag, commit SHA, original SHA-256); add the four `acctno:SchemaVersion` nodes and the `SchemaElementMapping` nodes of the D6 table (classes and properties with en/no labels)
- [ ] 7.3 Tests: extend `CodeListCoverageTest` with the `naeringsspesifikasjon` grouping list (same WARNING rule); `SaftReferenceTest`, `SchemaElementTest` per D9
- [ ] 7.4 The group's tests green; then `mvnq verify` green (full suite), test output clean

### Group 8: Docs

Files: `README.md`.
Tests: `mvnq -q test -Dtest=NamingTest` (README must not trip the naming rule if the test covers it; otherwise `grep -niE '\bnrs\b' README.md` is empty).

- [ ] 8.1 README: purpose and ownership (ledger17 domain knowledge, hosted in the sirktek org), modules and namespaces (D2) with the modules-vs-tags rationale, term tables (D4–D6), value semantics, code-list year policy (how to add a year when the coverage test warns), Maven coordinates and the GitHub Packages repository + `read:packages` token requirement, Java API example (`load(Set.of("NO"))`), consumer mapping (D7), refinement policy (`skos:broader`, codes frozen), not-in-scope list (VAT, non-asset transactions, IFRS links later), attribution for Skatteetaten sources; no third-party trademarks as names
- [ ] 8.2 README "Adopting": Maven dependency, `AccountingTaxonomy.load(Set.of("NO")).model()` example, Jena 5.5 requirement

## Follow-up changes (other repos, not tasks here)

- **ledger17:** depend on the artifact and map asset types to tax-return object types per year.
- **Sirk:** consumes the artifact through typed fields, not generic properties.
