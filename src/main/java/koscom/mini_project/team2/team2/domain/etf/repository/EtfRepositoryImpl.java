package koscom.mini_project.team2.team2.domain.etf.repository;

import koscom.mini_project.team2.team2.domain.etf.entity.Etf;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class EtfRepositoryImpl implements EtfRepositoryCustom {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<Etf> searchEtfs(Integer fltRt, Integer riskLevel) {

        String sql = """
            SELECT
                id,
                name,
                flt_rt,
                risk_level,
                category,
                description
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
            return etf;
        };
    }
}
