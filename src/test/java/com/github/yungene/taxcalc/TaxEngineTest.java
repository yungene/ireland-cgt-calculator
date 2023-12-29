package com.github.yungene.taxcalc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

public class TaxEngineTest {

	@Test
	public void testTaxEngineSimpleSellProfitAfterBuy() {
		Stock stockA = new Stock("StockA", "IsinA");
		Stock stockB = new Stock("StockB", "IsinB");

		var txBuyA1 = new Transaction(true, LocalDateTime.of(2021, 2, 12, 12, 0), stockA, 10,
				Transaction.euroToMil(30, 5), Transaction.euroToMil(32, 55), "ab1", 4);
		var txBuyB1 = new Transaction(true, LocalDateTime.of(2021, 2, 12, 12, 1), stockB, 10,
				Transaction.euroToMil(100, 0), Transaction.euroToMil(100, 0), "bb1", 3);
		var txSellA1 = new Transaction(false, LocalDateTime.of(2021, 5, 1, 11, 12), stockA, 10,
				Transaction.euroToMil(40, 5), Transaction.euroToMil(38, 55), "as1", 2);
		var txBuyB2 = new Transaction(true, LocalDateTime.of(2022, 3, 28, 16, 55), stockB, 5,
				Transaction.euroToMil(200, 0), Transaction.euroToMil(205, 0), "bb2", 1);
		var txSellB1 = new Transaction(false, LocalDateTime.of(2023, 11, 30, 9, 59), stockB, 15,
				Transaction.euroToMil(300, 0), Transaction.euroToMil(295, 0), "bs2", 0);

		List<Transaction> txs = List.of(txSellB1, txBuyB2, txSellA1, txBuyB1, txBuyA1);

		TaxEngine te = new TaxEngine(txs);

		te.calculateTaxFull();
		assertEquals(3, te.reports.size());
		var actualTR2021 = te.reports.get(2021);
		assertEquals(10 * (txSellA1.getEuroTotalPrice() - txBuyA1.getEuroTotalPrice()),
				actualTR2021.getTaxableNetGains());
		assertEquals(10 * (38_55_00 - 32_55_00), actualTR2021.getTaxableNetGains());

		var actualTR2023 = te.reports.get(2023);
		assertEquals(
				10 * (txSellB1.getEuroTotalPrice() - txBuyB1.getEuroTotalPrice())
						+ 5 * (txSellB1.getEuroTotalPrice() - txBuyB2.getEuroTotalPrice()),
				actualTR2023.getTaxableNetGains());
	}

	@Test
	public void TestOffsetingLossesAgainstGains() {
		Stock stockA = new Stock("StockA", "IsinA");
		Stock stockB = new Stock("StockB", "IsinB");

		var txBuyA1 = new Transaction(true, LocalDateTime.of(2021, 2, 12, 12, 0), stockA, 50,
				Transaction.euroToMil(10, 0), Transaction.euroToMil(10, 0), "ab1", 6);
		var txBuyA2 = new Transaction(true, LocalDateTime.of(2021, 3, 12, 12, 0), stockA, 50,
				Transaction.euroToMil(10, 0), Transaction.euroToMil(10, 0), "ab2", 5);
		var txBuyB1 = new Transaction(true, LocalDateTime.of(2021, 4, 12, 12, 1), stockB, 1000,
				Transaction.euroToMil(2, 0), Transaction.euroToMil(2, 0), "bb1", 4);
		// We make a gain on this sale.
		var txSellA1 = new Transaction(false, LocalDateTime.of(2021, 5, 1, 11, 12), stockA, 50,
				Transaction.euroToMil(20, 0), Transaction.euroToMil(20, 0), "as1", 3);
		// But we make a loss on the next sale
		var txSellA2 = new Transaction(false, LocalDateTime.of(2021, 5, 2, 11, 12), stockA, 50,
				Transaction.euroToMil(8, 0), Transaction.euroToMil(8, 0), "as2", 2);
		// We make loss of 500 on Stock B in 2021, and we can offset this loss again
		// gains on Stock A.
		var txSellB1 = new Transaction(false, LocalDateTime.of(2021, 11, 30, 9, 59), stockB, 500,
				Transaction.euroToMil(1, 0), Transaction.euroToMil(1, 0), "bs2", 1);
		// We make profit on Stock B in 2022, this is not affected by results in 2021.
		// If we want to carry over losses to next year, then we need to do this
		// manually.
		// Tax engine does not do that.
		var txSellB2 = new Transaction(false, LocalDateTime.of(2022, 1, 1, 9, 59), stockB, 500,
				Transaction.euroToMil(3, 0), Transaction.euroToMil(3, 0), "bs3", 0);

		List<Transaction> txs = List.of(txSellB2, txSellB1, txSellA2, txSellA1, txBuyB1, txBuyA2, txBuyA1);

		TaxEngine te = new TaxEngine(txs);

		te.calculateTaxFull();
		assertEquals(2, te.reports.size());
		var actualTR2021 = te.reports.get(2021);
		assertNotNull(actualTR2021);
		assertEquals(50 * Transaction.euroToMil(10, 0) - 50 * Transaction.euroToMil(2, 0)
				- 500 * Transaction.euroToMil(1, 0), actualTR2021.getTaxableNetGains());

		var actualTR2022 = te.reports.get(2022);
		assertEquals(Transaction.euroToMil(500 * 1, 0), actualTR2022.getTaxableNetGains());

	}

	@Test
	public void testRevenueExample1() {
		Stock stock = new Stock("Trio Ltd", "IsinA");
		var txBuy1 = new Transaction(true, LocalDateTime.of(2004, 1, 1, 12, 0), stock, 2000,
				Transaction.euroToMil(1, 0), Transaction.euroToMil(1, 0), "b1", 4);

		var txBuy2 = new Transaction(true, LocalDateTime.of(2006, 1, 1, 12, 0), stock, 4000,
				Transaction.euroToMil(1, 50), Transaction.euroToMil(1, 50), "b2", 3);
		// Change the price from 1.6666(6) to 1.67 for ease of use.
		var txSell1 = new Transaction(false, LocalDateTime.of(2017, 5, 1, 12, 0), stock, 3000,
				Transaction.euroToMil(1, 67), Transaction.euroToMil(1, 67), "s1", 2);
		TaxEngine te = new TaxEngine(List.of(txSell1, txBuy2, txBuy1));
		te.calculateTaxFull();
		assertEquals(3, te.reports.size());

		var actualTR2017 = te.reports.get(2017);
		assertNotNull(actualTR2017);
		assertEquals(
				Transaction.euroToMil(5010, 0) - 2000 * txBuy1.getEuroTotalPrice() - 1000 * txBuy2.getEuroTotalPrice(),
				actualTR2017.getTaxableNetGains());
	}

	// https://www.revenue.ie/en/gains-gifts-and-inheritance/transfering-an-asset/selling-or-disposing-of-shares.aspx
	// TODO: I believe this example is incorrect. Currently, we don't use the loss
	// and benefit Revenue, so we are not in debt to Revenue, but I believe this is
	// not correct and we can instead make use of this loss.
	@Test
	public void testRevenueExample2Jane() {
		Stock stock = new Stock("Abcee Ltd", "IsinA");
		var txBuy1 = new Transaction(true, LocalDateTime.of(2017, 4, 1, 12, 0), stock, 3000,
				Transaction.euroToMil(1, 0), Transaction.euroToMil(1, 0), "b1", 5);
		// again use 0.67 as a price instead of 0.66(6). Why do they pick such bad
		// examples?
		var txSell1 = new Transaction(false, LocalDateTime.of(2017, 4, 14, 12, 0), stock, 3000,
				Transaction.euroToMil(0, 67), Transaction.euroToMil(0, 67), "s1", 2);
		TaxEngine te = new TaxEngine(List.of(txSell1, txBuy1));
		te.calculateTaxFull();
		assertEquals(1, te.reports.size());

		var actualTR2017 = te.reports.get(2017);
		assertNotNull(actualTR2017);
		// Even though we made a loss, we have 0 taxable losses as the loss falls under
		// four weeks rule. So we swallow this loss and do not use it to offset any
		// gains.
		assertEquals(0, actualTR2017.getTaxableNetGains());
	}

	@Test
	public void testRevenueExample2Kevin() {
		Stock stockA = new Stock("Abcee Ltd", "IsinA");
		Stock stockB = new Stock("Xyzet Ltd", "IsinB");
		var txBuyA1 = new Transaction(true, LocalDateTime.of(2017, 4, 1, 12, 0), stockA, 3000,
				Transaction.euroToMil(1, 0), Transaction.euroToMil(1, 0), "ba1", 6);
		// again use 0.67 as a price instead of 0.66(6). Why do they pick such bad
		// examples?
		var txSellA1 = new Transaction(false, LocalDateTime.of(2017, 4, 14, 12, 0), stockA, 3000,
				Transaction.euroToMil(0, 67), Transaction.euroToMil(0, 67), "sa1", 5);
		// Buy-within-4-weeks-after-sell. This technically allows us to avail of the
		// loss made by previous sell-within-4-weeks-after-buy. But we can only use this
		// loss against the same stock. This is somewhat hard to keep track of, so we
		// eat this loss to benefit Revenue.
		// Note that we re-acquire less than we sold.
		var txBuyA2 = new Transaction(true, LocalDateTime.of(2017, 4, 21, 12, 0), stockA, 2000,
				Transaction.euroToMil(0, 50), Transaction.euroToMil(0, 50), "ba2", 4);
		// We eventually sell the re-acquired stock, and we make profit. We can
		// technically
		// offset some of the losses, but as we said, we don't do that for simplicity.
		var txSellA2 = new Transaction(false, LocalDateTime.of(2017, 6, 14, 12, 0), stockA, 2000,
				Transaction.euroToMil(1, 0), Transaction.euroToMil(1, 0), "sa2", 3);
		// We add another stock B. We make a profit on sale of it. However, we don't
		// offset any losses from Stock A against it as all the losses were under 4
		// weeks rule.
		var txBuyB1 = new Transaction(true, LocalDateTime.of(2017, 8, 21, 12, 0), stockB, 1000,
				Transaction.euroToMil(1, 0), Transaction.euroToMil(1, 0), "bb1", 2);
		var txSellB1 = new Transaction(false, LocalDateTime.of(2017, 11, 14, 12, 0), stockB, 1000,
				Transaction.euroToMil(2, 0), Transaction.euroToMil(2, 0), "sb1", 1);
		TaxEngine te = new TaxEngine(List.of(txBuyA1, txSellA1, txBuyA2, txSellA2, txBuyB1, txSellB1));
		te.calculateTaxFull();
		assertEquals(1, te.reports.size());

		var actualTR2017 = te.reports.get(2017);
		assertNotNull(actualTR2017);
		// Even though we made a loss, we have 0 taxable losses as the loss falls under
		// four weeks rule. So we swallow this loss and do not use it to offset any
		// gains.
		assertEquals(
				txSellA2.getQuantity() * (txSellA2.getEuroTotalPrice() - txBuyA2.getEuroTotalPrice())
						+ txSellB1.getQuantity() * (txSellB1.getEuroTotalPrice() - txBuyB1.getEuroTotalPrice()),
				actualTR2017.getTaxableNetGains());
	}

	/*
	 * The normal rules of identification apply to any excess where the quantity
	 * disposed of exceeds the quantity recently acquired. In the event of a sale of
	 * shares followed by a re-acquisition within four weeks a loss on the sale will
	 * be allowed only against gains derived from the disposal of the shares
	 * re-acquired within four weeks. Where the reacquisition involves a fraction
	 * only of the shares sold the restriction will be confined to a corresponding
	 * fraction of the loss
	 */
	@Test
	public void testWashSale() {
		Stock stockA = new Stock("Stock A", "IsinA");
		Stock stockB = new Stock("Stock B", "IsinB");
		var txBuyA1 = new Transaction(true, LocalDateTime.of(2020, 4, 1, 12, 0), stockA, 3000,
				Transaction.euroToMil(3, 0), Transaction.euroToMil(3, 0), "ba1", 6);
		// Sell for loss after more than 1 year of holding the stock
		var txSellA1 = new Transaction(false, LocalDateTime.of(2021, 4, 14, 12, 0), stockA, 3000,
				Transaction.euroToMil(2, 0), Transaction.euroToMil(2, 0), "sa1", 5);
		// Buy re-acquire part of what was sold within 28 days of sale. This means that
		// four weeks rule applies and we just did a wash sale on the proportion
		// re-acquired. We cannot use the losses for what we re-acquired to offset other
		// gains.
		// It should be safe to forget about the losses that can be offset against
		// re-acquired stocks.
		var txBuyA2 = new Transaction(true, LocalDateTime.of(2021, 4, 21, 12, 0), stockA, 500,
				Transaction.euroToMil(4, 0), Transaction.euroToMil(4, 0), "ba2", 4);
		// Re-acquire in 2 tranches
		var txBuyA3 = new Transaction(true, LocalDateTime.of(2021, 4, 25, 12, 0), stockA, 1500,
				Transaction.euroToMil(3, 0), Transaction.euroToMil(3, 0), "ba3", 3);

		// Buy stock B
		var txBuyB1 = new Transaction(true, LocalDateTime.of(2021, 8, 21, 12, 0), stockB, 2000,
				Transaction.euroToMil(1, 0), Transaction.euroToMil(1, 0), "bb1", 2);
		// Sell stock B for profit after holding it for more than 28 days.
		var txSellB1 = new Transaction(false, LocalDateTime.of(2021, 11, 14, 12, 0), stockB, 2000,
				Transaction.euroToMil(2, 0), Transaction.euroToMil(2, 0), "sb1", 1);
		TaxEngine te = new TaxEngine(List.of(txBuyA1, txSellA1, txBuyA2, txBuyA3, txBuyB1, txSellB1));
		te.calculateTaxFull();
		assertEquals(2, te.reports.size());

		var actualTR2021 = te.reports.get(2021);
		assertNotNull(actualTR2021);
		// We make 2000.00 gain on Stock B. We make 3000.00 loss on Stock A, but due to
		// re-acquiring and four week rule we can only use 1000.00 to offset the gains.
		// So we expect net gains to be 1000.00.
		assertEquals(1000 * Transaction.euroToMil(1, 0), actualTR2021.getTaxableNetGains());
	}

	@Test
	public void testWashSale2() {
		Stock stockA = new Stock("Stock A", "IsinA");
		Stock stockB = new Stock("Stock B", "IsinB");
		var txBuyA1 = new Transaction(true, LocalDateTime.of(2020, 4, 1, 12, 0), stockA, 3000,
				Transaction.euroToMil(3, 0), Transaction.euroToMil(3, 0), "ba1", 6);
		// Sell for loss after more than 1 year of holding the stock
		var txSellA1 = new Transaction(false, LocalDateTime.of(2021, 4, 14, 12, 0), stockA, 3000,
				Transaction.euroToMil(2, 0), Transaction.euroToMil(2, 0), "sa1", 5);
		// Buy re-acquire part of what was sold within 28 days of sale. This means that
		// four weeks rule applies and we just did a wash sale on the proportion
		// re-acquired. We cannot use the losses for what we re-acquired to offset other
		// gains.
		// It should be safe to forget about the losses that can be offset against
		// re-acquired stocks.
		var txBuyA2 = new Transaction(true, LocalDateTime.of(2021, 4, 21, 12, 0), stockA, 2000,
				Transaction.euroToMil(4, 0), Transaction.euroToMil(4, 0), "ba2", 4);
		var txSellA2 = new Transaction(false, LocalDateTime.of(2021, 6, 14, 12, 0), stockA, 2000,
				Transaction.euroToMil(6, 0), Transaction.euroToMil(6, 0), "sa2", 3);

		// Buy stock B
		var txBuyB1 = new Transaction(true, LocalDateTime.of(2021, 8, 21, 12, 0), stockB, 2000,
				Transaction.euroToMil(1, 0), Transaction.euroToMil(1, 0), "bb1", 2);
		// Sell stock B for profit after holding it for more than 28 days.
		var txSellB1 = new Transaction(false, LocalDateTime.of(2021, 11, 14, 12, 0), stockB, 2000,
				Transaction.euroToMil(2, 0), Transaction.euroToMil(2, 0), "sb1", 1);
		TaxEngine te = new TaxEngine(List.of(txBuyA1, txSellA1, txBuyA2, txSellA2, txBuyB1, txSellB1));
		te.calculateTaxFull();
		assertEquals(2, te.reports.size());

		var actualTR2021 = te.reports.get(2021);
		assertNotNull(actualTR2021);
		// We make 2000.00 gain on Stock B. We make 3000.00 loss on Stock A, but due to
		// re-acquiring and four week rule we can only use 1000.00 to offset the gains.
		// We also make 4000.00 on Stock A, but we choose to not use 2000.00 losses to
		// offset. So we expect net gains to be 5000.00.
		assertEquals(5000 * Transaction.euroToMil(1, 0), actualTR2021.getTaxableNetGains());
	}

	@Test
	public void testWashSale3() {
		Stock stockA = new Stock("Stock A", "IsinA");
		Stock stockB = new Stock("Stock B", "IsinB");
		var txBuyA1 = new Transaction(true, LocalDateTime.of(2020, 4, 1, 12, 0), stockA, 3000,
				Transaction.euroToMil(3, 0), Transaction.euroToMil(3, 0), "ba1", 6);
		// Sell for loss after more than 1 year of holding the stock
		var txSellA1 = new Transaction(false, LocalDateTime.of(2021, 4, 14, 12, 0), stockA, 3000,
				Transaction.euroToMil(2, 0), Transaction.euroToMil(2, 0), "sa1", 5);
		// Re-acquire more than sold. This should not change anything.
		var txBuyA2 = new Transaction(true, LocalDateTime.of(2021, 4, 21, 12, 0), stockA, 5000,
				Transaction.euroToMil(4, 0), Transaction.euroToMil(4, 0), "ba2", 4);
		var txSellA2 = new Transaction(false, LocalDateTime.of(2021, 6, 14, 12, 0), stockA, 5000,
				Transaction.euroToMil(6, 0), Transaction.euroToMil(6, 0), "sa2", 3);

		// Buy stock B
		var txBuyB1 = new Transaction(true, LocalDateTime.of(2021, 8, 21, 12, 0), stockB, 2000,
				Transaction.euroToMil(1, 0), Transaction.euroToMil(1, 0), "bb1", 2);
		// Sell stock B for profit after holding it for more than 28 days.
		var txSellB1 = new Transaction(false, LocalDateTime.of(2021, 11, 14, 12, 0), stockB, 2000,
				Transaction.euroToMil(2, 0), Transaction.euroToMil(2, 0), "sb1", 1);
		TaxEngine te = new TaxEngine(List.of(txBuyA1, txSellA1, txBuyA2, txSellA2, txBuyB1, txSellB1));
		te.calculateTaxFull();
		assertEquals(2, te.reports.size());

		var actualTR2021 = te.reports.get(2021);
		assertNotNull(actualTR2021);
		// We make 2000.00 gain on Stock B. We make 3000.00 loss on Stock A, but due to
		// re-acquiring and four week rule we can only use 1000.00 to offset the gains.
		// We also make 10000.00 on Stock A, but we choose to not use 2000.00 losses to
		// offset. So we expect net gains to be 12000.00.
		assertEquals(12000 * Transaction.euroToMil(1, 0), actualTR2021.getTaxableNetGains());
	}

	@Test
	public void testWashSale4() {
		Stock stockA = new Stock("Stock A", "IsinA");
		Stock stockB = new Stock("Stock B", "IsinB");
		var txBuyA1 = new Transaction(true, LocalDateTime.of(2020, 4, 1, 12, 0), stockA, 1000,
				Transaction.euroToMil(3, 0), Transaction.euroToMil(3, 0), "ba1", 10);
		var txBuyA2 = new Transaction(true, LocalDateTime.of(2021, 1, 1, 12, 0), stockA, 1000,
				Transaction.euroToMil(5, 0), Transaction.euroToMil(5, 0), "ba2", 8);
		// Sell for loss holding for >28 days
		var txSellA1 = new Transaction(false, LocalDateTime.of(2021, 4, 1, 12, 0), stockA, 1000,
				Transaction.euroToMil(2, 0), Transaction.euroToMil(2, 0), "sa1", 7);
		var txSellA2 = new Transaction(false, LocalDateTime.of(2021, 4, 2, 12, 0), stockA, 500,
				Transaction.euroToMil(3, 0), Transaction.euroToMil(3, 0), "sa2", 6);
		// Re-acquire
		var txBuyA3 = new Transaction(true, LocalDateTime.of(2021, 4, 21, 12, 0), stockA, 1000,
				Transaction.euroToMil(6, 0), Transaction.euroToMil(6, 0), "ba3", 5);
		// Note that txBuyA4 is >28 days after txSellA1.
		// It is on the 28th day past txSellA2. Note that we interpret 2 weeks as within
		// 28 days after day of sale. Not 100% if this right.
		var txBuyA4 = new Transaction(true, LocalDateTime.of(2021, 4, 30, 12, 0), stockA, 500,
				Transaction.euroToMil(7, 0), Transaction.euroToMil(7, 0), "ba4", 4);

		var txSellA3 = new Transaction(false, LocalDateTime.of(2021, 6, 14, 12, 0), stockA, 1000,
				Transaction.euroToMil(10, 0), Transaction.euroToMil(10, 0), "sa3", 3);

		// Buy stock B
		var txBuyB1 = new Transaction(true, LocalDateTime.of(2021, 8, 21, 12, 0), stockB, 2000,
				Transaction.euroToMil(1, 0), Transaction.euroToMil(1, 0), "bb1", 2);
		// Sell stock B for profit after holding it for more than 28 days.
		var txSellB1 = new Transaction(false, LocalDateTime.of(2021, 11, 14, 12, 0), stockB, 2000,
				Transaction.euroToMil(2, 0), Transaction.euroToMil(2, 0), "sb1", 1);
		TaxEngine te = new TaxEngine(
				List.of(txBuyA1, txBuyA2, txSellA1, txSellA2, txBuyA3, txBuyA4, txSellA3, txBuyB1, txSellB1));
		te.calculateTaxFull();
		assertEquals(2, te.reports.size());

		var actualTR2021 = te.reports.get(2021);
		assertNotNull(actualTR2021);
		// We make 2000.00 gain on Stock B. We make 1000.00+1000.00 loss on Stock A, but
		// due to re-acquiring and four week rule we are not going to use this loss for
		// offsetting. We then make 4500.00 gain on Stock A (5*500+4*500). So we expect
		// net gains to be 6500.00.
		assertEquals(6500 * Transaction.euroToMil(1, 0), actualTR2021.getTaxableNetGains());
	}

	// Test that it is FIFO within 4 weeks, not LIFO.
	// Test that we don't use loss for offsetting if we sell wtihin 4 weeks of buy.
	@Test
	public void testFourWeekSellNoReacquire1() {
		Stock stockA = new Stock("Stock A", "IsinA");
		Stock stockB = new Stock("Stock B", "IsinB");
		// Buy 100 of A in Jan 2020
		var txBuyA1 = new Transaction(true, LocalDateTime.of(2020, 1, 1, 12, 0), stockA, 100,
				Transaction.euroToMil(3, 0), Transaction.euroToMil(3, 0), "ba1", 10);
		// Buy 100 of A on 1 Mar 2021
		var txBuyA2 = new Transaction(true, LocalDateTime.of(2021, 3, 1, 12, 0), stockA, 100,
				Transaction.euroToMil(5, 0), Transaction.euroToMil(5, 0), "ba2", 8);
		// Buy 100 of A on 14 Mar 2021
		var txBuyA3 = new Transaction(true, LocalDateTime.of(2021, 3, 14, 12, 0), stockA, 100,
				Transaction.euroToMil(6, 0), Transaction.euroToMil(6, 0), "ba3", 6);
		// Sell 100 of A on 21 Mar 2021, we use FIFO within 28 days, so first sell what
		// we bought on 1 Mar 2021. We get loss of -300.00. We apply 4 week rule and
		// don't use this loss for offsetting gains.
		var txSellA1 = new Transaction(false, LocalDateTime.of(2021, 4, 1, 12, 0), stockA, 100,
				Transaction.euroToMil(2, 0), Transaction.euroToMil(2, 0), "sa1", 5);
		// Sell 50 of A on 22 Mar 2021, we use FIFO within 28 days, so first sell what
		// we bought on 14 Mar 2021. We get loss of -50.00. We apply 4 week rule and
		// don't use this loss for offsetting gains.
		var txSellA2 = new Transaction(false, LocalDateTime.of(2021, 4, 2, 12, 0), stockA, 50,
				Transaction.euroToMil(5, 0), Transaction.euroToMil(5, 0), "sa2", 4);

		// Sell 100 of A on 14 Jun 2021, we use normal FIFO as not buys in the last 4
		// weeks. So we first sell what we bought on 1 Jan 2020. We get a profit of
		// 700.00.
		var txSellA3 = new Transaction(false, LocalDateTime.of(2021, 6, 14, 12, 0), stockA, 100,
				Transaction.euroToMil(10, 0), Transaction.euroToMil(10, 0), "sa3", 3);

		// Buy stock B on 21 Aug 2021.
		var txBuyB1 = new Transaction(true, LocalDateTime.of(2021, 8, 21, 12, 0), stockB, 2000,
				Transaction.euroToMil(1, 0), Transaction.euroToMil(1, 0), "bb1", 2);
		// Sell stock B on 14 Nov 2021 for profit after holding it for more than 28
		// days. We get 2000.00 profit.
		var txSellB1 = new Transaction(false, LocalDateTime.of(2021, 11, 14, 12, 0), stockB, 2000,
				Transaction.euroToMil(2, 0), Transaction.euroToMil(2, 0), "sb1", 1);
		TaxEngine te = new TaxEngine(
				List.of(txSellB1, txBuyB1, txSellA3, txSellA2, txSellA1, txBuyA3, txBuyA2, txBuyA1));
		te.calculateTaxFull();

		var actualTR2021 = te.reports.get(2021);
		assertNotNull(actualTR2021);
		// We make 2000.00 gain on Stock B. We make -(300.00+50.00) loss on Stock A, but
		// due to four week rule we are not going to use this loss for offsetting. We
		// then make 700.00 gain on Stock A. So we expect net gains to be 2700.00.
		assertEquals(2700 * Transaction.euroToMil(1, 0), actualTR2021.getTaxableNetGains());
	}

	// Sell within28 days for profit
	@Test
	public void testFourWeekSellForProfit() {
		Stock stockA = new Stock("Stock A", "IsinA");
		Stock stockB = new Stock("Stock B", "IsinB");
		// Buy 500 of A on 1 Jan 2020
		var txBuyA1 = new Transaction(true, LocalDateTime.of(2020, 1, 1, 12, 0), stockA, 500,
				Transaction.euroToMil(1, 0), Transaction.euroToMil(1, 0), "ba1", 20);
		// Buy 100 of A on 1 Jan 2021
		var txBuyA2 = new Transaction(true, LocalDateTime.of(2021, 1, 1, 12, 0), stockA, 100,
				Transaction.euroToMil(3, 0), Transaction.euroToMil(3, 0), "ba2", 10);
		// Sell 100 of A on 12 Jan 2021 for profit of 100.00. We sell shares bought in
		// last 4 weeks.
		var txSellA1 = new Transaction(false, LocalDateTime.of(2021, 1, 12, 12, 0), stockA, 100,
				Transaction.euroToMil(4, 0), Transaction.euroToMil(4, 0), "sa1", 9);
		// Buy 100 of A on 20 Jan 2021. We re-acquire the shares. But 4 week rule for
		// re-acquire does not apply to profits, only to losses.
		var txBuyA3 = new Transaction(true, LocalDateTime.of(2021, 1, 21, 13, 0), stockA, 100,
				Transaction.euroToMil(2, 0), Transaction.euroToMil(2, 0), "ba3", 8);

		// Sell 100 of A on 14 Jun 2021, we use normal FIFO as not buys in the last 4
		// weeks. So we first sell what we bought on 1 Jan 2020. We get a profit of
		// 450.00.
		var txSellA3 = new Transaction(false, LocalDateTime.of(2021, 6, 14, 12, 0), stockA, 50,
				Transaction.euroToMil(10, 0), Transaction.euroToMil(10, 0), "sa3", 3);

		// Buy stock B on 21 Aug 2021.
		var txBuyB1 = new Transaction(true, LocalDateTime.of(2021, 8, 21, 12, 0), stockB, 2000,
				Transaction.euroToMil(1, 0), Transaction.euroToMil(1, 0), "bb1", 2);
		// Sell stock B on 14 Nov 2021 for profit after holding it for more than 28
		// days. We get 2000.00 profit.
		var txSellB1 = new Transaction(false, LocalDateTime.of(2021, 11, 14, 12, 0), stockB, 2000,
				Transaction.euroToMil(2, 0), Transaction.euroToMil(2, 0), "sb1", 1);
		TaxEngine te = new TaxEngine(List.of(txSellB1, txBuyB1, txSellA3, txBuyA3, txSellA1, txBuyA2, txBuyA1));
		te.calculateTaxFull();

		var actualTR2021 = te.reports.get(2021);
		assertNotNull(actualTR2021);
		// We make 2000.00 gain on Stock B. We make (100+450) on Stock A. So we expect
		// net gains to be 2550.00.
		assertEquals((100 + 450 + 2000) * Transaction.euroToMil(1, 0), actualTR2021.getTaxableNetGains());
	}

	// sell within 28 days for profit more than bought
	@Test
	public void testFourWeekSellForProfitRemainderFIFO() {
		Stock stockA = new Stock("Stock A", "IsinA");
		Stock stockB = new Stock("Stock B", "IsinB");
		// Buy 500 of A on 1 Jan 2020
		var txBuyA1 = new Transaction(true, LocalDateTime.of(2020, 1, 1, 12, 0), stockA, 100,
				Transaction.euroToMil(1, 0), Transaction.euroToMil(1, 0), "ba1", 20);
		// Buy 200 of A on 1 Oct 2020
		var txBuyA2 = new Transaction(true, LocalDateTime.of(2020, 10, 1, 12, 0), stockA, 200,
				Transaction.euroToMil(8, 0), Transaction.euroToMil(8, 0), "ba2", 15);
		// Buy 100 of A on 1 Jan 2021
		var txBuyA3 = new Transaction(true, LocalDateTime.of(2021, 1, 1, 12, 0), stockA, 100,
				Transaction.euroToMil(3, 0), Transaction.euroToMil(3, 0), "ba3", 10);
		// Sell 200 of A on 12 Jan 2021 for profit of 100.00+300.00=400.00. We sell 100
		// shares bought in last 4 weeks, and then use standard FIFO.
		var txSellA1 = new Transaction(false, LocalDateTime.of(2021, 1, 12, 12, 0), stockA, 200,
				Transaction.euroToMil(4, 0), Transaction.euroToMil(4, 0), "sa1", 9);
		// Buy 100 of A on 20 Jan 2021. We re-acquire the shares. But 4 week rule for
		// re-acquire does not apply to profits, only to losses.
		var txBuyA4 = new Transaction(true, LocalDateTime.of(2021, 1, 21, 13, 0), stockA, 100,
				Transaction.euroToMil(2, 0), Transaction.euroToMil(2, 0), "ba4", 8);

		// Sell 100 of A on 14 Jun 2021, we use normal FIFO as not buys in the last 4
		// weeks. So we first sell what we bought on 1 Oct 2020. We get a profit of 400.
		var txSellA3 = new Transaction(false, LocalDateTime.of(2021, 6, 14, 12, 0), stockA, 50,
				Transaction.euroToMil(10, 0), Transaction.euroToMil(10, 0), "sa3", 3);

		// Buy stock B on 21 Aug 2021.
		var txBuyB1 = new Transaction(true, LocalDateTime.of(2021, 8, 21, 12, 0), stockB, 2000,
				Transaction.euroToMil(1, 0), Transaction.euroToMil(1, 0), "bb1", 2);
		// Sell stock B on 14 Nov 2021 for profit after holding it for more than 28
		// days. We get 2000.00 profit.
		var txSellB1 = new Transaction(false, LocalDateTime.of(2021, 11, 14, 12, 0), stockB, 2000,
				Transaction.euroToMil(2, 0), Transaction.euroToMil(2, 0), "sb1", 1);
		TaxEngine te = new TaxEngine(
				List.of(txSellB1, txBuyB1, txSellA3, txBuyA4, txSellA1, txBuyA3, txBuyA2, txBuyA1));
		te.calculateTaxFull();

		var actualTR2021 = te.reports.get(2021);
		assertNotNull(actualTR2021);
		// We make 2000.00 gain on Stock B. We make (400+100) on Stock A. So we expect
		// net gains to be 2500.00.
		assertEquals((100 + 400 + 2000) * Transaction.euroToMil(1, 0), actualTR2021.getTaxableNetGains());
	}

	// Test 28 day boundaries. It is not fully clear, but I think it should be
	// 28 days before, 1 date of sale, 28 days after for the 4 weeks rule.
	// TODO: maybe highlight to user if we are at the boundary.
	@Test
	public void testFourWeekRuleBoundaries() {
		Stock stockA = new Stock("Stock A", "IsinA");
		var txBuyA1 = new Transaction(true, LocalDateTime.of(2023, 1, 1, 12, 0), stockA, 1000,
				Transaction.euroToMil(3, 0), Transaction.euroToMil(3, 0), "ba1", 10);

		var txSellA1 = new Transaction(false, LocalDateTime.of(2023, 5, 1, 12, 0), stockA, 1000,
				Transaction.euroToMil(2, 0), Transaction.euroToMil(2, 0), "sa1", 7);
		// Re-acquire on the 28th day past day of sale
		var txBuyA3 = new Transaction(true, LocalDateTime.of(2023, 5, 29, 15, 0), stockA, 1000,
				Transaction.euroToMil(6, 0), Transaction.euroToMil(6, 0), "ba2", 5);

		TaxEngine te = new TaxEngine(List.of(txBuyA1, txSellA1, txBuyA3));
		te.calculateTaxFull();

		var actualTR2023 = te.reports.get(2023);
		assertNotNull(actualTR2023);
		// We expect wash sale rule to apply
		assertEquals(0, actualTR2023.getTaxableNetGains());
	}

	@Test
	public void testFourWeekRuleBoundaries2() {
		Stock stockA = new Stock("Stock A", "IsinA");
		var txBuyA1 = new Transaction(true, LocalDateTime.of(2023, 1, 1, 12, 0), stockA, 1000,
				Transaction.euroToMil(3, 0), Transaction.euroToMil(3, 0), "ba1", 10);

		var txSellA1 = new Transaction(false, LocalDateTime.of(2023, 5, 1, 12, 0), stockA, 1000,
				Transaction.euroToMil(2, 0), Transaction.euroToMil(2, 0), "sa1", 7);
		// Re-acquire on the 29th day past day of sale
		var txBuyA3 = new Transaction(true, LocalDateTime.of(2023, 5, 30, 11, 0), stockA, 1000,
				Transaction.euroToMil(6, 0), Transaction.euroToMil(6, 0), "ba2", 5);

		TaxEngine te = new TaxEngine(List.of(txBuyA1, txSellA1, txBuyA3));
		te.calculateTaxFull();

		var actualTR2023 = te.reports.get(2023);
		assertNotNull(actualTR2023);
		// We expect loss
		assertEquals(-1000 * Transaction.euroToMil(1, 0), actualTR2023.getTaxableNetGains());
	}

	// These two tests cover the scenario from Revenue's example. But I am not sure
	// this is correct. The example benefits Revenue, but there is nothing in
	// official documents supporting this calculation.
	@Test
	public void testFourWeekRuleBoundariesSale() {
		Stock stockA = new Stock("Stock A", "IsinA");
		var txBuyA1 = new Transaction(true, LocalDateTime.of(2023, 1, 1, 12, 0), stockA, 1000,
				Transaction.euroToMil(3, 0), Transaction.euroToMil(3, 0), "ba1", 10);
		// sell on the 29th day
		var txSellA1 = new Transaction(false, LocalDateTime.of(2023, 1, 29, 15, 0), stockA, 1000,
				Transaction.euroToMil(2, 0), Transaction.euroToMil(2, 0), "sa1", 7);

		TaxEngine te = new TaxEngine(List.of(txBuyA1, txSellA1));
		te.calculateTaxFull();

		var actualTR2023 = te.reports.get(2023);
		assertNotNull(actualTR2023);
		// We expect 4 weeks rule to restrict the use of loss
		assertEquals(0, actualTR2023.getTaxableNetGains());
	}

	@Test
	public void testFourWeekRuleBoundariesSale2() {
		Stock stockA = new Stock("Stock A", "IsinA");
		var txBuyA1 = new Transaction(true, LocalDateTime.of(2023, 1, 1, 12, 0), stockA, 1000,
				Transaction.euroToMil(3, 0), Transaction.euroToMil(3, 0), "ba1", 10);
		// sell on the 30th day
		var txSellA1 = new Transaction(false, LocalDateTime.of(2023, 1, 30, 2, 0), stockA, 1000,
				Transaction.euroToMil(2, 0), Transaction.euroToMil(2, 0), "sa1", 7);

		TaxEngine te = new TaxEngine(List.of(txBuyA1, txSellA1));
		te.calculateTaxFull();

		var actualTR2023 = te.reports.get(2023);
		assertNotNull(actualTR2023);
		// We expect a loss
		assertEquals(-1000 * Transaction.euroToMil(1, 0), actualTR2023.getTaxableNetGains());
	}

	// Test 4 week boundary for FIFO, assume profit and check that it first matches
	// with 4 weeks.
	@Test
	public void testFourWeekRuleBoundariesSale3Profit() {
		Stock stockA = new Stock("Stock A", "IsinA");
		var txBuyA1 = new Transaction(true, LocalDateTime.of(2023, 1, 1, 12, 0), stockA, 1000,
				Transaction.euroToMil(5, 0), Transaction.euroToMil(5, 0), "ba1", 10);
		var txBuyA2 = new Transaction(true, LocalDateTime.of(2023, 3, 1, 12, 0), stockA, 1000,
				Transaction.euroToMil(3, 0), Transaction.euroToMil(3, 0), "ba2", 8);
		// sell on the 29th day
		var txSellA1 = new Transaction(false, LocalDateTime.of(2023, 3, 29, 2, 0), stockA, 1000,
				Transaction.euroToMil(4, 0), Transaction.euroToMil(4, 0), "sa1", 7);

		TaxEngine te = new TaxEngine(List.of(txBuyA1, txBuyA2, txSellA1));
		te.calculateTaxFull();

		var actualTR2023 = te.reports.get(2023);
		assertNotNull(actualTR2023);
		// We expect a profit
		assertEquals(1000 * Transaction.euroToMil(1, 0), actualTR2023.getTaxableNetGains());
	}

	@Test
	public void testFourWeekRuleFIFOProfit() {
		Stock stockA = new Stock("Stock A", "IsinA");
		var txBuyA1 = new Transaction(true, LocalDateTime.of(2023, 1, 1, 12, 0), stockA, 1000,
				Transaction.euroToMil(5, 0), Transaction.euroToMil(5, 0), "ba1", 10);
		var txBuyA2 = new Transaction(true, LocalDateTime.of(2023, 3, 1, 12, 0), stockA, 1000,
				Transaction.euroToMil(3, 0), Transaction.euroToMil(3, 0), "ba2", 8);
		// sell within 4 weeks of buy.
		var txSellA1 = new Transaction(false, LocalDateTime.of(2023, 3, 21, 2, 0), stockA, 1500,
				Transaction.euroToMil(4, 0), Transaction.euroToMil(4, 0), "sa1", 7);

		TaxEngine te = new TaxEngine(List.of(txBuyA1, txBuyA2, txSellA1));
		te.calculateTaxFull();

		var actualTR2023 = te.reports.get(2023);
		assertNotNull(actualTR2023);
		// We make 1000.00 profit on shares bought in last 4 weeks, we make -500.00 loss
		// on shares bought >4weeks ago.
		assertEquals((1000 - 500) * Transaction.euroToMil(1, 0), actualTR2023.getTaxableNetGains());
	}

	@Test
	public void testFourWeekRuleFIFOProfitLoss() {
		Stock stockA = new Stock("Stock A", "IsinA");
		var txBuyA1 = new Transaction(true, LocalDateTime.of(2023, 1, 1, 12, 0), stockA, 1000,
				Transaction.euroToMil(3, 0), Transaction.euroToMil(3, 0), "ba1", 10);
		var txBuyA2 = new Transaction(true, LocalDateTime.of(2023, 3, 1, 12, 0), stockA, 1000,
				Transaction.euroToMil(5, 0), Transaction.euroToMil(5, 0), "ba2", 8);
		// sell within 4 weeks of buy.
		var txSellA1 = new Transaction(false, LocalDateTime.of(2023, 3, 21, 2, 0), stockA, 1500,
				Transaction.euroToMil(4, 0), Transaction.euroToMil(4, 0), "sa1", 7);

		TaxEngine te = new TaxEngine(List.of(txBuyA1, txBuyA2, txSellA1));
		te.calculateTaxFull();

		var actualTR2023 = te.reports.get(2023);
		assertNotNull(actualTR2023);
		// We make -1000.00 loss on sale of shares bought <4 weeks ago, but we disregard
		// this loss for simplicity due to 4 weeks rule. We make 500 gain on older
		// shares and we count it for taxes.
		assertEquals(500 * Transaction.euroToMil(1, 0), actualTR2023.getTaxableNetGains());
	}

	// Test that we work at a 1 share granularity, not at a transaction granularity.
	// Overall, transaction made a loss, but
	@Test
	public void testFourWeekRuleFIFOProfitLossGranularity() {
		Stock stockA = new Stock("Stock A", "IsinA");
		var txBuyA1 = new Transaction(true, LocalDateTime.of(2023, 1, 1, 12, 0), stockA, 1000,
				Transaction.euroToMil(5, 0), Transaction.euroToMil(5, 0), "ba1", 10);
		var txBuyA2 = new Transaction(true, LocalDateTime.of(2023, 1, 12, 12, 0), stockA, 1000,
				Transaction.euroToMil(3, 0), Transaction.euroToMil(3, 0), "ba2", 8);
		// sell within 4 weeks of buy.
		var txSellA1 = new Transaction(false, LocalDateTime.of(2023, 1, 21, 2, 0), stockA, 1500,
				Transaction.euroToMil(4, 0), Transaction.euroToMil(4, 0), "sa1", 7);

		TaxEngine te = new TaxEngine(List.of(txBuyA1, txBuyA2, txSellA1));
		te.calculateTaxFull();

		var actualTR2023 = te.reports.get(2023);
		assertNotNull(actualTR2023);
		// We make -1000.00 loss on sale of shares bought <4 weeks ago, but we disregard
		// this loss for simplicity due to 4 weeks rule. We make 500 gain on shares <4
		// weeks ago and we count it for taxes. But since we work at per share level we
		// add up all profits and disregard all losses.
		assertEquals(500 * Transaction.euroToMil(1, 0), actualTR2023.getTaxableNetGains());
	}
}
