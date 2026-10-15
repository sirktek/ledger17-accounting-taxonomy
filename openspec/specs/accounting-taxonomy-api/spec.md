# accounting-taxonomy-api Specification

## Purpose
TBD - created by archiving change add-accounting-taxonomy. Update Purpose after archive.
## Requirements
### Requirement: Published artifact
The project SHALL publish `com.ledger17:accounting-taxonomy` to GitHub Packages
(`https://maven.pkg.github.com/sirktek/ledger17-accounting-taxonomy`) only, compiled for Java 17, with
Apache Jena as its only compile-scope dependency and with no dependency on any Sirk taxonomy artifact.

#### Scenario: Dependency tree
- **WHEN** the compile-scope dependency tree is listed
- **THEN** it contains only Jena and its transitive dependencies

### Requirement: Module loading by jurisdiction
`AccountingTaxonomy.load(Set<String> jurisdictions)` SHALL load `accounting-core.ttl` plus the module of
each requested ISO 3166-1 alpha-2 code from `SUPPORTED_JURISDICTIONS` (v1: `NO`). An empty set SHALL
load core only. An unknown, lowercase or null code SHALL throw `IllegalArgumentException` naming the
supported set; a missing or unparseable resource SHALL throw `IllegalStateException`.

#### Scenario: Core only fails closed
- **WHEN** `load(Set.of())` is called
- **THEN** the model contains no `acctno:` term, `property("wealthValue")` is empty and `norway()` throws `IllegalStateException`

#### Scenario: Norway
- **WHEN** `load(Set.of("NO"))` is called
- **THEN** `jurisdictions()` is `{NO}`, `property("wealthValue")` is present and `norway()` returns the rules view

#### Scenario: Unsupported jurisdiction
- **WHEN** `load(Set.of("SE"))` is called
- **THEN** it throws `IllegalArgumentException`

### Requirement: Asset type and property views
The API SHALL expose `assetTypes()` in design order, `assetType(code)` by notation, `properties()` and
`property(localName)` across the loaded modules, and `appliesTo(property, assetTypeCode)` from
`acct:appliesToAssetType` (absent means all). Local names SHALL be unique across modules.

#### Scenario: Applicability
- **WHEN** `appliesTo("partyRef", "BUILDING")` and `appliesTo("partyRef", "FINANCIAL_FIXED_ASSET")` are called
- **THEN** they return false and true

### Requirement: Norwegian rules view
`norway()` SHALL expose `accountRanges(code)`, `assetTypeForAccount(String)`, `accountTypesForAccount(String)`,
`defaultDepreciationGroup(code)`, `wealthValueDefault(code)`, `coveredYears(codeListName)`,
`depreciationGroups(incomeYear)` and `saft(code)`. `depreciationGroups` SHALL throw
`IllegalArgumentException` for an income year that no `saldogruppe` code list covers.

#### Scenario: Account lookup
- **WHEN** `assetTypeForAccount` is called with "1080", "1150", "1230", "1350", "1810", "1950" and "1930"
- **THEN** it returns INTANGIBLE, BUILDING, EQUIPMENT, FINANCIAL_FIXED_ASSET, FINANCIAL_INSTRUMENTS, BANK_ACCOUNT and empty

#### Scenario: Uncovered year
- **WHEN** `depreciationGroups(2019)` is called
- **THEN** it throws `IllegalArgumentException`

