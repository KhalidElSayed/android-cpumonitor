package com.tomdignan.UltimateResourceMonitor;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Reads CPU time statistics from /proc/stat. The time elapsed between
 * readings is left up to the caller.
 * 
 * TODO: Refactor into two classes, URMCpuStatReader and URMCpuUsageMonitor
 * 
 * @author Tom Dignan
 */
public class URMCPUStatReader {
	@SuppressWarnings("unused")
	private static final String TAG = "URMCPUStatReader";
	
	/** Used to mark CPUs in the results array as shutdown. */
	private static final float CPU_IS_ASLEEP = -1;
	
	/////////////////////////////////////////////////////////////////////////////////////////////
	//  THE INDEX VALUES ARE FOR THE INDEX IN THE PARSED STRING, NOT IN THE Long[] JIFFIES ARRAY!!!
	//	FOR THE Long[] ARRAY, SUBTRACT 1
	/////////////////////////////////////////////////////////////////////////////////////////////

	/** The time spent in USER processes in jiffies */
	private static final int INDEX_USER = 0;
	
	/** The time spent in niced processes in jiffies */
	private static final int INDEX_NICE = 1;
	
	/** The time spent in system processes in jiffies */
	private static final int INDEX_SYS = 2;
	
	/** The IDLE in jiffies (in the data after splitting off "CPU*" */
	private static final int INDEX_IDLE = 3;
	
	/** the time spent waiting for I/O to complete */
	private static final int INDEX_IOWAIT = 4;
	
	/** The time spent servicing interrupts */
	private static final int INDEX_IRQ = 5;
	
	/** The time spent servicing softirqs */
	private static final int INDEX_SOFTIRQ = 6;
	
	/** Support 8 cores max for now. */
	private static final int MAX_CPUS = 8;
	
	/** Path to the /proc/stat file */
	private static final String PROC_STAT_PATH = "/proc/stat";
	
	/** List of all possible CPUs that could appear in /proc/stat */
	private ArrayList<String> mCPUNames = new ArrayList<String>(MAX_CPUS);
	
	/** Total number of results seen so far */
	private int mTotalCPUNames;
	
	/** File reader for /proc/stat */
	RandomAccessFile mStatFileReader;

	/** 
	 * Used for buffering the /proc/stat file
	 */
	private byte[] mBuffer = new byte[8192];
	
	/** We will rely on this because it's fast, but it does make the code more
	 prone to breaking of the format of the /proc/stat file ever changes. */
	private static final int FIRST_STAT_OFFSET = 5;
	
	/** Just store the powers of 10 so they never have to be computed. */
	private static final long[] POWERS_10 = { 
		  1, 	
		  10, 
		  100, 
		  1000, 
		  10000, 
		  100000, 
		  1000000, 
		  10000000, 
		  100000000,
		  1000000000
	};

	/** No reason not to re-use this. Each call to getUsage() will swap this with mLastReading. */
	private HashMap<String,Long[]> mReading = null;
	
	
	/** Holder for the last reading, used to calculate usage over time. Each call to getUsage
	 * will swap this with mReading. */
	private HashMap<String,Long[]> mLastReading = null;
	
	/** Swap any objects */
	private static void swap(Object a, Object b) {
		Object tmp = a;
		a = b;
		b = tmp;
	}
	
	/**
	 * Constructs a new CPU stat reader. Initializes the time in MS and 
	 * takes a first reading for use in the first call to getUsage().
	 * 
	 * @throws FileNotFoundException
	 */
	public URMCPUStatReader() throws FileNotFoundException {
		initCPUs();
		File file = new File(PROC_STAT_PATH);
		mStatFileReader = new RandomAccessFile(file, "r");
	}
	
	
	/** Used for providing a mock /proc/stat for testing 
	 * @throws FileNotFoundException */
	public URMCPUStatReader(String mockPath) throws FileNotFoundException {
		initCPUs();
		File file = new File(mockPath);
		mStatFileReader = new RandomAccessFile(file, "r");
	}
	
	private void initCPUs() {
		// All systems will have these names. This will save a few iterations.
		mTotalCPUNames = 2;
		mCPUNames.add("cpu");
		mCPUNames.add("cpu0");
	}
	
	/** 
	 * Must be called before getUsage()! With this class, you are responsible for
	 * keeping your own time too, so call it the same amount of time before getUsage() 
	 * that you use between each getUsage() call!
	 */
	public void initializeReading() {
		mLastReading = getReading(mLastReading);
	}

	/**
	 * Returns an array of int representing the usage of each core. The first
	 * element is the aggregate of all cores.
	 * 
	 * The format of the return value is as noted below. Please do not attempt to read values
	 * past #results + 1, because it's not valid data if it is even there. Sometimes, the
	 * previous reading has a cpu that the current reading does not, or vice versa. This 
	 * is because sometimes the extra cores go to sleep when they are not in use. If this happens,
	 * that core will have a -1 in its position in the results array.
	 * 
	 * @param float[] resultHolder - If null, a new float[] resultHolder will be created
	 * and returned. On subsequent calls, you should pass this back in in order to
	 * reuse it. 
	 * 
	 * This m __must be synchronized__ as interleaving calls to getUsage 
	 * reuses the same internal HashMap for storing each reading.
	 * 
	 * @return float[] result [<#results>, <cpu>, <cpu0>, ..., <cpuN>]  
	 */
	public synchronized float[] getUsage(float[] resultHolder) {
		mReading = getReading(mReading);
		
		float[] results;

		// Save on allocation.
		if (resultHolder == null) {
			// The +1 is for the extra element we use to store the number
			// of results [0]...
			results = new float[MAX_CPUS + 1];
		} else {
			results = resultHolder;
		}
		
		// The first element will always be the total number of results.
		results[0] = mTotalCPUNames;
		
		// Calculate usage for each CPU statistic. If one is not there,
		// mark it with CPU_IS_ASLEEP (-1)
		for (int i = 0; i < mTotalCPUNames; i++) {
			String cpuName = mCPUNames.get(i);

			
			Long[] lastJiffies = mLastReading.get(cpuName);
			Long[] jiffies = mReading.get(cpuName);
			
			if (lastJiffies == null || jiffies == null) {
				results[i + 1] = CPU_IS_ASLEEP;
			} else {
				results[i + 1] = computeUsage(lastJiffies, jiffies);
			}
		}
		
		// Swap the current reading and the last reading, to reuse HashMaps
		swap(mLastReading, mReading);
		
		return results;
	}
	
	
	/**
	 * Computes usage given two cpu readings. The time elapsed between is left 
	 * up to the caller and he is responsible for that information.
	 * 
	 * returns usage as an integer 0-99 representing the percent of non-idle
	 * cputime used.
	 * 
	 * @param cpu
	 * @return int percent of time spent doing work.
	 */
	private float computeUsage(Long[] reading1, Long[] reading2) {
		long idle1 = reading1[INDEX_IDLE];
		//Log.d(TAG, "idle1 = " + String.valueOf(idle1));
		
		long idle2 = reading2[INDEX_IDLE];
		//Log.d(TAG, "idle2 = " + String.valueOf(idle2));
		

		long total1 = 0, total2 = 0;
		
		for (int i = 0; i <= INDEX_SOFTIRQ; i++) {
			total1 += reading1[i];
			total2 += reading2[i];
		}
		
		//Log.d(TAG, "total1 = " + String.valueOf(total1));
		//Log.d(TAG, "total2 = " + String.valueOf(total2));

		float idleDiff = idle2  - idle1;
		//Log.d(TAG, "idleDiff = " + String.valueOf(idleDiff));
		
		float totalDiff = total2 - total1;
		//Log.d(TAG, "totalDiff = " + String.valueOf(totalDiff));
		
		// the percent of non-idle time.
		float percentWork = (totalDiff - idleDiff) / totalDiff;
		//Log.d(TAG, "precentWork = " + String.valueOf(percentWork));
		
		return percentWork;
	}
	
	/**
	 * Update the list of available CPU statistics. Must be done whenever
	 * the total number of available CPU statistics increases.
	 */
	private void updateCPUList(HashMap<String,Long[]> reading) {
		//Start at the difference as to only add new names.
		int readingSize = reading.size();
		for (int i = mTotalCPUNames + 1; i <= readingSize; i++) {
			if (i == 0) {
				mCPUNames.add("cpu");
			} else {
				String cpu = "cpu" + String.valueOf(i - 2);
				mCPUNames.add(cpu);
			}
		}
		mTotalCPUNames = readingSize;
	}
	
	/**
	 * Get the CPU identifier for the byte after the 'cpu' prefix.
	 * 
	 * The function is written this way for speed, not for 
	 * conciseness or expressiveness. This avoids unnecessary concats.
	 * 
	 * @param b
	 * @return
	 */
	public static String getCpuName(byte b) {
		switch(b) {
		case ' ':
			return "cpu";
		case '0':
			return "cpu0";
		case '1': 
			return "cpu1";
		case '2':
			return "cpu2";
		case '3':
			return "cpu3";
		case '4':
			return "cpu4";
		case '5':
			return "cpu5";
		case '6':
			return "cpu6";
		case '7':
			return "cpu7";
			
		default:
			throw new IllegalArgumentException("Unknown CPU number");
		}
	}
	
	

	/**
	 * Reads the current CPU stats from /proc/stat and returns the results
	 * as a HashMap where the key is the identifier, i.e. "CPU" and the jiffies
	 * are an array of Long.
	 * 
	 * This has the side effect of updating the list of CPUs that this object
	 * stores internally.
	 * 
	 * @param If null, new storage for the reading will be created. The reading
	 * HashMap will always be expanded as necessary, but will first attempt to
	 * reuse old Long[] arrays from the last call.
	 * 
	 * @return
	 */
	public HashMap<String,Long[]> getReading(HashMap<String, Long[]> reading) {
		// Allocate for the user if he doesn't supply one. It is fine to pass
		// this one back in.
		if (reading == null) {
			reading = new HashMap<String, Long[]>(MAX_CPUS);
		}
		
		long startTime = System.currentTimeMillis();
		//Log.d(TAG, "getReading() start time (ms): " + String.valueOf(startTime));
		try {
			mStatFileReader.seek(0);
			int bytesRead = mStatFileReader.read(mBuffer);
			
			String cpuName = null;
			int i = -1;
			Long[] values = null;
			long value = 0;
			int valueIndex = 0;
			
			while (i++ < bytesRead) {
				
				// Check for ^cpu. lines
				if (mBuffer[i] == 'c' && mBuffer[i + 1] == 'p' && 
						mBuffer[i + 2] == 'u') {
				
					// Get appropriate CPU name.
					cpuName = getCpuName(mBuffer[i + 3]);
					
					// Initialize values
					values = reading.get(cpuName);
					if (values == null) {
						values = new Long[10];
					}
					valueIndex = 0;
					
					i += FIRST_STAT_OFFSET;
				}
			
				
				if (mBuffer[i] >= '0' && mBuffer[i] <= '9') {
					int j = i;
					
					// Find the end of the number. Don't run over newlines.
					while (mBuffer[j] != ' ' && mBuffer[j] != '\n') j++; j--;
					//System.out.println("mBuffer[j]=" + String.valueOf(mBuffer[j]));		
					
					int power = j - i;
					//System.out.println("Power="+power);
					
					// Walk i towards j and calculate the value
					while (i <= j) {
						long placeValue = (mBuffer[i++] - '0') * POWERS_10[power--];
						//System.out.println("placeValue=" + placeValue);
						value += placeValue;
					}
				}
			
				if (valueIndex < INDEX_SOFTIRQ) {
					values[valueIndex++] = value;
					value = 0;
				} else if (valueIndex == INDEX_SOFTIRQ) {
					values[valueIndex++] = value;
					value = 0;
					reading.put(cpuName, values);
				}
			}
				
			//System.out.print(new String(mBuffer));
			//System.out.println("breakpoint");
	
			
			if (reading.size() > mTotalCPUNames) {
				updateCPUList(reading);
			}
			
			return reading;
			
		} catch (IOException e) {
			System.out.println(TAG + " getReading(): caught IOException " + e.getMessage());
			return null;
		}
	}
	
	/**
	 * Call this when you are done using the stat reader!
	 */
	public void close() {
		try {
			mStatFileReader.close();
		} catch (IOException e) {
			//Log.e(TAG, "close(): caught IOException " + e.getMessage());
		}
	}
}
