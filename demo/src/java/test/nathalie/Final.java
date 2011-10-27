package test.nathalie;

import org.jppf.server.protocol.JPPFTask;

public class Final extends JPPFTask
{
	int res = 0;
	int c = 0;

	public Final(final int c)
	{
		this.c = c;
	}

	@Override
	public void run()
	{
		System.out.println("in Final(" + c + ')');
		res = (int) (c * Math.sin(c + 10));
		setResult("Result... " + res);
	}
}
