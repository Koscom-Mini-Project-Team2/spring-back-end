package koscom.mini_project.team2.team2.domain.news.repository;

import koscom.mini_project.team2.team2.domain.news.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
    List<Portfolio> findByMemberId(Long memberId);
    Optional<Portfolio> findByIdAndMemberId(Long id, Long memberId);
}