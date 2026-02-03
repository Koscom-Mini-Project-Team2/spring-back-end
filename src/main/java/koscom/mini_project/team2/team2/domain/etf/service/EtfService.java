package koscom.mini_project.team2.team2.domain.etf.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import koscom.mini_project.team2.team2.domain.etf.dto.*;
import koscom.mini_project.team2.team2.domain.etf.entity.Etf;
import koscom.mini_project.team2.team2.domain.etf.repository.EtfRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class EtfService {

    private static final Pattern INTEGER_PATTERN = Pattern.compile("-?\\d+");

    private final EtfRepository etfRepository;
    private final GptService gptService;
    private final ObjectMapper mapper;

    public EtfResponse create(ETFCreateRequest request) {
        Etf etf = Etf.builder()
                .name(request.name())
                .fltRt(request.fltRt())
                .riskLevel(request.riskLevel())
                .category(request.category())
                .description(request.description())
                .build();
        Etf saved = etfRepository.save(etf);
        return EtfResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public EtfResponse findById(Long id) {
        Etf etf = etfRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Etf not found. id=" + id));
        return EtfResponse.from(etf);
    }

    @Transactional
    public EtfResponse update(Long id,  EtfUpdateRequest request) {
        System.out.println("[TAG] : " + request);
        Etf etf = etfRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Etf not found. id=" + id));
        etf.setStockList(request.stockList());
        return EtfResponse.from(etf);
    }

    @Transactional(readOnly = true)
    public List<EtfResponse> findAll() {
        return etfRepository.findAll().stream()
                .map(EtfResponse::from)
                .toList();
    }

    public void delete(Long id) {
        if (!etfRepository.existsById(id)) {
            throw new EntityNotFoundException("Dummy not found. id=" + id);
        }
        etfRepository.deleteById(id);
    }

    public EtfRecommendResponseDto recommend(EtfRecommendRequest request) {

        Integer fltRt = parseIntegerFromGpt(
            gptService.callGpt(buildFltRtPrompt(request))
        );

        Integer riskLevel = parseIntegerFromGpt(
            gptService.callGpt(buildRiskLevelPrompt(request))
        );

        List<Etf> etfs = etfRepository.searchEtfs(fltRt, riskLevel);

        EtfRecommendResponseDto dto = callGptWithRetry(buildRecommendPrompt(request, etfs), 10);

        System.out.println("TAG[DTO]: " + dto.toString());

        return dto;

    }

    public EtfRecommendResponseDto callGptWithRetry(String prompt, int maxRetries) {

        String raw = null;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                raw = gptService.callGpt(prompt);

                EtfRecommendResponseDto dto =
                        mapper.readValue(raw, EtfRecommendResponseDto.class);

                return dto;

            } catch (Exception e) {
                lastException = e;

                System.err.println(
                        "[GPT_PARSE_RETRY] attempt=" + attempt +
                                ", reason=" + e.getClass().getSimpleName()
                );

                // 마지막 시도면 종료
                if (attempt == maxRetries) {
                    break;
                }

                // 🔥 다음 시도부터는 "포맷 복구 프롬프트" 사용
                prompt = buildRepairJsonPrompt(raw);
            }
        }

        throw new IllegalStateException(
                "Failed to get valid GPT response after " + maxRetries + " attempts",
                lastException
        );
    }

    private String buildRepairJsonPrompt(String raw) {
        return """
            너는 JSON 포맷 복구기다.
            
            아래 입력 텍스트를 반드시 지정된 JSON 스키마에 맞게 변환하라.
            설명, 문장, 주석, 코드블록, 마크다운 없이 JSON만 출력하라.
            
            반환 JSON 스키마:
            {
              "investmentProfile": "STRING",
              "etfRiskScore": 0,
              "dividendScore": 0,
              "expectedTotalReturn": 0,
              "portfolioWeights": [0,0,0,0,0],
              "etfs": [
                {"id":0,"name":"","fltRt":0,"riskLevel":0,"category":"","description":""}
              ],
              "reasonSummary": "LINE1\\nLINE2\\nLINE3"
            }
            
            규칙:
            - 숫자는 정수만 허용
            - portfolioWeights는 반드시 길이 5, 합은 100
            - etfs는 반드시 5개
            - 스키마 외 필드 출력 금지
            
            입력 텍스트:
        """ + raw;
    }

    private String buildRecommendPrompt(EtfRecommendRequest request, List<Etf> candidates) {
        StringBuilder sb = new StringBuilder();

        String candidatesJson = toEtfCandidateJson(candidates);

        sb.append("너는 금융 투자 추천 엔진이다.\n");
        sb.append("아래 [사용자 서베이 응답]과 [후보 ETF 목록]을 기반으로, 후보 ETF 중에서만 정확히 5개 ETF를 추천하라.\n\n");

        // ✅ 출력 강제: JSON ONLY
        sb.append("출력 규칙(매우 중요):\n");
        sb.append("1) 반드시 JSON만 출력한다. 설명/문장/코드블록/마크다운/따옴표 밖 텍스트를 절대 출력하지 마라.\n");
        sb.append("2) JSON의 필드명은 아래 스키마와 EXACTLY 동일해야 한다.\n");
        sb.append("3) 숫자 필드는 정수만 허용한다(소수 금지).\n");
        sb.append("4) etfs는 반드시 5개이며, 후보 ETF 목록에 있는 객체만 그대로 포함한다(임의로 생성 금지).\n");
        sb.append("5) portfolioWeights는 반드시 길이 5의 정수 리스트이고, 합은 정확히 100이어야 한다.\n");
        sb.append("6) reasonSummary는 정확히 3줄(줄바꿈 2회 포함)로 작성한다.\n\n");

        // ✅ 스키마 고정
        sb.append("반환 JSON 스키마(이 형태 그대로):\n");
        sb.append("{\n");
        sb.append("  \"investmentProfile\": \"STRING\",\n");
        sb.append("  \"etfRiskScore\": 0,\n");
        sb.append("  \"dividendScore\": 0,\n");
        sb.append("  \"expectedTotalReturn\": 0,\n");
        sb.append("  \"portfolioWeights\": [0,0,0,0,0],\n");
        sb.append("  \"etfs\": [\n");
        sb.append("    {\"id\":0,\"name\":\"\",\"fltRt\":0,\"riskLevel\":0,\"category\":\"\",\"description\":\"\"}\n");
        sb.append("  ],\n");
        sb.append("  \"reasonSummary\": \"LINE1\\nLINE2\\nLINE3\"\n");
        sb.append("}\n\n");

        // ✅ 점수 가이드 (신뢰성)
        sb.append("점수 산정 가이드:\n");
        sb.append("- etfRiskScore(0~100): 사용자의 위험 감내 수준이 높을수록 높게, 보수적일수록 낮게 산정.\n");
        sb.append("- dividendScore(0~100): 배당/현금흐름 선호가 강할수록 높게 산정.\n");
        sb.append("- expectedTotalReturn: 추정 총 수익률을 정수로만 제시(예: 8). 과장 금지.\n\n");

        // ✅ 입력 데이터
        sb.append("[사용자 서베이 응답]\n");
        sb.append("- 질의:\n");
        appendQaList(sb, request.qaList());

        sb.append("\n[후보 ETF 목록(JSON)]\n");
        sb.append(candidatesJson);

        sb.append("\n\n추가 제약:\n");
        sb.append("- 추천 ETF 5개는 서로 중복되지 않아야 한다.\n");
        sb.append("- 포트폴리오는 분산 원칙을 지키되, 사용자가 선택한 관심 테마/목적을 우선 반영한다.\n");
        sb.append("- 후보 목록의 description을 활용하여 추천 이유를 구성하되, 광고성 문구는 금지한다.\n");

        return sb.toString();
    }

    private String toEtfCandidateJson(List<Etf> candidates) {
        try {
            List<Etf> etfs = candidates.stream()
                    .map(e -> new Etf(
                            e.getId(),
                            e.getName(),
                            e.getFltRt(),
                            e.getRiskLevel(),
                            e.getCategory(),
                            e.getDescription(),
                            e.getStockList()
                    ))
                    .toList();

            return mapper.writeValueAsString(etfs);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize ETF candidates", e);
        }
    }


    private String buildPickOneEtfPrompt(EtfRecommendRequest request, String etfsStr) {
        StringBuilder sb = new StringBuilder();

        // 1️⃣ 역할과 목적 명시

        sb.append("아래는 ETF 정보들이야\n");
        sb.append(etfsStr + "\n");

        // 6️⃣ 실제 사용자 입력
        sb.append("아래는 사용자의 투자 성향 설문 응답이다.\n");

        sb.append("\n- 사용자 질의:\n");
        appendQaList(sb, request.qaList());

        return sb.toString();
    }

    private String buildRiskLevelPrompt(EtfRecommendRequest request) {
        StringBuilder sb = new StringBuilder();

        // 1️⃣ 역할과 목적 명시
        sb.append("너는 금융 투자 성향 분석 AI다.\n");
        sb.append("아래 설문 응답을 바탕으로 사용자의 감내 가능한 위험 수준(risk_level)을 ");
        sb.append("0~9 사이의 정수 하나로 평가해라.\n\n");

        // 2️⃣ risk_level 정의 (신뢰성 핵심)
        sb.append("risk_level 정의:\n");
        sb.append("0 = 매우 안정적 (손실 회피 강함, 변동성 거의 허용 안 함)\n");
        sb.append("2 = 저위험 혼합/지수 중심\n");
        sb.append("5 = 일반 주식형, 중립적 위험 감내\n");
        sb.append("7 = 테마·섹터 집중, 변동성 감내\n");
        sb.append("9 = 레버리지·투기성, 매우 공격적\n\n");

        // 3️⃣ 판단 기준 명시 (AI 주관 차단)
        sb.append("판단 기준:\n");
        sb.append("- 손실 발생 시 행동(즉시 매도 vs 유지/추가 매수)\n");
        sb.append("- 하락장에서의 대응 방식\n");
        sb.append("- 이익 발생 시 실현 성향\n");
        sb.append("- 투자 기간(단기 vs 장기)\n");
        sb.append("- 특정 테마/섹터에 대한 집중 성향\n");
        sb.append("- 감정적·주관적 표현은 사용하지 말 것\n\n");

        // 4️⃣ 점수 산정 가이드
        sb.append("점수 산정 가이드:\n");
        sb.append("- 손실 회피, 단기, 공포 반응 위주면 0~2\n");
        sb.append("- 장기 보유, 계획 유지, 중립적 반응이면 3~5\n");
        sb.append("- 하락장 추가 매수, 테마 집중, 공격적 반응이면 6~8\n");
        sb.append("- 레버리지·극단적 선택 성향이 명확할 경우만 9\n\n");

        // 5️⃣ 출력 형식 강제
        sb.append("출력 형식:\n");
        sb.append("- 반드시 정수 하나만 출력\n");
        sb.append("- 설명, 문장, 기호, 공백, 줄바꿈 없이 숫자만 출력\n");
        sb.append("- 출력 예시:\n");
        sb.append("3\n\n");

        // 6️⃣ 실제 사용자 입력
        sb.append("아래는 사용자의 투자 성향 설문 응답이다.\n");

        sb.append("\n- 사용자 질의:\n");
        appendQaList(sb, request.qaList());

        return sb.toString();
    }


    private String buildFltRtPrompt(EtfRecommendRequest request) {
        StringBuilder sb = new StringBuilder();

        // 1️⃣ 역할과 목표 명확화
        sb.append("너는 금융 투자 성향 분석 AI다.\n");
        sb.append("아래 설문 응답을 바탕으로 사용자가 감내할 수 있는 ");
        sb.append("시장 변동성 허용 수준(volatility_tolerance)을 하나의 정수 값으로 평가해라.\n\n");

        // 2️⃣ 점수 정의 (신뢰성 핵심)
        sb.append("평가 기준:\n");
        sb.append("- 출력 값은 -10000 ~ 10000 사이의 정수\n");
        sb.append("- 음수일수록 변동성 회피 성향이 강함\n");
        sb.append("- 0에 가까울수록 중립적 성향\n");
        sb.append("- 양수일수록 변동성을 감내하거나 선호함\n\n");

        // 3️⃣ 판단 규칙 (AI의 주관 차단)
        sb.append("판단 규칙:\n");
        sb.append("- 투자 기간, 손실 상황에서의 반응, 하락장 행동, 이익 실현 성향을 종합적으로 고려\n");
        sb.append("- 단기 손실에 민감하거나 공포 반응이 강하면 음수 방향으로 평가\n");
        sb.append("- 하락장에서 추가 매수, 장기 보유, 변동성 감내 행동이 많을수록 양수 방향으로 평가\n");
        sb.append("- 개인 의견이나 설명 없이 수치만 산출\n\n");

        // 4️⃣ 출력 형식 강제 (매우 중요)
        sb.append("출력 형식:\n");
        sb.append("- 반드시 정수 하나만 출력\n");
        sb.append("- 설명, 문장, 기호, 공백, 줄바꿈 없이 숫자만 출력\n");
        sb.append("- 예시 출력:\n");
        sb.append("3500\n\n");

        // 5️⃣ 실제 입력 데이터
        sb.append("아래는 사용자의 투자 성향 설문 응답이다.\n");

        sb.append("\n- 사용자 질의:\n");
        appendQaList(sb, request.qaList());

        return sb.toString();
    }

    private void appendQaList(StringBuilder sb, List<EtfRecommendRequest.QaItem> list) {
        if (list == null || list.isEmpty()) {
            sb.append("  (없음)\n");
            return;
        }
        int i = 1;
        for (var qa : list) {
            sb.append("  ").append(i++).append(") Q: ").append(safe(qa.question()))
                    .append(" | A: ").append(safe(qa.answer())).append("\n");
        }
    }

    private String safe(String s) {
        return (s == null) ? "" : s.replace("\n", " ").trim();
    }

    /**
     * GPT 응답 문자열에서 정수값을 추출하여 Integer로 반환
     *
     * @param gptResult GPT API로부터 받은 문자열
     * @return Integer 값
     * @throws IllegalArgumentException 정수를 추출할 수 없는 경우
     */
    public static Integer parseIntegerFromGpt(String gptResult) {
        if (gptResult == null || gptResult.isBlank()) {
            throw new IllegalArgumentException("GPT result is null or empty");
        }

        Matcher matcher = INTEGER_PATTERN.matcher(gptResult);

        if (!matcher.find()) {
            throw new IllegalArgumentException(
                    "No integer value found in GPT result: " + gptResult
            );
        }

        try {
            return Integer.valueOf(matcher.group());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Failed to parse integer from GPT result: " + gptResult, e
            );
        }


    }

}