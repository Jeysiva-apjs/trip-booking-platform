package com.jeysiva.payment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    // Failure trigger for exercising the saga's compensation path: a trip priced at or above this is
    // declined. Run with --booking.trip-price=100000 to see the trip cancelled and the seat/room released.
    private static final BigDecimal FAIL_THRESHOLD = new BigDecimal("100000");

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    // Idempotent, keyed on tripReference: if this trip was already charged (a redelivered request event,
    // say), return the existing payment instead of charging twice.
    @Transactional
    public Outcome charge(String tripReference, BigDecimal amount) {
        List<Payment> existing = paymentRepository.findByTripReferenceOrderByCreatedAtDesc(tripReference);
        if (!existing.isEmpty()) {
            return new Outcome(existing.get(0).getId(), true, "Already charged (idempotent replay)");
        }
        if (amount.compareTo(FAIL_THRESHOLD) >= 0) {
            return new Outcome(null, false, "Payment declined (amount >= " + FAIL_THRESHOLD + ")");
        }
        Payment saved = paymentRepository.save(new Payment(
                UUID.randomUUID().toString(), tripReference, amount, Instant.now()));
        return new Outcome(saved.getId(), true, "Approved");
    }

    @Transactional(readOnly = true)
    public Payment findById(String paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment " + paymentId + " not found"));
    }

    @Transactional(readOnly = true)
    public List<Payment> findByTripReference(String tripReference) {
        return paymentRepository.findByTripReferenceOrderByCreatedAtDesc(tripReference);
    }

    /** Result of a charge: the payment id (null when declined), whether it was approved, and why. */
    public record Outcome(String paymentId, boolean approved, String reason) {
    }
}
