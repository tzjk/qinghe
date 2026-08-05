package com.qinghe.life.exception;

import com.qinghe.life.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Map<String, String> FIELD_LABELS = createFieldLabels();
    private static final Map<String, String> DUPLICATE_KEY_MESSAGES = createDuplicateKeyMessages();
    private static final Map<String, Pattern> DUPLICATE_KEY_PATTERNS = createDuplicateKeyPatterns();

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException exception) {
        Integer code = exception.getCode() == null ? 400 : exception.getCode();
        log.warn("业务请求未完成：code={}", code);
        return Result.fail(code, exception.getUserMessage());
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(ForbiddenException.class)
    public Result<Void> handleForbiddenException(ForbiddenException exception) {
        log.warn("权限不足：{}", exception.getMessage());
        return Result.fail(403, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        String message = bindingMessage(exception.getBindingResult());
        log.warn("请求体参数校验未通过：message={}", message);
        return Result.fail(400, message);
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException exception) {
        String message = bindingMessage(exception.getBindingResult());
        log.warn("请求参数校验未通过：message={}", message);
        return Result.fail(400, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException exception) {
        String message = constraintViolationMessage(exception);
        log.warn("约束参数校验未通过：message={}", message);
        return Result.fail(400, message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException exception) {
        String parameter = fieldLabel(exception.getParameterName());
        log.warn("缺少请求参数：parameter={}", parameter);
        return Result.fail(400, "缺少请求参数：" + parameter);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException exception) {
        String parameter = fieldLabel(exception.getName());
        log.warn("请求参数类型不匹配：parameter={}", parameter);
        return Result.fail(400, parameter + "格式不正确");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception) {
        log.warn("请求体无法读取，可能存在 JSON 格式错误");
        return Result.fail(400, "请求体格式错误，请检查 JSON 数据");
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicateKeyException(DuplicateKeyException exception) {
        String constraint = findDuplicateConstraint(exception);
        String message = constraint == null ? "数据已存在，请勿重复提交" : DUPLICATE_KEY_MESSAGES.get(constraint);
        log.warn("唯一约束冲突：constraint={}", constraint == null ? "unknown" : constraint);
        return Result.fail(409, message);
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result<Void> handleDataIntegrityViolationException(DataIntegrityViolationException exception) {
        logSystemError("数据约束异常", exception);
        return Result.fail(409, "数据约束不满足，请检查后重试");
    }

    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException exception) {
        log.warn("请求方法不支持");
        return Result.fail(405, "请求方法不支持");
    }

    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    @ExceptionHandler({RedisConnectionFailureException.class, CannotGetJdbcConnectionException.class,
            DataAccessResourceFailureException.class, CannotCreateTransactionException.class})
    public Result<Void> handleInfrastructureException(Exception exception) {
        logSystemError("基础设施服务不可用", exception);
        return Result.fail(503, "服务暂时不可用，请稍后重试");
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnexpectedException(Exception exception) {
        logSystemError("未处理的系统异常", exception);
        return Result.fail(500, "系统繁忙，请稍后重试");
    }

    private String bindingMessage(BindingResult bindingResult) {
        FieldError fieldError = bindingResult.getFieldError();
        if (fieldError == null) {
            return "请求参数不符合要求";
        }
        return validationMessage(fieldError.getField(), fieldError.getCode());
    }

    private String constraintViolationMessage(ConstraintViolationException exception) {
        if (exception.getConstraintViolations().isEmpty()) {
            return "请求参数不符合要求";
        }
        ConstraintViolation<?> violation = exception.getConstraintViolations().iterator().next();
        String rule = violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName();
        return validationMessage(lastProperty(violation.getPropertyPath().toString()), rule);
    }

    private String validationMessage(String field, String rule) {
        String label = fieldLabel(field);
        if (Arrays.asList("NotBlank", "NotEmpty", "NotNull").contains(rule)) {
            return label + "不能为空";
        }
        if ("Pattern".equals(rule) || "typeMismatch".equals(rule)) {
            return label + "格式不正确";
        }
        if ("Size".equals(rule)) {
            return label + "长度不符合要求";
        }
        if (Arrays.asList("Min", "Max", "Positive", "PositiveOrZero", "Negative", "NegativeOrZero").contains(rule)) {
            return label + "数值范围不正确";
        }
        return label + "不符合要求";
    }

    private String fieldLabel(String field) {
        String normalized = lastProperty(field);
        String label = FIELD_LABELS.get(normalized);
        return label == null ? "参数" : label;
    }

    private String lastProperty(String propertyPath) {
        if (propertyPath == null || propertyPath.trim().isEmpty()) {
            return "";
        }
        String[] segments = propertyPath.split("\\.");
        return segments[segments.length - 1];
    }

    private String findDuplicateConstraint(Throwable exception) {
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<Throwable, Boolean>());
        Throwable current = exception;
        while (current != null && visited.add(current)) {
            String detail = current.getMessage();
            if (detail != null) {
                for (Map.Entry<String, Pattern> entry : DUPLICATE_KEY_PATTERNS.entrySet()) {
                    if (entry.getValue().matcher(detail).find()) {
                        return entry.getKey();
                    }
                }
            }
            current = current.getCause();
        }
        return null;
    }

    private void logSystemError(String event, Throwable exception) {
        log.error("{}：type={}", event, exception.getClass().getName());
        log.error(event, safeThrowable(exception, Collections.newSetFromMap(new IdentityHashMap<Throwable, Boolean>())));
    }

    private RuntimeException safeThrowable(Throwable source, Set<Throwable> visited) {
        RuntimeException safe = new RuntimeException(source.getClass().getName());
        safe.setStackTrace(source.getStackTrace());
        Throwable cause = source.getCause();
        if (cause != null && visited.add(source)) {
            safe.initCause(safeThrowable(cause, visited));
        }
        return safe;
    }

    private static Map<String, String> createFieldLabels() {
        Map<String, String> labels = new LinkedHashMap<String, String>();
        labels.put("phone", "手机号");
        labels.put("username", "用户名");
        labels.put("password", "密码");
        labels.put("confirmPassword", "确认密码");
        labels.put("code", "验证码");
        labels.put("nickname", "昵称");
        labels.put("avatarUrl", "头像地址");
        labels.put("receiverName", "收件人姓名");
        labels.put("receiverPhone", "收件人手机号");
        labels.put("province", "省份");
        labels.put("city", "城市");
        labels.put("district", "区县");
        labels.put("detailAddress", "详细地址");
        labels.put("isDefault", "默认地址标记");
        labels.put("goodsId", "商品编号");
        labels.put("quantity", "数量");
        labels.put("selected", "选中状态");
        labels.put("page", "页码");
        labels.put("size", "页大小");
        labels.put("categoryId", "分类编号");
        labels.put("shopId", "商铺编号");
        labels.put("id", "编号");
        return Collections.unmodifiableMap(labels);
    }

    private static Map<String, String> createDuplicateKeyMessages() {
        Map<String, String> messages = new LinkedHashMap<String, String>();
        messages.put("uk_qh_user_phone", "该手机号已注册");
        messages.put("uk_qh_user_username", "用户名已存在");
        messages.put("uk_qh_admin_username", "管理员账号已存在");
        messages.put("uk_qh_category_name", "分类名称已存在");
        messages.put("uk_qh_cart_user_goods", "购物车中已存在该商品");
        messages.put("uk_qh_order_no", "订单号已存在，请稍后重试");
        messages.put("uk_qh_user_coupon", "该优惠券已领取");
        messages.put("uk_qh_comment_order", "该订单已提交评价");
        messages.put("uk_qh_blog_like", "已点赞，请勿重复操作");
        messages.put("uk_qh_blog_favorite", "已收藏，请勿重复操作");
        return Collections.unmodifiableMap(messages);
    }

    private static Map<String, Pattern> createDuplicateKeyPatterns() {
        Map<String, Pattern> patterns = new LinkedHashMap<String, Pattern>();
        for (String constraint : DUPLICATE_KEY_MESSAGES.keySet()) {
            patterns.put(constraint, Pattern.compile("(?i)(?:^|[^A-Za-z0-9_])" + Pattern.quote(constraint) + "(?:$|[^A-Za-z0-9_])"));
        }
        return Collections.unmodifiableMap(patterns);
    }
}
