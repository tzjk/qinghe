package com.qinghe.life.redis;

import com.qinghe.life.exception.BusinessException;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

/** Keeps retry policy independent of exception message fragments in the consumer. */
@Component
public class SeckillStreamErrorClassifier {
    public Classification classify(Exception exception) {
        if (exception instanceof IllegalArgumentException) return Classification.invalid("STREAM_MESSAGE_INVALID");
        if (exception instanceof BusinessException) {
            BusinessException business = (BusinessException) exception;
            return business.getCode() == 400 ? Classification.invalid("STREAM_MESSAGE_INVALID") : Classification.permanent("STREAM_RETRY_EXHAUSTED");
        }
        if (exception instanceof DataAccessException) return Classification.retryable("DATABASE_TEMPORARY");
        return Classification.retryable("PERSISTENCE_TEMPORARY");
    }
    public static final class Classification {
        private final boolean retryable; private final String code;
        private Classification(boolean retryable, String code) { this.retryable = retryable; this.code = code; }
        public static Classification retryable(String code) { return new Classification(true, code); }
        public static Classification permanent(String code) { return new Classification(false, code); }
        public static Classification invalid(String code) { return new Classification(false, code); }
        public boolean isRetryable() { return retryable; }
        public String getCode() { return code; }
    }
}
