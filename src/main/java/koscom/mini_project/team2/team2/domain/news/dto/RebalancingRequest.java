package koscom.mini_project.team2.team2.domain.news.dto;

import lombok.*;

// 요청 DTO
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RebalancingRequest {
    private Long portfolioId;
    private Long memberId;
    private String userEmail;
    private Integer period;  // 리밸런싱 주기 (월)
}