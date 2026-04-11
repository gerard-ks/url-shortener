package org.urlshortener.urlshortener.filter.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.urlshortener.urlshortener.exception.ratelimit.RateLimitExceededException;

import java.io.IOException;


@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;

    public RateLimitFilter(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator")
                || path.startsWith("/swagger")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/favicon.ico");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String ip = getClientIp(request);
        String method = request.getMethod();
        String path = request.getRequestURI();

        // Vérifier limite et obtenir résultat
        RateLimitResult result = rateLimitService.tryConsume(ip, method, path);

        // Ajouter headers de rate limit
        result.toHeaders().forEach(response::setHeader);

        // Si limite dépassée, lancer exception
        if (result.isLimited()) {
            throw new RateLimitExceededException(result.retryAfterSeconds());
        }

        // Continuer la chaîne de filtres
        chain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        // Support des proxies (X-Forwarded-For)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
