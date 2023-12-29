package com.github.yungene.taxcalc;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * We use 4 point precision. I.e. 4.35 euro is stored as 43500 in Java. Degiro's
 * CSV seems to provide at most 4 point precision as well. They do some rounding
 * as well.
 */
class Transaction {

	public static long euroToMil(long euros, long cents) {
		return 10000 * euros + 100 * cents;
	}

	public static long getEurosFromMil(long mil) {
		return (long) (mil / 10000);
	}

	public static long getCentsFromMil(long mil) {
		return (long) ((mil - Transaction.getEurosFromMil(mil) * 10000) / 100);
	}

	public static String milToString(long mil) {
		return String.format("%d.%d", getEurosFromMil(mil), getCentsFromMil(mil));
	}

	private boolean isBuy;
	private LocalDateTime datetime;
	private Stock stock;
	private int quantity;
	private long euroPrice;
	// total is inclusive of fees
	private long euroTotalPrice;
	// Not unique if we don't use aggregate report. Single order can get executed as
	// multiple transactions on multiple exchanges.
	private String orderId;
	// Sequence number based on order in CSV file, so based on datetime.
	// Can be used for quick ordering. This is assumed to have the same or higher
	// resolution that datetime.
	private int seqNum;

	public Transaction(boolean isBuy, LocalDateTime datetime, Stock stock, int quantity, long euroPrice,
			long euroTotalPrice, String orderId, int seqNum) {
		super();
		if (quantity < 0 || euroPrice < 0 || euroTotalPrice < 0) {
			throw new IllegalArgumentException("Some arguments supplied are < 0. Please investigate.");
		}
		this.isBuy = isBuy;
		this.datetime = datetime;
		this.stock = stock;
		this.quantity = quantity;

		this.euroPrice = euroPrice;
		this.euroTotalPrice = euroTotalPrice;
		this.orderId = orderId;
		this.seqNum = seqNum;
	}

	public boolean isBuy() {
		return this.isBuy;
	}

	public boolean isSell() {
		return !this.isBuy();
	}

	public LocalDateTime getDatetime() {
		return datetime;
	}

	public Stock getStock() {
		return stock;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public long getEuroPrice() {
		return euroPrice;
	}

	// This spreads out fees over all the shares.
	public long getEuroTotalPrice() {
		return this.euroTotalPrice;
	}

	public int getAbsQuantity() {
		return Math.abs(this.quantity);
	}

	public String getOrderId() {
		return orderId;
	}

	public int getSeqNum() {
		return seqNum;
	}
	
//	public String prettyPrint() {
//		StringBuilder sb = new StringBuilder("");
//		sb.append(isBuy)
//		return sb.toString();
//	}

	@Override
	public String toString() {
		return "Transaction [isBuy=" + isBuy + ", datetime=" + datetime + ", stock=" + stock + ", quantity=" + quantity
				+ ", euroPrice=" + euroPrice + ", euroTotalPrice=" + euroTotalPrice + ", orderId=" + orderId
				+ ", seqNum=" + seqNum + "]";
	}

	public Transaction copyWithNewQuantity(int quantity) {
		if (quantity < 0) {
			throw new RuntimeException("quantity should be >= 0");
		}
		if (quantity > this.quantity) {
			throw new RuntimeException("Cannot increase quantity.");
		}
		return new Transaction(this.isBuy, this.datetime, this.stock, quantity, this.euroPrice, this.euroTotalPrice,
				this.orderId, this.seqNum);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Transaction other = (Transaction) obj;
		return Objects.equals(datetime, other.datetime) && euroPrice == other.euroPrice
				&& euroTotalPrice == other.euroTotalPrice && isBuy == other.isBuy
				&& Objects.equals(orderId, other.orderId) && quantity == other.quantity && seqNum == other.seqNum
				&& Objects.equals(stock, other.stock);
	}

	@Override
	public int hashCode() {
		return Objects.hash(datetime, euroPrice, euroTotalPrice, isBuy, orderId, quantity, seqNum, stock);
	}

}
