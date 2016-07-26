package vn.dungtin.mldriver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.*;

import java.util.ArrayList;
import java.util.List;

import vn.dungtin.db.MLDBHandler;
import vn.dungtin.db.dto.GpsCurrentLocationDTO;
import vn.dungtin.db.dto.TaxiOrderDTO;

/**
 * Created by buumb on 5/25/2016.
 */
public class MyLocation extends BaseActivity implements OnMapReadyCallback
{
	protected final String TAG = "MyLocation";
	private GoogleMap mGoogleMap;
	private SupportMapFragment mFragment;

	private GpsCurrentLocationDTO currentLocation;
	private GpsLocationChangedReceiver locationChangedReceiver = new GpsLocationChangedReceiver();

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mylocation);

		InitHeaderBar();
		InitFooterOrder();

		mFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
		//mFragment.getMapAsync(this);
	}

	@Override
	protected void updateOrderStatus()
	{

	}

	@Override
	protected void doRegisterBroadcast()
	{
		try
		{
			registerReceiver(locationChangedReceiver,
					new IntentFilter(Declaration.MSG_GPS_LOCATION_CHANGED));
			//--
			registerReceiver(
					orderChangedReceiver,
					new IntentFilter(Declaration.MSG_ORDER_UPDATE)
			);
		}
		catch (Exception ex)
		{
			Log.e(TAG, "[doRegisterBroadcast] " + ex.getMessage());
			ex.printStackTrace();
		}
	}

	@Override
	protected void doUnregisterBroadcast()
	{
		try
		{
			unregisterReceiver(locationChangedReceiver);
		}
		catch (Exception ex)
		{
			Log.e(TAG, "[doUnregisterBroadcast] locationChangedReceiver: " + ex.getMessage());
			ex.printStackTrace();
		}
		try
		{
			unregisterReceiver(
					orderChangedReceiver
			);
		}
		catch (Exception ex)
		{
			Log.e(TAG, "[doUnregisterBroadcast] orderChangedReceiver: " + ex.getMessage());
			ex.printStackTrace();
		}

	}

	private boolean runOnce = false;
	@Override
	public void onMapReady(GoogleMap googleMap)
	{

		mGoogleMap = googleMap;

		//this.mGoogleMap.getUiSettings().setMyLocationButtonEnabled(false);
		mGoogleMap.setMyLocationEnabled(true);
		mGoogleMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener()
		{
			@Override
			public void onCameraChange(CameraPosition cameraPosition)
			{
				//Log.d(TAG, "[onCameraChange] " + cameraPosition);
				if(!runOnce)
				{
					runOnce = true;
					//--
					GpsCurrentLocationDTO loc = mldbHandler.getCurrentLocation(
							WorkingSession.getInstance().getCurrentJournal().JournalId);
					if(loc != null)
					{
						LatLng latLng = new LatLng(loc.Latitude, loc.Longitude);
						displayTaxiAndCustomer(latLng);

						currentLocation = loc;
						currentRoute.add(latLng);
					}
					else
						displayTaxiAndCustomer(null);
				}
			}
		});
		//--
//		GpsCurrentLocationDTO loc = mldbHandler.getCurrentLocation(
//				WorkingSession.getInstance().getCurrentJournal().JournalId);
//		if(loc != null)
//		{
//			LatLng latLng = new LatLng(loc.Latitude, loc.Longitude);
//			displayTaxiAndCustomer(latLng);
//
//			currentLocation = loc;
//			currentRoute.add(latLng);
//		}
//		else
//			displayTaxiAndCustomer(null);
	}

	private void displayTaxiAndCustomer(LatLng latLng)
	{
		mGoogleMap.clear();
		//--
		LatLng latLng2 = null;
		if(WorkingSession.getInstance().getCurrentOrder() != null)
		{
			TaxiOrderDTO order = WorkingSession.getInstance().getCurrentOrder();

			//--
			latLng2 = new LatLng(order.OrderLat, order.OrderLng);
			mGoogleMap.addMarker(new MarkerOptions()
					.position(latLng2)
					.title(
							order.OrderAddress == null || order.OrderAddress == ""?
									"Vị trí đón": order.OrderAddress)
			);
		}
		//--
		if(latLng != null)
		{
//			mGoogleMap.addMarker(new MarkerOptions()
//					.position(latLng)
//					.icon(
//							BitmapDescriptorFactory.fromResource(R.drawable.taxi)
//					)
//			);
			if(currentLocation == null)
			{
				CameraUpdate cu = null;
				if(latLng2 != null)
				{
					LatLngBounds.Builder builder = new LatLngBounds.Builder();
					builder.include(latLng).include(latLng2);
					LatLngBounds bounds = builder.build();

					int padding = 200; // offset from edges of the map in pixels
					cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
				}
				else
				{
					CameraPosition.Builder builder = new CameraPosition.Builder();
					builder.zoom(13);
					builder.target(latLng);
					cu = CameraUpdateFactory.newCameraPosition(builder.build());
				}
				mGoogleMap.animateCamera(cu);
			}
		}
		//--

	}

	private List<LatLng> currentRoute = new ArrayList<>();
	private Polyline routeLine;
	@Override
	public void onPause()
	{
		Log.d(TAG, "[onPause]...");
		super.onPause();

		doUnregisterBroadcast();
		currentRoute.clear();
		currentLocation = null;
		routeLine = null;
	}

	@Override
	protected void onResume()
	{
		Log.d(TAG, "[onResume]...");
		super.onResume();
		mFragment.getMapAsync(this);
		doRegisterBroadcast();
		//--
		if(WorkingSession.getInstance().getCurrentOrder() != null)
		{
			TaxiOrderDTO order = WorkingSession.getInstance().getCurrentOrder();
			if (order.OrderStatus == 3) //--accepted
			{
				showFinishView();
			}
			else
			{
				showAcceptView();
			}
		}
	}

	//private Marker currentBalloon;
	public class GpsLocationChangedReceiver extends BroadcastReceiver
	{
		private final String TAG = "GpsLocationReceiver";
		@Override
		public void onReceive(Context context, Intent intent)
		{
			Log.d(TAG, "[onReceive]...");
			GpsCurrentLocationDTO loc = mldbHandler.getCurrentLocation(
					WorkingSession.getInstance().getCurrentJournal().JournalId);
			if(loc == null)
				return;
			LatLng latLng = new LatLng(loc.Latitude, loc.Longitude);
			displayTaxiAndCustomer(latLng);
			if (currentLocation == null)
			{
				currentLocation = loc;
				currentRoute.add(latLng);
			}
			else
			{
				currentLocation = loc;

//				boolean existed = false;
//				for(int i=currentRoute.size()-1;i >= 0;i--)
//				{
//					if(currentRoute.get(i).latitude == loc.Latitude &&
//							currentRoute.get(i).longitude == loc.Longitude)
//					{
//						existed = true;
//						break;
//					}
//				}
//				if(!existed)
//				{
//					currentRoute.add(latLng);
//				}
//
//				if(currentRoute.size() > 1)//at least 2 points
//				{
//					if(routeLine == null)
//					{
//						PolylineOptions points = new PolylineOptions().width(3).color(Color.RED);
//						for (LatLng point :
//								currentRoute)
//						{
//							points.add(point);
//						}
//						routeLine = mGoogleMap.addPolyline(points);
//					}
//					else
//					{
//						List<LatLng> points = routeLine.getPoints();
//						points.add(latLng);
//						routeLine.setPoints(points);
//					}
//				}
			}
		}
	}

	private BroadcastReceiver orderChangedReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (WorkingSession.getInstance().getCurrentOrder() == null)
			{
				WorkingSession.showErrorDialog(
						MyLocation.this,
						"Thông báo",
						"Khách đã huỷ chuyến",
						new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick(DialogInterface dialog, int which)
							{
								goToWaitingList();
							}
						}
				);
			}
		}
	};
}