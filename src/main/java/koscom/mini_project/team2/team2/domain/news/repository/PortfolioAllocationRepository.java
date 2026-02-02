package koscom.mini_project.team2.team2.domain.news.repository;

import koscom.mini_project.team2.team2.domain.news.entity.PortfolioAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PortfolioAllocationRepository extends JpaRepository<PortfolioAllocation, Long> {
    List<PortfolioAllocation> findByPortfolioId(Long portfolioId);
}