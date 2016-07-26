package vn.dungtin.servicestack;

/**
 * Created by buumb on 6/2/2016.
 */
public class LocationInfo
{
	public LocationDetail coords;

	public LocationInfo()
	{
		coords = new LocationDetail();
	}

	public class LocationDetail
	{
		//{"coords":{"latitude":0,"longitude":0,"accuracy":0,
		//          "altitude":0,"altitudeAccuracy":0,"heading":0,"speed":0}}
		public double Latitude;
		public double Longitude;
		public int Accuracy;
		public double Altitude;
		public double AltitudeAccuracy;
		public double Heading;
		public double Speed;
	}
}
