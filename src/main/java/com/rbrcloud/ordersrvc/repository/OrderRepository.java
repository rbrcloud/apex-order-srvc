package com.rbrcloud.ordersrvc.repository;

import com.rbrcloud.ordersrvc.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
}
