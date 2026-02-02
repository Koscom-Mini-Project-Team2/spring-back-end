package koscom.mini_project.team2.team2.domain.etf.repository;

import koscom.mini_project.team2.team2.domain.etf.entity.Etf;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EtfRepository extends JpaRepository<Etf, Long> {
    Optional<Etf> findByName(String name);
    List<Etf> findByNameContaining(String name);
}
