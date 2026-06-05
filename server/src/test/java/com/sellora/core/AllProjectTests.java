package com.sellora.core;

import com.sellora.core.application.usecases.*;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
  AuthServiceTest.class,
  AuthControllerTest.class,
  CategoryServiceTest.class,
  GlobalExceptionHandlerTest.class,
  GroupBuySessionControllerTest.class,
  GroupBuySessionServiceTest.class,
  JwtAuthenticationFilterTest.class,
  JwtServiceTest.class,
  ProductControllerTest.class,
  ProductServiceTest.class,
  ProductSpecificationTest.class,
  StoreServiceTest.class,
  CartServiceTest.class,
  CartControllerTest.class,
  FavoriteControllerTest.class,
  UserFavoriteServiceTest.class,
  MerchantRequisiteControllerTest.class,
  MerchantRequisiteServiceTest.class,
  PromoCodeControllerTest.class,
  ShippingCarrierControllerTest.class,
  UserSettingsControllerTest.class,
  UserSettingsServiceTest.class,
  UserControllerTest.class,
})
public class AllProjectTests {

}
