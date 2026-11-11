package com.mylog.platform.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "mylog")
public record MylogProperties(
        @NotBlank @Pattern(regexp = "api|worker|all") String appProfile,
        @Valid Web web,
        @Valid OpenApi openApi,
        @Valid Identity identity
) {
    public record Web(@NotEmpty List<@NotBlank String> allowedOrigins) {
    }

    public record OpenApi(boolean enabled) {
    }

    public record Identity(boolean enabled, @Valid OAuth oauth, @Valid PasswordReset passwordReset) {
        // Explicit: with the convenience constructor below, Spring cannot pick a binding constructor on its own.
        @ConstructorBinding
        public Identity {
            if (oauth == null) oauth = new OAuth(new OAuth.Google(false, null, null));
            if (passwordReset == null) passwordReset = PasswordReset.DEFAULTS;
        }

        public Identity(boolean enabled, OAuth oauth) {
            this(enabled, oauth, null);
        }

        /**
         * Rate limits for password reset, per 15-minute window unless noted. Defaults are the production
         * values; only the {@code local} profile raises the per-IP ones (application-local.yml) so local
         * test runs do not lock each other out. Per-email limits stay strict everywhere: they guard the
         * 8-digit code against guessing.
         */
        public record PasswordReset(
                @Positive Integer requestsPerIp,
                @Positive Integer requestsPerEmail,
                @Positive Integer requestsPerEmailDaily,
                @Positive Integer confirmsPerIp,
                @Positive Integer codeConfirmsPerEmail
        ) {
            public static final PasswordReset DEFAULTS = new PasswordReset(10, 5, 10, 15, 5);

            public PasswordReset {
                if (requestsPerIp == null) requestsPerIp = 10;
                if (requestsPerEmail == null) requestsPerEmail = 5;
                if (requestsPerEmailDaily == null) requestsPerEmailDaily = 10;
                if (confirmsPerIp == null) confirmsPerIp = 15;
                if (codeConfirmsPerEmail == null) codeConfirmsPerEmail = 5;
            }
        }

        public record OAuth(@Valid Google google) {
            public OAuth {
                if (google == null) google = new Google(false, null, null);
            }

            public record Google(boolean enabled, String clientId, String clientSecret) {
            }
        }
    }
}
