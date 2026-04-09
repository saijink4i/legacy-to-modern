package com.example.plms.service;

import com.example.plms.domain.PurchaseOrder;
import com.example.plms.domain.Supplier;
import com.example.plms.repository.PurchaseOrderRepository;
import com.example.plms.repository.SupplierRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DataMigrationRunner implements CommandLineRunner {

    private final PurchaseOrderRepository orderRepository;
    private final SupplierRepository supplierRepository;

    public DataMigrationRunner(PurchaseOrderRepository orderRepository, SupplierRepository supplierRepository) {
        this.orderRepository = orderRepository;
        this.supplierRepository = supplierRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 1. Fetch null supplier orders
        List<PurchaseOrder> orphanedOrders = orderRepository.findAll().stream()
                .filter(order -> order.getSupplier() == null)
                .toList();
        
        if (orphanedOrders.isEmpty()) {
            return;
        }

        // 2. Resolve AA01 Supplier
        Supplier aa01 = supplierRepository.findAll().stream()
                .filter(s -> "AA01".equals(s.getSupplierCode()))
                .findFirst()
                .orElse(null);

        // If AA01 does not exist, we just create a dummy one dynamically to avoid crashing the migration
        if (aa01 == null) {
            aa01 = new Supplier();
            aa01.setSupplierCode("AA01");
            aa01.setName("자동복구_기본거래처(AA01)");
            aa01 = supplierRepository.save(aa01);
        }

        // 3. Migrate and Save
        for (PurchaseOrder order : orphanedOrders) {
            order.setSupplier(aa01);
            orderRepository.save(order);
        }
        
        System.out.println("Data Migration SUCCESS: " + orphanedOrders.size() + " orders updated to AA01.");
    }
}
