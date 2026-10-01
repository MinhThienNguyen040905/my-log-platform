package com.mylog.safety.application;

import com.mylog.platform.web.InvalidRequestException;
import com.mylog.safety.application.query.SafetyResourceView;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class SafetyResourceService {
    private final SafetyResourceCatalog catalog;

    public SafetyResourceService(SafetyResourceCatalog catalog) { this.catalog = catalog; }

    @Transactional(readOnly = true)
    public List<SafetyResourceView> approvedFor(String locale, String countryCode) {
        if (locale == null || !locale.matches("[a-z]{2}(?:-[A-Z]{2})?")
                || countryCode == null || !countryCode.matches("[A-Z]{2}"))
            throw new InvalidRequestException();
        return catalog.approvedFor(locale, countryCode.toUpperCase(Locale.ROOT));
    }
}
