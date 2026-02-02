package koscom.mini_project.team2.team2.domain.news.controller;

import koscom.mini_project.team2.team2.domain.etf.entity.Etf;
import koscom.mini_project.team2.team2.domain.etf.repository.EtfRepository;
import koscom.mini_project.team2.team2.domain.news.dto.*;
import koscom.mini_project.team2.team2.domain.news.entity.EtfNews;
import koscom.mini_project.team2.team2.domain.news.service.NewsAnalysisService;
import koscom.mini_project.team2.team2.domain.news.service.NewsCollectorService;
import koscom.mini_project.team2.team2.domain.news.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/etf-alert")
@RequiredArgsConstructor
@Slf4j
public class EtfAlertController {

    private final EtfRepository etfRepository;
    private final NewsCollectorService newsCollector;
    private final NewsAnalysisService newsAnalysis;
    private final NotificationService notificationService;

    /**
     * ETF 뉴스 분석 및 알림 발송 (메인 API)
     * POST /api/etf-alert/analyze
     */
    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeETFNews(@RequestBody NewsAnalysisRequest request) {

        log.info("ETF 뉴스 분석 요청: {}", request.getEtfName());

        try {
            // 1. ETF 조회
            Etf etf = etfRepository.findByName(request.getEtfName())
                    .orElseThrow(() -> new RuntimeException("ETF를 찾을 수 없습니다: " + request.getEtfName()));

            // 2. 뉴스 수집
            log.info("뉴스 수집 시작: {}", etf.getName());
            List<EtfNews> newsList = newsCollector.collectAndSaveNews(etf);

            if (newsList.isEmpty()) {
                return ResponseEntity.ok(NewsAnalysisResponse.builder()
                        .etfId(etf.getId())
                        .etfName(etf.getName())
                        .etfCategory(etf.getCategory())
                        .etfRiskLevel(etf.getRiskLevel())
                        .totalNewsCollected(0)
                        .newsList(List.of())
                        .alertTriggered(false)
                        .analysisResult("수집된 뉴스가 없습니다.")
                        .notificationResult(NotificationResult.builder()
                                .emailSent(false)
                                .status("NO_NEWS")
                                .message("수집된 뉴스가 없어 알림을 발송하지 않았습니다.")
                                .build())
                        .processedAt(LocalDateTime.now())
                        .build());
            }

            // 3. AI 분석
            log.info("AI 분석 시작: {} 건의 뉴스", newsList.size());
            AnalysisResult analysis = newsAnalysis.analyzeNews(etf, newsList);

            // 4. 알림 발송 여부 결정 및 실행
            boolean emailSent = false;
            String notificationStatus;
            String notificationMessage;

            if (analysis.isShouldAlert()) {
                log.info("중요 뉴스 감지 - 알림 발송 시작");
                emailSent = notificationService.sendEmailAlert(
                        etf,
                        request.getUserEmail(),
                        analysis.getSummary(),
                        newsList,
                        request.getUserId()
                );

                notificationStatus = emailSent ? "SUCCESS" : "FAILED";
                notificationMessage = emailSent ?
                        "이메일 알림이 성공적으로 발송되었습니다." :
                        "이메일 발송에 실패했습니다. 로그를 확인해주세요.";
            } else {
                log.info("일반 뉴스로 판단 - 알림 발송하지 않음");
                notificationStatus = "NOT_REQUIRED";
                notificationMessage = "중요도가 낮아 알림을 발송하지 않았습니다.";
            }

            // 5. 응답 생성
            List<NewsItemDto> newsItemDtos = newsList.stream()
                    .map(news -> NewsItemDto.builder()
                            .newsId(news.getId())
                            .title(news.getTitle())
                            .content(news.getContent())
                            .url(news.getUrl())
                            .source(news.getSource())
                            .publishedAt(news.getPublishedAt())
                            .build())
                    .collect(Collectors.toList());

            NewsAnalysisResponse response = NewsAnalysisResponse.builder()
                    .etfId(etf.getId())
                    .etfName(etf.getName())
                    .etfCategory(etf.getCategory())
                    .etfRiskLevel(etf.getRiskLevel())
                    .totalNewsCollected(newsList.size())
                    .newsList(newsItemDtos)
                    .alertTriggered(analysis.isShouldAlert())
                    .analysisResult(analysis.getFullAnalysis())
                    .alertSummary(analysis.getSummary())
                    .notificationResult(NotificationResult.builder()
                            .emailSent(emailSent)
                            .status(notificationStatus)
                            .message(notificationMessage)
                            .build())
                    .processedAt(LocalDateTime.now())
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("ETF 뉴스 분석 중 오류 발생: {}", e.getMessage(), e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse.builder()
                            .status("ERROR")
                            .message("처리 중 오류가 발생했습니다: " + e.getMessage())
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }

    /**
     * 간단한 테스트용 API
     * GET /api/etf-alert/test?etfName=KODEX 200&email=test@example.com
     */
    @GetMapping("/test")
    public ResponseEntity<?> testAlert(
            @RequestParam String etfName,
            @RequestParam String email) {

        NewsAnalysisRequest request = new NewsAnalysisRequest();
        request.setEtfName(etfName);
        request.setUserEmail(email);
        request.setUserId(1L);  // 테스트용 더미 ID

        return analyzeETFNews(request);
    }
}