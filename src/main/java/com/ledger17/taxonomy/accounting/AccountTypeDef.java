package com.ledger17.taxonomy.accounting;

/**
 * An account type.
 *
 * @param uri the type IRI
 * @param labelEn English label
 * @param labelNo Norwegian label
 * @param broader IRI of the parent type, or null
 */
public record AccountTypeDef(String uri, String labelEn, String labelNo, String broader) {}
