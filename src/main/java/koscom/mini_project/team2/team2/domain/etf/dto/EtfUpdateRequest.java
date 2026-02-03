package koscom.mini_project.team2.team2.domain.etf.dto;

import koscom.mini_project.team2.team2.domain.etf.entity.Stock;

import java.util.List;

public record EtfUpdateRequest(
        List<Stock> stockList
) { }
