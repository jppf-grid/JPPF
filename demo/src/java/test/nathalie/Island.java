package test.nathalie;

import org.jppf.server.protocol.JPPFTask;

public class Island extends JPPFTask
{
	int c = 0;

	public Island(final int c)
	{
		this.c = c;
	}

	@Override
	public void run()
	{
		new Intercal().intercal(c);
		setResult("I am the island... " + c);
	}
}
