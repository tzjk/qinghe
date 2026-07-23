package com.qinghe.life.vo;
import lombok.Data;
import java.util.List;
@Data public class DormQrResolveVO { private String campusName; private String buildingName; private String roomNo; private String bedNo; private String assetSetCode; private String assetSetStatus; private String assetHealthStatus; private List<String> assetNames; private Boolean available; private String unavailableReason; }
