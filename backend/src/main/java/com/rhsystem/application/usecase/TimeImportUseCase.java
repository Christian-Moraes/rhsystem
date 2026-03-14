package com.rhsystem.application.usecase;

import com.rhsystem.application.dto.TimeEventResponse;
import com.rhsystem.application.dto.TimeImportResponse;
import com.rhsystem.domain.model.Employee;
import com.rhsystem.domain.model.TimeEvent;
import com.rhsystem.domain.model.TimeEventStatus;
import com.rhsystem.domain.model.TimeEventType;
import com.rhsystem.domain.repository.EmployeeRepository;
import com.rhsystem.domain.repository.TimeEventRepository;
import com.rhsystem.domain.repository.UserCompanyRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TimeImportUseCase {

    private static final Logger log = LoggerFactory.getLogger(TimeImportUseCase.class);

    private final TimeEventRepository timeEventRepository;
    private final EmployeeRepository employeeRepository;
    private final UserCompanyRepository userCompanyRepository;

    @Value("${groq.api-key:}")
    private String groqApiKey;

    @Value("${groq.api-key-file:}")
    private String groqApiKeyFile;

    @Value("${groq.model:}")
    private String groqModel;

    @Value("${groq.vision-fallbacks:meta-llama/llama-4-scout-17b-16e-instruct,llama-3.2-90b-vision-preview}")
    private String groqVisionFallbacks;

    @Value("${groq.thinking-model:}")
    private String groqThinkingModel;

    @Autowired
    private ObjectMapper objectMapper;

    @jakarta.annotation.PostConstruct
    public void initGroqKey() {
        if (groqApiKey == null || groqApiKey.isBlank()) {
            groqApiKey = System.getenv("GROQ_API_KEY");
            if (groqApiKey == null || groqApiKey.isBlank()) {
                groqApiKey = System.getenv("GROQ_APIKEY");
            }
        }
        if (groqApiKey != null) {
            groqApiKey = groqApiKey.replaceAll("\\s+", "").trim();
        }
        if (groqApiKey == null) {
            groqApiKey = "";
        }
        log.info("[IMPORT_IMAGE] GROQ_API_KEY ao iniciar: carregada={} length={} prefix={} keyFile={}",
                !groqApiKey.isEmpty(), groqApiKey.length(), groqApiKey.startsWith("gsk_") ? "gsk_" : (groqApiKey.isEmpty() ? "vazio" : "outro"), groqApiKeyFile);
    }

    /** Chave efetiva: se groq.api-key-file existir e tiver conteúdo, usa o arquivo (permite atualizar sem reiniciar). */
    private String getEffectiveGroqKey() {
        if (groqApiKeyFile != null && !groqApiKeyFile.isBlank()) {
            try {
                java.nio.file.Path path = java.nio.file.Path.of(groqApiKeyFile.trim());
                if (java.nio.file.Files.isRegularFile(path)) {
                    String fromFile = java.nio.file.Files.readString(path).replaceAll("\\s+", "").trim();
                    if (!fromFile.isEmpty()) {
                        return fromFile;
                    }
                }
            } catch (Exception e) {
                log.trace("[IMPORT_IMAGE] Não foi possível ler chave do arquivo {}: {}", groqApiKeyFile, e.getMessage());
            }
        }
        return groqApiKey != null ? groqApiKey.trim() : "";
    }

    public TimeImportUseCase(TimeEventRepository timeEventRepository,
                            EmployeeRepository employeeRepository,
                            UserCompanyRepository userCompanyRepository) {
        this.timeEventRepository = timeEventRepository;
        this.employeeRepository = employeeRepository;
        this.userCompanyRepository = userCompanyRepository;
    }

    @Transactional
    public TimeImportResponse processTimeImport(
            UUID userId, UUID companyId, UUID employeeId, MultipartFile file, String fileType,
            Integer month, Integer year, Integer quinzena) {
        verifyEmployeeAccess(userId, companyId, employeeId);

        List<TimeEvent> events = new ArrayList<>();

        if (fileType == null || fileType.isEmpty()) {
            fileType = detectFileType(file.getOriginalFilename());
        }

        int targetMonth = month != null ? month : LocalDate.now().getMonthValue();
        int targetYear = year != null ? year : LocalDate.now().getYear();

        switch (fileType.toUpperCase()) {
            case "IMAGE":
            case "JPG":
            case "JPEG":
            case "PNG":
                log.info("[IMPORT_IMAGE] Requisição: arquivo={} size={} bytes, mês/ano da tela={}/{}", file.getOriginalFilename(), file.getSize(), targetMonth, targetYear);
                events = processImageFile(employeeId, file, targetMonth, targetYear, quinzena);
                break;
            case "PDF":
                events = processPdfFile(employeeId, file);
                break;
            case "CSV":
                events = processCsvFile(employeeId, file);
                break;
            default:
                throw new RuntimeException("Tipo de arquivo não suportado: " + fileType);
        }

        // Merge: nunca criar duplicata. Só persiste batida se ainda não existir mesmo (eventTime ao minuto + eventType) para este funcionário.
        // Assim, reenviar a mesma imagem/quinzena apenas complementa o que falta, sem adicionar entradas/saídas repetidas.
        java.util.Set<String> existingKeys = new java.util.HashSet<>();
        // Sempre carregar o mês/ano alvo (tela) para não perder eventos já salvos
        List<TimeEvent> existingTarget = timeEventRepository.findByEmployeeIdAndMonth(employeeId, targetMonth, targetYear);
        for (TimeEvent ex : existingTarget) {
            existingKeys.add(mergeKey(ex.getEventTime(), ex.getEventType()));
        }
        // Carregar outros meses que aparecerem nos eventos extraídos (ex.: IA devolveu mês diferente da imagem)
        java.util.Set<String> monthsLoaded = new java.util.HashSet<>();
        monthsLoaded.add(targetYear + "-" + targetMonth);
        for (TimeEvent e : events) {
            LocalDate d = e.getEventTime().toLocalDate();
            String mk = d.getYear() + "-" + d.getMonthValue();
            if (monthsLoaded.add(mk)) {
                List<TimeEvent> existing = timeEventRepository.findByEmployeeIdAndMonth(employeeId, d.getMonthValue(), d.getYear());
                for (TimeEvent ex : existing) {
                    existingKeys.add(mergeKey(ex.getEventTime(), ex.getEventType()));
                }
            }
        }
        List<TimeEvent> savedEvents = new ArrayList<>();
        int skipped = 0;
        for (TimeEvent event : events) {
            String key = mergeKey(event.getEventTime(), event.getEventType());
            if (existingKeys.contains(key)) {
                skipped++;
                continue;
            }
            event.setId(UUID.randomUUID());
            event.setSource("IMPORT_" + fileType.toUpperCase());
            event.setCreatedAt(LocalDateTime.now());
            savedEvents.add(timeEventRepository.save(event));
            existingKeys.add(key);
        }
        if (skipped > 0) {
            log.info("[IMPORT] Merge: {} batidas já existiam e não foram duplicadas (total extraído: {})", skipped, events.size());
        }

        int importedCount = savedEvents.size();
        int responseMonth = targetMonth;
        int responseYear = targetYear;
        if (!savedEvents.isEmpty()) {
            LocalDate firstDate = savedEvents.get(0).getEventTime().toLocalDate();
            responseMonth = firstDate.getMonthValue();
            responseYear = firstDate.getYear();
        }
        List<TimeEvent> allMonthEvents = timeEventRepository.findByEmployeeIdAndMonth(employeeId, responseMonth, responseYear);
        allMonthEvents.sort((a, b) -> a.getEventTime().compareTo(b.getEventTime()));

        Map<String, List<String>> gradeByDate = new LinkedHashMap<>();
        for (TimeEvent e : allMonthEvents) {
            LocalDateTime t = e.getEventTime();
            String day = t.toLocalDate().toString();
            String hm = String.format("%02d:%02d", t.getHour(), t.getMinute());
            gradeByDate.computeIfAbsent(day, k -> new ArrayList<>()).add(hm);
        }

        TimeImportResponse response = new TimeImportResponse();
        response.setEvents(allMonthEvents.stream().map(this::toResponse).collect(Collectors.toList()));
        response.setTotalEvents(importedCount);
        response.setConfirmedEvents((int) savedEvents.stream()
                .filter(e -> e.getStatus() == TimeEventStatus.CONFIRMED)
                .count());
        response.setReviewEvents((int) savedEvents.stream()
                .filter(e -> e.getStatus() == TimeEventStatus.REVIEW)
                .count());
        response.setUncertainEvents((int) savedEvents.stream()
                .filter(e -> e.getStatus() == TimeEventStatus.UNCERTAIN)
                .count());
        response.setGradeByDate(gradeByDate);
        if (responseMonth != targetMonth || responseYear != targetYear) {
            String monthName = java.time.Month.of(responseMonth).getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.forLanguageTag("pt-BR"));
            response.setMessage(String.format("A imagem era do mês da própria folha: %s/%d. Alterar o mês na tela para %s/%d para ver a grade.", monthName, responseYear, monthName, responseYear));
        }

        return response;
    }

    /** Cria eventos de exemplo da 2ª quinzena (sem arquivo) para preencher a grade. */
    @Transactional
    public TimeImportResponse createDemoEventsForMonth(
            UUID userId, UUID companyId, UUID employeeId, int month, int year) {
        verifyEmployeeAccess(userId, companyId, employeeId);
        List<LocalDateTime> times = getDemoSecondHalfTimes(month, year);
        List<TimeEvent> savedEvents = new ArrayList<>();
        for (int i = 0; i < times.size(); i++) {
            TimeEvent event = new TimeEvent();
            event.setId(UUID.randomUUID());
            event.setEmployeeId(employeeId);
            event.setEventTime(times.get(i));
            event.setEventType(i % 2 == 0 ? TimeEventType.ENTRY : TimeEventType.EXIT);
            event.setStatus(TimeEventStatus.REVIEW);
            event.setSource("IMPORT_DEMO");
            event.setCreatedAt(LocalDateTime.now());
            savedEvents.add(timeEventRepository.save(event));
        }
        List<TimeEvent> allMonthEvents = timeEventRepository.findByEmployeeIdAndMonth(employeeId, month, year);
        allMonthEvents.sort((a, b) -> a.getEventTime().compareTo(b.getEventTime()));
        Map<String, List<String>> gradeByDate = new LinkedHashMap<>();
        for (TimeEvent e : allMonthEvents) {
            LocalDateTime t = e.getEventTime();
            String day = t.toLocalDate().toString();
            String hm = String.format("%02d:%02d", t.getHour(), t.getMinute());
            gradeByDate.computeIfAbsent(day, k -> new ArrayList<>()).add(hm);
        }
        TimeImportResponse response = new TimeImportResponse();
        response.setEvents(allMonthEvents.stream().map(this::toResponse).collect(Collectors.toList()));
        response.setTotalEvents(savedEvents.size());
        response.setReviewEvents(savedEvents.size());
        response.setConfirmedEvents(0);
        response.setUncertainEvents(0);
        response.setGradeByDate(gradeByDate);
        return response;
    }

    /** Importa por imagem apenas via IA (Groq). Sem fallback para dados de exemplo. */
    private List<TimeEvent> processImageFile(UUID employeeId, MultipartFile file, int targetMonth, int targetYear, Integer quinzena) {
        String key = getEffectiveGroqKey();
        if (key.isEmpty()) {
            throw new RuntimeException("Configure GROQ_API_KEY no ambiente ou em " + groqApiKeyFile + " para importar por imagem. Sem fallback para dados de exemplo.");
        }
        log.info("[IMPORT_IMAGE] GROQ_API_KEY presente: length={} prefix={}", key.length(), key.startsWith("gsk_") ? "gsk_..." : "outro");
        log.info("[IMPORT_IMAGE] Processando imagem: name={} size={} contentType={} mês/ano tela={}/{} quinzena={}", file.getOriginalFilename(), file.getSize(), file.getContentType(), targetMonth, targetYear, quinzena);
        List<TimeEvent> events = new ArrayList<>();
        try {
            byte[] bytes = file.getBytes();
            String contentType = file.getContentType();
            List<LocalDateTime> extractedTimes = extractTimesViaGroq(bytes, contentType, targetMonth, targetYear, quinzena);
            if (extractedTimes == null || extractedTimes.isEmpty()) {
                throw new RuntimeException("A IA não extraiu nenhum horário da imagem. Verifique se a folha de ponto está legível e tente outra foto.");
            }
            LocalDate minDate = extractedTimes.stream().map(LocalDateTime::toLocalDate).min(LocalDate::compareTo).orElse(null);
            LocalDate maxDate = extractedTimes.stream().map(LocalDateTime::toLocalDate).max(LocalDate::compareTo).orElse(null);
            log.info("[IMPORT_IMAGE] IA retornou {} horários, intervalo de datas: {} a {}", extractedTimes.size(), minDate, maxDate);
            // Alternar ENTRY/EXIT por DIA (não global): cada dia começa com ENTRY, assim um dia com horário extra ou ordem errada não desloca os próximos.
            LocalDate prevDate = null;
            int punchIndexInDay = 0;
            for (LocalDateTime eventTime : extractedTimes) {
                LocalDate d = eventTime.toLocalDate();
                if (!d.equals(prevDate)) {
                    prevDate = d;
                    punchIndexInDay = 0;
                }
                TimeEvent event = new TimeEvent();
                event.setEmployeeId(employeeId);
                event.setEventTime(eventTime);
                event.setEventType(punchIndexInDay % 2 == 0 ? TimeEventType.ENTRY : TimeEventType.EXIT);
                event.setStatus(TimeEventStatus.REVIEW);
                events.add(event);
                punchIndexInDay++;
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar imagem: " + e.getMessage(), e);
        }
        return events;
    }

    /** Chama API Groq (visão) — chave via groq.api-key / GROQ_API_KEY; nunca commitar a chave. */
    private List<LocalDateTime> extractTimesViaGroq(byte[] imageBytes, String contentType, int targetMonth, int targetYear, Integer quinzena) throws Exception {
        log.info("[IMPORT_IMAGE] extractTimesViaGroq: imageBytes={} bytes, targetMonth={}, targetYear={}", imageBytes.length, targetMonth, targetYear);
        String mime = contentType != null && contentType.startsWith("image/") ? contentType : "image/jpeg";
        String dataUrl = "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(imageBytes);
        String prompt = "Você analisa uma imagem de folha de ponto, cartão de ponto ou espelho de ponto (qualquer modelo: quinzena, mês completo, cartão físico, planilha, com texto impresso e/ou anotações à mão). "
                + "Extraia TODAS as batidas (horários) agrupadas por DATA.\n\n"
                + "REGRAS GLOBAIS — valem para QUALQUER tipo de ponto; siga sempre o que está na imagem:\n"
                + "(1) MÊS E ANO: Use o mês e o ano indicados na folha (ex.: Janeiro, Fevereiro, 01/2026, Mês/Ano, cabeçalho, etc.).\n"
                + "(2) DIAS: Identifique a coluna ou célula que traz o número do DIA (1 a 31). Pode ser \"Dia\", \"Data\", primeira coluna, etc. "
                + "Use exatamente os dias que aparecem na tabela — não assuma período (pode ser 1–15, 16–31, 1–31 ou outro).\n"
                + "(3) LINHAS: Para cada linha que tiver PELO MENOS UM horário preenchido, inclua esse dia no JSON. "
                + "Inclua a PRIMEIRA e a ÚLTIMA linha da tabela que tiverem dados (não omita o primeiro nem o último dia). Não pare antes do fim da tabela. Linha totalmente em branco = não inclua.\n"
                + "(4) HORÁRIOS: Em cada linha, leia os horários NA ORDEM em que aparecem (da esquerda para a direita). "
                + "Cada dia pode ter 2, 4, 6 ou mais horários conforme o layout (entrada/saída, manhã/tarde, extra, etc.). "
                + "Formato HH:mm em 24h. Célula vazia = não inclua na lista \"times\".\n\n"
                + "Responda APENAS um JSON válido, sem markdown:\n"
                + "{\"days\":[{\"date\":\"YYYY-MM-DD\",\"times\":[\"HH:mm\",\"HH:mm\",...]},...]}\n"
                + "Não invente datas nem horários. Inclua todos os dias que tiverem pelo menos um horário. Lista completa.";
        log.debug("[IMPORT_IMAGE] Prompt enviado à IA (primeiros 500 chars): {}", prompt.length() > 500 ? prompt.substring(0, 500) + "..." : prompt);
        List<String> modelsToTry = new ArrayList<>();
        if (groqModel != null && !groqModel.isBlank()) {
            modelsToTry.add(groqModel.trim());
        }
        if (groqVisionFallbacks != null && !groqVisionFallbacks.isBlank()) {
            for (String s : groqVisionFallbacks.split(",")) {
                String m = s.trim();
                if (!m.isEmpty() && !modelsToTry.contains(m)) modelsToTry.add(m);
            }
        }
        if (modelsToTry.isEmpty()) {
            modelsToTry.add("meta-llama/llama-4-scout-17b-16e-instruct");
            modelsToTry.add("llama-3.2-90b-vision-preview");
        }
        if (groqThinkingModel != null && !groqThinkingModel.isBlank()) {
            String t = groqThinkingModel.trim();
            if (!t.isEmpty() && !modelsToTry.contains(t)) modelsToTry.add(t);
        }
        List<LocalDateTime> result = null;
        Exception lastError = null;
        for (String modelId : modelsToTry) {
            try {
                log.info("[IMPORT_IMAGE] Tentando modelo Groq: {}", modelId);
                result = callGroqVision(dataUrl, prompt, modelId, targetMonth, targetYear);
                if (result != null && !result.isEmpty()) {
                    log.info("[IMPORT_IMAGE] Modelo {} retornou {} horários", modelId, result.size());
                    break;
                }
            } catch (Exception e) {
                log.warn("[IMPORT_IMAGE] Modelo {} falhou: {}", modelId, e.getMessage());
                lastError = e;
            }
        }
        if (result == null || result.isEmpty()) {
            if (lastError != null) throw new RuntimeException("Groq: nenhum modelo retornou dados. Último erro: " + lastError.getMessage(), lastError);
            throw new RuntimeException("Groq: nenhum horário extraído.");
        }
        return result;
    }

    private List<LocalDateTime> callGroqVision(String dataUrl, String prompt, String modelId, int targetMonth, int targetYear) throws Exception {
        var root = objectMapper.createObjectNode();
        root.put("model", modelId);
        root.put("max_tokens", 8192);
        root.put("temperature", 0.2);
        var messages = root.putArray("messages");
        var userMsg = messages.addObject();
        userMsg.put("role", "user");
        var content = userMsg.putArray("content");
        content.addObject().put("type", "text").put("text", prompt);
        content.addObject().put("type", "image_url").putObject("image_url").put("url", dataUrl);
        String body = objectMapper.writeValueAsString(root);
        String key = getEffectiveGroqKey();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                .header("Authorization", "Bearer " + key)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(90))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = HttpClient.newBuilder().build()
                .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new RuntimeException("Groq API " + response.statusCode() + ": " + response.body());
        }
        String responseBody = response.body();
        log.info("[IMPORT_IMAGE] Groq API resposta: status={} bodyLength={}", response.statusCode(), responseBody.length());
        try {
            JsonNode choices = objectMapper.readTree(responseBody).path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode msgContent = choices.get(0).path("message").path("content");
                String contentText = msgContent.isTextual() ? msgContent.asText("") : "";
                if (!msgContent.isTextual() && msgContent.isArray()) {
                    StringBuilder sb = new StringBuilder();
                    for (JsonNode part : msgContent) {
                        if (part.has("text")) sb.append(part.path("text").asText(""));
                    }
                    contentText = sb.toString();
                }
                if (!contentText.isBlank()) {
                    int len = Math.min(1000, contentText.length());
                    log.info("[IMPORT_IMAGE] Resposta da IA (até {} chars): {}", len, contentText.substring(0, len) + (contentText.length() > len ? "..." : ""));
                }
            }
        } catch (Exception e) {
            log.warn("[IMPORT_IMAGE] Não foi possível extrair conteúdo da resposta para log: {}", e.getMessage());
        }
        JsonNode errNode = objectMapper.readTree(responseBody).path("error");
        if (errNode != null && !errNode.isMissingNode()) {
            String errMsg = errNode.path("message").asText(errNode.toString());
            throw new RuntimeException("Groq API erro: " + errMsg);
        }
        LocalDate defaultDate = LocalDate.of(targetYear, targetMonth, 1);
        return parseGroqContent(responseBody, defaultDate);
    }

    private List<LocalDateTime> parseGroqContent(String jsonBody, LocalDate defaultDate) throws Exception {
        JsonNode root = objectMapper.readTree(jsonBody);
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.size() == 0) {
            throw new RuntimeException("Groq API não retornou choices. Resposta: " + (jsonBody.length() > 500 ? jsonBody.substring(0, 500) + "..." : jsonBody));
        }
        JsonNode message = choices.get(0).path("message");
        JsonNode contentNode = message.path("content");
        String text;
        if (contentNode.isTextual()) {
            text = contentNode.asText("").trim();
        } else if (contentNode.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode part : contentNode) {
                if (part.isObject()) {
                    if ("text".equals(part.path("type").asText(null))) {
                        sb.append(part.path("text").asText(""));
                    } else if (part.has("text")) {
                        sb.append(part.get("text").asText(""));
                    }
                }
            }
            text = sb.toString().trim();
        } else {
            text = contentNode.asText("").trim();
        }
        if (text.startsWith("```")) {
            // Tenta extrair apenas o bloco JSON dentro de ``` ``` (evita pegar texto explicativo antes/depois)
            int codeFenceStart = text.indexOf("```json");
            if (codeFenceStart < 0) {
                codeFenceStart = text.indexOf("```");
            }
            int codeFenceEnd = codeFenceStart >= 0 ? text.indexOf("```", codeFenceStart + 3) : -1;
            String inner = text;
            if (codeFenceStart >= 0 && codeFenceEnd > codeFenceStart) {
                inner = text.substring(codeFenceStart + 3, codeFenceEnd);
            }
            int o = inner.indexOf('{');
            int c = inner.lastIndexOf('}');
            if (o >= 0 && c > o) {
                text = inner.substring(o, c + 1);
            } else {
                int a = inner.indexOf('[');
                int b = inner.lastIndexOf(']');
                if (a >= 0 && b > a) {
                    text = inner.substring(a, b + 1);
                }
            }
        }
        List<LocalDateTime> out = new ArrayList<>();
        LocalDate def = defaultDate != null ? defaultDate : LocalDate.now();
        log.info("[IMPORT_IMAGE] parseGroqContent: defaultDate (fallback)={} texto recebido (até 400 chars)={}", def, text.length() > 400 ? text.substring(0, 400) + "..." : text);

        if (text.startsWith("{")) {
            try {
                JsonNode obj = objectMapper.readTree(text);
                JsonNode days = obj.path("days");
                if (days.isArray()) {
                    for (JsonNode dayNode : days) {
                        int day = -1;
                        String ds = dayNode.path("date").asText(null);
                        if (ds != null && !ds.isBlank()) {
                            ds = ds.trim();
                            if (ds.length() >= 10) {
                                try {
                                    String part = ds.substring(0, 10);
                                    if (part.matches("\\d{4}-\\d{2}-\\d{2}")) {
                                        day = LocalDate.parse(part).getDayOfMonth();
                                    } else if (part.matches("\\d{2}/\\d{2}/\\d{4}")) {
                                        String[] d = part.split("/");
                                        day = Integer.parseInt(d[0]);
                                    }
                                } catch (Exception ignored) { }
                            }
                        }
                        if (day < 1 || day > 31) {
                            JsonNode dayNum = dayNode.path("day");
                            if (!dayNum.isMissingNode() && dayNum.isNumber()) {
                                day = dayNum.asInt();
                            }
                        }
                        if (day < 1) day = 1;
                        int maxDay = def.lengthOfMonth();
                        if (day > maxDay) day = maxDay;
                        LocalDate normalized = LocalDate.of(def.getYear(), def.getMonthValue(), day);
                        JsonNode timesArr = dayNode.path("times");
                        if (timesArr.isArray()) {
                            for (JsonNode t : timesArr) {
                                LocalTime lt = parseTimeOnly(t.asText(null));
                                if (lt != null) out.add(LocalDateTime.of(normalized, lt));
                            }
                        }
                    }
                }
            } catch (Exception ignored) { }
        }

        if (out.isEmpty() && text.startsWith("[")) {
            JsonNode arr = objectMapper.readTree(text);
            if (arr.isArray()) {
                for (JsonNode el : arr) {
                    if (el.isTextual()) {
                        LocalTime lt = parseTimeOnly(el.asText());
                        if (lt != null) out.add(LocalDateTime.of(def, lt));
                    } else if (el.isObject()) {
                        String ds = el.path("date").asText(null);
                        int day = def.getDayOfMonth();
                        if (ds != null && ds.length() >= 10) {
                            try {
                                LocalDate parsed = LocalDate.parse(ds.substring(0, 10));
                                day = parsed.getDayOfMonth();
                            } catch (Exception ignored) { }
                        }
                        int maxDay = def.lengthOfMonth();
                        if (day < 1) day = 1;
                        if (day > maxDay) day = maxDay;
                        LocalDate normalized = LocalDate.of(def.getYear(), def.getMonthValue(), day);

                        JsonNode timesArr = el.path("times");
                        if (timesArr.isArray()) {
                            for (JsonNode t : timesArr) {
                                LocalTime lt = parseTimeOnly(t.asText(null));
                                if (lt != null) out.add(LocalDateTime.of(normalized, lt));
                            }
                        } else {
                            String ts = el.path("time").asText(null);
                            if (ts == null) continue;
                            LocalTime lt = parseTimeOnly(ts);
                            if (lt == null) continue;
                            out.add(LocalDateTime.of(normalized, lt));
                        }
                    }
                }
            }
        }
        if (!out.isEmpty()) {
            LocalDate minD = out.stream().map(LocalDateTime::toLocalDate).min(LocalDate::compareTo).orElse(null);
            LocalDate maxD = out.stream().map(LocalDateTime::toLocalDate).max(LocalDate::compareTo).orElse(null);
            log.info("[IMPORT_IMAGE] parseGroqContent: extraídos {} horários, datas {} a {} (defaultDate usado foi {})", out.size(), minD, maxD, def);
            int maxDayNum = maxD != null ? maxD.getDayOfMonth() : 0;
            int daysInMonth = def.lengthOfMonth();
            if (maxDayNum > 0 && maxDayNum < daysInMonth && daysInMonth > 1) {
                log.info("[IMPORT_IMAGE] Último dia extraído: {}. Mês tem {} dias. Se a folha tinha mais dias com dados, reimporte ou verifique truncamento da resposta.", maxDayNum, daysInMonth);
            }
        } else {
            log.warn("[IMPORT_IMAGE] parseGroqContent: nenhum horário extraído do JSON/array. Tentando fallback por regex.");
        }
        if (out.isEmpty()) {
            String haystack = text.isEmpty() ? jsonBody : text;
            Matcher m = Pattern.compile("(\\d{1,2}):(\\d{2})").matcher(haystack);
            List<int[]> timePairs = new ArrayList<>();
            while (m.find()) {
                try {
                    int h = Integer.parseInt(m.group(1));
                    int min = Integer.parseInt(m.group(2));
                    if (h <= 23 && min <= 59) timePairs.add(new int[]{h, min});
                } catch (NumberFormatException ignored) { }
            }
            int size = timePairs.size();
            if (size > 0) {
                int daysInMonth = def.lengthOfMonth();
                int dayCount = (size + 3) / 4;
                String lower = text.toLowerCase();
                boolean secondHalfHint = lower.contains("2ª quinzena") || lower.contains("2a quinzena")
                        || lower.contains("2º quinzena") || lower.contains("2° quinzena")
                        || lower.contains("segunda quinzena");
                int startDay = secondHalfHint ? 16 : 1;
                int endDay = Math.min(daysInMonth, startDay + dayCount - 1);
                log.warn("[IMPORT_IMAGE] parseGroqContent: FALLBACK REGEX usado (JSON não parseou). defaultDate={} timePairs={} dias inferidos={} preenchendo dias {} a {} (secondHalfHint={})", def, size, dayCount, startDay, endDay, secondHalfHint);
                int dayOffset = 0;
                int punchIndex = 0;
                for (int[] tp : timePairs) {
                    int day = startDay + dayOffset;
                    if (day > endDay || day > daysInMonth) break;
                    LocalDate dayDate = LocalDate.of(def.getYear(), def.getMonthValue(), day);
                    out.add(LocalDateTime.of(dayDate, LocalTime.of(tp[0], tp[1])));
                    punchIndex++;
                    if (punchIndex % 4 == 0) dayOffset++;
                }
            }
        }
        if (!out.isEmpty()) {
            LocalDate minD = out.stream().map(LocalDateTime::toLocalDate).min(LocalDate::compareTo).orElse(null);
            LocalDate maxD = out.stream().map(LocalDateTime::toLocalDate).max(LocalDate::compareTo).orElse(null);
            log.info("[IMPORT_IMAGE] parseGroqContent resultado final: {} horários, datas {} a {}", out.size(), minD, maxD);
        }
        return out;
    }

    private static LocalTime parseTimeOnly(String s) {
        if (s == null || s.isBlank()) return null;
        s = s.trim();
        try {
            if (s.length() >= 5) return LocalTime.parse(s.substring(0, 5), DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception ignored) { }
        return null;
    }

    private List<TimeEvent> processPdfFile(UUID employeeId, MultipartFile file) {
        List<TimeEvent> events = new ArrayList<>();
        
        try {
            String content = new String(file.getBytes());
            
            List<LocalDateTime> extractedTimes = extractTimesFromPdf(content);
            
            for (int i = 0; i < extractedTimes.size(); i++) {
                TimeEvent event = new TimeEvent();
                event.setEmployeeId(employeeId);
                event.setEventTime(extractedTimes.get(i));
                
                if (i % 2 == 0) {
                    event.setEventType(TimeEventType.ENTRY);
                } else {
                    event.setEventType(TimeEventType.EXIT);
                }
                
                event.setStatus(TimeEventStatus.REVIEW);
                events.add(event);
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar PDF: " + e.getMessage());
        }
        
        return events;
    }

    private List<TimeEvent> processCsvFile(UUID employeeId, MultipartFile file) {
        List<TimeEvent> events = new ArrayList<>();
        
        try {
            String content = new String(file.getBytes());
            String[] lines = content.split("\n");
            
            for (int i = 1; i < lines.length; i++) {
                String[] columns = lines[i].split(",");
                if (columns.length >= 2) {
                    String dateStr = columns[0].trim();
                    String timeStr = columns[1].trim();
                    
                    LocalDateTime eventTime = parseDateTime(dateStr, timeStr);
                    
                    TimeEvent event = new TimeEvent();
                    event.setEmployeeId(employeeId);
                    event.setEventTime(eventTime);
                    event.setEventType(TimeEventType.ENTRY);
                    event.setStatus(TimeEventStatus.CONFIRMED);
                    events.add(event);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar CSV: " + e.getMessage());
        }
        
        return events;
    }

    private List<LocalDateTime> extractTimesFromImage(String content) {
        List<LocalDateTime> times = new ArrayList<>();

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\d{2}:\\d{2}");
        java.util.regex.Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String timeStr = matcher.group();
            String[] parts = timeStr.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime eventTime = LocalDateTime.of(now.getYear(), now.getMonth(), now.getDayOfMonth(), hour, minute);
            times.add(eventTime);
        }

        return times;
    }

    /** Dados de exemplo da 2ª quinzena (folha de ponto) quando GROQ não está configurada — para a grade preencher. */
    private List<LocalDateTime> getDemoSecondHalfTimes(int targetMonth, int targetYear) {
        List<LocalDateTime> out = new ArrayList<>();
        int y = targetYear;
        int m = targetMonth;
        int[] day16 = {14, 0, 18, 1, 19, 0, 21, 53};
        int[] day17 = {14, 0, 21, 19};
        int[] day19 = {13, 58, 18, 3, 19, 4, 21, 55};
        int[] day20 = {13, 59, 18, 0, 19, 3, 21, 42};
        int[] day21 = {14, 0, 17, 59, 19, 0, 21, 42};
        int[] day22 = {14, 0, 18, 2, 19, 5, 21, 42};
        int[] day23 = {14, 0};
        int[] day25 = {13, 59, 18, 8, 19, 9};
        int[] day26 = {13, 59, 18, 5, 19, 8};
        int[] day27 = {14, 0, 17, 59, 19, 1, 21, 42};
        int[] day28 = {14, 0, 18, 12, 19, 14, 21, 42};
        int[][] days = {day16, day17, day19, day20, day21, day22, day23, day25, day26, day27, day28};
        int[] dayNums = {16, 17, 19, 20, 21, 22, 23, 25, 26, 27, 28};
        for (int i = 0; i < days.length; i++) {
            int[] times = days[i];
            int day = dayNums[i];
            for (int j = 0; j < times.length; j += 2) {
                int h = times[j];
                int min = times[j + 1];
                out.add(LocalDateTime.of(y, m, day, h, min));
            }
        }
        return out;
    }

    private List<LocalDateTime> extractTimesFromPdf(String content) {
        return extractTimesFromImage(content);
    }

    private LocalDateTime parseDateTime(String dateStr, String timeStr) {
        try {
            String[] dateParts = dateStr.split("/");
            int day = Integer.parseInt(dateParts[0]);
            int month = Integer.parseInt(dateParts[1]);
            int year = Integer.parseInt(dateParts[2]);
            
            String[] timeParts = timeStr.split(":");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);
            
            return LocalDateTime.of(year, month, day, hour, minute);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao parsear data/hora: " + dateStr + " " + timeStr);
        }
    }

    private String detectFileType(String filename) {
        if (filename == null) {
            return "UNKNOWN";
        }
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")) {
            return "IMAGE";
        } else if (lower.endsWith(".pdf")) {
            return "PDF";
        } else if (lower.endsWith(".csv")) {
            return "CSV";
        }
        return "UNKNOWN";
    }

    private void verifyEmployeeAccess(UUID userId, UUID companyId, UUID employeeId) {
        userCompanyRepository.findByUserIdAndCompanyId(userId, companyId)
                .orElseThrow(() -> new RuntimeException("Usuário não tem acesso a esta empresa"));

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Funcionário não encontrado"));

        if (!employee.getCompanyId().equals(companyId)) {
            throw new RuntimeException("Funcionário não pertence a esta empresa");
        }
    }

    /** Chave para merge: mesmo (eventTime ao minuto, eventType) = mesmo batimento, não duplicar. */
    private static String mergeKey(LocalDateTime eventTime, TimeEventType eventType) {
        return eventTime.truncatedTo(ChronoUnit.MINUTES).toString() + "|" + eventType.name();
    }

    private TimeEventResponse toResponse(TimeEvent timeEvent) {
        TimeEventResponse response = new TimeEventResponse();
        response.setId(timeEvent.getId());
        response.setEmployeeId(timeEvent.getEmployeeId());
        response.setEventType(timeEvent.getEventType().name());
        response.setEventTime(timeEvent.getEventTime());
        response.setSource(timeEvent.getSource());
        response.setStatus(timeEvent.getStatus().name());
        response.setCreatedAt(timeEvent.getCreatedAt());
        return response;
    }
}
