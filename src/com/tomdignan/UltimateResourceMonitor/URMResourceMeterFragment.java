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
	
	/** MUST be a handle to the "needle" which is rotated to indicate the position */
	private View mNeedle;	
	
	/** MUST indicate the size of the requested meter */
	private Integer mSizeCode = null;
	
	/** MUST be set to the current value of the meter [0,1] */
	private float mValue = 0;
	
	/** MUST be set to the current position of the needle in degrees */
	private float mDegrees = 0;
	
	/** MUST be set to the center of the meter on the x-axis */
	private int mXCenter;
	
	/** MUST be set to center of the meter on the y-axis */
	private int mYCenter;
	
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
	
	/** {@inheritDoc} */
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
	
	/**
	 * Used for testing alignment. Just rotates the needle of the 
	 * meter 360 degrees.
	 * 
	 * @float startAngle in degrees
	 * @float endAngle in degrees
	 */
	public void rotateNeedle(float startAngle, float endAngle) {
		mXCenter = mNeedle.getWidth() / 2 + 1;
        mYCenter = mNeedle.getHeight() / 2 + 1;		
		RotateAnimation animation = new RotateAnimation(startAngle, endAngle,
				mXCenter, mYCenter);
		animation.setDuration(1000);
		mNeedle.startAnimation(animation);
	}
    

    /**
     * Sets the current position of the resource meter. Should be a value 
     * between 0 and 1, inclusive.
     * 
     * @param float percent [0, 1]
     */
	public void setValue(float percent) {
		float endDegrees = percent *  300;
		rotateNeedle(mDegrees, endDegrees);
		mValue = percent;
		mDegrees = endDegrees;
	}
}
