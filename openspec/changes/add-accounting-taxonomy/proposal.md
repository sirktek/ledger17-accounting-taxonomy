## Why

ledger17 already models asset accounting: `AssetType` (INTANGIBLE … BANK_ACCOUNT), `AssetValuation
{year, wealthValue | wealthValuePerShare}`, `disposedAt`, `partyRef`, persisted as JSON, plus an internal
ontology. Sirk has a second, divergent copy of the same facts in its shared base taxonomy
(`wealthTaxValue`, `bookValue`, …) wired through `schema:domainIncludes`, which renders
them generically and can leak them to the marketplace/DPP. ledger17 is about to sync accounting data out
to asset management systems and needs one published vocabulary for that payload, built on what ledger17
already has. Most of it is not Norwegian; the Norwegian tax rules (saldogrupper, formuesverdi, NS 4102
ranges) must be loadable only by consumers that operate in Norway.

## What Changes

- New artifact `com.ledger17:accounting-taxonomy` (MIT, Java 17) in the public repo
  `sirktek/ledger17-accounting-taxonomy`, published to GitHub Packages only, using the GitHub Packages
  part of sirk-logistics-taxonomy's release setup.
- **Core module** `accounting-core.ttl`, namespace `http://ledger17.com/ontology/accounting#`, prefix
  `acct:`, jurisdiction-neutral: `acct:Asset`, `acct:AssetType` with ledger17's six values (notation =
  persisted code), properties named after ledger17's JSON keys (`assetType`, `partyRef`, `disposedAt`,
  `valuations`, `year`) plus `ledgerAccount`, `depreciationGroup`, `bookValue`, `taxValue`;
  `acct:AssetValuation`, `acct:DepreciationGroup` (class only), `acct:AccountType`,
  `acct:AssetPurchase`, `acct:Depreciation`, `acct:FiscalYear`.
- **Norway module** `accounting-no.ttl`, namespace `http://ledger17.com/ontology/accounting/no#`, prefix
  `acctno:`: NS 4102 account ranges, saldogrupper a–j with statutory rates (skatteloven § 14-41/§ 14-43),
  `wealthValue`/`wealthValuePerShare` with the formuesverdi rules (skatteloven § 4-17), per-asset-type
  formuesverdi applicability, year-versioned Skatteetaten code lists (`saldogruppe`), SAF-T
  grouping-code references (Skatteetaten's næringsspesifikasjon list), SKOS links to Skatteetaten datakatalog concepts, `rdfs:seeAlso` links to Skatteetaten reference pages, and the per-year capitalisation threshold (skatteloven § 14-40).
- Concepts from ledger17's internal ontology that this vocabulary needs are carried over under the new
  namespaces (mapping in design.md); its file is not copied. VAT and non-asset transaction types stay
  internal to ledger17.
- SHACL shapes per module. Thin Java API loading core plus requested jurisdictions
  (`AccountingTaxonomy.load(Set.of("NO"))`); unknown jurisdictions are rejected.
- Isolation: no Sirk IRIs, no `schema:domainIncludes`, no dependency on other Sirk taxonomies, no
  third-party trademarks in names.

## Capabilities

### New Capabilities
- `accounting-core`: jurisdiction-neutral vocabulary: terms, semantics, labels, SHACL, isolation rules.
- `accounting-norway`: Norway module: account ranges, saldogrupper, formuesverdi, year-versioned code
  lists, SAF-T grouping codes, datakatalog links.
- `accounting-taxonomy-api`: the published artifact and the Java loader API (module selection, typed
  views, Norway rules view).

### Modified Capabilities
- (none, new repo)

## Impact

- New repo only; no code changes elsewhere in this change.
- Consumers resolve from GitHub Packages, which needs authentication even for public packages:
  ledger17's build (lab17tech org) needs a token with `read:packages` on sirktek packages before it can
  adopt. Sirk services already read sirktek packages.
- Follow-up changes (not tasks here):
  - ledger17: depend on the artifact and map asset types to tax-return object types per year.
  - Sirk: consumes the artifact through typed fields, not generic properties.
