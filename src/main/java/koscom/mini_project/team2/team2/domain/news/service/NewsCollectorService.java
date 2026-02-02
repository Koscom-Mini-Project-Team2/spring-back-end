package koscom.mini_project.team2.team2.domain.news.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import koscom.mini_project.team2.team2.domain.etf.entity.Etf;
import koscom.mini_project.team2.team2.domain.news.entity.EtfComponent;
import koscom.mini_project.team2.team2.domain.news.entity.EtfNews;
import koscom.mini_project.team2.team2.domain.news.repository.EtfComponentRepository;
import koscom.mini_project.team2.team2.domain.news.repository.EtfNewsRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class NewsCollectorService {

    @Value("${koscom.cust-id}")
    private String koscomCustId;

    @Value("${koscom.auth-key}")
    private String koscomAuthKey;

    private final EtfNewsRepository etfNewsRepository;
    private final EtfComponentRepository etfComponentRepository;
    private final WebClient webClient;

    public NewsCollectorService(EtfNewsRepository etfNewsRepository,
                                EtfComponentRepository etfComponentRepository,
                                WebClient.Builder webClientBuilder) {
        this.etfNewsRepository = etfNewsRepository;
        this.etfComponentRepository = etfComponentRepository;
        this.webClient = webClientBuilder.build();
    }

    /**
     * ETF 구성 종목들의 뉴스 수집
     */
    public List<EtfNews> collectAndSaveNews(Etf etf) {
        log.info("ETF 뉴스 수집 시작: {}", etf.getName());

        // 1. ETF 구성 종목 조회
        List<EtfComponent> components = etfComponentRepository.findByEtfIdOrderByWeightDesc(etf.getId());

        if (components.isEmpty()) {
            log.warn("ETF 구성 종목이 없습니다: {}", etf.getName());
            return Collections.emptyList();
        }

        // 2. 상위 종목들의 뉴스 수집 (예: 상위 5개)
        List<EtfNews> allNews = new ArrayList<>();
        int maxComponents = Math.min(5, components.size());

        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);
        String sdate = weekAgo.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String edate = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        for (int i = 0; i < maxComponents; i++) {
            EtfComponent component = components.get(i);
            log.info("종목 뉴스 수집: {} ({})", component.getStockName(), component.getStockCode());

            try {
                List<KoscomNewsItem> newsItems = collectNewsFromKoscom(
                        component.getStockCode(),
                        sdate,
                        edate
                );

                List<EtfNews> savedNews = saveNewsItems(etf, component, newsItems);
                allNews.addAll(savedNews);

                // API 호출 제한 고려 (필요시)
                Thread.sleep(100);

            } catch (Exception e) {
                log.error("종목 뉴스 수집 실패: {} - {}", component.getStockName(), e.getMessage());
            }
        }

        log.info("ETF 뉴스 수집 완료: {} 건 저장", allNews.size());
        return allNews;
    }

    /**
     * KOSCOM API로 종목별 뉴스 수집
     */
    private List<KoscomNewsItem> collectNewsFromKoscom(String jcode, String sdate, String edate) {
        try {
            Map<String, String> requestBody = Map.of(
                    "cust_id", koscomCustId,
                    "auth_key", koscomAuthKey,
                    "jcode", jcode,
                    "sdate", sdate,
                    "edate", edate,
                    "dcnt", "20"
            );

            log.info("KOSCOM API 호출: jcode={}, sdate={}, edate={}", jcode, sdate, edate);

            String response = webClient.post()
                    .uri("https://checkapi.koscom.co.kr/news/news/news_jong")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(convertToMultiValueMap(requestBody)))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response == null || response.isEmpty()) {
                log.warn("KOSCOM API 응답이 비어있습니다.");
                return Collections.emptyList();
            }

            log.debug("KOSCOM API 응답 길이: {} bytes", response.length());

            return parseKoscomResponse(response);

        } catch (Exception e) {
            log.error("KOSCOM API 호출 실패: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private MultiValueMap<String, String> convertToMultiValueMap(Map<String, String> map) {
        MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap<>();
        map.forEach(multiValueMap::add);
        return multiValueMap;
    }

    /**
     * KOSCOM API 응답 파싱 (null-safe 버전)
     */
    private List<KoscomNewsItem> parseKoscomResponse(String jsonResponse) {
        try {
            log.debug("KOSCOM API 응답: {}", jsonResponse);  // 디버깅용 로그 추가

            Gson gson = new Gson();
            JsonObject root = gson.fromJson(jsonResponse, JsonObject.class);

            // success 필드 확인
            if (root.get("success") == null || !root.get("success").getAsBoolean()) {
                if (root.has("message")) {
                    JsonObject message = root.getAsJsonObject("message");
                    String errorMsg = message.has("desc") ? message.get("desc").getAsString() : "알 수 없는 오류";
                    log.error("KOSCOM API 오류: {}", errorMsg);
                } else {
                    log.error("KOSCOM API 응답에 success 필드가 없거나 false입니다.");
                }
                return Collections.emptyList();
            }

            // results 배열 확인
            if (!root.has("results")) {
                log.warn("KOSCOM API 응답에 results 필드가 없습니다.");
                return Collections.emptyList();
            }

            JsonArray results = root.getAsJsonArray("results");
            if (results.size() == 0) {
                log.info("수집된 뉴스가 없습니다.");
                return Collections.emptyList();
            }

            List<KoscomNewsItem> newsItems = new ArrayList<>();

            for (JsonElement element : results) {
                try {
                    JsonObject item = element.getAsJsonObject();

                    // 필수 필드만 체크하고, 선택적 필드는 null-safe하게 처리
                    if (!item.has("DATE") || !item.has("CODE") || !item.has("TITLE")) {
                        log.warn("필수 필드가 없는 뉴스 항목 건너뜀");
                        continue;
                    }

                    KoscomNewsItem newsItem = KoscomNewsItem.builder()
                            .date(getStringOrDefault(item, "DATE", ""))
                            .code(getStringOrDefault(item, "CODE", ""))
                            .time(getStringOrDefault(item, "TIME", "000000"))
                            .title(getStringOrDefault(item, "TITLE", "제목 없음"))
                            .bigcd(getStringOrDefault(item, "BIGCD", "1"))
                            .langcd(getStringOrDefault(item, "LANGCD", ""))
                            .srccd(getStringOrDefault(item, "SRCCD", ""))
                            .mtvcd(getIntOrDefault(item, "MTVCD", 0))
                            .impcd(getStringOrDefault(item, "IMPCD", ""))
                            .build();

                    newsItems.add(newsItem);

                } catch (Exception e) {
                    log.warn("뉴스 항목 파싱 실패: {}", e.getMessage());
                    continue;
                }
            }

            log.info("파싱 완료: {} 건", newsItems.size());
            return newsItems;

        } catch (Exception e) {
            log.error("응답 파싱 실패: {}", e.getMessage(), e);
            log.error("파싱 실패한 응답 내용: {}", jsonResponse);
            return Collections.emptyList();
        }
    }

    // Helper 메서드들 추가
    private String getStringOrDefault(JsonObject obj, String key, String defaultValue) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return defaultValue;
    }

    private Integer getIntOrDefault(JsonObject obj, String key, Integer defaultValue) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            try {
                return obj.get(key).getAsInt();
            } catch (Exception e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * 뉴스 아이템을 DB에 저장
     */
    private List<EtfNews> saveNewsItems(Etf etf, EtfComponent component, List<KoscomNewsItem> newsItems) {
        List<EtfNews> savedNews = new ArrayList<>();

        for (KoscomNewsItem item : newsItems) {
            // URL 생성 (뉴스코드 기반)
            String newsUrl = String.format("https://news.koscom.co.kr/news/%s", item.getCode());

            // 중복 체크
            if (etfNewsRepository.existsByEtfIdAndUrl(etf.getId(), newsUrl)) {
                continue;
            }

            // 날짜/시간 파싱
            LocalDateTime publishedAt = parseDateTime(item.getDate(), item.getTime());

            // 뉴스 저장
            EtfNews news = EtfNews.builder()
                    .etfId(etf.getId())
                    .title(item.getTitle())
                    .content(String.format("[%s] 관련 뉴스", component.getStockName()))  // 종목명 포함
                    .url(newsUrl)
                    .source("KOSCOM")
                    .publishedAt(publishedAt)
                    .analyzed(false)
                    .alertTriggered(false)
                    .build();

            savedNews.add(etfNewsRepository.save(news));
        }

        return savedNews;
    }

    private LocalDateTime parseDateTime(String date, String time) {
        try {
            // date: "20230312", time: "143000"
            String dateTimeStr = date + time;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            return LocalDateTime.parse(dateTimeStr, formatter);
        } catch (Exception e) {
            log.warn("날짜 파싱 실패: {} {}", date, time);
            return LocalDateTime.now();
        }
    }
}

/**
 * KOSCOM 뉴스 응답 DTO
 */
@Getter
@Setter
@Builder
class KoscomNewsItem {
    private String date;      // 일자 (YYYYMMDD)
    private String code;      // 뉴스코드
    private String time;      // 시간 (HHMMSS)
    private String title;     // 제목
    private String bigcd;     // 대분류코드 (1:뉴스, 2:공시)
    private String langcd;    // 언어코드
    private String srccd;     // 원천소스코드
    private Integer mtvcd;    // 뉴스원코드
    private String impcd;     // 중요기사구분
}

