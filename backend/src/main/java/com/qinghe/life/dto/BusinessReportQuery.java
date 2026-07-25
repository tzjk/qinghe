package com.qinghe.life.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class BusinessReportQuery {
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
    private Integer top;

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public Integer getTop() { return top; }
    public void setTop(Integer top) { this.top = top; }
}
