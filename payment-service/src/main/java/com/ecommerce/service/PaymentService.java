package com.ecommerce.service;

import com.ecommerce.dto.OrderPlacedEvent;
import com.ecommerce.event.PaymentCompletedEvent;
import com.ecommerce.model.Payment;
import com.ecommerce.model.PaymentStatus;
import com.ecommerce.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, PaymentCompletedEvent> kafkaTemplate;

    private static final String PAYMENT_TOPIC = "payment-completed";

    @Transactional
    public void processPayment(OrderPlacedEvent event) {
        log.info("Processing payment for orderId: {}", event.getOrderId());

        // Ödeme kaydını oluştur
        Payment payment = Payment.builder()
                .orderId(event.getOrderId())
                .customerId(event.getCustomerId())
                .amount(event.getTotalAmount())
                .status(PaymentStatus.COMPLETED)
                .build();

        Payment saved = paymentRepository.save(payment);
        log.info("Payment saved with id: {}", saved.getId());

        PaymentCompletedEvent completedEvent = PaymentCompletedEvent.builder()
                .paymentId(saved.getId())
                .orderId(event.getOrderId())
                .customerId(event.getCustomerId())
                .amount(event.getTotalAmount())
                .status(saved.getStatus().name())
                .build();

        kafkaTemplate.send(PAYMENT_TOPIC, event.getOrderId(), completedEvent);
        log.info("PaymentCompletedEvent published for orderId: {}", event.getOrderId());
    }

    public List<Payment> getPaymentsByOrderId(String orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    public List<Payment> getPaymentsByCustomerId(String customerId) {
        return paymentRepository.findByCustomerId(customerId);
    }
}
