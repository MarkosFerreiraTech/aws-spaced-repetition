package com.spacedrep.infrastructure.security;

/** Principal da requisicao autenticada, extraido do JWT. */
public record AuthenticatedUser(long userId, String email) {
}
