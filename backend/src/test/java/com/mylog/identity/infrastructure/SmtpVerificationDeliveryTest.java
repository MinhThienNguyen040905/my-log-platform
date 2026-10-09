package com.mylog.identity.infrastructure;

import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.env.MockEnvironment;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmtpVerificationDeliveryTest {
    @Test void sendsVerificationWithMylogDisplayName() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(message);
        var env = new MockEnvironment().withProperty("MYLOG_MAIL_FROM", "sender@example.com");

        new SmtpVerificationDelivery(sender, env).send("recipient@example.com", "test-token");

        InternetAddress from = (InternetAddress) message.getFrom()[0];
        assertEquals("mylog", from.getPersonal());
        assertEquals("sender@example.com", from.getAddress());
        assertEquals("recipient@example.com", ((InternetAddress) message.getAllRecipients()[0]).getAddress());
        verify(sender).send(message);
    }
}
