package be.hogent.tarsos.ui.link.coordinatessystems;

public enum Units {
	TIME,
	FREQUENCY,
	AMPLITUDE,
	NONE;

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
}
