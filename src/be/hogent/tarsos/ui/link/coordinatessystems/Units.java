package be.hogent.tarsos.ui.link.coordinatessystems;

public enum Units {
	TIME("s", 1000),
	FREQUENCY("cents", 1),
	AMPLITUDE("", 1000),
	NONE("", 1000);
	
	private String unit;
	private double factor;

	private Units(String unit, double factor){
		this.unit = unit;
		this.factor = factor;
	}
	
	public static int getMin(Units u) {
		switch (u) {
		case TIME:
			return 0;
		case FREQUENCY:
			return 200;
		case AMPLITUDE:
			return -1000;
		case NONE:
			return -1000;
		}
		return 0;
	}

	public static int getMax(Units u) {
		switch (u) {
		case TIME:
			return 10000;
		case FREQUENCY:
			return 9000;
		case AMPLITUDE:
			return 1000;
		case NONE:
			return 1000;
		}
		return 0;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	public double getFactor() {
		return factor;
	}

	public void setFactor(double factor) {
		this.factor = factor;
	}
	
	
}
