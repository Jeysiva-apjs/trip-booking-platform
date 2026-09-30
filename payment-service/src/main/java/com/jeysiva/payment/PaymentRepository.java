package com.jeysiva.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Persistence for recorded payment attempts. */
public interface PaymentRepository extends JpaRepository<Payment, String> {

    /** All attempts made for one booking reference, newest first — used for reconciliation and support queries. */
    List<Payment> findByTripReferenceOrderByCreatedAtDesc(String tripReference);
}
