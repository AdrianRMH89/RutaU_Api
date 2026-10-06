package com.rutau.service;

import com.rutau.dto.response.DemandReportResponseDTO;
import com.rutau.dto.response.ReportCountDTO;
import com.rutau.exception.BusinessRuleException;
import com.rutau.util.PdfWriter;
import com.rutau.util.XlsxWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportExportService {

    private final AdminReportService adminReportService;

    // Archivo listo para descargar
    public record ExportedFile(String fileName, String contentType, byte[] content) {}

    // ==================== US18 - Exportar reportes en PDF o Excel ====================

    public ExportedFile exportDemandReport(String format, LocalDate from, LocalDate to) {
        String type = format == null ? "" : format.trim().toLowerCase();
        if (!type.equals("xlsx") && !type.equals("pdf")) {
            throw new BusinessRuleException("Formato no válido: usa 'xlsx' (Excel) o 'pdf'");
        }

        // Se exporta exactamente el mismo reporte de la US17
        DemandReportResponseDTO report = adminReportService.demandReport(from, to);

        // US18 - Escenario de error: no se exporta un reporte vacío
        if (report.totalRequests() == 0) {
            throw new BusinessRuleException("No hay información para exportar");
        }

        String baseName = "reporte-demanda_" + report.from() + "_" + report.to();
        if (type.equals("xlsx")) {
            // US18 - Escenario exitoso: archivo .xlsx
            return new ExportedFile(baseName + ".xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", toExcel(report));
        }
        // US18 - Escenario alternativo: el mismo reporte en PDF, con sus gráficos
        return new ExportedFile(baseName + ".pdf", "application/pdf", toPdf(report));
    }

    private byte[] toExcel(DemandReportResponseDTO report) {
        List<List<Object>> summary = List.of(
                List.of("Reporte de demanda RutaU", ""),
                List.of("Desde", report.from().toString()),
                List.of("Hasta", report.to().toString()),
                List.of("Agrupado por", report.groupedBy().equals("WEEK") ? "Semana" : "Día"),
                List.of("Total de solicitudes", report.totalRequests()));
        return new XlsxWriter()
                .addSheet("Resumen", summary)
                .addSheet("Rutas", table("Ruta", report.topRoutes()))
                .addSheet("Horarios", table("Horario", report.topHours()))
                .addSheet("Por periodo", table(report.groupedBy().equals("WEEK") ? "Semana" : "Día", report.timeline()))
                .toBytes();
    }

    private List<List<Object>> table(String header, List<ReportCountDTO> rows) {
        List<List<Object>> table = new ArrayList<>();
        table.add(List.of(header, "Solicitudes"));
        rows.forEach(r -> table.add(List.of(r.label(), r.requests())));
        return table;
    }

    private byte[] toPdf(DemandReportResponseDTO report) {
        PdfWriter pdf = new PdfWriter()
                .title("RutaU - Reporte de rutas y horarios con mayor demanda")
                .line("Período: " + report.from() + " al " + report.to())
                .line("Agrupado por: " + (report.groupedBy().equals("WEEK") ? "semana" : "día"))
                .line("Total de solicitudes: " + report.totalRequests());
        pdf.heading("Rutas más solicitadas");
        chart(pdf, report.topRoutes());
        pdf.heading("Horarios más solicitados");
        chart(pdf, report.topHours());
        pdf.heading("Solicitudes por " + (report.groupedBy().equals("WEEK") ? "semana" : "día"));
        chart(pdf, report.timeline());
        return pdf.toBytes();
    }

    private void chart(PdfWriter pdf, List<ReportCountDTO> rows) {
        pdf.barChart(rows.stream().map(ReportCountDTO::label).toList(),
                rows.stream().map(ReportCountDTO::requests).toList());
    }
}
