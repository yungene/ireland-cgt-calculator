package com.github.yungene.taxcalc;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TaxEngine {
	public final static double TAX_EXEMPTION = 1270;
	public final static int FOUR_WEEK_RULE_DAYS = 28;

	List<Transaction> transactions;
	Map<Integer, TaxReport> reports;

	public TaxEngine(List<Transaction> transactions) {
		this.transactions = new ArrayList<>(transactions);
		// TODO: sort by datetime with seq number as tie-breaker. This way less
		// error-prone.
		this.transactions.sort((o1, o2) -> o2.getSeqNum() - o1.getSeqNum());
		this.reports = new HashMap<>();
	}


	void calculateTaxFull() {
		if (!reports.isEmpty()) {
			System.out.println("Already processed. Return without reprocess.");
			return;
		}
		Map<String, StockState> stockStates = new HashMap<>();
		for (var tx : this.transactions) {
			String isin = tx.getStock().getIsin();
			stockStates.putIfAbsent(isin, new StockState(isin));
			if (tx.isBuy()) {
				stockStates.get(isin).futureBuys.addLast(tx);
			}
		}

		Deque<Transaction> txDeque = new LinkedList<>(this.transactions);

		while (!txDeque.isEmpty()) {
			var tx = txDeque.removeFirst();
			int txYear = tx.getDatetime().getYear();
			this.reports.putIfAbsent(txYear, new TaxReport(txYear));

			var isin = tx.getStock().getIsin();
			var stockState = stockStates.get(isin);
			if (tx.isBuy()) {
				stockState.syncQueues(tx);
			} else {
				// Sell, so we do the matching
				var sellReport = new SellReport(tx.getStock(), tx);

				// first, we re-adjust our buy queues
				stockState.syncQueues(tx);

				int toMatch = tx.getQuantity();
				var futureThreshold = tx.getDatetime().truncatedTo(ChronoUnit.DAYS)
						.plusDays(TaxEngine.FOUR_WEEK_RULE_DAYS + 1);
				if (toMatch > 0 && !stockState.futureBuys.isEmpty()
						&& stockState.futureBuys.getFirst().getDatetime().isBefore(futureThreshold)) {
					sellReport.fourWeekRuleApplied |= true;
					sellReport.buyWithinFourWeeksAfterSell |= true;
					var sbPair = this.matchBuyAndSell(tx, stockState.futureBuys, txDeque);
					tx = sbPair[0];
					stockState.futureBuysFourWeeksRule.addLast(sbPair[1]);
				}

				// match sell with buys
				// try within 4 weeks first
				int toSell = tx.getQuantity();
				var fwThreshold = tx.getDatetime().truncatedTo(ChronoUnit.DAYS)
						.minusDays(TaxEngine.FOUR_WEEK_RULE_DAYS);
				if (toSell > 0 && !stockState.fourWeeksBuys.isEmpty()
						&& stockState.fourWeeksBuys.getFirst().getDatetime().isAfter(fwThreshold)) {
					sellReport.fourWeekRuleApplied |= true;
					sellReport.sellWithinFourWeeksAfterBuy |= true;
					var sbPair = this.matchBuyAndSell(tx, stockState.fourWeeksBuys, txDeque);
					tx = sbPair[0];
					sellReport.buyTransaction = sbPair[1];
				} else if (toSell > 0 && !stockState.oldBuys.isEmpty()) {
					var sbPair = this.matchBuyAndSell(tx, stockState.oldBuys, txDeque);
					tx = sbPair[0];
					sellReport.buyTransaction = sbPair[1];
				} else {
					throw new RuntimeException(String
							.format("Was not able to find enough buys to cover the sell. %s, %s", tx, sellReport));
				}
				sellReport.sellTrasaction = tx;
				this.reports.get(txYear).sales.add(sellReport);
			}
		}

		for (var report : this.reports.values()) {
			report.calculateGains();
		}
	}

	private Transaction[] matchBuyAndSell(Transaction sellTx, Deque<Transaction> buyDeque, Deque<Transaction> txDeque) {
		int toSell = sellTx.getQuantity();
		var buyTx = buyDeque.removeFirst();
		Transaction matchedBuyTx = buyTx;
		if (buyTx.getQuantity() > toSell) {
			// we need to split the buy tx into 2
			matchedBuyTx = buyTx.copyWithNewQuantity(toSell);
			var remTx = buyTx.copyWithNewQuantity(buyTx.getQuantity() - toSell);
			buyDeque.addFirst(remTx);
		} else if (buyTx.getQuantity() < toSell) {
			txDeque.addFirst(sellTx.copyWithNewQuantity(toSell - buyTx.getQuantity()));
			sellTx = sellTx.copyWithNewQuantity(buyTx.getQuantity());
		}
		// TODO: make this a class
		return new Transaction[] { sellTx, matchedBuyTx };
	}
}
