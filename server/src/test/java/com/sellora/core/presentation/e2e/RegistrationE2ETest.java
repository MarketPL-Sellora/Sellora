package com.sellora.core.presentation.e2e;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RegistrationE2ETest {
  static Playwright playwright;
  static Browser browser;
  BrowserContext context;
  Page page;

  @BeforeAll
  static void launchBrowser() {
    playwright = Playwright.create();
    browser = playwright.firefox().launch(new BrowserType.LaunchOptions().setHeadless(false));
  }

  @BeforeEach
  void createContext() {
    context = browser.newContext();
    page = context.newPage();
  }

  @Test
  void testSuccessfulRegistrationFlow() {
    page.navigate("http://localhost");

    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Увійти")).click();
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Реєстрація")).click();


    String uniqueEmail = "testuser_" + System.currentTimeMillis() + "@example.com";

    page.locator("input[type='email']").fill(uniqueEmail);

    page.locator("input[type='password']").nth(0).fill("12345678");


    page.locator("input[type='password']").nth(1).fill("12345678");

    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Зареєструватися").setExact(true)).click();

    page.waitForURL("http://localhost/");
  }

  @AfterEach
  void closeContext() {
    context.close();
  }

  @AfterAll
  static void closeBrowser() {
    playwright.close();
  }
}
