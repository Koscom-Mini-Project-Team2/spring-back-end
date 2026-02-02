package koscom.mini_project.team2.team2.domain.etf.dto;

import java.util.List;

public record EtfRecommendRequest(
        List<QaItem> qaList
) {
    public record QaItem(
            String question,
            String answer
    ) {}
}
