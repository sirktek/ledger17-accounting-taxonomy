package com.ledger17.taxonomy.accounting;

import static com.ledger17.taxonomy.accounting.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.junit.jupiter.api.Test;

class NamingTest {
  /** Built from parts so this source does not contain the forbidden word itself. */
  private static final Pattern FORBIDDEN = Pattern.compile("(?i)\\b" + "n" + "rs" + "\\b");

  private static List<Path> files(String root) throws IOException {
    try (Stream<Path> s = Files.walk(Paths.get(root))) {
      return s.filter(Files::isRegularFile).toList();
    }
  }

  @Test
  void noForbiddenTokenInSourceNamesOrContent() throws IOException {
    for (Path p : files("src")) {
      assertFalse(FORBIDDEN.matcher(p.toString()).find(), "path " + p);
      assertFalse(FORBIDDEN.matcher(Files.readString(p)).find(), "content of " + p);
    }
  }

  @Test
  void noIsolationViolationsInOntologyFiles() throws IOException {
    for (Path p : files("src/main/resources/ontologies")) {
      String text =
          Files.readAllLines(p).stream()
              .filter(l -> !l.stripLeading().startsWith("#"))
              .collect(java.util.stream.Collectors.joining("\n"))
              .toLowerCase();
      assertFalse(text.contains("sirktek"), p.toString());
      assertFalse(Pattern.compile("(?<![-\\w])schema:").matcher(text).find(), p.toString());
      assertFalse(text.contains("schema.org"), p.toString());
      assertFalse(text.contains("domainincludes"), p.toString());
    }
  }

  @Test
  void modelHasNoForbiddenIrisAndNoJurisdictionProperty() {
    Model m = AccountingTaxonomy.load(Set.of("NO")).model();
    for (Statement s : m.listStatements().toList()) {
      for (String u : new String[] {s.getSubject().getURI(), s.getPredicate().getURI()}) {
        if (u != null) {
          assertFalse(FORBIDDEN.matcher(u).find(), u);
          assertFalse(u.toLowerCase().contains("sirktek"), u);
          assertFalse(u.toLowerCase().endsWith("jurisdiction"), u);
        }
      }
      RDFNode o = s.getObject();
      if (o.isURIResource()) {
        assertFalse(FORBIDDEN.matcher(o.asResource().getURI()).find());
      } else if (o.isLiteral()) {
        assertFalse(FORBIDDEN.matcher(o.asLiteral().getLexicalForm()).find(), o.toString());
      }
    }
  }

  @Test
  void namespacesAndPrefixesAreAsDesigned() throws IOException {
    assertEquals("http://ledger17.com/ontology/accounting#", ACCT);
    assertEquals("http://ledger17.com/ontology/accounting/no#", ACCTNO);
    Pattern prefix = Pattern.compile("(?m)^@prefix\\s+([A-Za-z0-9]*):");
    Set<String> core = Set.of("rdf", "rdfs", "xsd", "skos", "sh", "dct", "acct");
    var coreText = Files.readString(Paths.get("src/main/resources/ontologies/accounting-core.ttl"));
    var m = prefix.matcher(coreText);
    while (m.find()) {
      assertTrue(core.contains(m.group(1)), "core prefix " + m.group(1));
    }
    var noText = Files.readString(Paths.get("src/main/resources/ontologies/accounting-no.ttl"));
    m = prefix.matcher(noText);
    while (m.find()) {
      assertTrue(core.contains(m.group(1)) || m.group(1).equals("acctno"), "no prefix " + m.group(1));
    }
    // core defines nothing in a country namespace
    for (Statement s : file("/ontologies/accounting-core.ttl").listStatements().toList()) {
      assertFalse(s.getSubject().isURIResource() && s.getSubject().getURI().startsWith(ACCTNO));
      assertFalse(s.getObject().isURIResource() && s.getObject().asResource().getURI().contains("/accounting/"));
    }
  }
}
