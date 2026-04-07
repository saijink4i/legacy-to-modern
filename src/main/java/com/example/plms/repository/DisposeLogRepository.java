package com.example.plms.repository;

import com.example.plms.domain.DisposeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DisposeLogRepository extends JpaRepository<DisposeLog, Long> {
}
