package com.example.plms.service;

import com.example.plms.domain.ReceiptLog;
import com.example.plms.domain.Supplier;
import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfTemplate;
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

    // Helper class for Page Number drawing (Current / Total)
    static class PageNumberEvent extends PdfPageEventHelper {
        private Font normalFont;
        private PdfTemplate totalPagesTemplate;
        
        public PageNumberEvent(Font normalFont) {
            this.normalFont = normalFont;
        }
        
        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            totalPagesTemplate = writer.getDirectContent().createTemplate(30, 16);
        }
        
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            String text = writer.getPageNumber() + " / ";
            float textSize = normalFont.getBaseFont().getWidthPoint(text, 10);
            
            cb.beginText();
            cb.setFontAndSize(normalFont.getBaseFont(), 10);
            
            float textBase = document.bottom() - 15;
            float center = document.getPageSize().getWidth() / 2;
            
            cb.setTextMatrix(center - (textSize / 2), textBase);
            cb.showText(text);
            cb.endText();
            
            cb.addTemplate(totalPagesTemplate, center + (textSize / 2), textBase);
        }
        
        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            totalPagesTemplate.beginText();
            totalPagesTemplate.setFontAndSize(normalFont.getBaseFont(), 10);
            totalPagesTemplate.showText(String.valueOf(writer.getPageNumber()));
            totalPagesTemplate.endText();
        }
    }

    public void generateSupplierSettlementZip(List<ReceiptLog> logs, LocalDate startDate, LocalDate endDate, ZipOutputStream zos) throws Exception {
        
        // Group by Supplier (Handle null suppliers safely by creating a dummy record or grouping to "Unspecified")
        Supplier dummySupplier = new Supplier();
        dummySupplier.setName("미지정_거래처");

        Map<Supplier, List<ReceiptLog>> groupedLogs = logs.stream()
                .filter(log -> log.getOrder() != null)
                .collect(Collectors.groupingBy(log -> log.getOrder().getSupplier() == null ? dummySupplier : log.getOrder().getSupplier()));

        // Load Font from ClassPathResource via Temp File (Safe for Spring Boot FAT Jars)
        org.springframework.core.io.ClassPathResource fontResource = new org.springframework.core.io.ClassPathResource("NotoSansJP.ttf");
        java.io.File tempFont = java.io.File.createTempFile("NotoSansJP_", ".ttf");
        tempFont.deleteOnExit();
        try (java.io.InputStream is = fontResource.getInputStream();
             java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFont)) {
            is.transferTo(fos);
        }
        
        BaseFont bf = BaseFont.createFont(tempFont.getAbsolutePath(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
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
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            writer.setPageEvent(new PageNumberEvent(normalFont));
            document.open();

            // 1. Header String
            Paragraph title = new Paragraph("清算書", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(25);
            document.add(title);

            // 2. Info Block
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String nowStr = java.time.LocalDateTime.now().format(dtf);
            
            document.add(new Paragraph("기간 : " + startDate + " ~ " + endDate, normalFont));
            document.add(new Paragraph("대상 회사 : " + supplierName, boldFont));
            document.add(new Paragraph("출력일 : " + nowStr, normalFont));
            document.add(new Paragraph(" ", normalFont)); // Spacing

            // 3. Table (7 Columns)
            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.5f, 1.5f, 2f, 2.5f, 1f, 1f, 1.5f});

            // Table Headers
            String[] headers = {"발주일", "입고일", "발주번호", "부품명", "발주갯수", "입고갯수", "청구금액(¥)"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(new Color(240, 240, 240));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(8);
                table.addCell(cell);
            }

            long grandTotal = 0;
            DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yy-MM-dd");

            for (ReceiptLog log : supplierLogs) {
                String orderDate = log.getOrder().getOrderDate().format(dateFmt);
                String receiveDate = log.getReceiveDate().format(dateFmt);
                String orderNo = log.getOrder().getOrderNumber();
                String partName = log.getOrder().getPart().getName();
                
                int orderQty = log.getOrder().getQuantity();
                int recvQty = log.getReceivedQuantity();
                int price = log.getOrder().getPart().getPrice();
                long total = (long) recvQty * price;
                grandTotal += total;

                table.addCell(new PdfPCell(new Phrase(orderDate, normalFont)));
                table.addCell(new PdfPCell(new Phrase(receiveDate, normalFont)));
                table.addCell(new PdfPCell(new Phrase(orderNo, normalFont)));
                table.addCell(new PdfPCell(new Phrase(partName, normalFont)));
                
                PdfPCell oQtyCell = new PdfPCell(new Phrase(String.valueOf(orderQty), normalFont));
                oQtyCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(oQtyCell);
                
                PdfPCell rQtyCell = new PdfPCell(new Phrase(String.valueOf(recvQty), normalFont));
                rQtyCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(rQtyCell);

                PdfPCell totalCell = new PdfPCell(new Phrase(String.format("%,d", total), normalFont));
                totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(totalCell);
            }
            document.add(table);

            // 4. Grand Total
            document.add(new Paragraph(" ", normalFont));
            Paragraph totalP = new Paragraph("총 청구 금액: ¥ " + String.format("%,d", grandTotal), titleFont);
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
