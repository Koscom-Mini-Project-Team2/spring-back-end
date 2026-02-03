package koscom.mini_project.team2.team2.domain.tutor.repository;

import koscom.mini_project.team2.team2.domain.tutor.entity.TutorHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TutorHistoryRepository extends JpaRepository<TutorHistory, Long> {

    /**
     * 특정 사용자의 튜터 이용 이력 조회
     */
    List<TutorHistory> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    /**
     * 최근 N개 질문 조회
     */
    List<TutorHistory> findTop10ByOrderByCreatedAtDesc();
}