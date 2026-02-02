package koscom.mini_project.team2.team2.domain.etf.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "etf")
public class Etf {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String name;

    @Column
    private Integer fltRt;

    @Column
    private Integer riskLevel;

    @Column
    private String category;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

}