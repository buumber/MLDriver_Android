package vn.dungtin.mldriver;

import android.content.Context;
import android.location.LocationManager;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by buumb on 6/2/2016.
 */

public class Utilities
{

	static final String TAG = "Utilities";

	public static Date StringToDate(String value)
	{
		if(value == null)
			return null;

		String format = "yyyy-MM-dd'T'HH:mm:ss";
		if (value.length() > 23)
			format = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
		else if (value.length() > 19)
			format = "yyyy-MM-dd'T'HH:mm:ss.SSS";
		return StringToDate(value, format);

	}

	public static Date StringToDate(String value, String format)
	{
		Date convertedDate;
		try
		{
			SimpleDateFormat dateFormat = new SimpleDateFormat(format);
			convertedDate = dateFormat.parse(value);
		}
		catch (ParseException e)
		{
			e.printStackTrace();
			convertedDate = null;
		}
		return convertedDate;

	}

	public static String DateToString(Date date)
	{
		try
		{
			SimpleDateFormat fdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
			return fdf.format(date);
		}
		catch (Exception e)
		{
			Log.e(TAG, e.getMessage());
		}
		return "";
	}

	public static String DateToString(Date date, String format)
	{
		try
		{
			SimpleDateFormat fdf = new SimpleDateFormat(format);
			return fdf.format(date);
		}
		catch (Exception e)
		{
			Log.e(TAG, e.getMessage());
		}
		return "";
	}

	public static String getHex(byte[] bytes)
	{
		StringBuilder sb = new StringBuilder();
        /*
         * for (int i = bytes.length - 1; i >= 0; --i) { int b = bytes[i] &
		 * 0xff; if (b < 0x10) sb.append('0');
		 * sb.append(Integer.toHexString(b)); if (i > 0) { sb.append(" "); } }
		 */
		for (int i = 0; i < bytes.length; i++)
		{
			int b = bytes[i] & 0xff;
			if (b < 0x10)
				sb.append('0');
			sb.append(Integer.toHexString(b));
		}
		return sb.toString();
	}

	public static long getDec(byte[] bytes)
	{
		long result = 0;
		long factor = 1;
		for (int i = 0; i < bytes.length; ++i)
		{
			long value = bytes[i] & 0xffl;
			result += value * factor;
			factor *= 256l;
		}
		return result;
	}

	public static String getDeviceInfo()
	{
		String serialNo = null;

		try
		{
			Class<?> c = Class.forName("android.os.SystemProperties");
			Method get = c.getMethod("get", String.class, String.class);
			serialNo = (String) (get.invoke(c, "ro.serialno", "unknown"));
		}
		catch (Exception ignored)
		{
		}
        /*String androidId = Settings.Secure.ANDROID_ID;

        StringBuffer buf = new StringBuffer();
        buf.append("ver:" + Build.VERSION.RELEASE);
        buf.append("." + Build.VERSION.INCREMENTAL);
        buf.append("-sdk:" + Build.VERSION.SDK_INT);
        buf.append("-fp:" + Build.FINGERPRINT);
        buf.append("-board:" + Build.BOARD);
        buf.append("-brand:" + Build.BRAND);
        buf.append("-device:" + Build.DEVICE);
        buf.append("-man:" + Build.MANUFACTURER);
        buf.append("-model:" + Build.MODEL);*/

		return String.format("%s-%s", serialNo, Build.MODEL);
	}

	//--Decrypt 32bit AES
	public static String RijndaelECBDecrypt(String text, String key) throws Exception
	{
		Cipher cipher = Cipher.getInstance("AES/ECB/PKCS7Padding");
		byte[] keyBytes = new byte[32];
		byte[] b = key.getBytes("UTF-8");
		int len = b.length;
		if (len > keyBytes.length) len = keyBytes.length;
		System.arraycopy(b, 0, keyBytes, 0, len);
		SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
		cipher.init(Cipher.DECRYPT_MODE, keySpec);

		byte[] results = cipher.doFinal(Base64.decode(text, Base64.DEFAULT));
		return new String(results, "UTF-8");
	}

	//--Encrypt 32bit AES
	public static String RijndaelECBEncrypt(String text, String key)
			throws Exception
	{
		Cipher cipher = Cipher.getInstance("AES/ECB/PKCS7Padding");
		byte[] keyBytes = new byte[32];
		byte[] b = key.getBytes("UTF-8");
		int len = b.length;
		if (len > keyBytes.length) len = keyBytes.length;
		System.arraycopy(b, 0, keyBytes, 0, len);
		SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

		cipher.init(Cipher.ENCRYPT_MODE, keySpec);

		byte[] results = cipher.doFinal(text.getBytes("UTF-8"));
		return Base64.encodeToString(results, Base64.DEFAULT);
	}

	public static boolean isGPSEnable(Context cxt)
	{
		LocationManager locationManager = (LocationManager) cxt.getSystemService(cxt.LOCATION_SERVICE);
		// getting GPS status
		return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
	}

	public static String StringAlignCenter(int width, String data)
	{
		if (data == null || data.length() == 0)
			return "";
		if (data.length() >= width)
			return data;
		//--
		int padCount = (width - data.length()) / 2;
		char[] x = data.toCharArray();

		char[] result = new char[width];
		Arrays.fill(result, ' ');
		System.arraycopy(x, 0, result, padCount, x.length);

		return String.valueOf(result);
	}

	public static String StringAlignLeftRight(int width, String leftData, String rightData)
	{
		if (leftData == null || leftData.length() == 0)
			leftData = " ";
		if (rightData == null || rightData.length() == 0)
			rightData = " ";

		if (leftData.length() + rightData.length() >= width)
		{
			if (leftData.length() >= width / 2)
			{
				leftData = leftData.substring(0, leftData.length() - 10);
				leftData += "...";
			}
			if (rightData.length() >= width / 2)
			{
				rightData = rightData.substring(0, rightData.length() - 10);
				rightData += "...";
			}
		}
		//--
		int padCount = width - (leftData.length() + rightData.length());
		char[] x = leftData.toCharArray();
		char[] y = rightData.toCharArray();

		char[] result = new char[width];
		Arrays.fill(result, ' ');
		System.arraycopy(x, 0, result, 0, x.length);
		System.arraycopy(y, 0, result, leftData.length() + padCount, y.length);

		return String.valueOf(result);
	}

	public static double DateDifferenceSecond(Date now, Date then)
	{
		return (double) (now.getTime() - then.getTime()) / 1000F;
	}

	public static String formatPhoneVN(String phone)
	{
		if(phone == null)
			return "";
		StringBuilder sb = new StringBuilder();
		String pattern = "XXX-XXX-XXXXXXXXX";
		int i = 0, j= 0;
		for(i=0, j=0;i<pattern.length() && j<phone.length();i++,j++)
		{
			if(pattern.charAt(i) == 'X')
			{
				sb.append(phone.charAt(j));

			}
			else
			{
				sb.append(pattern.charAt(i));
				j--;
			}
		}
		if(j<phone.length())
			sb.append(phone.substring(j));

		return sb.toString();
	}
}
