package com.ledger17.taxonomy.accounting;

import static com.ledger17.taxonomy.accounting.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.time.Year;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

class SchemaElementTest {
  private static final System.Logger LOG = System.getLogger(SchemaElementTest.class.getName());
  private static final String XSD = "http://www.w3.org/2001/XMLSchema";
  private static final String SKOS = "http://www.w3.org/2004/02/skos/core#";
  private static final Pattern FILE = Pattern.compile("/([^/]+\\.xsd)$");
  private static final AccountingTaxonomy T = AccountingTaxonomy.load(Set.of("NO"));
  private static final Model M = T.model();

  private static String str(Resource s, String p) {
    Statement st = s.getProperty(M.createProperty(ACCTNO + p));
    return st == null ? null : st.getObject().asLiteral().getLexicalForm();
  }

  private static Document parse(SchemaVersionRef v) throws Exception {
    try (InputStream in = SchemaElementTest.class.getResourceAsStream("/xsd/" + v.file)) {
      assertNotNull(in, "missing fixture " + v.file);
      DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
      f.setNamespaceAware(true);
      return f.newDocumentBuilder().parse(in);
    }
  }

  private record SchemaVersionRef(Resource node, String file, int year) {}

  private static Map<Resource, SchemaVersionRef> versions() {
    Map<Resource, SchemaVersionRef> out = new HashMap<>();
    M.listSubjectsWithProperty(RDF.type, M.createResource(ACCTNO + "SchemaVersion"))
        .forEachRemaining(
            v -> {
              String src = v.getProperty(M.createProperty("http://purl.org/dc/terms/source")).getObject().asResource().getURI();
              Matcher m = FILE.matcher(src);
              assertTrue(m.find(), src);
              assertTrue(src.contains("/blob/v1.62.115/src/resources/xsd/"), src);
              out.put(v, new SchemaVersionRef(v, m.group(1), Integer.parseInt(str(v, "incomeYear"))));
            });
    return out;
  }

  private static Element complexType(Document d, String name) {
    NodeList l = d.getDocumentElement().getElementsByTagNameNS(XSD, "complexType");
    for (int i = 0; i < l.getLength(); i++) {
      Element e = (Element) l.item(i);
      if (name.equals(e.getAttribute("name"))) {
        return e;
      }
    }
    return null;
  }

  /** True if the complex type declares the element, directly or through extension bases. */
  private static boolean declares(Document d, String type, String element) {
    Element ct = complexType(d, type);
    if (ct == null) {
      return false;
    }
    NodeList els = ct.getElementsByTagNameNS(XSD, "element");
    for (int i = 0; i < els.getLength(); i++) {
      if (element.equals(((Element) els.item(i)).getAttribute("name"))) {
        return true;
      }
    }
    NodeList ext = ct.getElementsByTagNameNS(XSD, "extension");
    for (int i = 0; i < ext.getLength(); i++) {
      String base = ((Element) ext.item(i)).getAttribute("base");
      if (declares(d, base, element)) {
        return true;
      }
    }
    return false;
  }

  private static void assertResolves(Document d, String path, String where) {
    String[] parts = path.split("/");
    assertEquals(2, parts.length, path);
    assertNotNull(complexType(d, parts[0]), "no complex type " + parts[0] + " in " + where);
    assertTrue(declares(d, parts[0], parts[1]), path + " not declared in " + where);
  }

  @Test
  void versionsMatchFixtures() throws Exception {
    Map<Resource, SchemaVersionRef> versions = versions();
    assertEquals(4, versions.size());
    for (SchemaVersionRef v : versions.values()) {
      Document d = parse(v);
      assertEquals(str(v.node, "schemaNamespace"), d.getDocumentElement().getAttribute("targetNamespace"), v.file);
      assertTrue(v.file.contains("_v" + str(v.node, "schemaVersion") + "_"), v.file);
      assertTrue(v.file.startsWith(str(v.node, "schemaName") + "_"), v.file);
      // income year: all 20xx_ prefixes of named code lists agree with incomeYear
      Set<String> years = new HashSet<>();
      Matcher m = Pattern.compile("eksternKodeliste=\"/formuesOgInntektsskatt/skattemelding/(\\d{4})_").matcher(slurp(v));
      while (m.find()) {
        years.add(m.group(1));
      }
      assertEquals(Set.of(String.valueOf(v.year)), years, v.file);
    }
  }

  private static String slurp(SchemaVersionRef v) throws Exception {
    try (InputStream in = SchemaElementTest.class.getResourceAsStream("/xsd/" + v.file)) {
      return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
    }
  }

  @Test
  void everyMappedElementResolves() throws Exception {
    Map<Resource, SchemaVersionRef> versions = versions();
    Set<Resource> mappings = new HashSet<>();
    M.listSubjectsWithProperty(RDF.type, M.createResource(ACCTNO + "SchemaElementMapping")).forEachRemaining(mappings::add);
    assertEquals(8, mappings.size());
    for (Resource mp : mappings) {
      Resource schema = mp.getPropertyResourceValue(M.createProperty(ACCTNO + "inSchema"));
      SchemaVersionRef v = versions.get(schema);
      assertNotNull(v, "mapping points at unknown schema version");
      Document d = parse(v);
      assertResolves(d, str(mp, "schemaElement"), v.file);
      String opening = str(mp, "openingSchemaElement");
      if (opening != null) {
        assertResolves(d, opening, v.file);
      }
    }
  }

  @Test
  void mappedTermsCoverEveryListedVersion() {
    Map<String, Set<String>> expected =
        Map.of(
            ACCT + "depreciationGroup", Set.of("naeringsspesifikasjon:6", "naeringsspesifikasjon:7"),
            ACCTNO + "depreciationRate", Set.of("naeringsspesifikasjon:6", "naeringsspesifikasjon:7"),
            ACCT + "taxValue", Set.of("naeringsspesifikasjon:6", "naeringsspesifikasjon:7"),
            ACCTNO + "wealthValue", Set.of("skattemeldingUpersonlig:5", "skattemeldingUpersonlig:6"));
    Map<String, Set<String>> actual = new HashMap<>();
    M.listSubjectsWithProperty(RDF.type, M.createResource(ACCTNO + "SchemaElementMapping"))
        .forEachRemaining(
            mp -> {
              Resource term = mp.getPropertyResourceValue(M.createProperty(ACCTNO + "mapsTerm"));
              Resource sv = mp.getPropertyResourceValue(M.createProperty(ACCTNO + "inSchema"));
              actual
                  .computeIfAbsent(term.getURI(), k -> new HashSet<>())
                  .add(str(sv, "schemaName") + ":" + str(sv, "schemaVersion"));
            });
    assertEquals(expected, actual);
  }

  @Test
  void mappingsAreNeverSkosMatches() {
    Set<Resource> mapped = new HashSet<>();
    M.listObjectsOfProperty(M.createProperty(ACCTNO + "mapsTerm")).forEachRemaining(o -> mapped.add(o.asResource()));
    for (String p : new String[] {"closeMatch", "exactMatch", "broadMatch", "narrowMatch", "relatedMatch"}) {
      M.listStatements(null, M.createProperty(SKOS + p), (RDFNode) null)
          .forEachRemaining(
              st -> {
                assertFalse(st.getObject().toString().contains("github.com"), st.toString());
                assertFalse(st.getSubject().hasProperty(M.createProperty(ACCTNO + "schemaElement")), st.toString());
                assertFalse(mapped.contains(st.getObject()), st.toString());
              });
    }
  }

  @Test
  void fixturesCarryNoDocumentation() throws Exception {
    for (SchemaVersionRef v : versions().values()) {
      String text = slurp(v);
      assertFalse(text.contains("xsd:annotation"), v.file);
      assertFalse(text.contains("documentation"), v.file);
    }
  }

  @Test
  void filingYearCoverageOnlyWarns() {
    int filingYear = Year.now(ZoneId.of("Europe/Oslo")).getValue() - 1;
    for (String schema : new String[] {"naeringsspesifikasjon", "skattemeldingUpersonlig"}) {
      boolean covered =
          versions().values().stream().anyMatch(v -> schema.equals(str(v.node, "schemaName")) && v.year == filingYear);
      if (!covered) {
        LOG.log(
            System.Logger.Level.WARNING,
            "No {0} schema version for income year {1}; add one to the Norway module",
            schema,
            filingYear);
      }
    }
  }
}
