package com.mylog.safety.application;

import com.mylog.safety.application.query.SafetyResourceView;

import java.util.List;

public interface SafetyResourceCatalog {
    List<SafetyResourceView> approvedFor(String locale, String countryCode);
}
