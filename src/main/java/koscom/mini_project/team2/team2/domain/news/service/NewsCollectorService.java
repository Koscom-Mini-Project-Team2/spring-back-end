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
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
        log.info("====================================");
        log.info("ETF 뉴스 수집 시작: {}", etf.getName());
        log.info("ETF ID: {}", etf.getId());
        log.info("====================================");

        // 1. ETF 구성 종목 조회
        List<EtfComponent> components = etfComponentRepository.findByEtfIdOrderByWeightDesc(etf.getId());

        if (components.isEmpty()) {
            log.warn("ETF 구성 종목이 없습니다: {}", etf.getName());
            return Collections.emptyList();
        }

        log.info("구성 종목 총 {}개 발견", components.size());

        // 2. 상위 종목들의 뉴스 수집 (예: 상위 5개)
        List<EtfNews> allNews = new ArrayList<>();
        int maxComponents = Math.min(5, components.size());

        log.info("상위 {}개 종목 선택", maxComponents);

        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);
        String sdate = weekAgo.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String edate = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        log.info("조회 기간: {} ~ {}", sdate, edate);

        for (int i = 0; i < maxComponents; i++) {
            EtfComponent component = components.get(i);
            log.info("----------------------------------------");
            log.info("종목 뉴스 수집 [{}/{}]: {} ({})",
                    i + 1, maxComponents, component.getStockName(), component.getStockCode());

            try {
                List<KoscomNewsItem> newsItems = collectNewsFromKoscom(
                        component.getStockCode(),
                        sdate,
                        edate
                );

                log.info("수집된 뉴스: {}건", newsItems.size());

                List<EtfNews> savedNews = saveNewsItems(etf, component, newsItems);
                allNews.addAll(savedNews);

                log.info("저장된 뉴스: {}건", savedNews.size());

                // API 호출 제한 고려 (필요시)
                Thread.sleep(100);

            } catch (Exception e) {
                log.error("종목 뉴스 수집 실패: {} - {}", component.getStockName(), e.getMessage());
                log.error("에러 상세:", e);
            }
        }

        log.info("========================================");
        log.info("ETF 뉴스 수집 완료: 총 {} 건 저장", allNews.size());
        log.info("========================================");
        return allNews;
    }

    /**
     * KOSCOM API로 종목별 뉴스 수집
     */
    private List<KoscomNewsItem> collectNewsFromKoscom(String jcode, String sdate, String edate) {
        log.info(">>> KOSCOM API 호출 준비");
        log.info(">>> cust_id: {}", koscomCustId != null && !koscomCustId.isEmpty() ? "존재" : "없음");
        log.info(">>> auth_key: {}", koscomAuthKey != null && !koscomAuthKey.isEmpty() ? "존재 (길이: " + koscomAuthKey.length() + ")" : "없음");
        log.info(">>> jcode: {}", jcode);
        log.info(">>> sdate: {}", sdate);
        log.info(">>> edate: {}", edate);

        try {
            Map<String, String> requestBody = Map.of(
                    "cust_id", koscomCustId,
                    "auth_key", koscomAuthKey,
                    "jcode", jcode,
                    "sdate", sdate,
                    "edate", edate,
                    "dcnt", "20"
            );

            log.info(">>> 요청 바디: {}", requestBody);
            log.info(">>> API 엔드포인트: https://checkapi.koscom.co.kr/news/news/news_jong");
            log.info(">>> Content-Type: application/x-www-form-urlencoded");

            log.info(">>> WebClient POST 요청 시작...");

            String response = webClient.post()
                    .uri("https://checkapi.koscom.co.kr/news/news/news_jong")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(convertToMultiValueMap(requestBody)))
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnSubscribe(s -> log.info(">>> 구독 시작"))
                    .doOnNext(r -> log.info(">>> 응답 수신: {} bytes", r != null ? r.length() : 0))
                    .doOnError(e -> {
                        log.error(">>> 에러 발생: {}", e.getClass().getName());
                        log.error(">>> 에러 메시지: {}", e.getMessage());
                        if (e instanceof WebClientResponseException) {
                            WebClientResponseException wcre = (WebClientResponseException) e;
                            log.error(">>> HTTP 상태: {}", wcre.getStatusCode());
                            log.error(">>> 응답 바디: {}", wcre.getResponseBodyAsString());
                            log.error(">>> 응답 헤더: {}", wcre.getHeaders());
                        }
                    })
                    .block();

            log.info(">>> API 호출 완료");

            if (response == null || response.isEmpty()) {
                log.warn(">>> KOSCOM API 응답이 비어있습니다.");
                return Collections.emptyList();
            }

            log.info(">>> 응답 길이: {} bytes", response.length());
            log.info(">>> 응답 내용 (처음 500자): {}",
                    response.length() > 500 ? response.substring(0, 500) + "..." : response);

            return parseKoscomResponse(response);

        } catch (Exception e) {
            log.error(">>> KOSCOM API 호출 중 예외 발생");
            log.error(">>> 예외 타입: {}", e.getClass().getName());
            log.error(">>> 예외 메시지: {}", e.getMessage());
            log.error(">>> 스택 트레이스:", e);
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
        log.info(">>> 응답 파싱 시작");

        try {
            log.debug(">>> 전체 응답: {}", jsonResponse);

            Gson gson = new Gson();
            JsonObject root = gson.fromJson(jsonResponse, JsonObject.class);

            log.info(">>> JSON 파싱 완료");

            // success 필드 확인
            if (root.get("success") == null) {
                log.error(">>> 'success' 필드가 없음");
                log.error(">>> 응답 구조: {}", root.keySet());
                return Collections.emptyList();
            }

            boolean success = root.get("success").getAsBoolean();
            log.info(">>> success: {}", success);

            if (!success) {
                if (root.has("message")) {
                    JsonObject message = root.getAsJsonObject("message");
                    String errorMsg = message.has("desc") ? message.get("desc").getAsString() : "알 수 없는 오류";
                    log.error(">>> KOSCOM API 오류: {}", errorMsg);
                    log.error(">>> 전체 message 객체: {}", message);
                } else {
                    log.error(">>> KOSCOM API 응답 success=false, message 필드 없음");
                }
                return Collections.emptyList();
            }

            // results 배열 확인
            if (!root.has("results")) {
                log.warn(">>> KOSCOM API 응답에 results 필드가 없습니다.");
                log.warn(">>> 응답에 있는 필드들: {}", root.keySet());
                return Collections.emptyList();
            }

            JsonArray results = root.getAsJsonArray("results");
            log.info(">>> results 배열 크기: {}", results.size());

            if (results.size() == 0) {
                log.info(">>> 수집된 뉴스가 없습니다.");
                return Collections.emptyList();
            }

            List<KoscomNewsItem> newsItems = new ArrayList<>();

            for (int i = 0; i < results.size(); i++) {
                try {
                    JsonElement element = results.get(i);
                    JsonObject item = element.getAsJsonObject();

                    // 필수 필드만 체크하고, 선택적 필드는 null-safe하게 처리
                    if (!item.has("DATE") || !item.has("CODE") || !item.has("TITLE")) {
                        log.warn(">>> 뉴스 항목 {}번: 필수 필드가 없는 항목 건너뜀", i);
                        log.warn(">>> 항목 내용: {}", item);
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

                    if (i < 3) {  // 처음 3개만 상세 로그
                        log.info(">>> 뉴스 {}번: {}", i, newsItem.getTitle());
                    }

                } catch (Exception e) {
                    log.warn(">>> 뉴스 항목 {}번 파싱 실패: {}", i, e.getMessage());
                    log.warn(">>> 에러 상세:", e);
                    continue;
                }
            }

            log.info(">>> 파싱 완료: {} 건", newsItems.size());
            return newsItems;

        } catch (Exception e) {
            log.error(">>> 응답 파싱 실패: {}", e.getMessage());
            log.error(">>> 파싱 에러 상세:", e);
            log.error(">>> 파싱 실패한 응답 내용: {}", jsonResponse);
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
     * 뉴스 아이템을 DB에 저장 (중복 제거 강화)
     */
    private List<EtfNews> saveNewsItems(Etf etf, EtfComponent component, List<KoscomNewsItem> newsItems) {
        log.info(">>> 뉴스 저장 시작: {}건", newsItems.size());

        List<EtfNews> savedNews = new ArrayList<>();

        // 이미 처리한 뉴스 제목을 추적 (같은 배치 내 중복 방지)
        Set<String> processedTitles = new HashSet<>();

        int duplicateInBatch = 0;
        int duplicateUrl = 0;
        int duplicateTitle = 0;
        int savedCount = 0;

        for (KoscomNewsItem item : newsItems) {
            // 1. 같은 배치 내에서 제목 중복 체크
            if (processedTitles.contains(item.getTitle())) {
                log.debug(">>> 배치 내 중복 제목 건너뜀: {}", item.getTitle());
                duplicateInBatch++;
                continue;
            }

            // 2. URL 중복 체크 (기존)
            String newsUrl = String.format("https://news.koscom.co.kr/news/%s", item.getCode());
            if (etfNewsRepository.existsByEtfIdAndUrl(etf.getId(), newsUrl)) {
                log.debug(">>> DB 중복 URL 건너뜀: {}", newsUrl);
                duplicateUrl++;
                continue;
            }

            // 3. 제목 중복 체크 (DB)
            if (etfNewsRepository.existsByEtfIdAndTitle(etf.getId(), item.getTitle())) {
                log.debug(">>> DB 중복 제목 건너뜀: {}", item.getTitle());
                duplicateTitle++;
                continue;
            }

            // 날짜/시간 파싱
            LocalDateTime publishedAt = parseDateTime(item.getDate(), item.getTime());

            // 뉴스 저장
            EtfNews news = EtfNews.builder()
                    .etfId(etf.getId())
                    .title(item.getTitle())
                    .content(String.format("[%s] 관련 뉴스", component.getStockName()))
                    .url(newsUrl)
                    .source("KOSCOM")
                    .publishedAt(publishedAt)
                    .analyzed(false)
                    .alertTriggered(false)
                    .build();

            savedNews.add(etfNewsRepository.save(news));
            processedTitles.add(item.getTitle());
            savedCount++;

            log.debug(">>> 뉴스 저장 완료: {}", item.getTitle());
        }

        log.info(">>> 저장 완료: {}건 저장", savedCount);
        log.info(">>> 중복 제거: 배치내={}건, URL={}건, 제목={}건",
                duplicateInBatch, duplicateUrl, duplicateTitle);

        return savedNews;
    }

    private LocalDateTime parseDateTime(String date, String time) {
        try {
            // date: "20230312", time: "143000"
            String dateTimeStr = date + time;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            return LocalDateTime.parse(dateTimeStr, formatter);
        } catch (Exception e) {
            log.warn(">>> 날짜 파싱 실패: {} {} - {}", date, time, e.getMessage());
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
