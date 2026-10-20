package com.ZyroTrade.security;

import com.ZyroTrade.Notification.Notification;
import com.ZyroTrade.Notification.NotificationRepository;
import com.ZyroTrade.user.Role;
import com.ZyroTrade.user.User;
import com.ZyroTrade.user.UserRepository;
import com.ZyroTrade.Wallet.Wallet;
import com.ZyroTrade.Wallet.WalletRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiSecurityIntegrationTests {
    @Autowired private MockMvc mvc;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private UserRepository users;
    @Autowired private NotificationRepository notifications;
    @Autowired private WalletRepository wallets;

    @Test
    void protectedRoutesReturnStableJsonErrorsAndAdminRoutesEnforceRole() throws Exception {
        mvc.perform(get("/api/wallet"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        mvc.perform(get("/api/admin/ledger/wallet-reconciliation")
                        .header("Authorization", "Bearer " + token(createUser("regular-user"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        User admin = createUser("admin-user");
        admin.setRole(Role.ROLE_ADMIN);
        admin = users.saveAndFlush(admin);
        mvc.perform(get("/api/admin/ledger/wallet-reconciliation")
                        .header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/wallet").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    void userCannotMarkAnotherUsersNotificationRead() throws Exception {
        User owner = createUser("notification-owner");
        User caller = createUser("notification-caller");
        Notification notification = new Notification();
        notification.setUsername(owner.getUsername());
        notification.setTitle("private");
        notification.setMessage("private message");
        notification = notifications.saveAndFlush(notification);

        mvc.perform(put("/api/notifications/{id}/read", notification.getId())
                        .header("Authorization", "Bearer " + token(caller)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        org.assertj.core.api.Assertions.assertThat(notifications.findById(notification.getId()).orElseThrow().isRead())
                .isFalse();
    }

    @Test
    void invalidRegistrationAndOpenApiContractsAreExposedSafely() throws Exception {
        mvc.perform(post("/auth/register").contentType("application/json")
                        .content("{\"username\":\"bad\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/orders'].post").exists())
                .andExpect(jsonPath("$.components.schemas.ApiError").exists());
    }

    @Test
    void financialApiReturnsDtosWithoutUserCredentialFields() throws Exception {
        User user = createUser("dto-user");
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(new java.math.BigDecimal("10.0000"));
        wallets.saveAndFlush(wallet);

        mvc.perform(get("/api/wallet").header("Authorization", "Bearer " + token(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(10.0))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.user").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    private User createUser(String prefix) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User user = new User();
        user.setUsername(prefix + "-" + suffix);
        user.setPassword("not-a-real-password-hash");
        user.setRole(Role.ROLE_USER);
        return users.saveAndFlush(user);
    }

    private String token(User user) {
        UserDetails details = org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
                .password("unused")
                .authorities(user.getRole().name())
                .build();
        return jwtUtil.generateToken(details);
    }
}
