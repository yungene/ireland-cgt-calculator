package com.github.yungene.taxcalc;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

public class StockStateTest {

	@Test
	public void testSyncQueues() {
		String isin = "ISIN";
		String stockName = "Stock Name";
		Stock stock = new Stock(stockName, isin);
		StockState ss = new StockState(isin);
		var currTime = LocalDateTime.of(2022, 8, 8, 12, 0);
		var tx = new Transaction(true, currTime, stock, 20, 1000, 1000, "23", 23);
		
		var txOld = new Transaction(true, currTime.minusDays(50), stock, 10, 0, 0, "100", 100);
		// on the edge of 28 days window, but still within 28 days inclusive.
		var txRecent = new Transaction(true, currTime.minusDays(28), stock, 12, 0, 0, "26", 26);
	
		var txFuture = new Transaction(true, currTime.plusDays(1), stock, 3, 0, 0, "22", 22);
		var txFuture2 = new Transaction(true, currTime.plusDays(3), stock, 3, 0, 0, "21", 21);
		ss.oldBuys.add(txOld);
		ss.fourWeeksBuys.add(txRecent);
		ss.futureBuys.add(tx);
		ss.futureBuys.add(txFuture);
		ss.futureBuys.add(txFuture2);
		
		ss.syncQueues(tx);
		assertIterableEquals(List.of(txOld), ss.oldBuys);
		assertIterableEquals(List.of(txRecent, tx), ss.fourWeeksBuys);
		assertIterableEquals(List.of(), ss.futureBuysFourWeeksRule);
		assertIterableEquals(List.of(txFuture, txFuture2), ss.futureBuys);
		
		ss.syncQueues(txFuture);
		assertIterableEquals(List.of(txOld, txRecent), ss.oldBuys);
		assertIterableEquals(List.of(tx, txFuture), ss.fourWeeksBuys);
		assertIterableEquals(List.of(), ss.futureBuysFourWeeksRule);
		assertIterableEquals(List.of(txFuture2), ss.futureBuys);
		
		var txSellFuture = new Transaction(false, txFuture2.getDatetime().plusDays(30), stock, 2, 0, 0, "4", 4);
		ss.syncQueues(txSellFuture);
		assertIterableEquals(List.of(txOld, txRecent, tx, txFuture, txFuture2), ss.oldBuys);
		assertIterableEquals(List.of(), ss.fourWeeksBuys);
		assertIterableEquals(List.of(), ss.futureBuysFourWeeksRule);
		assertIterableEquals(List.of(), ss.futureBuys);
	}
	
	@Test
	public void testSyncQueueFW() {
		String isin = "ISIN";
		String stockName = "Stock Name";
		Stock stock = new Stock(stockName, isin);
		StockState ss = new StockState(isin);
		var tx = new Transaction(true, LocalDateTime.of(2022, 8, 8, 12, 0), stock, 20, 1000, 1000, "5", 5);
		
		var tx2 = new Transaction(true, tx.getDatetime().minusDays(3), stock, 20, 1000, 1000, "6", 6);
		
		// Since we can split buys, we can end up with mutliple transactions for the same overall buy.
		// We assume this is the only case when buy times can be the same. 
		var txMulti = new Transaction(true, tx.getDatetime().plusDays(26), stock, 1, 0, 0, "2", 2);
		var txMulti2 = new Transaction(true, txMulti.getDatetime(), stock, 2, 0, 0, "2", 2);
		// we shave same orderId, but different orderNum. This could happen is we execture the same overall buy
		// using multiple exchanges. Time can be the same.
		var tx3 = new Transaction(true, txMulti.getDatetime(), stock, 2, 0, 0, "1", 1);
		
		ss.futureBuysFourWeeksRule.addAll(List.of(tx2, tx));
		ss.futureBuys.addAll(List.of(txMulti, txMulti2, tx3));
		
		ss.syncQueues(tx);
		assertIterableEquals(List.of(), ss.oldBuys);
		assertIterableEquals(List.of(tx2, tx), ss.fourWeeksBuys);
		assertIterableEquals(List.of(), ss.futureBuysFourWeeksRule);
		assertIterableEquals(List.of(txMulti, txMulti2, tx3), ss.futureBuys);
		
		ss.syncQueues(txMulti);
		assertIterableEquals(List.of(tx2), ss.oldBuys);
		assertIterableEquals(List.of(tx, txMulti, txMulti2), ss.fourWeeksBuys);
		assertIterableEquals(List.of(), ss.futureBuysFourWeeksRule);
		assertIterableEquals(List.of(tx3), ss.futureBuys);
		
		ss.syncQueues(txMulti2);
		// no change
		assertIterableEquals(List.of(tx2), ss.oldBuys);
		assertIterableEquals(List.of(tx, txMulti, txMulti2), ss.fourWeeksBuys);
		assertIterableEquals(List.of(), ss.futureBuysFourWeeksRule);
		assertIterableEquals(List.of(tx3), ss.futureBuys);
		
		ss.syncQueues(tx3);
		assertIterableEquals(List.of(tx2), ss.oldBuys);
		assertIterableEquals(List.of(tx, txMulti, txMulti2, tx3), ss.fourWeeksBuys);
		assertIterableEquals(List.of(), ss.futureBuysFourWeeksRule);
		assertIterableEquals(List.of(), ss.futureBuys);
	}

}
