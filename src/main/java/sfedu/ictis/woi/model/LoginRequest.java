package sfedu.ictis.woi.model;

public record LoginRequest(
        String username,
        String password
) {}