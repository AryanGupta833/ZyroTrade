package com.ZyroTrade.auth;


import com.ZyroTrade.DTO.LoginRequest;
import com.ZyroTrade.DTO.LoginResponse;
import com.ZyroTrade.DTO.RegisterRequest;
import com.ZyroTrade.Wallet.WalletRepository;
import com.ZyroTrade.Wallet.Wallet;
import com.ZyroTrade.security.JwtUtil;
import com.ZyroTrade.user.Role;
import com.ZyroTrade.user.User;
import com.ZyroTrade.user.UserRepository;
import com.ZyroTrade.ledger.LedgerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.math.BigDecimal;

@RestController
@RequestMapping("/auth")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Authentication", description = "Register and obtain a bearer token")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    @io.swagger.v3.oas.annotations.Operation(summary = "Authenticate and obtain a JWT bearer token")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.getUsername(),
                                request.getPassword()));

        String token = jwtUtil.generateToken(
                (UserDetails) authentication.getPrincipal());

        return new LoginResponse(token);
    }
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private LedgerService ledgerService;

    @PostMapping("/register")
    @Transactional
    @io.swagger.v3.oas.annotations.Operation(summary = "Register a user and create the initial wallet")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest registerRequest){
        String username = registerRequest.getUsername().trim();
        if (registerRequest.getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password must be at most 72 UTF-8 bytes");
        }
        if(userRepository.findByUsername(username).isPresent()){
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Username already exists");
        }

        User user=new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setRole(Role.ROLE_USER);
        user.setEnabled(true);

        userRepository.save(user);
        Wallet wallet=new Wallet();
        wallet.setUser(user);
        wallet.setBalance(new BigDecimal("10000.0000"));
        wallet = walletRepository.saveAndFlush(wallet);
        ledgerService.postOpeningWalletBalance(wallet, wallet.getBalance(), "WALLET_OPENING:" + wallet.getId());
        return ResponseEntity.ok(
                Map.of("message", "User registered successfully")
        );


    }

}
