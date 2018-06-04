package utils;

public class Pair
{

	private String first;
	private Integer second;

	/**
	 * Creates a new pair with two elements of different or same classes
	 * 
	 * @param first
	 *            - First object
	 * @param second
	 *            - Second object
	 */
	public Pair(String first, Integer second)
	{
		this.first = first;
		this.second = second;
	}

	/**
	 * @return Returns the first object in the pair
	 */
	public String getFirst()
	{
		return first;
	}

	/**
	 * @return Returns the second object in the pair
	 */
	public Integer getSecond()
	{
		return second;
	}

	/**
	 * Sets the first object in the pair
	 * 
	 * @param first
	 *            - New first element of the pair
	 */
	public void setFirst(String first)
	{
		this.first = first;
	}

	/**
	 * Sets the second object in the pair
	 * 
	 * @param second
	 *            - New second element of the pair
	 */
	public void setSecond(Integer second)
	{
		this.second = second;
	}
	
	@Override
	public String toString() {
		return first + " " + second;
	}

}