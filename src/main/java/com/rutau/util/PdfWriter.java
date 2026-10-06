package com.rutau.util;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// US18 - Genera un PDF sencillo (texto + gráficos de barras) sin librerías externas.
// Un PDF es texto con "objetos" numerados; aquí se escriben las páginas, las fuentes y los dibujos.
public class PdfWriter {

    private static final float PAGE_WIDTH = 595;   // tamaño A4 en puntos
    private static final float PAGE_HEIGHT = 842;
    private static final float MARGIN = 50;
    private static final Charset WIN_ANSI = Charset.forName("windows-1252");   // admite tildes y ñ

    private final List<StringBuilder> pages = new ArrayList<>();
    private StringBuilder page;
    private float y;

    public PdfWriter() {
        newPage();
    }

    public PdfWriter title(String text) {
        space(30);
        text(MARGIN, y, 18, true, text);
        y -= 30;
        return this;
    }

    public PdfWriter heading(String text) {
        space(40);
        y -= 10;
        text(MARGIN, y, 13, true, text);
        y -= 20;
        return this;
    }

    public PdfWriter line(String text) {
        space(16);
        text(MARGIN, y, 10, false, text);
        y -= 16;
        return this;
    }

    // Gráfico de barras horizontales: una barra por fila, proporcional al valor más alto
    public PdfWriter barChart(List<String> labels, List<Long> values) {
        long max = values.stream().mapToLong(Long::longValue).max().orElse(1);
        float maxWidth = PAGE_WIDTH - 2 * MARGIN - 40;
        for (int i = 0; i < labels.size(); i++) {
            space(32);
            text(MARGIN, y, 9, false, labels.get(i));
            float width = Math.max(2, maxWidth * values.get(i) / max);
            page.append(String.format(Locale.US, "0.15 0.45 0.75 rg %.2f %.2f %.2f 10 re f%n",
                    MARGIN, y - 15, width));
            text(MARGIN + width + 5, y - 14, 9, true, String.valueOf(values.get(i)));
            y -= 32;
        }
        return this;
    }

    public byte[] toBytes() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();
        write(out, "%PDF-1.4\n");

        int pageCount = pages.size();
        // Objetos: 1 catálogo, 2 páginas, 3 y 4 fuentes, luego (página, contenido) por cada página
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < pageCount; i++) {
            kids.append(5 + i * 2).append(" 0 R ");
        }
        offsets.add(out.size());
        write(out, "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n");
        offsets.add(out.size());
        write(out, "2 0 obj << /Type /Pages /Kids [" + kids + "] /Count " + pageCount + " >> endobj\n");
        offsets.add(out.size());
        write(out, "3 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >> endobj\n");
        offsets.add(out.size());
        write(out, "4 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >> endobj\n");

        for (int i = 0; i < pageCount; i++) {
            int pageObj = 5 + i * 2;
            byte[] content = pages.get(i).toString().getBytes(WIN_ANSI);
            offsets.add(out.size());
            write(out, pageObj + " 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
                    + "/Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents " + (pageObj + 1) + " 0 R >> endobj\n");
            offsets.add(out.size());
            write(out, (pageObj + 1) + " 0 obj << /Length " + content.length + " >> stream\n");
            out.writeBytes(content);
            write(out, "\nendstream endobj\n");
        }

        int xref = out.size();
        StringBuilder sb = new StringBuilder("xref\n0 " + (offsets.size() + 1) + "\n0000000000 65535 f \n");
        for (int offset : offsets) {
            sb.append(String.format("%010d 00000 n \n", offset));
        }
        sb.append("trailer << /Size ").append(offsets.size() + 1).append(" /Root 1 0 R >>\nstartxref\n")
                .append(xref).append("\n%%EOF\n");
        write(out, sb.toString());
        return out.toByteArray();
    }

    // ==================== Métodos de apoyo ====================

    private void newPage() {
        page = new StringBuilder();
        pages.add(page);
        y = PAGE_HEIGHT - MARGIN;
    }

    // Si no queda espacio en la página, se empieza una nueva
    private void space(float needed) {
        if (y - needed < MARGIN) {
            newPage();
        }
    }

    private void text(float x, float yPos, int size, boolean bold, String text) {
        page.append(String.format(Locale.US, "0 0 0 rg BT /%s %d Tf %.2f %.2f Td (%s) Tj ET%n",
                bold ? "F2" : "F1", size, x, yPos, escape(text)));
    }

    // Caracteres que el PDF no admite tal cual: se escapan o se reemplazan (la flecha no existe en WinAnsi)
    private static String escape(String text) {
        String safe = text.replace("→", "->");
        StringBuilder sb = new StringBuilder();
        for (char c : safe.toCharArray()) {
            if (c == '(' || c == ')' || c == '\\') {
                sb.append('\\').append(c);
            } else if (WIN_ANSI.newEncoder().canEncode(c)) {
                sb.append(c);
            } else {
                sb.append('?');
            }
        }
        return sb.toString();
    }

    private static void write(ByteArrayOutputStream out, String text) {
        out.writeBytes(text.getBytes(StandardCharsets.ISO_8859_1));
    }
}
