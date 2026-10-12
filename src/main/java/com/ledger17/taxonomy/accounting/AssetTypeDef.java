package com.ledger17.taxonomy.accounting;

/**
 * An asset type.
 *
 * @param uri the type IRI
 * @param code the persisted code (skos:notation)
 * @param labelEn English label
 * @param labelNo Norwegian label
 * @param comment English comment
 * @param requiresCounterparty whether an asset of this type names a counterparty
 */
public record AssetTypeDef(
    String uri,
    String code,
    String labelEn,
    String labelNo,
    String comment,
    boolean requiresCounterparty) {}
