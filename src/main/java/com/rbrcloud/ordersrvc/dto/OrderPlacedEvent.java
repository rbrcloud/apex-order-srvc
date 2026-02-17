package com.rbrcloud.ordersrvc.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.rbrcloud.ordersrvc.enums.OrderSide;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPlacedEvent {

    private Long orderId;

    private Long userId;

    private String ticker;

    private Integer quantity;

    private BigDecimal price;

    private OrderSide orderSide;

    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    private LocalDateTime placedAt;
}
