package com.qinghe.life.service.impl;

import com.qinghe.life.entity.OperateLog;
import com.qinghe.life.mapper.OperateLogMapper;
import com.qinghe.life.service.OperateLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperateLogServiceImpl implements OperateLogService {
    private final OperateLogMapper operateLogMapper;

    public OperateLogServiceImpl(OperateLogMapper operateLogMapper) {
        this.operateLogMapper = operateLogMapper;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void save(OperateLog operateLog) {
        operateLogMapper.insert(operateLog);
    }
}
