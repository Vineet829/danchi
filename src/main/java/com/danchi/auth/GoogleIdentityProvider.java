package com.danchi.auth;

public interface GoogleIdentityProvider {

    GoogleUserInfo exchange(String code, String redirectUri);
}
