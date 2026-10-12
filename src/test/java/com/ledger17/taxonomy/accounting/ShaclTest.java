package com.ledger17.taxonomy.accounting;

import static com.ledger17.taxonomy.accounting.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.Test;

class ShaclTest {
  private static final Model CORE = AccountingTaxonomy.load(Set.of()).model();
  private static final Model NO = AccountingTaxonomy.load(Set.of("NO")).model();

  private static boolean conforms(Model shapesAndVocab, Model data) {
    Model all = ModelFactory.createUnion(shapesAndVocab, data);
    return ShaclValidator.get().validate(Shapes.parse(shapesAndVocab.getGraph()), all.getGraph()).conforms();
  }

  private static Resource asset(Model d, String account) {
    Resource a = d.createResource("urn:test:asset");
    a.addProperty(d.createProperty(ACCT + "assetType"), d.createResource(ACCT + "Equipment"));
    if (account != null) {
      a.addProperty(d.createProperty(ACCT + "ledgerAccount"), account);
    }
    return a;
  }

  @Test
  void ledgerAccountPattern() {
    for (String acc : new String[] {"123", "12050", "12a5"}) {
      assertFalse(conforms(CORE, asset0(acc)), acc);
    }
    assertTrue(conforms(CORE, asset0("1205")));
  }

  private static Model asset0(String account) {
    Model d = ModelFactory.createDefaultModel();
    asset(d, account);
    return d;
  }

  @Test
  void inconsistentGroupAndClass() {
    Model d = ModelFactory.createDefaultModel();
    asset(d, "1205").addProperty(d.createProperty(ACCT + "accountGroup"), "13");
    assertFalse(conforms(CORE, d));
    Model ok = ModelFactory.createDefaultModel();
    asset(ok, "1205").addProperty(ok.createProperty(ACCT + "accountGroup"), "12")
        .addProperty(ok.createProperty(ACCT + "accountClass"), "1");
    assertTrue(conforms(CORE, ok));
    Model badClass = ModelFactory.createDefaultModel();
    asset(badClass, "1205").addProperty(badClass.createProperty(ACCT + "accountClass"), "2");
    assertFalse(conforms(CORE, badClass));
  }

  @Test
  void ledger17ShapedAssetConforms() {
    Model d = ModelFactory.createDefaultModel();
    Resource a = asset(d, "1205");
    a.addProperty(d.createProperty(ACCT + "disposedAt"), d.createTypedLiteral("2026-01-31", XSDDatatype.XSDdate));
    Resource v = d.createResource("urn:test:valuation").addProperty(RDF.type, d.createResource(ACCT + "AssetValuation"));
    v.addLiteral(d.createProperty(ACCT + "year"), d.createTypedLiteral("2025", XSDDatatype.XSDinteger));
    a.addProperty(d.createProperty(ACCT + "valuations"), v);
    assertTrue(conforms(CORE, d));
  }

  @Test
  void assetNeedsAssetTypeInTheList() {
    Model d = ModelFactory.createDefaultModel();
    d.createResource("urn:test:asset")
        .addProperty(d.createProperty(ACCT + "assetType"), d.createResource(ACCT + "Equipment"))
        .addProperty(d.createProperty(ACCT + "assetType"), d.createResource(ACCT + "Building"));
    assertFalse(conforms(CORE, d));
    Model e = ModelFactory.createDefaultModel();
    e.createResource("urn:test:asset").addProperty(e.createProperty(ACCT + "assetType"), e.createResource("urn:other:Thing"));
    assertFalse(conforms(CORE, e));
    Model f = ModelFactory.createDefaultModel();
    asset(f, "1205").addProperty(f.createProperty(ACCT + "disposedAt"), "not a date");
    assertFalse(conforms(CORE, f));
  }

  @Test
  void valuationWithoutYear() {
    Model d = ModelFactory.createDefaultModel();
    d.createResource("urn:test:valuation").addProperty(RDF.type, d.createResource(ACCT + "AssetValuation"));
    assertFalse(conforms(CORE, d));
  }

  @Test
  void norwegianLedgerAccountRange() {
    assertFalse(conforms(NO, asset0("0123")));
    assertFalse(conforms(NO, asset0("9123")));
    assertTrue(conforms(NO, asset0("1205")));
    // core alone accepts 0123 (chart-neutral)
    assertTrue(conforms(CORE, asset0("0123")));
  }

  @Test
  void bothWealthFieldsRejected() {
    Model d = ModelFactory.createDefaultModel();
    Resource v = d.createResource("urn:test:valuation").addProperty(RDF.type, d.createResource(ACCT + "AssetValuation"));
    v.addLiteral(d.createProperty(ACCT + "year"), d.createTypedLiteral("2025", XSDDatatype.XSDinteger));
    v.addLiteral(d.createProperty(ACCTNO + "wealthValue"), 100);
    assertTrue(conforms(NO, d));
    v.addLiteral(d.createProperty(ACCTNO + "wealthValuePerShare"), 10);
    assertFalse(conforms(NO, d));
  }

  @Test
  void messagesInBothLanguages() {
    Model m = AccountingTaxonomy.load(Set.of("NO")).model();
    var shProperty = m.createProperty("http://www.w3.org/ns/shacl#property");
    var shSparql = m.createProperty("http://www.w3.org/ns/shacl#sparql");
    var shMessage = m.createProperty("http://www.w3.org/ns/shacl#message");
    var messageNo = m.createProperty(ACCT + "messageNo");
    java.util.List<org.apache.jena.rdf.model.RDFNode> nodes = new java.util.ArrayList<>();
    nodes.addAll(m.listObjectsOfProperty(shProperty).toList());
    nodes.addAll(m.listObjectsOfProperty(shSparql).toList());
    assertFalse(nodes.isEmpty());
    for (var n : nodes) {
      Resource r = n.asResource();
      var en = r.listProperties(shMessage).toList();
      assertEquals(1, en.size(), r + " sh:message count");
      assertEquals("en", en.get(0).getLanguage(), r.toString());
      var no = r.listProperties(messageNo).toList();
      assertEquals(1, no.size(), r + " acct:messageNo count");
      assertEquals("no", no.get(0).getLanguage(), r.toString());
    }
  }
}
