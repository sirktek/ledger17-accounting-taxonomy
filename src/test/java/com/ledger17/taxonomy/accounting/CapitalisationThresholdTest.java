package com.ledger17.taxonomy.accounting;

import static com.ledger17.taxonomy.accounting.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.Test;

class CapitalisationThresholdTest {
  private static final String XSD_INTEGER = "http://www.w3.org/2001/XMLSchema#integer";
  private static final AccountingTaxonomy T = AccountingTaxonomy.load(Set.of("NO"));

  @Test
  void everySaldogruppeListHasOneThresholdAndLifetimeCitingParagraph14_40() {
    Model m = T.model();
    List<Resource> lists =
        m.listSubjectsWithProperty(RDF.type, m.createResource(ACCTNO + "CodeList")).toList().stream()
            .filter(cl -> m.contains(cl, m.createProperty(ACCTNO + "codeListName"), "saldogruppe"))
            .toList();
    assertEquals(2, lists.size());
    for (Resource cl : lists) {
      for (String p : List.of("capitalisationThreshold", "minimumLifetimeYears")) {
        var values = m.listObjectsOfProperty(cl, m.createProperty(ACCTNO + p)).toList();
        assertEquals(1, values.size(), cl + " " + p);
        assertEquals(XSD_INTEGER, values.get(0).asLiteral().getDatatypeURI());
      }
      var src = m.listObjectsOfProperty(cl, m.createProperty("http://purl.org/dc/terms/source")).toList();
      assertTrue(src.stream().anyMatch(o -> o.asLiteral().getString().contains("14-40")), cl.toString());
    }
  }

  @Test
  void valuesFor2025And2026() {
    Model m = T.model();
    for (int year : new int[] {2025, 2026}) {
      Resource cl = m.createResource(ACCTNO + "SaldogruppeCodeList" + year);
      assertEquals(30000, m.getProperty(cl, m.createProperty(ACCTNO + "capitalisationThreshold")).getInt());
      assertEquals(3, m.getProperty(cl, m.createProperty(ACCTNO + "minimumLifetimeYears")).getInt());
    }
  }
}
