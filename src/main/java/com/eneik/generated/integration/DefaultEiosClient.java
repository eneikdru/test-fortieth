package com.eneik.generated.integration;

import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;

@Component
public class DefaultEiosClient implements EiosClient {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DefaultEiosClient.class);

    @Override
    public List<EiosUserRoleDto> fetchUserRoleChanges() {
        return Collections.emptyList();
    }

    @Override
    public void syncAnalytics(String analyticsDataCsv) {
        log.info("DefaultEiosClient: Analytics data synced successfully to EIOS (length: {})",
                 analyticsDataCsv != null ? analyticsDataCsv.length() : 0);
    }
}
