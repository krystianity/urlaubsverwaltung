package org.synyx.urlaubsverwaltung.ui.pages;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;

import static com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED;

public class SettingsPage {

    private static final String WORKING_TIME_TAB_SELECTOR = "[data-test-id=settings-tab-working-time]";
    private static final String OVERTIME_ENABLED_SELECTOR = "[data-test-id=setting-overtime-enabled]";
    private static final String OVERTIME_DISABLED_SELECTOR = "[data-test-id=setting-overtime-disabled]";
    private static final String SAVE_BUTTON_SELECTOR = "[data-test-id=settings-save-button]";
    private static final String HALF_DAY_DISABLE_SELECTOR = "[data-test-id=vacation-half-day-disable]";

    private final Page page;

    public SettingsPage(Page page) {
        this.page = page;
    }

    public void clickWorkingTimeTab() {
        page.waitForResponse(Response::ok, () -> page.locator(WORKING_TIME_TAB_SELECTOR).click());
        page.waitForLoadState(DOMCONTENTLOADED);
    }

    public void enableOvertime() {
        page.locator(OVERTIME_ENABLED_SELECTOR).click();
    }
    public void disableOvertime() {
        page.locator(OVERTIME_DISABLED_SELECTOR).click();
    }

    public void saveSettings() {
        page.waitForResponse(Response::ok, () -> page.locator(SAVE_BUTTON_SELECTOR).first().click());
        page.waitForLoadState(DOMCONTENTLOADED);
    }

    public void clickDisableHalfDayAbsence() {
        page.locator(HALF_DAY_DISABLE_SELECTOR).click();
    }
}
