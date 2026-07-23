package com.qinghe.life;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qinghe.life.common.Result;
import com.qinghe.life.dto.SendCodeRequest;
import com.qinghe.life.exception.BusinessException;
import com.qinghe.life.exception.GlobalExceptionHandler;
import com.qinghe.life.interceptor.LoginInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void businessExceptionKeepsBusinessCodeAndMessage() {
        Result<Void> result = handler.handleBusinessException(new BusinessException(404, "地址不存在或无权操作"));

        assertEquals(Integer.valueOf(404), result.getCode());
        assertEquals("地址不存在或无权操作", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void dtoValidationReturnsFriendlyFieldMessage() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new SendCodeRequest(), "request");
        bindingResult.addError(new FieldError("request", "phone", null, false, new String[]{"Pattern"}, null, "手机号格式不正确"));

        Result<Void> result = handler.handleMethodArgumentNotValidException(new MethodArgumentNotValidException(null, bindingResult));

        assertEquals(Integer.valueOf(400), result.getCode());
        assertEquals("手机号格式不正确", result.getMessage());
    }

    @Test
    void malformedJsonReturnsSafeMessage() {
        Result<Void> result = handler.handleHttpMessageNotReadableException(new HttpMessageNotReadableException("invalid json"));

        assertEquals(Integer.valueOf(400), result.getCode());
        assertEquals("请求体格式错误，请检查 JSON 数据", result.getMessage());
        assertFalse(result.getMessage().contains("invalid json"));
    }

    @Test
    void knownDuplicateKeyUsesConstraintWhitelistWithoutLeakingSql() {
        String detail = "Duplicate entry '13900009991' for key 'uk_qh_user_phone'";
        Result<Void> result = handler.handleDuplicateKeyException(new DuplicateKeyException(detail));

        assertEquals(Integer.valueOf(409), result.getCode());
        assertEquals("该手机号已注册", result.getMessage());
        assertFalse(result.getMessage().contains("Duplicate entry"));
        assertFalse(result.getMessage().contains("uk_qh_user_phone"));
    }

    @Test
    void unknownDuplicateKeyUsesGenericMessageWithoutLeakingSql() {
        String detail = "Duplicate entry 'sensitive-value' for key 'uk_unknown_constraint'";
        Result<Void> result = handler.handleDuplicateKeyException(new DuplicateKeyException(detail));

        assertEquals(Integer.valueOf(409), result.getCode());
        assertEquals("数据已存在，请勿重复提交", result.getMessage());
        assertFalse(result.getMessage().contains("sensitive-value"));
        assertFalse(result.getMessage().contains("uk_unknown_constraint"));
    }

    @Test
    void cartAndOrderDuplicateKeysUseTheirOwnFriendlyMessages() {
        Result<Void> cart = handler.handleDuplicateKeyException(new DuplicateKeyException("for key 'uk_qh_cart_user_goods'"));
        Result<Void> order = handler.handleDuplicateKeyException(new DuplicateKeyException("for key 'uk_qh_order_no'"));

        assertEquals("购物车中已存在该商品", cart.getMessage());
        assertEquals("订单号已存在，请稍后重试", order.getMessage());
    }

    @Test
    void integrityAndUnknownExceptionsUseSafeMessages() {
        Result<Void> integrity = handler.handleDataIntegrityViolationException(new DataIntegrityViolationException("SQL must not be exposed"));
        Result<Void> unexpected = handler.handleUnexpectedException(new IllegalStateException("internal detail"));

        assertEquals("数据约束不满足，请检查后重试", integrity.getMessage());
        assertFalse(integrity.getMessage().contains("SQL"));
        assertEquals(Integer.valueOf(500), unexpected.getCode());
        assertEquals("系统繁忙，请稍后重试", unexpected.getMessage());
        assertFalse(unexpected.getMessage().contains("internal detail"));
    }

    @Test
    void unauthenticatedRequestStillReturnsHttp401() throws Exception {
        LoginInterceptor interceptor = new LoginInterceptor(new ObjectMapper());
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean continueChain = interceptor.preHandle(new MockHttpServletRequest("GET", "/api/cart"), response, new Object());

        assertFalse(continueChain);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("未登录或登录已过期"));
    }
}
