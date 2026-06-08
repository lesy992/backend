package com.sum.backend.dto;

import lombok.Getter;

@Getter
public class CreateUser {

    private String username;
    private String loginId;
    private String password;
    private String email;
}
