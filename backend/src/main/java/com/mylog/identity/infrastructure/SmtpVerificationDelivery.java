package com.mylog.identity.infrastructure;

import com.mylog.identity.application.VerificationDelivery;
import com.mylog.platform.web.DependencyUnavailableException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import jakarta.mail.MessagingException;

@Component
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
final class SmtpVerificationDelivery implements VerificationDelivery {
    private final JavaMailSender sender;
    private final String from;
    private final String verificationUrl;

    SmtpVerificationDelivery(JavaMailSender sender, Environment env) {
        this.sender = sender;
        this.from = env.getProperty("MYLOG_MAIL_FROM", env.matchesProfiles("local", "test") ? "noreply@mylog.local" : "");
        if (from.isBlank()) throw new IllegalStateException("Mail sender address is required");
        String configured = env.getProperty("MYLOG_VERIFICATION_URL",
                env.matchesProfiles("local", "test") ? "http://localhost:3000/auth/verify-email" : "");
        URI url;
        try { url = URI.create(configured); }
        catch (IllegalArgumentException e) { throw new IllegalStateException("Verification URL is invalid", e); }
        if (url.getHost() == null || url.getQuery() != null || url.getFragment() != null
                || !("https".equals(url.getScheme()) ||
                (env.matchesProfiles("local", "test") && "http".equals(url.getScheme()))))
            throw new IllegalStateException("Verification URL must be an HTTPS page URL");
        this.verificationUrl = url.toString();
    }

    @Override public void send(String email, String token, String code) {
        try {
            var message = sender.createMimeMessage();
            var helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setFrom(from, "mylog");
            helper.setTo(email);
            helper.setSubject("mylog: xác minh email");
            helper.setText("Xác minh email mylog: " + verificationUrl + "?token=" + token
                    + "\nLiên kết hết hạn sau 24 giờ. Trang xác minh chỉ gửi yêu cầu khi bạn bấm xác nhận."
                    + "\nNếu mở email trên thiết bị khác, nhập mã: " + code
                    + "\nMã gồm 8 chữ số và hết hạn sau 10 phút. Nếu đã xác minh bằng một cách, cách còn lại sẽ không dùng được nữa.");
            sender.send(message);
        } catch (MailException | MessagingException | UnsupportedEncodingException e) {
            throw new DependencyUnavailableException();
        }
    }
}
