package koscom.mini_project.team2.team2.domain.news.service;

import koscom.mini_project.team2.team2.domain.etf.entity.Etf;
import koscom.mini_project.team2.team2.domain.news.entity.EtfComponent;
import koscom.mini_project.team2.team2.domain.news.repository.EtfComponentRepository;
import koscom.mini_project.team2.team2.domain.etf.repository.EtfRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EtfComponentInitializer {

    private final EtfComponentRepository etfComponentRepository;
    private final EtfRepository etfRepository;

    /**
     * ETF 구성종목 초기 데이터 세팅
     * 애플리케이션 시작 시 자동 실행하려면 @PostConstruct 추가
     */
    public void initializeAllEtfComponents() {
        log.info("ETF 구성종목 초기화 시작");

        // 1. KODEX 200
        initializeKodex200();

        // 2. TIGER 미국S&P500
        initializeTigerSP500();

        // 3. KODEX 레버리지
        initializeKodexLeverage();

        // 4. TIGER 2차전지테마
        initializeTigerBattery();

        // 5. KODEX 코스닥150
        initializeKodexKosdaq150();

        log.info("ETF 구성종목 초기화 완료");
    }

    /**
     * 1. KODEX 200 (국내 대표 ETF)
     */
    private void initializeKodex200() {
        Etf etf = etfRepository.findByName("KODEX 200").orElse(null);
        if (etf == null) {
            log.warn("KODEX 200 ETF를 찾을 수 없습니다.");
            return;
        }

        // 기존 데이터 삭제
        etfComponentRepository.deleteAllByEtfId(etf.getId());

        List<EtfComponent> components = Arrays.asList(
                createComponent(etf.getId(), "005930", "삼성전자", 26.85),
                createComponent(etf.getId(), "000660", "SK하이닉스", 5.32),
                createComponent(etf.getId(), "035420", "NAVER", 3.89),
                createComponent(etf.getId(), "051910", "LG화학", 2.45),
                createComponent(etf.getId(), "006400", "삼성SDI", 2.31),
                createComponent(etf.getId(), "035720", "카카오", 2.18),
                createComponent(etf.getId(), "005380", "현대차", 2.05),
                createComponent(etf.getId(), "012330", "현대모비스", 1.87),
                createComponent(etf.getId(), "028260", "삼성물산", 1.65),
                createComponent(etf.getId(), "207940", "삼성바이오로직스", 1.58)
        );

        etfComponentRepository.saveAll(components);
        log.info("KODEX 200 구성종목 {} 건 등록 완료", components.size());
    }

    /**
     * 2. TIGER 미국S&P500 (해외 ETF)
     */
    private void initializeTigerSP500() {
        Etf etf = etfRepository.findByName("TIGER 미국S&P500").orElse(null);
        if (etf == null) {
            log.warn("TIGER 미국S&P500 ETF를 찾을 수 없습니다.");
            return;
        }

        etfComponentRepository.deleteAllByEtfId(etf.getId());

        // S&P500은 미국 종목이므로 종목코드를 임의로 설정 (실제는 티커)
        // 실제 뉴스 수집 시에는 한글 종목명으로 검색
        List<EtfComponent> components = Arrays.asList(
                createComponent(etf.getId(), "AAPL", "애플", 7.2),
                createComponent(etf.getId(), "MSFT", "마이크로소프트", 6.8),
                createComponent(etf.getId(), "AMZN", "아마존", 3.5),
                createComponent(etf.getId(), "NVDA", "엔비디아", 3.2),
                createComponent(etf.getId(), "GOOGL", "알파벳A", 2.9),
                createComponent(etf.getId(), "META", "메타", 2.4),
                createComponent(etf.getId(), "TSLA", "테슬라", 2.1),
                createComponent(etf.getId(), "BRK.B", "버크셔해서웨이", 1.8),
                createComponent(etf.getId(), "UNH", "유나이티드헬스", 1.5),
                createComponent(etf.getId(), "JNJ", "존슨앤존슨", 1.4)
        );

        etfComponentRepository.saveAll(components);
        log.info("TIGER 미국S&P500 구성종목 {} 건 등록 완료", components.size());
    }

    /**
     * 3. KODEX 레버리지 (변동성 높은 ETF)
     */
    private void initializeKodexLeverage() {
        Etf etf = etfRepository.findByName("KODEX 레버리지").orElse(null);
        if (etf == null) {
            log.warn("KODEX 레버리지 ETF를 찾을 수 없습니다.");
            return;
        }

        etfComponentRepository.deleteAllByEtfId(etf.getId());

        // 레버리지 ETF는 KOSPI200 선물 2배 추종
        // 실제 구성은 선물이지만, 대표 종목으로 표현
        List<EtfComponent> components = Arrays.asList(
                createComponent(etf.getId(), "005930", "삼성전자", 28.5),
                createComponent(etf.getId(), "000660", "SK하이닉스", 5.8),
                createComponent(etf.getId(), "035420", "NAVER", 4.2),
                createComponent(etf.getId(), "051910", "LG화학", 2.8),
                createComponent(etf.getId(), "006400", "삼성SDI", 2.6),
                createComponent(etf.getId(), "005380", "현대차", 2.3),
                createComponent(etf.getId(), "035720", "카카오", 2.1),
                createComponent(etf.getId(), "012330", "현대모비스", 2.0),
                createComponent(etf.getId(), "028260", "삼성물산", 1.8),
                createComponent(etf.getId(), "068270", "셀트리온", 1.7)
        );

        etfComponentRepository.saveAll(components);
        log.info("KODEX 레버리지 구성종목 {} 건 등록 완료", components.size());
    }

    /**
     * 4. TIGER 2차전지테마 (테마형 ETF)
     */
    private void initializeTigerBattery() {
        Etf etf = etfRepository.findByName("TIGER 2차전지테마").orElse(null);
        if (etf == null) {
            log.warn("TIGER 2차전지테마 ETF를 찾을 수 없습니다.");
            return;
        }

        etfComponentRepository.deleteAllByEtfId(etf.getId());

        List<EtfComponent> components = Arrays.asList(
                createComponent(etf.getId(), "051910", "LG화학", 15.2),
                createComponent(etf.getId(), "006400", "삼성SDI", 14.8),
                createComponent(etf.getId(), "373220", "LG에너지솔루션", 13.5),
                createComponent(etf.getId(), "066970", "엘앤에프", 8.3),
                createComponent(etf.getId(), "096770", "SK이노베이션", 7.9),
                createComponent(etf.getId(), "247540", "에코프로비엠", 7.2),
                createComponent(etf.getId(), "086520", "에코프로", 6.8),
                createComponent(etf.getId(), "005070", "코스모신소재", 5.4),
                createComponent(etf.getId(), "020150", "일진머티리얼즈", 4.9),
                createComponent(etf.getId(), "278280", "천보", 4.2)
        );

        etfComponentRepository.saveAll(components);
        log.info("TIGER 2차전지테마 구성종목 {} 건 등록 완료", components.size());
    }

    /**
     * 5. KODEX 코스닥150 (코스닥 대표 ETF)
     */
    private void initializeKodexKosdaq150() {
        Etf etf = etfRepository.findByName("KODEX 코스닥150").orElse(null);
        if (etf == null) {
            log.warn("KODEX 코스닥150 ETF를 찾을 수 없습니다.");
            return;
        }

        etfComponentRepository.deleteAllByEtfId(etf.getId());

        List<EtfComponent> components = Arrays.asList(
                createComponent(etf.getId(), "086520", "에코프로", 9.2),
                createComponent(etf.getId(), "247540", "에코프로비엠", 7.8),
                createComponent(etf.getId(), "263750", "펄어비스", 4.5),
                createComponent(etf.getId(), "196170", "알테오젠", 4.2),
                createComponent(etf.getId(), "357780", "솔브레인", 3.8),
                createComponent(etf.getId(), "039030", "이오테크닉스", 3.5),
                createComponent(etf.getId(), "112040", "위메이드", 3.2),
                createComponent(etf.getId(), "041510", "에스엠", 2.9),
                createComponent(etf.getId(), "293490", "카카오게임즈", 2.7),
                createComponent(etf.getId(), "108860", "셀바스AI", 2.5)
        );

        etfComponentRepository.saveAll(components);
        log.info("KODEX 코스닥150 구성종목 {} 건 등록 완료", components.size());
    }

    private EtfComponent createComponent(Long etfId, String stockCode, String stockName, Double weight) {
        return EtfComponent.builder()
                .etfId(etfId)
                .stockCode(stockCode)
                .stockName(stockName)
                .weight(weight)
                .build();
    }
}