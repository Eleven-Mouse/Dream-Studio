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

    /**
     * 定义不需要验证 token 的路径
     */
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

        // ✅ 如果是登录等公开接口，直接放行，不验证 token
        if (isPermitAllPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        // 1. 获取 Request Header 里的 Authorization
        String bearerToken = request.getHeader("Authorization");

        // 2. 判断 Token 是否存在且格式正确
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);

            try {
                // 3. 解析 Token
                Claims claims = jwtUtil.parseToken(token);

                // 4. 获取用户名
                String username = claims.getSubject();

                // 5. 获取权限信息
                List<String> roles = claims.get("roles", List.class);

                // 将 List<String> 转为 Spring Security 需要的 List<SimpleGrantedAuthority>
                List<SimpleGrantedAuthority> authorities = null;
                if (roles != null) {
                    authorities = roles.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());
                }

                // 6. 构建认证对象
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);

                // 7. 将认证信息存入 SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (Exception e) {
                logger.error("Token 校验失败: " + e.getMessage());
            }
        }

        // 8. 继续执行过滤器链
        chain.doFilter(request, response);
    }

    /**
     * 判断当前路径是否需要跳过 token 验证
     */
    private boolean isPermitAllPath(String path) {
        for (String permitPath : PERMIT_ALL_PATHS) {
            if (path.startsWith(permitPath)) {
                return true;
            }
        }
        return false;
    }
}