package org.synyx.urlaubsverwaltung.calendarintegration;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;


/**
 * Settings to sync absences with a Google calendar.
 */
@Deprecated(since = "4.0.0", forRemoval = true)
@Embeddable
public class GoogleCalendarSettings {

    @Column(name = "calendar_google_client_id")
    private String clientId = null;

    @Column(name = "calendar_google_client_secret")
    private String clientSecret = null;

    @Column(name = "calendar_google_calendar_id")
    private String calendarId = null;

    @Column(name = "calendar_google_refresh_token")
    private String refreshToken = null;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getCalendarId() {
        return calendarId;
    }

    public void setCalendarId(String calendarId) {
        this.calendarId = calendarId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GoogleCalendarSettings that = (GoogleCalendarSettings) o;
        return Objects.equals(getClientId(), that.getClientId()) &&
            Objects.equals(getClientSecret(), that.getClientSecret()) &&
            Objects.equals(getCalendarId(), that.getCalendarId()) &&
            Objects.equals(getRefreshToken(), that.getRefreshToken());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClientId(), getClientSecret(), getCalendarId(), getRefreshToken());
    }
}
