package com.qinghe.life.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qinghe.life.dto.SendCodeRequest;
import com.qinghe.life.dto.UserLoginRequest;
import com.qinghe.life.dto.PasswordLoginRequest;
import com.qinghe.life.dto.InitialProfileCompleteRequest;
import com.qinghe.life.dto.UserProfileUpdateRequest;
import com.qinghe.life.dto.UserRegisterRequest;
import com.qinghe.life.dto.AddressCreateDTO;
import com.qinghe.life.entity.User;
import com.qinghe.life.entity.StudentProfile;
import com.qinghe.life.exception.BusinessException;
import com.qinghe.life.mapper.UserMapper;
import com.qinghe.life.mapper.StudentProfileMapper;
import com.qinghe.life.oss.AliyunOSSOperator;
import com.qinghe.life.service.UserService;
import com.qinghe.life.service.AddressService;
import com.qinghe.life.utils.RedisKeys;
import com.qinghe.life.utils.UserContext;
import com.qinghe.life.vo.LoginVO;
import com.qinghe.life.vo.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserMapper userMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final StringRedisTemplate redisTemplate;
    private final Environment environment;
    private final AddressService addressService;
    private final AliyunOSSOperator ossOperator;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserServiceImpl(UserMapper userMapper, StudentProfileMapper studentProfileMapper, StringRedisTemplate redisTemplate, Environment environment, AddressService addressService, AliyunOSSOperator ossOperator) {
        this.userMapper = userMapper;
        this.studentProfileMapper = studentProfileMapper;
        this.redisTemplate = redisTemplate;
        this.environment = environment;
        this.addressService = addressService;
        this.ossOperator = ossOperator;
    }

    @Override
    public String sendCode(SendCodeRequest request) {
        String phone = request.getPhone();
        log.info("验证码发送请求，手机号 {}", maskPhone(phone));
        try {
            String code = createCode();
            redisTemplate.opsForValue().set(RedisKeys.code(phone), code, RedisKeys.LOGIN_CODE_TTL_MINUTES, TimeUnit.MINUTES);
            log.info("验证码已写入 Redis，手机号 {}", maskPhone(phone));
            if (isDevelopmentProfile()) {
                log.info("[LOCAL DEV ONLY] phone={}, code={}, ttlMinutes={}", maskPhone(phone), code, RedisKeys.LOGIN_CODE_TTL_MINUTES);
            }
            return "验证码已发送";
        } catch (Exception exception) {
            log.error("验证码写入 Redis 失败，手机号 {}", maskPhone(phone), exception);
            throw new BusinessException(503, "验证码服务暂不可用");
        }
    }

    @Override
    @Transactional
    public void register(UserRegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("两次密码不一致");
        }

        String phone = request.getPhone();
        String codeKey = RedisKeys.code(phone);
        String cachedCode = redisTemplate.opsForValue().get(codeKey);
        if (cachedCode == null) {
            throw new BusinessException("验证码已过期或尚未获取");
        }
        if (!cachedCode.equals(request.getCode())) {
            throw new BusinessException("验证码错误");
        }

        User usernameOwner = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, request.getUsername()));
        if (usernameOwner != null) {
            throw new BusinessException("用户名已存在");
        }

        User phoneOwner = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getPhone, phone));
        String passwordHash = passwordEncoder.encode(request.getPassword());
        if (phoneOwner == null) {
            User user = new User();
            user.setPhone(phone);
            user.setUsername(request.getUsername());
            user.setPasswordHash(passwordHash);
            user.setProfileCompleted(0);
            user.setNickname(defaultNickname());
            user.setGender(0);
            user.setStatus(1);
            userMapper.insert(user);
            log.info("账号注册成功，用户 {}", user.getId());
        } else if (isLegacyPhoneUser(phoneOwner)) {
            phoneOwner.setUsername(request.getUsername());
            phoneOwner.setPasswordHash(passwordHash);
            userMapper.updateById(phoneOwner);
            log.info("历史手机号用户已绑定账号，用户 {}", phoneOwner.getId());
        } else {
            throw new BusinessException("该手机号已注册");
        }

        redisTemplate.delete(codeKey);
    }

    @Override
    public LoginVO login(UserLoginRequest request) {
        String phone = request.getPhone();
        log.info("验证码登录请求，手机号 {}", maskPhone(phone));
        String cached = redisTemplate.opsForValue().get(RedisKeys.code(phone));
        if (cached == null) {
            log.warn("验证码登录失败：验证码已过期或尚未获取，手机号 {}", maskPhone(phone));
            throw new BusinessException("验证码已过期或尚未获取");
        }
        if (!cached.equals(request.getCode())) {
            log.warn("验证码登录失败：验证码错误，手机号 {}", maskPhone(phone));
            throw new BusinessException("验证码错误");
        }
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getPhone, phone));
        boolean newUser = user == null;
        if (newUser) {
            user = new User();
            user.setPhone(phone);
            user.setNickname(defaultNickname());
            user.setProfileCompleted(0);
            user.setGender(0);
            user.setStatus(1);
            userMapper.insert(user);
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(403, "当前用户不可登录");
        }
        LoginVO login = createLogin(user, newUser);
        redisTemplate.delete(RedisKeys.code(phone));
        log.info("验证码登录成功，手机号 {}，用户 {}", maskPhone(phone), user.getId());
        return login;
    }

    @Override
    public LoginVO passwordLogin(PasswordLoginRequest request) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getPhone, request.getPhone()));
        if (user == null || isBlank(user.getPasswordHash())) {
            if (user != null) throw new BusinessException("该账号未设置密码，请使用验证码登录");
            throw new BusinessException("手机号或密码错误");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) throw new BusinessException("手机号或密码错误");
        if (!Integer.valueOf(1).equals(user.getStatus())) throw new BusinessException(403, "当前用户不可登录");
        return createLogin(user, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserDTO completeInitialProfile(InitialProfileCompleteRequest request) {
        Long id = UserContext.getUserId();
        if (id == null) throw new BusinessException(401, "未登录或登录已过期");
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(401, "用户不存在");
        AddressCreateDTO address = new AddressCreateDTO();
        address.setReceiverName(request.getReceiverName()); address.setReceiverPhone(request.getReceiverPhone());
        address.setCampusId(request.getCampusId()); address.setBuildingId(request.getBuildingId()); address.setFloor(request.getFloor());
        address.setRoomNo(request.getRoomNo()); address.setDeliveryPoint(request.getDeliveryPoint()); address.setDetail(request.getDetail());
        address.setLabel(request.getLabel()); address.setRemark(request.getRemark()); address.setIsDefault(true);
        addressService.create(address);
        user.setNickname(request.getNickname().trim()); user.setAvatarUrl(request.getAvatarUrl()); user.setProfileCompleted(1);
        if (!isBlank(request.getPassword())) user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        userMapper.updateById(user);
        return syncCurrentSession(user);
    }

    @Override
    public UserDTO currentUser() {
        Long id = UserContext.getUserId();
        if (id == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(401, "用户不存在");
        return syncCurrentSession(user);
    }

    @Override
    public UserDTO updateProfile(UserProfileUpdateRequest request) {
        Long id = UserContext.getUserId();
        if (id == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }
        user.setNickname(request.getNickname().trim());
        user.setAvatarUrl(request.getAvatarUrl());
        userMapper.updateById(user);
        return syncCurrentSession(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserDTO uploadAvatar(org.springframework.web.multipart.MultipartFile file) {
        Long userId = UserContext.getUserId();
        if (userId == null) throw new BusinessException(401, "未登录或登录已过期");
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(401, "用户不存在");
        ValidatedImage image = validateImage(file);
        LocalDate today = LocalDate.now();
        String objectKey = "qinghe-life-service/" + today.getYear() + "/" + String.format("%02d", today.getMonthValue()) + "/" + UUID.randomUUID() + ".webp";
        String uploadedUrl;
        try {
            uploadedUrl = ossOperator.upload(objectKey, new ByteArrayInputStream(image.bytes), image.bytes.length, image.contentType);
        } catch (Exception exception) {
            log.error("头像上传失败，用户 {}，type={}", userId, exception.getClass().getSimpleName());
            throw new BusinessException(503, "头像上传服务暂不可用");
        }
        String oldAvatarUrl = user.getAvatarUrl();
        try {
            user.setAvatarUrl(uploadedUrl);
            if (userMapper.updateById(user) != 1) throw new BusinessException("头像保存失败，请稍后重试");
            UserDTO dto = syncCurrentSession(user);
            safelyDeleteOldAvatar(userId, oldAvatarUrl);
            return dto;
        } catch (Exception exception) {
            safelyDeleteNewAvatar(objectKey);
            if (exception instanceof BusinessException) throw (BusinessException) exception;
            log.error("头像地址保存失败，用户 {}，type={}", userId, exception.getClass().getSimpleName());
            throw new BusinessException(503, "头像保存失败，请稍后重试");
        }
    }

    @Override
    public void logout() {
        String token = UserContext.getToken();
        if (token != null) {
            redisTemplate.delete(RedisKeys.token(token));
        }
        UserContext.clear();
    }

    private boolean isLegacyPhoneUser(User user) {
        return isBlank(user.getUsername()) && isBlank(user.getPasswordHash());
    }

    private LoginVO createLogin(User user, boolean newUser) {
        UserDTO dto = userView(user);
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForHash().putAll(RedisKeys.token(token), dto.toMap());
        redisTemplate.expire(RedisKeys.token(token), RedisKeys.LOGIN_TOKEN_TTL_MINUTES, TimeUnit.MINUTES);
        return new LoginVO(token, newUser, dto.getProfileCompleted(), dto.getHasPassword(), dto);
    }

    private UserDTO syncCurrentSession(User user) {
        UserDTO dto = userView(user);
        String token = UserContext.getToken();
        if (token != null) redisTemplate.opsForHash().putAll(RedisKeys.token(token), dto.toMap());
        UserContext.setUser(dto);
        return dto;
    }

    private UserDTO userView(User user) {
        UserDTO dto = UserDTO.fromUser(user);
        StudentProfile profile = studentProfileMapper.selectOne(Wrappers.<StudentProfile>lambdaQuery()
                .eq(StudentProfile::getUserId, user.getId()).eq(StudentProfile::getCurrentFlag, 1));
        dto.setHasStudentProfile(profile != null);
        if (profile != null) {
            dto.setRealName(profile.getRealName());
            dto.setStudentNo(profile.getStudentNo());
        }
        return dto;
    }

    private ValidatedImage validateImage(org.springframework.web.multipart.MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() <= 0) throw new BusinessException("请选择有效图片");
        if (file.getSize() > 2L * 1024L * 1024L) throw new BusinessException("头像图片不能超过 2MB");
        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!(fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png") || fileName.endsWith(".webp"))) {
            throw new BusinessException("仅支持 jpg、jpeg、png 或 webp 图片");
        }
        try {
            byte[] bytes = file.getBytes();
            String detectedType = detectImageType(bytes);
            if (detectedType == null || !detectedType.equals(contentType)) throw new BusinessException("图片类型或文件内容不匹配");
            if ((fileName.endsWith(".png") && !"image/png".equals(detectedType))
                    || ((fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) && !"image/jpeg".equals(detectedType))
                    || (fileName.endsWith(".webp") && !"image/webp".equals(detectedType))) {
                throw new BusinessException("图片扩展名与文件内容不匹配");
            }
            return new ValidatedImage(bytes, detectedType);
        } catch (IOException exception) {
            throw new BusinessException("图片读取失败，请重新选择");
        }
    }

    private String detectImageType(byte[] bytes) {
        if (bytes == null || bytes.length < 12) return null;
        if ((bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff) return "image/jpeg";
        if ((bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47 && bytes[4] == 0x0d && bytes[5] == 0x0a && bytes[6] == 0x1a && bytes[7] == 0x0a) return "image/png";
        if (bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F' && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') return "image/webp";
        return null;
    }

    private void safelyDeleteNewAvatar(String objectKey) {
        try { ossOperator.deleteObject(objectKey); } catch (Exception exception) { log.warn("新头像补偿删除失败，type={}", exception.getClass().getSimpleName()); }
    }

    private void safelyDeleteOldAvatar(Long userId, String oldAvatarUrl) {
        String oldKey = ossOperator.ownAvatarKey(userId, oldAvatarUrl);
        if (oldKey == null) return;
        try { ossOperator.deleteObject(oldKey); } catch (Exception exception) { log.warn("旧头像清理失败，用户 {}，type={}", userId, exception.getClass().getSimpleName()); }
    }

    private static class ValidatedImage {
        private final byte[] bytes; private final String contentType;
        private ValidatedImage(byte[] bytes, String contentType) { this.bytes = bytes; this.contentType = contentType; }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String defaultNickname() {
        return "青禾用户_" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }

    private boolean isDevelopmentProfile() {
        return environment.acceptsProfiles(Profiles.of("dev", "local"));
    }

    private String createCode() {
        String code;
        do {
            code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        } while ("123456".equals(code));
        return code;
    }

    private String maskPhone(String phone) {
        return phone == null || phone.length() < 7 ? "***" : phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
