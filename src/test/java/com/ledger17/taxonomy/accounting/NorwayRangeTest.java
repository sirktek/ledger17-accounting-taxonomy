package com.ledger17.taxonomy.accounting;

import static com.ledger17.taxonomy.accounting.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.stream.Collectors;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.Test;

class NorwayRangeTest {
  private static final AccountingTaxonomy T = AccountingTaxonomy.load(Set.of("NO"));
  private static final NorwegianRules R = T.norway();

  private static String span(AccountRange r) {
    return r.from() + "-" + r.to();
  }

  @Test
  void assetTypeRanges() {
    Map<String, List<String>> expected = new LinkedHashMap<>();
    expected.put("INTANGIBLE", List.of("1000-1099"));
    expected.put("BUILDING", List.of("1100-1199"));
    expected.put("EQUIPMENT", List.of("1200-1299"));
    expected.put("FINANCIAL_FIXED_ASSET", List.of("1300-1399"));
    expected.put("FINANCIAL_INSTRUMENTS", List.of("1800-1899"));
    expected.put("BANK_ACCOUNT", List.of("1920-1920", "1950-1950", "1960-1960"));
    expected.forEach((code, spans) -> {
      assertEquals(spans, R.accountRanges(code).stream().map(NorwayRangeTest::span).sorted().toList(), code);
    });
    assertEquals(Set.of("10"), R.accountRanges("INTANGIBLE").get(0).groups());
    assertTrue(R.accountRanges("INTANGIBLE").get(0).wholeGroups());
    assertEquals(Set.of("12"), R.accountRanges("EQUIPMENT").get(0).groups());
    for (AccountRange r : R.accountRanges("BANK_ACCOUNT")) {
      assertEquals(Set.of("19"), r.groups());
      assertFalse(r.wholeGroups());
    }
    assertTrue(R.accountRanges("UNKNOWN").isEmpty());
  }

  @Test
  void assetTypeForAccount() {
    assertEquals("INTANGIBLE", R.assetTypeForAccount("1080").orElseThrow().code());
    assertEquals("BUILDING", R.assetTypeForAccount("1150").orElseThrow().code());
    assertEquals("EQUIPMENT", R.assetTypeForAccount("1230").orElseThrow().code());
    assertEquals("FINANCIAL_FIXED_ASSET", R.assetTypeForAccount("1350").orElseThrow().code());
    assertEquals("FINANCIAL_INSTRUMENTS", R.assetTypeForAccount("1810").orElseThrow().code());
    assertEquals("BANK_ACCOUNT", R.assetTypeForAccount("1950").orElseThrow().code());
    assertTrue(R.assetTypeForAccount("1930").isEmpty());
    assertTrue(R.assetTypeForAccount("12050").isEmpty());
  }

  @Test
  void accountTypesForAccount() {
    assertEquals(
        List.of(ACCT + "EquityAccount", ACCT + "EquityAndLiabilitiesAccount"),
        R.accountTypesForAccount("2050").stream().map(AccountTypeDef::uri).toList());
    assertEquals(
        List.of(ACCT + "LiabilityAccount", ACCT + "EquityAndLiabilitiesAccount"),
        R.accountTypesForAccount("2400").stream().map(AccountTypeDef::uri).toList());
    assertEquals(List.of(ACCT + "AssetAccount"), R.accountTypesForAccount("1205").stream().map(AccountTypeDef::uri).toList());
    assertEquals(List.of(ACCT + "ExpenseAccount"), R.accountTypesForAccount("7000").stream().map(AccountTypeDef::uri).toList());
    assertTrue(R.accountTypesForAccount("9000").isEmpty());
  }

  // ---- direct model checks of account type ranges ----

  private record Span(String from, String to, Set<String> groups) {}

  private static Span span(Model m, Resource owner) {
    Resource range = m.getProperty(owner, m.createProperty(ACCTNO + "accountRange")).getResource();
    String from = m.getProperty(range, m.createProperty(ACCTNO + "fromAccount")).getString();
    String to = m.getProperty(range, m.createProperty(ACCTNO + "toAccount")).getString();
    Set<String> groups =
        m.listObjectsOfProperty(range, m.createProperty(ACCTNO + "coversGroup")).toList().stream()
            .map(n -> n.asLiteral().getString()).collect(Collectors.toSet());
    return new Span(from, to, groups);
  }

  private static Span of(String type) {
    return span(T.model(), T.model().createResource(ACCT + type));
  }

  private static boolean overlap(Span a, Span b) {
    return a.from().compareTo(b.to()) <= 0 && b.from().compareTo(a.to()) <= 0;
  }

  private static boolean inside(Span child, Span parent) {
    return child.from().compareTo(parent.from()) >= 0 && child.to().compareTo(parent.to()) <= 0;
  }

  @Test
  void accountTypeRangesNest() {
    assertEquals("1000-1999", of("AssetAccount").from() + "-" + of("AssetAccount").to());
    assertEquals("2000-2999", of("EquityAndLiabilitiesAccount").from() + "-" + of("EquityAndLiabilitiesAccount").to());
    assertEquals("2000-2099", of("EquityAccount").from() + "-" + of("EquityAccount").to());
    assertEquals("2100-2999", of("LiabilityAccount").from() + "-" + of("LiabilityAccount").to());
    assertEquals("3000-3999", of("RevenueAccount").from() + "-" + of("RevenueAccount").to());
    assertEquals("4000-8999", of("ExpenseAccount").from() + "-" + of("ExpenseAccount").to());

    assertFalse(overlap(of("EquityAccount"), of("LiabilityAccount")));
    List<Span> top = List.of(of("AssetAccount"), of("EquityAndLiabilitiesAccount"), of("RevenueAccount"), of("ExpenseAccount"));
    for (int i = 0; i < top.size(); i++) {
      for (int j = i + 1; j < top.size(); j++) {
        assertFalse(overlap(top.get(i), top.get(j)), i + " vs " + j);
      }
    }
    assertTrue(inside(of("EquityAccount"), of("EquityAndLiabilitiesAccount")));
    assertTrue(inside(of("LiabilityAccount"), of("EquityAndLiabilitiesAccount")));
    for (String code : List.of("INTANGIBLE", "BUILDING", "EQUIPMENT", "FINANCIAL_FIXED_ASSET", "FINANCIAL_INSTRUMENTS", "BANK_ACCOUNT")) {
      for (AccountRange r : R.accountRanges(code)) {
        assertTrue(inside(new Span(r.from(), r.to(), r.groups()), of("AssetAccount")), code);
      }
    }
    // asset-type ranges do not overlap each other
    List<AccountRange> all = new ArrayList<>();
    for (AssetTypeDef d : T.assetTypes()) {
      all.addAll(R.accountRanges(d.code()));
    }
    for (int i = 0; i < all.size(); i++) {
      for (int j = i + 1; j < all.size(); j++) {
        assertFalse(
            overlap(new Span(all.get(i).from(), all.get(i).to(), null), new Span(all.get(j).from(), all.get(j).to(), null)));
      }
    }
  }

  @Test
  void coversGroupMatchesBounds() {
    List<Span> spans = new ArrayList<>();
    for (AssetTypeDef d : T.assetTypes()) {
      R.accountRanges(d.code()).forEach(r -> spans.add(new Span(r.from(), r.to(), r.groups())));
    }
    for (AccountTypeDef d : T.accountTypes()) {
      spans.add(span(T.model(), T.model().createResource(d.uri())));
    }
    for (Span s : spans) {
      Set<String> derived = new TreeSet<>();
      for (int g = Integer.parseInt(AccountNumbers.accountGroup(s.from()));
          g <= Integer.parseInt(AccountNumbers.accountGroup(s.to()));
          g++) {
        derived.add(String.format("%02d", g));
      }
      assertEquals(derived, new TreeSet<>(s.groups()), s.from() + "-" + s.to());
    }
  }

  @Test
  void ledgerRangeAccessorBounds() {
    AccountRange r = R.accountRanges("EQUIPMENT").get(0);
    assertTrue(r.contains("1200"));
    assertTrue(r.contains("1299"));
    assertFalse(r.contains("1300"));
    assertFalse(r.contains("120"));
  }
}
