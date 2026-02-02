package koscom.mini_project.team2.team2.domain.news.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class NotificationResult {
    private boolean emailSent;
    private String status;
    private String message;
}