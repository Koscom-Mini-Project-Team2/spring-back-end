package koscom.mini_project.team2.team2.domain.news.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import koscom.mini_project.team2.team2.domain.etf.entity.Etf;
import koscom.mini_project.team2.team2.domain.news.dto.AnalysisResult;
import koscom.mini_project.team2.team2.domain.news.entity.EtfNews;
import koscom.mini_project.team2.team2.domain.news.repository.EtfNewsRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class NewsAnalysisService {

    @Value("${anthropic.api-key}")
    private String anthropicApiKey;

    private final WebClient webClient;
    private final EtfNewsRepository etfNewsRepository;

    public NewsAnalysisService(WebClient.Builder webClientBuilder,
                               EtfNewsRepository etfNewsRepository) {
        this.webClient = webClientBuilder
                .baseUrl("https://api.anthropic.com")
                .build();
        this.etfNewsRepository = etfNewsRepository;
    }

    /**
     * 수집된 뉴스 분석 및 알림 필요성 판단
     */
    public AnalysisResult analyzeNews(Etf etf, List<EtfNews> newsList) {
        if (newsList.isEmpty()) {
            return AnalysisResult.builder()
                    .shouldAlert(false)
                    .reason("분석할 뉴스가 없습니다.")
                    .build();
        }

        String newsContent = formatNewsForAnalysis(newsList);
        String prompt = createAnalysisPrompt(etf, newsContent);

        try {
            String response = callClaudeAPI(prompt);
            AnalysisResult result = parseAnalysisResult(response);

            // 분석 완료 표시
            newsList.forEach(news -> {
                news.setAnalyzed(true);
                news.setAlertTriggered(result.isShouldAlert());
                etfNewsRepository.save(news);
            });

            return result;

        } catch (Exception e) {
            log.error("뉴스 분석 실패: {}", e.getMessage(), e);
            return AnalysisResult.builder()
                    .shouldAlert(false)
                    .reason("분석 중 오류가 발생했습니다: " + e.getMessage())
                    .build();
        }
    }

    private String createAnalysisPrompt(Etf etf, String newsContent) {
        return String.format("""
            당신은 금융 전문가입니다. 다음 ETF 상품에 대한 최신 뉴스를 분석하여
            투자자에게 즉시 알림을 보내야 할 중요한 변동사항이 있는지 판단해주세요.

            === ETF 정보 ===
            상품명: %s
            카테고리: %s
            위험도: %d (1~9, 1이 가장 안정적)
            설명: %s
            전달 대비 변동률: %d%%

            === 최신 뉴스 ===
            %s

            === 판단 기준 ===
            1. 해당 ETF의 기초자산이나 섹터에 중대한 영향을 미칠 수 있는 뉴스인가?
            2. 단기적으로 가격 변동을 유발할 가능성이 높은가?
            3. 투자자가 즉시 알아야 할 긴급한 정보인가?
            4. ETF의 위험도와 카테고리를 고려했을 때 투자자에게 중요한가?

            === 응답 형식 (반드시 이 형식을 따라주세요) ===
            ALERT: YES 또는 NO
            REASON: 판단 근거를 한 문장으로
            SUMMARY: 핵심 내용 요약 (알림이 필요한 경우만, 2-3문장)
            """,
                etf.getName(),
                etf.getCategory() != null ? etf.getCategory() : "미분류",
                etf.getRiskLevel() != null ? etf.getRiskLevel() : 5,
                etf.getDescription() != null ? etf.getDescription() : "정보 없음",
                etf.getFltRt() != null ? etf.getFltRt() : 0,
                newsContent);
    }

    private String formatNewsForAnalysis(List<EtfNews> newsList) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < newsList.size(); i++) {
            EtfNews news = newsList.get(i);
            sb.append(String.format("%d. [%s] %s\n   %s\n   출처: %s\n\n",
                    i + 1,
                    news.getPublishedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    news.getTitle(),
                    news.getContent(),
                    news.getUrl()));
        }
        return sb.toString();
    }

    private String callClaudeAPI(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "model", "claude-sonnet-4-20250514",
                "max_tokens", 1500,
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                )
        );

        String response = webClient.post()
                .uri("/v1/messages")
                .header("x-api-key", anthropicApiKey)
                .header("anthropic-version", "2023-06-01")
                .header("content-type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        Gson gson = new Gson();
        JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
        JsonArray content = jsonResponse.getAsJsonArray("content");
        return content.get(0).getAsJsonObject().get("text").getAsString();
    }

    private AnalysisResult parseAnalysisResult(String aiResponse) {
        boolean shouldAlert = aiResponse.contains("ALERT: YES");
        String reason = extractField(aiResponse, "REASON:");
        String summary = extractField(aiResponse, "SUMMARY:");

        return AnalysisResult.builder()
                .shouldAlert(shouldAlert)
                .reason(reason)
                .summary(summary)
                .fullAnalysis(aiResponse)
                .build();
    }

    private String extractField(String text, String fieldName) {
        int startIdx = text.indexOf(fieldName);
        if (startIdx == -1) return "";

        startIdx += fieldName.length();
        int endIdx = text.indexOf("\n", startIdx);
        if (endIdx == -1) endIdx = text.length();

        return text.substring(startIdx, endIdx).trim();
    }
}
