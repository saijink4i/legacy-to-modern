package com.example.plms.repository;

import com.example.plms.domain.OrderStatus;
import com.example.plms.domain.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    Optional<PurchaseOrder> findByOrderNumber(String orderNumber);
    List<PurchaseOrder> findByStatus(OrderStatus status);
}
