package com.example.smartvisionstock.aspect;

import com.example.smartvisionstock.annotation.LogOperation;
import com.example.smartvisionstock.entity.OperationLog;
import com.example.smartvisionstock.repository.OperationLogRepository;
import com.example.smartvisionstock.util.UserContext;
import javax.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 操作日志 AOP 切面
 * 拦截所有标注了 @LogOperation 的方法，自动记录操作日志到数据库
 * 使用 UserContext 获取当前用户（由 JwtAuthenticationFilter 写入），避免重复解析 JWT
 */
@Aspect
@Component
public class OperationLogAspect {

    @Autowired
    private OperationLogRepository operationLogRepository;

    @Around("@annotation(com.example.smartvisionstock.annotation.LogOperation)")
    public Object logOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        LogOperation logAnnotation = method.getAnnotation(LogOperation.class);

        OperationLog log = new OperationLog();
        log.setOperationType(logAnnotation.type());
        log.setModule(logAnnotation.module());
        log.setDetail(logAnnotation.description());

        // 优先从 UserContext 获取当前登录用户（JwtAuthenticationFilter 已在请求进入时设置）
        Long userId = UserContext.getCurrentUserId();
        String username = UserContext.getCurrentUsername();
        if (userId != null) {
            log.setOperatorId(userId);
            log.setOperatorName(username);
        } else {
            // 未登录时兜底
            log.setOperatorId(0L);
            log.setOperatorName("anonymous");
        }

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String ipAddress = request.getRemoteAddr();
        if ("0:0:0:0:0:0:0:1".equals(ipAddress)) {
            ipAddress = "127.0.0.1";
        }
        log.setIpAddress(ipAddress);

        Object result = null;
        try {
            result = joinPoint.proceed();
            log.setDetail(log.getDetail() + " - 成功");
            return result;
        } catch (Throwable e) {
            log.setDetail(log.getDetail() + " - 失败: " + e.getMessage());
            throw e;
        } finally {
            operationLogRepository.save(log);
        }
    }
}
