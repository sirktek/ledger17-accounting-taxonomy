package com.ledger17.taxonomy.accounting;

import static com.ledger17.taxonomy.accounting.TestSupport.ACCT;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.Test;

class AssetTypeTest {
  private static final AccountingTaxonomy T = AccountingTaxonomy.load(Set.of());

  @Test
  void sixTypesWithLedger17Codes() {
    List<AssetTypeDef> types = T.assetTypes();
    assertEquals(
        List.of("INTANGIBLE", "BUILDING", "EQUIPMENT", "FINANCIAL_FIXED_ASSET", "FINANCIAL_INSTRUMENTS", "BANK_ACCOUNT"),
        types.stream().map(AssetTypeDef::code).toList());
  }

  @Test
  void designTable() {
    String[][] rows = {
      {"Intangible", "INTANGIBLE", "Intangible asset", "Immateriell eiendel", "false"},
      {"Building", "BUILDING", "Building", "Bygning", "false"},
      {"Equipment", "EQUIPMENT", "Equipment and vehicles", "Utstyr og kjøretøy", "false"},
      {"FinancialFixedAsset", "FINANCIAL_FIXED_ASSET", "Financial fixed asset", "Finansielt anleggsmiddel", "true"},
      {"FinancialInstruments", "FINANCIAL_INSTRUMENTS", "Securities", "Verdipapir", "false"},
      {"BankAccount", "BANK_ACCOUNT", "Bank account", "Bankkonto", "false"},
    };
    var accountType = T.model().createProperty(ACCT + "accountType");
    for (String[] r : rows) {
      AssetTypeDef d = T.assetType(r[1]).orElseThrow();
      assertEquals(ACCT + r[0], d.uri());
      assertEquals(r[2], d.labelEn());
      assertEquals(r[3], d.labelNo());
      assertEquals(Boolean.parseBoolean(r[4]), d.requiresCounterparty(), r[1]);
      assertNotNull(d.comment());
      assertTrue(
          T.model().contains(T.model().createResource(d.uri()), accountType, T.model().createResource(ACCT + "AssetAccount")));
    }
    assertTrue(T.assetType("LAND").isEmpty());
    assertTrue(T.model().contains(T.model().createResource(ACCT + "DepreciationGroup"), RDF.type));
    assertTrue(T.model().listSubjectsWithProperty(RDF.type, T.model().createResource(ACCT + "DepreciationGroup")).toList().isEmpty());
  }
}
