package koscom.mini_project.team2.team2.domain.etf.dto;

import koscom.mini_project.team2.team2.domain.etf.entity.Etf;
import koscom.mini_project.team2.team2.domain.etf.entity.Stock;

import java.util.List;

public record EtfResponse(
        Long id,
        String name,
        Integer fltRt,
        Integer riskLevel,
        String category,
        String description,
        List<Stock> stockList
) {
    public static EtfResponse from(Etf etf) {
        return new EtfResponse(etf.getId(), etf.getName(), etf.getFltRt(),
                etf.getRiskLevel(), etf.getCategory(), etf.getDescription(), etf.getStockList());
    }
}
