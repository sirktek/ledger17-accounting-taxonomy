package com.ledger17.taxonomy.accounting;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

/** The accounting vocabulary: core plus the modules of the requested jurisdictions. */
public final class AccountingTaxonomy {
  /** Namespace of the core vocabulary. */
  public static final String CORE_NAMESPACE = "http://ledger17.com/ontology/accounting#";
  /** Namespace of the Norway module. */
  public static final String NO_NAMESPACE = "http://ledger17.com/ontology/accounting/no#";
  /** Classpath resource of the core ontology. */
  public static final String CORE_RESOURCE = "/ontologies/accounting-core.ttl";
  /** Classpath resource of the Norway module. */
  public static final String NO_RESOURCE = "/ontologies/accounting-no.ttl";
  private static final Map<String, Module> MODULES = Map.of("NO", new Module(NO_NAMESPACE, NO_RESOURCE));
  /** Supported jurisdictions, ISO 3166-1 alpha-2. */
  public static final Set<String> SUPPORTED_JURISDICTIONS = Set.copyOf(MODULES.keySet());

  static final String SKOS = "http://www.w3.org/2004/02/skos/core#";

  private record Module(String namespace, String resource) {}

  private static final List<String> ASSET_TYPE_ORDER =
      List.of(
          "INTANGIBLE",
          "BUILDING",
          "EQUIPMENT",
          "FINANCIAL_FIXED_ASSET",
          "FINANCIAL_INSTRUMENTS",
          "BANK_ACCOUNT");
  private static final List<String> ACCOUNT_TYPE_ORDER =
      List.of(
          "AssetAccount",
          "EquityAndLiabilitiesAccount",
          "EquityAccount",
          "LiabilityAccount",
          "RevenueAccount",
          "ExpenseAccount");

  final Model model;
  private final Set<String> jurisdictions;
  private final NorwegianRules norway;
  private final Map<String, Resource> assetTypeResources;
  private final List<AssetTypeDef> assetTypes;
  private final Map<String, AssetTypeDef> assetTypesByCode;
  private final List<AccountTypeDef> accountTypes;
  private final List<PropertyDef> properties;
  private final Map<String, PropertyDef> propertiesByName;

  AccountingTaxonomy(Model model, Set<String> jurisdictions) {
    this.model = model;
    this.jurisdictions = jurisdictions;
    this.assetTypeResources = scanAssetTypeResources();
    this.assetTypes = scanAssetTypes();
    Map<String, AssetTypeDef> byCode = new LinkedHashMap<>();
    for (AssetTypeDef d : assetTypes) {
      byCode.put(d.code(), d);
    }
    this.assetTypesByCode = Collections.unmodifiableMap(byCode);
    this.accountTypes = scanAccountTypes();
    this.properties = scanProperties();
    Map<String, PropertyDef> byName = new LinkedHashMap<>();
    for (PropertyDef p : properties) {
      PropertyDef previous = byName.put(p.name(), p);
      if (previous != null) {
        throw new IllegalStateException(
            "Duplicate property local name " + p.name() + ": " + previous.uri() + " and " + p.uri());
      }
    }
    this.propertiesByName = Collections.unmodifiableMap(byName);
    this.norway = jurisdictions.contains("NO") ? new NorwegianRules(this) : null;
  }

  /**
   * Loads core plus the module of each jurisdiction. An empty set loads core only.
   *
   * @throws IllegalArgumentException for a null, lowercase or unknown code
   * @throws IllegalStateException if a resource is missing or cannot be parsed, or if two loaded
   *     modules define a property with the same local name
   */
  public static AccountingTaxonomy load(Set<String> jurisdictions) {
    if (jurisdictions == null) {
      throw unsupported(null);
    }
    for (String code : jurisdictions) {
      if (code == null || !MODULES.containsKey(code)) {
        throw unsupported(code);
      }
    }
    Model model = ModelFactory.createDefaultModel();
    read(model, CORE_RESOURCE);
    for (String code : new TreeSet<>(jurisdictions)) {
      read(model, MODULES.get(code).resource());
    }
    return new AccountingTaxonomy(model, Set.copyOf(jurisdictions));
  }

  private static IllegalArgumentException unsupported(String code) {
    return new IllegalArgumentException(
        "Unsupported jurisdiction " + code + "; supported: " + new TreeSet<>(SUPPORTED_JURISDICTIONS));
  }

  private static void read(Model model, String resource) {
    try (InputStream in = AccountingTaxonomy.class.getResourceAsStream(resource)) {
      if (in == null) {
        throw new IllegalStateException("Missing resource " + resource);
      }
      RDFDataMgr.read(model, in, Lang.TURTLE);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot read " + resource, e);
    } catch (RuntimeException e) {
      if (e instanceof IllegalStateException ise) {
        throw ise;
      }
      throw new IllegalStateException("Cannot parse " + resource, e);
    }
  }

  /** The loaded jurisdictions. */
  public Set<String> jurisdictions() {
    return jurisdictions;
  }

  /** The union of all loaded files, including shapes. Callers must not mutate it. */
  public Model model() {
    return model;
  }

  /** Asset types in design order. */
  public List<AssetTypeDef> assetTypes() {
    return assetTypes;
  }

  private List<AssetTypeDef> scanAssetTypes() {
    List<AssetTypeDef> result = new ArrayList<>();
    for (Resource r : model.listSubjectsWithProperty(RDF.type, res(CORE_NAMESPACE + "AssetType")).toList()) {
      result.add(assetTypeDef(r));
    }
    result.sort(
        Comparator.comparingInt((AssetTypeDef d) -> orderIndex(ASSET_TYPE_ORDER, d.code()))
            .thenComparing(AssetTypeDef::code));
    return List.copyOf(result);
  }

  /** The asset type with the given notation. */
  public Optional<AssetTypeDef> assetType(String code) {
    return Optional.ofNullable(assetTypesByCode.get(code));
  }

  /** Account types in design order, with their broader type. */
  public List<AccountTypeDef> accountTypes() {
    return accountTypes;
  }

  private List<AccountTypeDef> scanAccountTypes() {
    List<AccountTypeDef> result = new ArrayList<>();
    for (Resource r : model.listSubjectsWithProperty(RDF.type, res(CORE_NAMESPACE + "AccountType")).toList()) {
      Resource broader = resource(r, SKOS + "broader");
      result.add(
          new AccountTypeDef(
              r.getURI(), label(r, "en"), label(r, "no"), broader == null ? null : broader.getURI()));
    }
    result.sort(
        Comparator.comparingInt((AccountTypeDef d) -> orderIndex(ACCOUNT_TYPE_ORDER, localName(d.uri())))
            .thenComparing(AccountTypeDef::uri));
    return List.copyOf(result);
  }

  /** Properties declared on assets or valuations in the loaded modules (annotations excluded). */
  public List<PropertyDef> properties() {
    return properties;
  }

  private List<PropertyDef> scanProperties() {
    Resource asset = res(CORE_NAMESPACE + "Asset");
    Resource valuation = res(CORE_NAMESPACE + "AssetValuation");
    List<PropertyDef> result = new ArrayList<>();
    for (Resource r : model.listSubjectsWithProperty(RDF.type, RDF.Property).toList()) {
      Resource domain = resource(r, RDFS.domain.getURI());
      if (r.isURIResource() && domain != null && (domain.equals(asset) || domain.equals(valuation))) {
        result.add(propertyDef(r, domain));
      }
    }
    result.sort(Comparator.comparing(PropertyDef::uri));
    return List.copyOf(result);
  }

  /** The property with the given local name across loaded modules. */
  public Optional<PropertyDef> property(String localName) {
    return Optional.ofNullable(propertiesByName.get(localName));
  }

  /** Whether the property applies to the asset type; an absent restriction means all types. */
  public boolean appliesTo(String propertyLocalName, String assetTypeCode) {
    if (assetType(assetTypeCode).isEmpty()) {
      return false;
    }
    return property(propertyLocalName)
        .map(p -> p.appliesTo().isEmpty() || p.appliesTo().contains(assetTypeCode))
        .orElse(false);
  }

  /**
   * The Norway rules view.
   *
   * @throws IllegalStateException if the Norway module is not loaded
   */
  public NorwegianRules norway() {
    if (norway == null) {
      throw new IllegalStateException("Norway module not loaded; use load(Set.of(\"NO\"))");
    }
    return norway;
  }

  // ---- helpers shared with NorwegianRules ----

  Resource res(String uri) {
    return model.createResource(uri);
  }

  Property prop(String uri) {
    return model.createProperty(uri);
  }

  Resource resource(Resource s, String propertyUri) {
    Statement st = s.getProperty(prop(propertyUri));
    return st != null && st.getObject().isResource() ? st.getObject().asResource() : null;
  }

  String string(Resource s, String propertyUri) {
    Statement st = s.getProperty(prop(propertyUri));
    return st == null ? null : st.getObject().asLiteral().getLexicalForm();
  }

  List<Resource> resources(Resource s, String propertyUri) {
    return model.listObjectsOfProperty(s, prop(propertyUri)).toList().stream()
        .filter(RDFNode::isResource)
        .map(RDFNode::asResource)
        .collect(Collectors.toList());
  }

  List<String> strings(Resource s, String propertyUri) {
    return model.listObjectsOfProperty(s, prop(propertyUri)).toList().stream()
        .filter(RDFNode::isLiteral)
        .map(n -> n.asLiteral().getLexicalForm())
        .collect(Collectors.toList());
  }

  String label(Resource r, String lang) {
    return langLiteral(r, RDFS.label, lang);
  }

  AssetTypeDef assetTypeDef(Resource r) {
    String cp = string(r, CORE_NAMESPACE + "requiresCounterparty");
    return new AssetTypeDef(
        r.getURI(),
        string(r, SKOS + "notation"),
        label(r, "en"),
        label(r, "no"),
        langLiteral(r, RDFS.comment, "en"),
        Boolean.parseBoolean(cp));
  }

  private PropertyDef propertyDef(Resource r, Resource domain) {
    Resource range = resource(r, RDFS.range.getURI());
    Set<String> applies = new TreeSet<>();
    for (Resource t : resources(r, CORE_NAMESPACE + "appliesToAssetType")) {
      applies.add(string(t, SKOS + "notation"));
    }
    String ns = r.getNameSpace();
    String jurisdiction = null;
    for (Map.Entry<String, Module> e : MODULES.entrySet()) {
      if (e.getValue().namespace().equals(ns)) {
        jurisdiction = e.getKey();
      }
    }
    return new PropertyDef(
        r.getURI(),
        r.getLocalName(),
        label(r, "en"),
        label(r, "no"),
        langLiteral(r, RDFS.comment, "en"),
        domain.getURI(),
        range == null ? null : range.getURI(),
        Set.copyOf(applies),
        jurisdiction);
  }

  private String langLiteral(Resource r, Property p, String lang) {
    for (Statement st : r.listProperties(p).toList()) {
      RDFNode o = st.getObject();
      if (o.isLiteral()) {
        Literal l = o.asLiteral();
        if (lang.equalsIgnoreCase(l.getLanguage())) {
          return l.getLexicalForm();
        }
      }
    }
    return null;
  }

  static String localName(String uri) {
    int i = Math.max(uri.lastIndexOf('#'), uri.lastIndexOf('/'));
    return uri.substring(i + 1);
  }

  private static int orderIndex(List<String> order, String key) {
    int i = order.indexOf(key);
    return i < 0 ? order.size() : i;
  }

  Map<String, Resource> assetTypeResources() {
    return assetTypeResources;
  }

  private Map<String, Resource> scanAssetTypeResources() {
    Map<String, Resource> m = new LinkedHashMap<>();
    for (Resource r : model.listSubjectsWithProperty(RDF.type, res(CORE_NAMESPACE + "AssetType")).toList()) {
      m.put(string(r, SKOS + "notation"), r);
    }
    return Collections.unmodifiableMap(m);
  }
}
