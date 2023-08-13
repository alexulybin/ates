package ru.toughdev.ates.authn.security.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtTokenFilter extends GenericFilterBean {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain filterChain)
            throws IOException, ServletException {

        var request = (HttpServletRequest) req;
        // нужно для доступа к консоли H2 и логину
        if (request.getRequestURI().startsWith("/h2-console") || request.getRequestURI().startsWith("/api/auth/login")) {
            filterChain.doFilter(req, res);
            return;
        }

        try {
            var token = jwtTokenProvider.resolveToken((HttpServletRequest) req);
            if (jwtTokenProvider.validateToken(token)) {
                var auth = jwtTokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(auth);
                filterChain.doFilter(req, res);
            }
        } catch (Exception e) {
            ((HttpServletResponse) res).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.getOutputStream().write(e.getMessage().getBytes());
        }
    }
}
