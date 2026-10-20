package com.ZyroTrade.DTO;


import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class RegisterRequest {
    @NotBlank @Size(min = 3, max = 64)
    private String username;
    @NotBlank @Size(min = 12, max = 72)
    private String password;
}
