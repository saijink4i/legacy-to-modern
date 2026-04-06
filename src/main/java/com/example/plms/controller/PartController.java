package com.example.plms.controller;

import com.example.plms.domain.Part;
import com.example.plms.service.PartLifecycleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/")
public class PartController {

    private final PartLifecycleService partService;

    public PartController(PartLifecycleService partService) {
        this.partService = partService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("parts", partService.getAllParts());
        return "index";
    }

    @PostMapping("/parts")
    public String registerPart(Part part, RedirectAttributes redirectAttributes) {
        try {
            partService.registerPart(part);
            redirectAttributes.addFlashAttribute("message", "신규 부품이 등록되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "부품 등록 실패: " + e.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/parts/{code}/order")
    public String orderPart(@PathVariable String code, @RequestParam int quantity, @RequestParam(required = false) String remarks, RedirectAttributes redirectAttributes) {
        try {
            partService.orderPart(code, quantity, remarks);
            redirectAttributes.addFlashAttribute("message", "부품(" + code + ") 발주가 성공적으로 접수되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "발주 실패: " + e.getMessage());
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
