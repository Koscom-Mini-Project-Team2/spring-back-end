package koscom.mini_project.team2.team2.domain.news.controller;

import koscom.mini_project.team2.team2.domain.etf.entity.Etf;
import koscom.mini_project.team2.team2.domain.news.entity.EtfComponent;
import koscom.mini_project.team2.team2.domain.news.repository.EtfComponentRepository;
import koscom.mini_project.team2.team2.domain.etf.repository.EtfRepository;
import koscom.mini_project.team2.team2.domain.news.service.EtfComponentInitializer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/etf-components")
@RequiredArgsConstructor
public class EtfComponentAdminController {

    private final EtfComponentInitializer initializer;
    private final EtfComponentRepository repository;
    private final EtfRepository etfRepository;

    /**
     * 모든 ETF 구성종목 초기화
     * POST /api/admin/etf-components/initialize
     */
    @PostMapping("/initialize")
    public ResponseEntity<Map<String, Object>> initializeAll() {
        try {
            initializer.initializeAllEtfComponents();

            long totalCount = repository.count();

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "ETF 구성종목 초기화 완료",
                    "totalComponents", (Object) totalCount  // 명시적 캐스팅
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "error",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * 특정 ETF의 구성종목 조회
     * GET /api/admin/etf-components/{etfId}
     */
    @GetMapping("/{etfId}")
    public ResponseEntity<Map<String, Object>> getComponents(@PathVariable Long etfId) {
        Etf etf = etfRepository.findById(etfId)
                .orElseThrow(() -> new RuntimeException("ETF를 찾을 수 없습니다."));

        List<EtfComponent> components = repository.findByEtfIdOrderByWeightDesc(etfId);

        return ResponseEntity.ok(Map.of(
                "etfId", (Object) etf.getId(),
                "etfName", etf.getName(),
                "totalComponents", (Object) components.size(),
                "components", components
        ));
    }

    /**
     * 모든 ETF 목록 및 구성종목 수 조회
     * GET /api/admin/etf-components/summary
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        List<Etf> etfs = etfRepository.findAll();

        List<Map<String, Object>> summary = etfs.stream()
                .map(etf -> {
                    long componentCount = repository.findByEtfId(etf.getId()).size();
                    Map<String, Object> map = new HashMap<>();
                    map.put("etfId", etf.getId());
                    map.put("etfName", etf.getName());
                    map.put("category", etf.getCategory() != null ? etf.getCategory() : "미분류");
                    map.put("componentCount", componentCount);
                    return map;
                })
                .toList();

        return ResponseEntity.ok(Map.of(
                "totalEtfs", (Object) etfs.size(),
                "etfs", summary
        ));
    }
}