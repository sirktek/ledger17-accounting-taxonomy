package com.ledger17.taxonomy.accounting;

import java.util.regex.Pattern;

/** Chart-neutral helpers for four-digit ledger account numbers: class (first digit), group (first two). */
public final class AccountNumbers {
  private static final Pattern FOUR_DIGITS = Pattern.compile("^[0-9]{4}$");

  private AccountNumbers() {}

  /** Whether the value is exactly four ASCII digits. */
  public static boolean isValid(String account) {
    return account != null && FOUR_DIGITS.matcher(account).matches();
  }

  /** The first two digits. @throws IllegalArgumentException if the account is not four digits */
  public static String accountGroup(String account) {
    require(account);
    return account.substring(0, 2);
  }

  /** The first digit. @throws IllegalArgumentException if the account is not four digits */
  public static String accountClass(String account) {
    require(account);
    return account.substring(0, 1);
  }

  private static void require(String account) {
    if (!isValid(account)) {
      throw new IllegalArgumentException("Not a four-digit account number: " + account);
    }
  }
}
