

# Absolutely NO WARRANTY. No guarantee of correctness. Please proof-check any answers and contact accountant if needed. This is a personal project shared for educational purposes.

# Work in progress

To build a JAR:
```
mvn package
```


Then run the shaded JAR, passing the path to file via `--f` flag. E.g.:
```
java -jar target/ireland-cgt-calculator-0.0.1-SNAPSHOT-shaded.jar --f="Transactions.csv"
```

# Tax Calculator


The goal of the calculator is to calculate capital gains tax (CGT) due for a specific year. The calculator is made to work with Degiro exports. Calculation is done for CGT in Ireland. 

Degiro provides two main documents: transactions statement and account statement. Transactions statement is easier to work with and provides enough information to calculate CGT. 

Features:
- Standard FIFO CGT
- 4 weeks rule for 28 days before + 1 day of sale + 28 after.
    - My own interpretation of the rules. See [Sources.md](/Sources.md) for my sources. And see tests for examples. 

Limitations:
- Does not handle stock splits yet. Need an example to add logic.
- Might lose a bit of precision. Stock price is calculated as value divided by quantity. Broker fees are evenly spread across all the stocks in a transaction. I try to round the values in such a way to benefit Revenue, not the user. This way, in the worst case we might overpay a few cents.
- Currency autofx fees not included, but I am not sure if they are to be included in fees for tax purposes.

TODO:
- Export results in CSV format for post processing.
- Add logger
    - Log details of every sale, what it was offset against
- Output current portfolio
- Snapshotting, incremental updates


