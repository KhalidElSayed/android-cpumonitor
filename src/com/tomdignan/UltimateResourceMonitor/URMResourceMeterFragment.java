package com.tomdignan.UltimateResourceMonitor;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * This class defines a reusable resource meter fragment.
 */
public class URMResourceMeterFragment extends Fragment {
	@SuppressWarnings("unused")
	private static final String TAG = "URMResourceMonitor";
	
	/** Namespace for XML attributes */
	public static final String NS_URM = "http://schemas.tomdignan.com/urm";
	
	/** Public, empty,  constructor */
	public URMResourceMeterFragment() {}
	
	/** MUST be set to the current value of the meter [0,1] */
	private float mValue = 0;
	
	/** ViewHolder pattern */
	private static class ViewHolder {
		public TextView tvResourceName;
		public TextView tvResourceUsage;
	}
	
	/** References to views */
	private ViewHolder mVH = new ViewHolder();
	
	/** {@inheritDoc} */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.urm_resource_meter, null);
		mVH.tvResourceName = (TextView) view.findViewById(R.id.tvResourceName);
		mVH.tvResourceUsage = (TextView) view.findViewById(R.id.tvResourceUsage);
		return view;
	}
	
	/**
	 * Sets the displayed name of this resource monitor 
	 * @param name
	 */
	public void setName(String name) {
		mVH.tvResourceName.setText(name);
	}
    /**
     * Sets the current position of the resource meter. Should be a value 
     * between 0 and 1, inclusive.
     * 
     * @param float percent [0, 1]
     */
	public void setValue(Float percent) {
		percent = (float) Math.floor(percent * 1000) / 10;
		mVH.tvResourceUsage.setText(String.valueOf(percent) + "%");
	}
}
