package com.example.plms.repository;

import com.example.plms.domain.ReceiptLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReceiptLogRepository extends JpaRepository<ReceiptLog, Long> {
}
