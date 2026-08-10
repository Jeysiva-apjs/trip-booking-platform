package com.jeysiva.booking.user;

/**
 * Descriptive label on an account: USER books trips, ADMIN also creates inventory.
 *
 * <p>Nothing enforces this — the platform has no authentication, so every endpoint is open to every
 * caller. It records intent for seeded data and for whatever authorization gets built later.
 */
public enum Role {
    USER,
    ADMIN
}
