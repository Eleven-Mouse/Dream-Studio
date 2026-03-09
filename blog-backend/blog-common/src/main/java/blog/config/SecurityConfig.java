package blog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    public SecurityConfig(JwtFilter jwtFilter, JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) {
        this.jwtFilter = jwtFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
    }

    /**
     * 1. 密码加密器
     * Spring Security 要求必须配置，这里使用最标准的 BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 2. 暴露 AuthenticationManager
     * 在 Login 接口中需要用它来手动触发认证（验证账号密码）
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * 3. 核心安全过滤链配置
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF
                .csrf(AbstractHttpConfigurer::disable)

                // 启用 CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 设置 Session 管理为无状态
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 配置异常处理
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))

                // 配置路径拦截规则
                .authorizeHttpRequests(auth -> auth
                        // 允许静态资源
                        .requestMatchers("/images/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // ✅ 放行登录接口（关键修改！）
                        .requestMatchers("/v1/auth/login").permitAll()
                        .requestMatchers("/v1/auth/test").permitAll()

                        // 放行管理员登录接口
                        .requestMatchers("/admin/auth/**").permitAll()

                        // 公共只读接口 (GET 请求)
                        .requestMatchers(HttpMethod.GET,
                                "/api/articles/**",
                                "/api/categories/**",
                                "/api/tags/**",
                                "/api/moments/**",
                                "/api/friendlinks/**",
                                "/api/archive/**",
                                "/api/comments/**"
                        ).permitAll()

                        // 评论接口 (POST 请求)
                        .requestMatchers(HttpMethod.POST, "/api/comments/**").permitAll()

                        // 管理员写操作 (需要 ADMIN 角色)
                        .requestMatchers(HttpMethod.POST, "/articles/**", "/moment/**", "/friendlinks/**", "/upload/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/articles/**", "/moment/**", "/friendlinks/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/articles/**", "/moment/**", "/friendlinks/**").hasRole("ADMIN")

                        // 后台管理页面
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // 其他所有请求都需要认证
                        .anyRequest().authenticated()
                )

                // 添加 JWT 过滤器
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 4. CORS 跨域配置源
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 允许的前端域名
        configuration.setAllowedOrigins(List.of(
                "http://localhost:8080",
                "http://localhost:3000",
                "http://8.162.7.124:8080",
                "http://8.162.7.124:3000"
        ));

        // 允许的方法
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // 允许的 Header
        configuration.setAllowedHeaders(List.of("*"));

        // 是否允许携带 Cookie
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对所有接口应用该配置
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}