package com.rhsystem.application.service;

import com.rhsystem.application.dto.ClosureBreakdownResponse;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

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

    private static String cellNumber(int value) {
        return "<Cell><Data ss:Type=\"Number\">" + value + "</Data></Cell>";
    }

    private static String cellFormula(String formula) {
        return "<Cell ss:Formula=\"" + attrEscape(formula) + "\"><Data ss:Type=\"Number\">0</Data></Cell>";
    }

    private static String rowFromParts(String... parts) {
        StringBuilder sb = new StringBuilder("<Row>");
        for (String p : parts) {
            sb.append(p);
        }
        sb.append("</Row>");
        return sb.toString();
    }

    private static String rowEmpty() {
        return "<Row/>";
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
                + sheetsInner
                + "\n</Workbook>";
    }

    private static String formatMinutesPt(int minutes) {
        int h = minutes / 60;
        int m = minutes % 60;
        return h + "h " + (m < 10 ? "0" : "") + m + "m";
    }

    private static String hoursDecimalBr(int minutes) {
        return String.format(Locale.GERMANY, "%.2f", minutes / 60.0).replace('.', ',');
    }

    private static String yn(Boolean b) {
        return Boolean.TRUE.equals(b) ? "sim" : "não";
    }

    private static String dash(Object o) {
        return o == null ? "—" : Objects.toString(o);
    }

    public byte[] toSpreadsheetXml(ClosureBreakdownResponse r) {
        String monthLabel = Month.of(r.getMonth()).getDisplayName(TextStyle.FULL, new Locale("pt", "BR"));
        String company = r.getCompanyName() != null ? r.getCompanyName() : "—";

        StringBuilder resumo = new StringBuilder();
        resumo.append(rowFromParts(cellString("Fechamento de ponto — exportação (servidor)")));
        resumo.append(rowFromParts(cellString("Empresa"), cellString(company)));
        resumo.append(rowFromParts(cellString("Funcionário"), cellString(r.getEmployeeName())));
        resumo.append(rowFromParts(cellString("Período"), cellString(monthLabel + " / " + r.getYear())));
        resumo.append(rowEmpty());
        resumo.append(rowFromParts(cellString("Cadastro da empresa")));
        resumo.append(rowFromParts(cellString("Hora extra habilitada"), cellString(yn(r.getCompanyEnableOvertime()))));
        resumo.append(rowFromParts(cellString("Banco de horas habilitado"), cellString(yn(r.getCompanyEnableTimeBank()))));
        if (r.getCompanyOvertimePercentage() != null) {
            resumo.append(rowFromParts(cellString("% hora extra (cadastro)"), cellString(r.getCompanyOvertimePercentage() + "%")));
        }
        if (r.getCompanyWeekendPercentage() != null) {
            resumo.append(rowFromParts(cellString("% fim de semana (cadastro)"), cellString(r.getCompanyWeekendPercentage() + "%")));
        }
        resumo.append(rowEmpty());
        resumo.append(rowFromParts(cellString("Escala aplicada no cálculo")));
        resumo.append(rowFromParts(cellString("Norma diária (minutos)"), cellNumber(r.getScheduleDailyNormMinutes())));
        resumo.append(rowFromParts(cellString("Norma diária (h dec)"), cellString(hoursDecimalBr(r.getScheduleDailyNormMinutes()))));
        resumo.append(rowFromParts(cellString("Entrada esperada"), cellString(dash(r.getScheduleEntryTime()))));
        resumo.append(rowFromParts(cellString("Saída esperada"), cellString(dash(r.getScheduleExitTime()))));
        resumo.append(rowFromParts(cellString("Intervalo (min)"), cellString(dash(r.getScheduleBreakMinutes()))));
        resumo.append(rowFromParts(cellString("Tolerância atraso (min)"), cellString(dash(r.getScheduleToleranceLate()))));
        resumo.append(rowFromParts(cellString("Tolerância saída (min)"), cellString(dash(r.getScheduleToleranceLeave()))));
        resumo.append(rowEmpty());
        resumo.append(rowFromParts(cellString("Resumo — valores calculados agora (motor; Fechar Mês / Recalcular)")));
        resumo.append(rowFromParts(cellString("Horas trabalhadas (min)"), cellNumber(r.getCalculatedWorkedMinutes())));
        resumo.append(rowFromParts(cellString("Horas trabalhadas (h dec)"), cellString(hoursDecimalBr(r.getCalculatedWorkedMinutes()))));
        resumo.append(rowFromParts(cellString("Horas trabalhadas (texto)"), cellString(formatMinutesPt(r.getCalculatedWorkedMinutes()))));
        resumo.append(rowFromParts(cellString("Horas extras (min)"), cellNumber(r.getCalculatedExtraMinutes())));
        resumo.append(rowFromParts(cellString("Horas extras (h dec)"), cellString(hoursDecimalBr(r.getCalculatedExtraMinutes()))));
        resumo.append(rowFromParts(cellString("Horas extras (texto)"), cellString(formatMinutesPt(r.getCalculatedExtraMinutes()))));
        resumo.append(rowFromParts(cellString("Atrasos (min)"), cellNumber(r.getCalculatedDelayMinutes())));
        resumo.append(rowFromParts(cellString("Atrasos (texto)"), cellString(formatMinutesPt(r.getCalculatedDelayMinutes()))));
        resumo.append(rowFromParts(cellString("Ausências (min)"), cellNumber(r.getCalculatedAbsenceMinutes())));
        resumo.append(rowFromParts(cellString("Ausências (texto)"), cellString(formatMinutesPt(r.getCalculatedAbsenceMinutes()))));

        if (r.getClosedAt() != null) {
            resumo.append(rowEmpty());
            resumo.append(rowFromParts(cellString("Último fechamento salvo"), cellString(r.getClosedAt())));
            resumo.append(rowFromParts(cellString("Armazenado — trabalhadas (min)"), cellString(dash(r.getStoredWorkedMinutes()))));
            resumo.append(rowFromParts(cellString("Armazenado — extras (min)"), cellString(dash(r.getStoredExtraMinutes()))));
            resumo.append(rowFromParts(cellString("Armazenado — atrasos (min)"), cellString(dash(r.getStoredDelayMinutes()))));
            resumo.append(rowFromParts(cellString("Armazenado — ausências (min)"), cellString(dash(r.getStoredAbsenceMinutes()))));
        }

        List<ClosureBreakdownResponse.DailyLine> lines = r.getDailyLines();
        int n = lines != null ? lines.size() : 0;

        StringBuilder det = new StringBuilder();
        det.append(rowFromParts(
                cellString("Data"),
                cellString("Batidas (E/S)"),
                cellString("1ª entrada"),
                cellString("Trabalhado bruto (min)"),
                cellString("Trabalhado contábil (min)"),
                cellString("Extras dia (min)"),
                cellString("Atraso dia (min)"),
                cellString("Ausência dia (min)"),
                cellString("Fórmula / regra (linha)")
        ));
        if (lines != null) {
            for (ClosureBreakdownResponse.DailyLine d : lines) {
                det.append(rowFromParts(
                        cellString(d.getDate()),
                        cellString(d.getPunchTimeline() != null ? d.getPunchTimeline() : "—"),
                        cellString(dash(d.getFirstEntryTime())),
                        cellNumber(d.getWorkedRawMinutes()),
                        cellNumber(d.getWorkedCountedMinutes()),
                        cellNumber(d.getExtraMinutes()),
                        cellNumber(d.getDelayMinutes()),
                        cellNumber(d.getAbsenceMinutes()),
                        cellString(d.getCalculationNote() != null ? d.getCalculationNote() : "")
                ));
            }
        }
        if (n > 0) {
            int firstDataRow = 2;
            int lastDataRow = 1 + n;
            det.append(rowFromParts(
                    cellString("TOTAIS (Σ colunas numéricas — conferir Resumo)"),
                    cellString(""),
                    cellString(""),
                    cellFormula("=SUM(R" + firstDataRow + "C4:R" + lastDataRow + "C4)"),
                    cellFormula("=SUM(R" + firstDataRow + "C5:R" + lastDataRow + "C5)"),
                    cellFormula("=SUM(R" + firstDataRow + "C6:R" + lastDataRow + "C6)"),
                    cellFormula("=SUM(R" + firstDataRow + "C7:R" + lastDataRow + "C7)"),
                    cellFormula("=SUM(R" + firstDataRow + "C8:R" + lastDataRow + "C8)"),
                    cellString("Soma das linhas listadas (dias com evento ou trecho).")
            ));
        }

        StringBuilder regras = new StringBuilder();
        regras.append(rowFromParts(cellString("Regras do motor e parâmetros (exportação)")));
        regras.append(rowEmpty());
        if (r.getFormulaDocumentation() != null) {
            for (String line : r.getFormulaDocumentation()) {
                regras.append(rowFromParts(cellString(line)));
            }
        }

        String inner = worksheet("Resumo", resumo.toString())
                + worksheet("Detalhe diário", det.toString())
                + worksheet("Fórmulas e regras", regras.toString());

        return workbookXml(inner).getBytes(StandardCharsets.UTF_8);
    }

    public byte[] toCsv(ClosureBreakdownResponse r) {
        String monthLabel = Month.of(r.getMonth()).getDisplayName(TextStyle.FULL, new Locale("pt", "BR"));
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
