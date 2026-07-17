package com.example.smartvisionstock.config;

import com.example.smartvisionstock.util.JwtUtil;
import com.example.smartvisionstock.util.TokenBlacklist;
import com.example.smartvisionstock.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

/**
 * JWT 认证过滤器：每次请求只执行一次，解析 Authorization 头中的 token，
 * 将用户信息（userId、username、role）写入 SecurityContext 和 UserContext（ThreadLocal）。
 * 同时检查 Token 是否在黑名单中（已登出），黑名单中的 Token 直接返回 401。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenBlacklist tokenBlacklist;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                // 检查 Token 是否有效（Access Token，拒绝 Refresh Token）
                if (!jwtUtil.validateAccessToken(token)) {
                    logger.debug("JWT Token 无效或已过期");
                    // Token 无效，主动返回 401，让前端拦截器处理（刷新Token或跳转登录）
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":401,\"message\":\"Token 已失效，请重新登录\"}");
                    return;
                }

                // 检查 Token 是否在黑名单中（已登出）
                String jti = jwtUtil.getJtiFromToken(token);
                if (tokenBlacklist.isBlacklisted(jti)) {
                    logger.debug("JWT Token 已失效（已在黑名单中），返回 401");
                    // 黑名单中的 Token 主动返回 401，不继续过滤器链
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":401,\"message\":\"Token 已失效，请重新登录\"}");
                    return;
                }

                Long userId   = jwtUtil.getUserIdFromToken(token);
                String username = jwtUtil.getUsernameFromToken(token);
                String role     = jwtUtil.getRoleFromToken(token);

                if (userId != null && username != null
                        && SecurityContextHolder.getContext().getAuthentication() == null) {

                    // 写入 ThreadLocal，供 Service / Aspect 层使用（含角色）
                    UserContext.setCurrentUser(userId, username, role);

                    // 写入 Spring Security Context（用角色构建权限标识）
                    String authority = "ROLE_" + (role != null ? role : "USER");
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    username,
                                    null,
                                    Collections.singletonList(new SimpleGrantedAuthority(authority))
                            );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }

            // 继续过滤器链（请求不携带 Token 或 Token 有效时）
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            // token 格式严重错误（非标准 JWT），不阻断请求，让 Spring Security 拦截
            logger.debug("JWT 过滤器异常: " + e.getMessage());
            filterChain.doFilter(request, response);
        } finally {
            // 请求结束，清理 ThreadLocal，防止线程复用时数据污染
            UserContext.clear();
        }
    }
}
