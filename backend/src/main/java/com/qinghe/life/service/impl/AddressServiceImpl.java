package com.qinghe.life.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qinghe.life.dto.AddressCreateDTO;
import com.qinghe.life.dto.AddressUpdateDTO;
import com.qinghe.life.entity.Building;
import com.qinghe.life.entity.Campus;
import com.qinghe.life.entity.UserAddress;
import com.qinghe.life.exception.BusinessException;
import com.qinghe.life.mapper.BuildingMapper;
import com.qinghe.life.mapper.CampusMapper;
import com.qinghe.life.mapper.UserAddressMapper;
import com.qinghe.life.service.AddressService;
import com.qinghe.life.utils.UserContext;
import com.qinghe.life.vo.AddressVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AddressServiceImpl implements AddressService {
    private static final String CAMPUS_ADDRESS = "CAMPUS";

    private final UserAddressMapper userAddressMapper;
    private final CampusMapper campusMapper;
    private final BuildingMapper buildingMapper;

    public AddressServiceImpl(UserAddressMapper userAddressMapper, CampusMapper campusMapper,
                              BuildingMapper buildingMapper) {
        this.userAddressMapper = userAddressMapper;
        this.campusMapper = campusMapper;
        this.buildingMapper = buildingMapper;
    }

    @Override
    public List<AddressVO> listCurrentUser() {
        Long userId = currentUserId();
        return userAddressMapper.selectList(Wrappers.<UserAddress>lambdaQuery()
                        .eq(UserAddress::getUserId, userId)
                        .orderByDesc(UserAddress::getIsDefault)
                        .orderByDesc(UserAddress::getId))
                .stream().map(this::toView).collect(Collectors.toList());
    }

    @Override
    public AddressVO getCurrentUserAddress(Long id) {
        return toView(requireOwnedAddress(id, currentUserId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AddressVO create(AddressCreateDTO request) {
        Long userId = currentUserId();
        boolean firstAddress = userAddressMapper.selectCount(Wrappers.<UserAddress>lambdaQuery()
                .eq(UserAddress::getUserId, userId)) == 0;
        UserAddress address = new UserAddress();
        address.setUserId(userId);
        applyCampusAddress(address, request.getCampusId(), request.getBuildingId(), request.getFloor(), request.getRoomNo(),
                request.getDeliveryPoint(), request.getDetail(), request.getLabel(), request.getRemark());
        boolean makeDefault = firstAddress || Boolean.TRUE.equals(request.getIsDefault());
        if (makeDefault) {
            clearDefault(userId);
        }
        address.setReceiverName(request.getReceiverName());
        address.setReceiverPhone(request.getReceiverPhone());
        address.setIsDefault(makeDefault ? 1 : 0);
        userAddressMapper.insert(address);
        return toView(address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AddressVO update(Long id, AddressUpdateDTO request) {
        Long userId = currentUserId();
        UserAddress address = requireOwnedAddress(id, userId);
        applyCampusAddress(address, request.getCampusId(), request.getBuildingId(), request.getFloor(), request.getRoomNo(),
                request.getDeliveryPoint(), request.getDetail(), request.getLabel(), request.getRemark());
        address.setReceiverName(request.getReceiverName());
        address.setReceiverPhone(request.getReceiverPhone());
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            clearDefault(userId);
            address.setIsDefault(1);
        }
        userAddressMapper.updateById(address);
        return toView(address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long userId = currentUserId();
        UserAddress address = requireOwnedAddress(id, userId);
        userAddressMapper.deleteById(address.getId());
        if (Integer.valueOf(1).equals(address.getIsDefault())) {
            UserAddress replacement = userAddressMapper.selectOne(Wrappers.<UserAddress>lambdaQuery()
                    .eq(UserAddress::getUserId, userId)
                    .orderByDesc(UserAddress::getId)
                    .last("LIMIT 1"));
            if (replacement != null) {
                replacement.setIsDefault(1);
                userAddressMapper.updateById(replacement);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AddressVO setDefault(Long id) {
        Long userId = currentUserId();
        UserAddress address = requireOwnedAddress(id, userId);
        clearDefault(userId);
        address.setIsDefault(1);
        userAddressMapper.updateById(address);
        return toView(address);
    }

    private void applyCampusAddress(UserAddress address, Long campusId, Long buildingId, String floor, String roomNo,
                                    String deliveryPoint, String detail, String label, String remark) {
        if (isBlank(roomNo) && isBlank(deliveryPoint)) {
            throw new BusinessException(400, "房间号或配送点至少填写一项");
        }
        Campus campus = campusMapper.selectById(campusId);
        if (campus == null || !Integer.valueOf(1).equals(campus.getStatus())) {
            throw new BusinessException(400, "校区不存在或未启用");
        }
        Building building = buildingMapper.selectById(buildingId);
        if (building == null || !Integer.valueOf(1).equals(building.getStatus())) {
            throw new BusinessException(400, "楼栋不存在或未启用");
        }
        if (!campusId.equals(building.getCampusId())) {
            throw new BusinessException(400, "所选楼栋不属于当前校区");
        }
        address.setCampusId(campus.getId());
        address.setArea(building.getArea());
        address.setBuildingId(building.getId());
        address.setBuildingType(building.getBuildingType());
        address.setBuildingName(building.getBuildingName());
        address.setFloor(trimToNull(floor));
        address.setRoomNo(trimToNull(roomNo));
        address.setDeliveryPoint(trimToNull(deliveryPoint));
        address.setDetail(trimToNull(detail));
        address.setLabel(trimToNull(label));
        address.setRemark(trimToNull(remark));
        address.setAddressType(CAMPUS_ADDRESS);
    }

    private AddressVO toView(UserAddress address) {
        Campus campus = address.getCampusId() == null ? null : campusMapper.selectById(address.getCampusId());
        return AddressVO.fromAddress(address, campus == null ? null : campus.getCampusName());
    }

    private Long currentUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        return userId;
    }

    private UserAddress requireOwnedAddress(Long id, Long userId) {
        UserAddress address = userAddressMapper.selectOne(Wrappers.<UserAddress>lambdaQuery()
                .eq(UserAddress::getId, id)
                .eq(UserAddress::getUserId, userId));
        if (address == null) {
            throw new BusinessException(404, "地址不存在或无权操作");
        }
        return address;
    }

    private void clearDefault(Long userId) {
        userAddressMapper.update(null, Wrappers.<UserAddress>lambdaUpdate()
                .eq(UserAddress::getUserId, userId)
                .eq(UserAddress::getIsDefault, 1)
                .set(UserAddress::getIsDefault, 0));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
