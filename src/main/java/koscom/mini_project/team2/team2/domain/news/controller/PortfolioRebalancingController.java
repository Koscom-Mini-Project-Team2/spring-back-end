package koscom.mini_project.team2.team2.domain.news.controller;

import koscom.mini_project.team2.team2.domain.news.dto.ErrorResponse;
import koscom.mini_project.team2.team2.domain.news.dto.RebalancingRequest;
import koscom.mini_project.team2.team2.domain.news.dto.RebalancingResponse;
import koscom.mini_project.team2.team2.domain.news.service.RebalancingNotificationService;
import koscom.mini_project.team2.team2.domain.news.service.RebalancingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
@Slf4j
public class PortfolioRebalancingController {

    private final RebalancingService rebalancingService;
    private final RebalancingNotificationService notificationService;

    /**
     * 포트폴리오 리밸런싱 분석 및 알림
     * POST /api/portfolio/rebalancing/analyze
     */
    @PostMapping("/rebalancing/analyze")
    public ResponseEntity<?> analyzeRebalancing(@RequestBody RebalancingRequest request) {
        log.info("포트폴리오 리밸런싱 분석 요청: portfolioId={}, memberId={}, period={}개월",
                request.getPortfolioId(), request.getMemberId(), request.getPeriod());

        try {
            // 1. 리밸런싱 분석
            RebalancingResponse response = rebalancingService.analyzeAndRecommendRebalancing(
                    request.getPortfolioId(),
                    request.getMemberId()
            );

            // 2. 이메일 발송 (선택적)
            if (request.getUserEmail() != null && !request.getUserEmail().isEmpty()) {
                notificationService.sendRebalancingEmail(
                        response,
                        request.getUserEmail(),
                        request.getMemberId(),
                        request.getPeriod()
                );
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("리밸런싱 분석 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse.builder()
                            .status("ERROR")
                            .message("리밸런싱 분석 중 오류가 발생했습니다: " + e.getMessage())
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }

    /**
     * 테스트용 간단한 API
     * GET /api/portfolio/rebalancing/test?portfolioId=1&memberId=1&email=test@example.com&period=3
     */
    @GetMapping("/rebalancing/test")
    public ResponseEntity<?> testRebalancing(
            @RequestParam Long portfolioId,
            @RequestParam Long memberId,
            @RequestParam String email,
            @RequestParam(defaultValue = "3") Integer period) {

        RebalancingRequest request = new RebalancingRequest();
        request.setPortfolioId(portfolioId);
        request.setMemberId(memberId);
        request.setUserEmail(email);
        request.setPeriod(period);

        return analyzeRebalancing(request);
    }
}