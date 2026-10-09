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
import java.nio.charset.StandardCharsets;
import jakarta.mail.MessagingException;

@Component
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
final class SmtpVerificationDelivery implements VerificationDelivery {
    private final JavaMailSender sender;
    private final String from;

    SmtpVerificationDelivery(JavaMailSender sender, Environment env) {
        this.sender = sender;
        this.from = env.getProperty("MYLOG_MAIL_FROM", env.matchesProfiles("local", "test") ? "noreply@mylog.local" : "");
        if (from.isBlank()) throw new IllegalStateException("Mail sender address is required");
    }

    @Override public void send(String email, String token) {
        try {
            var message = sender.createMimeMessage();
            var helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setFrom(from, "mylog");
            helper.setTo(email);
            helper.setSubject("mylog: xác minh email");
            helper.setText("Mã xác minh mylog của bạn: " + token + "\nMã này hết hạn sau 24 giờ.");
            sender.send(message);
        } catch (MailException | MessagingException | UnsupportedEncodingException e) {
            throw new DependencyUnavailableException();
        }
    }
}
