package koscom.mini_project.team2.team2.domain.news.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import koscom.mini_project.team2.team2.domain.etf.entity.Etf;
import koscom.mini_project.team2.team2.domain.etf.repository.EtfRepository;
import koscom.mini_project.team2.team2.domain.news.dto.AnalysisResult;
import koscom.mini_project.team2.team2.domain.news.entity.EtfNews;
import koscom.mini_project.team2.team2.domain.news.service.NewsAnalysisService;
import koscom.mini_project.team2.team2.domain.news.service.NewsCollectorService;
import koscom.mini_project.team2.team2.domain.news.dto.*;
import koscom.mini_project.team2.team2.domain.news.entity.Portfolio;
import koscom.mini_project.team2.team2.domain.news.entity.PortfolioAllocation;
import koscom.mini_project.team2.team2.domain.news.entity.RebalancingHistory;
import koscom.mini_project.team2.team2.domain.news.repository.PortfolioAllocationRepository;
import koscom.mini_project.team2.team2.domain.news.repository.PortfolioRepository;
import koscom.mini_project.team2.team2.domain.news.repository.RebalancingHistoryRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RebalancingService {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioAllocationRepository allocationRepository;
    private final EtfRepository etfRepository;
    private final NewsCollectorService newsCollector;
    private final NewsAnalysisService newsAnalysis;
    private final RebalancingHistoryRepository historyRepository;

    @Value("${anthropic.api-key}")
    private String anthropicApiKey;

    private final WebClient webClient;

    public RebalancingService(PortfolioRepository portfolioRepository,
                              PortfolioAllocationRepository allocationRepository,
                              EtfRepository etfRepository,
                              NewsCollectorService newsCollector,
                              NewsAnalysisService newsAnalysis,
                              RebalancingHistoryRepository historyRepository,
                              WebClient.Builder webClientBuilder) {
        this.portfolioRepository = portfolioRepository;
        this.allocationRepository = allocationRepository;
        this.etfRepository = etfRepository;
        this.newsCollector = newsCollector;
        this.newsAnalysis = newsAnalysis;
        this.historyRepository = historyRepository;
        this.webClient = webClientBuilder
                .baseUrl("https://api.anthropic.com")
                .build();
    }

    /**
     * 포트폴리오 리밸런싱 분석
     */
    public RebalancingResponse analyzeAndRecommendRebalancing(Long portfolioId, Long memberId) {
        log.info("포트폴리오 리밸런싱 분석 시작: portfolioId={}", portfolioId);

        // 1. 포트폴리오 조회
        Portfolio portfolio = portfolioRepository.findByIdAndMemberId(portfolioId, memberId)
                .orElseThrow(() -> new RuntimeException("포트폴리오를 찾을 수 없습니다."));

        // 2. 포트폴리오 구성 조회
        List<PortfolioAllocation> allocations = allocationRepository.findByPortfolioId(portfolioId);

        if (allocations.isEmpty()) {
            throw new RuntimeException("포트폴리오 구성이 비어있습니다.");
        }

        // 3. 각 ETF의 뉴스 수집 및 분석
        List<EtfNewsAnalysis> etfAnalyses = new ArrayList<>();

        for (PortfolioAllocation allocation : allocations) {
            Etf etf = etfRepository.findById(allocation.getEtfId())
                    .orElseThrow(() -> new RuntimeException("ETF를 찾을 수 없습니다: " + allocation.getEtfId()));

            log.info("ETF 뉴스 수집: {}", etf.getName());
            List<EtfNews> newsList = newsCollector.collectAndSaveNews(etf);

            if (!newsList.isEmpty()) {
                AnalysisResult analysis = newsAnalysis.analyzeNews(etf, newsList);

                etfAnalyses.add(EtfNewsAnalysis.builder()
                        .etf(etf)
                        .allocation(allocation)
                        .newsList(newsList)
                        .analysis(analysis)
                        .build());
            }
        }

        // 4. AI에게 포트폴리오 전체 리밸런싱 추천 요청
        RebalancingRecommendation recommendation = requestRebalancingRecommendation(
                portfolio, allocations, etfAnalyses
        );

        // 5. 응답 생성
        return buildRebalancingResponse(portfolio, allocations, etfAnalyses, recommendation);
    }

    /**
     * AI에게 리밸런싱 추천 요청
     */
    private RebalancingRecommendation requestRebalancingRecommendation(
            Portfolio portfolio,
            List<PortfolioAllocation> allocations,
            List<EtfNewsAnalysis> etfAnalyses) {

        String prompt = createRebalancingPrompt(portfolio, allocations, etfAnalyses);

        try {
            String response = callClaudeAPI(prompt);
            return parseRebalancingRecommendation(response, allocations);
        } catch (Exception e) {
            log.error("리밸런싱 추천 요청 실패: {}", e.getMessage(), e);
            return RebalancingRecommendation.builder()
                    .rebalancingRequired(false)
                    .reason("분석 중 오류가 발생했습니다.")
                    .riskLevel("UNKNOWN")
                    .recommendedWeights(new HashMap<>())
                    .advice("")
                    .fullAnalysis("")
                    .build();
        }
    }

    /**
     * 리밸런싱 프롬프트 생성
     */
    private String createRebalancingPrompt(
            Portfolio portfolio,
            List<PortfolioAllocation> allocations,
            List<EtfNewsAnalysis> etfAnalyses) {

        StringBuilder prompt = new StringBuilder();

        prompt.append("당신은 전문 자산운용 포트폴리오 매니저입니다. ");
        prompt.append("다음 포트폴리오의 최신 뉴스를 분석하여 리밸런싱이 필요한지 판단하고, ");
        prompt.append("필요하다면 구체적인 비중 조정을 추천해주세요.\n\n");

        prompt.append("=== 현재 포트폴리오 구성 ===\n");
        for (PortfolioAllocation allocation : allocations) {
            Etf etf = etfRepository.findById(allocation.getEtfId()).orElse(null);
            if (etf != null) {
                prompt.append(String.format("- %s: %.0f%% (위험도: %d, 카테고리: %s)\n",
                        etf.getName(),
                        allocation.getTargetWeight(),
                        etf.getRiskLevel() != null ? etf.getRiskLevel() : 5,
                        etf.getCategory() != null ? etf.getCategory() : "미분류"));
            }
        }

        prompt.append("\n=== 각 ETF의 최신 뉴스 및 분석 ===\n");
        for (EtfNewsAnalysis analysis : etfAnalyses) {
            prompt.append(String.format("\n【%s】\n", analysis.getEtf().getName()));
            prompt.append(String.format("현재 비중: %.0f%%\n", analysis.getAllocation().getTargetWeight()));

            if (analysis.getAnalysis() != null && analysis.getAnalysis().getReason() != null) {
                prompt.append(String.format("AI 분석: %s\n", analysis.getAnalysis().getReason()));
            }

            prompt.append("주요 뉴스:\n");
            int newsCount = Math.min(3, analysis.getNewsList().size());
            for (int i = 0; i < newsCount; i++) {
                EtfNews news = analysis.getNewsList().get(i);
                prompt.append(String.format("  - %s (%s)\n",
                        news.getTitle(),
                        news.getPublishedAt().format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))));
            }
        }

        prompt.append("\n=== 분석 요청 사항 ===\n");
        prompt.append("1. 위 뉴스들을 종합하여 포트폴리오 리밸런싱이 필요한지 판단해주세요.\n");
        prompt.append("2. 리밸런싱이 필요하다면, 각 ETF의 추천 비중(%)을 제시해주세요.\n");
        prompt.append("3. 리밸런싱 근거를 명확히 설명해주세요.\n");
        prompt.append("4. 투자자가 알아야 할 위험 요소를 알려주세요.\n\n");

        prompt.append("=== 응답 형식 (반드시 이 형식을 따라주세요) ===\n");
        prompt.append("REBALANCING: YES 또는 NO\n");
        prompt.append("REASON: 리밸런싱 판단 근거를 2-3문장으로\n");
        prompt.append("RISK_LEVEL: LOW, MEDIUM, HIGH 중 하나\n");
        prompt.append("RECOMMENDATIONS:\n");

        for (PortfolioAllocation allocation : allocations) {
            Etf etf = etfRepository.findById(allocation.getEtfId()).orElse(null);
            if (etf != null) {
                prompt.append(String.format("- %s: %.0f -> [추천비중]%% (이유)\n",
                        etf.getName(),
                        allocation.getTargetWeight()));
            }
        }

        prompt.append("\nADVICE: 투자자를 위한 조언 (2-3문장)\n");

        return prompt.toString();
    }

    private String callClaudeAPI(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "model", "claude-sonnet-4-20250514",
                "max_tokens", 2000,
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

    /**
     * AI 응답 파싱
     */
    private RebalancingRecommendation parseRebalancingRecommendation(
            String aiResponse,
            List<PortfolioAllocation> currentAllocations) {

        boolean required = aiResponse.contains("REBALANCING: YES");
        String reason = extractField(aiResponse, "REASON:");
        String riskLevel = extractField(aiResponse, "RISK_LEVEL:");
        String advice = extractField(aiResponse, "ADVICE:");

        Map<Long, Double> recommendedWeights = parseRecommendedWeights(aiResponse, currentAllocations);

        return RebalancingRecommendation.builder()
                .rebalancingRequired(required)
                .reason(reason)
                .riskLevel(riskLevel)
                .recommendedWeights(recommendedWeights)
                .advice(advice)
                .fullAnalysis(aiResponse)
                .build();
    }

    private Map<Long, Double> parseRecommendedWeights(String aiResponse, List<PortfolioAllocation> allocations) {
        Map<Long, Double> weights = new HashMap<>();

        for (PortfolioAllocation allocation : allocations) {
            Etf etf = etfRepository.findById(allocation.getEtfId()).orElse(null);
            if (etf == null) continue;

            String etfName = etf.getName();
            // "KODEX 200: 45 -> 40%" 형식 파싱
            Pattern pattern = Pattern.compile(Pattern.quote(etfName) + ".*?->\\s*(\\d+)%");
            Matcher matcher = pattern.matcher(aiResponse);

            if (matcher.find()) {
                try {
                    double weight = Double.parseDouble(matcher.group(1));
                    weights.put(allocation.getEtfId(), weight);
                } catch (Exception e) {
                    log.warn("비중 파싱 실패: {}", etfName);
                    weights.put(allocation.getEtfId(), allocation.getTargetWeight());
                }
            } else {
                weights.put(allocation.getEtfId(), allocation.getTargetWeight());
            }
        }

        return weights;
    }

    private String extractField(String text, String fieldName) {
        int startIdx = text.indexOf(fieldName);
        if (startIdx == -1) return "";

        startIdx += fieldName.length();
        int endIdx = text.indexOf("\n", startIdx);
        if (endIdx == -1) endIdx = text.length();

        return text.substring(startIdx, endIdx).trim();
    }

    /**
     * 응답 생성
     */
    private RebalancingResponse buildRebalancingResponse(
            Portfolio portfolio,
            List<PortfolioAllocation> allocations,
            List<EtfNewsAnalysis> etfAnalyses,
            RebalancingRecommendation recommendation) {

        // 현재 포트폴리오
        List<AllocationDto> currentPortfolio = new ArrayList<>();
        List<AllocationChangeDto> recommendedPortfolio = new ArrayList<>();

        for (PortfolioAllocation allocation : allocations) {
            Etf etf = etfRepository.findById(allocation.getEtfId()).orElse(null);
            if (etf == null) continue;

            double currentWeight = allocation.getTargetWeight();
            double recommendedWeight = recommendation.getRecommendedWeights()
                    .getOrDefault(allocation.getEtfId(), currentWeight);
            double change = recommendedWeight - currentWeight;

            currentPortfolio.add(AllocationDto.builder()
                    .etfId(etf.getId())
                    .etfName(etf.getName())
                    .category(etf.getCategory())
                    .currentWeight(currentWeight)
                    .build());

            if (recommendation.isRebalancingRequired()) {
                // ETF별 뉴스 분석 결과 찾기
                String changeReason = generateChangeReason(etf, change, etfAnalyses, recommendation);

                recommendedPortfolio.add(AllocationChangeDto.builder()
                        .etfId(etf.getId())
                        .etfName(etf.getName())
                        .category(etf.getCategory())
                        .currentWeight(currentWeight)
                        .recommendedWeight(recommendedWeight)
                        .changeAmount(change)
                        .changeReason(changeReason)
                        .build());
            }
        }

        // 뉴스 근거
        List<NewsEvidenceDto> newsEvidence = new ArrayList<>();

        log.info(">>> newsEvidence 생성 시작: etfAnalyses 크기 = {}", etfAnalyses.size());

        for (EtfNewsAnalysis analysis : etfAnalyses) {
            log.info(">>> ETF: {}, 뉴스 개수: {}",
                    analysis.getEtf().getName(),
                    analysis.getNewsList().size());

            // 각 ETF의 최대 3개 뉴스만 추가
            int newsCount = Math.min(3, analysis.getNewsList().size());

            for (int i = 0; i < newsCount; i++) {
                EtfNews news = analysis.getNewsList().get(i);

                // impact 결정
                String impact = "NEUTRAL";
                if (analysis.getAnalysis() != null && analysis.getAnalysis().isShouldAlert()) {
                    impact = "NEGATIVE";
                }

                // summary 결정
                String summary = "";
                if (analysis.getAnalysis() != null && analysis.getAnalysis().getReason() != null) {
                    summary = analysis.getAnalysis().getReason();
                } else {
                    summary = news.getContent() != null ? news.getContent() : "관련 뉴스";
                }

                NewsEvidenceDto evidence = NewsEvidenceDto.builder()
                        .etfId(analysis.getEtf().getId())
                        .etfName(analysis.getEtf().getName())
                        .newsTitle(news.getTitle())
                        .newsUrl(news.getUrl())
                        .publishedAt(news.getPublishedAt())
                        .impact(impact)
                        .summary(summary)
                        .build();

                newsEvidence.add(evidence);

                log.info(">>> 뉴스 추가: {}", news.getTitle());
            }
        }

        log.info(">>> newsEvidence 생성 완료: 총 {}건", newsEvidence.size());

        // 조언 문장 분리
        List<String> recommendations = new ArrayList<>();
        if (recommendation.getAdvice() != null && !recommendation.getAdvice().isEmpty()) {
            String[] sentences = recommendation.getAdvice().split("\\. ");
            for (String sentence : sentences) {
                if (!sentence.trim().isEmpty()) {
                    recommendations.add(sentence.trim().replaceAll("\\.$", ""));
                }
            }
        }

        return RebalancingResponse.builder()
                .portfolioId(portfolio.getId())
                .portfolioName(portfolio.getPortfolioName())
                .rebalancingRequired(recommendation.isRebalancingRequired())
                .rebalancingReason(recommendation.getReason())
                .currentPortfolio(currentPortfolio)
                .recommendedPortfolio(recommendedPortfolio)
                .newsEvidence(newsEvidence)
                .riskAssessment(recommendation.getRiskLevel())
                .recommendations(recommendations)
                .analyzedAt(java.time.LocalDateTime.now())
                .build();
    }

    /**
     * ETF별 비중 변경 이유 생성
     */
    private String generateChangeReason(
            Etf etf,
            double change,
            List<EtfNewsAnalysis> etfAnalyses,
            RebalancingRecommendation recommendation) {

        // 해당 ETF의 뉴스 분석 찾기
        EtfNewsAnalysis etfAnalysis = etfAnalyses.stream()
                .filter(analysis -> analysis.getEtf().getId().equals(etf.getId()))
                .findFirst()
                .orElse(null);

        String etfName = etf.getName();
        String action = "";
        String reason = "";

        // 변동 크기 판단
        if (Math.abs(change) < 1.0) {
            return String.format("%s는 현재 시장 상황에서 적절한 비중을 유지하고 있어 조정이 필요하지 않아", etfName);
        }

        // 증가/감소 판단
        if (change > 0) {
            action = "늘렸어";
        } else {
            action = "줄였어";
        }

        // 이유 생성
        if (etfAnalysis != null && etfAnalysis.getAnalysis() != null) {
            String analysisReason = etfAnalysis.getAnalysis().getReason();

            if (change > 0) {
                // 비중 증가 - 긍정적 이유
                if (analysisReason != null && !analysisReason.isEmpty()) {
                    if (analysisReason.contains("상승") || analysisReason.contains("호재") ||
                            analysisReason.contains("긍정") || analysisReason.contains("성장")) {
                        reason = "긍정적인 시장 흐름과 성장 가능성";
                    } else if (analysisReason.contains("안정") || analysisReason.contains("유지")) {
                        reason = "안정적인 시장 환경";
                    } else {
                        reason = "포트폴리오 분산 강화";
                    }
                } else {
                    reason = "리스크 분산 및 포트폴리오 안정화";
                }
            } else {
                // 비중 감소 - 부정적 이유
                if (analysisReason != null && !analysisReason.isEmpty()) {
                    if (analysisReason.contains("하락") || analysisReason.contains("악재") ||
                            analysisReason.contains("부정") || analysisReason.contains("위험")) {
                        reason = "시장 불확실성과 하방 리스크";
                    } else if (analysisReason.contains("과열") || analysisReason.contains("급등") ||
                            analysisReason.contains("변동성")) {
                        reason = "과열된 시장과 높은 변동성";
                    } else if (analysisReason.contains("조정")) {
                        reason = "조정 가능성과 리스크 관리";
                    } else {
                        reason = "과도한 비중으로 인한 리스크 증가";
                    }
                } else {
                    reason = "포트폴리오 리밸런싱 필요";
                }
            }
        } else {
            // 뉴스 분석이 없는 경우 카테고리 기반 이유 생성
            if (change > 0) {
                if (etf.getCategory() != null && etf.getCategory().contains("해외")) {
                    reason = "해외 분산투자 강화";
                } else if (etf.getCategory() != null && etf.getCategory().contains("채권")) {
                    reason = "안정 자산 비중 확대";
                } else {
                    reason = "포트폴리오 균형 조정";
                }
            } else {
                if (etf.getCategory() != null && etf.getCategory().contains("국내")) {
                    reason = "국내 시장 과도한 집중 완화";
                } else {
                    reason = "리스크 관리 및 조정";
                }
            }
        }

        // 최종 문장 생성
        return String.format("%s를 %s한 이유로 비중을 %s", etfName, reason, action);
    }
}

// Helper 클래스들
@Getter
@Builder
class EtfNewsAnalysis {
    private Etf etf;
    private PortfolioAllocation allocation;
    private List<EtfNews> newsList;
    private AnalysisResult analysis;
}

@Getter
@Builder
class RebalancingRecommendation {
    private boolean rebalancingRequired;
    private String reason;
    private String riskLevel;
    private Map<Long, Double> recommendedWeights;
    private String advice;
    private String fullAnalysis;
}