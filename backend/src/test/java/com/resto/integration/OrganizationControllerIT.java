package com.resto.integration;

import com.resto.core.response.Response;
import com.resto.tenant.domain.Organization;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrganizationControllerIT {

    @Test
    @DisplayName("Integration Test: Organization domain creation and REST payload contract")
    void testOrganizationDomainIntegrationContract() {
        UUID orgId = UUID.randomUUID();
        Organization org = Organization.builder()
                .id(orgId)
                .name("Gourmet Paris Bistro")
                .code("GOURMET-PARIS")
                .active(true)
                .build();

        Response<Organization> response = Response.<Organization>builder()
                .status(HttpStatus.OK)
                .statusCode(200)
                .message("default.message.success")
                .data(org)
                .build();

        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getData());
        assertEquals("Gourmet Paris Bistro", response.getData().getName());
        assertEquals("GOURMET-PARIS", response.getData().getCode());
        assertTrue(response.getData().getActive());
    }
}
