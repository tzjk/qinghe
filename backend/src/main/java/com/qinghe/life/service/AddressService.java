package com.qinghe.life.service;

import com.qinghe.life.dto.AddressCreateDTO;
import com.qinghe.life.dto.AddressUpdateDTO;
import com.qinghe.life.vo.AddressVO;

import java.util.List;

public interface AddressService {
    List<AddressVO> listCurrentUser();
    AddressVO getCurrentUserAddress(Long id);
    AddressVO create(AddressCreateDTO request);
    AddressVO update(Long id, AddressUpdateDTO request);
    void delete(Long id);
    AddressVO setDefault(Long id);
}
