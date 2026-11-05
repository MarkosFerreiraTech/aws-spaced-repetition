package com.spacedrep.domain.model;

/** Conta de usuario. O hash da senha nunca sai do dominio/infraestrutura. */
public record UserAccount(Long id, String email, String passwordHash, String displayName) {
}
