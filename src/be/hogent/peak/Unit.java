package be.hogent.peak;


public class Unit implements Comparable<Unit>
{

	public int getPlace()
	{
		return place;
	}

	public double getValue()
	{
		return value;
	}

	@Override
	public int compareTo(Unit o)
	{
		return o.value.compareTo(value);      
	}

	public Unit(Double value, int place)
	{
		this.value = value;
		this.place = place;
	}

	private Double value;
	private int place;

}
