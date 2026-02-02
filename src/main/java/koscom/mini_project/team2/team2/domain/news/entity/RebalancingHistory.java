package koscom.mini_project.team2.team2.domain.news.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// 리밸런싱 이력
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "rebalancing_history")
public class RebalancingHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long portfolioId;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private Integer period;  // 리밸런싱 주기 (월)

    @Column(length = 2000)
    private String reason;

    @Column(length = 5000)
    private String newsEvidence;

    @Column(length = 3000)
    private String beforeAllocation;

    @Column(length = 3000)
    private String afterAllocation;

    @Column(nullable = false)
    private boolean emailSent;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}