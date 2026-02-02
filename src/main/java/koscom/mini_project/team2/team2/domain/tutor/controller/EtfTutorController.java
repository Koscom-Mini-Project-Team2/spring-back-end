package koscom.mini_project.team2.team2.domain.tutor.controller;

import jakarta.validation.Valid;
import koscom.mini_project.team2.team2.domain.tutor.dto.TutorAnswerResponse;
import koscom.mini_project.team2.team2.domain.tutor.dto.TutorQuestionRequest;
import koscom.mini_project.team2.team2.domain.tutor.entity.TutorHistory;
import koscom.mini_project.team2.team2.domain.tutor.repository.TutorHistoryRepository;
import koscom.mini_project.team2.team2.domain.tutor.service.EtfTutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/etf-tutor")
@RequiredArgsConstructor
@Slf4j
public class EtfTutorController {

    private final EtfTutorService tutorService;
    private final TutorHistoryRepository tutorHistoryRepository;

    /**
     * ETF 관련 질문에 대한 AI 튜터 답변 제공
     * POST /api/etf-tutor/ask
     */
    @PostMapping("/ask")
    public ResponseEntity<TutorAnswerResponse> askQuestion(
            @Valid @RequestBody TutorQuestionRequest request) {

        log.info("ETF 튜터 질문 수신: {}", request.getQuestion());

        TutorAnswerResponse response = tutorService.generateAnswer(
                request.getQuestion(),
                request.getMemberId()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 헬스 체크 엔드포인트
     * GET /api/etf-tutor/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        response.put("service", "ETF AI Tutor");
        response.put("message", "Service is running");
        return ResponseEntity.ok(response);
    }

    /**
     * 사용자별 튜터 이용 이력 조회 (선택적)
     * GET /api/etf-tutor/history/{memberId}
     */
    @GetMapping("/history/{memberId}")
    public ResponseEntity<List<TutorHistory>> getTutorHistory(
            @PathVariable Long memberId) {

        log.info("튜터 이력 조회 요청: memberId={}", memberId);

        List<TutorHistory> history = tutorHistoryRepository
                .findByMemberIdOrderByCreatedAtDesc(memberId);

        return ResponseEntity.ok(history);
    }

    /**
     * 최근 질문 목록 조회 (선택적)
     * GET /api/etf-tutor/recent
     */
    @GetMapping("/recent")
    public ResponseEntity<List<TutorHistory>> getRecentQuestions() {

        log.info("최근 질문 목록 조회");

        List<TutorHistory> recentHistory = tutorHistoryRepository
                .findTop10ByOrderByCreatedAtDesc();

        return ResponseEntity.ok(recentHistory);
    }

    /**
     * 간단한 테스트 엔드포인트
     * GET /api/etf-tutor/test?question=ETF가 뭔가요?
     */
    @GetMapping("/test")
    public ResponseEntity<TutorAnswerResponse> testQuestion(
            @RequestParam(defaultValue = "ETF가 무엇인가요?") String question) {

        TutorQuestionRequest request = TutorQuestionRequest.builder()
                .question(question)
                .memberId(null)
                .build();

        return askQuestion(request);
    }
}