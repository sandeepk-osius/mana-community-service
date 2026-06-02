package com.manacommunity.api.exception;

import org.springframework.http.HttpStatus;

/** Thrown when the community invite code provided during registration is invalid. */
public class InvalidInviteCodeException extends ManaCommunityException {

    public InvalidInviteCodeException(String code) {
        super("No community found for invite code: '" + code + "'. Please check and try again.",
                HttpStatus.BAD_REQUEST, "INVALID_INVITE_CODE");
    }
}
