package com.example.plms.controller;

import com.example.plms.domain.Inventory;
import com.example.plms.domain.Part;
import com.example.plms.domain.PurchaseOrder;
import com.example.plms.service.PartLifecycleService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class PartController {

    private final PartLifecycleService partService;

    public PartController(PartLifecycleService partService) {
        this.partService = partService;
    }

    // 1. Inventory View (Dashboard)
    @GetMapping("/")
    public String index(Model model) {
        List<Inventory> inventories = partService.getAllInventories();
        
        // Hide items with 0 stock and 0 pending orders
        List<Inventory> activeInventories = inventories.stream()
                .filter(inv -> inv.getCurrentStock() > 0 || inv.getPendingIncoming() > 0)
                .collect(Collectors.toList());

        List<PurchaseOrder> pendingOrders = partService.getPendingOrders();
        Map<String, List<PurchaseOrder>> pendingOrdersMap = pendingOrders.stream()
                .collect(Collectors.groupingBy(t -> t.getPart().getProductCode()));

        model.addAttribute("inventories", activeInventories);
        model.addAttribute("allMasterParts", partService.getAllParts());
        model.addAttribute("pendingOrdersMap", pendingOrdersMap);
        return "index";
    }

    // 2. Barcode Receive Page
    @GetMapping("/receive")
    public String receivePage() {
        return "receive";
    }

    // Barcode Receive Processing
    @PostMapping("/receive/process")
    public String processReceive(@RequestParam String orderNumber, RedirectAttributes redirectAttributes) {
        try {
            partService.receiveOrder(orderNumber.trim());
            redirectAttributes.addFlashAttribute("message", "주문번호 [" + orderNumber + "] 의 입고 처리가 성공적으로 완료되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "입하 실패: " + e.getMessage());
        }
        return "redirect:/receive";
    }

    // 3. Part Master Page
    @GetMapping("/master")
    public String partMaster(Model model) {
        model.addAttribute("parts", partService.getAllParts());
        return "part-master";
    }

    @PostMapping("/master/register")
    public String registerMaster(@ModelAttribute Part part, RedirectAttributes redirectAttributes) {
        try {
            partService.registerPart(part);
            redirectAttributes.addFlashAttribute("message", "부품 마스터(" + part.getProductCode() + ")가 신규 등록되었습니다. 기초 재고 0개가 자동 할당됩니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "마스터 등록 실패: " + e.getMessage());
        }
        return "redirect:/master";
    }

    @PostMapping("/master/update")
    public String updateMaster(@RequestParam String productCode, @RequestParam int price, @RequestParam int orderUnit, @RequestParam String expirationDate, @RequestParam int leadTimeDays, RedirectAttributes redirectAttributes) {
        try {
            partService.updatePartMaster(productCode, price, orderUnit, expirationDate, leadTimeDays);
            redirectAttributes.addFlashAttribute("message", "부품 마스터(" + productCode + ")가 수정되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "마스터 수정 실패: " + e.getMessage());
        }
        return "redirect:/master";
    }

    // 4. Order API
    @PostMapping("/parts/{code}/order")
    public String orderPart(@PathVariable String code, @RequestParam int quantity, @RequestParam(required = false) String remarks, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expectedArrivalDate, RedirectAttributes redirectAttributes) {
        try {
            PurchaseOrder order = partService.orderPart(code, quantity, remarks, expectedArrivalDate);
            redirectAttributes.addFlashAttribute("message", "부품 발주가 성공적으로 접수되었습니다. (주문 번호: " + order.getOrderNumber() + ")");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "발주 실패: " + e.getMessage());
        }
        return "redirect:/";
    }

    // 5. Dispose API
    @PostMapping("/parts/{code}/dispose")
    public String disposePart(@PathVariable String code, @RequestParam int quantity, @RequestParam(required = false) String remarks, RedirectAttributes redirectAttributes) {
        try {
            partService.disposePart(code, quantity, remarks);
            redirectAttributes.addFlashAttribute("message", "불량 식별: 재고 폐기가 완료되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "폐기 실패: " + e.getMessage());
        }
        return "redirect:/";
    }
}
