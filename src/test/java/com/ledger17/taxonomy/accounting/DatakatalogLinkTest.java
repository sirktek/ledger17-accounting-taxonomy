package com.ledger17.taxonomy.accounting;

import static com.ledger17.taxonomy.accounting.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.Test;

class DatakatalogLinkTest {
  private static final String SKOS = "http://www.w3.org/2004/02/skos/core#";
  private static final String RATES = "https://www.skatteetaten.no/satser/avskrivningssatser/";
  private static final String WEALTH = "https://www.skatteetaten.no/satser/formuesskatt/";
  private static final String BUYING =
      "https://www.skatteetaten.no/bedrift-og-organisasjon/skatt/skattemelding-naringsdrivende/fradrag/eiendeler-utstyr-eiendom/bedriftens-eiendeler/kjope-eiendeler-til-bedriften/";
  private static final String ABC_2025 =
      "https://www.skatteetaten.no/rettskilder/type/handboker/skatte-abc/2025/d-2-driftsmiddel--avskrivning-painntektsforing-av-saldo/";
  private static final AccountingTaxonomy T = AccountingTaxonomy.load(Set.of("NO"));

  private static boolean see(Model m, String subject, String target) {
    return m.contains(
        m.createResource(subject), m.createProperty("http://www.w3.org/2000/01/rdf-schema#seeAlso"), m.createResource(target));
  }

  @Test
  void matchTargetsAreDatakatalogConceptsOnly() {
    Model m = T.model();
    int count = 0;
    for (String p : new String[] {"closeMatch", "exactMatch"}) {
      Property prop = m.createProperty(SKOS + p);
      for (RDFNode o : m.listObjectsOfProperty(prop).toList()) {
        count++;
        assertTrue(
            o.isURIResource()
                && o.asResource().getURI().matches("^https://data\\.skatteetaten\\.no/web/datakatalog/begrep/[0-9a-f-]{36}$"),
            o.toString());
      }
    }
    assertEquals(2, count);
  }

  @Test
  void buildingHasTheTwoCloseMatches() {
    Model m = T.model();
    Resource b = m.createResource(ACCT + "Building");
    Property close = m.createProperty(SKOS + "closeMatch");
    for (String id : new String[] {"9c33fd0b-2964-11e6-b2bc-96405985e0fa", "41b9c8b8-973b-11e9-bed2-005056821322"}) {
      assertTrue(m.contains(b, close, m.createResource("https://data.skatteetaten.no/web/datakatalog/begrep/" + id)), id);
    }
  }

  @Test
  void referencePagesAreSeeAlsoLinks() {
    Model m = T.model();
    assertTrue(see(m, ACCTNO + "wealthValue", WEALTH));
    assertTrue(see(m, ACCTNO + "wealthValueDefault", WEALTH));
    assertTrue(see(m, ACCT + "DepreciationGroup", RATES));
    assertTrue(see(m, ACCTNO + "depreciationRate", RATES));
    assertTrue(see(m, ACCT + "taxValue", BUYING));
    assertTrue(see(m, ACCTNO + "capitalisationThreshold", BUYING));
    assertTrue(see(m, ACCTNO + "minimumLifetimeYears", BUYING));
    assertTrue(see(m, ACCTNO + "SaldogruppeCodeList2025", ABC_2025));
  }

  @Test
  void coreOnlyHasNoneOfThem() {
    Model m = AccountingTaxonomy.load(Set.of()).model();
    assertFalse(m.listObjectsOfProperty(m.createProperty(SKOS + "closeMatch")).hasNext());
    assertFalse(m.listStatements(null, m.createProperty("http://www.w3.org/2000/01/rdf-schema#seeAlso"), (RDFNode) null)
        .filterKeep(s -> s.getObject().isURIResource() && s.getObject().asResource().getURI().contains("skatteetaten"))
        .hasNext());
  }
}
