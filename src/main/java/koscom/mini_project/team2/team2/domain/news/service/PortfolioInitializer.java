package koscom.mini_project.team2.team2.domain.news.service;

import koscom.mini_project.team2.team2.domain.etf.entity.Etf;
import koscom.mini_project.team2.team2.domain.etf.repository.EtfRepository;
import koscom.mini_project.team2.team2.domain.news.entity.Portfolio;
import koscom.mini_project.team2.team2.domain.news.entity.PortfolioAllocation;
import koscom.mini_project.team2.team2.domain.news.repository.PortfolioAllocationRepository;
import koscom.mini_project.team2.team2.domain.news.repository.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PortfolioInitializer {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioAllocationRepository allocationRepository;
    private final EtfRepository etfRepository;

    /**
     * 테스트용 포트폴리오 초기화
     */
    public void initializeTestPortfolio() {
        log.info("테스트 포트폴리오 초기화 시작");

        // 1. 테스트 포트폴리오 생성
        Portfolio portfolio = Portfolio.builder()
                .memberId(1L)
                .portfolioName("내 안정형 포트폴리오")
                .description("장기 투자용 안정형 포트폴리오")
                .build();

        Portfolio savedPortfolio = portfolioRepository.save(portfolio);
        log.info("포트폴리오 생성 완료: {}", savedPortfolio.getId());

        // 2. 포트폴리오 구성 (3개 ETF)
        Etf kodex200 = etfRepository.findByName("KODEX 200").orElse(null);
        Etf tigerSp500 = etfRepository.findByName("TIGER 미국S&P500").orElse(null);
        Etf kodexKosdaq150 = etfRepository.findByName("KODEX 코스닥150").orElse(null);

        if (kodex200 != null && tigerSp500 != null && kodexKosdaq150 != null) {
            List<PortfolioAllocation> allocations = Arrays.asList(
                    createAllocation(savedPortfolio.getId(), kodex200.getId(), 45.0),
                    createAllocation(savedPortfolio.getId(), tigerSp500.getId(), 30.0),
                    createAllocation(savedPortfolio.getId(), kodexKosdaq150.getId(), 25.0)
            );

            allocationRepository.saveAll(allocations);
            log.info("포트폴리오 구성 완료: {} 개 ETF", allocations.size());
        } else {
            log.warn("일부 ETF를 찾을 수 없습니다.");
        }

        log.info("테스트 포트폴리오 초기화 완료");
    }

    private PortfolioAllocation createAllocation(Long portfolioId, Long etfId, Double weight) {
        return PortfolioAllocation.builder()
                .portfolioId(portfolioId)
                .etfId(etfId)
                .targetWeight(weight)
                .build();
    }
}