package com.resto.payment.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.resto.payment.dto.CashSessionReportDto;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class CashSessionPdfService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] generate(CashSessionReportDto report) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

            document.add(new Paragraph("Rapport de caisse", titleFont));
            document.add(new Paragraph(" ", bodyFont));

            document.add(line("Magasin", nullToDash(report.getStoreName()), bodyFont));
            document.add(line("Caissier", nullToDash(report.getCashierName()), bodyFont));
            document.add(line("Ouverture", formatDt(report.getOpenedAt()), bodyFont));
            document.add(line("Fermeture", formatDt(report.getClosedAt()), bodyFont));
            if (report.getOpeningFloat() != null) {
                document.add(line("Fond de caisse", money(report.getOpeningFloat()), bodyFont));
            }
            if (report.getClosingNotes() != null && !report.getClosingNotes().isBlank()) {
                document.add(line("Notes", report.getClosingNotes(), bodyFont));
            }

            document.add(new Paragraph(" ", bodyFont));
            document.add(new Paragraph("Chiffre d'affaires", sectionFont));
            document.add(line("CA total", money(report.getTotalRevenue()), bodyFont));
            document.add(line("Nombre de paiements", String.valueOf(report.getPaymentCount()), bodyFont));

            if (report.getRevenueByMethod() != null && !report.getRevenueByMethod().isEmpty()) {
                document.add(new Paragraph(" ", bodyFont));
                document.add(new Paragraph("Répartition par moyen de paiement", sectionFont));
                PdfPTable methodTable = new PdfPTable(2);
                methodTable.setWidthPercentage(100);
                methodTable.setWidths(new float[]{3f, 2f});
                addHeaderCell(methodTable, "Moyen", smallFont);
                addHeaderCell(methodTable, "Montant", smallFont);
                for (Map.Entry<String, BigDecimal> entry : report.getRevenueByMethod().entrySet()) {
                    addCell(methodTable, entry.getKey(), smallFont);
                    addCell(methodTable, money(entry.getValue()), smallFont);
                }
                document.add(methodTable);
            }

            document.add(new Paragraph(" ", bodyFont));
            document.add(new Paragraph("Ventes par produit", sectionFont));
            PdfPTable productTable = new PdfPTable(3);
            productTable.setWidthPercentage(100);
            productTable.setWidths(new float[]{4f, 1.5f, 2f});
            addHeaderCell(productTable, "Produit", smallFont);
            addHeaderCell(productTable, "Qté", smallFont);
            addHeaderCell(productTable, "Montant", smallFont);
            if (report.getProducts() == null || report.getProducts().isEmpty()) {
                addCell(productTable, "Aucun produit", smallFont);
                addCell(productTable, "-", smallFont);
                addCell(productTable, "-", smallFont);
            } else {
                for (CashSessionReportDto.ProductLine product : report.getProducts()) {
                    addCell(productTable, nullToDash(product.getProductName()), smallFont);
                    addCell(productTable, String.valueOf(product.getQuantity()), smallFont);
                    addCell(productTable, money(product.getTotalAmount()), smallFont);
                }
            }
            document.add(productTable);

            document.add(new Paragraph(" ", bodyFont));
            document.add(new Paragraph("Historique des commandes", sectionFont));
            PdfPTable orderTable = new PdfPTable(5);
            orderTable.setWidthPercentage(100);
            orderTable.setWidths(new float[]{1.2f, 2.2f, 2f, 1.8f, 1.8f});
            addHeaderCell(orderTable, "N°", smallFont);
            addHeaderCell(orderTable, "Heure", smallFont);
            addHeaderCell(orderTable, "Statut", smallFont);
            addHeaderCell(orderTable, "Encaissé", smallFont);
            addHeaderCell(orderTable, "Moyens", smallFont);
            if (report.getOrders() == null || report.getOrders().isEmpty()) {
                addCell(orderTable, "-", smallFont);
                addCell(orderTable, "Aucune commande", smallFont);
                addCell(orderTable, "-", smallFont);
                addCell(orderTable, "-", smallFont);
                addCell(orderTable, "-", smallFont);
            } else {
                for (CashSessionReportDto.OrderLine order : report.getOrders()) {
                    addCell(orderTable, order.getOrderNumber() != null ? "#" + order.getOrderNumber() : "-", smallFont);
                    addCell(orderTable, formatDt(order.getCreatedAt()), smallFont);
                    String status = (order.getStatus() != null ? order.getStatus() : "-")
                            + " / " + (order.getPaymentStatus() != null ? order.getPaymentStatus() : "-");
                    addCell(orderTable, status, smallFont);
                    addCell(orderTable, money(order.getSessionPaidAmount()), smallFont);
                    addCell(orderTable,
                            order.getPaymentMethods() != null ? String.join(", ", order.getPaymentMethods()) : "-",
                            smallFont);
                }
            }
            document.add(orderTable);

            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Failed to generate cash session PDF", e);
        }
    }

    private static Paragraph line(String label, String value, Font font) {
        return new Paragraph(label + " : " + value, font);
    }

    private static void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(240, 240, 240));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setPadding(4);
        table.addCell(cell);
    }

    private static void addCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "-", font));
        cell.setPadding(4);
        table.addCell(cell);
    }

    private static String money(BigDecimal value) {
        if (value == null) {
            return "0.00";
        }
        return value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private static String formatDt(java.time.OffsetDateTime value) {
        if (value == null) {
            return "-";
        }
        return DT.format(value);
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
