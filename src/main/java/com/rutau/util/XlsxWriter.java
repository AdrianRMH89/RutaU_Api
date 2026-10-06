package com.rutau.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

// US18 - Genera un archivo Excel (.xlsx) sencillo sin librerías externas.
// Un .xlsx es un ZIP con archivos XML: aquí se arma ese ZIP con una hoja por cada tabla.
public class XlsxWriter {

    // nombre de la hoja -> filas (cada fila es una lista de celdas: texto o número)
    private final Map<String, List<List<Object>>> sheets = new LinkedHashMap<>();

    public XlsxWriter addSheet(String name, List<List<Object>> rows) {
        sheets.put(name, rows);
        return this;
    }

    public byte[] toBytes() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            put(zip, "[Content_Types].xml", contentTypes());
            put(zip, "_rels/.rels", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                    </Relationships>""");
            put(zip, "xl/workbook.xml", workbook());
            put(zip, "xl/_rels/workbook.xml.rels", workbookRels());
            put(zip, "xl/styles.xml", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                    <fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font></fonts>
                    <fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills>
                    <borders count="1"><border/></borders>
                    <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
                    <cellXfs count="2"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/></cellXfs>
                    </styleSheet>""");
            int i = 1;
            for (List<List<Object>> rows : sheets.values()) {
                put(zip, "xl/worksheets/sheet" + i++ + ".xml", sheet(rows));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    private String contentTypes() {
        StringBuilder sb = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                <Default Extension="xml" ContentType="application/xml"/>
                <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
                """);
        for (int i = 1; i <= sheets.size(); i++) {
            sb.append("<Override PartName=\"/xl/worksheets/sheet").append(i)
                    .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>\n");
        }
        return sb.append("</Types>").toString();
    }

    private String workbook() {
        StringBuilder sb = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                <sheets>
                """);
        int i = 1;
        for (String name : sheets.keySet()) {
            sb.append("<sheet name=\"").append(escape(name)).append("\" sheetId=\"").append(i)
                    .append("\" r:id=\"rId").append(i++).append("\"/>\n");
        }
        return sb.append("</sheets>\n</workbook>").toString();
    }

    private String workbookRels() {
        StringBuilder sb = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                """);
        int i = 1;
        for (; i <= sheets.size(); i++) {
            sb.append("<Relationship Id=\"rId").append(i)
                    .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet")
                    .append(i).append(".xml\"/>\n");
        }
        sb.append("<Relationship Id=\"rId").append(i)
                .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>\n");
        return sb.append("</Relationships>").toString();
    }

    // La primera fila de cada hoja es el encabezado (en negrita)
    private String sheet(List<List<Object>> rows) {
        StringBuilder sb = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                <cols><col min="1" max="1" width="60" customWidth="1"/><col min="2" max="10" width="18" customWidth="1"/></cols>
                <sheetData>
                """);
        for (int r = 0; r < rows.size(); r++) {
            sb.append("<row r=\"").append(r + 1).append("\">");
            List<Object> cells = rows.get(r);
            for (int c = 0; c < cells.size(); c++) {
                String ref = (char) ('A' + c) + String.valueOf(r + 1);
                String style = r == 0 ? " s=\"1\"" : "";
                Object value = cells.get(c);
                if (value instanceof Number number) {
                    sb.append("<c r=\"").append(ref).append("\"").append(style).append("><v>")
                            .append(number).append("</v></c>");
                } else {
                    sb.append("<c r=\"").append(ref).append("\"").append(style).append(" t=\"inlineStr\"><is><t>")
                            .append(escape(String.valueOf(value))).append("</t></is></c>");
                }
            }
            sb.append("</row>\n");
        }
        return sb.append("</sheetData>\n</worksheet>").toString();
    }

    private static void put(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
