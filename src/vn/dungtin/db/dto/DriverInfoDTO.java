package vn.dungtin.db.dto;

import java.util.Date;

import vn.dungtin.servicestack.DriverInfo;

/**
 * Created by buumb on 5/29/2016.
 */
public class DriverInfoDTO
{
	public int DriverId;
	public String StaffCardNumber;
	public String CarNumber;
	public String FullName;
	public String PhoneNumber;
	public String Password;
	public int Status;
	public Date CreatedDate;
	public String ClientId;

//	public enum DriverStatus
//	{
//		NotSet(0),
//		Ready(1),
//		Confirmed(2),
//		Accepted(3)
//		; //--required to separate with below methods
//
//		private final int statusCode;
//		private DriverStatus(int code)
//		{
//			statusCode = code;
//		}
//	}

	public DriverInfoDTO()
	{

	}

	public DriverInfoDTO(DriverInfo source)
	{
		if(source == null)
			return;
		this.DriverId = source.DriverId;
		this.StaffCardNumber = source.StaffCard;
		this.CarNumber = source.TaxiNo;
		this.FullName = source.DriverName;
		this.Status = source.DriverStatus;
		this.ClientId = source.DriverClientID;
	}
}