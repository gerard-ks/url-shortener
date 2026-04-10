package org.urlshortener.urlshortener.filter.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.urlshortener.urlshortener.exception.security.UnauthorizedOperationException;
import org.urlshortener.urlshortener.service.interfaces.TokenService;

import java.io.IOException;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Order(2) // Après RateLimitFilter (HIGHEST_PRECEDENCE)
@Slf4j
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    public static final String TOKEN_ATTRIBUTE = "auth.token";

    private static final Set<String> PROTECTED_METHODS = Set.of("PUT", "DELETE");

    private final TokenService tokenService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String method = request.getMethod();
        String path = request.getRequestURI();

        // Vérifier si l'endpoint est protégé
        if (!requiresAuthentication(method, path)) {
            chain.doFilter(request, response);
            return;
        }

        // Extraire le token
        String token = extractToken(request);

        // Valider le format
        if (!tokenService.isValidTokenFormat(token)) {
            throw UnauthorizedOperationException.invalidToken();
        }

        // Stocker le token dans la requête
        request.setAttribute(TOKEN_ATTRIBUTE, token);

        log.debug("Token authenticated for {} {}", method, path);

        chain.doFilter(request, response);
    }


    private boolean requiresAuthentication(String method, String path) {
        return PROTECTED_METHODS.contains(method)
                && path.startsWith("/api/v1/shorten");
    }


    private String extractToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");

        if (authorization == null || authorization.isBlank()) {
            throw UnauthorizedOperationException.missingToken();
        }

        if (!authorization.startsWith("Bearer ")) {
            throw UnauthorizedOperationException.missingToken();
        }

        String token = authorization.substring(7).trim();

        if (token.isEmpty()) {
            throw UnauthorizedOperationException.missingToken();
        }

        return token;
    }
}
