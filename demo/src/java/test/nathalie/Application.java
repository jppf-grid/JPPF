package test.nathalie;

public class Application
{
	public static void main(String...args)
	{
		try
		{
			new Beginning().begin();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
