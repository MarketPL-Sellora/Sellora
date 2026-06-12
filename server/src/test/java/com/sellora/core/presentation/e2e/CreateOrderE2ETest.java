package com.sellora.core.presentation.e2e;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.SelectOption;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.regex.Pattern;

public class CreateOrderE2ETest{
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
  void testLoginFlow() {
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
    Locator cartButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Кошик"));
    cartButton.waitFor();
    cartButton.click();

    // Додано: Очікування відмальовування DOM-вузлів кошика перед пошуком елементів всередині нього
    page.locator("text='Кошик'").first().waitFor();

    Locator productContainer = page.locator("div.group")
      .filter(new Locator.FilterOptions().setHasText("MacBook Pro M3 14\""))
      .first();

    productContainer.locator("label").click();

    assertTrue(productContainer.locator("input[type='checkbox']").isChecked());

    // Розкоментований блок для завершення сценарію
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("ОФОРМИТИ ЗАМОВЛЕННЯ")).click();

    page.getByPlaceholder("Наприклад: Олексій")
      .fill("Test Product");

    page.getByPlaceholder("Наприклад: Коваленко")
      .fill("Test Product");

    page.getByPlaceholder("+38 (0__) ___ __ __")
      .fill("Test Product");

    page.locator("button:has-text('Відділення')").first().click();

    Locator cityInput = page.getByPlaceholder("Оберіть місто");
    cityInput.click();
    cityInput.fill("Київ");

    Locator cityOption = page.locator("text='Київ'").last();

    cityOption.waitFor(new Locator.WaitForOptions().setTimeout(5000));

    cityOption.click();

    Locator delivery = page.getByPlaceholder("Відділення / Поштомат");
    delivery.click();
    delivery.fill("Відділення №1 ");

    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("ОФОРМИТИ ЗАМОВЛЕННЯ")).click();

    Pattern orderUrlPattern = Pattern.compile(".*/order/\\d+");

    page.waitForURL(orderUrlPattern);

    assertTrue(orderUrlPattern.matcher(page.url()).matches(),
      "Не вдалося перейти на сторінку створеного замовлення. Поточний URL: " + page.url());
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
