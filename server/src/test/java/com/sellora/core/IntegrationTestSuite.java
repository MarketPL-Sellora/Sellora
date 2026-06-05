package com.sellora.core;

import com.sellora.core.presentation.controllers.*;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
  AuthControllerIT.class,
  CartControllerIT.class,
  GroupBuySessionControllerIT.class,
  ProductControllerIT.class,
  StoreControllerIT.class
  // Додавайте сюди нові класи IT тестів
})
public class IntegrationTestSuite {
  // Цей клас залишається порожнім.
  // Він слугує лише точкою входу (Runner-ом) для JUnit.
}
