package koscom.mini_project.team2.team2.domain.news.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "alert_history")
public class AlertHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long etfId;  // Etf의 ID를 직접 저장

    @Column(nullable = false)
    private String etfName;

    @Column(nullable = false, length = 20)
    private String alertType;  // "NEWS" or "PRICE_CHANGE"

    @Column(nullable = false, length = 20)
    private String channel;  // "EMAIL" or "SMS"

    @Column(nullable = false)
    private Long memberId;

    @Column(length = 2000)
    private String content;

    @Column(nullable = false)
    private boolean sent;

    @Column
    private String failureReason;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
    }
}