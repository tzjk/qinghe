package com.qinghe.life.service;
import com.qinghe.life.dto.DormQrRequest;
import com.qinghe.life.dto.StudentProfileSaveRequest;
import com.qinghe.life.vo.DormQrResolveVO;
import com.qinghe.life.vo.StudentDormVO;
import com.qinghe.life.vo.StudentProfileVO;
public interface StudentDormService { StudentProfileVO getProfile(); StudentProfileVO saveProfile(StudentProfileSaveRequest request); DormQrResolveVO resolveQr(DormQrRequest request); StudentDormVO checkIn(DormQrRequest request); StudentDormVO myDorm(); }
