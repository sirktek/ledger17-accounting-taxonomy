package com.ledger17.taxonomy.accounting;

import static com.ledger17.taxonomy.accounting.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.Test;

class DepreciationGroupTest {
  private static final AccountingTaxonomy T = AccountingTaxonomy.load(Set.of("NO"));

  @Test
  void groupsAToJWithStatutoryRates() {
    String[] notations = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j"};
    int[] rates = {30, 20, 24, 20, 14, 12, 5, 4, 2, 10};
    var type = T.model().createResource(ACCT + "DepreciationGroup");
    var rate = T.model().createProperty(ACCTNO + "depreciationRate");
    var notation = T.model().createProperty("http://www.w3.org/2004/02/skos/core#notation");
    var source = T.model().createProperty("http://purl.org/dc/terms/source");
    for (int i = 0; i < notations.length; i++) {
      var g = T.model().createResource(ACCTNO + "DepreciationGroup" + notations[i].toUpperCase());
      assertTrue(T.model().contains(g, RDF.type, type), notations[i]);
      assertEquals(notations[i], T.model().getProperty(g, notation).getString());
      assertEquals(0, new BigDecimal(T.model().getProperty(g, rate).getString()).compareTo(BigDecimal.valueOf(rates[i])), notations[i]);
      assertTrue(T.model().getProperty(g, source).getString().contains("14-4"), notations[i]);
    }
    assertEquals(10, T.model().listSubjectsWithProperty(RDF.type, type).toList().size());
  }

  @Test
  void defaultsPerAssetType() {
    NorwegianRules r = T.norway();
    assertEquals("h", r.defaultDepreciationGroup("BUILDING").orElseThrow());
    assertEquals("d", r.defaultDepreciationGroup("EQUIPMENT").orElseThrow());
    for (String c : List.of("INTANGIBLE", "FINANCIAL_FIXED_ASSET", "FINANCIAL_INSTRUMENTS", "BANK_ACCOUNT")) {
      assertTrue(r.defaultDepreciationGroup(c).isEmpty(), c);
    }
  }

  @Test
  void wealthValueDefaults() {
    NorwegianRules r = T.norway();
    assertEquals("taxValue", r.wealthValueDefault("EQUIPMENT").orElseThrow());
    assertEquals("taxValue", r.wealthValueDefault("INTANGIBLE").orElseThrow());
    assertEquals("bookValue", r.wealthValueDefault("FINANCIAL_INSTRUMENTS").orElseThrow());
    assertEquals("bookValue", r.wealthValueDefault("BANK_ACCOUNT").orElseThrow());
    assertTrue(r.wealthValueDefault("BUILDING").isEmpty());
    assertTrue(r.wealthValueDefault("FINANCIAL_FIXED_ASSET").isEmpty());
  }

  @Test
  void uncoveredYearThrows() {
    assertThrows(IllegalArgumentException.class, () -> T.norway().depreciationGroups(2019));
  }
}
