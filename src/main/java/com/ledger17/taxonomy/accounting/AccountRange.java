package com.ledger17.taxonomy.accounting;

import java.util.Set;

/**
 * An inclusive span of four-digit account numbers.
 *
 * @param from first account
 * @param to last account
 * @param groups the two-digit groups the range touches
 * @param wholeGroups true when the range is exactly whole groups
 */
public record AccountRange(String from, String to, Set<String> groups, boolean wholeGroups) {
  /** Whether the account is a valid four-digit number inside this range. */
  public boolean contains(String account) {
    return AccountNumbers.isValid(account)
        && account.compareTo(from) >= 0
        && account.compareTo(to) <= 0;
  }
}
