package com.tomdignan.UltimateResourceMonitor;


import java.util.ArrayList;

import com.tomdignan.UltimateResourceMonitor.URMResourceMonitor.OnResourcesReceivedListener;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.widget.Button;

/**
 * Main Activity of the application. Responsible for displaying four meters
 * or three depending on whether the user has a dual core processor.
 * 
 * CPU0, CPU1, RAM, and Battery.
 * 
 * @author Tom Dignan
 */
public class URMMonitorActivity extends FragmentActivity 
implements View.OnClickListener, OnResourcesReceivedListener {
	private URMResourceMonitor mResourceMonitor = new URMResourceMonitor();
	
	@SuppressWarnings("unused")
	private static final String TAG = "URMMonitorActivity";
	
	/** 
	 * Number of meters to be found by default. They are actually
	 * created in XML, so this number only controls their acquisition
	 */
	private static final int DEFAULT_METERS = 4;
	
	/** List of available resource meters */
	private ArrayList<URMResourceMeterFragment> mResourceMeters = 
			new ArrayList<URMResourceMeterFragment>(DEFAULT_METERS);
	
	/** {@inheritDoc} */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.urm_monitor_layout);
        initResourceMeters();
        attachResourceMonitor();
    }

    /**
     * Connect the resource monitor to the implemented
     * OnResourcesReceivedListener.
     */
    private void attachResourceMonitor() {
    	mResourceMonitor.setOnResourcesReceivedListener(this);
    }
    
    /** 
     * Get the expected (4) resource meters that were defined
     * in XML and store them in the list mResourceMeters. 
     * 
     * Must be called before attempting to use any resource meters.
     */
    private void initResourceMeters() {
        FragmentManager manager = getSupportFragmentManager();
		mResourceMeters.add((URMResourceMeterFragment) manager
				.findFragmentById(R.id.fResourceMeter1));
		
		mResourceMeters.add((URMResourceMeterFragment) manager
				.findFragmentById(R.id.fResourceMeter2));
		
		mResourceMeters.add((URMResourceMeterFragment) manager
				.findFragmentById(R.id.fResourceMeter3));
		
    }
    
	public synchronized void onClick(View view) {
		int id = view.getId();
		switch(id) {
		case R.id.bTest:
			Button button = (Button) view;
			if (mResourceMonitor.isStarted()) {
				mResourceMonitor.stop();
				button.setText("Start Monitoring");
			} else {
				mResourceMonitor.start();
				button.setText("Stop Monitoring");
			}
			
//			for (URMResourceMeterFragment meter : mResourceMeters) {
//				meter.rotateNeedle(0, 20);
//			}
//			
			break;
		}
	}

	/**
	 * Receives results from the resource monitor.
	 * 
	 * @param long[] cpuUsages -- First result is the number of CPUs
	 * available in the array (It is always 8 longs, but doesn't use
	 * all of the available space. This is so it can be used when phones
	 * with more cores come out.), second result is the aggregate of all cores,
	 * all subsequent results are the cpu0->cpuN. Don't read past cpuUsages[0]
	 * elements!
	 */
	private static class UpdateResourcesRunnable implements Runnable {
		public ArrayList<URMResourceMeterFragment> resourceMeters;
		public float[] cpuUsages;

		public void run() {
			for (int i = 1; i <= cpuUsages[0]; i++) {
				int number = i - 1;
				URMResourceMeterFragment meter = resourceMeters.get(number);
				meter.setName("cpu" + (number == 0 ? " " : number + " "));
				meter.setValue(cpuUsages[i]);
			}
		}
	}
	
	private static UpdateResourcesRunnable sUpdateResourcesRunnable = new UpdateResourcesRunnable();

	public void onResourcesReceived(final float[] cpuUsages) {
		sUpdateResourcesRunnable.cpuUsages = cpuUsages;
		sUpdateResourcesRunnable.resourceMeters = mResourceMeters;
		runOnUiThread(sUpdateResourcesRunnable);
	}
}