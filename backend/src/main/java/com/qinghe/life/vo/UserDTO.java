package com.qinghe.life.vo;
import com.qinghe.life.entity.User;
import lombok.Data;
import java.util.HashMap;
import java.util.Map;
@Data public class UserDTO {
    private Long id; private String username; private String nickname; private String avatarUrl; private String phoneMasked; private Boolean profileCompleted; private Boolean hasPassword;
    private String realName; private String studentNo; private Boolean hasStudentProfile;
    public static UserDTO fromUser(User user) { UserDTO dto = new UserDTO(); dto.setId(user.getId()); dto.setUsername(user.getUsername()); dto.setNickname(user.getNickname()); dto.setAvatarUrl(user.getAvatarUrl()); dto.setPhoneMasked(mask(user.getPhone())); dto.setProfileCompleted(Integer.valueOf(1).equals(user.getProfileCompleted())); dto.setHasPassword(user.getPasswordHash() != null && !user.getPasswordHash().trim().isEmpty()); return dto; }
    /** Redis session is deliberately authentication/display-minimal. Profile data remains in MySQL. */
    public Map<String, String> toMap() { Map<String,String> map = new HashMap<String,String>(); map.put("id", String.valueOf(id)); map.put("username", username == null ? "" : username); map.put("nickname", nickname == null ? "" : nickname); map.put("avatarUrl", avatarUrl == null ? "" : avatarUrl); map.put("profileCompleted", String.valueOf(Boolean.TRUE.equals(profileCompleted))); map.put("hasPassword", String.valueOf(Boolean.TRUE.equals(hasPassword))); map.put("hasStudentProfile", String.valueOf(Boolean.TRUE.equals(hasStudentProfile))); return map; }
    public static UserDTO fromMap(Map<Object,Object> map) { UserDTO dto = new UserDTO(); dto.setId(Long.valueOf(value(map, "id"))); dto.setUsername(value(map, "username")); dto.setNickname(value(map, "nickname")); dto.setAvatarUrl(value(map, "avatarUrl")); dto.setProfileCompleted(Boolean.valueOf(value(map, "profileCompleted"))); dto.setHasPassword(Boolean.valueOf(value(map, "hasPassword"))); dto.setHasStudentProfile(Boolean.valueOf(value(map, "hasStudentProfile"))); return dto; }
    private static String value(Map<Object,Object> map, String key) { Object value = map.get(key); return value == null ? "" : String.valueOf(value); }
    private static String mask(String phone) { return phone == null || phone.length() < 7 ? "" : phone.substring(0,3) + "****" + phone.substring(phone.length()-4); }
}
