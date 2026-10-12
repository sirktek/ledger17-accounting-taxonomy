## ADDED Requirements

### Requirement: Core module file and namespace
The artifact SHALL ship `ontologies/accounting-core.ttl` defining all core terms in namespace
`http://ledger17.com/ontology/accounting#` with prefix `acct:`, using only the prefixes `rdf`, `rdfs`, `xsd`,
`skos`, `sh`, `dct` and `acct`. The core module SHALL contain no jurisdiction-specific rule, value, code
list or external code reference.

#### Scenario: Core loads alone
- **WHEN** `accounting-core.ttl` is parsed without any other module
- **THEN** it parses without errors and contains no IRI in any `http://ledger17.com/ontology/accounting/<country>#` namespace

### Requirement: No third-party trademarks in names
No IRI, prefix, file name, resource path, Java type name or label in the artifact SHALL contain a
third-party trademark or the name of ledger17's internal ontology, in any letter case. Statutes and standards SHALL be
cited only in `rdfs:comment` or `dct:source`.

#### Scenario: Naming scan
- **WHEN** the ontology files and Java sources are scanned for the forbidden tokens listed in `NamingTest`
- **THEN** there is no match

### Requirement: Isolation from asset management systems
The ontology files SHALL NOT reference any `sirktek` IRI, the `schema:` namespace or
`schema:domainIncludes`, and SHALL NOT define a per-term jurisdiction tag property. Applicability SHALL be
expressed with `acct:appliesToAssetType`.

#### Scenario: Isolation scan
- **WHEN** all loaded triples are scanned
- **THEN** no IRI contains `sirktek` or `schema.org` and no property named `jurisdiction` exists

### Requirement: Carried-over concepts
The core module SHALL define `acct:AssetPurchase`, `acct:Depreciation`, `acct:FiscalYear`,
`acct:AccountType` and the account type values `acct:AssetAccount`, `acct:EquityAndLiabilitiesAccount`,
`acct:EquityAccount`, `acct:LiabilityAccount`, `acct:RevenueAccount` and `acct:ExpenseAccount`, carried over from ledger17's internal
ontology per the design's mapping table. It SHALL NOT define VAT terms or non-asset transaction types.

#### Scenario: Carried-over terms present
- **WHEN** the core module is loaded
- **THEN** each carried-over term exists with `@en` and `@no` labels

#### Scenario: Equity and liabilities hierarchy
- **WHEN** the account types are loaded
- **THEN** `acct:EquityAccount` and `acct:LiabilityAccount` each have `skos:broader acct:EquityAndLiabilitiesAccount`, and no other account type has a broader value

#### Scenario: Out-of-scope terms absent
- **WHEN** the loaded model is searched for invoice, salary, bank-transaction, transfer, VAT or MVA classes
- **THEN** none exist

### Requirement: Asset types mirror ledger17
The core module SHALL define `acct:AssetType` with exactly the instances `acct:Intangible`,
`acct:Building`, `acct:Equipment`, `acct:FinancialFixedAsset`, `acct:FinancialInstruments` and
`acct:BankAccount`, whose `skos:notation` equals ledger17's persisted codes `INTANGIBLE`, `BUILDING`,
`EQUIPMENT`, `FINANCIAL_FIXED_ASSET`, `FINANCIAL_INSTRUMENTS` and `BANK_ACCOUNT`. Each SHALL carry
`acct:accountType acct:AssetAccount`. `acct:FinancialFixedAsset` SHALL carry
`acct:requiresCounterparty true`. Later refinements SHALL be new instances linked with `skos:broader`;
existing notations SHALL NOT change.

#### Scenario: Codes match ledger17
- **WHEN** the notations of all `acct:AssetType` instances are collected
- **THEN** they equal ledger17's `AssetType` enum names exactly

### Requirement: Asset and valuation properties
The core module SHALL define `acct:Asset` with the properties `assetType`, `partyRef`, `disposedAt`,
`valuations`, `ledgerAccount` and `depreciationGroup`, and `acct:AssetValuation` with `year`,
`bookValue` and `taxValue`. Local names that ledger17 persists SHALL equal its JSON keys.
`acct:DepreciationGroup` SHALL be defined as a class without instances. Applicability SHALL be:
`partyRef` → FinancialFixedAsset; `depreciationGroup` → Intangible, Building, Equipment; `taxValue` → all
except BankAccount; all others → all types.

#### Scenario: ledger17 JSON keys resolve
- **WHEN** ledger17's asset JSON keys `assetType`, `partyRef`, `disposedAt`, `valuations` and `year` are looked up as local names in core
- **THEN** each resolves to an `acct:` property

#### Scenario: No core depreciation group values
- **WHEN** the core module is loaded alone
- **THEN** `acct:DepreciationGroup` has no instances

### Requirement: Four-digit account numbers with derived group and class
`acct:ledgerAccount` values SHALL be strings of exactly four digits (`^[0-9]{4}$`); longer sub-account
numbers SHALL NOT be accepted. The core module SHALL define `acct:accountGroup` (the first two digits) and
`acct:accountClass` (the first digit), and `acct:AssetShape` SHALL reject values that differ from the
digits of `ledgerAccount`. `AccountNumbers.accountGroup` and `AccountNumbers.accountClass` SHALL derive
them positionally, without lookup tables.

#### Scenario: Pattern
- **WHEN** assets with `acct:ledgerAccount` "1205", "123", "12050" and "12a5" are validated against the core shapes
- **THEN** only "1205" conforms

#### Scenario: Group and class derivation
- **WHEN** `AccountNumbers.accountGroup("1205")` and `AccountNumbers.accountClass("1205")` are called
- **THEN** they return "12" and "1"

#### Scenario: Inconsistent group
- **WHEN** an asset with `acct:ledgerAccount "1205"` and `acct:accountGroup "13"` is validated
- **THEN** the report contains a violation

### Requirement: Bilingual labels
Every non-shape subject defined in any module file SHALL have exactly one `rdfs:label` in `@en`, exactly
one in `@no`, and an `rdfs:comment` in `@en` stating what the value means.

#### Scenario: Label check
- **WHEN** the subjects of each module file are checked
- **THEN** each has one `@en` label, one `@no` label and an `@en` comment

### Requirement: Core SHACL shape
The core module SHALL define `acct:AssetShape` (targets subjects of `acct:assetType`): exactly one
`assetType` in `acct:AssetType`, `disposedAt` of datatype `xsd:date`; and `acct:AssetValuationShape`
(targets `acct:AssetValuation`): exactly one integer `year`. Messages SHALL be given in `@en` and `@no`.

#### Scenario: ledger17-shaped asset conforms
- **WHEN** a ledger17-shaped asset with one valuation is validated against the core shapes
- **THEN** the report conforms

#### Scenario: Missing year
- **WHEN** a valuation without `acct:year` is validated
- **THEN** the report contains a violation
