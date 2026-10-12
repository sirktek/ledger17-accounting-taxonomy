package com.ledger17.taxonomy.accounting;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ApplicabilityTest {
  private static final List<String> ALL =
      List.of("INTANGIBLE", "BUILDING", "EQUIPMENT", "FINANCIAL_FIXED_ASSET", "FINANCIAL_INSTRUMENTS", "BANK_ACCOUNT");
  private static final AccountingTaxonomy T = AccountingTaxonomy.load(Set.of("NO"));

  private static void check(String property, Set<String> expected) {
    for (String code : ALL) {
      assertEquals(expected.contains(code), T.appliesTo(property, code), property + " " + code);
    }
  }

  @Test
  void coreProperties() {
    check("partyRef", Set.of("FINANCIAL_FIXED_ASSET"));
    check("depreciationGroup", Set.of("INTANGIBLE", "BUILDING", "EQUIPMENT"));
    check("taxValue", Set.of("INTANGIBLE", "BUILDING", "EQUIPMENT", "FINANCIAL_FIXED_ASSET", "FINANCIAL_INSTRUMENTS"));
    for (String p : List.of("assetType", "disposedAt", "valuations", "ledgerAccount", "year", "bookValue")) {
      check(p, Set.copyOf(ALL));
    }
  }

  @Test
  void norwayProperties() {
    check("wealthValue", Set.of("INTANGIBLE", "BUILDING", "EQUIPMENT", "FINANCIAL_FIXED_ASSET"));
    check("wealthValuePerShare", Set.of("FINANCIAL_FIXED_ASSET"));
    assertFalse(T.appliesTo("noSuchProperty", "BUILDING"));
  }

  @Test
  void jurisdictionAndLedger17Keys() {
    assertEquals("NO", T.property("wealthValue").orElseThrow().jurisdiction());
    assertNull(T.property("taxValue").orElseThrow().jurisdiction());
    for (String key : List.of("assetType", "partyRef", "disposedAt", "valuations", "year")) {
      assertTrue(T.property(key).isPresent(), key);
    }
    assertTrue(T.property("jurisdiction").isEmpty());
    assertTrue(T.property("name").isEmpty());
    assertTrue(T.property("identifiers").isEmpty());
  }
}
