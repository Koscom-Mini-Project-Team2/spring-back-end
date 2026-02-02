package koscom.mini_project.team2.team2.domain.tutor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorQuestionRequest {

    @NotBlank(message = "질문을 입력해주세요.")
    private String question;

    private Long memberId;  // 선택적: 사용자 식별용
}