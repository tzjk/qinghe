package com.qinghe.life.vo;

import com.qinghe.life.entity.Admin;
import java.util.Map;
import lombok.Data;

@Data
public class AdminInfoVO {
    private Long id;
    private String username;
    private String displayName;

    public static AdminInfoVO fromAdmin(Admin admin) {
        AdminInfoVO result = new AdminInfoVO();
        result.setId(admin.getId());
        result.setUsername(admin.getUsername());
        result.setDisplayName(admin.getDisplayName() == null ? admin.getUsername() : admin.getDisplayName());
        return result;
    }

    public static AdminInfoVO fromMap(Map<Object, Object> session) {
        AdminInfoVO result = new AdminInfoVO();
        result.setId(Long.valueOf(value(session, "adminId")));
        result.setUsername(value(session, "username"));
        result.setDisplayName(value(session, "displayName"));
        return result;
    }

    private static String value(Map<Object, Object> session, String key) {
        Object value = session.get(key);
        return value == null ? "" : String.valueOf(value);
    }
}
