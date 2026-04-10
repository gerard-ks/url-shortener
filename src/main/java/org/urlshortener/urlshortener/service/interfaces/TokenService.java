package org.urlshortener.urlshortener.service.interfaces;

public interface TokenService {
    String generateToken();
    String hashToken(String token);
    boolean verifyToken(String providedToken, String storedHash);
    boolean isValidTokenFormat(String token);
}
