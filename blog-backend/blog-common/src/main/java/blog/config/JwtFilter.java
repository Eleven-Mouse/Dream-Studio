package blog.config;

import blog.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    private static final List<String> PERMIT_ALL_PATHS = List.of(
            "/v1/auth/login",
            "/v1/auth/test",
            "/user/login",
            "/user/register",
            "/captcha/send",
            "/actuator/health"
    );
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        if (isPermitAllPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);

            try {
                Claims claims = jwtUtil.parseToken(token);

                String username = claims.getSubject();

                List<String> roles = claims.get("roles", List.class);

                List<SimpleGrantedAuthority> authorities = null;
                if (roles != null) {
                    authorities = roles.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (Exception e) {
                logger.error("Token 校验失败: " + e.getMessage());
            }
        }

        chain.doFilter(request, response);
    }

    private boolean isPermitAllPath(String path) {
        for (String permitPath : PERMIT_ALL_PATHS) {
            if (path.startsWith(permitPath)) {
                return true;
            }
        }
        return false;
    }
}
