package koscom.mini_project.team2.team2.domain.news.service;

import com.google.gson.Gson;
import jakarta.mail.internet.MimeMessage;
import koscom.mini_project.team2.team2.domain.news.dto.*;
import koscom.mini_project.team2.team2.domain.news.entity.RebalancingHistory;
import koscom.mini_project.team2.team2.domain.news.repository.RebalancingHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
@Slf4j
@RequiredArgsConstructor
public class RebalancingNotificationService {

    private final JavaMailSender mailSender;
    private final RebalancingHistoryRepository historyRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * 리밸런싱 알림 이메일 발송
     */
    public boolean sendRebalancingEmail(
            RebalancingResponse response,
            String toEmail,
            Long memberId,
            Integer period) {

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(String.format("[포트폴리오 알림] %s 리밸런싱 %s",
                    response.getPortfolioName(),
                    response.isRebalancingRequired() ? "권장" : "분석 완료"));

            String htmlContent = createRebalancingEmailContent(response);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("리밸런싱 이메일 발송 성공: {} -> {}", response.getPortfolioName(), toEmail);

            // 이력 저장
            saveRebalancingHistory(response, memberId, period, true);

            return true;

        } catch (Exception e) {
            log.error("리밸런싱 이메일 발송 실패: {}", e.getMessage(), e);
            saveRebalancingHistory(response, memberId, period, false);
            return false;
        }
    }

    /**
     * 리밸런싱 이메일 HTML 생성
     */
    private String createRebalancingEmailContent(RebalancingResponse response) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html><head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<style>");
        html.append("body { font-family: 'Malgun Gothic', sans-serif; line-height: 1.6; color: #333; background-color: #f5f5f5; margin: 0; padding: 0; }");
        html.append(".container { max-width: 700px; margin: 20px auto; background-color: white; border-radius: 10px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }");
        html.append(".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px 20px; text-align: center; }");
        html.append(".header h1 { margin: 0; font-size: 24px; }");
        html.append(".header .subtitle { margin: 10px 0 0 0; font-size: 14px; opacity: 0.9; }");
        html.append(".content { padding: 30px 20px; }");
        html.append(".alert-box { background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; border-radius: 4px; }");
        html.append(".alert-box.success { background-color: #d4edda; border-left-color: #28a745; }");
        html.append(".section { margin: 25px 0; }");
        html.append(".section-title { font-size: 18px; font-weight: bold; margin-bottom: 15px; color: #667eea; display: flex; align-items: center; }");
        html.append(".section-title::before { content: ''; display: inline-block; width: 4px; height: 18px; background-color: #667eea; margin-right: 10px; }");
        html.append(".allocation-table { width: 100%; border-collapse: collapse; margin: 15px 0; }");
        html.append(".allocation-table th { background-color: #f8f9fa; padding: 12px; text-align: left; font-weight: 600; border-bottom: 2px solid #dee2e6; font-size: 14px; }");
        html.append(".allocation-table td { padding: 12px; border-bottom: 1px solid #dee2e6; font-size: 14px; }");
        html.append(".weight-bar { background-color: #e9ecef; height: 20px; border-radius: 10px; overflow: hidden; position: relative; margin: 5px 0; }");
        html.append(".weight-bar-fill { background: linear-gradient(90deg, #667eea 0%, #764ba2 100%); height: 100%; transition: width 0.3s; }");
        html.append(".change-badge { display: inline-block; padding: 4px 8px; border-radius: 4px; font-size: 12px; font-weight: bold; }");
        html.append(".change-badge.decrease { background-color: #fee; color: #c00; }");
        html.append(".change-badge.increase { background-color: #efe; color: #0a0; }");
        html.append(".change-badge.maintain { background-color: #eee; color: #666; }");
        html.append(".news-item { background-color: #f8f9fa; padding: 15px; margin: 10px 0; border-radius: 5px; border-left: 3px solid #667eea; }");
        html.append(".news-title { font-weight: bold; color: #333; margin-bottom: 5px; font-size: 14px; }");
        html.append(".news-meta { font-size: 12px; color: #666; margin-bottom: 8px; }");
        html.append(".news-link { color: #667eea; text-decoration: none; font-size: 13px; }");
        html.append(".recommendations { background-color: #fff8e1; padding: 15px; border-radius: 5px; margin: 15px 0; }");
        html.append(".recommendations ul { margin: 10px 0; padding-left: 20px; }");
        html.append(".recommendations li { margin: 8px 0; font-size: 14px; }");
        html.append(".footer { background-color: #f8f9fa; padding: 20px; text-align: center; color: #666; font-size: 12px; }");
        html.append(".risk-badge { display: inline-block; padding: 6px 12px; border-radius: 20px; font-size: 13px; font-weight: bold; }");
        html.append(".risk-low { background-color: #d4edda; color: #155724; }");
        html.append(".risk-medium { background-color: #fff3cd; color: #856404; }");
        html.append(".risk-high { background-color: #f8d7da; color: #721c24; }");
        html.append("</style>");
        html.append("</head><body>");

        html.append("<div class='container'>");

        // 헤더
        html.append("<div class='header'>");
        html.append("<h1>🔄 포트폴리오 리밸런싱 분석</h1>");
        html.append("<div class='subtitle'>").append(response.getPortfolioName()).append("</div>");
        html.append("</div>");

        html.append("<div class='content'>");

        // 리밸런싱 필요 여부
        if (response.isRebalancingRequired()) {
            html.append("<div class='alert-box'>");
            html.append("<strong>⚠️ 리밸런싱이 권장됩니다</strong><br>");
            html.append(response.getRebalancingReason());
            html.append("</div>");
        } else {
            html.append("<div class='alert-box success'>");
            html.append("<strong>✅ 현재 포트폴리오 유지 권장</strong><br>");
            html.append(response.getRebalancingReason());
            html.append("</div>");
        }

        // 위험도 평가
        html.append("<div style='margin: 20px 0; text-align: center;'>");
        html.append("<span style='color: #666; font-size: 14px;'>현재 시장 위험도: </span>");
        String riskClass = "risk-medium";
        String riskText = "MEDIUM";
        if (response.getRiskAssessment() != null) {
            riskClass = "risk-" + response.getRiskAssessment().toLowerCase();
            riskText = response.getRiskAssessment();
        }
        html.append("<span class='risk-badge ").append(riskClass).append("'>").append(riskText).append("</span>");
        html.append("</div>");

        // 현재 포트폴리오 vs 추천 포트폴리오
        if (response.isRebalancingRequired() && !response.getRecommendedPortfolio().isEmpty()) {
            html.append("<div class='section'>");
            html.append("<div class='section-title'>📊 포트폴리오 조정 권장</div>");
            html.append("<table class='allocation-table'>");
            html.append("<thead><tr>");
            html.append("<th>ETF</th>");
            html.append("<th style='text-align: center;'>현재</th>");
            html.append("<th style='text-align: center;'>추천</th>");
            html.append("<th style='text-align: center;'>변동</th>");
            html.append("</tr></thead>");
            html.append("<tbody>");

            for (AllocationChangeDto allocation : response.getRecommendedPortfolio()) {
                html.append("<tr>");
                html.append("<td><strong>").append(allocation.getEtfName()).append("</strong><br>");
                html.append("<span style='font-size: 12px; color: #666;'>").append(allocation.getCategory()).append("</span></td>");
                html.append("<td style='text-align: center;'>").append(String.format("%.0f%%", allocation.getCurrentWeight())).append("</td>");
                html.append("<td style='text-align: center;'><strong>").append(String.format("%.0f%%", allocation.getRecommendedWeight())).append("</strong></td>");

                double change = allocation.getChangeAmount();
                String badge = "";
                if (Math.abs(change) < 1.0) {
                    badge = "<span class='change-badge maintain'>유지</span>";
                } else if (change > 0) {
                    badge = String.format("<span class='change-badge increase'>+%.0f%%</span>", change);
                } else {
                    badge = String.format("<span class='change-badge decrease'>%.0f%%</span>", change);
                }
                html.append("<td style='text-align: center;'>").append(badge).append("</td>");
                html.append("</tr>");
            }

            html.append("</tbody></table>");
            html.append("</div>");
        } else {
            // 현재 포트폴리오만 표시
            html.append("<div class='section'>");
            html.append("<div class='section-title'>📊 현재 포트폴리오 구성</div>");

            for (AllocationDto allocation : response.getCurrentPortfolio()) {
                html.append("<div style='margin: 15px 0;'>");
                html.append("<div style='display: flex; justify-content: space-between; margin-bottom: 5px;'>");
                html.append("<span><strong>").append(allocation.getEtfName()).append("</strong>");
                html.append(" <span style='font-size: 12px; color: #666;'>(").append(allocation.getCategory()).append(")</span></span>");
                html.append("<span><strong>").append(String.format("%.0f%%", allocation.getCurrentWeight())).append("</strong></span>");
                html.append("</div>");
                html.append("<div class='weight-bar'>");
                html.append("<div class='weight-bar-fill' style='width: ").append(allocation.getCurrentWeight()).append("%;'></div>");
                html.append("</div>");
                html.append("</div>");
            }
            html.append("</div>");
        }

        // 뉴스 근거
        if (response.getNewsEvidence() != null && !response.getNewsEvidence().isEmpty()) {
            html.append("<div class='section'>");
            html.append("<div class='section-title'>📰 주요 뉴스</div>");

            for (NewsEvidenceDto news : response.getNewsEvidence()) {
                html.append("<div class='news-item'>");
                html.append("<div class='news-title'>").append(news.getNewsTitle()).append("</div>");
                html.append("<div class='news-meta'>");
                html.append("[").append(news.getEtfName()).append("] | ");
                html.append(news.getPublishedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                html.append("</div>");
                if (news.getSummary() != null && !news.getSummary().isEmpty()) {
                    html.append("<p style='margin: 10px 0 5px 0; font-size: 13px; color: #555;'>").append(news.getSummary()).append("</p>");
                }
                html.append("<a href='").append(news.getNewsUrl()).append("' class='news-link'>기사 전문 보기 →</a>");
                html.append("</div>");
            }
            html.append("</div>");
        }

        // 투자 조언
        if (response.getRecommendations() != null && !response.getRecommendations().isEmpty()) {
            html.append("<div class='section'>");
            html.append("<div class='section-title'>💡 투자 조언</div>");
            html.append("<div class='recommendations'>");
            html.append("<ul>");
            for (String recommendation : response.getRecommendations()) {
                if (!recommendation.trim().isEmpty()) {
                    html.append("<li>").append(recommendation.trim()).append("</li>");
                }
            }
            html.append("</ul>");
            html.append("</div>");
            html.append("</div>");
        }

        html.append("</div>");

        // 푸터
        html.append("<div class='footer'>");
        html.append("<p>본 메일은 AI 기반 포트폴리오 분석 서비스에서 자동 발송되었습니다.</p>");
        html.append("<p>분석 시각: ").append(response.getAnalyzedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("</p>");
        html.append("<p style='margin-top: 10px; color: #999; font-size: 11px;'>");
        html.append("※ 본 정보는 투자 참고용이며, 투자 판단의 책임은 투자자 본인에게 있습니다.");
        html.append("</p>");
        html.append("</div>");

        html.append("</div>");
        html.append("</body></html>");

        return html.toString();
    }

    /**
     * 리밸런싱 이력 저장
     */
    private void saveRebalancingHistory(RebalancingResponse response, Long memberId, Integer period, boolean emailSent) {
        try {
            Gson gson = new Gson();

            String newsEvidenceJson = gson.toJson(response.getNewsEvidence());
            String beforeAllocation = gson.toJson(response.getCurrentPortfolio());
            String afterAllocation = response.isRebalancingRequired() ?
                    gson.toJson(response.getRecommendedPortfolio()) : beforeAllocation;

            RebalancingHistory history = RebalancingHistory.builder()
                    .portfolioId(response.getPortfolioId())
                    .memberId(memberId)
                    .period(period)
                    .reason(response.getRebalancingReason())
                    .newsEvidence(newsEvidenceJson)
                    .beforeAllocation(beforeAllocation)
                    .afterAllocation(afterAllocation)
                    .emailSent(emailSent)
                    .build();

            historyRepository.save(history);

        } catch (Exception e) {
            log.error("리밸런싱 이력 저장 실패: {}", e.getMessage());
        }
    }
}