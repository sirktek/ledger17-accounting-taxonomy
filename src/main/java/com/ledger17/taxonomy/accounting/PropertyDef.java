package com.ledger17.taxonomy.accounting;

import java.util.Set;

/**
 * An asset-level or valuation-level property.
 *
 * @param uri the property IRI
 * @param name the local name (the JSON key)
 * @param labelEn English label
 * @param labelNo Norwegian label
 * @param comment English comment
 * @param domain domain class IRI
 * @param range range IRI
 * @param appliesTo asset type codes it applies to; empty means all
 * @param jurisdiction ISO 3166-1 alpha-2 module code, or null for core
 */
public record PropertyDef(
    String uri,
    String name,
    String labelEn,
    String labelNo,
    String comment,
    String domain,
    String range,
    Set<String> appliesTo,
    String jurisdiction) {}
