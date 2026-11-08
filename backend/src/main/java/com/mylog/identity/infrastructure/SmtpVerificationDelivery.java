package com.mylog.identity.infrastructure;

import com.mylog.identity.application.VerificationDelivery;
import com.mylog.platform.web.DependencyUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import jakarta.mail.MessagingException;

@Component
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
final class SmtpVerificationDelivery implements VerificationDelivery {
    private static final Logger log = LoggerFactory.getLogger(SmtpVerificationDelivery.class);
    private final JavaMailSender sender;
    private final String from;
    private final String verificationUrl;
    private final String passwordResetUrl;

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

        String resetConfigured = env.getProperty("MYLOG_PASSWORD_RESET_URL",
                env.matchesProfiles("local", "test") ? "http://localhost:3000/auth/reset-password" : "");
        URI resetUri;
        try { resetUri = URI.create(resetConfigured); }
        catch (IllegalArgumentException e) { throw new IllegalStateException("Password reset URL is invalid", e); }
        if (resetUri.getHost() == null || resetUri.getQuery() != null || resetUri.getFragment() != null
                || !("https".equals(resetUri.getScheme()) ||
                (env.matchesProfiles("local", "test") && "http".equals(resetUri.getScheme()))))
            throw new IllegalStateException("Password reset URL must be an HTTPS page URL");
        this.passwordResetUrl = resetUri.toString();
    }

    @Override public void send(String email, String token, String code) {
        try {
            var message = sender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from, "mylog");
            helper.setTo(email);
            helper.setSubject("mylog: xác minh email");
            String link = verificationUrl + "?token=" + token;
            String plainText = "Xác minh email mylog: " + link
                    + "\nLiên kết hết hạn sau 24 giờ. Trang xác minh chỉ gửi yêu cầu khi bạn bấm xác nhận."
                    + "\nNếu mở email trên thiết bị khác, nhập mã: " + code
                    + "\nMã gồm 8 chữ số và hết hạn sau 10 phút. Nếu đã xác minh bằng một cách, cách còn lại sẽ không dùng được nữa.";
            String html = """
                    <!doctype html>
                    <html lang="vi"><body style="font-family:Arial,sans-serif;color:#1f2937;line-height:1.6">
                    <h1 style="font-size:22px">Xác minh email mylog</h1>
                    <p>Bấm nút dưới đây để mở trang xác minh, sau đó bấm xác nhận trên trang.</p>
                    <p><a href="%s" style="display:inline-block;background-color:#2563eb;color:#ffffff;padding:12px 20px;border-radius:6px;text-decoration:none;font-weight:bold">Mở trang xác minh email</a></p>
                    <p>Liên kết hết hạn sau 24 giờ.</p>
                    <p>Nếu mở email trên thiết bị khác, nhập mã: <strong>%s</strong></p>
                    <p>Mã gồm 8 chữ số và hết hạn sau 10 phút. Nếu đã xác minh bằng một cách, cách còn lại sẽ không dùng được nữa.</p>
                    </body></html>
                    """.formatted(HtmlUtils.htmlEscape(link), HtmlUtils.htmlEscape(code));
            helper.setText(plainText, html);
            sender.send(message);
        } catch (MailException | MessagingException | UnsupportedEncodingException e) {
            throw new DependencyUnavailableException();
        }
    }

    @Override public void sendPasswordChangedAlert(String email, Instant changedAt) {
        try {
            var message = sender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from, "mylog");
            helper.setTo(email);
            helper.setSubject("mylog: Mật khẩu của bạn vừa được thay đổi");
            String plainText = "Mật khẩu tài khoản mylog của bạn vừa được thay đổi vào lúc " + changedAt
                    + ".\nNếu không phải bạn thực hiện thay đổi này, vui lòng liên hệ quản trị viên hoặc thực hiện khôi phục tài khoản ngay lập tức.";
            String html = """
                    <!doctype html>
                    <html lang="vi"><body style="font-family:Arial,sans-serif;color:#1f2937;line-height:1.6">
                    <h1 style="font-size:22px">Mật khẩu của bạn vừa được thay đổi</h1>
                    <p>Mật khẩu tài khoản mylog của bạn vừa được thay đổi vào lúc <strong>%s</strong>.</p>
                    <p>Nếu không phải bạn thực hiện thay đổi này, vui lòng liên hệ quản trị viên hoặc thực hiện khôi phục tài khoản ngay lập tức.</p>
                    </body></html>
                    """.formatted(HtmlUtils.htmlEscape(changedAt.toString()));
            helper.setText(plainText, html);
            sender.send(message);
        } catch (Exception e) {
            log.error("Failed to deliver password change alert email to {}", email, e);
        }
    }

    @Override public void sendPasswordReset(String email, String token, String code) {
        try {
            var message = sender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from, "mylog");
            helper.setTo(email);
            helper.setSubject("mylog: Đặt lại mật khẩu");
            String link = passwordResetUrl + "#token=" + token;
            String plainText = "Đặt lại mật khẩu mylog: " + link
                    + "\nLiên kết hết hạn sau 1 giờ."
                    + "\nNếu mở email trên thiết bị khác, nhập mã xác minh: " + code
                    + "\nMã gồm 8 chữ số và hết hạn sau 15 phút."
                    + "\nNếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này. Tài khoản của bạn vẫn an toàn.";
            String html = """
                    <!doctype html>
                    <html lang="vi"><body style="font-family:Arial,sans-serif;color:#1f2937;line-height:1.6">
                    <h1 style="font-size:22px">Đặt lại mật khẩu mylog</h1>
                    <p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản mylog của bạn.</p>
                    <p><a href="%s" style="display:inline-block;background-color:#2563eb;color:#ffffff;padding:12px 20px;border-radius:6px;text-decoration:none;font-weight:bold">Đặt lại mật khẩu mới</a></p>
                    <p>Liên kết hết hạn sau 1 giờ.</p>
                    <p>Nếu mở email trên thiết bị khác, bạn có thể nhập mã xác minh: <strong>%s</strong></p>
                    <p>Mã gồm 8 chữ số và hết hạn sau 15 phút.</p>
                    <p style="color:#6b7280;font-size:13px;margin-top:24px">Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này. Tài khoản của bạn vẫn được bảo vệ an toàn.</p>
                    </body></html>
                    """.formatted(HtmlUtils.htmlEscape(link), HtmlUtils.htmlEscape(code));
            helper.setText(plainText, html);
            sender.send(message);
        } catch (Exception e) {
            log.error("Failed to deliver password reset email to {}", email, e);
        }
    }

    @Override public void sendOAuthAccountNotice(String email) {
        try {
            var message = sender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from, "mylog");
            helper.setTo(email);
            helper.setSubject("mylog: Yêu cầu đặt lại mật khẩu tài khoản Google");
            String plainText = "Bạn nhận được email này vì có yêu cầu đặt lại mật khẩu cho tài khoản mylog của bạn."
                    + "\nTài khoản của bạn được liên kết và đăng nhập thông qua Google, không sử dụng mật khẩu riêng."
                    + "\nVui lòng mở trang đăng nhập mylog và chọn 'Tiếp tục với Google'."
                    + "\nNếu bạn không yêu cầu điều này, tài khoản của bạn vẫn hoàn toàn an toàn.";
            String html = """
                    <!doctype html>
                    <html lang="vi"><body style="font-family:Arial,sans-serif;color:#1f2937;line-height:1.6">
                    <h1 style="font-size:22px">Thông báo đăng nhập tài khoản mylog</h1>
                    <p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản mylog của bạn.</p>
                    <p>Tài khoản của bạn được liên kết và đăng nhập thông qua <strong>Google</strong>, vì vậy không sử dụng mật khẩu riêng của mylog.</p>
                    <p>Vui lòng mở trang đăng nhập và bấm <strong>'Tiếp tục với Google'</strong> để truy cập cuốn sổ của bạn.</p>
                    <p style="color:#6b7280;font-size:13px;margin-top:24px">Nếu bạn không gửi yêu cầu này, vui lòng bỏ qua email. Tài khoản của bạn vẫn hoàn toàn an toàn.</p>
                    </body></html>
                    """;
            helper.setText(plainText, html);
            sender.send(message);
        } catch (Exception e) {
            log.error("Failed to deliver OAuth account notice email to {}", email, e);
        }
    }
}
