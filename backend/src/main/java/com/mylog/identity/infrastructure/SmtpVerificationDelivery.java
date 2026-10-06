package com.mylog.identity.infrastructure;

import com.mylog.identity.application.VerificationDelivery;
import com.mylog.platform.web.DependencyUnavailableException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

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
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("mylog: xác minh email");
        message.setText("Mã xác minh mylog của bạn: " + token + "\nMã này hết hạn sau 24 giờ.");
        try { sender.send(message); }
        catch (MailException e) { throw new DependencyUnavailableException(); }
    }
}
