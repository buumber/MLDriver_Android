package vn.dungtin.db.dto;

import java.util.Date;

/**
 * Created by buumb on 5/30/2016.
 */
public class GpsCurrentLocationDTO
{
	public double Longitude;
	public double Latitude;
	public double Altitude;
	public double Bearing;
	public double Speed;
	public double Accuracy;
	public long JournalID;
	public Date UpdatedDate;

	public String toString()
	{
		return String.format("Latitude: %f, Longitude: %f, JournalId: %d",
				Latitude, Longitude, JournalID);
	}
}
