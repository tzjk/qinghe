package com.qinghe.life;

import com.qinghe.life.annotation.OperateLog;
import com.qinghe.life.service.OperateLogService;
import com.qinghe.life.utils.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Import(OperateLogAspectFailureToleranceTest.TestConfig.class)
class OperateLogAspectFailureToleranceTest {
    @MockBean
    private OperateLogService operateLogService;

    @org.springframework.beans.factory.annotation.Autowired
    private FailureToleranceFacade facade;

    @AfterEach
    void cleanThreadLocals() {
        UserContext.clear();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void logStorageFailureDoesNotChangeBusinessResult() {
        doThrow(new IllegalStateException("store failed")).when(operateLogService).save(any());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
                new MockHttpServletRequest("POST", "/api/test/operate-log/storage-failure")));

        String result = facade.successWhenLoggingFails();

        assertEquals("business-ok", result);
        ArgumentCaptor<com.qinghe.life.entity.OperateLog> captor = ArgumentCaptor.forClass(com.qinghe.life.entity.OperateLog.class);
        verify(operateLogService).save(captor.capture());
        assertEquals(Integer.valueOf(1), captor.getValue().getSuccess());
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        FailureToleranceFacade failureToleranceFacade() {
            return new FailureToleranceFacade();
        }
    }

    static class FailureToleranceFacade {
        @OperateLog(module = "操作日志测试", action = "OPERATE_LOG_TEST_保存失败")
        public String successWhenLoggingFails() {
            return "business-ok";
        }
    }
}
