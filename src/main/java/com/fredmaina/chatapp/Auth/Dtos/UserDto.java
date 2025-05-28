package com.fredmaina.chatapp.Auth.Dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;


@Getter
@Setter
@AllArgsConstructor
public class UserDto {
    private UUID userId;
    private String firstName;
    private String lastName ;
    private String email ;
    private String username ;
}
