package com.github.yungene.taxcalc;

import java.time.temporal.ChronoUnit;
import java.util.Deque;
import java.util.LinkedList;

public class StockState {
	String isin;
	
	// oldBuys <-- fourWeeksBuys <-- NOW <-- futureBuysFourWeeksRule <-- futureBuys
	Deque<Transaction> futureBuys;
	Deque<Transaction> futureBuysFourWeeksRule;
	Deque<Transaction> fourWeeksBuys;
	Deque<Transaction> oldBuys;

	StockState(String isin) {
		this.isin = isin;
		this.futureBuys = new LinkedList<>();
		this.futureBuysFourWeeksRule = new LinkedList<>();
		this.fourWeeksBuys = new LinkedList<>();
		this.oldBuys = new LinkedList<>();
	}
	
	/**
	 * Re-adjust the queues given the transaction tx. 
	 * @param tx
	 */
	void syncQueues(Transaction tx) {
		while (!this.futureBuysFourWeeksRule.isEmpty()) {
			var fState = this.futureBuysFourWeeksRule.getFirst();
			if (fState.getSeqNum() == tx.getSeqNum() || fState.getDatetime().isBefore(tx.getDatetime())) {
				this.fourWeeksBuys.addLast(this.futureBuysFourWeeksRule.removeFirst());
				continue;
			}
			break;
		}
		while (!this.futureBuys.isEmpty()) {
			var fState = this.futureBuys.getFirst();
			if (fState.getSeqNum() == tx.getSeqNum() || fState.getDatetime().isBefore(tx.getDatetime())) {
				this.fourWeeksBuys.addLast(this.futureBuys.removeFirst());
				continue;
			}
			break;
		}
		var fwThreshold = tx.getDatetime().truncatedTo(ChronoUnit.DAYS).minusDays(28);
		while (!this.fourWeeksBuys.isEmpty()
				&& this.fourWeeksBuys.getFirst().getDatetime().isBefore(fwThreshold)) {
			this.oldBuys.addLast(this.fourWeeksBuys.removeFirst());
		}
	}
}
