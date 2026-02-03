package koscom.mini_project.team2.team2.domain.tutor.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import koscom.mini_project.team2.team2.domain.tutor.dto.TutorAnswerResponse;
import koscom.mini_project.team2.team2.domain.tutor.entity.TutorHistory;
import koscom.mini_project.team2.team2.domain.tutor.repository.TutorHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Service
@Slf4j
public class EtfTutorService {

    private final TutorHistoryRepository tutorHistoryRepository;
    private final WebClient webClient;

    @Value("${anthropic.api-key}")
    private String anthropicApiKey;

    private static final String MODEL = "claude-sonnet-4-20250514";
    private static final int MAX_TOKENS = 2000;

    private String promptTemplate;

    // RebalancingService와 동일한 패턴으로 생성자 구성
    public EtfTutorService(
            TutorHistoryRepository tutorHistoryRepository,
            WebClient.Builder webClientBuilder) {
        this.tutorHistoryRepository = tutorHistoryRepository;
        this.webClient = webClientBuilder
                .baseUrl("https://api.anthropic.com")
                .build();
    }

    /**
     * ETF 관련 질문에 대한 AI 튜터 답변 생성
     */
    public TutorAnswerResponse generateAnswer(String question, Long memberId) {
        log.info("ETF 튜터 질문 처리 시작 - 질문: {}", question);

        try {
            // 1. 프롬프트 템플릿 로드 및 구성
            String fullPrompt = buildPrompt(question);

            // 2. Claude API 호출
            String answer = callClaudeApi(fullPrompt);

            // 3. 이력 저장
            saveTutorHistory(memberId, question, answer);

            // 4. 응답 생성
            return TutorAnswerResponse.builder()
                    .question(question)
                    .answer(answer)
                    .timestamp(LocalDateTime.now())
                    .model(MODEL)
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("ETF 튜터 답변 생성 실패", e);

            return TutorAnswerResponse.builder()
                    .question(question)
                    .answer("죄송합니다. 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.")
                    .timestamp(LocalDateTime.now())
                    .model(MODEL)
                    .success(false)
                    .build();
        }
    }

    /**
     * 프롬프트 템플릿 로드 및 질문 삽입
     */
    private String buildPrompt(String question) {
        if (promptTemplate == null) {
            try {
                ClassPathResource resource = new ClassPathResource("prompts/etf-tutor-prompt.txt");
                promptTemplate = new String(
                        resource.getInputStream().readAllBytes(),
                        StandardCharsets.UTF_8
                );
                log.info("프롬프트 템플릿 로드 완료");
            } catch (IOException e) {
                log.error("프롬프트 템플릿 로드 실패", e);
                throw new RuntimeException("프롬프트 템플릿을 불러올 수 없습니다.", e);
            }
        }

        return promptTemplate.replace("{user_question}", question);
    }

    /**
     * Claude API 호출 - RebalancingService와 유사한 패턴
     */
    private String callClaudeApi(String prompt) {
        log.info("Claude API 호출 시작");

        // RebalancingService의 callClaudeAPI 메서드와 동일한 방식
        Gson gson = new Gson();

        // 요청 바디 구성
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", MODEL);
        requestBody.addProperty("max_tokens", MAX_TOKENS);

        JsonArray messages = new JsonArray();
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);
        messages.add(message);

        requestBody.add("messages", messages);

        // API 호출
        String response = webClient.post()
                .uri("/v1/messages")
                .header("x-api-key", anthropicApiKey)
                .header("anthropic-version", "2023-06-01")
                .header("content-type", "application/json")
                .bodyValue(gson.toJson(requestBody))
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (response == null) {
            throw new RuntimeException("Claude API 응답이 없습니다.");
        }

        // 응답 파싱
        JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
        JsonArray content = jsonResponse.getAsJsonArray("content");

        if (content != null && content.size() > 0) {
            String answer = content.get(0).getAsJsonObject().get("text").getAsString();
            log.info("Claude API 응답 성공");
            return answer;
        }

        throw new RuntimeException("Claude API 응답 파싱 실패");
    }

    /**
     * 튜터 이용 이력 저장
     */
    private void saveTutorHistory(Long memberId, String question, String answer) {
        try {
            TutorHistory history = TutorHistory.builder()
                    .memberId(memberId)
                    .question(question)
                    .answer(answer)
                    .modelUsed(MODEL)
                    .build();

            tutorHistoryRepository.save(history);
            log.info("튜터 이력 저장 완료");
        } catch (Exception e) {
            log.warn("튜터 이력 저장 실패 (계속 진행)", e);
        }
    }
}