package koscom.mini_project.team2.team2.domain.etf.repository;

import koscom.mini_project.team2.team2.domain.etf.entity.Etf;
import koscom.mini_project.team2.team2.domain.etf.entity.Stock;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class EtfRepositoryImpl implements EtfRepositoryCustom {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper; // ✅ Spring Boot가 자동 Bean 제공

    private static final TypeReference<List<Stock>> STOCK_LIST_TYPE =
            new TypeReference<>() {};

    @Override
    public List<Etf> searchEtfs(Integer fltRt, Integer riskLevel) {

        String sql = """
            SELECT
                id,
                name,
                flt_rt,
                risk_level,
                category,
                description,
                stock_list
            FROM etf
            WHERE flt_rt BETWEEN ? AND ?
               OR risk_level BETWEEN ? AND ?
            LIMIT 10
        """;

        int fltRtMin = fltRt - 30;
        int fltRtMax = fltRt + 30;
        int riskMin = riskLevel - 3;
        int riskMax = riskLevel + 3;

        return jdbcTemplate.query(
                sql,
                new Object[]{fltRtMin, fltRtMax, riskMin, riskMax},
                etfRowMapper()
        );
    }

    private RowMapper<Etf> etfRowMapper() {
        return (rs, rowNum) -> {
            Etf etf = new Etf();
            etf.setId(rs.getLong("id"));
            etf.setName(rs.getString("name"));
            etf.setFltRt(rs.getInt("flt_rt"));
            etf.setRiskLevel(rs.getInt("risk_level"));
            etf.setCategory(rs.getString("category"));
            etf.setDescription(rs.getString("description"));

            // ✅ 핵심: JSON String -> List<Stock>
            String stockJson = rs.getString("stock_list");
            etf.setStockList(parseStockList(stockJson));

            return etf;
        };
    }

    private List<Stock> parseStockList(String json) {
        try {
            if (json == null || json.isBlank()) return Collections.emptyList();
            return objectMapper.readValue(json, STOCK_LIST_TYPE);
        } catch (Exception e) {
            // 운영에서는 로그 남기고 빈 리스트/예외 중 선택
            // 여기서는 검색/조회가 깨지지 않도록 빈 리스트 반환
            return Collections.emptyList();
        }
    }
}
