package com.ledger17.taxonomy.accounting;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AccountNumbersTest {
  @Test
  void validity() {
    assertTrue(AccountNumbers.isValid("1205"));
    assertTrue(AccountNumbers.isValid("0123"));
    for (String bad : new String[] {"123", "12050", "12a5", "", " 1205", "1205\n"}) {
      assertFalse(AccountNumbers.isValid(bad), bad);
    }
    assertFalse(AccountNumbers.isValid(null));
  }

  @Test
  void derivation() {
    assertEquals("12", AccountNumbers.accountGroup("1205"));
    assertEquals("1", AccountNumbers.accountClass("1205"));
    assertThrows(IllegalArgumentException.class, () -> AccountNumbers.accountGroup("12050"));
    assertThrows(IllegalArgumentException.class, () -> AccountNumbers.accountClass("12a5"));
  }
}
