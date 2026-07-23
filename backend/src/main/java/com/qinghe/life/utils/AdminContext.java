package com.qinghe.life.utils;

import com.qinghe.life.vo.AdminInfoVO;

public final class AdminContext {
    private static final ThreadLocal<AdminInfoVO> ADMIN_HOLDER = new ThreadLocal<AdminInfoVO>();
    private static final ThreadLocal<String> TOKEN_HOLDER = new ThreadLocal<String>();

    private AdminContext() {
    }

    public static void setAdmin(AdminInfoVO admin) {
        ADMIN_HOLDER.set(admin);
    }

    public static AdminInfoVO getAdmin() {
        return ADMIN_HOLDER.get();
    }

    public static Long getAdminId() {
        return getAdmin() == null ? null : getAdmin().getId();
    }

    public static void setToken(String token) {
        TOKEN_HOLDER.set(token);
    }

    public static String getToken() {
        return TOKEN_HOLDER.get();
    }

    public static void clear() {
        ADMIN_HOLDER.remove();
        TOKEN_HOLDER.remove();
    }
}
