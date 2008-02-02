package test.crash;

import java.util.*;
import java.util.concurrent.*;

public class StandaloneMatrixRunner
{
	private static ExecutorService threadPool = Executors.newFixedThreadPool(4);

	public static void main(String...args)
	{
		try
		{
			int size = 1000;
			int iterations = 1000;
			System.out.println("Matrix size = "+size+", "+iterations+" iterations");
			Matrix a = new Matrix(size);
			Matrix b = new Matrix(size);
			long totalTime = 0L;
			for (int iter=0; iter<iterations; iter++)
			{
				long start = System.currentTimeMillis();
				List<ComputeTask> tasks = new ArrayList<ComputeTask>();
				for (int i=0; i<size; i++)
				{
					ComputeTask task = new ComputeTask(a.getRow(i), b);
					tasks.add(task);
				}
				List<Future<?>> futureList = new ArrayList<Future<?>>(tasks.size());
				for (ComputeTask task : tasks)
				{
					futureList.add(threadPool.submit(task));
				}
				for (Future<?> future : futureList) future.get();
				long elapsed = System.currentTimeMillis() - start;
				totalTime += elapsed;
				System.out.println("Iteration #"+(iter+1)+" done in "+elapsed+" ms");
			}
			long avg = totalTime / iterations;
			System.out.println("Average iteration time: " + avg + " ms");
			System.exit(0);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static class ComputeTask implements Runnable
	{
		private Matrix b = null;
		private double[] result = null;
		private double[] rowValues = null;

		public ComputeTask(double[] rowValues, Matrix b)
		{
			this.rowValues = rowValues;
			this.b = b;
		}

		public void run()
		{
			int size = rowValues.length;
			result = new double[size];
			for (int col=0; col<size; col++)
			{
				double sum = 0d;
				for (int row=0; row<size; row++)
				{
					sum += b.getValueAt(row, col) * rowValues[row];
				}
				result[col] = sum;
			}
		}
	}

	public static class Matrix
	{
		private static final double RANDOM_RANGE = 1e6d;
		private int size = 0;
		private double[][] values = null;

		public Matrix(int newSize)
		{
			this.size = newSize;
			values = new double[size][size];
			Random rand = new Random(System.currentTimeMillis());
			for (int i=0; i<values.length; i++)
			{
				for (int j=0; j<values[i].length; j++)
					values[i][j] = RANDOM_RANGE * (2d * rand.nextDouble() - 1d);
			}
		}

		public double[] getRow(int row)
		{
			return (row < size) ? values[row] : null;
		}

		public double getValueAt(int row, int column)
		{
			return values[row][column];
		}
	}
}
