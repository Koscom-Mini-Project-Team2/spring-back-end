package koscom.mini_project.team2.team2.domain.news.repository;

import koscom.mini_project.team2.team2.domain.news.entity.EtfNews;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EtfNewsRepository extends JpaRepository<EtfNews, Long> {
    List<EtfNews> findByEtfIdAndAnalyzedFalseOrderByPublishedAtDesc(Long etfId);
    List<EtfNews> findByEtfIdOrderByPublishedAtDesc(Long etfId, Pageable pageable);
    boolean existsByEtfIdAndUrl(Long etfId, String url);
    boolean existsByEtfIdAndTitle(Long etfId, String title);
}
