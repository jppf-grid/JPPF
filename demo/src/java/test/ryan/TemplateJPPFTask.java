package test.ryan;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.jppf.server.protocol.*;

public class TemplateJPPFTask extends CommandLineTask
{

	/**
	 * 
	 */
	private String number;

	public TemplateJPPFTask(String number)
	{
		this.number = number;
	}

	public void run()
	{
		// ...
		try
		{
			System.out.println("Number: " + number);
			setCommandList("/bin/sh", "-c", "cat /etc/passwd|grep -s A > " + "/tmp/ryanrathsam/output-" + number + ".txt");

			this.setCaptureOutput(true);
			this.launchProcess();

			FileLocation fileLoc = new FileLocation("/tmp/ryanrathsam/output-" + number + ".txt");
			InputStream is = fileLoc.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);

			// Read in the File
			String line = "";
			List<String> file = new ArrayList<String>();
			List<String> bFile = new ArrayList<String>();
			while ((line = br.readLine()) != null)
			{
				file.add(line);
			}

			// "Process" the File
			for (int i = file.size() - 1; i >= 0; i--)
			{
				bFile.add(file.get(i));
			}

			// "Write" out the File
			for (String l : bFile)
			{
				System.out.println(l);
			}

			setResult("Succesfully Ran***\nOutput: " + getStandardOutput() + "\nError: " + getErrorOutput());
		}
		catch (InterruptedException e)
		{
			setException(e);
			System.out.println(e.getStackTrace().toString());
			return;
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			setException(e);
			System.out.println(e.getStackTrace().toString());
		}

		// eventually set the execution results
		//setResult("the execution was performed successfully");
	}

}
