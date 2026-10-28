package com.mylog.knowledge.infrastructure;

import com.mylog.analysis.application.KnowledgeRetriever;
import com.mylog.knowledge.application.ReflectiveQuestionGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@ConditionalOnProperty(prefix = "mylog.rag.embedding", name = "provider", havingValue = "gemini")
final class GeminiReflectiveQuestionGenerator implements ReflectiveQuestionGenerator {
    private static final Logger log = LoggerFactory.getLogger(GeminiReflectiveQuestionGenerator.class);
    private static final int MAX_RETRIES = 5;
    private static final int MAX_ATTEMPTS = MAX_RETRIES + 1;
    private static final long INITIAL_RETRY_DELAY_MS = 500;
    private static final long MAX_RETRY_DELAY_MS = 8000;
    private static final String NO_QUESTIONS = "Chưa đủ thông tin để tạo câu hỏi phù hợp.";
    private static final String PROMPT_VERSION = "reflective-questions-v2";
    private static final String SYSTEM_PROMPT = """
            # ROLE
            
            Bạn là một AI đồng hành, đóng vai một người bạn tinh tế, chân thành và biết lắng nghe.
            
            Nhiệm vụ của bạn là chuyển những kiến thức được cung cấp thành các câu hỏi tự vấn nhẹ nhàng, giúp người dùng nhìn lại cảm xúc, suy nghĩ và những điều đang diễn ra trong cuộc sống.
            
            Bạn không phải chuyên gia đang phân tích người dùng, cũng không phải người thầy đưa ra bài học.
            
            Hãy trò chuyện như một người bạn thật sự quan tâm đến người đối diện.
            
            # OBJECTIVE
            
            Dựa trên nội dung được truy xuất từ hệ thống RAG, tạo tối đa 3 câu hỏi mang tính tâm tình, gợi mở sự tự suy ngẫm.
            
            Mỗi câu hỏi nên khiến người dùng cảm thấy:
            - "Ừ nhỉ, mình chưa từng nghĩ về điều đó."
            - "Câu hỏi này giống như đang nói đến mình."
            - "Mình muốn dành chút thời gian để suy nghĩ về chuyện này."
            
            Không cần cố gắng triết lý hay quá sâu sắc. Điều quan trọng là tự nhiên, gần gũi và có ý nghĩa.
            
            # INPUT
            
            Bạn nhận dữ liệu JSON bao gồm:
            - topicCode: Mã chủ đề.
            - excerpts: Các đoạn nội dung được truy xuất từ RAG.
            - citation: Thông tin tham chiếu chỉ sử dụng nội bộ.
            
            # CONTENT UNDERSTANDING
            
            Trước khi tạo câu hỏi:
            
            1. Đọc và hiểu nội dung được cung cấp.
            2. Bỏ qua metadata, thông tin bản quyền, nhà xuất bản và những đoạn không mang giá trị kiến thức.
            3. Xác định các ý chính thực sự được hỗ trợ bởi văn bản.
            4. Tìm những liên hệ tự nhiên giữa các ý đó với trải nghiệm đời thường.
            5. Chỉ sử dụng những ý nghĩa có căn cứ trong dữ liệu đầu vào.
            
            Không tự suy đoán hoàn cảnh, cảm xúc hoặc tình trạng tâm lý của người dùng.
            
            Nếu dữ liệu chỉ chứa tên chủ đề, chỉ tạo câu hỏi khái quát liên quan đến chủ đề đó.
            
            # CONVERSATION STYLE
            
            ## Quy tắc xưng hô bắt buộc
            
            - "Bạn": Dùng để gọi người dùng.
            - "Mình": Dùng khi AI tự xưng.
            - Không dùng "mình" để thay thế cho người dùng.
            - Không dùng "tôi", "em", "anh", "chị" hoặc "quý khách".
            - Không cần đưa cả hai đại từ vào mọi câu hỏi nếu khiến câu văn thiếu tự nhiên.
            
            Ví dụ đúng:
            - "Có bao giờ bạn tự hỏi điều gì khiến bản thân cố gắng nhiều đến vậy không?"
            - "Mình hơi tò mò, gần đây có điều gì khiến bạn cảm thấy vui hơn một chút không?"
            - "Nếu nhìn lại khoảng thời gian vừa qua, điều gì khiến bạn cảm thấy tự hào nhất?"
            - "Dạo này, bạn có dành được chút thời gian cho những điều mình yêu thích không?"
            
            Ví dụ không phù hợp:
            - "Có bao giờ mình cảm thấy mệt mỏi không?" (Dùng sai đại từ chỉ người dùng)
            - "Bạn cần làm gì để cải thiện bản thân?"
            - "Tại sao bạn không quan tâm đến cảm xúc của mình?"
            - "Bạn có nhận thức được vấn đề của bản thân không?"
            
            # QUESTION GENERATION
            
            Hãy tưởng tượng bạn và người dùng đang ngồi trò chuyện trong một buổi tối yên tĩnh.
            
            Bạn không cố khai thác thông tin, không yêu cầu người dùng thay đổi và cũng không tìm cách dạy họ điều gì.
            
            Bạn chỉ nhẹ nhàng gợi ra một suy nghĩ, để người dùng tự khám phá câu trả lời.
            
            Khi đặt câu hỏi:
            
            - Hỏi như một người bạn đang thật sự tò mò và quan tâm.
            - Ưu tiên câu hỏi mở, không có đáp án đúng hoặc sai.
            - Mỗi câu hỏi chỉ gợi mở một suy nghĩ chính.
            - Có thể liên hệ với những tình huống, cảm xúc hoặc lựa chọn quen thuộc khi nội dung cho phép.
            - Không ép người dùng thừa nhận một vấn đề.
            - Không mặc định người dùng đang buồn, cô đơn, căng thẳng hoặc gặp khó khăn.
            - Không đưa lời khuyên trá hình dưới dạng câu hỏi.
            - Không cố tạo cảm giác sâu sắc bằng những câu văn phức tạp.
            - Khuyến khích sự tự khám phá thay vì hướng người dùng đến một câu trả lời có sẵn.
            
            # TONE & PERSONALITY
            
            Giọng văn:
            - Ấm áp, chân thành, nhẹ nhàng.
            - Gần gũi như một người bạn.
            - Có chút tò mò và quan tâm nhưng không xâm phạm đời tư.
            - Tự nhiên, đời thường, không sáo rỗng.
            - Không mang cảm giác đang thực hiện bài kiểm tra tâm lý.
            
            Có thể sử dụng những cách mở đầu:
            
            - "Có bao giờ bạn..."
            - "Không biết bạn có từng..."
            - "Dạo gần đây, bạn có..."
            - "Mình tò mò một chút, liệu bạn..."
            - "Nếu nhìn lại một chút, bạn thấy..."
            - "Điều gì khiến bạn..."
            - "Bạn còn nhớ lần gần nhất..."
            
            Không lặp lại cùng một kiểu mở đầu cho tất cả câu hỏi.
            
            Hãy linh hoạt để mỗi câu hỏi giống một lời tâm sự thật sự.
            
            # EMOTIONAL SAFETY
            
            - Không phán xét hoặc quy kết cảm xúc.
            - Không thúc ép người dùng chia sẻ chuyện riêng tư.
            - Không tạo cảm giác tội lỗi hoặc thiếu sót.
            - Không giả định rằng người dùng cần được chữa lành.
            - Không cố tình khơi gợi ký ức đau buồn.
            - Tôn trọng quyền không trả lời của người dùng.
            
            # GROUNDING RULES
            
            Đây là yêu cầu bắt buộc:
            
            - Mọi câu hỏi phải liên hệ rõ ràng với ít nhất một ý nghĩa được hỗ trợ bởi input.
            - Không tự bổ sung lý thuyết hoặc kết luận không có trong dữ liệu.
            - Không đánh đồng tiêu đề tài liệu với những nội dung nghiên cứu đã được chứng minh.
            - Không tạo câu hỏi chỉ vì chúng nghe hay hoặc sâu sắc.
            - Nếu một ý không có đủ căn cứ, hãy bỏ qua.
            
            # OUTPUT FORMAT
            
            - Trả về tối đa 3 câu hỏi bằng tiếng Việt.
            - Mỗi câu hỏi khoảng 15–30 từ.
            - Mỗi câu chỉ tập trung vào một ý chính.
            - Các câu hỏi nên khác nhau về góc nhìn.
            - Sử dụng "bạn" cho người dùng và "mình" cho AI khi cần xưng hô.
            - Không đề cập tài liệu, tác giả, nguồn nghiên cứu hoặc citation.
            - Không giải thích, phân tích hoặc đưa lời khuyên.
            - Không có lời dẫn hoặc lời kết.
            - Không dùng emoji.
            - Đánh số 1, 2, 3.
            - Nếu dữ liệu không đủ để tạo câu hỏi có căn cứ, trả về: "Chưa đủ thông tin để tạo câu hỏi phù hợp."
            
            # FINAL CHECK
            
            Trước khi trả lời, hãy tự đánh giá:
            
            1. Câu hỏi có giống một người bạn thật sự sẽ hỏi không?
            2. Người dùng có cảm thấy thoải mái khi nghe không?
            3. Câu hỏi có tạo không gian để người dùng tự suy nghĩ không?
            4. Có vô tình áp đặt cảm xúc hoặc nhận định nào không?
            5. Nội dung câu hỏi có được hỗ trợ bởi dữ liệu RAG không?
            6. Đại từ "bạn" và "mình" có được sử dụng đúng vai trò không?
            
            Nếu câu hỏi quá học thuật, hãy viết lại theo cách đời thường.
            
            Nếu câu hỏi quá chung chung, hãy làm cụ thể hơn mà không bịa thêm bối cảnh.
            
            Nếu câu hỏi quá nặng nề, hãy diễn đạt nhẹ nhàng hơn.
            
            Nếu câu hỏi thiếu căn cứ, hãy loại bỏ.
            
            # CORE PRINCIPLE
            
            "Hãy hỏi như một người bạn đang quan tâm, chứ không phải một người đang cố tìm ra vấn đề của người khác."
            
            Ưu tiên:
            Tính chính xác > Sự tự nhiên > Cảm giác được thấu hiểu > Chiều sâu suy ngẫm > Số lượng câu hỏi.
            """;
    private static final Pattern NUMBERED_QUESTION = Pattern.compile("^([1-3])[.)]\\s+(.+)$");

    private final URI endpoint;
    private final String model;
    private final String apiKey;
    private final Duration timeout;
    private final ObjectMapper mapper;
    private final HttpClient client;
    private final boolean providerDataApproved;

    @Autowired
    GeminiReflectiveQuestionGenerator(
            @Value("${mylog.rag.embedding.endpoint}") String endpoint,
            @Value("${mylog.rag.generation.model:gemini-3.8-flash}") String model,
            @Value("${mylog.rag.generation.api-key:}") String apiKey,
            @Value("${mylog.rag.embedding.timeout-ms:30000}") int timeoutMs,
            @Value("${mylog.rag.embedding.provider-data-approved:false}") boolean providerDataApproved,
            ObjectMapper mapper) {
        this(endpoint, model, apiKey, timeoutMs, providerDataApproved, mapper, HttpClient.newHttpClient());
    }

    GeminiReflectiveQuestionGenerator(String endpoint, String model, String apiKey, int timeoutMs,
                                      boolean providerDataApproved, ObjectMapper mapper, HttpClient client) {
        URI base = URI.create(endpoint);
        boolean localHttp = "http".equalsIgnoreCase(base.getScheme())
                && ("localhost".equalsIgnoreCase(base.getHost()) || "127.0.0.1".equals(base.getHost()));
        if (!("https".equalsIgnoreCase(base.getScheme()) || localHttp) || base.getHost() == null
                || base.getUserInfo() != null || base.getQuery() != null || base.getFragment() != null
                || model == null || !model.matches("[A-Za-z0-9._-]{1,120}") || timeoutMs < 1000 || timeoutMs > 120000)
            throw new IllegalArgumentException("Invalid reflective question provider configuration");
        String root = base.toString().replaceAll("/+$", "");
        this.endpoint = URI.create(root + "/models/" + model + ":generateContent");
        this.model = model;
        this.apiKey = apiKey;
        this.timeout = Duration.ofMillis(timeoutMs);
        this.mapper = mapper;
        this.client = client;
        this.providerDataApproved = providerDataApproved;
    }

    @Override
    public List<String> generate(String topicCode, List<KnowledgeRetriever.Passage> excerpts) {
        if (excerpts.isEmpty()) return List.of();
        if (apiKey == null || apiKey.isBlank() || !providerDataApproved) {
            log.error("RAG question generation is not configured: apiKeyConfigured={}, providerDataApproved={}",
                    apiKey != null && !apiKey.isBlank(), providerDataApproved);
            throw new IllegalStateException("Gemini RAG generation is not configured or approved");
        }
        try {
            Map<String, Object> input = new LinkedHashMap<>();
            input.put("topicCode", topicCode);
            input.put("excerpts", excerpts.stream().map(GeminiReflectiveQuestionGenerator::excerptInput).toList());
            Map<String, Object> requestBody = Map.of(
                    "systemInstruction", Map.of("parts", List.of(Map.of("text", SYSTEM_PROMPT))),
                    "contents", List.of(Map.of("role", "user", "parts",
                            List.of(Map.of("text", mapper.writeValueAsString(input))))),
                    "generationConfig", Map.of("candidateCount", 1, "temperature", 0.2, "maxOutputTokens", 512));
            HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(requestBody))).build();
            HttpResponse<String> response = sendWithRetries(request);
            if (response.statusCode() != 200) {
                log.warn("RAG question generation provider returned non-success: model={}, status={}",
                        model, response.statusCode());
                throw new IllegalStateException("Gemini generation provider unavailable");
            }
            if (response.body().length() > 16384) {
                log.warn("RAG question generation provider response exceeded size limit: model={}", model);
                throw new IllegalStateException("Gemini generation provider unavailable");
            }
            JsonNode envelope = mapper.readTree(response.body());
            JsonNode candidates = envelope.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                log.warn("RAG question generation provider returned no candidate: model={}", model);
                throw new IllegalStateException("Gemini generation returned no complete candidate");
            }
            String finishReason = candidates.get(0).path("finishReason").asText();
            if (!"STOP".equals(finishReason)) {
                log.warn("RAG question generation did not finish normally: model={}, finishReason={}", model,
                        finishReason);
                throw new IllegalStateException("Gemini generation returned no complete candidate");
            }
            JsonNode parts = candidates.get(0).path("content").path("parts");
            if (!parts.isArray()) throw new IllegalStateException("Gemini generation returned no text");
            StringBuilder output = new StringBuilder();
            for (JsonNode part : parts) {
                JsonNode text = part.get("text");
                if (text != null && text.isTextual()) output.append(text.asText());
            }
            if (output.isEmpty()) throw new IllegalStateException("Gemini generation returned no text");
            return parseQuestions(output.toString());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("RAG question generation was interrupted: model={}", model);
            throw new IllegalStateException("Gemini generation provider unavailable");
        } catch (Exception exception) {
            log.warn("RAG question generation failed: model={}, failureType={}", model,
                    exception.getClass().getSimpleName());
            throw new IllegalStateException("Gemini generation provider unavailable");
        }
    }

    private HttpResponse<String> sendWithRetries(HttpRequest request) throws IOException, InterruptedException {
        for (int retry = 0; ; retry++) {
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();
                if (!isRetryableStatus(status) || retry == MAX_RETRIES) return response;
                log.warn("RAG question generation received a transient provider status; retrying: "
                                + "model={}, status={}, attempt={}/{}, nextAttempt={}/{}",
                        model, status, retry + 1, MAX_ATTEMPTS, retry + 2, MAX_ATTEMPTS);
            } catch (IOException exception) {
                if (retry == MAX_RETRIES) throw exception;
                log.warn("RAG question generation transport failure; retrying: "
                                + "model={}, failureType={}, attempt={}/{}, nextAttempt={}/{}",
                        model, exception.getClass().getSimpleName(), retry + 1, MAX_ATTEMPTS,
                        retry + 2, MAX_ATTEMPTS);
            }
            Thread.sleep(retryDelayMillis(retry));
        }
    }

    private static boolean isRetryableStatus(int status) {
        return status == 429 || status >= 500;
    }

    private static long retryDelayMillis(int retry) {
        return Math.min(MAX_RETRY_DELAY_MS, INITIAL_RETRY_DELAY_MS * (1L << retry));
    }

    private static Map<String, Object> excerptInput(KnowledgeRetriever.Passage passage) {
        Map<String, Object> citation = new LinkedHashMap<>();
        citation.put("itemId", passage.itemId());
        citation.put("versionId", passage.versionId());
        citation.put("version", passage.version());
        citation.put("chunkId", passage.chunkId());
        citation.put("sourceName", passage.sourceName());
        citation.put("sourceUrl", passage.sourceUrl());
        return Map.of("text", passage.text(), "citation", citation);
    }

    private static List<String> parseQuestions(String output) {
        String normalized = output.strip();
        if (NO_QUESTIONS.equals(normalized)) return List.of();
        if (normalized.startsWith("```") || normalized.length() > 2048)
            throw new IllegalStateException("Invalid reflective question output");
        List<String> questions = new ArrayList<>();
        for (String line : normalized.split("\\R")) {
            if (line.isBlank()) continue;
            Matcher matcher = NUMBERED_QUESTION.matcher(line.strip());
            if (!matcher.matches() || Integer.parseInt(matcher.group(1)) != questions.size() + 1)
                throw new IllegalStateException("Invalid reflective question output");
            String question = matcher.group(2).strip();
            if (!question.endsWith("?") || question.split("\\s+").length > 35 || question.length() > 300)
                throw new IllegalStateException("Invalid reflective question output");
            questions.add(question);
            if (questions.size() > 3) throw new IllegalStateException("Invalid reflective question output");
        }
        return List.copyOf(questions);
    }
}
