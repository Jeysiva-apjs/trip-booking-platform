package com.jeysiva.payment;

/** No payment exists with the requested id. Maps to HTTP 404. */
public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(String message) {
        super(message);
    }
}
