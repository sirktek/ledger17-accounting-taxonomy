package com.ledger17.taxonomy.accounting;

import static com.ledger17.taxonomy.accounting.TestSupport.ACCTNO;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.junit.jupiter.api.Test;

class ModuleLoadingTest {

  private static Set<String> uris(Model m) {
    Set<String> result = new HashSet<>();
    for (Statement s : m.listStatements().toList()) {
      if (s.getSubject().isURIResource()) {
        result.add(s.getSubject().getURI());
      }
      result.add(s.getPredicate().getURI());
      RDFNode o = s.getObject();
      if (o.isURIResource()) {
        result.add(o.asResource().getURI());
      }
    }
    return result;
  }

  @Test
  void coreOnlyHasNoNorwegianContent() {
    AccountingTaxonomy t = AccountingTaxonomy.load(Set.of());
    Set<String> uris = uris(t.model());
    assertTrue(uris.stream().noneMatch(u -> u.startsWith(ACCTNO)), "acctno IRI in core-only load");
    assertTrue(uris.stream().noneMatch(u -> u.contains("data.skatteetaten.no")), "datakatalog IRI");
    assertTrue(uris.stream().noneMatch(u -> u.contains("saft")), "SAF-T annotation");
    assertTrue(t.property("wealthValue").isEmpty());
    assertThrows(IllegalStateException.class, t::norway);
    assertTrue(t.jurisdictions().isEmpty());
  }

  @Test
  void norwayAddsItsTerms() {
    AccountingTaxonomy t = AccountingTaxonomy.load(Set.of("NO"));
    Set<String> uris = uris(t.model());
    assertTrue(uris.stream().anyMatch(u -> u.startsWith(ACCTNO)));
    assertTrue(uris.contains(ACCTNO + "accountRange"));
    assertTrue(uris.contains(ACCTNO + "saftGroupingCategory"));
    assertTrue(uris.contains(ACCTNO + "DepreciationGroupA"));
    assertTrue(t.property("wealthValue").isPresent());
    assertNotNull(t.norway());
    assertEquals(Set.of("NO"), t.jurisdictions());
  }

  @Test
  void unsupportedJurisdictionsAreRejected() {
    assertThrows(IllegalArgumentException.class, () -> AccountingTaxonomy.load(Set.of("SE")));
    assertThrows(IllegalArgumentException.class, () -> AccountingTaxonomy.load(Set.of("no")));
    assertThrows(IllegalArgumentException.class, () -> AccountingTaxonomy.load(null));
  }
}
