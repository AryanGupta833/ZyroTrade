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

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {

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

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody RegisterRequest registerRequest){
        System.out.println("Register controller hit");
        if(userRepository.findByUsername(registerRequest.getUsername()).isPresent()){
            throw new RuntimeException("Username already exists");
        }

        User user=new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setRole(Role.ROLE_USER);
        user.setEnabled(true);

        userRepository.save(user);
        Wallet wallet=new Wallet();
        wallet.setUser(user);
        wallet.setBalance(10000);
        walletRepository.save(wallet);
        return ResponseEntity.ok(
                Map.of("message", "User registered successfully")
        );


    }

}
