package com.rbrcloud.ordersrvc.service;

import com.rbrcloud.ordersrvc.dto.OrderPlacedEvent;
import com.rbrcloud.ordersrvc.entity.Order;
import com.rbrcloud.ordersrvc.enums.OrderStatus;
import com.rbrcloud.ordersrvc.repository.OrderRepository;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    private static final String ORDER_PLACED_TOPIC = "order.placed.event";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void processOrder(@Nonnull OrderPlacedEvent event) {
        // Map DTO to Entity to set initial status
        Order order = Order.builder()
                .userId(event.getUserId())
                .ticker(event.getTicker())
                .orderSide(event.getOrderSide())
                .quantity(event.getQuantity())
                .status(OrderStatus.SUBMITTED)
                .createdAt(LocalDateTime.now())
                .price(event.getPrice())
                .build();
        Order savedOrder = orderRepository.save(order);

        // Update event with order id and created time
        event.setOrderId(savedOrder.getId());
        event.setPlacedAt(savedOrder.getCreatedAt());

        // Publish to kafka
        kafkaTemplate.send(ORDER_PLACED_TOPIC, event.getTicker(), event);
        log.info("Published OrderPlacedEvent to kafka: {}", event.getOrderId());
    }
}
