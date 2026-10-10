package com.campus.lostandfound.dto;

/** The few facts we take from a verified Google ID token. */
public record GoogleProfile(String googleId, String email, String name) {
}
