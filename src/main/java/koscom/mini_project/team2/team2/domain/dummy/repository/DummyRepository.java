package koscom.mini_project.team2.team2.domain.dummy.repository;

import koscom.mini_project.team2.team2.domain.dummy.entity.Dummy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DummyRepository extends JpaRepository<Dummy, Long> {
}
