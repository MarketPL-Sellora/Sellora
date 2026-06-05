package com.sellora.core.presentation.e2e;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OpenFavoritesPageE2ETest{
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

    page.waitForURL("http://localhost/");


    Locator favoritesButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Улюблені"));

    favoritesButton.waitFor();
    favoritesButton.click();

    page.waitForURL("**/cabinet?tab=wishlist");

    page.waitForTimeout(2000);

    assertTrue(page.url().contains("/cabinet?tab=wishlist"), "Не вдалося перейти на сторінку Улюблених");
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
