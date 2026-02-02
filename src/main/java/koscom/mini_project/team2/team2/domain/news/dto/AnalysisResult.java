package koscom.mini_project.team2.team2.domain.news.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnalysisResult {
    private boolean shouldAlert;
    private String reason;
    private String summary;
    private String fullAnalysis;
}
