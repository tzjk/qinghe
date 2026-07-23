package com.qinghe.life.service;

import com.qinghe.life.common.PageResult;
import com.qinghe.life.dto.AcademicChangeRequest;
import com.qinghe.life.dto.AdminStudentQuery;
import com.qinghe.life.dto.BatchOperationRequest;
import com.qinghe.life.dto.BatchPreviewRequest;
import com.qinghe.life.vo.AdminStudentDetailVO;
import com.qinghe.life.vo.AdminStudentProfileVersionVO;
import com.qinghe.life.vo.AdminStudentVO;
import com.qinghe.life.vo.BatchPreviewVO;
import java.util.List;

public interface AdminAcademicDormService {
    PageResult<AdminStudentVO> page(AdminStudentQuery query);
    AdminStudentDetailVO detail(Long userId);
    List<AdminStudentProfileVersionVO> history(Long userId);
    void transferMajor(Long userId, AcademicChangeRequest request);
    void suspend(Long userId, AcademicChangeRequest request);
    void drop(Long userId, AcademicChangeRequest request);
    void graduate(Long userId, AcademicChangeRequest request);
    void reinstate(Long userId, AcademicChangeRequest request);
    BatchPreviewVO previewGraduation(BatchPreviewRequest request);
    void batchGraduate(BatchOperationRequest request);
    BatchPreviewVO previewCheckout(BatchPreviewRequest request);
    void batchCheckout(BatchOperationRequest request);
    BatchPreviewVO previewAssetRelease(BatchPreviewRequest request);
    void batchReleaseAssets(BatchOperationRequest request);
    BatchPreviewVO previewQrDisable(BatchPreviewRequest request);
    void batchDisableQr(BatchOperationRequest request);
    BatchPreviewVO previewQrRotate(BatchPreviewRequest request);
    void batchRotateQr(BatchOperationRequest request);
}
