package koscom.mini_project.team2.team2.domain.news.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class RebalancingResponse {
    private Long portfolioId;
    private String portfolioName;
    private boolean rebalancingRequired;
    private String rebalancingReason;

    private java.util.List<AllocationDto> currentPortfolio;
    private java.util.List<AllocationChangeDto> recommendedPortfolio;
    private java.util.List<NewsEvidenceDto> newsEvidence;

    private String riskAssessment;
    private java.util.List<String> recommendations;

    private java.time.LocalDateTime analyzedAt;
}
