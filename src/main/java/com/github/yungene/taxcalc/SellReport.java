package com.github.yungene.taxcalc;

public class SellReport {
	// Report is associated with a single stock type
	Stock stock;
	// Report is associated with a single sell transaction
	Transaction originalSellTransaction;
	// In case of 4 week rule, we can split sell transaction into two, if we sell more than we bought in past 4 weeks.
	Transaction sellTrasaction;
	// Each share sold has a corresponding buy transaction. This lists all the buy transactions.
	// Note that transactions here can be "partial" when we sell less than what was originally bought in the buy transaction.
	// This means that quantity of buy is adjusted to match the sell.
	// Sum of all quantities bought should be the same as what was sold.
	Transaction buyTransaction;

	// Whether we applied four week rule here.
	boolean fourWeekRuleApplied = false;
	boolean buyWithinFourWeeksAfterSell = false;
	boolean sellWithinFourWeeksAfterBuy = false;
	
	private long netGains = 0;
	
	public SellReport(Stock stock, Transaction originalSellTransaction) {
		this.stock = stock;
		this.originalSellTransaction = originalSellTransaction;
	}
	
	
	public SellReport(Stock stock, Transaction originalSellTransaction, Transaction sellTrasaction,
			Transaction buyTransaction) {
		super();
		this.stock = stock;
		this.originalSellTransaction = originalSellTransaction;
		this.sellTrasaction = sellTrasaction;
		this.buyTransaction = buyTransaction;
	}

	private long calculateNetGains() {
		long netGains = 0;
		// TODO: also calculate losses and gains
		long gains = this.buyTransaction.getQuantity() * (this.originalSellTransaction.getEuroTotalPrice() - this.buyTransaction.getEuroTotalPrice());
		netGains += gains;
		
		this.netGains = netGains;
		return netGains;
	}

	public long getNetGains() {
		this.calculateNetGains();
		return this.netGains;
	}
	
	public long getTaxableNetGains() {
		// TODO: have a setter for buyTransaction that refreshes the value
		this.calculateNetGains();
		if (this.fourWeekRuleApplied) {
			return Math.max(this.netGains, 0);
		}
		return this.netGains;
	}

	@Override
	public String toString() {
		return "SellReport [stock=" + stock + ", netTaxableGains=" + this.getTaxableNetGains()  + ", netGains=" + netGains + ", sellTrasaction=" + sellTrasaction + ", buyTransaction=" + buyTransaction
				+ ", originalSellTransaction=" + originalSellTransaction
				+ ", fourWeekRuleApplied=" + fourWeekRuleApplied + ", buyWithinFourWeeksAfterSell="
				+ buyWithinFourWeeksAfterSell + ", sellWithinFourWeeksAfterBuy=" + sellWithinFourWeeksAfterBuy + "]";
	}
	
	
}
