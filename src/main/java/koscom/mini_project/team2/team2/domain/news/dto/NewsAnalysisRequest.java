package koscom.mini_project.team2.team2.domain.news.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NewsAnalysisRequest {
    private String etfName;      // ETF 이름으로 검색
    private String userEmail;    // 알림 받을 이메일
    private String userPhone;    // 알림 받을 전화번호 (선택)
    private Long userId;
}
