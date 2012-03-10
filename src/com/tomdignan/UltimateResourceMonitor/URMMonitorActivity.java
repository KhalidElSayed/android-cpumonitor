package com.tomdignan.UltimateResourceMonitor;


import java.util.ArrayList;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.View;

/**
 * Main Activity of the application. Responsible for displaying four meters
 * or three depending on whether the user has a dual core processor.
 * 
 * CPU0, CPU1, RAM, and Battery.
 * 
 * @author Tom Dignan
 */
public class URMMonitorActivity extends FragmentActivity 
implements View.OnClickListener {

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
        getResourceMeters();
    }

    /** 
     * Get the expected (4) resource meters that were defined
     * in XML and store them in the list mResourceMeters. 
     * 
     * Must be called before attempting to use any resource meters.
     */
    private void getResourceMeters() {
        FragmentManager manager = getSupportFragmentManager();
        
		mResourceMeters.add((URMResourceMeterFragment) manager
				.findFragmentById(R.id.fResourceMeter1));
		
		mResourceMeters.add((URMResourceMeterFragment) manager
				.findFragmentById(R.id.fResourceMeter2));
		
		mResourceMeters.add((URMResourceMeterFragment) manager
				.findFragmentById(R.id.fResourceMeter3));
		
		mResourceMeters.add((URMResourceMeterFragment) manager
				.findFragmentById(R.id.fResourceMeter4));
    }
    
	public void onClick(View view) {
		int id = view.getId();
		switch(id) {
		case R.id.bTest:
			for (URMResourceMeterFragment meter : mResourceMeters) {
				meter.rotateNeedle();
			}
		}
	}
}