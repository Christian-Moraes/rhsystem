package com.rhsystem.application.service;

import com.rhsystem.application.dto.ClosureBreakdownResponse;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

/**
 * Gera CSV e planilha Excel (SpreadsheetML .xls) no servidor — mesmo conteúdo esperado do fechamento.
 */
@Service
public class ClosureExportService {

    private static String xmlEscape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String attrEscape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;");
    }

    private static String cellString(String value) {
        return "<Cell><Data ss:Type=\"String\">" + xmlEscape(value != null ? value : "") + "</Data></Cell>";
    }

    private static String cellFormulaWithStyle(String formula, String styleId) {
        return "<Cell ss:StyleID=\"" + attrEscape(styleId) + "\" ss:Formula=\"" + attrEscape(formula)
                + "\"><Data ss:Type=\"Number\">0</Data></Cell>";
    }

    private static String cellDate(String isoDate) {
        if (isoDate == null || isoDate.isBlank()) {
            return cellString("");
        }
        try {
            LocalDate d = LocalDate.parse(isoDate);
            return "<Cell ss:StyleID=\"date\"><Data ss:Type=\"DateTime\">"
                    + d + "T00:00:00.000</Data></Cell>";
        } catch (Exception ignored) {
            return cellString(isoDate);
        }
    }

    private static String cellTime(String hhmm) {
        if (hhmm == null || hhmm.isBlank()) {
            return cellString("");
        }
        try {
            LocalTime t = LocalTime.parse(hhmm);
            return "<Cell ss:StyleID=\"time\"><Data ss:Type=\"DateTime\">1899-12-30T"
                    + t + ":00.000</Data></Cell>";
        } catch (Exception ignored) {
            return cellString(hhmm);
        }
    }

    private static String rowFromParts(String... parts) {
        StringBuilder sb = new StringBuilder("<Row>");
        for (String p : parts) {
            sb.append(p);
        }
        sb.append("</Row>");
        return sb.toString();
    }

    private static String worksheet(String name, String tableInner) {
        return "<Worksheet ss:Name=\"" + attrEscape(name) + "\"><Table>" + tableInner + "</Table></Worksheet>";
    }

    private static String workbookXml(String sheetsInner) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<?mso-application progid=\"Excel.Sheet\"?>\n"
                + "<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\" "
                + "xmlns:o=\"urn:schemas-microsoft-com:office:office\" "
                + "xmlns:x=\"urn:schemas-microsoft-com:office:excel\" "
                + "xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\" "
                + "xmlns:html=\"http://www.w3.org/TR/REC-html40\">\n"
                + "<Styles>"
                + "<Style ss:ID=\"date\"><NumberFormat ss:Format=\"yyyy-mm-dd\"/></Style>"
                + "<Style ss:ID=\"time\"><NumberFormat ss:Format=\"hh:mm\"/></Style>"
                + "<Style ss:ID=\"duration\"><NumberFormat ss:Format=\"[h]:mm\"/></Style>"
                + "</Styles>"
                + sheetsInner
                + "\n</Workbook>";
    }

    private static String excelTimeLiteralFromMinutes(int minutes) {
        int safe = Math.max(0, minutes);
        int h = safe / 60;
        int m = safe % 60;
        return "TIME(" + h + "," + m + ",0)";
    }

    private static String[] extractPunchSlots(String punchTimeline) {
        String[] slots = new String[] { "", "", "", "" };
        if (punchTimeline == null || punchTimeline.isBlank() || "—".equals(punchTimeline)) {
            return slots;
        }
        String[] parts = punchTimeline.split("\\|");
        int idx = 0;
        for (String part : parts) {
            String token = part.trim();
            int space = token.lastIndexOf(' ');
            String time = space >= 0 ? token.substring(space + 1).trim() : token;
            if (!time.matches("\\d{2}:\\d{2}")) {
                continue;
            }
            if (idx >= slots.length) {
                break;
            }
            slots[idx++] = time;
        }
        return slots;
    }

    private static String hoursDecimalBr(int minutes) {
        return String.format(Locale.GERMANY, "%.2f", minutes / 60.0).replace('.', ',');
    }

    private static String yn(Boolean b) {
        return Boolean.TRUE.equals(b) ? "sim" : "não";
    }

    public byte[] toSpreadsheetXml(ClosureBreakdownResponse r) {
        if (r.getCompanyOvertimePercentage() == null || r.getCompanyWeekendPercentage() == null) {
            throw new RuntimeException(
                    "Exportação de planilha exige percentuais de hora extra cadastrados na empresa "
                            + "(normal e fim de semana).");
        }
        int overtimePct = r.getCompanyOvertimePercentage();
        int weekendPct = r.getCompanyWeekendPercentage();
        int dailyNormMinutes = Math.max(0, r.getScheduleDailyNormMinutes());
        int weekendNormMinutes = dailyNormMinutes / 2;
        int tierOneCapMinutes = 120;
        String dailyNormExpr = excelTimeLiteralFromMinutes(dailyNormMinutes);
        String weekendNormExpr = excelTimeLiteralFromMinutes(weekendNormMinutes);
        String tierOneCapExpr = excelTimeLiteralFromMinutes(tierOneCapMinutes);
        String dailyTierTwoStartExpr = excelTimeLiteralFromMinutes(dailyNormMinutes + tierOneCapMinutes);
        String weekendTierTwoStartExpr = excelTimeLiteralFromMinutes(weekendNormMinutes + tierOneCapMinutes);
        List<ClosureBreakdownResponse.DailyLine> lines = r.getDailyLines();
        StringBuilder controle = new StringBuilder();
        controle.append(rowFromParts(
                cellString("Data"),
                cellString("Entrada Manhã"),
                cellString("Saída Manhã"),
                cellString("Entrada Tarde"),
                cellString("Saída Tarde"),
                cellString("Horas Trabalhadas"),
                cellString("Horas Extras " + overtimePct + "%"),
                cellString("Horas Extras " + weekendPct + "%"),
                cellString("Horas Faltas")
        ));

        if (lines != null) {
            for (ClosureBreakdownResponse.DailyLine d : lines) {
                String[] slots = extractPunchSlots(d.getPunchTimeline());
                controle.append(rowFromParts(
                        cellDate(d.getDate()),
                        cellTime(slots[0]),
                        cellTime(slots[1]),
                        cellTime(slots[2]),
                        cellTime(slots[3]),
                        cellFormulaWithStyle("=IF(OR(RC[-4]=\"\",RC[-3]=\"\"),0,RC[-3]-RC[-4])+IF(OR(RC[-2]=\"\",RC[-1]=\"\"),0,RC[-1]-RC[-2])", "duration"),
                        cellFormulaWithStyle("=IF(WEEKDAY(RC[-6])=7,IF(RC[-1]>" + weekendNormExpr + ",MIN(RC[-1]-" + weekendNormExpr + "," + tierOneCapExpr + "),0),IF(RC[-1]>" + dailyNormExpr + ",MIN(RC[-1]-" + dailyNormExpr + "," + tierOneCapExpr + "),0))", "duration"),
                        cellFormulaWithStyle("=IF(WEEKDAY(RC[-7])=7,IF(RC[-2]>" + weekendTierTwoStartExpr + ",RC[-2]-" + weekendTierTwoStartExpr + ",0),IF(RC[-2]>" + dailyTierTwoStartExpr + ",RC[-2]-" + dailyTierTwoStartExpr + ",0))", "duration"),
                        cellFormulaWithStyle("=IF(WEEKDAY(RC[-8])=7,IF(RC[-3]<" + weekendNormExpr + "," + weekendNormExpr + "-RC[-3],0),IF(RC[-3]<" + dailyNormExpr + "," + dailyNormExpr + "-RC[-3],0))", "duration")
                ));
            }
        }
        if (lines != null && !lines.isEmpty()) {
            int firstDataRow = 2;
            int lastDataRow = lines.size() + 1;
            controle.append(rowFromParts(
                    cellString("TOTAL"),
                    cellString(""),
                    cellString(""),
                    cellString(""),
                    cellString(""),
                    cellFormulaWithStyle("=SUM(R" + firstDataRow + "C6:R" + lastDataRow + "C6)", "duration"),
                    cellFormulaWithStyle("=SUM(R" + firstDataRow + "C7:R" + lastDataRow + "C7)", "duration"),
                    cellFormulaWithStyle("=SUM(R" + firstDataRow + "C8:R" + lastDataRow + "C8)", "duration"),
                    cellFormulaWithStyle("=SUM(R" + firstDataRow + "C9:R" + lastDataRow + "C9)", "duration")
            ));
        }

        String inner = worksheet("Controle de Jornada", controle.toString());

        return workbookXml(inner).getBytes(StandardCharsets.UTF_8);
    }

    public byte[] toCsv(ClosureBreakdownResponse r) {
        String company = r.getCompanyName() != null ? r.getCompanyName() : "—";
        StringBuilder lines = new StringBuilder();
        lines.append('\ufeff');

        lines.append("SEÇÃO;Resumo\n");
        lines.append(csvEsc("Empresa")).append(';').append(csvEsc(company)).append('\n');
        lines.append(csvEsc("Funcionário")).append(';').append(csvEsc(r.getEmployeeName())).append('\n');
        lines.append("Mês;").append(r.getMonth()).append(";Ano;").append(r.getYear()).append('\n');
        lines.append(csvEsc("Hora extra habilitada (empresa)")).append(';').append(csvEsc(yn(r.getCompanyEnableOvertime()))).append('\n');
        lines.append(csvEsc("Banco de horas habilitado (empresa)")).append(';').append(csvEsc(yn(r.getCompanyEnableTimeBank()))).append('\n');
        lines.append(csvEsc("Trabalhadas (min)")).append(';').append(r.getCalculatedWorkedMinutes())
                .append(';').append(csvEsc("h dec")).append(';').append(hoursDecimalBr(r.getCalculatedWorkedMinutes())).append('\n');
        lines.append(csvEsc("Extras (min)")).append(';').append(r.getCalculatedExtraMinutes())
                .append(';').append(csvEsc("h dec")).append(';').append(hoursDecimalBr(r.getCalculatedExtraMinutes())).append('\n');
        lines.append("Atrasos (min);").append(r.getCalculatedDelayMinutes()).append('\n');
        lines.append("Ausências (min);").append(r.getCalculatedAbsenceMinutes()).append('\n');
        lines.append('\n');

        lines.append("SEÇÃO;Detalhe diário\n");
        lines.append("Data;Batidas E/S;1ª entrada;Bruto min;Contábil min;Extras min;Atraso min;Ausência min;Nota cálculo\n");
        if (r.getDailyLines() != null) {
            for (ClosureBreakdownResponse.DailyLine d : r.getDailyLines()) {
                lines.append(csvEsc(d.getDate())).append(';')
                        .append(csvEsc(d.getPunchTimeline() != null ? d.getPunchTimeline() : "—")).append(';')
                        .append(csvEsc(d.getFirstEntryTime())).append(';')
                        .append(d.getWorkedRawMinutes()).append(';')
                        .append(d.getWorkedCountedMinutes()).append(';')
                        .append(d.getExtraMinutes()).append(';')
                        .append(d.getDelayMinutes()).append(';')
                        .append(d.getAbsenceMinutes()).append(';')
                        .append(csvEsc(d.getCalculationNote() != null ? d.getCalculationNote() : ""))
                        .append('\n');
            }
        }
        lines.append('\n');
        lines.append("SEÇÃO;Fórmulas e regras\n");
        if (r.getFormulaDocumentation() != null) {
            for (String f : r.getFormulaDocumentation()) {
                lines.append(csvEsc(f)).append('\n');
            }
        }

        return lines.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String csvEsc(String s) {
        if (s == null) {
            return "\"\"";
        }
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
