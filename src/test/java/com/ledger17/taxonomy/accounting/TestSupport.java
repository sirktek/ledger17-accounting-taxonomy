package com.ledger17.taxonomy.accounting;

import java.io.InputStream;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

/** Shared helpers for the ontology tests. */
final class TestSupport {
  static final String ACCT = AccountingTaxonomy.CORE_NAMESPACE;
  static final String ACCTNO = AccountingTaxonomy.NO_NAMESPACE;

  private TestSupport() {}

  /** Parses one packaged ontology file on its own. */
  static Model file(String resource) {
    Model m = ModelFactory.createDefaultModel();
    try (InputStream in = TestSupport.class.getResourceAsStream(resource)) {
      RDFDataMgr.read(m, in, Lang.TURTLE);
    } catch (java.io.IOException e) {
      throw new IllegalStateException(e);
    }
    return m;
  }
}
