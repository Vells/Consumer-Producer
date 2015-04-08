/**
 * Vela Dimitrova Mineva
 * Consumer-Producer Problem
 * Date: 03/23/2015
 */


public class Producer implements Runnable {

	// Instance Data
	private int average;
	private int numrequests;
	private int numworkers;

	// Constructor
	public Producer(int arate, int numrequests, int numworkers) {
		this.average = Coordinator.getAverageTime(arate);
		this.numrequests = numrequests;
		this.numworkers = numworkers;
	}

	/**
	 * A method that produces a requests and places it on
	 * the shared queue.
	 * @param None
	 * @param None
	 */
	public void run() {
		
		int produced = 0;	// Keep track of the number of produced requests
		while (produced < this.numrequests) {		
			
			long request = Coordinator.sleep(this.average);;
			produced += 1;

			// Acquire the semaphore that controls the number of empty 
			// spaces in the queue if the queue's size is unlimited
			if (!Coordinator.unlimited) {
				try {
					Coordinator.emptyCount.acquire();
				} catch (InterruptedException ie) {
					System.out.println(ie.getStackTrace());
					System.exit(1);
				}
			}

			// Acquire the semaphore that controls 
			// the access to the critical section
			try {
				Coordinator.mutex.acquire();
			} catch (InterruptedException ie) {
				System.out.println(ie.getStackTrace());
				System.exit(1);
			}

			// Critical section
			Coordinator.sharedQueue.addLast(request);
			
			// Release acquired semaphores
			Coordinator.mutex.release();
			Coordinator.fillCount.release();
		}

		int nullRequests = 0;	// Keep track of the number of null requests
		while(nullRequests < numworkers) {

			// Acquire the semaphore that controls the number of empty
			// spaces in the queue if the queue's size is unlimited.
			if (!Coordinator.unlimited) {
				try {
					Coordinator.emptyCount.acquire();
				} catch (InterruptedException ie) {
					System.err.println(ie.getStackTrace());
					System.exit(1);
				}
			}

			// Acquire the semaphore that controls 
			// the access to the critical section
			try {
				Coordinator.mutex.acquire();
			} catch (InterruptedException ie) {
				System.err.println(ie.getStackTrace());
				System.exit(1);
			}

			Coordinator.sharedQueue.addLast(new Long(-1));
			nullRequests += 1;

			// Release acquired semaphores
			Coordinator.mutex.release();
			Coordinator.fillCount.release();
		}

	}

}
