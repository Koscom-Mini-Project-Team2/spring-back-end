package koscom.mini_project.team2.team2.domain.news.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

// 추천 포트폴리오 DTO
@Getter
@Setter
@Builder
public class AllocationChangeDto {
    private Long etfId;
    private String etfName;
    private String category;
    private Double currentWeight;
    private Double recommendedWeight;
    private Double changeAmount;
    private String changeReason;
}
