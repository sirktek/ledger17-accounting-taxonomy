package com.ledger17.taxonomy.accounting;

import static com.ledger17.taxonomy.accounting.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.Test;

class CarryOverTest {
  private static final Model M = AccountingTaxonomy.load(Set.of("NO")).model();

  private static boolean defined(String uri) {
    return M.contains(M.createResource(uri), RDF.type);
  }

  @Test
  void everyCarriedOverTermExists() {
    List<String> core =
        List.of(
            "AssetPurchase", "Depreciation", "FiscalYear", "AccountType", "AssetAccount",
            "EquityAndLiabilitiesAccount", "EquityAccount", "LiabilityAccount", "RevenueAccount",
            "ExpenseAccount", "ledgerAccount");
    for (String t : core) {
      assertTrue(defined(ACCT + t), t);
    }
    assertTrue(defined(ACCTNO + "LedgerAccountShape"));
  }

  @Test
  void accountTypeHierarchy() {
    var broader = M.createProperty("http://www.w3.org/2004/02/skos/core#broader");
    Resource parent = M.createResource(ACCT + "EquityAndLiabilitiesAccount");
    for (var d : AccountingTaxonomy.load(Set.of()).accountTypes()) {
      boolean child = d.uri().endsWith("#EquityAccount") || d.uri().endsWith("#LiabilityAccount");
      if (child) {
        assertEquals(parent.getURI(), d.broader(), d.uri());
      } else {
        assertNull(d.broader(), d.uri());
      }
    }
    assertEquals(6, AccountingTaxonomy.load(Set.of()).accountTypes().size());
    assertTrue(M.contains(M.createResource(ACCT + "EquityAccount"), broader, parent));
  }

  @Test
  void noVatInvoiceOrTransactionTerms() {
    Set<String> banned =
        Set.of("vat", "mva", "invoice", "salary", "transfer", "transaction", "settlement");
    M.listSubjects().forEachRemaining(s -> {
      if (s.isURIResource()) {
        for (String token : s.getLocalName().split("(?<=[a-z0-9])(?=[A-Z])")) {
          assertFalse(banned.contains(token.toLowerCase()), s.getURI());
        }
      }
    });
  }
}
