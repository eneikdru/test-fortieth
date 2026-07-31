package com.eneik.generated.integration;

import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;

@Component
public class DefaultEiosClient implements EiosClient {
    @Override
    public List<EiosUserRoleDto> fetchUserRoleChanges() {
        return Collections.emptyList();
    }
}
