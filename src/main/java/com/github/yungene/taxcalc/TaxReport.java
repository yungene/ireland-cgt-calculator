package com.github.yungene.taxcalc;

import java.util.ArrayList;
import java.util.List;

public class TaxReport {
	int year;
	List<SellReport> sales;

	private Long totalGains = null;
	private Long totalLosses = null;
	private Long netGains = null;

	public TaxReport(int year) {
		this.year = year;
		this.sales = new ArrayList<>();
	}

	public TaxReport(int year, List<SellReport> sales) {
		this.year = year;
		this.sales = sales;
		this.calculateGains();
	}

	public long getTaxableNetGains() {
		if (this.netGains == null) {
			this.calculateGains();
		}
		return this.netGains;
	}

	public void calculateGains() {
		long totalGains = 0;
		long totalLosses = 0;
		long netGains = 0;
		for (var sellReport : this.sales) {
			var gains = sellReport.getTaxableNetGains();
			netGains += gains;
		}

		this.totalGains = totalGains;
		this.totalLosses = totalLosses;
		this.netGains = netGains;
	}

	public String prettyPrint() {
		StringBuilder sb = new StringBuilder("");
		long taxableGains = this.getTaxableNetGains();
		sb.append(String.format("TaxReport for year %d. Total taxable gains are %s. Sales for the year were:\n",
				this.year, Transaction.milToString(taxableGains)));
		for (var sp : sales) {
			sb.append("\t");
			sb.append(sp);
			sb.append("\n");
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		return "TaxReport [year=" + year + ", totalGains=" + totalGains + ", totalLosses=" + totalLosses + ", netGains="
				+ netGains / 10000.0 + ", sales=" + sales + "]";
	}

}
