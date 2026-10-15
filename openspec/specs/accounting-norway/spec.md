# accounting-norway Specification

## Purpose
TBD - created by archiving change add-accounting-taxonomy. Update Purpose after archive.
## Requirements
### Requirement: Norway module file and namespace
The artifact SHALL ship `ontologies/accounting-no.ttl` defining its terms in namespace
`http://ledger17.com/ontology/accounting/no#` with prefix `acctno:`. It SHALL extend core terms only by
adding statements, never by redefining them, and SHALL use no prefixes other than core's plus `acctno`.

#### Scenario: Statements about core terms are module-scoped
- **WHEN** core is loaded without the Norway module
- **THEN** no core asset type carries `acctno:accountRange`, SAF-T references or `skos:closeMatch`

### Requirement: Account ranges
The Norway module SHALL give each asset type its NS 4102 account ranges via `acctno:accountRange`
(Intangible 1000-1099, Building 1100-1199, Equipment 1200-1299, FinancialFixedAsset 1300-1399,
FinancialInstruments 1800-1899, BankAccount 1920, 1950, 1960) and each account type its range (Asset
1000-1999, EquityAndLiabilities 2000-2999, Equity 2000-2099, Liability 2100-2999, Revenue 3000-3999,
Expense 4000-8999). Each range SHALL state 4-digit bounds (`acctno:fromAccount`, `acctno:toAccount`) and
name the 2-digit groups it touches (`acctno:coversGroup`). It SHALL
define `acctno:LedgerAccountShape` requiring `acct:ledgerAccount` to match `^[1-8][0-9]{3}$`.

#### Scenario: Account type ranges nest
- **WHEN** all account type ranges are compared
- **THEN** sibling ranges do not overlap, Equity and Liability lie inside EquityAndLiabilities, and each range's `coversGroup` equals the groups derived from its bounds

#### Scenario: Whole class 2
- **WHEN** account 2050 and account 2400 are resolved to account types
- **THEN** both resolve under `acct:EquityAndLiabilitiesAccount`, the first as Equity and the second as Liability

#### Scenario: Malformed ledger account
- **WHEN** an asset with `acct:ledgerAccount "0123"` is validated with the Norway module loaded
- **THEN** the report contains a violation

### Requirement: Depreciation groups
The Norway module SHALL define `acctno:DepreciationGroupA` … `acctno:DepreciationGroupJ` as instances of
`acct:DepreciationGroup` with `skos:notation` "a"–"j" equal to Skatteetaten's `saldogruppe` code, and
`acctno:depreciationRate` 30, 20, 24, 20, 14, 12, 5, 4, 2 and 10 percent with `dct:source` citing
skatteloven § 14-41/§ 14-43. Building SHALL default to group h and Equipment to group d via
`acctno:defaultDepreciationGroup`.

#### Scenario: Rates
- **WHEN** the depreciation groups are listed
- **THEN** they are a–j with the stated rates

### Requirement: Formuesverdi
The Norway module SHALL define `acctno:wealthValue` (applies to Intangible, Building, Equipment,
FinancialFixedAsset) and `acctno:wealthValuePerShare` (applies to FinancialFixedAsset) on
`acct:AssetValuation`, with local names equal to ledger17's JSON keys. `acctno:wealthValueDefault` SHALL
state the value that counts when none is given: `acct:taxValue` for Intangible and Equipment (driftsmidler
at 31.12, skatteloven § 4-17), `acct:bookValue` for FinancialInstruments and BankAccount, none for Building
and FinancialFixedAsset. `acctno:WealthValueShape` SHALL reject a valuation carrying both wealth fields.

#### Scenario: Driftsmidler default
- **WHEN** the formuesverdi default of `EQUIPMENT` is read
- **THEN** it is `taxValue`

#### Scenario: Both wealth fields
- **WHEN** a valuation with both `wealthValue` and `wealthValuePerShare` is validated
- **THEN** the report contains a violation

### Requirement: Year-versioned code lists
The Norway module SHALL model each referenced Skatteetaten code list per income year as an
`acctno:CodeList` with `codeListName`, `incomeYear`, `codeListPath`, `codeListIdentifier` and
`codeListVersion`, and SHALL link every code value to each year's list via `acctno:inCodeList`. Codes not
modeled SHALL be listed via `acctno:excludedCode`. Version 1 SHALL cover `saldogruppe` for 2025 and 2026.
The SAF-T `naeringsspesifikasjon` grouping list is the exception: it SHALL be one `acctno:CodeList` node
that covers income years 2025 and 2026, has no `codeListPath`, `codeListIdentifier` or `codeListVersion`,
and its codes SHALL be literals (`acctno:saftGroupingCode`) with no `acctno:inCodeList`.

#### Scenario: Fixture agrees with the model
- **WHEN** each `acctno:CodeList` is compared with the Skatteetaten file stored as a test fixture for that year
- **THEN** for each `saldogruppe` list identifier and version match and every fixture code is either modeled for that year or excluded, and vice versa

#### Scenario: Filing year not covered
- **WHEN** the tests run and income year (current year in Europe/Oslo − 1) has no `saldogruppe` code list
- **THEN** the test is reported as skipped with a message naming the missing income year
- **AND** the build does not fail

### Requirement: SAF-T grouping codes
Each asset type SHALL carry, in the Norway module, `acctno:saftGroupingCategory` and `acctno:saftGroupingCode` values listing every
`naeringsspesifikasjon` grouping code within its account range except 1296, 1298 and 1299. Only codes
SHALL be shipped, never description texts. The module SHALL NOT reference the SAF-T General Ledger
Standard Accounts; `acctno:accountRange` SHALL hold NS 4102 account numbers only.

#### Scenario: Grouping codes inside the range
- **WHEN** the grouping codes of each asset type are checked against its account ranges
- **THEN** every code lies inside a range of that type

### Requirement: Datakatalog links
The Norway module SHALL link terms to Skatteetaten datakatalog concepts
(`https://data.skatteetaten.no/web/datakatalog/begrep/<uuid>`) with `skos:exactMatch` only when the
definitions coincide in scope and `skos:closeMatch` otherwise. `acct:Building` SHALL have
`skos:closeMatch` to begrep `9c33fd0b-2964-11e6-b2bc-96405985e0fa` and
`41b9c8b8-973b-11e9-bed2-005056821322`. A SKOS mapping SHALL target only a concept URI that appears in a
Skatteetaten XSD `skatt:begrepsreferanse` attribute. Concepts without such a URI (formuesverdi,
saldogruppe, avskrivningssats, skattemessig verdi) SHALL be linked with `rdfs:seeAlso` to a Skatteetaten
web page instead: current-year pages on the term, year-specific pages on that year's `acctno:CodeList`.

#### Scenario: Link format
- **WHEN** all `skos:exactMatch` and `skos:closeMatch` objects are checked
- **THEN** each matches `^https://data\.skatteetaten\.no/web/datakatalog/begrep/[0-9a-f-]{36}$`

#### Scenario: Web pages are never SKOS matches
- **WHEN** the Norway module is loaded
- **THEN** no `skos:closeMatch` or `skos:exactMatch` object starts with `https://www.skatteetaten.no/`
- **AND** `acctno:depreciationRate` has `rdfs:seeAlso <https://www.skatteetaten.no/satser/avskrivningssatser/>`
- **AND** `acctno:SaldogruppeCodeList2025` has an `rdfs:seeAlso` to the Skatte-ABC 2025 D-2 page

### Requirement: Capitalisation threshold per income year
Each `saldogruppe` `acctno:CodeList` SHALL carry exactly one `acctno:capitalisationThreshold` (NOK,
`xsd:integer`) and one `acctno:minimumLifetimeYears` (`xsd:integer`), the limits at or above which an
asset is a driftsmiddel that must be capitalised, citing skatteloven § 14-40 in `dct:source`.

#### Scenario: Threshold values
- **WHEN** the 2025 and 2026 `saldogruppe` code lists are read
- **THEN** each has `acctno:capitalisationThreshold` 30000 and `acctno:minimumLifetimeYears` 3

#### Scenario: Missing threshold
- **WHEN** a `saldogruppe` code list lacks `acctno:capitalisationThreshold` or `acctno:minimumLifetimeYears`
- **THEN** the test suite fails

### Requirement: Schema element mappings
The Norway module SHALL map `acct:depreciationGroup`, `acctno:depreciationRate`, `acct:taxValue` and
`acctno:wealthValue` to element paths in Skatteetaten's XSDs (github.com/Skatteetaten/skattemeldingen),
per schema version, using `acctno:SchemaElementMapping` nodes that point at an `acctno:SchemaVersion`
(schema name, version, target namespace, income year, `dct:source` = the XSD's URL at a release tag). These
mappings SHALL NOT be expressed as SKOS matches, and SHALL be in addition to the `rdfs:seeAlso` links. The
module SHALL NOT state a formula between opening value, depreciation and closing value, and SHALL NOT model
a per-group rate cap from the schemas.

#### Scenario: Mapped elements exist in the cited version
- **WHEN** each `acctno:schemaElement` and `acctno:openingSchemaElement` path is resolved against the stored XSD fixture of its `acctno:SchemaVersion`
- **THEN** the complex type exists and declares the element, directly or through an `xsd:extension` base

#### Scenario: Version coverage
- **WHEN** the mappings are listed
- **THEN** `acct:depreciationGroup`, `acctno:depreciationRate` and `acct:taxValue` map to `naeringsspesifikasjon` v6 (income year 2025) and v7 (2026), and `acctno:wealthValue` maps to `skattemeldingUpersonlig` v5 (2025) and v6 (2026)

#### Scenario: Not a concept match
- **WHEN** the SKOS match objects of the Norway module are listed
- **THEN** none is a GitHub URL or a schema element

