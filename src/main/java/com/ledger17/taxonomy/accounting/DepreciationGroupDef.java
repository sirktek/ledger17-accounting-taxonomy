package com.ledger17.taxonomy.accounting;

import java.math.BigDecimal;

/**
 * A tax depreciation group.
 *
 * @param uri the group IRI
 * @param notation the code, for example "d"
 * @param labelEn English label
 * @param labelNo Norwegian label
 * @param rate statutory maximum declining-balance rate in percent
 */
public record DepreciationGroupDef(
    String uri, String notation, String labelEn, String labelNo, BigDecimal rate) {}
