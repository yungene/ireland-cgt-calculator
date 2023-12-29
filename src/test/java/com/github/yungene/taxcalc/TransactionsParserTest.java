package com.github.yungene.taxcalc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class TransactionsParserTest {
	
	final static String csvHeader = "Date,Time,Product,ISIN,Reference,Venue,Quantity,Price,,Local value,,Value,,Exchange rate,Transaction and/or third,,Total,,Order ID";

	String newLineSep;
	
	@BeforeEach
	public void setUp() {
		this.newLineSep = System.getProperty("line.separator");
	}
	
	@Test
	public void testParseCsvSingleLineBuy() throws IOException {
		var tx1 = new Transaction(true, LocalDateTime.of(2022, 8, 8, 12, 0),
				new Stock("RYANAIR HOLDINGS PLC", "IE00BYTBXV33"), 20, 120600L, 121850L,
				"a28e99c3-4321-1234-88ec-b20fcbef968a", 0);
		var parser = new TransactionsParser();
		String singleLineInput = String.join(
				newLineSep,
				csvHeader,
				"08-08-2022,12:00,RYANAIR HOLDINGS PLC,IE00BYTBXV33,IRL,XMSM,20,12.0600,EUR,-241.20,EUR,-241.20,EUR,,-2.50,EUR,-243.70,EUR,a28e99c3-4321-1234-88ec-b20fcbef968a"
				);
		List<Transaction> txsOut = parser.parseCsv(new StringReader(singleLineInput));
		assertEquals(1, txsOut.size());
		assertEquals(tx1, txsOut.get(0));
	}
	
	@Test
	public void testParseCsvSingleLineBuyDollars() throws IOException {
		var tx1 = new Transaction(
				true,
				LocalDateTime.of(2023, 10, 8, 12, 12),
				new Stock("BERKSHIRE HATHAWAY INC", "US0846707026"),
				2,
				653_37_00L/2,
				655_37_00L/2,
				"500c21cf-4321-1234-88ec-b20fcbef968a",
				0
				);
		var parser = new TransactionsParser();
		String singleLineInput = String.join(
				newLineSep,
				csvHeader,
				"08-10-2023,12:12,BERKSHIRE HATHAWAY INC,US0846707026,NSY,SOHO,2,349.5500,USD,-699.10,USD,-653.37,EUR,1.0700,-2.00,EUR,-655.37,EUR,500c21cf-4321-1234-88ec-b20fcbef968a"
				);
		List<Transaction> txsOut = parser.parseCsv(new StringReader(singleLineInput));
		assertEquals(1, txsOut.size());
		assertEquals(tx1, txsOut.get(0));
	}

	@Test
	public void testParseCsvSingleLineSellGBX() throws IOException {
		var tx1 = new Transaction(
				false,
				LocalDateTime.of(2023, 4, 11, 14, 5),
				new Stock("F&C INVESTMENT TRUST PLC", "GB0003466074"),
				70,
				(long)Math.ceil(733_63_00L/70),
				(long)Math.ceil(728_73_00L/70),
				"f0eda334-4321-1234-88ec-b20fcbef968a",
				0
				);
		var parser = new TransactionsParser();
		String singleLineInput = String.join(
				newLineSep,
				csvHeader,
				"11-04-2023,14:05,F&C INVESTMENT TRUST PLC,GB0003466074,LSE,MESI,-70,899.0000,GBX,62930.00,GBX,733.63,EUR,85.7795,-4.90,EUR,728.73,EUR,f0eda334-4321-1234-88ec-b20fcbef968a"
				);
		List<Transaction> txsOut = parser.parseCsv(new StringReader(singleLineInput));
		assertEquals(1, txsOut.size());
		assertEquals(tx1, txsOut.get(0));
	}
	
	@Test
	public void testParseCsvSingleLineBuyNoFees() throws IOException {
		var tx1 = new Transaction(true, LocalDateTime.of(2022, 8, 8, 12, 0),
				new Stock("RYANAIR HOLDINGS PLC", "IE00BYTBXV33"), 20, 120600L, 120600L,
				"a28e99c3-4321-1234-88ec-b20fcbef968a", 0);
		var parser = new TransactionsParser();
		String singleLineInput = String.join(
				newLineSep,
				csvHeader,
				"08-08-2022,12:00,RYANAIR HOLDINGS PLC,IE00BYTBXV33,IRL,XMSM,20,12.0600,EUR,-241.20,EUR,-241.20,EUR,,,EUR,-241.20,EUR,a28e99c3-4321-1234-88ec-b20fcbef968a"
				);
		List<Transaction> txsOut = parser.parseCsv(new StringReader(singleLineInput));
		assertEquals(1, txsOut.size());
		assertEquals(tx1, txsOut.get(0));
	}
	
	@Test
	public void testParseCsvMultiLine() throws IOException {
		var tx1 = new Transaction(
				true,
				LocalDateTime.of(2023, 10, 8, 12, 12),
				new Stock("BERKSHIRE HATHAWAY INC", "US0846707026"),
				2,
				653_37_00L/2,
				655_37_00L/2,
				"500c21cf-4321-1234-88ec-b20fcbef968a",
				0
				);
		var tx2 = new Transaction(
				false,
				LocalDateTime.of(2023, 4, 11, 14, 5),
				new Stock("F&C INVESTMENT TRUST PLC", "GB0003466074"),
				70,
				(long)Math.ceil(733_63_00L/70),
				(long)Math.ceil(728_73_00L/70),
				"f0eda334-4321-1234-88ec-b20fcbef968a",
				1
				);
		var tx3 = new Transaction(
				true,
				LocalDateTime.of(2022, 8, 8, 12, 0),
				new Stock("RYANAIR HOLDINGS PLC", "IE00BYTBXV33"),
				20,
				120600L,
				121850L,
				"a28e99c3-4321-1234-88ec-b20fcbef968a",
				2
				);
		List<Transaction> expectedTxs = Arrays.asList(tx1, tx2, tx3);
		var parser = new TransactionsParser();
		String multiLineInput = String.join(
				newLineSep,
				csvHeader,
				"08-10-2023,12:12,BERKSHIRE HATHAWAY INC,US0846707026,NSY,SOHO,2,349.5500,USD,-699.10,USD,-653.37,EUR,1.0700,-2.00,EUR,-655.37,EUR,500c21cf-4321-1234-88ec-b20fcbef968a",
				"11-04-2023,14:05,F&C INVESTMENT TRUST PLC,GB0003466074,LSE,MESI,-70,899.0000,GBX,62930.00,GBX,733.63,EUR,85.7795,-4.90,EUR,728.73,EUR,f0eda334-4321-1234-88ec-b20fcbef968a",
				"08-08-2022,12:00,RYANAIR HOLDINGS PLC,IE00BYTBXV33,IRL,XMSM,20,12.0600,EUR,-241.20,EUR,-241.20,EUR,,-2.50,EUR,-243.70,EUR,a28e99c3-4321-1234-88ec-b20fcbef968a"
				);
		List<Transaction> txsOut = parser.parseCsv(new StringReader(multiLineInput));
		assertEquals(3, txsOut.size());
		assertIterableEquals(expectedTxs, txsOut);
	}
}
