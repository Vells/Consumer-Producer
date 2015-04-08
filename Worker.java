/**
 * Vela Dimitrova Mineva
 * Consumer-Producer Problem
 * Date: 03/23/2015
 */

import java.util.concurrent.Semaphore;


public class Worker implements Runnable {

	// Instance data
	private int average;

	// Controls the access to the data that has to be updated
	private static Semaphore updateData = new Semaphore(1);

	protected static long totalQueueTime = 0;
	protected static long totalTotalTime = 0;

	// Constructor
	public Worker(int srate, int numrequests) {
		this.average = Coordinator.getAverageTime(srate);
	}

	/**
	 * A method that consumes a request, i.e. finds the queue time and
	 * the total time for a request and then updates the total sums
	 * @param long timeStamp - time when request is placed on the queue
	 * @param long currentTime - time when a request is taken off the queue
	 * @param long completionTime - completion time for a request
	 * @return none
	 */
	private void consumeRequest(long timeStamp, long currentTime, long completionTime) {
		// Acquire the semaphore that controls the
		// access to updating data
		try {
			updateData.acquire();
		} catch (InterruptedException ie) {
			System.out.println(ie.getStackTrace());
			System.exit(1);
		}

		// Update Data
		long queueTime = currentTime - (long) timeStamp;
		totalQueueTime += queueTime;
		long totalTime = completionTime - timeStamp;
		totalTotalTime += totalTime;

		updateData.release();
	}

	/**
	 * A method that takes requests off the queue and consumes it
	 * @param None
	 * @return None 
	 * 
	 */
	public void run() {

		while(true) {

			// Acquire the semaphore that controls 
			// the number of requests in the queue
			try {
				Coordinator.fillCount.acquire();
			} catch (InterruptedException ie) {
				System.err.println(ie.getStackTrace());
				System.exit(1);
			}

			// Acquire the semaphore that controls 
			// the access to the critical section
			try {
				Coordinator.mutex.acquire();
			} catch (InterruptedException ie) {
				System.out.println(ie.getStackTrace());
				System.exit(1);
			}

			Long timeStamp = Coordinator.sharedQueue.poll();
			long currentTime = System.nanoTime();

			Coordinator.mutex.release();
			// Release a semaphore if the queue's size is unlimited
			if (!Coordinator.unlimited)
				Coordinator.emptyCount.release();
			
			// If the removed request is null, terminate the thread
			if (timeStamp < 0) {
				break;
			}
		
			long completionTime = Coordinator.sleep(this.average);
			consumeRequest((long) timeStamp, currentTime, completionTime);
		}
	}

	/**
	 * A method that calculates and returns the average queue time
	 * @param None
	 * @param long - average queue time in microseconds
	 */
	public static long getAverageQueueTime() {
		return totalQueueTime / ((long) 1000 * Coordinator.numrequests);
	}

	/**
	 * A method that calculates and returns the average total time
	 * @param None
	 * @param long - average total time in microseconds
	 */
	public static long getAverageTotalTime() {
		return totalTotalTime / ((long) 1000 * Coordinator.numrequests);
	}

	/**
	 * A static method that prints the average queue time and the average total time
	 * @param None
	 * @param None 
	 */
	public static void printResults() {
		System.out.println("Results:");
		System.out.println("Average request queue time = " + getAverageQueueTime() + " us");
		System.out.println("Average request total time = " + getAverageTotalTime() + " us");
	}

}
