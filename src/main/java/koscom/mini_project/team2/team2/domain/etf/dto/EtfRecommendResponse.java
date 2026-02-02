package koscom.mini_project.team2.team2.domain.etf.dto;

import java.util.List;
import java.util.Map;

public record EtfRecommendResponse(
        String investmentProfile,
        String etfRiskLevel,
        String dividendYield,
        String expectedTotalReturn,
        Map<String, Integer> portfolioAllocation,
        List<EtfItem> etfs
) {
    public record EtfItem(
            String ticker,
            String name,
            String reasonSummary3Lines,
            String details
    ) {}
}
