package com.example.plms.controller;

import com.example.plms.domain.Part;
import com.example.plms.domain.PartTransaction;
import com.example.plms.service.PartLifecycleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/")
public class PartController {

    private final PartLifecycleService partService;

    public PartController(PartLifecycleService partService) {
        this.partService = partService;
    }

    @GetMapping
    public String index(Model model) {
        List<Part> allParts = partService.getAllParts();
        List<Part> inventoryParts = allParts.stream()
                .filter(p -> p.getStockQuantity() > 0 || p.getIncomingQuantity() > 0)
                .collect(Collectors.toList());

        Map<String, List<PartTransaction>> pendingOrdersMap = partService.getPendingTransactions().stream()
                .collect(Collectors.groupingBy(t -> t.getPart().getProductCode()));

        model.addAttribute("parts", inventoryParts);
        model.addAttribute("allMasterParts", allParts);
        model.addAttribute("pendingOrdersMap", pendingOrdersMap);
        return "index";
    }

    @GetMapping("/master")
    public String masterData(Model model) {
        model.addAttribute("parts", partService.getAllParts());
        return "part-master";
    }

    @PostMapping("/parts")
    public String registerPart(Part part, RedirectAttributes redirectAttributes) {
        try {
            partService.registerPart(part);
            redirectAttributes.addFlashAttribute("message", "신규 부품이 등록되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "부품 등록 실패: " + e.getMessage());
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

    @PostMapping("/parts/{code}/order")
    public String orderPart(@PathVariable String code, @RequestParam int quantity, @RequestParam(required = false) String remarks, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expectedArrivalDate, RedirectAttributes redirectAttributes) {
        try {
            partService.orderPart(code, quantity, remarks, expectedArrivalDate);
            redirectAttributes.addFlashAttribute("message", "부품(" + code + ") 발주가 성공적으로 접수되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "발주 실패: " + e.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/parts/{code}/receive")
    public String receivePart(@PathVariable String code, @RequestParam int quantity, @RequestParam(required = false) String remarks, RedirectAttributes redirectAttributes) {
        try {
            partService.receivePartByCode(code, quantity, remarks);
            redirectAttributes.addFlashAttribute("message", "부품(" + code + ") 입고 완료 (재고 반영됨)");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "입고 실패: " + e.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/parts/{code}/dispose")
    public String disposePart(@PathVariable String code, @RequestParam int quantity, @RequestParam(required = false) String remarks, RedirectAttributes redirectAttributes) {
        try {
            partService.disposePart(code, quantity, remarks);
            redirectAttributes.addFlashAttribute("message", "부품(" + code + ") 폐기가 성공적으로 처리되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "폐기 실패: " + e.getMessage());
        }
        return "redirect:/";
    }
}
