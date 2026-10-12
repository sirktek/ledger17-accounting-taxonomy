
## Handoff: Group 1
Changed: pom.xml, mvnw, mvnw.cmd, .mvn/, .gitignore, LICENSE.md, README.md (stub), .github/workflows/{build_only,build_tag_and_release}.yml,
src/main/java/com/ledger17/taxonomy/accounting/package-info.java, src/main/resources/ontologies/.gitkeep, OntologySyntaxTest.
Verified: `mvnq -q verify` green (OntologySyntaxTest passes with zero .ttl files); dependency:tree compile scope is Jena only.
Notes: build_only.yml copied verbatim from the template (runs PMD `pmd:check` as in the template). Release workflow publishes to GitHub Packages only, revision 1.${run_number}.
Unverified: CI workflows not run. Deviation: used aggregate `junit-jupiter` 5.11.3 instead of separate api/engine.

## Handoff: Group 2
Changed: src/main/resources/ontologies/accounting-core.ttl (new); removed ontologies/.gitkeep.
Verified: `mvnq -q test -Dtest=OntologySyntaxTest` green; grep finds no sirktek, schema:, or forbidden token.
Notes: an ontology header node `<http://ledger17.com/ontology/accounting>` (typed rdfs:Resource, with labels/comment) is included; LabelsTest must treat it as a normal subject (it has en/no labels). SHACL SPARQL constraints use full IRIs (no sh:prefixes). AssetShape also has pattern checks on ledgerAccount/accountGroup/accountClass. taxValue appliesTo lists five types (all except BankAccount). Blank-node sh:property/sh:sparql nodes carry no labels (shape internals).
Unverified: SHACL behaviour (ShaclTest is a later group).

## Handoff: Group 3
Changed: src/main/resources/ontologies/accounting-no.ttl (new).
Verified: `mvnq -q test -Dtest=OntologySyntaxTest` green; no `nrs`, sirktek or schema: in the file.
Notes: AccountRange nodes are named IRIs (`acctno:RangeIntangible`, `RangeBankAccount1920/1950/1960`, `RangeAssetAccount` ...) with en/no labels and en comment. `wealthValueDefault` and `defaultDepreciationGroup` values are resources (`acct:taxValue`, `acctno:DepreciationGroupH`), not notations. `WealthValueShape` uses sh:sparql. `acct:FiscalYear` gets an extra `rdfs:comment` in this file. Declared (no instance data): CodeList class and fields, `inCodeList`, `excludedCode`, thresholds, `saftGroupingCategory` (set per type) and `saftGroupingCode` (no values). `incomeYear` has no domain (shared with SchemaVersion in group 7). SchemaVersion/SchemaElementMapping terms, seeAlso and closeMatch are left to groups 6/7.
Unverified: SHACL behaviour, label/range tests (later groups).

## Handoff: Group 4
Changed: AccountNumbers, AccountRange, AccountTypeDef, AssetTypeDef, PropertyDef, DepreciationGroupDef, SaftReference, AccountingTaxonomy, NorwegianRules under src/main/java/com/ledger17/taxonomy/accounting/.
Verified: `mvnq -q compile` and `OntologySyntaxTest` green. No behavioural tests yet (later groups); loader code not exercised at runtime.
Notes: assetTypes()/accountTypes() order is hardcoded lists (D4 order) since RDF has no order. properties() = rdf:Property subjects with rdfs:domain acct:Asset or acct:AssetValuation (annotations excluded); jurisdiction derived from namespace. appliesTo returns false for an unknown property. accountRanges/saft/defaults return empty/null-category for unknown codes. depreciationGroups uses saldogruppe CodeLists' incomeYear and group inCodeList; throws until group 6 data exists. Rates parsed with new BigDecimal(lexical form), e.g. "30.0". AccountType instances must be typed `a acct:AccountType` (assumed, not checked at runtime).

## Handoff: Group 5
Changed (tests): src/test/java/com/ledger17/taxonomy/accounting/{TestSupport,ModuleLoadingTest,NamingTest,LabelsTest,CarryOverTest,AssetTypeTest,ApplicabilityTest,ShaclTest,AccountNumbersTest,NorwayRangeTest,DepreciationGroupTest}.java; src/test/resources/simplelogger.properties (slf4j-simple level error).
Fix in ontologies (defect exposed by ShaclTest): Jena 5.5 rejects two `sh:message` values on an `sh:sparql` constraint ("More than one match"). Final form (see the messageNo fix below and the spec): one `sh:message`@en plus `acct:messageNo`@no on every constraint.
Verified: `mvnq -q test -Dtest=<the eleven group classes>` green, no stderr output.
Notes: NamingTest builds its forbidden token from parts; its isolation scan ignores `#` comment lines (core.ttl header comment says "no schema.org terms"). Wealth/year literals in SHACL tests must be xsd:integer. Account-type ranges are read directly from the model in NorwayRangeTest (no API for them). Rates compared with compareTo.
Unverified/deferred to group 6: `depreciationGroups(year)` returning a-j for 2025/2026, code list tests, thresholds, SAF-T codes; datakatalog and schema-element checks (group 7). ModuleLoadingTest asserts no datakatalog IRI in core-only but does not yet assert datakatalog presence for NO.

## Handoff: Group 5 fix (messageNo)
- Declared `acct:messageNo` (rdf:Property, range rdf:langString, en/no labels, en comment, no domain) in accounting-core.ttl; replaced all `rdfs:comment ...@no` shape messages with `acct:messageNo` in core (8) and no (2); each shape keeps one `sh:message @en`.
- Added `ShaclTest.messagesInBothLanguages`; the listed test classes were run (see result in report).

## Handoff: Group 6
Changed: accounting-no.ttl (SaldogruppeCodeList2025/2026 with identifier/version/path/excludedCode "acd"/threshold 30000 and lifetime 3/dct:source 14-40/seeAlso; inCodeList on groups a-j; Building closeMatch x2; reference-page seeAlso links; Skatte-ABC 2025 D-2 on the 2025 list); src/test/resources/kodeliste/{NOTICE,2025,2026} (fixtures pinned to skattemeldingen commit 9784809838c8c910b494f91f03d25a7c4fe44a36, SHA-256 in NOTICE); tests CodeListCoverageTest, CapitalisationThresholdTest, DatakatalogLinkTest; DepreciationGroupTest gained codeListYearsReturnGroupsAToJ.
Verified: the five group test classes green; full `mvnq -q test` run also executed (see result in report). Coverage warning goes through System.Logger.
Notes: NorwegianRules unchanged (depreciationGroups already works from the data). Skatte-ABC D-2 URL found from the live 2025 index page. 2026 list has no Skatte-ABC link (only a 2025 one exists in v1). The NO file now has statements about acct:taxValue/acct:DepreciationGroup/acct:Building (seeAlso, closeMatch), which are ignored by LabelsTest as they are core terms.
Unverified: SAF-T grouping list fixture and SaftReferenceTest (later group).
