package com.example.plms.service;

import com.example.plms.domain.Part;
import com.example.plms.domain.PartTransaction;
import com.example.plms.domain.Status;
import com.example.plms.repository.PartRepository;
import com.example.plms.repository.PartTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class PartLifecycleServiceTest {

    @Autowired
    private PartLifecycleService partService;
    
    @Autowired
    private PartRepository partRepository;
    
    @Autowired
    private PartTransactionRepository transactionRepository;

    private Part testPart;

    @BeforeEach
    void setUp() {
        Part part = new Part();
        part.setProductCode("P-1001");
        part.setName("테스트 부품");
        part.setStockQuantity(5);
        testPart = partRepository.save(part);
    }

    @Test
    @DisplayName("발주 로직 검증: 정상 수량 발주 시 ORDERED 상태의 트랜잭션이 생성되어야 한다.")
    void orderPart_Success() {
        PartTransaction transaction = partService.orderPart("P-1001", 10, "긴급 발주");
        
        assertThat(transaction.getStatus()).isEqualTo(Status.ORDERED);
        assertThat(transaction.getQuantity()).isEqualTo(10);
        assertThat(transaction.getPart().getProductCode()).isEqualTo("P-1001");
        
        Part part = partRepository.findByProductCode("P-1001").get();
        assertThat(part.getStockQuantity()).isEqualTo(5); // 입고 전까지는 재고 수량 변동이 일어날지 않는다.
    }

    @Test
    @DisplayName("입고 로직 검증: 발주된 부품 입고 시 재고가 올바르게 증가해야 한다.")
    void receivePart_Success() {
        PartTransaction order = partService.orderPart("P-1001", 15, "정상 발주");
        
        PartTransaction received = partService.receivePart(order.getId(), "검수 완료");
        
        assertThat(received.getStatus()).isEqualTo(Status.RECEIVED);
        
        Part part = partRepository.findByProductCode("P-1001").get();
        assertThat(part.getStockQuantity()).isEqualTo(20); // 5 + 15
    }

    @Test
    @DisplayName("폐기 로직 검증: 재고가 충분할 때 폐기 시 재고가 감소하고 DISPOSED 상태 트랜잭션이 생성된다.")
    void disposePart_Success() {
        PartTransaction transaction = partService.disposePart("P-1001", 2, "불량 폐기");
        
        assertThat(transaction.getStatus()).isEqualTo(Status.DISPOSED);
        assertThat(transaction.getQuantity()).isEqualTo(2);
        
        Part part = partRepository.findByProductCode("P-1001").get();
        assertThat(part.getStockQuantity()).isEqualTo(3); // 5 - 2
    }

    @Test
    @DisplayName("폐기 로직 검증: 재고보다 많은 수량 폐기 시도 시 예외가 발생한다.")
    void disposePart_Fail_NotEnoughStock() {
        assertThrows(IllegalStateException.class, () -> {
            partService.disposePart("P-1001", 10, "초과 수량 폐기 시도");
        });
    }
}
