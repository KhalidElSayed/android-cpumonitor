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
	private static final int INDEX_USER = 1;
	
	/** The time spent in niced processes in jiffies */
	private static final int INDEX_NICE = 2;
	
	/** The time spent in system processes in jiffies */
	private static final int INDEX_SYS = 3;
	
	/** The IDLE in jiffies (in the data after splitting off "CPU*" */
	private static final int INDEX_IDLE = 4;
	
	/** the time spent waiting for I/O to complete */
	private static final int INDEX_IOWAIT = 5;
	
	/** The time spent servicing interrupts */
	private static final int INDEX_IRQ = 6;
	
	/** The time spent servicing softirqs */
	private static final int INDEX_SOFTIRQ = 7;
	
	/** Support 8 cores max for now. */
	private static final int MAX_CPUS = 8;
	
	/** Path to the /proc/stat file */
	private static final String PROC_STAT_PATH = "/proc/stat";
	
	/** Holder for the last reading, used to calculate usage over time */
	private HashMap<String,Long[]> mLastReading = null;
	
	/** List of all possible CPUs that could appear in /proc/stat */
	private ArrayList<String> mCPUNames = new ArrayList<String>(MAX_CPUS);
	
	/** Total number of results seen so far */
	private int mTotalCPUNames;
	
	/** File reader for /proc/stat */
	RandomAccessFile mStatFileReader;

	/**
	 * Constructs a new CPU stat reader. Initializes the time in MS and 
	 * takes a first reading for use in the first call to getUsage().
	 * 
	 * @throws FileNotFoundException
	 */
	public URMCPUStatReader() throws FileNotFoundException {
		// All systems will have these names. This will save a few iterations.
		mTotalCPUNames = 2;
		mCPUNames.add("cpu");
		mCPUNames.add("cpu0");
		
		File file = new File(PROC_STAT_PATH);
		mStatFileReader = new RandomAccessFile(file, "r");
	}
	
	/** 
	 * Must be called before getUsage()! With this class, you are responsible for
	 * keeping your own time too, so call it the same amount of time before getUsage() 
	 * that you use between each getUsage() call!
	 */
	public void initializeReading() {
		mLastReading = getReading();
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
	 * @return float[] result [<#results>, <cpu>, <cpu0>, ..., <cpuN>]  
	 */
	public float[] getUsage(float[] resultHolder) {
		HashMap<String,Long[]> currentReading = getReading();
		long currentTimeMS = System.currentTimeMillis();
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

			
			Long[] oldJiffies = mLastReading.get(cpuName);
			Long[] newJiffies = currentReading.get(cpuName);
			
			if (oldJiffies == null || newJiffies == null) {
				
				results[i + 1] = CPU_IS_ASLEEP;
			} else {
				results[i + 1] = computeUsage(oldJiffies, newJiffies);
			}
		}
		
		// Save the new reading as the last reading
		mLastReading = currentReading;
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
		long idle1 = reading1[INDEX_IDLE - 1];
		//Log.d(TAG, "idle1 = " + String.valueOf(idle1));
		
		long idle2 = reading2[INDEX_IDLE - 1];
		//Log.d(TAG, "idle2 = " + String.valueOf(idle2));
		

		long total1 = 0, total2 = 0;
		for (int i = 0; i < reading1.length; i++) {
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
	 * Better to use this than have readLine() allocate way more lines than
	 * we will ever need.
	 */
	
	// 4096 is more bytes than we will EVER need for buffering the entire 
	// /proc/stat file
	private byte[] mLineBuffer = new byte[4096];
	
	
	/**
	 * Reads the current CPU stats from /proc/stat and returns the results
	 * as a HashMap where the key is the identifier, i.e. "CPU" and the jiffies
	 * are an array of Long.
	 * 
	 * This has the side effect of updating the list of CPUs that this object
	 * stores internally.
	 * 
	 * @return
	 */
	public HashMap<String,Long[]> getReading() {
		long startTime = System.currentTimeMillis();
		//Log.d(TAG, "getReading() start time (ms): " + String.valueOf(startTime));
		HashMap<String,Long[]> reading = new HashMap<String, Long[]>(MAX_CPUS);
		try {
			mStatFileReader.seek(0);
			mStatFileReader.read(mLineBuffer);
			String[] lines = (new String(mLineBuffer).split("\n"));
			
			for (String line : lines) {
				if (line.startsWith("cpu")) {
					String[] tokens = line.split(" +");
					Long[] jiffies = new Long[tokens.length - 1];
					
					for (int i = 1; i < tokens.length; i++) {
						//Log.d(TAG, "Token="+ tokens[i]);
						jiffies[i - 1] = Long.parseLong(tokens[i]);
					}
					
					reading.put(tokens[0], jiffies);
					line = mStatFileReader.readLine();
				} else {
					// We can assume that they are all at the beginning.
					break;
				}
			}
			
			if (reading.size() > mTotalCPUNames) {
				updateCPUList(reading);
			}
			
			
			long endTime = System.currentTimeMillis();
			
			//Log.d(TAG, "getReading() end time (ms): " + String.valueOf(System.currentTimeMillis()));
			//Log.d(TAG, "getReading() time elapsed (ms): " + String.valueOf(endTime - startTime));
			
			return reading;
		} catch (IOException e) {
			//Log.e(TAG, "getReading(): caught IOException " + e.getMessage());
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
