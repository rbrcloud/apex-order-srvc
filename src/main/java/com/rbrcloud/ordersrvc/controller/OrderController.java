package com.rbrcloud.ordersrvc.controller;

import com.rbrcloud.ordersrvc.dto.OrderPlacedEvent;
import com.rbrcloud.ordersrvc.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/order")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<Void> placeOrder(@RequestBody OrderPlacedEvent orderPlacedEvent) {
        orderService.processOrder(orderPlacedEvent);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}