package com.rhsystem.application.service;

import com.rhsystem.application.dto.ClosureBreakdownResponse;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
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

    private static List<String> extractPunchSlots(String punchTimeline) {
        List<String> slots = new ArrayList<>();
        if (punchTimeline == null || punchTimeline.isBlank() || "—".equals(punchTimeline)) {
            return slots;
        }
        String[] parts = punchTimeline.split("\\|");
        for (String part : parts) {
            String token = part.trim();
            int space = token.lastIndexOf(' ');
            String time = space >= 0 ? token.substring(space + 1).trim() : token;
            if (!time.matches("\\d{2}:\\d{2}")) {
                continue;
            }
            slots.add(time);
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
        int maxPunches = 0;
        if (lines != null) {
            for (ClosureBreakdownResponse.DailyLine d : lines) {
                int count = extractPunchSlots(d.getPunchTimeline()).size();
                if (count > maxPunches) {
                    maxPunches = count;
                }
            }
        }
        if (maxPunches == 0) {
            maxPunches = 4;
        }
        int workedCol = 2 + maxPunches;
        int extraRegularCol = workedCol + 1;
        int extraWeekendCol = workedCol + 2;
        int absenceCol = workedCol + 3;
        StringBuilder controle = new StringBuilder();
        List<String> header = new ArrayList<>();
        header.add(cellString("Data"));
        for (int i = 1; i <= maxPunches; i++) {
            int pair = (i + 1) / 2;
            header.add(cellString((i % 2 == 1 ? "Entrada " : "Saída ") + pair));
        }
        header.add(cellString("Horas Trabalhadas"));
        header.add(cellString("Horas Extras " + overtimePct + "%"));
        header.add(cellString("Horas Extras " + weekendPct + "%"));
        header.add(cellString("Horas Faltas"));
        controle.append(rowFromParts(header.toArray(new String[0])));

        if (lines != null) {
            for (ClosureBreakdownResponse.DailyLine d : lines) {
                List<String> slots = extractPunchSlots(d.getPunchTimeline());
                List<String> row = new ArrayList<>();
                row.add(cellDate(d.getDate()));
                for (int i = 0; i < maxPunches; i++) {
                    row.add(cellTime(i < slots.size() ? slots.get(i) : ""));
                }

                StringBuilder workedFormula = new StringBuilder("=");
                boolean hasPairs = false;
                for (int c = 2; c <= 1 + maxPunches; c += 2) {
                    int inCol = c;
                    int outCol = c + 1;
                    if (outCol > 1 + maxPunches) {
                        break;
                    }
                    if (hasPairs) {
                        workedFormula.append("+");
                    }
                    workedFormula.append("IF(OR(RC").append(inCol).append("=\"\",RC").append(outCol)
                            .append("=\"\"),0,RC").append(outCol).append("-RC").append(inCol).append(")");
                    hasPairs = true;
                }
                if (!hasPairs) {
                    workedFormula.append("0");
                }

                row.add(cellFormulaWithStyle(workedFormula.toString(), "duration"));
                row.add(cellFormulaWithStyle(
                        "=IF(WEEKDAY(RC1)=7,IF(RC" + workedCol + ">" + weekendNormExpr + ",MIN(RC" + workedCol + "-" + weekendNormExpr + "," + tierOneCapExpr + "),0),IF(RC" + workedCol + ">" + dailyNormExpr + ",MIN(RC" + workedCol + "-" + dailyNormExpr + "," + tierOneCapExpr + "),0))",
                        "duration"));
                row.add(cellFormulaWithStyle(
                        "=IF(WEEKDAY(RC1)=7,IF(RC" + workedCol + ">" + weekendTierTwoStartExpr + ",RC" + workedCol + "-" + weekendTierTwoStartExpr + ",0),IF(RC" + workedCol + ">" + dailyTierTwoStartExpr + ",RC" + workedCol + "-" + dailyTierTwoStartExpr + ",0))",
                        "duration"));
                row.add(cellFormulaWithStyle(
                        "=IF(WEEKDAY(RC1)=7,IF(RC" + workedCol + "<" + weekendNormExpr + "," + weekendNormExpr + "-RC" + workedCol + ",0),IF(RC" + workedCol + "<" + dailyNormExpr + "," + dailyNormExpr + "-RC" + workedCol + ",0))",
                        "duration"));
                controle.append(rowFromParts(row.toArray(new String[0])));
            }
        }
        if (lines != null && !lines.isEmpty()) {
            int firstDataRow = 2;
            int lastDataRow = lines.size() + 1;
            List<String> total = new ArrayList<>();
            total.add(cellString("TOTAL"));
            for (int i = 0; i < maxPunches; i++) {
                total.add(cellString(""));
            }
            total.add(cellFormulaWithStyle("=SUM(R" + firstDataRow + "C" + workedCol + ":R" + lastDataRow + "C" + workedCol + ")", "duration"));
            total.add(cellFormulaWithStyle("=SUM(R" + firstDataRow + "C" + extraRegularCol + ":R" + lastDataRow + "C" + extraRegularCol + ")", "duration"));
            total.add(cellFormulaWithStyle("=SUM(R" + firstDataRow + "C" + extraWeekendCol + ":R" + lastDataRow + "C" + extraWeekendCol + ")", "duration"));
            total.add(cellFormulaWithStyle("=SUM(R" + firstDataRow + "C" + absenceCol + ":R" + lastDataRow + "C" + absenceCol + ")", "duration"));
            controle.append(rowFromParts(total.toArray(new String[0])));
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
