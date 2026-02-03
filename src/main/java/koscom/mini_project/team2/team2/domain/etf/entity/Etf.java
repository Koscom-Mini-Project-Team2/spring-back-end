package koscom.mini_project.team2.team2.domain.etf.entity;

import jakarta.persistence.*;
import koscom.mini_project.team2.team2.config.StockListJsonConverter;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

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

    @Lob
    @Column(name = "stock_list", columnDefinition = "TEXT")
    @Convert(converter = StockListJsonConverter.class)
    private List<Stock> stockList = new ArrayList<>();

}