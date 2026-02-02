package koscom.mini_project.team2.team2.domain.news.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

// 뉴스 근거 DTO
@Getter
@Setter
@Builder
public class NewsEvidenceDto {
    private Long etfId;
    private String etfName;
    private String newsTitle;
    private String newsUrl;
    private java.time.LocalDateTime publishedAt;
    private String impact;
    private String summary;
}
