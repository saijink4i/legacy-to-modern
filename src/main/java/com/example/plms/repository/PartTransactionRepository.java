package com.example.plms.repository;

import com.example.plms.domain.PartTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartTransactionRepository extends JpaRepository<PartTransaction, Long> {
    List<PartTransaction> findByPartIdOrderByTransactionDateDesc(Long partId);
}
