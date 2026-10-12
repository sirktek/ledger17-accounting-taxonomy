package com.ledger17.taxonomy.accounting;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SaftReferenceTest {
  private static final Set<String> NOT_ASSETS = Set.of("1296", "1298", "1299");
  private static final AccountingTaxonomy T = AccountingTaxonomy.load(Set.of("NO"));

  /** Category and code rows of the fixture. */
  static List<String[]> fixture() throws Exception {
    List<String[]> rows = new ArrayList<>();
    try (InputStream in = SaftReferenceTest.class.getResourceAsStream("/saft/naeringsspesifikasjon.csv")) {
      assertNotNull(in, "missing saft fixture");
      BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
      assertEquals("GroupingCategory;GroupingCode", r.readLine());
      String line;
      while ((line = r.readLine()) != null) {
        if (!line.isBlank()) {
          String[] cols = line.split(";");
          assertEquals(2, cols.length, "codes only: " + line);
          rows.add(cols);
        }
      }
    }
    return rows;
  }

  @Test
  void codesAreInFixtureAndInsideRange() throws Exception {
    Set<String> all = new HashSet<>();
    for (String[] row : fixture()) {
      all.add(row[1]);
    }
    for (AssetTypeDef type : T.assetTypes()) {
      SaftReference ref = T.norway().saft(type.code());
      assertNotNull(ref.groupingCategory(), type.code());
      List<AccountRange> ranges = T.norway().accountRanges(type.code());
      for (String code : ref.groupingCodes()) {
        assertTrue(all.contains(code), code + " not in fixture");
        assertTrue(ranges.stream().anyMatch(r -> r.contains(code)), code + " outside range of " + type.code());
        assertFalse(NOT_ASSETS.contains(code), code);
      }
    }
  }

  @Test
  void everyFixtureCodeInsideARangeIsListed() throws Exception {
    for (String[] row : fixture()) {
      String category = row[0];
      String code = row[1];
      if (NOT_ASSETS.contains(code)) {
        continue;
      }
      var type = T.norway().assetTypeForAccount(code);
      if (type.isPresent()) {
        SaftReference ref = T.norway().saft(type.get().code());
        assertEquals(category, ref.groupingCategory(), code);
        assertTrue(ref.groupingCodes().contains(code), code + " missing on " + type.get().code());
      }
    }
  }

  @Test
  void unknownTypeHasNoCodes() {
    assertNull(T.norway().saft("NOPE").groupingCategory());
    assertTrue(T.norway().saft("NOPE").groupingCodes().isEmpty());
  }
}
