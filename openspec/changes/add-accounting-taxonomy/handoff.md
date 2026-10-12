
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
