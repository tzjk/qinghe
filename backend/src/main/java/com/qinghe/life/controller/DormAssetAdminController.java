package com.qinghe.life.controller;

import com.qinghe.life.annotation.OperateLog;
import com.qinghe.life.common.PageResult;
import com.qinghe.life.common.Result;
import com.qinghe.life.dto.*;
import com.qinghe.life.service.DormAssetService;
import com.qinghe.life.vo.*;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/dorm")
public class DormAssetAdminController {
    private final DormAssetService dormAssetService; private final com.qinghe.life.service.AdminDormCheckinService checkinService; private final com.qinghe.life.service.AdminAcademicDormService academicService;
    public DormAssetAdminController(DormAssetService dormAssetService,com.qinghe.life.service.AdminDormCheckinService checkinService,com.qinghe.life.service.AdminAcademicDormService academicService) { this.dormAssetService = dormAssetService;this.checkinService=checkinService;this.academicService=academicService; }
    @GetMapping("/campuses") public Result<List<CampusVO>> listCampuses(){ return Result.success(dormAssetService.listAvailableCampuses()); }
    @GetMapping("/buildings") public Result<PageResult<AdminDormBuildingVO>> listBuildings(@Valid AdminDormBuildingQuery query){ return Result.success(dormAssetService.listBuildings(query)); }
    @GetMapping("/buildings/available") public Result<List<AdminDormBuildingVO>> listAvailableBuildings(@RequestParam(required=false) Long campusId){ return Result.success(dormAssetService.listAvailableBuildings(campusId)); }
    @GetMapping("/buildings/{buildingId}") public Result<AdminDormBuildingVO> buildingDetail(@PathVariable Long buildingId){ return Result.success(dormAssetService.buildingDetail(buildingId)); }
    @PostMapping("/buildings") @OperateLog(module="校区楼栋",action="新增楼栋") public Result<AdminDormBuildingVO> createBuilding(@Valid @RequestBody AdminDormBuildingCreateRequest request){ return Result.success(dormAssetService.createBuilding(request)); }
    @PutMapping("/buildings/{buildingId}") @OperateLog(module="校区楼栋",action="编辑楼栋") public Result<AdminDormBuildingVO> updateBuilding(@PathVariable Long buildingId,@Valid @RequestBody AdminDormBuildingUpdateRequest request){ return Result.success(dormAssetService.updateBuilding(buildingId,request)); }
    @PutMapping("/buildings/{buildingId}/status") @OperateLog(module="校区楼栋",action="修改楼栋状态") public Result<Void> updateBuildingStatus(@PathVariable Long buildingId,@Valid @RequestBody DormBuildingStatusRequest request){ dormAssetService.updateBuildingStatus(buildingId,request); return Result.success(); }
    @DeleteMapping("/buildings/{buildingId}") @OperateLog(module="校区楼栋",action="删除楼栋") public Result<Void> deleteBuilding(@PathVariable Long buildingId){ dormAssetService.deleteBuilding(buildingId); return Result.success(); }
    @PutMapping("/buildings/{buildingId}/code") @OperateLog(module="校区楼栋",action="维护楼栋编码") public Result<Void> updateBuildingCode(@PathVariable Long buildingId,@Valid @RequestBody BuildingCodeUpdateRequest request){ dormAssetService.updateBuildingCode(buildingId,request); return Result.success(); }
    @PostMapping("/rooms") @OperateLog(module="宿舍资产",action="新增寝室") public Result<DormRoomVO> createRoom(@Valid @RequestBody DormRoomSaveRequest request){ return Result.success(dormAssetService.createRoom(request)); }
    @GetMapping("/rooms") public Result<PageResult<DormRoomVO>> listRooms(@RequestParam(required=false) Long buildingId,@Valid PageQuery pageQuery){ return Result.success(dormAssetService.listRooms(buildingId,pageQuery)); }
    @PutMapping("/rooms/{roomId}") @OperateLog(module="宿舍资产",action="编辑寝室") public Result<DormRoomVO> updateRoom(@PathVariable Long roomId,@Valid @RequestBody DormRoomUpdateRequest request){ return Result.success(dormAssetService.updateRoom(roomId,request)); }
    @PutMapping("/rooms/{roomId}/status") @OperateLog(module="宿舍资产",action="修改寝室状态") public Result<Void> updateRoomStatus(@PathVariable Long roomId,@Valid @RequestBody DormDirectoryStatusRequest request){ dormAssetService.updateRoomStatus(roomId,request); return Result.success(); }
    @DeleteMapping("/rooms/{roomId}") @OperateLog(module="宿舍资产",action="删除寝室") public Result<Void> deleteRoom(@PathVariable Long roomId){ dormAssetService.deleteRoom(roomId); return Result.success(); }
    @GetMapping("/rooms/{roomId}/beds") public Result<List<DormBedVO>> listBeds(@PathVariable Long roomId){ return Result.success(dormAssetService.listBeds(roomId)); }
    @PostMapping("/rooms/{roomId}/beds") @OperateLog(module="宿舍资产",action="新增床位") public Result<DormBedVO> createBed(@PathVariable Long roomId,@Valid @RequestBody DormBedSaveRequest request){ return Result.success(dormAssetService.createBed(roomId,request)); }
    @PutMapping("/beds/{bedId}") @OperateLog(module="宿舍资产",action="编辑床位") public Result<DormBedVO> updateBed(@PathVariable Long bedId,@Valid @RequestBody DormBedUpdateRequest request){ return Result.success(dormAssetService.updateBed(bedId,request)); }
    @PutMapping("/beds/{bedId}/status") @OperateLog(module="宿舍资产",action="修改床位状态") public Result<Void> updateBedStatus(@PathVariable Long bedId,@Valid @RequestBody DormDirectoryStatusRequest request){ dormAssetService.updateBedStatus(bedId,request); return Result.success(); }
    @DeleteMapping("/beds/{bedId}") @OperateLog(module="宿舍资产",action="删除床位") public Result<Void> deleteBed(@PathVariable Long bedId){ dormAssetService.deleteBed(bedId); return Result.success(); }
    @PostMapping("/beds/{bedId}/asset-set") @OperateLog(module="宿舍资产",action="生成资产套装") public Result<AssetSetDetailVO> generateAssetSet(@PathVariable Long bedId){ return Result.success(dormAssetService.generateAssetSet(bedId)); }
    @PostMapping("/beds/{bedId}/asset-set/supplement") @OperateLog(module="宿舍资产",action="补齐固定资产") public Result<AssetSetDetailVO> supplementFixedAssets(@PathVariable Long bedId){ return Result.success(dormAssetService.supplementFixedAssets(bedId)); }
    @GetMapping("/beds/{bedId}/asset-set") public Result<AssetSetDetailVO> getAssetSet(@PathVariable Long bedId){ return Result.success(dormAssetService.getAssetSet(bedId)); }
    @PutMapping("/assets/{assetId}") @OperateLog(module="宿舍资产",action="编辑资产") public Result<AssetVO> updateAsset(@PathVariable Long assetId,@Valid @RequestBody AssetUpdateRequest request){ return Result.success(dormAssetService.updateAsset(assetId,request)); }
    @GetMapping("/asset-sets/{assetSetId}/qrcode") public void downloadQr(@PathVariable Long assetSetId,HttpServletResponse response) throws java.io.IOException { byte[] png=dormAssetService.qrPng(assetSetId); response.setContentType("image/png"); response.setHeader("Content-Disposition","attachment; filename=asset-set-"+assetSetId+".png"); response.getOutputStream().write(png); }
    @GetMapping("/asset-sets/qrcode.zip") public void downloadQrZip(@RequestParam List<Long> ids,HttpServletResponse response) throws java.io.IOException { byte[] zip=dormAssetService.qrZip(ids); response.setContentType("application/zip"); response.setHeader("Content-Disposition","attachment; filename=dorm-asset-qrcodes.zip"); response.getOutputStream().write(zip); }
    @GetMapping("/checkins") public Result<PageResult<AdminDormCheckinVO>> checkins(@Valid AdminDormCheckinQuery query){return Result.success(checkinService.page(query));}
    @GetMapping("/checkins/{id}") public Result<AdminDormCheckinVO> checkin(@PathVariable Long id){return Result.success(checkinService.detail(id));}
    @PostMapping("/checkins/{id}/checkout") @OperateLog(module="入住管理",action="办理退宿") public Result<Void> checkout(@PathVariable Long id,@Valid @RequestBody DormCheckoutRequest request){checkinService.checkout(id,request);return Result.success();}
    @PostMapping("/checkins/{id}/transfer") @OperateLog(module="入住管理",action="办理换寝") public Result<Void> transfer(@PathVariable Long id,@Valid @RequestBody DormTransferRequest request){checkinService.transfer(id,request);return Result.success();}
    @GetMapping("/available-beds") public Result<List<AvailableDormBedVO>> availableBeds(@RequestParam(required=false) Long campusId,@RequestParam(required=false) Long buildingId,@RequestParam(required=false) Long roomId){return Result.success(checkinService.availableBeds(campusId,buildingId,roomId));}
    @GetMapping("/students") public Result<PageResult<AdminStudentVO>> students(@Valid AdminStudentQuery query){return Result.success(academicService.page(query));}
    @GetMapping("/students/{userId}") public Result<AdminStudentDetailVO> student(@PathVariable Long userId){return Result.success(academicService.detail(userId));}
    @GetMapping("/students/{userId}/history") public Result<List<AdminStudentProfileVersionVO>> studentHistory(@PathVariable Long userId){return Result.success(academicService.history(userId));}
    @PostMapping("/students/{userId}/transfer-major") @OperateLog(module="学籍管理",action="办理转专业") public Result<Void> transferMajor(@PathVariable Long userId,@Valid @RequestBody AcademicChangeRequest request){academicService.transferMajor(userId,request);return Result.success();}
    @PostMapping("/students/{userId}/suspend") @OperateLog(module="学籍管理",action="办理休学") public Result<Void> suspend(@PathVariable Long userId,@Valid @RequestBody AcademicChangeRequest request){academicService.suspend(userId,request);return Result.success();}
    @PostMapping("/students/{userId}/drop") @OperateLog(module="学籍管理",action="办理退学") public Result<Void> drop(@PathVariable Long userId,@Valid @RequestBody AcademicChangeRequest request){academicService.drop(userId,request);return Result.success();}
    @PostMapping("/students/{userId}/graduate") @OperateLog(module="学籍管理",action="办理毕业") public Result<Void> graduate(@PathVariable Long userId,@Valid @RequestBody AcademicChangeRequest request){academicService.graduate(userId,request);return Result.success();}
    @PostMapping("/students/{userId}/reinstate") @OperateLog(module="学籍管理",action="办理复学") public Result<Void> reinstate(@PathVariable Long userId,@Valid @RequestBody AcademicChangeRequest request){academicService.reinstate(userId,request);return Result.success();}
    @PostMapping("/batch/graduation/preview") public Result<BatchPreviewVO> previewGraduation(@Valid @RequestBody BatchPreviewRequest request){return Result.success(academicService.previewGraduation(request));}
    @PostMapping("/batch/graduation") @OperateLog(module="学籍管理",action="批量毕业") public Result<Void> batchGraduate(@Valid @RequestBody BatchOperationRequest request){academicService.batchGraduate(request);return Result.success();}
    @PostMapping("/batch/checkout/preview") public Result<BatchPreviewVO> previewCheckout(@Valid @RequestBody BatchPreviewRequest request){return Result.success(academicService.previewCheckout(request));}
    @PostMapping("/batch/checkout") @OperateLog(module="入住管理",action="批量退宿") public Result<Void> batchCheckout(@Valid @RequestBody BatchOperationRequest request){academicService.batchCheckout(request);return Result.success();}
    @PostMapping("/batch/assets/release/preview") public Result<BatchPreviewVO> previewRelease(@Valid @RequestBody BatchPreviewRequest request){return Result.success(academicService.previewAssetRelease(request));}
    @PostMapping("/batch/assets/release") @OperateLog(module="宿舍资产",action="批量释放资产") public Result<Void> batchRelease(@Valid @RequestBody BatchOperationRequest request){academicService.batchReleaseAssets(request);return Result.success();}
    @PostMapping("/batch/qrcode/disable/preview") public Result<BatchPreviewVO> previewDisableQr(@Valid @RequestBody BatchPreviewRequest request){return Result.success(academicService.previewQrDisable(request));}
    @PostMapping("/batch/qrcode/disable") @OperateLog(module="宿舍资产",action="批量停用二维码") public Result<Void> batchDisableQr(@Valid @RequestBody BatchOperationRequest request){academicService.batchDisableQr(request);return Result.success();}
    @PostMapping("/batch/qrcode/rotate/preview") public Result<BatchPreviewVO> previewRotateQr(@Valid @RequestBody BatchPreviewRequest request){return Result.success(academicService.previewQrRotate(request));}
    @PostMapping("/batch/qrcode/rotate") @OperateLog(module="宿舍资产",action="批量轮换二维码") public Result<Void> batchRotateQr(@Valid @RequestBody BatchOperationRequest request){academicService.batchRotateQr(request);return Result.success();}
}
