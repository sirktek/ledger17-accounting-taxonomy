package com.ledger17.taxonomy.accounting;

import java.util.List;

/**
 * SAF-T grouping reference of an asset type.
 *
 * @param groupingCategory the grouping category, or null if none
 * @param groupingCodes the grouping codes within the type's range, sorted
 */
public record SaftReference(String groupingCategory, List<String> groupingCodes) {}
