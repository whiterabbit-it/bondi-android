package com.bondi_android.tracking;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.bondi_android.R;
import com.bondi_android.checkin.CheckinActivity;

public class TrackingLineActivity extends Activity implements LocationListener {
	private static final int TWO_MINUTES = 1000 * 60 * 2;
	private static final int UPDATE_RATE = 100;

	private LocationManager locationManager;
	private Location currentLocation;
	private TextView text;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tracking_line);

		// Add selected line to the text.
		text = (TextView) findViewById(R.id.trackingText);
		Intent intent = this.getIntent();
		String selectedLine = intent
				.getStringExtra(CheckinActivity.SELECTED_LINE);
		text.setText(text.getText() + selectedLine);

		// Getting LocationManager object
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		registerNewBestProvider(null);
	}

	private void registerNewBestProvider(String oldProvider) {
		Criteria criteria = new Criteria();
		// Getting the name of the provider that meets the criteria
		String newProvider = locationManager.getBestProvider(criteria, false);

		if (newProvider != null && !newProvider.equals("") && !newProvider.equals(oldProvider)) {
			if (oldProvider!=null && locationManager!=null) {
				locationManager.removeUpdates(this);
			}

			// Get the location from the given provider
			currentLocation = locationManager.getLastKnownLocation(newProvider);

			locationManager.requestLocationUpdates(newProvider, UPDATE_RATE, 1, this);

			if (currentLocation != null)
				onLocationChanged(currentLocation);
			else
				Toast.makeText(getBaseContext(), "No location was cached",
						Toast.LENGTH_SHORT).show();
			return;
		} 

		//If here, there's no new provider.
		if (oldProvider==null) {
			Toast.makeText(getBaseContext(), "No Provider Found",
					Toast.LENGTH_SHORT).show();
		}
	}
	
	@Override
	protected void onDestroy() {
		if (locationManager != null) {
			locationManager.removeUpdates(this);
		}
		Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show();
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.tracking_line, menu);
		return true;
	}

	@Override
	public void onLocationChanged(Location location) {
		if (this.isBetterLocation(location, currentLocation)) {
			// Report to the UI that the location was updated
			String msg = "Updated Location: "
					+ Double.toString(location.getLatitude()) + ","
					+ Double.toString(location.getLongitude());
			Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
			/*
			 * TODO OBS: If your app accesses the network or does other
			 * long-running work after receiving a location update, adjust the
			 * fastest interval to a slower value. This prevents your app from
			 * receiving updates it can't use. Once the long-running work is
			 * done, set the fastest interval back to a fast value.
			 */
		}
	}

	public void stopTracking(View view) {
		// TODO Stop tracking
		finish();
	}

	/**
	 * Determines whether one Location reading is better than the current
	 * Location fix
	 * 
	 * @param location
	 *            The new Location that you want to evaluate
	 * @param currentBestLocation
	 *            The current Location fix, to which you want to compare the new
	 *            one
	 */
	protected boolean isBetterLocation(Location location,
			Location currentBestLocation) {
		if (currentBestLocation == null) {
			// A new location is always better than no location
			return true;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
		boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use
		// the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			return true;
			// If the new location is more than two minutes older, it must be
			// worse
		} else if (isSignificantlyOlder) {
			return false;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation
				.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(location.getProvider(),
				currentBestLocation.getProvider());

		// Determine location quality using a combination of timeliness and
		// accuracy
		if (isMoreAccurate) {
			return true;
		} else if (isNewer && !isLessAccurate) {
			return true;
		} else if (isNewer && !isSignificantlyLessAccurate
				&& isFromSameProvider) {
			return true;
		}
		return false;
	}

	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}

	@Override
	public void onProviderDisabled(String provider) {
		registerNewBestProvider(provider);
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
	}
}