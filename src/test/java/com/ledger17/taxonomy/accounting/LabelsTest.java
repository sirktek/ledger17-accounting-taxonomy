package com.ledger17.taxonomy.accounting;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.Test;

class LabelsTest {
  private static final String SH = "http://www.w3.org/ns/shacl#";

  private static long count(Model m, Resource s, org.apache.jena.rdf.model.Property p, String lang) {
    return m.listStatements(s, p, (org.apache.jena.rdf.model.RDFNode) null).toList().stream()
        .filter(st -> st.getObject().isLiteral() && lang.equals(st.getObject().asLiteral().getLanguage()))
        .count();
  }

  @Test
  void everyDefinedSubjectHasLabelsAndComment() {
    for (String res : List.of("/ontologies/accounting-core.ttl", "/ontologies/accounting-no.ttl")) {
      Model m = TestSupport.file(res);
      int checked = 0;
      for (Resource s : m.listSubjectsWithProperty(RDF.type).toList()) {
        if (!s.isURIResource() || m.contains(s, RDF.type, m.createResource(SH + "NodeShape"))) {
          continue;
        }
        checked++;
        assertEquals(1, count(m, s, RDFS.label, "en"), res + " en label " + s);
        assertEquals(1, count(m, s, RDFS.label, "no"), res + " no label " + s);
        assertTrue(count(m, s, RDFS.comment, "en") >= 1, res + " en comment " + s);
        for (Statement st : m.listStatements(s, RDFS.label, (org.apache.jena.rdf.model.RDFNode) null).toList()) {
          assertFalse(st.getObject().asLiteral().getLanguage().isEmpty(), "untagged label " + s);
        }
      }
      assertTrue(checked > 10, res);
    }
  }
}
