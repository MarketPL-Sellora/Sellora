package com.sellora.core.presentation.e2e;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.SelectOption;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MerchantRequisitesE2ETest{
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

    page.locator("input[type='email']").fill("toha@gmail.com");

    page.locator("input[type='password']").first().fill("123123123");

    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Увійти")).nth(1).click();


    page.locator("text='Кабінет'").first().click(new Locator.ClickOptions().setForce(true));

    page.waitForURL("**/cabinet");

    page.waitForTimeout(1000);

    page.locator("text='Платіжні реквізити'").first().click(new Locator.ClickOptions().setForce(true));
    page.locator("text='Додати реквізит'").first().click(new Locator.ClickOptions().setForce(true));
    page.waitForTimeout(2000);
    String timestamp = String.valueOf(System.currentTimeMillis());
    page.getByPlaceholder("12345678")
      .fill("1"+timestamp.substring(timestamp.length() - 5));

    page.waitForTimeout(1000);

    page.getByPlaceholder("UA000000000000000000000000000")
      .fill("UA1112223334445556"+timestamp.substring(timestamp.length() - 5));
    page.waitForTimeout(1000);
    page.getByPlaceholder("АТ КБ «ПриватБанк»")
      .fill("Bank_" + timestamp.substring(timestamp.length() - 5));

    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Зберегти")).first().click();

    page.waitForURL("**/cabinet?tab=requisites");
    assertTrue(page.url().contains("cabinet?tab=requisites"), "Не вдалося зберегти товар");
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
