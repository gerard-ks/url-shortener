package org.urlshortener.urlshortener.service.implementation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.urlshortener.urlshortener.service.interfaces.TokenService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import static org.urlshortener.urlshortener.constants.Constants.*;

@Service
@Slf4j
public class TokenServiceImpl implements TokenService {

    @Value("${app.security.token-secret}")
    private String secret;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Génère un token de propriété unique.
     * Format : tok_<32 caractères Base62>
     * Exemple : tok_aB3xY9kLmN2pQ5rT8vW1zC4fH7jK0sD
     *
     * @return token en clair (à retourner au client UNE SEULE FOIS)
     */
    @Override
    public String generateToken() {
        StringBuilder token = new StringBuilder(TOKEN_PREFIX);

        for (int i = 0; i < TOKEN_LENGTH; i++) {
            int index = secureRandom.nextInt(BASE62_ALPHABET.length());
            token.append(BASE62_ALPHABET.charAt(index));
        }

        String generatedToken = token.toString();
        log.debug("Token generated (length: {})", generatedToken.length());

        return generatedToken;
    }

    /**
     * Hash un token avec SHA-256.
     * Le hash est stocké en base, JAMAIS le token en clair.
     *
     * @param token le token en clair
     * @return hash hexadécimal (64 caractères)
     */
    @Override
    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = digest.digest((token + secret).getBytes(StandardCharsets.UTF_8));

            // Convertir en hexadécimal
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // Ne devrait jamais arriver (SHA-256 toujours disponible)
            log.error("SHA-256 algorithm not found", e);
            throw new IllegalStateException("Hash algorithm not available", e);
        }
    }

    /**
     * Vérifie qu'un token correspond à un hash.
     * Utilise une comparaison en temps constant (anti timing-attack).
     *
     * @param providedToken le token fourni par le client
     * @param storedHash le hash stocké en base
     * @return true si le token est valide
     */
    @Override
    public boolean verifyToken(String providedToken, String storedHash) {
        String providedHash = hashToken(providedToken);
        return timingSafeEquals(providedHash, storedHash);
    }

    /**
     * Comparaison en temps constant (protection timing-attack).
     * Évite de révéler la longueur du hash par timing.
     *
     * @param a première chaîne
     * @param b seconde chaîne
     * @return true si égales
     */
    private boolean timingSafeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }

        if (a.length() != b.length()) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }

        return result == 0;
    }

    /**
     * Valide le format d'un token.
     *
     * @param token le token à valider
     * @return true si le format est valide
     */
    @Override
    public boolean isValidTokenFormat(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        if (!token.startsWith(TOKEN_PREFIX)) {
            return false;
        }

        if (token.length() != TOKEN_PREFIX.length() + TOKEN_LENGTH) {
            return false;
        }

        // Vérifier que le corps ne contient que des caractères Base62
        String body = token.substring(TOKEN_PREFIX.length());
        for (char c : body.toCharArray()) {
            if (BASE62_ALPHABET.indexOf(c) == -1) {
                return false;
            }
        }

        return true;
    }
}
