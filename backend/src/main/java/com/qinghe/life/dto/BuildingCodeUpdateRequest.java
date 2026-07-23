package com.qinghe.life.dto;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
@Data public class BuildingCodeUpdateRequest { @NotBlank @Size(max = 32) @Pattern(regexp = "[A-Za-z0-9_-]+") private String buildingCode; }
