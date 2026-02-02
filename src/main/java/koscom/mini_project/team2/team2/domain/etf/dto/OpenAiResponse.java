package koscom.mini_project.team2.team2.domain.etf.dto;

import java.util.List;

public record OpenAiResponse(
        List<Output> output
) {
    public record Output(
            String type,
            List<Content> content
    ) {}

    public record Content(
            String type,
            String text
    ) {}
}
