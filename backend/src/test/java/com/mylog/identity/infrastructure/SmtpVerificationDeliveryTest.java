package com.mylog.identity.infrastructure;

import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.env.MockEnvironment;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmtpVerificationDeliveryTest {
    @Test void sendsClickableVerificationButtonAndPlainTextFallback() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(message);
        var env = new MockEnvironment().withProperty("MYLOG_MAIL_FROM", "sender@example.com")
                .withProperty("MYLOG_VERIFICATION_URL", "https://example.test/auth/verify-email");

        new SmtpVerificationDelivery(sender, env).send("recipient@example.com", "test-token", "01234567");
        message.saveChanges();

        InternetAddress from = (InternetAddress) message.getFrom()[0];
        assertEquals("mylog", from.getPersonal());
        assertEquals("sender@example.com", from.getAddress());
        assertEquals("recipient@example.com", ((InternetAddress) message.getAllRecipients()[0]).getAddress());
        String plainText = findPart(message, "text/plain");
        String html = findPart(message, "text/html");
        assertTrue(plainText.contains("https://example.test/auth/verify-email?token=test-token"));
        assertTrue(plainText.contains("01234567"));
        assertTrue(html.contains("<a href=\"https://example.test/auth/verify-email?token=test-token\""));
        assertTrue(html.contains("Mở trang xác minh email</a>"));
        assertTrue(html.contains("01234567"));
        verify(sender).send(message);
    }

    private String findPart(Part part, String contentType) throws Exception {
        Object content = part.getContent();
        if (content instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                String found = findPart(multipart.getBodyPart(i), contentType);
                if (found != null) return found;
            }
        }
        if (part.isMimeType(contentType) && content instanceof String text) return text;
        return null;
    }
}
