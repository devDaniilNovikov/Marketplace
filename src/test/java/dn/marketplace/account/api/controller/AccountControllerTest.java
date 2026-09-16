package dn.marketplace.account.api.controller;

import dn.marketplace.account.api.dto.AccountResponse;
import dn.marketplace.account.api.enums.BusinessStatus;
import dn.marketplace.account.service.AccountService;
import dn.marketplace.core.security.JitProvisioningFilter;
import dn.marketplace.core.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AccountController.class, excludeAutoConfiguration = OAuth2ResourceServerWebSecurityAutoConfiguration.class)
@Import({SecurityConfig.class, JitProvisioningFilter.class})
@TestPropertySource(properties = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:1/jwks")
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @Test
    void me_без_токена_401() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void approve_не_админ_403() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/accounts/{id}/seller-application/approve", id)
                        .with(jwt().jwt(jwt -> jwt.subject(id.toString()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void approve_админ_200() throws Exception {
        UUID id = UUID.randomUUID();
        when(accountService.approveSeller(id)).thenReturn(response(id, BusinessStatus.SELLER));

        mockMvc.perform(post("/api/v1/accounts/{id}/seller-application/approve", id)
                        .with(jwt().jwt(jwt -> jwt.subject(UUID.randomUUID().toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessStatus").value("SELLER"));
    }

    @Test
    void reject_пустая_причина_400() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/accounts/{id}/seller-application/reject", id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ban_себя_403() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/accounts/{id}/ban", id)
                        .with(jwt().jwt(jwt -> jwt.subject(id.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_me_204() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(delete("/api/v1/accounts/me")
                        .with(jwt().jwt(jwt -> jwt.subject(id.toString()))))
                .andExpect(status().isNoContent());
        verify(accountService).delete(id);
    }

    @Test
    void reject_админ_передаёт_причину() throws Exception {
        UUID id = UUID.randomUUID();
        when(accountService.rejectSeller(eq(id), eq("мало документов")))
                .thenReturn(response(id, BusinessStatus.SELLER_REJECTED));

        mockMvc.perform(post("/api/v1/accounts/{id}/seller-application/reject", id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"мало документов\"}"))
                .andExpect(status().isOk());
        verify(accountService).rejectSeller(id, "мало документов");
    }

    @Test
    void getById_владелец_200() throws Exception {
        UUID id = UUID.randomUUID();
        when(accountService.findById(id)).thenReturn(response(id, BusinessStatus.BUYER));
        mockMvc.perform(get("/api/v1/accounts/{id}", id)
                        .with(jwt().jwt(jwt -> jwt.subject(id.toString()))))
                .andExpect(status().isOk());
    }

    private static AccountResponse response(UUID id, BusinessStatus status) {
        return AccountResponse.builder()
                .id(id)
                .username("alice")
                .businessStatus(status)
                .banned(false)
                .deleted(false)
                .build();
    }
}
