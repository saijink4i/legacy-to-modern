package com.example.plms.controller;

import com.example.plms.domain.Inventory;
import com.example.plms.domain.Part;
import com.example.plms.domain.PurchaseOrder;
import com.example.plms.service.PartLifecycleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

@Controller
public class PartController {

    private final PartLifecycleService partService;

    @Autowired
    private com.example.plms.service.PdfGeneratorService pdfGeneratorService;

    public PartController(PartLifecycleService partService) {
        this.partService = partService;
    }

    // 0. Landing Page (Portal View)
    @GetMapping("/")
    public String index() {
        return "landing";
    }

    // 1. Inventory View
    @GetMapping("/inventory")
    public String inventory(Model model) {
        List<Inventory> inventories = partService.getAllInventories();
        
        // Hide items with 0 stock and 0 pending orders
        List<Inventory> activeInventories = inventories.stream()
                .filter(inv -> inv.getCurrentStock() > 0 || inv.getPendingIncoming() > 0)
                .collect(Collectors.toList());

        List<PurchaseOrder> pendingOrders = partService.getPendingOrders();
        Map<String, List<PurchaseOrder>> pendingOrdersMap = pendingOrders.stream()
                .collect(Collectors.groupingBy(t -> t.getPart().getProductCode()));

        Map<String, List<com.example.plms.domain.ReceiptLog>> receiptLogsMap = partService.getAllReceiptLogs().stream()
                .collect(Collectors.groupingBy(r -> r.getOrder().getPart().getProductCode()));

        model.addAttribute("inventories", activeInventories);
        model.addAttribute("pendingOrdersMap", pendingOrdersMap);
        model.addAttribute("receiptLogsMap", receiptLogsMap);
        return "inventory";
    }

    // 2. Barcode Receive Page
    @GetMapping("/receive")
    public String receivePage(Model model) {
        model.addAttribute("allMasterParts", partService.getAllParts());
        model.addAttribute("pendingOrders", partService.getPendingOrders());
        return "receive";
    }

    // Barcode Receive Processing
    @PostMapping("/receive/process")
    public String processReceive(@RequestParam String orderNumber, @RequestParam int receivedQuantity, RedirectAttributes redirectAttributes) {
        try {
            partService.receiveOrder(orderNumber.trim(), receivedQuantity);
            redirectAttributes.addFlashAttribute("message", "注文No [" + orderNumber + "] の入庫処理 (" + receivedQuantity + "個) が完了されました。");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "入庫失敗: " + e.getMessage());
        }
        return "redirect:/receive";
    }

    // 3. Part Master Page
    @GetMapping("/master")
    public String partMaster(Model model) {
        model.addAttribute("parts", partService.getAllParts());
        model.addAttribute("suppliers", partService.getAllSuppliers());
        return "part-master";
    }

    @PostMapping("/master/register")
    public String registerMaster(@ModelAttribute Part part, @RequestParam(defaultValue = "0") int stockQuantity, RedirectAttributes redirectAttributes) {
        try {
            partService.registerPart(part, stockQuantity);
            redirectAttributes.addFlashAttribute("message", "部品マスタ(" + part.getProductCode() + ")が新規登録されました。初期在庫(" + stockQuantity + "個)が割当されました。");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "部品マスタ登録失敗: " + e.getMessage());
        }
        return "redirect:/master";
    }

    @PostMapping("/master/update")
    public String updateMaster(@RequestParam String productCode, @RequestParam int price, @RequestParam int orderUnit, @RequestParam String expirationDate, @RequestParam int leadTimeDays, RedirectAttributes redirectAttributes) {
        try {
            partService.updatePartMaster(productCode, price, orderUnit, expirationDate, leadTimeDays);
            redirectAttributes.addFlashAttribute("message", "部品マスタ(" + productCode + ")が修正されました。");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "部品マスタ修正失敗: " + e.getMessage());
        }
        return "redirect:/master";
    }
    
    @PostMapping("/master/supplier/register")
    public String registerSupplier(@ModelAttribute com.example.plms.domain.Supplier supplier, RedirectAttributes redirectAttributes) {
        try {
            partService.registerSupplier(supplier);
            redirectAttributes.addFlashAttribute("message", "取引先マスタ(" + supplier.getSupplierCode() + ")が新規登録されました。");
            redirectAttributes.addFlashAttribute("activeTab", "supplier");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "取引先登録失敗: " + e.getMessage());
        }
        return "redirect:/master";
    }

    @PostMapping("/master/supplier/update")
    public String updateSupplier(@RequestParam Long id, @RequestParam String supplierCode, @RequestParam String name, @RequestParam String contactInfo, RedirectAttributes redirectAttributes) {
        try {
            partService.updateSupplier(id, supplierCode, name, contactInfo);
            redirectAttributes.addFlashAttribute("message", "取引先マスタ(" + supplierCode + ")が修正されました。");
            redirectAttributes.addFlashAttribute("activeTab", "supplier");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "取引先修正失敗: " + e.getMessage());
        }
        return "redirect:/master";
    }

    // 4. Order UI & API
    @GetMapping("/order")
    public String orderPage(Model model) {
        model.addAttribute("allMasterParts", partService.getAllParts());
        model.addAttribute("allSuppliers", partService.getAllSuppliers());
        model.addAttribute("orders", partService.getAllPurchaseOrders());
        return "order";
    }

    @PostMapping("/parts/{code}/order")
    public String orderPart(@PathVariable String code, @RequestParam int quantity, @RequestParam(required = false) String remarks, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expectedArrivalDate, @RequestParam(required = false) Long supplierId, RedirectAttributes redirectAttributes) {
        try {
            PurchaseOrder order = partService.orderPart(code, quantity, remarks, expectedArrivalDate, supplierId);
            redirectAttributes.addFlashAttribute("message", "部品発注が成功的に登録されました。(発注No: " + order.getOrderNumber() + ")");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "発注失敗: " + e.getMessage());
        }
        return "redirect:/order";
    }
    
    @PostMapping("/order/update")
    public String updateOrder(@RequestParam String orderNumber, @RequestParam int quantity, @RequestParam(required = false) String remarks, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expectedArrivalDate, @RequestParam(required = false) Long supplierId, RedirectAttributes redirectAttributes) {
        try {
            PurchaseOrder order = partService.updateOrder(orderNumber, quantity, remarks, expectedArrivalDate, supplierId);
            redirectAttributes.addFlashAttribute("message", "発注No [" + order.getOrderNumber() + "] が成功的に修正されました。");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "発注修正失敗: " + e.getMessage());
        }
        return "redirect:/order";
    }

    // 5. Dispose UI & API
    @GetMapping("/dispose")
    public String disposePage(Model model) {
        List<Inventory> activeInventories = partService.getAllInventories().stream()
                .filter(inv -> inv.getCurrentStock() > 0)
                .collect(Collectors.toList());
        model.addAttribute("inventories", activeInventories);
        return "dispose";
    }

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

    // 7. 거래처 (Supplier) 단일 삭제 (Delete)
    @PostMapping("/master/supplier/delete")
    public String deleteSupplierMaster(@RequestParam Long id, RedirectAttributes redirectAttributes) {
        try {
            partService.deleteSupplier(id);
            redirectAttributes.addFlashAttribute("message", "取引先マスターが削除されました。");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "削除失敗: " + e.getMessage());
        }
        return "redirect:/master";
    }

    // 8. 계상 (Accounting) View
    @GetMapping("/accounting")
    public String accountingView(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Model model) {
            
        LocalDate start;
        LocalDate end;
        
        if (startDate == null || endDate == null) {
            LocalDate today = LocalDate.now();
            start = today.withDayOfMonth(1);
            end = today.withDayOfMonth(today.lengthOfMonth());
        } else {
            start = LocalDate.parse(startDate);
            end = LocalDate.parse(endDate);
        }

        List<com.example.plms.domain.ReceiptLog> receipts = partService.getReceiptsBetween(start, end);

        // Group explicitly for View presentation
        Map<com.example.plms.domain.Supplier, Long> summary = receipts.stream()
            .collect(Collectors.groupingBy(
                r -> r.getOrder().getSupplier() == null ? new com.example.plms.domain.Supplier() : r.getOrder().getSupplier(),
                Collectors.summingLong(r -> (long) r.getReceivedQuantity() * r.getOrder().getPart().getPrice())
            ));

        model.addAttribute("startDate", start);
        model.addAttribute("endDate", end);
        model.addAttribute("receipts", receipts);
        model.addAttribute("summary", summary);
        
        return "accounting";
    }

    // 9. 계상 (Accounting) PDF Zip Download
    @GetMapping("/accounting/export-zip")
    public void exportAccountingZip(
            @RequestParam String startDate,
            @RequestParam String endDate,
            HttpServletResponse response) {
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            List<com.example.plms.domain.ReceiptLog> logs = partService.getReceiptsBetween(start, end);

            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename=\"Settlements_" + startDate + "_to_" + endDate + ".zip\"");

            ZipOutputStream zos = new ZipOutputStream(response.getOutputStream());
            pdfGeneratorService.generateSupplierSettlementZip(logs, start, end, zos);
            zos.close();

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
        }
    }
}
