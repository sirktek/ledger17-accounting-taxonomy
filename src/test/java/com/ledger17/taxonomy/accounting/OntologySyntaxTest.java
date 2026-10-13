package com.ledger17.taxonomy.accounting;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.jupiter.api.Test;

/** Syntax gate: every Turtle file under src/main/resources/ontologies must parse. */
class OntologySyntaxTest {

    @Test
    void allTurtleFilesParse() throws IOException {
        Path dir = Paths.get("src/main/resources/ontologies");
        List<Path> files;
        try (Stream<Path> s = Files.walk(dir)) {
            files = s.filter(p -> p.toString().endsWith(".ttl")).toList();
        }
        List<String> names = files.stream().map(p -> p.getFileName().toString()).toList();
        assertTrue(names.contains("accounting-core.ttl"), "found " + names);
        assertTrue(names.contains("accounting-no.ttl"), "found " + names);
        for (Path f : files) {
            org.apache.jena.rdf.model.Model m =
                assertDoesNotThrow(() -> RDFDataMgr.loadModel(f.toUri().toString()), "Syntax error in " + f);
            assertTrue(m.size() > 0, "empty model " + f);
        }
    }
}
