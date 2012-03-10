package com.tomdignan.UltimateResourceMonitor;


import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.RotateAnimation;

/**
 * This class defines a reusable resource meter fragment.
 */
public class URMResourceMeterFragment extends Fragment {
	@SuppressWarnings("unused")
	private static final String TAG = "URMResourceMonitor";
	
	/** Namespace for XML attributes */
	public static final String NS_URM = "http://schemas.tomdignan.com/urm";
	
	/** size = ( "small" | "medium" | "large" ) */
	public static final String ATTR_SIZE = "size";

	/** Small meter size */
	public static final int SIZE_SMALL = 0;
	
	/** Medium meter size */
	public static final int SIZE_MEDIUM = 1;
	
	/** Large meter size */
	public static final int SIZE_LARGE = 2;
	
	/** Public, empty,  constructor */
	public URMResourceMeterFragment() {}
	
	/** Handle to the "needle" which is rotated to indicate the position */
	private View mNeedle;	
	
	/** Indicates the size of the requested meter */
	private Integer mSizeCode = null;
	
	/** Maps size names to size codes */
	private static int getSizeCode(String sizeName) {
		if (sizeName.equalsIgnoreCase("medium")) {
			return SIZE_MEDIUM;
		} else if (sizeName.equalsIgnoreCase("small")) {
			return SIZE_SMALL;
		} else if (sizeName.equalsIgnoreCase("large")) {
			return SIZE_LARGE;
		} else {
			throw new IllegalArgumentException("Invalid size: " + sizeName);
		}
	}
	
	/**
	 * This is overridden so we can read the "urm:size" attribute
	 * {@inheritDoc}
	 */
	@Override
	public void onInflate(Activity activity, AttributeSet attrs,
			Bundle savedInstanceState) {
		super.onInflate(activity, attrs, savedInstanceState);
		// Save the size for use in onCreateView
		mSizeCode = getSizeCode(attrs.getAttributeValue(NS_URM, ATTR_SIZE));
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view; 
		
		switch (mSizeCode) {
			case SIZE_SMALL:
				view =  inflater.inflate(R.layout.urm_resource_meter_small, null);
				break;
			case SIZE_LARGE:
				view = inflater.inflate(R.layout.urm_resource_meter_large, null);
				break;
			// Medium is default, but unless incorrect programming occurs, the size code should
			// always be present. So if this error is shown, that constitutes a bug.
			default:
				Log.e(TAG, "onCreateView: Invalid size code: " + mSizeCode + " falling back on SIZE_MEDIUM");
			case SIZE_MEDIUM:
				view = inflater.inflate(R.layout.urm_resource_meter_medium, null);
				break;
		}
		
		mNeedle = view.findViewById(R.id.ivMeterNeedle);
		return view;
	}
	

	
    public void rotateNeedle() {
        int xCenter = mNeedle.getWidth() / 2 + 1;
        int yCenter = mNeedle.getHeight() / 2 + 1;
        RotateAnimation animation = new RotateAnimation(0f, 360f, xCenter, yCenter);
        animation.setDuration(1200);
        mNeedle.startAnimation(animation);
    }
}
