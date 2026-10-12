package com.mylog.identity.api;

import com.mylog.identity.application.DevAccountService;
import com.mylog.identity.api.response.DevAccountResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DevAccountControllerTest {
    private final DevAccountService service = mock(DevAccountService.class);
    private final DevAccountController controller = new DevAccountController(service,
            new MockEnvironment().withProperty("MYLOG_DEV_USERS_KEY", "synthetic-dev-key-123456"));

    @Test void rejectsRequestsWithoutTheDevelopmentKey() {
        assertEquals(404, controller.list(null, null, 50).getStatusCode().value());
        assertEquals(404, controller.list("wrong-key", null, 50).getStatusCode().value());
        verifyNoInteractions(service);
    }

    @Test void returnsOnlyTheRequestedPageWithoutCaching() {
        UUID before = UUID.randomUUID();
        var account = new DevAccountService.Account(UUID.randomUUID(), "ACTIVE", "person@example.test");
        when(service.list(before, 50)).thenReturn(List.of(account));

        var response = controller.list("synthetic-dev-key-123456", before, 50);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals(List.of(DevAccountResponse.from(account)), response.getBody());
        verify(service).list(before, 50);
    }

    @Test void rejectsUnboundedPages() {
        assertEquals(400, controller.list("synthetic-dev-key-123456", null, 101).getStatusCode().value());
        verifyNoInteractions(service);
    }
}
