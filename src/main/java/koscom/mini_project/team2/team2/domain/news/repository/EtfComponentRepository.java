package koscom.mini_project.team2.team2.domain.news.repository;

import koscom.mini_project.team2.team2.domain.news.entity.EtfComponent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EtfComponentRepository extends JpaRepository<EtfComponent, Long> {
    List<EtfComponent> findByEtfId(Long etfId);
    List<EtfComponent> findByEtfIdOrderByWeightDesc(Long etfId);  // 비중 높은 순
    void deleteAllByEtfId(Long etfId);
}