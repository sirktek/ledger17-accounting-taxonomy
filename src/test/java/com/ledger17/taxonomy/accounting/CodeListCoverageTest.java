package com.ledger17.taxonomy.accounting;

import static com.ledger17.taxonomy.accounting.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.time.Year;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

class CodeListCoverageTest {
  private static final System.Logger LOG = System.getLogger(CodeListCoverageTest.class.getName());
  private static final String SKOS = "http://www.w3.org/2004/02/skos/core#";
  private static final AccountingTaxonomy T = AccountingTaxonomy.load(Set.of("NO"));

  private static Document fixture(int year) throws Exception {
    String path = "/kodeliste/" + year + "/" + year + "_saldogruppe.xml";
    try (InputStream in = CodeListCoverageTest.class.getResourceAsStream(path)) {
      assertNotNull(in, "missing fixture " + path);
      DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
      f.setNamespaceAware(true);
      return f.newDocumentBuilder().parse(in);
    }
  }

  private static String text(Document d, String tag) {
    return d.getDocumentElement().getElementsByTagNameNS("*", tag).item(0).getTextContent().trim();
  }

  private static Set<String> fixtureCodes(Document d) {
    Set<String> codes = new HashSet<>();
    NodeList kodes = d.getDocumentElement().getElementsByTagNameNS("*", "kode");
    for (int i = 0; i < kodes.getLength(); i++) {
      Element k = (Element) kodes.item(i);
      codes.add(k.getElementsByTagNameNS("*", "tekniskNavn").item(0).getTextContent().trim());
    }
    return codes;
  }

  private static Set<String> strings(Model m, Resource s, String p) {
    Set<String> out = new HashSet<>();
    m.listObjectsOfProperty(s, m.createProperty(p)).forEachRemaining(o -> out.add(o.asLiteral().getLexicalForm()));
    return out;
  }

  private static Set<Resource> saldogruppeLists(Model m) {
    Set<Resource> out = new HashSet<>();
    m.listSubjectsWithProperty(RDF.type, m.createResource(ACCTNO + "CodeList"))
        .forEachRemaining(
            cl -> {
              if (strings(m, cl, ACCTNO + "codeListName").contains("saldogruppe")) {
                out.add(cl);
              }
            });
    return out;
  }

  @Test
  void fixtureAgreesWithModelBothWays() throws Exception {
    Model m = T.model();
    Set<Resource> lists = saldogruppeLists(m);
    assertFalse(lists.isEmpty());
    for (Resource cl : lists) {
      int year = Integer.parseInt(strings(m, cl, ACCTNO + "incomeYear").iterator().next());
      Document d = fixture(year);
      assertEquals(Set.of(text(d, "identifikator")), strings(m, cl, ACCTNO + "codeListIdentifier"), "id " + year);
      assertEquals(Set.of(text(d, "versjonsnummer")), strings(m, cl, ACCTNO + "codeListVersion"), "version " + year);
      assertEquals(
          Set.of("/formuesOgInntektsskatt/skattemelding/" + text(d, "tekniskNavn") + ".xml"),
          strings(m, cl, ACCTNO + "codeListPath"));

      Set<String> modelled = new HashSet<>();
      m.listSubjectsWithProperty(m.createProperty(ACCTNO + "inCodeList"), cl)
          .forEachRemaining(g -> modelled.addAll(strings(m, g, SKOS + "notation")));
      Set<String> excluded = strings(m, cl, ACCTNO + "excludedCode");
      assertTrue(Set.copyOf(modelled).stream().noneMatch(excluded::contains), "modelled and excluded overlap");

      Set<String> union = new HashSet<>(modelled);
      union.addAll(excluded);
      assertEquals(fixtureCodes(d), union, "codes " + year);
    }
  }

  @Test
  void filingYearCoverageOnlyWarns() {
    int filingYear = Year.now(ZoneId.of("Europe/Oslo")).getValue() - 1;
    if (!T.norway().coveredYears("saldogruppe").contains(filingYear)) {
      LOG.log(
          System.Logger.Level.WARNING,
          "No saldogruppe code list for income year {0}; add one to the Norway module",
          filingYear);
    }
  }

  @Test
  void uncoveredYearThrows() {
    assertThrows(IllegalArgumentException.class, () -> T.norway().depreciationGroups(2019));
  }
}
