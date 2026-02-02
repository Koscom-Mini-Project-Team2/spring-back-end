package koscom.mini_project.team2.team2.domain.news.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import koscom.mini_project.team2.team2.domain.etf.entity.Etf;
import koscom.mini_project.team2.team2.domain.news.entity.AlertHistory;
import koscom.mini_project.team2.team2.domain.news.entity.EtfNews;
import koscom.mini_project.team2.team2.domain.news.repository.AlertHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender mailSender;
    private final AlertHistoryRepository alertHistoryRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * 이메일 알림 발송
     */
    public boolean sendEmailAlert(Etf etf, String toEmail, String summary, List<EtfNews> newsList, Long memberId) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(String.format("[ETF 중요 알림] %s 관련 주요 뉴스 발생", etf.getName()));

            String htmlContent = createEmailContent(etf, summary, newsList);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("이메일 발송 성공: {} -> {}", etf.getName(), toEmail);

            // 알림 이력 저장
            saveAlertHistory(etf, memberId, "EMAIL", toEmail, summary, true, null);

            return true;

        } catch (Exception e) {
            log.error("이메일 발송 실패: {}", e.getMessage(), e);

            // 실패 이력 저장
            saveAlertHistory(etf, memberId, "EMAIL", toEmail, summary, false, e.getMessage());

            return false;
        }
    }

    /**
     * 이메일 HTML 콘텐츠 생성
     */
    private String createEmailContent(Etf etf, String summary, List<EtfNews> newsList) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html><head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<style>");
        html.append("body { font-family: 'Malgun Gothic', sans-serif; line-height: 1.6; color: #333; }");
        html.append(".container { max-width: 600px; margin: 0 auto; padding: 20px; }");
        html.append(".header { background-color: #0066cc; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }");
        html.append(".content { background-color: #f9f9f9; padding: 20px; border: 1px solid #ddd; }");
        html.append(".etf-info { background-color: white; padding: 15px; margin: 15px 0; border-left: 4px solid #0066cc; }");
        html.append(".summary { background-color: #fff3cd; padding: 15px; margin: 15px 0; border-radius: 5px; }");
        html.append(".news-item { background-color: white; padding: 15px; margin: 10px 0; border-radius: 5px; border: 1px solid #e0e0e0; }");
        html.append(".news-title { font-weight: bold; color: #0066cc; margin-bottom: 5px; }");
        html.append(".news-meta { font-size: 0.9em; color: #666; margin-bottom: 10px; }");
        html.append(".footer { text-align: center; padding: 20px; color: #666; font-size: 0.9em; }");
        html.append("</style>");
        html.append("</head><body>");

        html.append("<div class='container'>");

        // 헤더
        html.append("<div class='header'>");
        html.append("<h1>🔔 ETF 중요 알림</h1>");
        html.append("</div>");

        html.append("<div class='content'>");

        // ETF 정보
        html.append("<div class='etf-info'>");
        html.append("<h2>").append(etf.getName()).append("</h2>");
        html.append("<p><strong>카테고리:</strong> ").append(etf.getCategory() != null ? etf.getCategory() : "미분류").append("</p>");
        html.append("<p><strong>위험도:</strong> ").append(etf.getRiskLevel() != null ? etf.getRiskLevel() : "-").append(" / 9</p>");
        html.append("<p><strong>전달 대비 변동률:</strong> ").append(etf.getFltRt() != null ? etf.getFltRt() + "%" : "-").append("</p>");
        html.append("</div>");

        // AI 분석 요약
        html.append("<div class='summary'>");
        html.append("<h3>📊 AI 분석 결과</h3>");
        html.append("<p>").append(summary).append("</p>");
        html.append("</div>");

        // 관련 뉴스 목록
        html.append("<h3>📰 관련 뉴스</h3>");

        for (EtfNews news : newsList) {
            html.append("<div class='news-item'>");
            html.append("<div class='news-title'>").append(news.getTitle()).append("</div>");
            html.append("<div class='news-meta'>");
            html.append("출처: ").append(news.getSource()).append(" | ");
            html.append("발행일: ").append(news.getPublishedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            html.append("</div>");
            if (news.getContent() != null && !news.getContent().isEmpty()) {
                html.append("<p>").append(news.getContent()).append("</p>");
            }
            html.append("<p><a href='").append(news.getUrl()).append("' style='color: #0066cc;'>기사 전문 보기 →</a></p>");
            html.append("</div>");
        }

        html.append("</div>");

        // 푸터
        html.append("<div class='footer'>");
        html.append("<p>본 메일은 ETF 뉴스 알림 서비스에서 자동 발송되었습니다.</p>");
        html.append("<p>발송 시각: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("</p>");
        html.append("</div>");

        html.append("</div>");
        html.append("</body></html>");

        return html.toString();
    }

    /**
     * 알림 발송 이력 저장
     */
    private void saveAlertHistory(Etf etf, Long memberId, String channel, String recipient,
                                  String content, boolean sent, String failureReason) {
        try {
            AlertHistory history = AlertHistory.builder()
                    .etfId(etf.getId())
                    .etfName(etf.getName())
                    .memberId(memberId)
                    .alertType("NEWS")
                    .channel(channel)
                    .content(content)
                    .sent(sent)
                    .failureReason(failureReason)
                    .build();

            alertHistoryRepository.save(history);

        } catch (Exception e) {
            log.error("알림 이력 저장 실패: {}", e.getMessage());
        }
    }
}