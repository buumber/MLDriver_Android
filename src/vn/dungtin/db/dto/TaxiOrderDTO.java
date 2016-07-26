package vn.dungtin.db.dto;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Date;

import vn.dungtin.mldriver.Utilities;

/**
 * Created by buumb on 6/2/2016.
 */
public class TaxiOrderDTO extends Object
{
	public int OrderId;
	public String OrderAddress;
	public double OrderLat;
	public double OrderLng;
	public double OrderLogAccuracy;
	public String OrderPhone;
	public int OrderStatus;
	public Date OrderDate;
	public int TaxiType;
	public Date ConfirmedDate;
	public Date AcceptedDate;
	public Date FinishedDate;
	public Date CreatedDate;
	public int JournalId;
	public int DriverId;
	public double FinishedOrderLat;
	public double FinishedOrderLng;

	public boolean AreIdentical(TaxiOrderDTO candidate)
	{
		if(candidate == null)
			return false;
		if(candidate.OrderId != this.OrderId)
			return false;
		if(!candidate.OrderAddress.equals(this.OrderAddress))
			return false;
//		else if(candidate.OrderLat != this.OrderLat)
//			return false;
//		else if(candidate.OrderLng != this.OrderLng)
//			return false;
//		else if(candidate.OrderLogAccuracy != this.OrderLogAccuracy)
//			return false;
		if(candidate.OrderPhone != null && this.OrderPhone != null
				&& !candidate.OrderPhone.equals(this.OrderPhone))
			return false;
		if(candidate.OrderStatus != this.OrderStatus)
			return false;
//		else if(candidate.TaxiType != this.TaxiType)
//			return false;
		return true;
	}

	public TaxiOrderDTO()
	{

	}

	public TaxiOrderDTO(TaxiOrderDTO copy)
	{
		this.OrderId = copy.OrderId;
		this.OrderAddress = copy.OrderAddress;
		this.OrderLat = copy.OrderLat;
		this.OrderLng = copy.OrderLng;
		this.OrderLogAccuracy = copy.OrderLogAccuracy;
		this.OrderPhone = copy.OrderPhone;
		this.OrderStatus = copy.OrderStatus;
		this.OrderDate = copy.OrderDate;
		this.TaxiType = copy.TaxiType;
		this.ConfirmedDate = copy.ConfirmedDate;
		this.AcceptedDate = copy.AcceptedDate;
		this.FinishedDate = copy.FinishedDate;
		this.CreatedDate = copy.CreatedDate;
		this.JournalId = copy.JournalId;
		this.DriverId = copy.DriverId;
		this.FinishedOrderLat = copy.FinishedOrderLat;
		this.FinishedOrderLng = copy.FinishedOrderLng;
	}

	public String toJson()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		for (Field field : this.getClass().getFields())
		{
			try
			{
				Type type = field.getGenericType();
				if (type instanceof ParameterizedType)
					continue; //not process yet
				//--
				type = field.getType();

				if (type.equals(String.class) && field.get(this) != null)
				{
					sb.append(String.format("\"%s\": \"%s\"",
							field.getName(),
							field.get(this).toString()));
				}
				else if (type.equals(Long.TYPE))
					sb.append(String.format("\"%s\": %d",
							field.getName(), field.getLong(this)));
				else if (type.equals(Date.class) && field.get(this) != null)
					sb.append(String.format("\"%s\": \"%s\"",
							field.getName(), Utilities.DateToString((Date) field.get(this))));
				else if (type.equals(Integer.TYPE))
					sb.append(String.format("\"%s\": %d",
							field.getName(), field.getInt(this)));
				else if (type.equals(Double.TYPE))
					sb.append(String.format("\"%s\": %f",field.getName(), field.getDouble(this)));
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
		sb.append("}");

		return sb.toString();
	}
}
//
//public enum TaxiOrderStatus
//{
//	Available = 0,
//	GuestWaiting, = 1
//	DriverConfirmed, = 2
//	DriverAccepted, = 3
//	Finish, = 4
//  NotFound, = 5
//  Cancelled = 6
//  Unavailable = -1
//}