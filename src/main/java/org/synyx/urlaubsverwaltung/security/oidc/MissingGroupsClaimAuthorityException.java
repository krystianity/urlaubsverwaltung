package org.synyx.urlaubsverwaltung.security.oidc;

import org.springframework.security.authentication.AccountStatusException;

/**
 * Thrown when a user doesn't have the required authority from the groups claim.
 */
public class MissingGroupsClaimAuthorityException extends AccountStatusException {
    /**
     * Constructs a <code>MissingAuthorityException</code> with the specified message.
     *
     * @param msg the detail message
     */
    MissingGroupsClaimAuthorityException(String msg) {
        super(msg);
    }
}
