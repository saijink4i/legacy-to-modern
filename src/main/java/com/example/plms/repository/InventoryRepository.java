package com.example.plms.repository;

import com.example.plms.domain.Inventory;
import com.example.plms.domain.Part;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByPart(Part part);
}
