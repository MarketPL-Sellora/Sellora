package com.sellora.core.presentation.e2e;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BuyProductWithDefaultPriceE2ETest{
  static Playwright playwright;
  static Browser browser;
  Page page;

  @BeforeAll
  static void launchBrowser() {
    playwright = Playwright.create();
    browser = playwright.firefox().launch(new BrowserType.LaunchOptions().setHeadless(false));
  }

  @BeforeEach
  void createContext() {
    page = browser.newPage();
  }

  @Test
  void testBuyingProductWithDefaultPrice() {
    page.navigate("http://localhost");

    page.locator("button:has-text('Увійти')").first().click();

    page.locator("input[type='email']").fill("toha@gmail.com");

    page.locator("input[type='password']").first().fill("123123123");

    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Увійти")).nth(1).click();

    page.waitForURL("http://localhost/");

    Locator productLink = page.locator("a[href='/product/2']");

    productLink.waitFor();

    productLink.click();


    Locator standardPriceButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Купити за стандартною ціною"));

    standardPriceButton.waitFor();
    standardPriceButton.click();

    Locator successMessage = page.locator("text=Товар додано до кошика!");

    successMessage.waitFor(new Locator.WaitForOptions().setTimeout(5000));

    assertTrue(successMessage.isVisible(), "Повідомлення 'Товар додано до кошика!' не з'явилося");
  }

  @AfterEach
  void closePage() {
    page.close();
  }

  @AfterAll
  static void closeBrowser() {
    playwright.close();
  }
}
