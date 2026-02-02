package koscom.mini_project.team2.team2.domain.etf.repository;

import koscom.mini_project.team2.team2.domain.etf.entity.Etf;

import java.util.List;

public interface EtfRepositoryCustom {

    List<Etf> searchEtfs(Integer fltRt, Integer riskLevel);

}