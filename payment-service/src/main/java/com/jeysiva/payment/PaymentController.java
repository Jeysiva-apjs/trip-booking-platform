package com.jeysiva.payment;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

// Read-only API to verify recorded payments. Charging is driven by the saga (Kafka), not by REST.
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse get(@PathVariable String paymentId) {
        return PaymentResponse.from(paymentService.findById(paymentId));
    }

    @GetMapping
    public List<PaymentResponse> byTripReference(@RequestParam String tripReference) {
        return paymentService.findByTripReference(tripReference).stream()
                .map(PaymentResponse::from)
                .toList();
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<String> handleNotFound(PaymentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    public record PaymentResponse(String paymentId, String tripReference, BigDecimal amount, Instant createdAt) {
        static PaymentResponse from(Payment payment) {
            return new PaymentResponse(
                    payment.getId(), payment.getTripReference(), payment.getAmount(), payment.getCreatedAt());
        }
    }
}
