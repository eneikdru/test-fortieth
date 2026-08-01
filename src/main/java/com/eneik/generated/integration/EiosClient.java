package com.eneik.generated.integration;

import java.util.List;

public interface EiosClient {
    List<EiosUserRoleDto> fetchUserRoleChanges();
    void syncAnalytics(String analyticsDataCsv);
}
