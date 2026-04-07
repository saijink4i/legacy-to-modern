package com.example.plms.service;

import com.example.plms.domain.Part;
import com.example.plms.domain.PartTransaction;
import com.example.plms.domain.Status;
import com.example.plms.repository.PartRepository;
import com.example.plms.repository.PartTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PartLifecycleService {

    private final PartRepository partRepository;
    private final PartTransactionRepository transactionRepository;

    public PartLifecycleService(PartRepository partRepository, PartTransactionRepository transactionRepository) {
        this.partRepository = partRepository;
        this.transactionRepository = transactionRepository;
    }

    // 부품 신규 등록
    public Part registerPart(Part part) {
        if (part.getPrice() <= 0) {
            throw new IllegalArgumentException("부품 가격은 0보다 커야 합니다.");
        }
        if (part.getOrderUnit() <= 0) {
            throw new IllegalArgumentException("발주 단위 수량은 1 이상이어야 합니다.");
        }
        return partRepository.save(part);
    }
    
    // 부품 마스터 업데이트
    public Part updatePartMaster(String productCode, int price, int orderUnit, int expirationDays) {
        if (price <= 0) {
            throw new IllegalArgumentException("부품 가격은 0보다 커야 합니다.");
        }
        if (orderUnit <= 0) {
            throw new IllegalArgumentException("발주 단위 수량은 1 이상이어야 합니다.");
        }
        Part part = partRepository.findByProductCode(productCode)
                .orElseThrow(() -> new IllegalArgumentException("해당 부품 코드(" + productCode + ")를 찾을 수 없습니다."));
        
        part.setPrice(price);
        part.setOrderUnit(orderUnit);
        part.setExpirationDays(expirationDays);
        return partRepository.save(part);
    }
    
    // 부품 단건 혹은 전체 목록 조회
    @Transactional(readOnly = true)
    public List<Part> getAllParts() {
        return partRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Part> getPartByCode(String productCode) {
        return partRepository.findByProductCode(productCode);
    }

    // 1. 발주 (Order)
    public PartTransaction orderPart(String productCode, int quantity, String remarks) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("발주 수량은 1개 이상이어야 합니다.");
        }

        Part part = partRepository.findByProductCode(productCode)
                .orElseThrow(() -> new IllegalArgumentException("해당 부품 코드(" + productCode + ")를 찾을 수 없습니다."));

        int totalQuantity = quantity * part.getOrderUnit();

        PartTransaction transaction = new PartTransaction();
        transaction.setPart(part);
        transaction.setStatus(Status.ORDERED);
        transaction.setQuantity(totalQuantity);
        transaction.setRemarks(remarks + " (単位: " + part.getOrderUnit() + " x " + quantity + ")");
        
        part.setIncomingQuantity(part.getIncomingQuantity() + totalQuantity);
        partRepository.save(part);

        return transactionRepository.save(transaction);
    }

    // 2. 단건 입고 (Receive by code)
    public PartTransaction receivePartByCode(String productCode, int quantity, String remarks) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("입고 수량은 1개 이상이어야 합니다.");
        }

        Part part = partRepository.findByProductCode(productCode)
                .orElseThrow(() -> new IllegalArgumentException("해당 부품 코드(" + productCode + ")를 찾을 수 없습니다."));

        if (part.getIncomingQuantity() < quantity) {
            throw new IllegalStateException("입고 수량이 입하 예정 물량보다 많습니다.");
        }

        part.setIncomingQuantity(part.getIncomingQuantity() - quantity);
        part.setStockQuantity(part.getStockQuantity() + quantity);
        partRepository.save(part);

        PartTransaction transaction = new PartTransaction();
        transaction.setPart(part);
        transaction.setStatus(Status.RECEIVED);
        transaction.setQuantity(quantity);
        transaction.setRemarks(remarks);

        return transactionRepository.save(transaction);
    }

    // 2. 단건 입고 (Receive)
    public PartTransaction receivePart(Long transactionId, String remarks) {
        PartTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("트랜잭션을 찾을 수 없습니다."));

        if (transaction.getStatus() != Status.ORDERED) {
            throw new IllegalStateException("발주 상태(ORDERED)인 정보만 입고 처리할 수 있습니다.");
        }

        transaction.setStatus(Status.RECEIVED);
        String finalRemarks = Optional.ofNullable(transaction.getRemarks()).orElse("") 
                              + " | Received: " + Optional.ofNullable(remarks).orElse("");
        transaction.setRemarks(finalRemarks);

        Part part = transaction.getPart();
        part.setStockQuantity(part.getStockQuantity() + transaction.getQuantity());
        partRepository.save(part);

        return transactionRepository.save(transaction);
    }

    // 3. 폐기 (Dispose)
    public PartTransaction disposePart(String productCode, int quantity, String remarks) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("폐기 수량은 1개 이상이어야 합니다.");
        }

        Part part = partRepository.findByProductCode(productCode)
                .orElseThrow(() -> new IllegalArgumentException("해당 부품 코드(" + productCode + ")를 찾을 수 없습니다."));

        if (part.getStockQuantity() < quantity) {
            throw new IllegalStateException("부품의 재고가 폐기 수량보다 적어 처리할 수 없습니다.");
        }

        part.setStockQuantity(part.getStockQuantity() - quantity);
        partRepository.save(part);

        PartTransaction transaction = new PartTransaction();
        transaction.setPart(part);
        transaction.setStatus(Status.DISPOSED);
        transaction.setQuantity(quantity);
        transaction.setRemarks(remarks);

        return transactionRepository.save(transaction);
    }

    // 레거시 코드 개선: 다량 발주건에 대한 일괄 병렬/스트림 처리
    public List<PartTransaction> bulkReceive(List<Long> transactionIds, String batchRemarks) {
        return transactionIds.stream()
                .map(transactionRepository::findById)
                .flatMap(Optional::stream)
                .filter(t -> t.getStatus() == Status.ORDERED)
                .map(t -> {
                    t.setStatus(Status.RECEIVED);
                    t.setRemarks(Optional.ofNullable(t.getRemarks()).orElse("") + " | BATCH: " + batchRemarks);

                    Part part = t.getPart();
                    part.setStockQuantity(part.getStockQuantity() + t.getQuantity());
                    partRepository.save(part);

                    return transactionRepository.save(t);
                })
                .toList();
    }
}
