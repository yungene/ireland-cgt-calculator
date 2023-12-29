package com.github.yungene.taxcalc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class TransactionsParser {
	final static String COMMA_DELIMETER = ",";

	/**
	 * Ugly way of mapping columns to indices.
	 */
	static class DegiroCSVMapping {
		final static int DATE = 0;
		final static int TIME = 1;
		final static int PRODUCT_NAME = 2;
		final static int ISIN = 3;
		final static int QUANTITY = 6;
		final static int VALUE_EUR = 11;
		final static int FEES_EUR = 14;
		final static int TOTAL_VALUE_EUR = 16;
		final static int ORDER_ID = 18;
	}

	private int seqNum;

	TransactionsParser() {
		this.seqNum = 0;
	}

	/**
	 * Sequence number does not make much sense if this method is called multiple
	 * times.
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public List<Transaction> parseFile(File file) throws IOException {
		try (FileReader fr = new FileReader(file)) {
			return parseCsv(fr);
		}
	}

	List<Transaction> parseCsv(Reader in) throws IOException {
		List<Transaction> result = new ArrayList<Transaction>();
		try (BufferedReader br = new BufferedReader(in)) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] values = line.split(COMMA_DELIMETER);
				if (values.length >= 18) {
					if (values[0].equals("Date")) {
						// Skip the first title row if it exists
						continue;
					}

					int[] dateVals = Arrays.stream(values[DegiroCSVMapping.DATE].split("-")).mapToInt(Integer::parseInt)
							.toArray();
					int[] timeVals = Arrays.stream(values[DegiroCSVMapping.TIME].split(":")).mapToInt(Integer::parseInt)
							.toArray();
					LocalDateTime dt = LocalDateTime.of(dateVals[2], dateVals[1], dateVals[0], timeVals[0],
							timeVals[1]);
					String productName = values[DegiroCSVMapping.PRODUCT_NAME];
					String isin = values[DegiroCSVMapping.ISIN];
					Stock stock = new Stock(productName, isin);
					int quantity = Integer.parseInt(values[DegiroCSVMapping.QUANTITY]);
					boolean isBuy = quantity >= 0;
					long euroValue = decimalStringToLong(values[DegiroCSVMapping.VALUE_EUR]);
					/*
					 * String euroFeesString = values[DegiroCSVMapping.FEES_EUR]; if
					 * (euroFeesString.isBlank()) { euroFeesString = "0"; }
					 */
//					long euroFees = decimalStringToLong(euroFeesString);
//					if (euroFees > 0) {
//						throw new RuntimeException(String.format("Fees are > 0 for %s", line));
//					}
					long euroTotalValue = decimalStringToLong(values[DegiroCSVMapping.TOTAL_VALUE_EUR]);
					// We might be losing precision here
					// We don't have any other way since Degiro only provides price in local
					// currency, so we can only get it if we are dealing with EUR. Alternatively, we
					// can use exchange rate, but don't think it's necessarily better.
					long euroPrice = roundWorstCase(euroValue / quantity, isBuy);
					long euroTotalPrice = roundWorstCase(euroTotalValue / quantity, isBuy);
					String orderId = values[DegiroCSVMapping.ORDER_ID];
					Transaction transaction = new Transaction(isBuy, dt, stock, Math.abs(quantity), Math.abs(euroPrice),
							Math.abs(euroTotalPrice), orderId, seqNum++);
					result.add(transaction);
				}
			}
		}

		return result;
	}

	static long decimalStringToLong(String val) {
		return new BigDecimal(val).setScale(4, RoundingMode.HALF_UP).unscaledValue().longValueExact();
	}

	// Round down for buys, and round up for sells.
	// This way any error due to precision will benefit the Revenue.
	static long roundWorstCase(double val, boolean isBuy) {
		if (!isBuy) {
			return (long) Math.ceil(val);
		} else {
			return (long) Math.floor(val);
		}
	}
}
