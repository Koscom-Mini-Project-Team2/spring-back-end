package koscom.mini_project.team2.team2.domain.etf.dto;

public record ETFCreateRequest(
        String name,
        Integer fltRt,
        Integer riskLevel,
        String category,
        String description
) { }
