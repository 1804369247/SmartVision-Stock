package com.example.smartvisionstock.interceptor;

import com.example.smartvisionstock.annotation.RequiresPermission;
import com.example.smartvisionstock.util.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 权限拦截器：校验 @RequiresPermission 注解的接口访问权限
 * 依赖 JwtAuthenticationFilter 已将用户信息写入 UserContext
 */
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();
        RequiresPermission permissionAnnotation = method.getAnnotation(RequiresPermission.class);

        // 未标注权限注解，直接放行（认证已由 JwtAuthenticationFilter + SecurityConfig 处理）
        if (permissionAnnotation == null) {
            return true;
        }

        // 通过 UserContext 获取当前登录用户（由 JwtAuthenticationFilter 在请求开始时写入）
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "请先登录");
            return false;
        }

        String currentRole = UserContext.getCurrentRole();
        // ADMIN 拥有所有权限，直接放行
        if ("ADMIN".equalsIgnoreCase(currentRole)) {
            return true;
        }

        // 注解中可配置多个允许角色（逗号分隔），只要当前角色匹配其中一个即可通过
        String[] requiredValues = permissionAnnotation.value().split(",");
        boolean hasPermission = Arrays.stream(requiredValues)
                .map(String::trim)
                .anyMatch(v -> v.equalsIgnoreCase(currentRole));

        if (!hasPermission) {
            sendErrorResponse(response, HttpStatus.FORBIDDEN, "权限不足，无法访问该接口");
            return false;
        }

        return true;
    }

    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws Exception {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", message);

        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
