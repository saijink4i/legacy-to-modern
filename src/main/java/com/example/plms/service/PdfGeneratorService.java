package com.example.plms.service;

import com.example.plms.domain.ReceiptLog;
import com.example.plms.domain.Supplier;
import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class PdfGeneratorService {

    public void generateSupplierSettlementZip(List<ReceiptLog> logs, LocalDate startDate, LocalDate endDate, ZipOutputStream zos) throws Exception {
        
        // Group by Supplier (Handle null suppliers safely by creating a dummy record or grouping to "Unspecified")
        Supplier dummySupplier = new Supplier();
        dummySupplier.setName("미지정_거래처");

        Map<Supplier, List<ReceiptLog>> groupedLogs = logs.stream()
                .filter(log -> log.getOrder() != null)
                .collect(Collectors.groupingBy(log -> log.getOrder().getSupplier() == null ? dummySupplier : log.getOrder().getSupplier()));

        // Load Font
        ClassPathResource fontResource = new ClassPathResource("NotoSansJP.ttf");
        byte[] fontBytes = fontResource.getInputStream().readAllBytes();
        BaseFont bf = BaseFont.createFont("NotoSansJP.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, fontBytes, null);
        Font titleFont = new Font(bf, 18, Font.BOLD, Color.BLACK);
        Font headerFont = new Font(bf, 12, Font.BOLD, Color.DARK_GRAY);
        Font normalFont = new Font(bf, 10, Font.NORMAL, Color.BLACK);
        Font boldFont = new Font(bf, 11, Font.BOLD, Color.BLACK);

        for (Map.Entry<Supplier, List<ReceiptLog>> entry : groupedLogs.entrySet()) {
            Supplier supplier = entry.getKey();
            List<ReceiptLog> supplierLogs = entry.getValue();

            String supplierName = supplier.getName() != null ? supplier.getName() : "미지정_거래처";
            String safeFilename = supplierName.replaceAll("[\\\\/:*?\"<>|]", "_") + "_" + startDate + "_to_" + endDate + ".pdf";

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);
            document.open();

            // 1. Header String
            Paragraph title = new Paragraph("정산 전표 (Settlement Invoice)", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // 2. Info Block
            String periodStr = "정산 기간: " + startDate + " ~ " + endDate;
            document.add(new Paragraph(periodStr, normalFont));
            document.add(new Paragraph("거래처명: " + supplierName, boldFont));
            document.add(new Paragraph("발행일자: " + LocalDate.now(), normalFont));
            document.add(new Paragraph(" ", normalFont)); // Spacing

            // 3. Table
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.5f, 2f, 3f, 1f, 1.5f, 1.5f});

            // Table Headers
            String[] headers = {"입고일시", "주문번호", "부품명", "수량", "단가(¥)", "합계(¥)"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(new Color(240, 240, 240));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(8);
                table.addCell(cell);
            }

            long grandTotal = 0;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm");

            for (ReceiptLog log : supplierLogs) {
                String date = log.getReceiveDate().format(formatter);
                String orderNo = log.getOrder().getOrderNumber();
                String partName = log.getOrder().getPart().getName();
                int qty = log.getReceivedQuantity();
                int price = log.getOrder().getPart().getPrice();
                long total = (long) qty * price;
                grandTotal += total;

                table.addCell(new PdfPCell(new Phrase(date, normalFont)));
                table.addCell(new PdfPCell(new Phrase(orderNo, normalFont)));
                table.addCell(new PdfPCell(new Phrase(partName, normalFont)));
                
                PdfPCell qtyCell = new PdfPCell(new Phrase(String.valueOf(qty), normalFont));
                qtyCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(qtyCell);

                PdfPCell priceCell = new PdfPCell(new Phrase(String.format("%,d", price), normalFont));
                priceCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(priceCell);

                PdfPCell totalCell = new PdfPCell(new Phrase(String.format("%,d", total), normalFont));
                totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(totalCell);
            }
            document.add(table);

            // 4. Grand Total
            document.add(new Paragraph(" ", normalFont));
            Paragraph totalP = new Paragraph("총 정산 대금: ¥ " + String.format("%,d", grandTotal), titleFont);
            totalP.setAlignment(Element.ALIGN_RIGHT);
            document.add(totalP);

            document.close();

            // Write PDF to Zip
            ZipEntry zipEntry = new ZipEntry(safeFilename);
            zos.putNextEntry(zipEntry);
            zos.write(baos.toByteArray());
            zos.closeEntry();
        }
        
        // Prevent java.util.zip.ZipException when there are no logs to process in the time period
        if (groupedLogs.isEmpty()) {
            ZipEntry emptyEntry = new ZipEntry("NO_DATA.txt");
            zos.putNextEntry(emptyEntry);
            zos.write("해당 기간의 정산 데이터가 존재하지 않습니다. (No Settlement Data)".getBytes("UTF-8"));
            zos.closeEntry();
        }
    }
}
