package koscom.mini_project.team2.team2.domain.tutor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorAnswerResponse {

    private String question;
    private String answer;
    private LocalDateTime timestamp;
    private String model;
    private boolean success;
}