package koscom.mini_project.team2.team2.domain.news.controller;

import koscom.mini_project.team2.team2.domain.news.service.PortfolioInitializer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/portfolio")
@RequiredArgsConstructor
public class PortfolioAdminController {

    private final PortfolioInitializer portfolioInitializer;

    /**
     * 테스트용 포트폴리오 생성
     * POST /api/admin/portfolio/initialize
     */
    @PostMapping("/initialize")
    public ResponseEntity<?> initializePortfolio() {
        try {
            portfolioInitializer.initializeTestPortfolio();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "테스트 포트폴리오가 생성되었습니다."
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }
}