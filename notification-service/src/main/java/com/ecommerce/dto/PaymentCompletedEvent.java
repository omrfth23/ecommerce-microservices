package com.ecommerce.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentCompletedEvent {

    private String paymentId;
    private String orderId;
    private String customerId;
    private BigDecimal amount;
    private String status;
}
