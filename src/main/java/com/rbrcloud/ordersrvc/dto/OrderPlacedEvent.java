package com.rbrcloud.ordersrvc.dto;

import com.rbrcloud.ordersrvc.enums.OrderSide;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OrderPlacedEvent {

    private Long orderId;

    private Long userId;

    private String ticker;

    private Integer quantity;

    private BigDecimal price;

    private OrderSide orderSide;

    private LocalDateTime placedAt;
}
