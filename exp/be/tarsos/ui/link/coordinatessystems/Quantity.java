package be.tarsos.ui.link.coordinatessystems;

import java.text.DecimalFormat;

public enum Quantity {
	TIME("Time", 0, 10000),
	FREQUENCY("Frequency", 200, 9000),
	AMPLITUDE("Amplitude", -1000, 1000),
	NONE("None", -1000, 1000);

	private String name;
	private int min;
	private int max;
	private Unit unit;
	private DecimalFormat nft;

	Quantity(String name, int min, int max) {
		this.name = name;
		this.min = min;
		this.max = max;
		nft = new DecimalFormat("#0.##");
		nft.setDecimalSeparatorAlwaysShown(false);
	}

	public int getMin() {
		return min;
	}

	public int getMax() {
		return max;
	}

	public String getQuantityName() {
		return name;
	}

	public String getFormattedString(double value) {
		return new String(nft.format(this.getUnit().getValueInUnits(value)) + " " + this.getUnit().getUnit());
	}

	public String getFormattedString(float value) {
		return new String(nft.format(this.getUnit().getValueInUnits(value)) + " " + this.getUnit().getUnit());
	}

	public String getFormattedString(int value) {
		return new String(this.getUnit().getValueInUnits(value) + " " + this.getUnit().getUnit());
	}

	public Unit getUnit() {
		return unit;
	}

	public void setUnit(Unit unit) {
		this.unit = unit;
	}
	
	public String getName(){
		return name;
	}
	
	public String toString(){
		if (!getUnit().getUnit().isEmpty()){
			return name + "[" + this.unit.getUnit() + "]";
		} else {
			return name;
		}
	}
}
