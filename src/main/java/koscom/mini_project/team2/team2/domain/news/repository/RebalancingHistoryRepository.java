package koscom.mini_project.team2.team2.domain.news.repository;

import koscom.mini_project.team2.team2.domain.news.entity.RebalancingHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RebalancingHistoryRepository extends JpaRepository<RebalancingHistory, Long> {
}