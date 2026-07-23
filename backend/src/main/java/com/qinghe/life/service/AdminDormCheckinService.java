package com.qinghe.life.service;
import com.qinghe.life.common.PageResult; import com.qinghe.life.dto.*; import com.qinghe.life.vo.*; import java.util.List;
public interface AdminDormCheckinService { PageResult<AdminDormCheckinVO> page(AdminDormCheckinQuery query); AdminDormCheckinVO detail(Long id); void checkout(Long id,DormCheckoutRequest request); void transfer(Long id,DormTransferRequest request); List<AvailableDormBedVO> availableBeds(Long campusId,Long buildingId,Long roomId); }
