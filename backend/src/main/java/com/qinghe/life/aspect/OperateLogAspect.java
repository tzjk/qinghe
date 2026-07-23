package com.qinghe.life.aspect;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qinghe.life.annotation.OperateLog;
import com.qinghe.life.service.OperateLogService;
import com.qinghe.life.utils.AdminContext;
import com.qinghe.life.utils.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Aspect
@Component
public class OperateLogAspect {
    public static final int MAX_SUMMARY_LENGTH = 2000;
    private static final String MASK = "***";
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?<!\\d)(1\\d{2})\\d{4}(\\d{4})(?!\\d)");

    private final OperateLogService operateLogService;
    private final ObjectMapper objectMapper;

    public OperateLogAspect(OperateLogService operateLogService, ObjectMapper objectMapper) {
        this.operateLogService = operateLogService;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(operateLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperateLog operateLog) throws Throwable {
        long startedAt = System.nanoTime();
        Object result = null;
        Throwable failure = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable throwable) {
            failure = throwable;
            throw throwable;
        } finally {
            writeLogSafely(joinPoint, operateLog, result, failure, startedAt);
        }
    }

    private void writeLogSafely(ProceedingJoinPoint joinPoint, OperateLog annotation, Object result,
                                Throwable failure, long startedAt) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            HttpServletRequest request = currentRequest();
            com.qinghe.life.entity.OperateLog logEntity = new com.qinghe.life.entity.OperateLog();
            logEntity.setUserId(UserContext.getUserId() == null ? AdminContext.getAdminId() : UserContext.getUserId());
            logEntity.setModule(annotation.module());
            logEntity.setAction(annotation.action());
            logEntity.setControllerClass(signature.getDeclaringType().getSimpleName());
            logEntity.setControllerMethod(signature.getMethod().getName());
            logEntity.setRequestPath(request == null ? "" : request.getRequestURI());
            logEntity.setHttpMethod(request == null ? "" : request.getMethod());
            logEntity.setRequestSummary(summarizeArguments(joinPoint.getArgs()));
            logEntity.setResponseSummary(failure == null ? summarize(result) : null);
            logEntity.setSuccess(failure == null ? 1 : 0);
            logEntity.setExceptionSummary(failure == null ? null : summarizeException(failure));
            logEntity.setDurationMs((System.nanoTime() - startedAt) / 1_000_000L);
            logEntity.setIp(request == null ? null : clientIp(request));
            logEntity.setOperateTime(LocalDateTime.now());
            operateLogService.save(logEntity);
        } catch (Exception exception) {
            log.error("操作日志保存失败，已忽略：type={}", exception.getClass().getName());
        }
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes) {
            return ((ServletRequestAttributes) attributes).getRequest();
        }
        return null;
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.trim().isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String summarizeArguments(Object[] args) {
        List<Object> safeArgs = new ArrayList<Object>();
        if (args != null) {
            for (Object arg : args) {
                safeArgs.add(sanitize(arg, null, new IdentityHashMap<Object, Boolean>()));
            }
        }
        return toLimitedJson(safeArgs);
    }

    private String summarize(Object value) {
        return toLimitedJson(sanitize(value, null, new IdentityHashMap<Object, Boolean>()));
    }

    private String summarizeException(Throwable failure) {
        return limitAndMask(failure.getClass().getSimpleName());
    }

    private String toLimitedJson(Object value) {
        try {
            return limitAndMask(objectMapper.writeValueAsString(value));
        } catch (Exception exception) {
            return "[serialization_failed:" + valueType(value) + "]";
        }
    }

    private Object sanitize(Object value, String fieldName, IdentityHashMap<Object, Boolean> visited) {
        if (isSensitiveField(fieldName)) {
            return MASK;
        }
        if (isPhoneField(fieldName) && value instanceof CharSequence) {
            return maskPhone(value.toString());
        }
        if (value == null || value instanceof CharSequence || value instanceof Number
                || value instanceof Boolean || value instanceof Enum || value instanceof Character) {
            return value;
        }
        if (isUnsafeValue(value)) {
            return "[omitted:" + valueType(value) + "]";
        }
        if (visited.put(value, Boolean.TRUE) != null) {
            return "[circular_reference]";
        }
        try {
            if (value instanceof Map) {
                Map<Object, Object> safeMap = new LinkedHashMap<Object, Object>();
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                    String key = String.valueOf(entry.getKey());
                    safeMap.put(key, sanitize(entry.getValue(), key, visited));
                }
                return safeMap;
            }
            if (value instanceof Collection) {
                List<Object> safeValues = new ArrayList<Object>();
                for (Object item : (Collection<?>) value) {
                    safeValues.add(sanitize(item, null, visited));
                }
                return safeValues;
            }
            if (value.getClass().isArray()) {
                List<Object> safeValues = new ArrayList<Object>();
                for (int index = 0; index < Array.getLength(value); index++) {
                    safeValues.add(sanitize(Array.get(value, index), null, visited));
                }
                return safeValues;
            }
            Map<String, Object> bean = objectMapper.convertValue(value, new TypeReference<Map<String, Object>>() { });
            return sanitize(bean, null, visited);
        } catch (Exception exception) {
            return "[omitted:" + valueType(value) + "]";
        } finally {
            visited.remove(value);
        }
    }

    private boolean isUnsafeValue(Object value) {
        return value instanceof ServletRequest || value instanceof ServletResponse || value instanceof MultipartFile
                || value instanceof InputStream || value instanceof Reader || value instanceof File
                || value instanceof ByteBuffer || value instanceof byte[];
    }

    private boolean isSensitiveField(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        String normalized = fieldName.replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
        return normalized.contains("password") || normalized.contains("token") || normalized.contains("authorization") || normalized.contains("qrcontent") || normalized.contains("studentno") || normalized.contains("realname") || normalized.equals("reason")
                || normalized.contains("redispassword") || normalized.contains("databasepassword")
                || normalized.equals("code") || normalized.endsWith("code");
    }

    private boolean isPhoneField(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        String normalized = fieldName.toLowerCase(Locale.ROOT);
        return normalized.contains("phone") || normalized.contains("mobile") || normalized.endsWith("tel");
    }

    private String limitAndMask(String raw) {
        String phoneMasked = maskPhone(raw == null ? "" : raw);
        if (phoneMasked.length() <= MAX_SUMMARY_LENGTH) {
            return phoneMasked;
        }
        return phoneMasked.substring(0, MAX_SUMMARY_LENGTH - 3) + "...";
    }

    private String maskPhone(String raw) {
        Matcher matcher = PHONE_PATTERN.matcher(raw == null ? "" : raw);
        return matcher.replaceAll("$1****$2");
    }

    private String valueType(Object value) {
        return value == null ? "null" : value.getClass().getSimpleName();
    }
}
