package com.ledger17.taxonomy.accounting;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

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
        for (Path f : files) {
            assertDoesNotThrow(() -> RDFDataMgr.loadModel(f.toUri().toString()), "Syntax error in " + f);
        }
    }
}
