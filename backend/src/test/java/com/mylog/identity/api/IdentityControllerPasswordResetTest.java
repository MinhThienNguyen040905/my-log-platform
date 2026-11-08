package com.mylog.identity.api;

import com.mylog.identity.api.request.ConfirmPasswordResetCodeRequest;
import com.mylog.identity.api.request.ConfirmPasswordResetRequest;
import com.mylog.identity.api.request.PasswordResetRequest;
import com.mylog.identity.application.IdentityService;
import com.mylog.platform.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class IdentityControllerPasswordResetTest {

    private IdentityService identityService;
    private CurrentUserProvider currentUserProvider;
    private IdentityController controller;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        identityService = mock(IdentityService.class);
        currentUserProvider = mock(CurrentUserProvider.class);
        controller = new IdentityController(identityService, currentUserProvider);
        request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");
    }

    @Test
    @DisplayName("requestPasswordReset - Gọi identity.requestPasswordReset và trả về HTTP 202 với message")
    void requestPasswordReset_success() {
        PasswordResetRequest req = new PasswordResetRequest("user@example.com");

        ResponseEntity<Map<String, String>> response = controller.requestPasswordReset(req, request);

        assertThat(response.getStatusCode().value()).isEqualTo(202);
        assertThat(response.getBody()).containsKey("message");
        verify(identityService).requestPasswordReset("user@example.com", "192.168.1.100");
    }

    @Test
    @DisplayName("confirmPasswordReset - Gọi identity.resetPasswordWithToken và trả về HTTP 204")
    void confirmPasswordReset_success() {
        ConfirmPasswordResetRequest req = new ConfirmPasswordResetRequest("token_123", "NewSecurePassword123!");

        ResponseEntity<Void> response = controller.confirmPasswordReset(req, request);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(identityService).resetPasswordWithToken("token_123", "NewSecurePassword123!", "192.168.1.100");
    }

    @Test
    @DisplayName("confirmPasswordResetCode - Gọi identity.resetPasswordWithCode và trả về HTTP 204")
    void confirmPasswordResetCode_success() {
        ConfirmPasswordResetCodeRequest req = new ConfirmPasswordResetCodeRequest(
                "user@example.com", "12345678", "NewSecurePassword123!"
        );

        ResponseEntity<Void> response = controller.confirmPasswordResetCode(req, request);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(identityService).resetPasswordWithCode("user@example.com", "12345678", "NewSecurePassword123!", "192.168.1.100");
    }
}

