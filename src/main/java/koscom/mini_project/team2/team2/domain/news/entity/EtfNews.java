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
@Table(name = "etf_news")
public class EtfNews {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long etfId;  // Etf의 ID를 직접 저장

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 2000)
    private String content;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column
    private String source;

    @Column(nullable = false)
    private LocalDateTime publishedAt;

    @Column(nullable = false)
    private LocalDateTime collectedAt;

    @Column
    private boolean analyzed = false;

    @Column
    private boolean alertTriggered = false;

    @PrePersist
    protected void onCreate() {
        collectedAt = LocalDateTime.now();
    }
}
