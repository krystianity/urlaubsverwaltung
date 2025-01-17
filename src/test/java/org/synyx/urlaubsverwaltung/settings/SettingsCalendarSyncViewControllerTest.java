package org.synyx.urlaubsverwaltung.settings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.validation.Errors;
import org.synyx.urlaubsverwaltung.absence.Absence;
import org.synyx.urlaubsverwaltung.calendarintegration.CalendarSettings;
import org.synyx.urlaubsverwaltung.calendarintegration.providers.CalendarProvider;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class SettingsCalendarSyncViewControllerTest {

    private SettingsCalendarSyncViewController sut;

    private static final String OATUH_REDIRECT_REL = "/google-api-handshake";
    private static final String ERRORS_ATTRIBUTE = "errors";
    private static final String OAUTH_ERROR_ATTRIBUTE = "oautherrors";
    private static final String OAUTH_ERROR_VALUE = "some-error";

    private static final CalendarProvider SOME_CALENDAR_PROVIDER = new SomeCalendarProvider();
    private static final CalendarProvider ANOTHER_CALENDAR_PROVIDER = new AnotherCalendarProvider();
    private static final List<CalendarProvider> CALENDAR_PROVIDER_LIST = List.of(SOME_CALENDAR_PROVIDER, ANOTHER_CALENDAR_PROVIDER);

    private static final String SOME_GOOGLE_REFRESH_TOKEN = "0815-4711-242";

    @Mock
    private SettingsService settingsService;
    @Mock
    private SettingsCalendarSyncValidator settingsValidator;

    private final Clock clock = Clock.systemUTC();

    @BeforeEach
    void setUp() {
        sut = new SettingsCalendarSyncViewController(settingsService, CALENDAR_PROVIDER_LIST, settingsValidator, clock);
    }

    @Test
    void getAuthorizedRedirectUrl() {
        final String actual = sut.getAuthorizedRedirectUrl("http://localhost:8080/web/settings/calendar-sync", OATUH_REDIRECT_REL);
        final String expected = "http://localhost:8080/web" + OATUH_REDIRECT_REL;
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void ensureSettingsDetailsFillsModelCorrectly() throws Exception {

        final CalendarSettings calendarSettings = new CalendarSettings();

        final Settings settings = new Settings();
        settings.setId(42L);
        settings.setCalendarSettings(calendarSettings);

        when(settingsService.getSettings()).thenReturn(settings);

        final SettingsCalendarSyncDto expectedSettingsCalendarSyncDto = new SettingsCalendarSyncDto();
        expectedSettingsCalendarSyncDto.setId(42L);
        expectedSettingsCalendarSyncDto.setCalendarSettings(calendarSettings);

        perform(get("/web/settings/calendar-sync"))
            .andExpect(model().attribute("settings", allOf(
                    hasProperty("id", is(42L)),
                    hasProperty("calendarSettings", sameInstance(calendarSettings))
            )))
            .andExpect(model().attribute("providers", contains("SomeCalendarProvider", "AnotherCalendarProvider")))
            .andExpect(model().attribute("availableTimezones", containsInAnyOrder(TimeZone.getAvailableIDs())))
            .andExpect(model().attribute("authorizedRedirectUrl",
                sut.getAuthorizedRedirectUrl("http://localhost/web/settings/calendar-sync", OATUH_REDIRECT_REL)));
    }

    @Test
    void ensureSettingsDetailsAddsOAuthErrorToModelIfErrorProvidedAndNoCurrentRefreshToken() throws Exception {

        when(settingsService.getSettings()).thenReturn(someSettingsWithoutGoogleCalendarRefreshToken());

        perform(get("/web/settings/calendar-sync")
            .param(OAUTH_ERROR_ATTRIBUTE, OAUTH_ERROR_VALUE))
            .andExpect(model().attribute(OAUTH_ERROR_ATTRIBUTE, OAUTH_ERROR_VALUE))
            .andExpect(model().attribute(ERRORS_ATTRIBUTE, OAUTH_ERROR_VALUE));
    }

    @Test
    void ensureSettingsDetailsDoesNotAddOAuthErrorToModelIfErrorProvidedAndCurrentRefreshToken() throws Exception {

        when(settingsService.getSettings()).thenReturn(someSettingsWithGoogleCalendarRefreshToken());

        perform(get("/web/settings/calendar-sync")
            .param(OAUTH_ERROR_ATTRIBUTE, OAUTH_ERROR_VALUE))
            .andExpect(model().attribute(OAUTH_ERROR_ATTRIBUTE, nullValue()))
            .andExpect(model().attribute(ERRORS_ATTRIBUTE, nullValue()));
    }

    @Test
    void ensureSettingsDetailsDoesNotAddOAuthErrorToModelIfNoErrorProvidedAndNoCurrentRefreshToken() throws Exception {

        when(settingsService.getSettings()).thenReturn(someSettingsWithoutGoogleCalendarRefreshToken());

        perform(get("/web/settings/calendar-sync"))
            .andExpect(model().attribute(OAUTH_ERROR_ATTRIBUTE, nullValue()))
            .andExpect(model().attribute(ERRORS_ATTRIBUTE, nullValue()));
    }

    @Test
    void ensureSettingsDetailsDoesNotAddOAuthErrorToModelIfNoErrorProvidedAndCurrentRefreshToken() throws Exception {

        when(settingsService.getSettings()).thenReturn(someSettingsWithGoogleCalendarRefreshToken());

        perform(get("/web/settings/calendar-sync"))
            .andExpect(model().attribute(OAUTH_ERROR_ATTRIBUTE, nullValue()))
            .andExpect(model().attribute(ERRORS_ATTRIBUTE, nullValue()));
    }

    @Test
    void ensureSettingsDetailsSetsDefaultExchangeTimeZoneIfNoneConfigured() throws Exception {

        final Settings settings = someSettingsWithNoExchangeTimezone();
        when(settingsService.getSettings()).thenReturn(settings);

        assertThat(settings.getCalendarSettings().getExchangeCalendarSettings().getTimeZoneId()).isNull();

        perform(get("/web/settings/calendar-sync"));

        assertThat(settings.getCalendarSettings().getExchangeCalendarSettings().getTimeZoneId())
            .isEqualTo(clock.getZone().getId());
    }

    @Test
    void ensureSettingsDetailsDoesNotAlterExchangeTimeZoneIfAlreadyConfigured() throws Exception {

        final String timeZoneId = "XYZ";
        final Settings settings = someSettingsWithExchangeTimeZone(timeZoneId);
        when(settingsService.getSettings()).thenReturn(someSettingsWithExchangeTimeZone(timeZoneId));

        assertThat(settings.getCalendarSettings().getExchangeCalendarSettings().getTimeZoneId()).isEqualTo(timeZoneId);

        perform(get("/web/settings/calendar-sync"));

        assertThat(settings.getCalendarSettings().getExchangeCalendarSettings().getTimeZoneId()).isEqualTo(timeZoneId);
    }

    @Test
    void ensureSettingsDetailsUsesCorrectView() throws Exception {
        when(settingsService.getSettings()).thenReturn(new Settings());
        perform(get("/web/settings/calendar-sync"))
            .andExpect(view().name("settings/calendar/settings_calendar_sync"));
    }

    @Test
    void ensureSettingsSavedShowsFormIfValidationFails() throws Exception {

        doAnswer(invocation -> {
            Errors errors = invocation.getArgument(1);
            errors.rejectValue("calendarSettings", "error");
            return null;
        }).when(settingsValidator).validate(any(), any());

        perform(
            post("/web/settings/calendar-sync")
                .param("calendarSettings.provider", "NoopCalendarSyncProvider")
                .param("calendarSettings.exchangeCalendarSettings.email", "")
                .param("calendarSettings.exchangeCalendarSettings.password", "")
                .param("calendarSettings.exchangeCalendarSettings.ewsUrl", "")
                .param("calendarSettings.exchangeCalendarSettings.calendar", "")
                .param("calendarSettings.exchangeCalendarSettings.timeZoneId", "Z")
                .param("calendarSettings.googleCalendarSettings.clientId", "")
                .param("calendarSettings.googleCalendarSettings.clientSecret", "")
                .param("calendarSettings.googleCalendarSettings.calendarId", "")
                .param("calendarSettings.googleCalendarSettings.authorizedRedirectUrl", "http://localhost:8080/web/google-api-handshake")
        )
            .andExpect(view().name("settings/calendar/settings_calendar_sync"));
    }

    @Test
    void ensureSettingsSavedSavesSettingsIfValidationSuccessfully() throws Exception {

        when(settingsService.getSettings()).thenReturn(new Settings());

        perform(
            post("/web/settings/calendar-sync")
                .param("absenceTypeSettings.items[0].id", "10")
                .param("specialLeaveSettings.specialLeaveSettingsItems[0].id", "11")
                .param("calendarSettings.clientId", "clientId")
        );

        verify(settingsService).save(any(Settings.class));
    }

    @Test
    void ensureSettingsSavedAddFlashAttributeAndRedirectsToSettings() throws Exception {

        when(settingsService.getSettings()).thenReturn(new Settings());

        perform(
            post("/web/settings/calendar-sync")
                .param("absenceTypeSettings.items[0].id", "10")
                .param("specialLeaveSettings.specialLeaveSettingsItems[0].id", "11")
                .param("calendarSettings.clientId", "clientId")
        )
            .andExpect(flash().attribute("success", true));

        perform(
            post("/web/settings/calendar-sync")
                .param("absenceTypeSettings.items[0].id", "10")
                .param("specialLeaveSettings.specialLeaveSettingsItems[0].id", "11")
                .param("calendarSettings.clientId", "clientId")
        )
            .andExpect(status().isFound())
            .andExpect(redirectedUrl("/web/settings/calendar-sync"));
    }

    private static Settings someSettingsWithNoExchangeTimezone() {
        return new Settings();
    }

    private static Settings someSettingsWithExchangeTimeZone(String timeZoneId) {
        final Settings settings = new Settings();
        settings.getCalendarSettings().getExchangeCalendarSettings().setTimeZoneId(timeZoneId);
        return settings;
    }

    private static Settings someSettingsWithoutGoogleCalendarRefreshToken() {
        return new Settings();
    }

    private static Settings someSettingsWithGoogleCalendarRefreshToken() {
        final Settings settings = new Settings();
        settings.getCalendarSettings().getGoogleCalendarSettings().setRefreshToken(SOME_GOOGLE_REFRESH_TOKEN);
        return settings;
    }

    private ResultActions perform(MockHttpServletRequestBuilder builder) throws Exception {
        return standaloneSetup(sut).build().perform(builder);
    }

    private static class SomeCalendarProvider implements CalendarProvider {

        @Override
        public boolean isRealProviderConfigured() {
            return true;
        }

        @Override
        public Optional<String> add(Absence absence, CalendarSettings calendarSettings) {
            throw new UnsupportedOperationException("This is just a mock to have some named CalendarProvider impl.");
        }

        @Override
        public void update(Absence absence, String eventId, CalendarSettings calendarSettings) {
            throw new UnsupportedOperationException("This is just a mock to have some named CalendarProvider impl.");
        }

        @Override
        public void delete(String eventId, CalendarSettings calendarSettings) {
            throw new UnsupportedOperationException("This is just a mock to have some named CalendarProvider impl.");
        }

        @Override
        public void checkCalendarSyncSettings(CalendarSettings calendarSettings) {
            throw new UnsupportedOperationException("This is just a mock to have some named CalendarProvider impl.");
        }
    }

    private static class AnotherCalendarProvider implements CalendarProvider {

        @Override
        public boolean isRealProviderConfigured() {
            return true;
        }

        @Override
        public Optional<String> add(Absence absence, CalendarSettings calendarSettings) {
            throw new UnsupportedOperationException("This is just a mock to have some named CalendarProvider impl.");
        }

        @Override
        public void update(Absence absence, String eventId, CalendarSettings calendarSettings) {
            throw new UnsupportedOperationException("This is just a mock to have some named CalendarProvider impl.");
        }

        @Override
        public void delete(String eventId, CalendarSettings calendarSettings) {
            throw new UnsupportedOperationException("This is just a mock to have some named CalendarProvider impl.");
        }

        @Override
        public void checkCalendarSyncSettings(CalendarSettings calendarSettings) {
            throw new UnsupportedOperationException("This is just a mock to have some named CalendarProvider impl.");
        }
    }
}
