package info.rio.project_ai901.services.report.components;

import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSimpleField;
import org.springframework.stereotype.Component;

import info.rio.project_ai901.services.report.dto.SalesReportData;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class SalesReportWordBuilder {

    private static final String BLUE = "1F3864";
    private static final String GRAY = "F2F2F2";
    private static final String MUTED = "7F7F7F";
    private static final String FONT = "Calibri";
    private static final Locale ES = Locale.forLanguageTag("es-MX");
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DMY_HM = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] build(SalesReportData d) {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            NumberFormat money = NumberFormat.getCurrencyInstance(ES);

            setPage(doc);
            addFooter(doc);
            addTitle(doc, d);
            addSummary(doc, d, money);
            addBranches(doc, d, money);
            addProducts(doc, d, money);
            addAnalysis(doc, d);
            addDetail(doc, d, money);

            doc.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ---------- Pagina y pie ----------

    private void setPage(XWPFDocument doc) {
        var body = doc.getDocument().getBody();
        CTSectPr sect = body.isSetSectPr() ? body.getSectPr() : body.addNewSectPr();

        CTPageSz size = sect.isSetPgSz() ? sect.getPgSz() : sect.addNewPgSz();
        size.setW(BigInteger.valueOf(12240));   // carta
        size.setH(BigInteger.valueOf(15840));

        CTPageMar mar = sect.isSetPgMar() ? sect.getPgMar() : sect.addNewPgMar();
        mar.setTop(BigInteger.valueOf(1134));   // 2 cm
        mar.setBottom(BigInteger.valueOf(1134));
        mar.setLeft(BigInteger.valueOf(1134));
        mar.setRight(BigInteger.valueOf(1134));
    }

    private void addFooter(XWPFDocument doc) {
        XWPFFooter footer = doc.createFooter(HeaderFooterType.DEFAULT);
        XWPFParagraph p = footer.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun r = p.createRun();
        style(r, 9, false, MUTED);
        r.setText("Documento generado automáticamente  |  Página ");
        CTSimpleField page = p.getCTP().addNewFldSimple();
        page.setInstr("PAGE");
    }

    // ---------- Secciones ----------

    private void addTitle(XWPFDocument doc, SalesReportData d) {
        para(doc, "Informe de ventas", 26, true, false, BLUE, 0, 60);
        para(doc, d.scope() + "  ·  " + d.period().label(), 13, false, false, "404040", 0, 60);
        para(doc, "Generado el " + DMY_HM.format(d.generatedAt()) + " por " + d.generatedBy(),
                9, false, false, MUTED, 0, 40);
        para(doc, "Criterio: se consideran ventas los pedidos en estado Entregado cuya fecha de pedido "
                + "está dentro del periodo.", 9, false, true, MUTED, 0, 200);
    }

    private void addSummary(XWPFDocument doc, SalesReportData d, NumberFormat money) {
        heading(doc, "Resumen ejecutivo");
        table(doc,
                new String[]{"Indicador", "Valor"},
                new int[]{4000, 5972},
                new boolean[]{false, true},
                List.of(
                        new String[]{"Pedidos entregados", String.valueOf(d.totalOrders())},
                        new String[]{"Ventas totales", money.format(d.totalSales())},
                        new String[]{"Promedio por pedido", money.format(d.averageTicket())}));
    }

    private void addBranches(XWPFDocument doc, SalesReportData d, NumberFormat money) {
        heading(doc, "Ventas por sucursal");
        List<String[]> rows = new ArrayList<>();
        for (var b : d.branches()) {
            rows.add(new String[]{
                    b.name() + " (" + b.code() + ")",
                    String.valueOf(b.orders()),
                    money.format(b.total()),
                    money.format(b.average()),
                    b.sharePercent() + " %"});
        }
        table(doc,
                new String[]{"Sucursal", "Pedidos", "Total", "Promedio", "% del total"},
                new int[]{3300, 1300, 2000, 1900, 1472},
                new boolean[]{false, true, true, true, true},
                rows);
    }

    private void addProducts(XWPFDocument doc, SalesReportData d, NumberFormat money) {
        heading(doc, "Productos con mejor rendimiento");
        if (d.topProducts().isEmpty()) {
            para(doc, "No hay productos para mostrar en este periodo.", 11, false, false, "000000", 0, 100);
            return;
        }
        List<String[]> rows = new ArrayList<>();
        int n = 1;
        for (var p : d.topProducts()) {
            rows.add(new String[]{String.valueOf(n++), p.sku(), p.name(),
                    String.valueOf(p.units()), money.format(p.revenue())});
        }
        table(doc,
                new String[]{"#", "SKU", "Producto", "Unidades", "Ingresos"},
                new int[]{600, 1800, 4000, 1400, 2172},
                new boolean[]{false, false, false, true, true},
                rows);
    }

    private void addAnalysis(XWPFDocument doc, SalesReportData d) {
        heading(doc, "Análisis");
        if (d.analysis() == null || d.analysis().isBlank()) {
            para(doc, "El análisis no está disponible en este momento.", 11, false, true, MUTED, 0, 100);
            return;
        }
        String clean = d.analysis().replace("**", "").replace("#", "");
        for (String block : clean.split("\\R+")) {
            if (!block.isBlank()) {
                para(doc, block.trim(), 11, false, false, "000000", 0, 120);
            }
        }
        para(doc, "Análisis generado con IA a partir de las cifras de este informe.",
                9, false, true, MUTED, 60, 0);
    }

    private void addDetail(XWPFDocument doc, SalesReportData d, NumberFormat money) {
        // Salto de pagina antes del detalle
        XWPFParagraph pb = doc.createParagraph();
        pb.createRun().addBreak(BreakType.PAGE);

        heading(doc, "Detalle de pedidos");
        if (d.ordersTruncated()) {
            para(doc, "Se muestran los primeros " + d.orders().size() + " de " + d.totalOrders()
                    + " pedidos.", 9, false, true, MUTED, 0, 100);
        }
        List<String[]> rows = new ArrayList<>();
        for (var o : d.orders()) {
            rows.add(new String[]{o.code(), DMY.format(o.date()), o.branch(), money.format(o.total())});
        }
        table(doc,
                new String[]{"Código", "Fecha", "Sucursal", "Total"},
                new int[]{2800, 2200, 3000, 1972},
                new boolean[]{false, false, false, true},
                rows);
    }

    // ---------- Ayudantes de formato ----------

    private void heading(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(280);
        p.setSpacingAfter(100);
        p.setKeepNext(true);
        p.setBorderBottom(Borders.SINGLE);
        XWPFRun r = p.createRun();
        style(r, 14, true, BLUE);
        r.setText(text);
    }

    private XWPFParagraph para(XWPFDocument doc, String text, int size, boolean bold, boolean italic,
                               String color, int before, int after) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(before);
        p.setSpacingAfter(after);
        XWPFRun r = p.createRun();
        style(r, size, bold, color);
        r.setItalic(italic);
        r.setText(text);
        return p;
    }

    private void table(XWPFDocument doc, String[] headers, int[] widths, boolean[] right, List<String[]> rows) {
        XWPFTable t = doc.createTable(1, headers.length);
        t.setWidth("100%");
        t.setCellMargins(60, 100, 60, 100);
        t.setTopBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "BFBFBF");
        t.setBottomBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "BFBFBF");
        t.setInsideHBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "D9D9D9");
        t.setLeftBorder(XWPFTable.XWPFBorderType.NONE, 0, 0, "FFFFFF");
        t.setRightBorder(XWPFTable.XWPFBorderType.NONE, 0, 0, "FFFFFF");
        t.setInsideVBorder(XWPFTable.XWPFBorderType.NONE, 0, 0, "FFFFFF");

        XWPFTableRow head = t.getRow(0);
        head.setRepeatHeader(true);   // la cabecera se repite en cada pagina
        for (int i = 0; i < headers.length; i++) {
            cell(head.getCell(i), headers[i], true, "FFFFFF", BLUE, right[i], widths[i]);
        }

        int n = 0;
        for (String[] values : rows) {
            XWPFTableRow row = t.createRow();
            String fill = (n++ % 2 == 1) ? GRAY : null;   // filas alternadas
            for (int i = 0; i < values.length; i++) {
                cell(row.getCell(i), values[i], false, "000000", fill, right[i], widths[i]);
            }
        }
    }

    private void cell(XWPFTableCell c, String text, boolean bold, String color, String fill,
                      boolean right, int width) {
        c.setWidth(String.valueOf(width));
        if (fill != null) {
            c.setColor(fill);
        }
        c.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        XWPFParagraph p = c.getParagraphs().get(0);
        p.setAlignment(right ? ParagraphAlignment.RIGHT : ParagraphAlignment.LEFT);
        XWPFRun r = p.createRun();
        style(r, 10, bold, color);
        r.setText(text == null ? "" : text);
    }

    private void style(XWPFRun r, int size, boolean bold, String color) {
        r.setFontFamily(FONT);
        r.setFontSize(size);
        r.setBold(bold);
        r.setColor(color);
    }
}