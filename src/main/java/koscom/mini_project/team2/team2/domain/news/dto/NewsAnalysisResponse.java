package koscom.mini_project.team2.team2.domain.news.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class NewsAnalysisResponse {
    private Long etfId;
    private String etfName;
    private String etfCategory;
    private Integer etfRiskLevel;

    private int totalNewsCollected;
    private List<NewsItemDto> newsList;

    private boolean alertTriggered;
    private String analysisResult;
    private String alertSummary;

    private NotificationResult notificationResult;

    private LocalDateTime processedAt;
}
