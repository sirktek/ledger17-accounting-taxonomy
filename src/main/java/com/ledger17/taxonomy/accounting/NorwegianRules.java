package com.ledger17.taxonomy.accounting;

import static com.ledger17.taxonomy.accounting.AccountingTaxonomy.CORE_NAMESPACE;
import static com.ledger17.taxonomy.accounting.AccountingTaxonomy.NO_NAMESPACE;
import static com.ledger17.taxonomy.accounting.AccountingTaxonomy.SKOS;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;

/** Norwegian rules: NS 4102 account ranges, depreciation groups, formuesverdi defaults, SAF-T codes. */
public final class NorwegianRules {
  private static final String SALDOGRUPPE = "saldogruppe";

  private final AccountingTaxonomy t;

  NorwegianRules(AccountingTaxonomy taxonomy) {
    this.t = taxonomy;
  }

  /** The account ranges of an asset type; empty if the code is unknown. */
  public List<AccountRange> accountRanges(String assetTypeCode) {
    Resource r = t.assetTypeResources().get(assetTypeCode);
    return r == null ? List.of() : ranges(r);
  }

  /** The asset type whose ranges contain the account, if any. */
  public Optional<AssetTypeDef> assetTypeForAccount(String account) {
    for (AssetTypeDef d : t.assetTypes()) {
      for (AccountRange range : accountRanges(d.code())) {
        if (range.contains(account)) {
          return Optional.of(d);
        }
      }
    }
    return Optional.empty();
  }

  /** Account types whose ranges contain the account, most specific first, then broader. */
  public List<AccountTypeDef> accountTypesForAccount(String account) {
    List<AccountTypeDef> result = new ArrayList<>();
    for (AccountTypeDef d : t.accountTypes()) {
      if (ranges(t.res(d.uri())).stream().anyMatch(r -> r.contains(account))) {
        result.add(d);
      }
    }
    result.sort(Comparator.comparingInt((AccountTypeDef d) -> -depth(d)));
    return List.copyOf(result);
  }

  private int depth(AccountTypeDef d) {
    int n = 0;
    String b = d.broader();
    while (b != null && n < 20) {
      n++;
      Resource parent = t.resource(t.res(b), SKOS + "broader");
      b = parent == null ? null : parent.getURI();
    }
    return n;
  }

  /** The default depreciation group notation of an asset type. */
  public Optional<String> defaultDepreciationGroup(String assetTypeCode) {
    Resource r = t.assetTypeResources().get(assetTypeCode);
    if (r == null) {
      return Optional.empty();
    }
    Resource g = t.resource(r, NO_NAMESPACE + "defaultDepreciationGroup");
    return g == null ? Optional.empty() : Optional.ofNullable(t.string(g, SKOS + "notation"));
  }

  /** Local name of the valuation property that counts as formuesverdi when none is given. */
  public Optional<String> wealthValueDefault(String assetTypeCode) {
    Resource r = t.assetTypeResources().get(assetTypeCode);
    if (r == null) {
      return Optional.empty();
    }
    Resource p = t.resource(r, NO_NAMESPACE + "wealthValueDefault");
    return p == null ? Optional.empty() : Optional.of(p.getLocalName());
  }

  /** Income years covered by the named code list. */
  public SortedSet<Integer> coveredYears(String codeListName) {
    SortedSet<Integer> years = new TreeSet<>();
    for (Resource cl : codeLists(codeListName)) {
      for (String y : t.strings(cl, NO_NAMESPACE + "incomeYear")) {
        years.add(Integer.parseInt(y));
      }
    }
    return years;
  }

  /**
   * Depreciation groups in the saldogruppe code list of the income year, sorted by notation.
   *
   * @throws IllegalArgumentException if no saldogruppe code list covers the year
   */
  public List<DepreciationGroupDef> depreciationGroups(int incomeYear) {
    Set<Resource> lists = new java.util.HashSet<>();
    for (Resource cl : codeLists(SALDOGRUPPE)) {
      if (t.strings(cl, NO_NAMESPACE + "incomeYear").contains(Integer.toString(incomeYear))) {
        lists.add(cl);
      }
    }
    if (lists.isEmpty()) {
      throw new IllegalArgumentException(
          "Income year " + incomeYear + " not covered; covered: " + coveredYears(SALDOGRUPPE));
    }
    List<DepreciationGroupDef> result = new ArrayList<>();
    for (Resource g :
        t.model.listSubjectsWithProperty(RDF.type, t.res(CORE_NAMESPACE + "DepreciationGroup")).toList()) {
      if (t.resources(g, NO_NAMESPACE + "inCodeList").stream().anyMatch(lists::contains)) {
        String rate = t.string(g, NO_NAMESPACE + "depreciationRate");
        result.add(
            new DepreciationGroupDef(
                g.getURI(),
                t.string(g, SKOS + "notation"),
                t.label(g, "en"),
                t.label(g, "no"),
                rate == null ? null : new BigDecimal(rate)));
      }
    }
    result.sort(Comparator.comparing(DepreciationGroupDef::notation));
    return List.copyOf(result);
  }

  /** SAF-T grouping category and codes of an asset type; category null and no codes if unknown. */
  public SaftReference saft(String assetTypeCode) {
    Resource r = t.assetTypeResources().get(assetTypeCode);
    if (r == null) {
      return new SaftReference(null, List.of());
    }
    List<String> codes = new ArrayList<>(t.strings(r, NO_NAMESPACE + "saftGroupingCode"));
    codes.sort(Comparator.naturalOrder());
    return new SaftReference(t.string(r, NO_NAMESPACE + "saftGroupingCategory"), List.copyOf(codes));
  }

  private List<Resource> codeLists(String name) {
    List<Resource> result = new ArrayList<>();
    for (Resource cl :
        t.model.listSubjectsWithProperty(RDF.type, t.res(NO_NAMESPACE + "CodeList")).toList()) {
      if (name.equals(t.string(cl, NO_NAMESPACE + "codeListName"))) {
        result.add(cl);
      }
    }
    return result;
  }

  private List<AccountRange> ranges(Resource owner) {
    List<AccountRange> result = new ArrayList<>();
    for (Resource rn : t.resources(owner, NO_NAMESPACE + "accountRange")) {
      String wg = t.string(rn, NO_NAMESPACE + "wholeGroups");
      result.add(
          new AccountRange(
              t.string(rn, NO_NAMESPACE + "fromAccount"),
              t.string(rn, NO_NAMESPACE + "toAccount"),
              Set.copyOf(t.strings(rn, NO_NAMESPACE + "coversGroup")),
              Boolean.parseBoolean(wg)));
    }
    result.sort(Comparator.comparing(AccountRange::from));
    return result;
  }
}
