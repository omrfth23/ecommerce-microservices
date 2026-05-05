package com.ecommerce.consumer;

import com.ecommerce.dto.PaymentCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationConsumer {

    @KafkaListener(
            topics = "payment-completed",
            groupId = "notification-service-group"
    )
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("=== NOTIFICATION ===");
        log.info("Payment {} completed for order {} — customer: {}",
                event.getPaymentId(), event.getOrderId(), event.getCustomerId());
    }
}
