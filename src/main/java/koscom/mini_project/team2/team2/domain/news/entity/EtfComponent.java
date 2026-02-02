package koscom.mini_project.team2.team2.domain.news.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// ETF 구성 종목 정보
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "etf_component")
public class EtfComponent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long etfId;  // Etf의 ID

    @Column(nullable = false, length = 6)
    private String stockCode;  // 종목 코드 (ex: "005930")

    @Column(nullable = false)
    private String stockName;  // 종목명 (ex: "삼성전자")

    @Column
    private Double weight;  // 구성 비율(%)

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}