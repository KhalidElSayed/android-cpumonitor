package com.tomdignan.UltimateResourceMonitor;

import java.io.FileNotFoundException;
import java.util.Arrays;

import android.util.Log;

/**
 * Polls our various monitors for results.
 */
public class URMResourceMonitor {
	private static final String TAG = "URMResourceMonitor";

	/** 
	 * Thread for monitoring resources. 
	 * Created/started/stopped/destroyed by startMonitor/stopMonitor 
	 */
	private Thread mMonitorThread = null;
	
	/** Interface for handling the results of this resource polling */
	public interface OnResourcesReceivedListener {
		public void onResourcesReceived(long[] cpuUsages);
	}

	/** 
	 * Reference to the OnResourcesReceivedListener used to send results back
	 * to the user.
	 */
	protected OnResourcesReceivedListener mListener = null;
	
	/** Resources will be returned every POLL_FREQUENCY_MS */
	protected static final int POLL_FREQUENCY_MS = 1000;
	
	/** Register an OnResourcesReceivedListener to get results */
	public void setOnResourcesReceivedListener(OnResourcesReceivedListener listener) {
		mListener = listener;
	}
	
	/**  
	 * Publish results to the registered OnResourcesReceivedListener
	 * If one is not available, a warning will be printed to the logs.
	 */
	private void publishResults(long[] cpuUsages) {
		if (mListener != null) {
			mListener.onResourcesReceived(cpuUsages);
		} else {
			Log.w(TAG, "publishResults: Can't publish results, no OnResourcesReceivedListener is set");
		}
	}
	
	/** 
	 * Begin monitoring resources. 
	 * Make sure to register an OnResourcesReceievedListener first.
	 * 
	 * @returns True if monitoring has been started | False if monitoring was already 
	 * started or an error occured.
	 */
	public synchronized boolean start() {
		if (mMonitorThread == null) {
			try {
				mMonitorThread = new Thread(new ResourceMonitorTask());
				mMonitorThread.start();
			} catch (FileNotFoundException e) {
				// This may not be the most elegant way to handle the exception.
				Log.e(TAG, "startMonitoring caught FileNotFound: " + e.getMessage());
				return false;
			}
			return true;
		} 
	
		Log.e(TAG, "startMonitoring(): already started");
		return false;
	} 
	
	/** 
	 * Returns whether the monitor is started.
	 * @return boolean
	 */
	public synchronized boolean isStarted() {
		return mMonitorThread != null;
	}
	
	/**
	 * Stop monitoring resources and clean up resources used while monitoring.
	 * 
	 * @return boolean True if stopped | False if not stopped or if an error occurred.
	 */
	public synchronized boolean stop() {
		if (mMonitorThread != null) {
			mMonitorThread.interrupt();
			mMonitorThread = null;
			return true;
		} 

		Log.e(TAG, "stopMonitoring(): already stopped");
		return false;
	}
	
	/** Executed every second to grab resource statistics and publish */
	private class ResourceMonitorTask implements Runnable {
		URMCPUStatReader mCPUStatReader;
		
		public ResourceMonitorTask() throws FileNotFoundException {
			mCPUStatReader = new URMCPUStatReader();
		}
		
		
		public void run() {
			// Supposed to be null on the first call to getUsage().
			float[] cpuResults = null; 
			
			// Initialize
			mCPUStatReader.initializeReading();
			
			// Sleep the standard amount of time to allow for proper timing
			// between the initialization and first reading
			try {
				Thread.sleep(POLL_FREQUENCY_MS);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			while(!Thread.interrupted()) {
				cpuResults = mCPUStatReader.getUsage(cpuResults);
				Log.d(TAG, "cpuResults=" + Arrays.toString(cpuResults));
				try {
					Thread.sleep(POLL_FREQUENCY_MS);
				} catch (InterruptedException e) {
					Log.d(TAG, "Thread interrupted");
					break;
				}
			}
		}
	};
}
