package com.withcare.statistic.dto;

public class ReportStatDTO {

	private int total_count; // 전체 신고 건수
	private int weekly_count; // 최근 7일 신고 건수

	public int getTotal_count() {
		return total_count;
	}

	public void setTotal_count(int total_count) {
		this.total_count = total_count;
	}

	public int getWeekly_count() {
		return weekly_count;
	}

	public void setWeekly_count(int weekly_count) {
		this.weekly_count = weekly_count;
	}

}
