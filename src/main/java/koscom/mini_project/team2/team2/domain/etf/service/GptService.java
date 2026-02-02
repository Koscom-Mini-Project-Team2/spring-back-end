package koscom.mini_project.team2.team2.domain.etf.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelOption;
import jakarta.transaction.Transactional;
import koscom.mini_project.team2.team2.domain.etf.dto.OpenAiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class GptService {

    @Value("${openai_api_key}")
    private String openAiApiKey;

    @Value("${openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${openai.model:gpt-5.2}")
    private String model;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GptService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000) // 연결 타임아웃
                .responseTimeout(Duration.ofSeconds(60));

        this.restClient = RestClient.builder()
                .requestFactory(new ReactorClientHttpRequestFactory(httpClient))
                .build();
    }

    public String callGpt(String prompt) {

        // Responses API 요청 바디 (최소 형태)
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("input", prompt);

        // OpenAI Responses API 호출
        String apiResponse = restClient.post()
                .uri(baseUrl + "/responses")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        if (apiResponse == null) {
            throw new IllegalStateException("OpenAI response is null");
        }

        OpenAiResponse response = null;
        try {
            response = objectMapper.readValue(apiResponse, OpenAiResponse.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // message(text)만 추출
        String message = response.output().stream()
                .filter(o -> "message".equals(o.type()))
                .findFirst()
                .flatMap(o -> o.content().stream()
                        .filter(c -> "output_text".equals(c.type()))
                        .findFirst())
                .map(OpenAiResponse.Content::text)
                .orElseThrow(() -> new IllegalStateException("No message text found"));

        return message;
    }
}
