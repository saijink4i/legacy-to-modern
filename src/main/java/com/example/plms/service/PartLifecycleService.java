package com.example.plms.service;

import com.example.plms.domain.*;
import com.example.plms.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PartLifecycleService {

    private final PartRepository partRepository;
    private final SupplierRepository supplierRepository;
    private final InventoryRepository inventoryRepository;
    private final PurchaseOrderRepository orderRepository;
    private final ReceiptLogRepository receiptRepository;
    private final DisposeLogRepository disposeRepository;

    public PartLifecycleService(PartRepository partRepository,
                                SupplierRepository supplierRepository,
                                InventoryRepository inventoryRepository,
                                PurchaseOrderRepository orderRepository,
                                ReceiptLogRepository receiptRepository,
                                DisposeLogRepository disposeRepository) {
        this.partRepository = partRepository;
        this.supplierRepository = supplierRepository;
        this.inventoryRepository = inventoryRepository;
        this.orderRepository = orderRepository;
        this.receiptRepository = receiptRepository;
        this.disposeRepository = disposeRepository;
    }

    // 부품 신규 등록
    public Part registerPart(Part part, int initialStock) {
        if (part.getPrice() < 0) {
            throw new IllegalArgumentException("부품 가격은 0 이상이어야 합니다.");
        }
        if (part.getOrderUnit() <= 0) {
            throw new IllegalArgumentException("발주 단위 수량은 1 이상이어야 합니다.");
        }
        if (part.getExpirationDate() == null || part.getExpirationDate().trim().isEmpty()) {
            part.setExpirationDate("9999999");
        }
        Part savedPart = partRepository.save(part);
        
        // 부품 마스터 생성 시 자동으로 재고 마스터도 생성
        Inventory inventory = new Inventory();
        inventory.setPart(savedPart);
        inventory.setCurrentStock(initialStock > 0 ? initialStock : 0);
        inventoryRepository.save(inventory);
        
        return savedPart;
    }
    
    // 거래처 신규 등록 (선택적)
    public Supplier registerSupplier(Supplier supplier) {
        return supplierRepository.save(supplier);
    }
    
    // 거래처 전체 조회
    @Transactional(readOnly = true)
    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }
    
    // 거래처 업데이트
    public Supplier updateSupplier(Long id, String supplierCode, String name, String contactInfo) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 거래처를 찾을 수 없습니다."));
        supplier.setSupplierCode(supplierCode);
        supplier.setName(name);
        supplier.setContactInfo(contactInfo);
        return supplierRepository.save(supplier);
    }
    
    // 부품 마스터 업데이트
    public Part updatePartMaster(String productCode, int price, int orderUnit, String expirationDate, int leadTimeDays) {
        if (price < 0) {
            throw new IllegalArgumentException("부품 가격은 0 이상이어야 합니다.");
        }
        if (orderUnit <= 0) {
            throw new IllegalArgumentException("발주 단위 수량은 1 이상이어야 합니다.");
        }
        Part part = partRepository.findByProductCode(productCode)
                .orElseThrow(() -> new IllegalArgumentException("해당 부품 코드(" + productCode + ")를 찾을 수 없습니다."));
        
        part.setPrice(price);
        part.setOrderUnit(orderUnit);
        
        if (expirationDate == null || expirationDate.trim().isEmpty()) {
            part.setExpirationDate("9999999");
        } else {
            part.setExpirationDate(expirationDate);
        }
        
        part.setLeadTimeDays(leadTimeDays);
        return partRepository.save(part);
    }
    
    // 부품 및 재고 전체 조회
    @Transactional(readOnly = true)
    public List<Part> getAllParts() {
        return partRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    public List<Inventory> getAllInventories() {
        return inventoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Part> getPartByCode(String productCode) {
        return partRepository.findByProductCode(productCode);
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrder> getPendingOrders() {
        return orderRepository.findByStatus(OrderStatus.PENDING);
    }
    
    @Transactional(readOnly = true)
    public List<PurchaseOrder> getAllPurchaseOrders() {
        List<PurchaseOrder> orders = orderRepository.findAll();
        orders.sort(Comparator.comparing(PurchaseOrder::getOrderDate).reversed());
        return orders;
    }
    
    @Transactional(readOnly = true)
    public List<ReceiptLog> getAllReceiptLogs() {
        return receiptRepository.findAllByOrderByReceiveDateDesc();
    }

    // 1. 발주 (Order)
    public PurchaseOrder orderPart(String productCode, int quantity, String remarks, LocalDate expectedArrivalDate, Long supplierId) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("발주 수량은 1개 이상이어야 합니다.");
        }

        Part part = partRepository.findByProductCode(productCode)
                .orElseThrow(() -> new IllegalArgumentException("해당 부품 코드(" + productCode + ")를 찾을 수 없습니다."));

        Inventory inventory = inventoryRepository.findByPart(part)
                .orElseThrow(() -> new IllegalStateException("재고 정보가 초기화되지 않았습니다."));

        int totalQuantity = quantity * part.getOrderUnit();

        PurchaseOrder order = new PurchaseOrder();
        // 간단한 고유 주문번호 생성: "ORD-날짜-밀리초"
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uniqueSuffix = String.valueOf(System.currentTimeMillis()).substring(8);
        order.setOrderNumber("ORD-" + datePrefix + "-" + uniqueSuffix);
        
        order.setPart(part);
        order.setStatus(OrderStatus.PENDING);
        order.setQuantity(totalQuantity);
        
        LocalDate minDate = LocalDate.now().plusDays(part.getLeadTimeDays());
        if (expectedArrivalDate != null && expectedArrivalDate.isBefore(minDate)) {
            throw new IllegalArgumentException("입하 예정일은 기본 리드타임(" + minDate + ") 이전으로 설정할 수 없습니다.");
        }
        order.setExpectedArrivalDate(expectedArrivalDate != null ? expectedArrivalDate : minDate);
        order.setRemarks(remarks);
        
        // 재고 마스터 업데이트: 입하 예정 수량 증가
        inventory.setPendingIncoming(inventory.getPendingIncoming() + totalQuantity);
        inventoryRepository.save(inventory);

        return orderRepository.save(order);
    }
    
    // 1-2. 발주 수정 (Edit Order) 제한: PENDING 상태만
    public PurchaseOrder updateOrder(String orderNumber, int updatedQuantity, String remarks, LocalDate expectedArrivalDate, Long supplierId) {
        if (updatedQuantity <= 0) {
            throw new IllegalArgumentException("발주 수량은 1개 이상이어야 합니다.");
        }

        PurchaseOrder order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("해당 주문번호 (" + orderNumber + ")를 찾을 수 없습니다."));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("입하 대기 중(PENDING)인 발주 건만 수정할 수 있습니다.");
        }

        Part part = order.getPart();
        Inventory inventory = inventoryRepository.findByPart(part)
                .orElseThrow(() -> new IllegalStateException("재고 정보가 없습니다."));

        int originalTotalQuantity = order.getQuantity();
        int newTotalQuantity = updatedQuantity * part.getOrderUnit();
        int difference = newTotalQuantity - originalTotalQuantity;
        
        // Update Inventory pending counts
        inventory.setPendingIncoming(inventory.getPendingIncoming() + difference);
        if(inventory.getPendingIncoming() < 0) {
           inventory.setPendingIncoming(0); // safeguard
        }
        inventoryRepository.save(inventory);

        // Update Order
        order.setQuantity(newTotalQuantity);
        order.setRemarks(remarks);
        
        if (supplierId != null) {
            supplierRepository.findById(supplierId).ifPresent(order::setSupplier);
        } else {
            order.setSupplier(null);
        }
        
        LocalDate minDate = LocalDate.now().plusDays(part.getLeadTimeDays());
        if (expectedArrivalDate != null && expectedArrivalDate.isBefore(minDate)) {
            throw new IllegalArgumentException("입하 예정일은 기본 리드타임(" + minDate + ") 이전으로 설정할 수 없습니다.");
        }
        order.setExpectedArrivalDate(expectedArrivalDate != null ? expectedArrivalDate : minDate);

        return orderRepository.save(order);
    }

    // 2. 바코드 기반 입하 처리 (Receive - Partial Supported)
    public ReceiptLog receiveOrder(String orderNumber, int actualReceivedQuantity) {
        if (actualReceivedQuantity <= 0) {
            throw new IllegalArgumentException("입수량은 1개 이상이어야 합니다.");
        }

        PurchaseOrder order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("해당 주문번호 (" + orderNumber + ")를 찾을 수 없습니다."));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("해당 주문은 이미 처리되었거나 취소되었습니다.");
        }

        Inventory inventory = inventoryRepository.findByPart(order.getPart())
                .orElseThrow(() -> new IllegalStateException("재고 마스터가 존재하지 않습니다."));

        // Calculate differences
        int oldRemaining = order.getQuantity() - order.getReceivedQuantity();
        int safeDeduct = Math.min(actualReceivedQuantity, oldRemaining);
        if (safeDeduct < 0) safeDeduct = 0; // Guard just in case

        // 재고 마스터 업데이트: 예정 수량 차감(최대 잔여량까지만), 실제 재고 증가
        inventory.setPendingIncoming(Math.max(0, inventory.getPendingIncoming() - safeDeduct));
        inventory.setCurrentStock(inventory.getCurrentStock() + actualReceivedQuantity);
        inventoryRepository.save(inventory);

        // Update Tracker
        order.setReceivedQuantity(order.getReceivedQuantity() + actualReceivedQuantity);
        order.setLastReceiptDate(java.time.LocalDateTime.now());
        
        // 상태값 동적 변경 (초과수납 포함)
        if (order.getReceivedQuantity() > order.getQuantity()) {
            order.setStatus(OrderStatus.OVER_RECEIVED);
        } else if (order.getReceivedQuantity() == order.getQuantity()) {
            order.setStatus(OrderStatus.COMPLETED);
        }
        orderRepository.save(order);

        // 입하 로그 기록
        ReceiptLog log = new ReceiptLog();
        log.setOrder(order);
        log.setReceivedQuantity(actualReceivedQuantity);
        log.setRemarks("바코드 스캔 입고 완료");

        return receiptRepository.save(log);
    }

    // 3. 폐기 (Dispose)
    public DisposeLog disposePart(String productCode, int quantity, String reason) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("폐기 수량은 1개 이상이어야 합니다.");
        }

        Part part = partRepository.findByProductCode(productCode)
                .orElseThrow(() -> new IllegalArgumentException("해당 부품 코드(" + productCode + ")를 찾을 수 없습니다."));

        Inventory inventory = inventoryRepository.findByPart(part)
                .orElseThrow(() -> new IllegalStateException("재고 정보가 없습니다."));

        if (inventory.getCurrentStock() < quantity) {
            throw new IllegalStateException("지정한 수량만큼의 재고가 부족합니다.");
        }

        // 재고 마스터 업데이트: 실제 재고 차감
        inventory.setCurrentStock(inventory.getCurrentStock() - quantity);
        inventoryRepository.save(inventory);

        // 폐기 로그 기록
        DisposeLog log = new DisposeLog();
        log.setPart(part);
        log.setDisposedQuantity(quantity);
        log.setReason(reason);

        return disposeRepository.save(log);
    }
}
