package com.qinghe.life.utils;
import com.qinghe.life.vo.UserDTO;
public final class UserContext {
    private static final ThreadLocal<UserDTO> USER_HOLDER = new ThreadLocal<UserDTO>();
    private static final ThreadLocal<String> TOKEN_HOLDER = new ThreadLocal<String>();

    private UserContext() {
    }

    public static void setUser(UserDTO user) { USER_HOLDER.set(user); }
    public static UserDTO getUser() { return USER_HOLDER.get(); }
    public static Long getUserId() { return getUser() == null ? null : getUser().getId(); }
    public static void setToken(String token) { TOKEN_HOLDER.set(token); }
    public static String getToken() { return TOKEN_HOLDER.get(); }
    public static void clear() { USER_HOLDER.remove(); TOKEN_HOLDER.remove(); }
}
