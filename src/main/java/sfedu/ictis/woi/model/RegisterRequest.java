package sfedu.ictis.woi.model;

public record RegisterRequest(
        String username,
        String password,
        String firstName,
        String lastName
) {}