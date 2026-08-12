package com.prince.unispot.user.presentation.dto;

//immutable , inherently thread safe, private final fields and canonical contructor, only gettr, as the name of fields  
public record RegisterRequest(
    String email,
    String password
) {}