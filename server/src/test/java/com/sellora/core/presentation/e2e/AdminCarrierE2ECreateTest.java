package com.sellora.core.presentation.e2e;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.SelectOption;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AdminCarrierE2ECreateTest{
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
  void OpenFavoritesTest() {
    page.navigate("http://localhost");

    page.locator("button:has-text('Увійти')").first().click();

    page.locator("input[type='email']").fill("admin@sellora.ua");

    page.locator("input[type='password']").first().fill("12345678");

    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Увійти")).nth(1).click();


    page.locator("text='Кабінет'").first().click(new Locator.ClickOptions().setForce(true));

    page.waitForURL("**/cabinet");

    page.waitForTimeout(1000);

    page.locator("text='Поштові служби'").first().click(new Locator.ClickOptions().setForce(true));
    page.locator("text='Створити'").first().click(new Locator.ClickOptions().setForce(true));
    page.waitForTimeout(2000);
    String timestamp = String.valueOf(System.currentTimeMillis());
    page.getByPlaceholder("Наприклад: Нова Пошта")
      .fill("TESTTEST");

    page.waitForTimeout(1000);

    page.getByPlaceholder("Наприклад: nova_poshta")
      .fill("test_test_test");
    page.waitForTimeout(1000);

    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Зберегти")).first().click();

    page.waitForURL("**/cabinet?tab=shipping-carriers");
    assertTrue(page.url().contains("cabinet?tab=shipping-carriers"), "Не вдалося зберегти товар");
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
