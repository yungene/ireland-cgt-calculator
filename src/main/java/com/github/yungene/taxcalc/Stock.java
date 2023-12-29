package com.github.yungene.taxcalc;

import java.util.Objects;

class Stock {
	private final String name;
	private final String isin;
	
	public Stock(String name, String isin) {
		super();
		this.name = name;
		this.isin = isin;
	}

	@Override
	public int hashCode() {
		return Objects.hash(isin, name);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Stock other = (Stock) obj;
		return Objects.equals(isin, other.isin) && Objects.equals(name, other.name);
	}

	public String getName() {
		return name;
	}

	public String getIsin() {
		return isin;
	}

	@Override
	public String toString() {
		return "Stock [name=" + name + ", isin=" + isin + "]";
	}
}
