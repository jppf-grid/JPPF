package test.hazelcast;

import java.util.Map;

import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;

import com.hazelcast.core.Hazelcast;

public class Runner
{
	/**
	 * JPPF client used to submit execution requests.
	 */
	private static JPPFClient jppfClient = null;

	public static void main(String[] args)
	{
		try
		{
			jppfClient = new JPPFClient();
			int nTask = 2;

			System.out.println("getting distributed map");
			Map<Object, Object> map = Hazelcast.getMap("myobjects");
			System.out.println("adding pojos to the map");
			for (int i = 0; i < nTask; i++) map.put(i, new TestObject("id_" + i));
			System.out.println("waiting 2s ...");
			Thread.sleep(2000L);

			// perform tasks
			JPPFJob job = new JPPFJob();
			for (int i = 0; i < nTask; i++) job.addTask(new Task(i));

			System.out.println("submitting jppf job");
			jppfClient.submit(job);
			System.out.println("print results");
			for (int i = 0; i < nTask; i++) System.out.println("after task " + map.get(i));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			jppfClient.close();
		}
		System.exit(0);
	}
}
