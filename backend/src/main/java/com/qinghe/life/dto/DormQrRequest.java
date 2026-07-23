package com.qinghe.life.dto;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
@Data public class DormQrRequest { @NotBlank @Size(max=100) private String qrContent; }
