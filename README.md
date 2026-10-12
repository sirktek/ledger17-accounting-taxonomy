# ledger17-accounting-taxonomy

RDF vocabulary for asset accounting (asset types, valuations, account types) with a small Java API.
A jurisdiction-neutral core plus a Norway module. Maven coordinates `com.ledger17:accounting-taxonomy`,
published to GitHub Packages. MIT licensed.

## Purpose and ownership

The terms are ledger17 domain knowledge. The repository is hosted in the sirktek organization; names,
package and IRIs are ledger17's. The vocabulary describes how an accounting system records assets. It does
not produce values (no depreciation runs, no formuesverdi calculation) and defines no sync protocol or
persistence.

## Modules and namespaces

| Module | File (`src/main/resources/ontologies/`) | Namespace | Prefix |
|---|---|---|---|
| core | `accounting-core.ttl` | `http://ledger17.com/ontology/accounting#` | `acct:` |
| Norway | `accounting-no.ttl` | `http://ledger17.com/ontology/accounting/no#` | `acctno:` |

The loader always adds core. The Norway module may also state facts about core terms (for example an
account range on `acct:Building`); those statements exist only when it is loaded. A future country `xx`
gets `accounting-xx.ttl`, `.../accounting/xx#`, `acctxx:`.

Why modules and not per-term jurisdiction tags:

- Tags fail open: every consumer must filter, and a generic loader that forgets shows national tax terms
  everywhere. A module that is not loaded is not in the graph.
- Tags put different countries' statutory values (rates, ranges) on one resource.
- One country's statute changes would churn a shared file; modules change independently.

No third-party trademark appears in any IRI, prefix, file name, Java name or label (checked by
`NamingTest`). Statutes and standards (regnskapsloven, skatteloven, NS 4102 from Standard Norge, SAF-T) are
cited factually in comments and `dct:source` only.

## Core terms (`acct:`)

Classes:

| Term | Meaning |
|---|---|
| `Asset` | An asset as an accounting system records it |
| `AssetType` | Asset type; six values below |
| `AssetValuation` | The values of one asset at the end of one fiscal year |
| `DepreciationGroup` | Tax depreciation pool defined by a jurisdiction module; core has no values |
| `AccountType` | Values `AssetAccount`, `EquityAndLiabilitiesAccount`, `EquityAccount`, `LiabilityAccount`, `RevenueAccount`, `ExpenseAccount` |
| `AssetPurchase`, `Depreciation`, `FiscalYear` | Acquisition event (cost basis), depreciation event, fiscal year |

`EquityAccount` and `LiabilityAccount` are `skos:broader` `EquityAndLiabilitiesAccount` (the financing side
of the balance sheet). Equity is not a liability.

Asset types (`skos:notation` is the exact code consumers persist):

| Instance | Notation | Label en / no | Counterparty required |
|---|---|---|---|
| `acct:Intangible` | `INTANGIBLE` | Intangible asset / Immateriell eiendel | no |
| `acct:Building` | `BUILDING` | Building / Bygning | no |
| `acct:Equipment` | `EQUIPMENT` | Equipment and vehicles / Utstyr og kjøretøy | no |
| `acct:FinancialFixedAsset` | `FINANCIAL_FIXED_ASSET` | Financial fixed asset / Finansielt anleggsmiddel | yes |
| `acct:FinancialInstruments` | `FINANCIAL_INSTRUMENTS` | Securities / Verdipapir | no |
| `acct:BankAccount` | `BANK_ACCOUNT` | Bank account / Bankkonto | no |

Asset properties (`rdfs:domain acct:Asset`; the local name is the key a consumer stores):

| Property | Range | Applies to |
|---|---|---|
| `assetType` | `acct:AssetType` | all |
| `partyRef` | `xsd:string`, the investee party's id | FinancialFixedAsset |
| `disposedAt` | `xsd:date` | all |
| `valuations` | `acct:AssetValuation`, one per year | all |
| `ledgerAccount` | `xsd:string`, `^[0-9]{4}$` | all |
| `accountGroup`, `accountClass` | `xsd:string`, first two digits / first digit of `ledgerAccount` | all |
| `depreciationGroup` | `acct:DepreciationGroup` | Intangible, Building, Equipment |

Valuation fields (`rdfs:domain acct:AssetValuation`):

| Field | Applies to | Meaning |
|---|---|---|
| `year` | all | `xsd:integer`, end year of the fiscal year |
| `bookValue` | all | Carrying amount: cost less accumulated depreciation and write-downs |
| `taxValue` | all except BankAccount | Tax written-down value that the depreciation rate applies to |

Applicability is stated with `acct:appliesToAssetType` on a property (repeatable; absent means all types).
`acct:defaultAssetType` lets a consumer annotate its own class with a default asset type.
Account numbers are exactly four digits; sub-accounts are not allowed.

## Norway terms (`acctno:`)

| Term | Meaning |
|---|---|
| `wealthValue` (Formuesverdi) | Total formuesverdi at 31.12, entered manually. Applies to Intangible, Building, Equipment, FinancialFixedAsset. Exclusive with `wealthValuePerShare`. |
| `wealthValuePerShare` | Per-share formuesverdi (FinancialFixedAsset); total is per-share value times the year-end holding |
| `accountRange` | NS 4102 account numbers only (labels are our own) as `acctno:AccountRange` nodes: `fromAccount`, `toAccount`, `coversGroup`, `wholeGroups` |
| `defaultDepreciationGroup` | Building `h`, Equipment `d` |
| `wealthValueDefault` | Which value counts when none is given |
| `DepreciationGroupA` to `J` | Notation `a`-`j` (the Skatteetaten saldogruppe code) with `depreciationRate` |
| `CodeList`, `SchemaVersion`, `SchemaElementMapping` | Year-versioned code lists and XSD element mappings, below |
| `LedgerAccountShape`, `WealthValueShape` | SHACL: `^[1-8][0-9]{3}$`; not both wealth fields |

Account ranges (NS 4102 numbers):

| Type | Range | Default depreciation group | Default formuesverdi | SAF-T grouping category |
|---|---|---|---|---|
| Intangible | 1000-1099 | none | `acct:taxValue` | balanseverdiForAnleggsmiddel |
| Building | 1100-1199 | h | none (Skatteetaten values property) | balanseverdiForAnleggsmiddel |
| Equipment | 1200-1299 | d | `acct:taxValue` | balanseverdiForAnleggsmiddel |
| FinancialFixedAsset | 1300-1399 | none | none | balanseverdiForAnleggsmiddel |
| FinancialInstruments | 1800-1899 | none | `acct:bookValue` | balanseverdiForOmloepsmiddel |
| BankAccount | 1920, 1950, 1960 | none | `acct:bookValue` | balanseverdiForOmloepsmiddel |

Account types: AssetAccount 1000-1999, EquityAndLiabilitiesAccount 2000-2999 (EquityAccount 2000-2099,
LiabilityAccount 2100-2999), RevenueAccount 3000-3999, ExpenseAccount 4000-8999.

Depreciation groups a-j carry the statutory maximum declining-balance rate (skatteloven § 14-43), percent:

| a | b | c | d | e | f | g | h | i | j |
|---|---|---|---|---|---|---|---|---|---|
| 30 | 20 | 24 | 20 | 14 | 12 | 5 | 4 | 2 | 10 |

Group h also has higher rates for some buildings; v1 does not model them. Rates are documentation with
`dct:source`; the statute is the source of truth.

Other Norway data:

- Capitalisation threshold (skatteloven § 14-40) per income year on each saldogruppe code list:
  `capitalisationThreshold` 30 000 NOK and `minimumLifetimeYears` 3 for 2025 and 2026.
- SAF-T grouping codes: `saftGroupingCategory` and repeatable `saftGroupingCode` per asset type, from
  Skatteetaten's næringsspesifikasjon list. Codes only, no description texts. 1296, 1298 and 1299 are not
  listed (not assets). The SAF-T General Ledger Standard Accounts are not used.
- `skos:closeMatch` from `acct:Building` to Skatteetaten datakatalog concepts (fast eiendom; fast eiendom
  som formuesobjekt). Terms without a datakatalog concept get `rdfs:seeAlso` to skatteetaten.no pages;
  web pages are never match targets.
- Schema element mappings to Skatteetaten's XSDs (naeringsspesifikasjon v6/v7, skattemeldingUpersonlig
  v5/v6) for `depreciationGroup`, `depreciationRate`, `taxValue` and `wealthValue`. These map to data
  elements, never SKOS matches.

## Value semantics

- Amounts are decimals in the reporting currency (NOK in the Norway module).
- A valuation holds `wealthValue` or `wealthValuePerShare`, never both.
- `disposedAt` drops the asset from every fiscal year ending on or after that date.
- Default formuesverdi: driftsmidler (Intangible, Equipment) use `taxValue` at 31.12; FinancialInstruments
  and BankAccount use `bookValue` and take no manual formuesverdi.
- SHACL messages: one `sh:message@en` per constraint plus `acct:messageNo@no` (annotation property).
  Jena 5.5's SHACL parser rejects two `sh:message` values on an `sh:sparql` constraint.
- Applicability and range membership are API checks, not SHACL, because consumer data may be partial.

## Code-list year policy

Code lists are year-versioned. Version 1 covers 2025 and 2026.

`CodeListCoverageTest` and `SchemaElementTest` check the filing year (current year minus one, Europe/Oslo).
If its lists are missing, the missing year is reported as a skipped test; the build still passes.
To add a year once Skatteetaten publishes it:

1. Add the year's `<year>_saldogruppe.xml` (from Skatteetaten/skattemeldingen) under
   `src/test/resources/kodeliste/<year>/` and update its NOTICE.
2. Add an `acctno:SaldogruppeCodeList<year>` instance (name, `incomeYear`, `codeListPath`,
   `codeListIdentifier`, `codeListVersion`, thresholds, `rdfs:seeAlso`; `excludedCode` for codes not modeled).
3. Add `acctno:inCodeList` triples on each depreciation group that appears in the list.
4. Extend the SAF-T grouping list coverage and add `SchemaVersion` and mapping nodes for the new XSD
   versions, with their stripped fixtures.
5. Run `mvnq test`. No new term is needed.

## Refinement and compatibility

Refinements (for example Land or Vehicle under Equipment) are new instances with `skos:broader` pointing at
the v1 value. v1 codes and property local names are frozen per major version; renaming a term is a breaking
change.

## Consumer mapping

- Consumers store values in typed fields named after the taxonomy properties. Enum codes equal
  `skos:notation`; JSON keys equal local names. Local names are unique across modules, so
  `property(localName)` resolves a key across the loaded modules.
- Load modules by the organization's country: `NO` uses `load(Set.of("NO"))`; others use core only.
- Domain taxonomies annotate their own classes with `acct:defaultAssetType` (for example real estate to
  `acct:Building`, machines and hardware to `acct:Equipment`). This is a class annotation, not a property
  declaration, so generic category-property loaders never surface accounting terms.

## Not in scope

- VAT/MVA and non-asset transactions (invoices, salary, bank transactions, transfers).
- Producing values: depreciation runs, formuesverdi calculation.
- Other countries' modules (additive later).
- IFRS links: a later item, `skos:closeMatch` from core terms to IFRS taxonomy elements.

## Adopting

GitHub Packages requires authentication even for public packages. Create a token with `read:packages`,
add a server with id `github` to `~/.m2/settings.xml`, and declare the repository:

```xml
<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/sirktek/ledger17-accounting-taxonomy</url>
  </repository>
</repositories>

<dependency>
  <groupId>com.ledger17</groupId>
  <artifactId>accounting-taxonomy</artifactId>
  <version>1.N</version> <!-- the release number from the GitHub Packages page -->
</dependency>
```

Requires Java 17 and Apache Jena 5.5 (`jena-arq` is the only compile dependency; `jena-shacl` 5.5.0 is
needed only if you validate with the shapes).

```java
import com.ledger17.taxonomy.accounting.AccountingTaxonomy;
import java.util.Set;

AccountingTaxonomy taxonomy = AccountingTaxonomy.load(Set.of("NO")); // Set.of() = core only
org.apache.jena.rdf.model.Model model = taxonomy.model();            // union incl. shapes; do not mutate
taxonomy.assetType("BUILDING");                                       // Optional<AssetTypeDef>
taxonomy.appliesTo("wealthValue", "EQUIPMENT");                       // true
taxonomy.norway().assetTypeForAccount("1150");                        // BUILDING
taxonomy.norway().depreciationGroups(2026);                           // a-j with rates
```

Unknown or lowercase country codes throw `IllegalArgumentException`; `norway()` throws
`IllegalStateException` when the Norway module is not loaded.

## Sources and attribution

- Skatteetaten/skattemeldingen (Apache-2.0, tag v1.62.115): saldogruppe code lists and stripped XSDs
  (annotations removed) used as test fixtures, with NOTICE files under `src/test/resources`.
- Skatteetaten/saf-t (Apache-2.0): næringsspesifikasjon grouping codes (codes only), pinned commit in
  `src/test/resources/saft/NOTICE`. Test fixture only.
- Skatteetaten datakatalog concept IRIs (as `skos:closeMatch` targets) and skatteetaten.no pages (as
  `rdfs:seeAlso`). Comments paraphrase these sources; no text is copied.
- NS 4102 (Standard Norge): account numbers only, with our own labels.
