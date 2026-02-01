package koscom.mini_project.team2.team2.domain.etf.dto;

import koscom.mini_project.team2.team2.domain.etf.entity.Etf;

public record EtfResponse(
        Long id,
        Integer fltRt,
        Integer riskLevel,
        String category,
        String description
) {
    public static EtfResponse from(Etf etf) {
        return new EtfResponse(etf.getId(), etf.getFltRt(),
                etf.getRiskLevel(), etf.getCategory(), etf.getDescription());
    }
}
