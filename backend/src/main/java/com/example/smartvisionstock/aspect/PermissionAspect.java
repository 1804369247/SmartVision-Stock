package com.example.smartvisionstock.aspect;

import com.example.smartvisionstock.annotation.RequiresPermission;
import com.example.smartvisionstock.service.UserService;
import com.example.smartvisionstock.util.UserContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;

/**
 * @RequiresPermission 注解的 AOP 切面
 * 拦截所有带有该注解的 Controller 方法，校验当前登录用户是否具备对应权限。
 * 若无权限则抛出 403 Forbidden 异常，由全局异常处理器统一返回给前端。
 */
@Aspect
@Component
public class PermissionAspect {

    @Autowired
    private UserService userService;

    @Around("@annotation(com.example.smartvisionstock.annotation.RequiresPermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequiresPermission annotation = method.getAnnotation(RequiresPermission.class);

        if (annotation == null) {
            return joinPoint.proceed();
        }

        String requiredPermission = annotation.value();

        // 获取当前登录用户ID
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
        }

        // ADMIN 角色直接放行（不走权限表）
        String role = UserContext.getCurrentRole();
        if ("ADMIN".equals(role)) {
            return joinPoint.proceed();
        }

        // 校验权限
        boolean hasPermission = userService.checkPermission(currentUserId, requiredPermission);
        if (!hasPermission) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    String.format("权限不足：当前用户[%s]缺少[%s]权限", UserContext.getCurrentUsername(), requiredPermission)
            );
        }

        return joinPoint.proceed();
    }
}
