/**
 * Vela Dimitrova Mineva
 * Consumer-Producer Problem
 * Date: 03/23/2015
 */

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.Semaphore;


public class Coordinator {

	//Shared data
	protected static LinkedList<Long> sharedQueue;
	protected static boolean unlimited = false;
	protected static Semaphore mutex;
	protected static Semaphore emptyCount;
	protected static Semaphore fillCount;

	private static Random randomizer = new Random();
	private static HashMap<String, int[]> options;
	
	// Default configuration parameters
	private static int numworkers = 1;
	private static int qsize = 10;
	protected static int numrequests = 100;
	private static int arate = 10;
	private static int srate = 10;

	// Static Methods 
	
	/**
	 * A method that returns the average time
	 * based on a given rate
	 * @param int rate (requests per second)
	 * @return int (in milliseconds)
	 */
	public static int getAverageTime(int rate) {
		return (int) (1.0 / rate * 1000);
	}

	/**
	 * A method that produces a random number between 0 and twice
	 * the given average (in milliseconds)
	 * @param int avg - average time (in milliseconds)
	 * @return int - a random integer i, 0 <= i < 2 * avg 
	 */
	public static int uniform(int avg) {
		int range = 2 * avg;
		return randomizer.nextInt(range);
	}

	/**
	 * A method that puts a thread to sleep and returns 
	 * the time it wakes up
	 * @param int averageTime - average time in milliseconds
	 * @return long completionTime
	 */
	public static long sleep(int averageTime) {
		int randomValue = Coordinator.uniform(averageTime);
		try {
			Thread.sleep(randomValue);
		} catch (InterruptedException ie) {
			System.out.println(ie.getStackTrace());
			System.exit(1);
		}
		long completionTime = System.nanoTime();
		return completionTime;
	}
	
	/**
	 * A method that checks the range of a parameter value provided by the user
	 * and returns a boolean value indicating its validity
	 * @param String argument - given parameter
	 * @param int input - parsed parameter value
	 * @return boolean
	 */
	public static boolean isValidRange(String argument, int input) {
		int min = options.get(argument)[0];
		int max = options.get(argument)[1];
		if (input < min) {
			System.out.printf("Parameter %s is below minimum value of %d.\n", argument, min);
			return false;
		}
		else if (input > max) {
			System.out.printf("Parameter %s exceeds maximum value of %d.\n", argument, max);
			return false;
		}
		return true;
	}

	/**
	 * A method that updates a parameter with the given value provided 
	 * the value is in the specified range; otherwise the program is terminated.
	 * @param String argument - given parameter
	 * @param int input - parsed parameter value
	 * @return None
	 */
	public static void updateArgument(String argument, int input) {
		if (argument.equals("qsize") && input == -1) {
			qsize = -1;
			return;
		}
		
		if (!isValidRange(argument, input)) {
			System.exit(0);
		}
		
		switch(argument) {
		case "numworkers":
			numworkers = input;
			break;
		case "qsize":
			qsize = input;
			break;
		case "numrequests":
			numrequests = input;
			break;
		case "arate":
			arate = input;
			break;
		case "srate":
			srate = input;
			break;
		default:
			printInvalidArgError(argument);
			System.exit(0);
		}
	}
	
	/**
	 * A method that prints an error if the format of
	 * the entered parameters is invalid.
	 * @param: String arg
	 * @return None 
	 */
	public static void printInvalidArgError(String arg) {
		System.out.println("Invalid argument: " + arg);
		System.out.println("Usage: java Assignment2 [numworkers=INT]"
				+ " [qsize=INT] [numrequests=INT] [arate=INT] [srate=INT]");
		System.out.println("Note: INT is an integer.");
		System.out.println("Goodbye! ~");
	}
	
	/**
	 * A method that parses an argument provided by the user and
	 * calls the appropriate method to update the parameter value
	 * @param String arg
	 * @return None
	 */
	public static void parseArg(String arg) {

		String[] param = arg.split("=");
		if (param.length != 2) {
			printInvalidArgError(arg);
			System.exit(0);
		}

		// Parse parameter value or exit if the type is not an integer
		int input = 0;
		try {
			input = Integer.parseInt(param[1]);
		}
		catch (NumberFormatException e) {
			printInvalidArgError(arg);
			System.exit(0);
		}
		
		updateArgument(param[0], input);		
	}
	
	/**
	 * A method that updates the configuration parameters if command-line
	 * arguments are provided and prints the final parameter values
	 * @param String[] args - command line input
	 * @return None
	 */
	public static void setSystemArguments(String[] args) {
		if (args.length > 0) {
			for(String arg: args) {
				parseArg(arg);
			}
		}
		// Print parameter values
		System.out.println("Parameter values: ");
		System.out.printf("numworkers=%d qsize=%d numrequests=%d arate=%d srate=%d\n",
				numworkers, qsize, numrequests, arate, srate);
	}
	
	/**
	 * A method that sets the valid ranges for the entered parameters 
	 * @param None
	 * @return None
	 */
	public static void setValidArgsRanges() {
		options = new HashMap<String, int[]>();
		options.put("numworkers", new int[] {1, 10});
		options.put("qsize", new int[] {1, 100});
		options.put("numrequests", new int[] {10, 1000000});
		options.put("arate", new int[] {1, 100});
		options.put("srate", new int[] {1, 100});
	}
	
	
	public static void main(String[] args) {

		// Set valid ranges for parameters
		setValidArgsRanges();
		// Set system arguments
		setSystemArguments(args);

		// Queue of requests that is shared by the producer and the worker threads
		sharedQueue = new LinkedList<Long>();		
		// Controls the access to critical section - all thread need access to this one
		mutex = new Semaphore(1);
		// Controls the number of available spaces in the queue
		emptyCount = new Semaphore(qsize); 
		// Controls the number of items already in the queue and ready to be consumed
		fillCount = new Semaphore(0);
		
		if (qsize == -1)
			unlimited = true;

		// Create a producer - produces until there are available spaces in the queue
		Thread producer = new Thread(new Producer(arate, numrequests, numworkers));
		// Start the threads
		producer.start();

		// Create workers, place them in an array and start the 
		Thread[] workers = new Thread[numworkers];
		for (int i = 0; i < numworkers; i++) {
			Thread worker = new Thread(new Worker(srate, numrequests));
			workers[i] = worker;
			worker.start();
		}

		// Wait until both threads are done before quitting this program
		try {
			producer.join();
			for(Thread worker: workers) {
				worker.join();
			}
		} catch (InterruptedException ie) {
			System.err.println(ie.getStackTrace());
			System.exit(1);
		}

		// Print out results
		Worker.printResults();
	}
}
