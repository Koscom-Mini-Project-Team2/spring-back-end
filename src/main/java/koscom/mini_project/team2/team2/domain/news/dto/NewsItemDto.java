package koscom.mini_project.team2.team2.domain.news.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class NewsItemDto {
    private Long newsId;
    private String title;
    private String content;
    private String url;
    private String source;
    private LocalDateTime publishedAt;
}

