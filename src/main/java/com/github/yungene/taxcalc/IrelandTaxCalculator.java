package com.github.yungene.taxcalc;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Entry point of our command-line program.
 * 
 * Here we use picocli for parsing command line arguments.
 */
@Command(name = "IrelandTaxCalculator", mixinStandardHelpOptions = true, version = "IrelandTaxCalculator 0.1", description = "IrelandTaxCalculator")
public class IrelandTaxCalculator implements Callable<Integer> {

	@Option(names = "--f", required = true, description = "Transactions.csv as exported from Degiro.")
	private File transactionsFile;

	@Override
	public Integer call() throws Exception {
		var txsParser = new TransactionsParser();
		List<Transaction> txs = txsParser.parseFile(this.transactionsFile);
		var taxEngine = new TaxEngine(txs);
		taxEngine.calculateTaxFull();
		for (var report : taxEngine.reports.values()) {
			System.out.println(report.prettyPrint());
		}
		return 0;
	}

	public static void main(String... args) {
		System.exit(new CommandLine(new IrelandTaxCalculator()).execute(args));
	}

}
