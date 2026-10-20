package com.ZyroTrade.auth;

import com.ZyroTrade.DTO.IdentityResponse;
import com.ZyroTrade.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/me")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Authentication")
public class IdentityController {
    private final UserRepository users;

    public IdentityController(UserRepository users) {
        this.users = users;
    }

    @GetMapping
    @io.swagger.v3.oas.annotations.Operation(summary = "Get the authenticated user's identity and role")
    public IdentityResponse me(@AuthenticationPrincipal UserDetails principal) {
        var user = users.findByUsername(principal.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return new IdentityResponse(user.getId(), user.getUsername(), user.getRole().name());
    }
}
