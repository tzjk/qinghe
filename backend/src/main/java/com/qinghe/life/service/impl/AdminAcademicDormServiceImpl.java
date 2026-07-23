package com.qinghe.life.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qinghe.life.common.PageResult;
import com.qinghe.life.dto.*;
import com.qinghe.life.entity.AssetSet;
import com.qinghe.life.entity.DormCheckin;
import com.qinghe.life.entity.StudentProfile;
import com.qinghe.life.exception.BusinessException;
import com.qinghe.life.mapper.AssetSetMapper;
import com.qinghe.life.mapper.DormCheckinMapper;
import com.qinghe.life.mapper.StudentProfileMapper;
import com.qinghe.life.service.AdminAcademicDormService;
import com.qinghe.life.service.DormCheckinLifecycle;
import com.qinghe.life.utils.AdminContext;
import com.qinghe.life.vo.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAcademicDormServiceImpl implements AdminAcademicDormService {
    private static final int LIMIT = 100;
    private final StudentProfileMapper profiles;
    private final DormCheckinMapper checkins;
    private final AssetSetMapper sets;
    private final DormCheckinLifecycle lifecycle;
    private final SecureRandom random = new SecureRandom();

    public AdminAcademicDormServiceImpl(StudentProfileMapper profiles, DormCheckinMapper checkins, AssetSetMapper sets, DormCheckinLifecycle lifecycle) {
        this.profiles = profiles; this.checkins = checkins; this.sets = sets; this.lifecycle = lifecycle;
    }

    @Override public PageResult<AdminStudentVO> page(AdminStudentQuery query) {
        admin();
        IPage<StudentProfile> data = profiles.selectPage(new Page<StudentProfile>(query.getPage(), query.getSize()), Wrappers.<StudentProfile>lambdaQuery()
            .eq(StudentProfile::getCurrentFlag, 1).like(text(query.getStudentName()) != null, StudentProfile::getRealName, text(query.getStudentName()))
            .like(text(query.getStudentNo()) != null, StudentProfile::getStudentNo, text(query.getStudentNo()))
            .like(text(query.getCollegeName()) != null, StudentProfile::getCollegeName, text(query.getCollegeName()))
            .like(text(query.getMajorName()) != null, StudentProfile::getMajorName, text(query.getMajorName()))
            .like(text(query.getClassName()) != null, StudentProfile::getClassName, text(query.getClassName()))
            .eq(text(query.getStudentStatus()) != null, StudentProfile::getStudentStatus, text(query.getStudentStatus()))
            .exists("IN_DORM".equals(query.getAccommodationStatus()), "SELECT 1 FROM qh_dorm_checkin dc WHERE dc.user_id = qh_student_profile.user_id AND dc.active_flag = 1")
            .notExists("NOT_IN_DORM".equals(query.getAccommodationStatus()), "SELECT 1 FROM qh_dorm_checkin dc WHERE dc.user_id = qh_student_profile.user_id AND dc.active_flag = 1")
            .orderByDesc(StudentProfile::getId));
        Map<Long, DormCheckin> active = activeByUsers(data.getRecords().stream().map(StudentProfile::getUserId).collect(Collectors.toList()));
        List<AdminStudentVO> values = data.getRecords().stream().map(p -> studentView(p, active.get(p.getUserId()))).collect(Collectors.toList());
        return new PageResult<AdminStudentVO>(values, data.getTotal(), data.getCurrent(), data.getSize());
    }

    @Override public AdminStudentDetailVO detail(Long userId) {
        admin(); StudentProfile profile = current(userId); DormCheckin checkin = activeByUsers(Collections.singletonList(userId)).get(userId);
        AdminStudentDetailVO value = new AdminStudentDetailVO(); value.setProfile(studentView(profile, checkin)); value.setMaskedContactPhone(maskPhone(profile.getContactPhone())); value.setDormSummary(summary(checkin)); value.setHistory(history(userId)); return value;
    }
    @Override public List<AdminStudentProfileVersionVO> history(Long userId) { admin(); return profiles.selectList(Wrappers.<StudentProfile>lambdaQuery().eq(StudentProfile::getUserId, userId).orderByDesc(StudentProfile::getId)).stream().map(this::version).collect(Collectors.toList()); }

    @Override @Transactional(rollbackFor = Exception.class) public void transferMajor(Long userId, AcademicChangeRequest request) {
        Long admin = admin(); StudentProfile old = lockCurrent(userId); requireStatus(old, "ENROLLED");
        replace(old, "ENROLLED", request, admin, "转专业：", required(request.getCollegeName(), "新学院"), required(request.getMajorName(), "新专业"), required(request.getClassName(), "新班级"));
    }
    @Override @Transactional(rollbackFor = Exception.class) public void suspend(Long userId, AcademicChangeRequest request) {
        Long admin = admin(); StudentProfile old = lockCurrent(userId); requireStatus(old, "ENROLLED");
        if (Boolean.TRUE.equals(request.getCheckoutDorm())) { DormCheckin checkin = lifecycle.lockActiveByUser(userId); if (checkin != null) lifecycle.checkout(checkin, "休学退宿：" + required(request.getReason(), "异动原因"), admin, "SUSPENDED_CHECKOUT"); }
        replace(old, "SUSPENDED", request, admin, "休学：", old.getCollegeName(), old.getMajorName(), old.getClassName());
    }
    @Override @Transactional(rollbackFor = Exception.class) public void drop(Long userId, AcademicChangeRequest request) { terminal(userId, request, "DROPPED", "退学处理：", "DROPPED_CHECKOUT"); }
    @Override @Transactional(rollbackFor = Exception.class) public void graduate(Long userId, AcademicChangeRequest request) { terminal(userId, request, "GRADUATED", "毕业处理：", "GRADUATED_CHECKOUT"); }
    @Override @Transactional(rollbackFor = Exception.class) public void reinstate(Long userId, AcademicChangeRequest request) {
        Long admin = admin(); StudentProfile old = lockCurrent(userId); requireStatus(old, "SUSPENDED");
        replace(old, "ENROLLED", request, admin, "复学：", old.getCollegeName(), old.getMajorName(), old.getClassName());
    }

    @Override public BatchPreviewVO previewGraduation(BatchPreviewRequest request) { return studentPreview("GRADUATE", request.getIds(), (p, c) -> "ENROLLED".equals(p.getStudentStatus())); }
    @Override @Transactional(rollbackFor = Exception.class) public void batchGraduate(BatchOperationRequest request) {
        Long admin = admin(); BatchPreviewVO preview = previewGraduation(previewRequest(request)); verify(preview, request); if (preview.getIneligibleCount() > 0) throw new BusinessException("存在已毕业或不符合条件的学生，请重新预览");
        for (Long userId : ordered(request.getIds())) terminal(userId, change(request), "GRADUATED", "毕业处理：", "GRADUATED_CHECKOUT");
    }
    @Override public BatchPreviewVO previewCheckout(BatchPreviewRequest request) { return studentPreview("CHECKOUT", request.getIds(), (p, c) -> c != null); }
    @Override @Transactional(rollbackFor = Exception.class) public void batchCheckout(BatchOperationRequest request) {
        Long admin = admin(); BatchPreviewVO preview = previewCheckout(previewRequest(request)); verify(preview, request); if (preview.getIneligibleCount() > 0) throw new BusinessException("存在无当前入住的学生，请重新预览");
        for (Long userId : ordered(request.getIds())) { DormCheckin checkin = lifecycle.lockActiveByUser(userId); if (checkin == null) throw new BusinessException("当前入住已变化，请重新预览"); lifecycle.checkout(checkin, "批量退宿：" + request.getReason().trim(), admin, "BATCH_CHECKED_OUT"); }
    }
    @Override public BatchPreviewVO previewAssetRelease(BatchPreviewRequest request) { return assetPreview("RELEASE_ASSET", request.getIds(), (s, active) -> "OCCUPIED".equals(s.getStatus()) && !active.contains(s.getId())); }
    @Override @Transactional(rollbackFor = Exception.class) public void batchReleaseAssets(BatchOperationRequest request) { BatchPreviewVO preview = previewAssetRelease(previewRequest(request)); verify(preview, request); if (preview.getIneligibleCount() > 0) throw new BusinessException("存在不可释放或有当前入住的资产套装，请重新预览"); for (Long id : ordered(request.getIds())) lifecycle.releaseWithoutActiveCheckin(id); }
    @Override public BatchPreviewVO previewQrDisable(BatchPreviewRequest request) { return assetPreview("DISABLE_QR", request.getIds(), (s, active) -> Integer.valueOf(1).equals(s.getQrStatus())); }
    @Override @Transactional(rollbackFor = Exception.class) public void batchDisableQr(BatchOperationRequest request) { BatchPreviewVO preview = previewQrDisable(previewRequest(request)); verify(preview, request); if (preview.getIneligibleCount() > 0) throw new BusinessException("存在已停用二维码，请重新预览"); for (AssetSet set : lockedSets(request.getIds())) { if (!Integer.valueOf(1).equals(set.getQrStatus())) throw new BusinessException("二维码状态已变化，请重新预览"); set.setQrStatus(0); if (sets.updateById(set) != 1) throw new BusinessException("二维码停用失败，请稍后重试"); } }
    @Override public BatchPreviewVO previewQrRotate(BatchPreviewRequest request) { return assetPreview("ROTATE_QR", request.getIds(), (s, active) -> "AVAILABLE".equals(s.getStatus()) && !active.contains(s.getId())); }
    @Override @Transactional(rollbackFor = Exception.class) public void batchRotateQr(BatchOperationRequest request) { BatchPreviewVO preview = previewQrRotate(previewRequest(request)); verify(preview, request); if (preview.getIneligibleCount() > 0) throw new BusinessException("仅空闲且无当前入住的套装允许轮换，请重新预览"); for (AssetSet set : lockedSets(request.getIds())) { if (!"AVAILABLE".equals(set.getStatus()) || hasActiveSet(set.getId())) throw new BusinessException("资产套装状态已变化，请重新预览"); set.setQrToken(token()); set.setQrRotatedTime(LocalDateTime.now()); if (sets.updateById(set) != 1) throw new BusinessException("二维码轮换失败，请稍后重试"); } }

    private void terminal(Long userId, AcademicChangeRequest request, String status, String prefix, String checkoutStatus) {
        Long admin = admin(); StudentProfile old = lockCurrent(userId); requireStatus(old, "ENROLLED"); DormCheckin checkin = lifecycle.lockActiveByUser(userId); if (checkin != null) lifecycle.checkout(checkin, prefix + required(request.getReason(), "异动原因"), admin, checkoutStatus);
        replace(old, status, request, admin, prefix, old.getCollegeName(), old.getMajorName(), old.getClassName());
    }
    private void replace(StudentProfile old, String status, AcademicChangeRequest request, Long admin, String prefix, String college, String major, String clazz) {
        LocalDateTime effective = request.getEffectiveTime() == null ? LocalDateTime.now() : request.getEffectiveTime(); String reason = prefix + required(request.getReason(), "异动原因");
        if (profiles.update(null, Wrappers.<StudentProfile>lambdaUpdate().eq(StudentProfile::getId, old.getId()).eq(StudentProfile::getCurrentFlag, 1).set(StudentProfile::getCurrentFlag, null).set(StudentProfile::getEndTime, effective)) != 1) throw new BusinessException("学生当前资料已变化，请重新查询");
        StudentProfile next = new StudentProfile(); next.setUserId(old.getUserId()); next.setRealName(old.getRealName()); next.setStudentNo(old.getStudentNo()); next.setCampusId(old.getCampusId()); next.setCollegeName(college); next.setMajorName(major); next.setClassName(clazz); next.setContactPhone(old.getContactPhone()); next.setStudentStatus(status); next.setEffectiveTime(effective); next.setCurrentFlag(1); next.setChangeReason(reason); next.setOperatorAdminId(admin);
        profiles.insert(next);
    }
    private BatchPreviewVO studentPreview(String operation, List<Long> ids, java.util.function.BiPredicate<StudentProfile, DormCheckin> eligible) {
        admin(); List<Long> selected = ordered(ids); List<StudentProfile> values = profiles.selectList(Wrappers.<StudentProfile>lambdaQuery().in(StudentProfile::getUserId, selected).eq(StudentProfile::getCurrentFlag, 1)); Map<Long, StudentProfile> map = values.stream().collect(Collectors.toMap(StudentProfile::getUserId, x -> x)); Map<Long, DormCheckin> active = activeByUsers(selected); List<AdminStudentVO> valid = new ArrayList<AdminStudentVO>(); List<String> skipped = new ArrayList<String>(); int checkins = 0;
        for (Long id : selected) { StudentProfile p = map.get(id); DormCheckin c = active.get(id); if (p == null || !eligible.test(p, c)) { skipped.add("所选学生当前状态不符合处理条件"); continue; } if (c != null) checkins++; valid.add(studentView(p, c)); }
        return preview(operation, selected.size(), valid, checkins, countAssets(active.values()), skipped, digest(operation, values, active.values()));
    }
    private BatchPreviewVO assetPreview(String operation, List<Long> ids, java.util.function.BiPredicate<AssetSet, Set<Long>> eligible) {
        admin(); List<Long> selected = ordered(ids); List<AssetSet> values = sets.selectBatchIds(selected); Map<Long, AssetSet> map = values.stream().collect(Collectors.toMap(AssetSet::getId, x -> x)); Set<Long> active = activeSetIds(selected); List<String> skipped = new ArrayList<String>(); int valid = 0; int occupied = 0;
        for (Long id : selected) { AssetSet set = map.get(id); if (set == null || !eligible.test(set, active)) skipped.add("所选资产套装不符合处理条件"); else { valid++; if (active.contains(id)) occupied++; } }
        return preview(operation, selected.size(), Collections.<AdminStudentVO>emptyList(), occupied, valid, skipped, digest(operation, Collections.<StudentProfile>emptyList(), values));
    }
    private BatchPreviewVO preview(String operation, int selected, List<AdminStudentVO> students, int checkins, int assets, List<String> skipped, String token) { BatchPreviewVO value = new BatchPreviewVO(); value.setOperation(operation); value.setSelectedCount(selected); value.setEligibleCount(students.isEmpty() && assets > 0 ? assets : students.size()); value.setCurrentCheckinCount(checkins); value.setAssetSetCount(assets); value.setIneligibleCount(skipped.size()); value.setPreviewToken(token); value.setStudents(students); value.setSkippedReasons(skipped); return value; }
    private void verify(BatchPreviewVO preview, BatchOperationRequest request) { if (!preview.getPreviewToken().equals(request.getPreviewToken())) throw new BusinessException(409, "预览范围或当前状态已变化，请重新预览"); }
    private StudentProfile current(Long userId) { StudentProfile value = profiles.selectOne(Wrappers.<StudentProfile>lambdaQuery().eq(StudentProfile::getUserId, userId).eq(StudentProfile::getCurrentFlag, 1)); if (value == null) throw new BusinessException(404, "当前学生资料不存在"); return value; }
    private StudentProfile lockCurrent(Long userId) { StudentProfile value = profiles.selectOne(Wrappers.<StudentProfile>lambdaQuery().eq(StudentProfile::getUserId, userId).eq(StudentProfile::getCurrentFlag, 1).last("FOR UPDATE")); if (value == null) throw new BusinessException("当前学生资料已变化，请重新查询"); return value; }
    private Map<Long, DormCheckin> activeByUsers(List<Long> userIds) { if (userIds == null || userIds.isEmpty()) return Collections.emptyMap(); return checkins.selectList(Wrappers.<DormCheckin>lambdaQuery().in(DormCheckin::getUserId, userIds).eq(DormCheckin::getActiveFlag, 1)).stream().collect(Collectors.toMap(DormCheckin::getUserId, x -> x, (a,b) -> a)); }
    private List<AssetSet> lockedSets(List<Long> ids) { List<AssetSet> result = new ArrayList<AssetSet>(); for (Long id : ordered(ids)) result.add(lifecycle.lockAssetSet(id)); return result; }
    private int countAssets(Collection<DormCheckin> values) { return (int) values.stream().map(DormCheckin::getAssetSetId).filter(Objects::nonNull).distinct().count(); }
    private boolean hasActiveSet(Long id) { return checkins.selectCount(Wrappers.<DormCheckin>lambdaQuery().eq(DormCheckin::getAssetSetId, id).eq(DormCheckin::getActiveFlag, 1)) > 0; }
    private Set<Long> activeSetIds(List<Long> ids) { if (ids.isEmpty()) return Collections.emptySet(); return checkins.selectList(Wrappers.<DormCheckin>lambdaQuery().in(DormCheckin::getAssetSetId, ids).eq(DormCheckin::getActiveFlag, 1)).stream().map(DormCheckin::getAssetSetId).collect(Collectors.toSet()); }
    private void requireStatus(StudentProfile profile, String value) { if (!value.equals(profile.getStudentStatus())) throw new BusinessException("当前学籍状态不允许该操作"); }
    private AdminStudentVO studentView(StudentProfile p, DormCheckin checkin) { AdminStudentVO v = new AdminStudentVO(); v.setUserId(p.getUserId()); v.setProfileId(p.getId()); v.setRealName(p.getRealName()); v.setMaskedStudentNo(mask(p.getStudentNo())); v.setCollegeName(p.getCollegeName()); v.setMajorName(p.getMajorName()); v.setClassName(p.getClassName()); v.setStudentStatus(p.getStudentStatus()); v.setEffectiveTime(p.getEffectiveTime()); v.setCurrentCheckin(checkin != null); v.setDormSummary(summary(checkin)); return v; }
    private AdminStudentProfileVersionVO version(StudentProfile p) { AdminStudentProfileVersionVO v = new AdminStudentProfileVersionVO(); v.setId(p.getId()); v.setCollegeName(p.getCollegeName()); v.setMajorName(p.getMajorName()); v.setClassName(p.getClassName()); v.setStudentStatus(p.getStudentStatus()); v.setEffectiveTime(p.getEffectiveTime()); v.setEndTime(p.getEndTime()); v.setChangeReason(p.getChangeReason()); v.setOperatorAdminId(p.getOperatorAdminId()); v.setCurrent(Integer.valueOf(1).equals(p.getCurrentFlag())); return v; }
    private String summary(DormCheckin c) { return c == null ? "未入住" : c.getCampusNameSnapshot() + " " + c.getBuildingNameSnapshot() + " " + c.getRoomNoSnapshot() + "室 " + c.getBedNoSnapshot() + "床"; }
    private String digest(String operation, Collection<?> first, Collection<?> second) { try { MessageDigest md = MessageDigest.getInstance("SHA-256"); md.update(operation.getBytes(StandardCharsets.UTF_8)); for (Object x : first) md.update(String.valueOf(x).getBytes(StandardCharsets.UTF_8)); for (Object x : second) md.update(String.valueOf(x).getBytes(StandardCharsets.UTF_8)); byte[] bytes = md.digest(); StringBuilder b = new StringBuilder(); for (byte x : bytes) b.append(String.format("%02x", x)); return b.toString(); } catch (Exception e) { throw new IllegalStateException(e); } }
    private String token() { byte[] bytes = new byte[32]; random.nextBytes(bytes); StringBuilder b = new StringBuilder(64); for (byte x : bytes) b.append(String.format("%02x", x)); return b.toString(); }
    private List<Long> ordered(List<Long> ids) { if (ids == null || ids.isEmpty() || ids.size() > LIMIT || new HashSet<Long>(ids).size() != ids.size() || ids.contains(null)) throw new BusinessException("单次必须选择1至100个不重复对象"); List<Long> values = new ArrayList<Long>(ids); Collections.sort(values); return values; }
    private Long admin() { Long value = AdminContext.getAdminId(); if (value == null) throw new BusinessException(401, "管理员登录已过期"); return value; }
    private AcademicChangeRequest change(BatchOperationRequest request) { AcademicChangeRequest value = new AcademicChangeRequest(); value.setReason(request.getReason()); return value; }
    private BatchPreviewRequest previewRequest(BatchOperationRequest request) { BatchPreviewRequest value = new BatchPreviewRequest(); value.setIds(request.getIds()); return value; }
    private String text(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }
    private String required(String value, String label) { if (value == null || value.trim().isEmpty()) throw new BusinessException(label + "不能为空"); return value.trim(); }
    private String mask(String value) { return value == null || value.length() < 5 ? "***" : value.substring(0,2) + "***" + value.substring(value.length()-2); }
    private String maskPhone(String value) { return value == null ? null : value.replaceAll("^(\\d{3})\\d+(\\d{4})$", "$1****$2"); }
}
