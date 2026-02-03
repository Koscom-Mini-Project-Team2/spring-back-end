package koscom.mini_project.team2.team2.domain.etf.dto;

import koscom.mini_project.team2.team2.domain.etf.entity.Etf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EtfRecommendResponseDto {
    private String investmentType;            // 투자 성향
    private String investmentProfile;            // 투자 성향
    private Integer etfRiskScore;                 // 0~100
    private Integer dividendScore;                // 0~100
    private Integer expectedTotalReturn;          // 정수
    private List<Integer> portfolioWeights;       // 5개, 합=100
    private List<Etf> etfs;                    // 5개
    private String reasonSummary;                 // 3줄 요약
}

