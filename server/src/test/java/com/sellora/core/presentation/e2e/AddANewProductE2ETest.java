package com.sellora.core.presentation.e2e;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.SelectOption;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AddANewProductE2ETest{
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

    page.locator("text='Мої товари'").first().click(new Locator.ClickOptions().setForce(true));
    page.locator("text='Додати товар'").first().click(new Locator.ClickOptions().setForce(true));


    page.getByPlaceholder("Наприклад: iPhone 15 Pro Max 256GB Natural Titanium")
      .fill("Test Product " + System.currentTimeMillis());

    page.locator("select").selectOption("Електроніка");

    page.waitForTimeout(2000);

    page.locator("select.appearance-none").selectOption(new SelectOption().setLabel("Електроніка"));


    page.getByPlaceholder("0.00").nth(0).fill("1500");
    page.waitForTimeout(2000);


    page.locator("input[type='number']").nth(1).fill("100");

    page.getByPlaceholder("Детально опишіть товар: матеріал, розміри, особливості використання, комплектацію тощо. Хороший опис підвищує конверсію.")
      .fill("a;sdkf;lkasd;flka;sldkf;alskdf;jasldjfhkjahsgdfkhaskdjhfkajshdfkjahsdkjf");
    page.waitForTimeout(2000);

    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Зберегти товар")).first().click();

    page.waitForURL("**/cabinet?tab=my-products");
    assertTrue(page.url().contains("tab=my-products"), "Не вдалося зберегти товар");
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
