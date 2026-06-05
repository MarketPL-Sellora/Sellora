package com.sellora.core.presentation.e2e;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LoginE2ETest {
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
    // 1. Заходимо на сайт
    page.navigate("http://localhost");

    // 2. Натискаємо кнопку "Увійти" в шапці (це перша кнопка на сторінці)
    // Використовуємо css-селектор для надійності: button з текстом "Увійти"
    page.locator("button:has-text('Увійти')").first().click();

    // 3. Заповнюємо поля
    // Знаходимо інпут за типом email
    page.locator("input[type='email']").fill("toha@gmail.com");

    // Знаходимо інпут за типом password (перший за порядком у формі)
    page.locator("input[type='password']").first().fill("123123123");

    // 4. Натискаємо кнопку "Увійти" всередині форми
    // Це саме та кнопка, яка відправляє форму (тип submit)
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Увійти")).nth(1).click();

    // 5. Чекаємо редирект
    page.waitForURL("http://localhost/");

    assertTrue(page.locator("text=Кабінет").isVisible(), "Кнопка 'Кабінет' не з'явилася після входу!");
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
