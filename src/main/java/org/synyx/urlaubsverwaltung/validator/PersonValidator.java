
package org.synyx.urlaubsverwaltung.validator;

import org.apache.log4j.Logger;

import org.springframework.util.StringUtils;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import org.synyx.urlaubsverwaltung.util.NumberUtil;
import org.synyx.urlaubsverwaltung.util.PropertiesUtil;
import org.synyx.urlaubsverwaltung.view.PersonForm;

import java.math.BigDecimal;

import java.util.Locale;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * This class validate if an person's form ('PersonForm') is filled correctly by the user, else it saves error messages
 * in errors object.
 *
 * @author  Aljona Murygina
 */
public class PersonValidator implements Validator {

    // logs general errors like "properties file not found"
    private static final Logger LOG = Logger.getLogger("errorLog");

    private static final String MANDATORY_FIELD = "error.mandatory.field";
    private static final String ERROR_ENTRY = "error.entry";
    private static final String ERROR_EMAIL = "error.email";
    private static final String ERROR_NUMBER = "error.number";
    private static final String ERROR_LENGTH = "error.length";

    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";
    private static final String VACATION_DAYS_ENT = "vacationDaysEnt";
    private static final String ANNUAL_VACATION_ENT = "annualVacationDaysEnt";
    private static final String REMAINING_VACATION_DAYS_ENT = "remainingVacationDaysEnt";
    private static final String VACATION_DAYS_ACC = "vacationDaysAcc";
    private static final String REMAINING_VACATION_DAYS_ACC = "remainingVacationDaysAcc";
    private static final String YEAR = "year";
    private static final String EMAIL = "email";

    // a regex for email addresses that are valid, but may be "strange looking" (e.g. tomr$2@example.com)
    // original from: http://www.markussipila.info/pub/emailvalidator.php?action=validate
    // modified by adding following characters: äöüß
    private static final String EMAIL_PATTERN =
        "^[a-zäöüß0-9,!#\\$%&'\\*\\+/=\\?\\^_`\\{\\|}~-]+(\\.[a-zäöüß0-9,!#\\$%&'\\*\\+/=\\?\\^_`\\{\\|}~-]+)*@[a-zäöüß0-9-]+(\\.[a-zäöüß0-9-]+)*\\.([a-z]{2,})$";

    private static final String NAME_PATTERN = "\\p{L}+"; // any kind of letter from any language.

    private static final String MAX_DAYS = "annual.vacation.max";

    private static final String CUSTOM_PROPERTIES_FILE = "custom.properties";

    private Pattern pattern;
    private Matcher matcher;

    private PropertiesValidator propValidator;
    private Properties customProperties;

    public PersonValidator(PropertiesValidator propValidator) {

        this.propValidator = propValidator;

        try {
            this.customProperties = PropertiesUtil.load(CUSTOM_PROPERTIES_FILE);
        } catch (Exception ex) {
            LOG.error("No properties file found.");
            LOG.error(ex.getMessage(), ex);
        }
    }

    @Override
    public boolean supports(Class<?> clazz) {

        return PersonForm.class.equals(clazz);
    }


    @Override
    public void validate(Object target, Errors errors) {

        PersonForm form = (PersonForm) target;

        // field first name
        validateName(form.getFirstName(), FIRST_NAME, errors);

        // field last name
        validateName(form.getLastName(), LAST_NAME, errors);

        // field email address
        validateEmail(form.getEmail(), errors);

        // field year
        validateYear(form.getYear(), errors);
    }


    private boolean matchPattern(String nameOfPattern, String matchSequence) {

        pattern = Pattern.compile(nameOfPattern);
        matcher = pattern.matcher(matchSequence);

        return matcher.matches();
    }


    /**
     * This method checks if the field firstName or the field lastName is filled and if it is filled, it validates the
     * entry with a regex.
     *
     * @param  name  (may be the field firstName or lastName)
     * @param  field
     * @param  errors
     */
    protected void validateName(String name, String field, Errors errors) {

        // is the name field null/empty?
        if (!StringUtils.hasText(name)) {
            errors.rejectValue(field, MANDATORY_FIELD);
        } else {
            // is String length alright?
            if (!validateStringLength(name)) {
                errors.rejectValue(field, ERROR_LENGTH);
            }

            // contains the name field digits?
            if (!matchPattern(NAME_PATTERN, name)) {
                errors.rejectValue(field, ERROR_ENTRY);
            }
        }
    }


    /**
     * This method checks if the field email is filled and if it is filled, it validates the entry with a regex.
     *
     * @param  email
     * @param  errors
     */
    protected void validateEmail(String email, Errors errors) {

        // is email field null or empty
        if (!StringUtils.hasText(email)) {
            errors.rejectValue(EMAIL, MANDATORY_FIELD);
        } else {
            // String length alright?
            if (!validateStringLength(email)) {
                errors.rejectValue(EMAIL, ERROR_LENGTH);
            }

            // validation with regex
            email = email.trim().toLowerCase();

            if (!matchPattern(EMAIL_PATTERN, email)) {
                errors.rejectValue(EMAIL, ERROR_EMAIL);
            }
        }
    }


    /**
     * This method checks if the field year is filled and if it is filled, it checks if the year entry makes sense (at
     * the moment: from 2010 - 2030 alright)
     *
     * @param  yearForm
     * @param  errors
     */
    protected void validateYear(String yearForm, Errors errors) {

        // is year field filled?
        if (!StringUtils.hasText(yearForm)) {
            errors.rejectValue(YEAR, MANDATORY_FIELD);
        } else {
            try {
                int year = Integer.parseInt(yearForm);

                if (year < 2010 || year > 2030) {
                    errors.rejectValue(YEAR, ERROR_ENTRY);
                }
            } catch (NumberFormatException ex) {
                errors.rejectValue(YEAR, ERROR_ENTRY);
            }
        }
    }


    /**
     * This method validates if mandatory field annual vacation days is filled and if the entry is valid.
     *
     * @param  form
     * @param  errors
     * @param  locale
     */
    public void validateAnnualVacation(PersonForm form, Errors errors, Locale locale) {

        // only achieved if invalid property values are precluded by method validateProperties
        String propValue = customProperties.getProperty(MAX_DAYS);
        double max = Double.parseDouble(propValue);

        if (StringUtils.hasText(form.getAnnualVacationDays())) {
            try {
                validateNumberOfDays(NumberUtil.parseNumber(form.getAnnualVacationDays(), locale),
                    ANNUAL_VACATION_ENT, max, errors);
            } catch (NumberFormatException ex) {
                errors.rejectValue(ANNUAL_VACATION_ENT, ERROR_ENTRY);
            }
        } else {
            errors.rejectValue(ANNUAL_VACATION_ENT, MANDATORY_FIELD);
        }
    }


    /**
     * This method checks if the value of property with key 'annual.vacation.max' is valid. If it's not, the tool
     * manager is notified and editing the person is not possible.
     *
     * @param  form
     * @param  errors
     */
    public void validateProperties(PersonForm form, Errors errors) {

        propValidator.validateAnnualVacationProperty(customProperties, errors);
    }


    /**
     * This method gets the property value for maximal number of days and notifies Tool-Manager if necessary (false
     * property value) or validate the number of entitlement's remaining vacation days with method validateNumberOfDays.
     *
     * @param  form
     * @param  errors
     * @param  locale
     */
    public void validateRemainingVacationDays(PersonForm form, Errors errors, Locale locale) {

        // only achieved if invalid property values are precluded by method validateProperties
        String propValue = customProperties.getProperty(MAX_DAYS);
        double max = Double.parseDouble(propValue);

        if (StringUtils.hasText(form.getRemainingVacationDays())) {
            try {
                // field entitlement's remaining vacation days
                validateNumberOfDays(NumberUtil.parseNumber(form.getRemainingVacationDays(), locale),
                    REMAINING_VACATION_DAYS_ENT, max, errors);
            } catch (NumberFormatException ex) {
                errors.rejectValue(REMAINING_VACATION_DAYS_ENT, ERROR_ENTRY);
            }
        } else {
            errors.rejectValue(REMAINING_VACATION_DAYS_ENT, MANDATORY_FIELD);
        }
    }


    /**
     * This method validates if the holiday entitlement's fields remaining vacation days and vacation days are filled
     * and if they are filled, it checks if the number of days is realistic
     *
     * @param  days
     * @param  field
     * @param  maximumDays
     * @param  errors
     */
    protected void validateNumberOfDays(BigDecimal days, String field, double maximumDays, Errors errors) {

        // is field filled?
        if (days == null) {
            if (errors.getFieldErrors(field).isEmpty()) {
                errors.rejectValue(field, MANDATORY_FIELD);
            }
        } else {
            // is number of days < 0 ?
            if (days.compareTo(BigDecimal.ZERO) == -1) {
                errors.rejectValue(field, ERROR_ENTRY);
            }

            // is number of days unrealistic?
            if (days.compareTo(BigDecimal.valueOf(maximumDays)) == 1) {
                errors.rejectValue(field, ERROR_ENTRY);
            }
        }
    }


    /**
     * Checks if a String has a valid length.
     *
     * @param  string
     *
     * @return
     */
    protected boolean validateStringLength(String string) {

        if (string.length() > 50) {
            return false;
        } else {
            return true;
        }
    }
}
