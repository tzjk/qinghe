package com.qinghe.life.vo;

import java.util.List;
import lombok.Data;

@Data
public class BatchPreviewVO {
    private String operation;
    private int selectedCount;
    private int eligibleCount;
    private int currentCheckinCount;
    private int assetSetCount;
    private int ineligibleCount;
    private String previewToken;
    private List<AdminStudentVO> students;
    private List<String> skippedReasons;
}
