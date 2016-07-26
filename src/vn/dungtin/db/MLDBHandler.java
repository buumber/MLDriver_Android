package vn.dungtin.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import vn.dungtin.db.dto.DriverInfoDTO;
import vn.dungtin.db.dto.GpsCurrentLocationDTO;
import vn.dungtin.db.dto.JournalDTO;
import vn.dungtin.db.dto.TaxiOrderDTO;
import vn.dungtin.mldriver.Utilities;

/**
 * Created by buumb on 5/29/2016.
 */
public class MLDBHandler extends SQLiteOpenHelper
{
	private static final Object synchronizedLock = "LOCK";
	private static final int DATABASE_VERSION = 4;
	private final String TAG = "MLDBHandler-Short";
	private static final String DATABASE_NAME = "mlg-driver.db";

	public MLDBHandler(Context context)
	{
		super(context,
				DATABASE_NAME,
				null,
				DATABASE_VERSION
		);
	}

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		Log.d(TAG, "Setup new database...");
		try
		{
			String CREATE_DRIVER_TABLE = "CREATE TABLE DriverInfo(" +
					"DriverId integer," +
					"StaffCardNumber text," +
					"CarNumber text," +
					"FullName text," +
					"PhoneNumber text," +
					"Password text," +
					"Status int," +
					"ClientId text," +
					"CreatedDate datetime DEFAULT CURRENT_TIMESTAMP" +
					");";

			String CREATE_TABLE_JOURNAL = "CREATE TABLE Journal (" +
					"JournalId integer primary key autoincrement," +
					"Timestamp datetime DEFAULT CURRENT_TIMESTAMP," +
					"DriverId int," +
					"Longitude real," +
					"Latitude real," +
					"DeviceId text);";

			String CREATE_TABLE_RUNTIME_VALUES_STATUS = "create table RuntimeValue(\n" +
					"\tName text,\n" +
					"\tValue text);";

			String CREATE_TABLE_GPS_LOCATION = "create table GpsCurrentLocation (\n" +
					"\tLongitude real,\n" +
					"\tLatitude real,\n" +
					"\tAltitude real,\n" +
					"\tBearing real,\n" +
					"\tSpeed real,\n" +
					"\tAccuracy real,\n" +
					"\tJournalId int,\n" +
					"\tUpdatedDate datetime DEFAULT CURRENT_TIMESTAMP\n" +
					");";

			String CREATE_TABLE_ORDER = "create table TaxiOrder (\n" +
					"\tOrderId integer,\n" +
					"\tOrderAddress text,\n" +
					"\tOrderLat real,\n" +
					"\tOrderLng real,\n" +
					"\tOrderLogAccuracy real,\n" +
					"\tOrderPhone text,\n" +
					"\tOrderStatus integer,\n" +
					"\tOrderDate datetime,\n" +
					"\tTaxiType integer,\n" +
					"\tConfirmedDate datetime,\n" +
					"\tAcceptedDate datetime,\n" +
					"\tFinishedDate datetime,\n" +
					"\tCreatedDate datetime,\n" +
					"\tJournalId integer,\n" +
					"\tDriverId integer,\n" +
					"\tFinishedOrderLat real,\n" +
					"\tFinishedOrderLng real\n" +
					");";

			String CREATE_TABLE_ORDER_CANCELLED = "create table OrderCancelled (\n" +
					"\tOrderId integer,\n" +
					"\tJournalId integer,\n" +
					"\tDriverId integer,\n" +
					"\tCreatedDate datetime\n" +
					");";

			//---
			db.execSQL(CREATE_DRIVER_TABLE);
			db.execSQL(CREATE_TABLE_JOURNAL);
			db.execSQL(CREATE_TABLE_RUNTIME_VALUES_STATUS);
			db.execSQL(CREATE_TABLE_GPS_LOCATION);
			db.execSQL(CREATE_TABLE_ORDER);
			db.execSQL(CREATE_TABLE_ORDER_CANCELLED);
		}
		catch (Exception ex)
		{
			Log.e(TAG, ex.getMessage());
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		db.execSQL("DROP TABLE IF EXISTS DriverInfo");
		db.execSQL("DROP TABLE IF EXISTS Journal");
		db.execSQL("DROP TABLE IF EXISTS RuntimeValue");
		db.execSQL("DROP TABLE IF EXISTS GpsCurrentLocation");
		db.execSQL("DROP TABLE IF EXISTS TaxiOrder");
		db.execSQL("DROP TABLE IF EXISTS OrderCancelled");

		onCreate(db);
	}

	public synchronized <T> long addTableRow(T valueObject, String table)
	{
		return addTableRowWithExclusive(valueObject, table, null);
	}

	public <T> long addTableRowWithExclusive(T valueObject,
	                                         String table,
	                                         List<String> exclusiveField)
	{
		if (valueObject == null || table == null || table.length() == 0)
			return -1;

		long rs = -1;
		synchronized (synchronizedLock)
		{
			//Log.i(TAG, String.format("[addTableRowWithExclusive] ------%s -------", table));
			//Log.i(TAG, valueObject.getClass().getName());
			//---
			ContentValues values = new ContentValues();
			for (Field field : valueObject.getClass().getFields())
			{
				try
				{
					if (exclusiveField != null && exclusiveField.contains(field.getName()))
						continue;
					if (java.lang.reflect.Modifier.isStatic(field.getModifiers()))
						continue;
					//---
					Type type = field.getGenericType();
					if (type instanceof ParameterizedType)
						continue; //not process yet
					//--
					type = field.getType();

					if (type.equals(String.class) && field.get(valueObject) != null)
						values.put(field.getName(), field.get(valueObject).toString());
					else if (type.equals(Long.TYPE))
						values.put(field.getName(), field.getLong(valueObject));
					else if (type.equals(Date.class) && field.get(valueObject) != null)
						values.put(field.getName(), Utilities.DateToString((Date) field.get(valueObject)));
					else if (type.equals(Integer.TYPE))
						values.put(field.getName(), field.getInt(valueObject));
					else if (type.equals(Double.TYPE))
						values.put(field.getName(), field.getDouble(valueObject));
				}
				catch (IllegalAccessException e)
				{
					e.printStackTrace();
					Log.e(TAG, "[addTableRowWithExclusive] IllegalAccessException: " + e.toString());
				}
			}

//			for(String key: values.keySet())
//			{
//				Log.i(TAG, "[" + key + "] = " + values.get(key));
//			}
			//---
			try
			{
				SQLiteDatabase db = this.getWritableDatabase();
				// Inserting Row
				rs = db.insert(table, null, values);
				db.close(); // Closing database connection
			}
			catch (Exception ex)
			{
				Log.e(TAG, "[addTableRowWithExclusive] IllegalAccessException: " + ex.toString());
			}
		}//end of synchronized

		return rs;
	}

	public long addTableRowByContentValues(ContentValues values, String table)
	{
		if (values == null || values.size() == 0 || table == null || table.length() == 0)
			return -1;

		long rs = -1;
		//Log.i(TAG, String.format("[addTableRowByContentValues] ------addTableRow: %s -------", table));
		//---
		synchronized (synchronizedLock)
		{
//			for(String key: values.keySet())
//			{
//				Log.i(TAG, "[" + key + "] = " + values.get(key));
//			}
			//---
			try
			{
				SQLiteDatabase db = this.getWritableDatabase();
				// Inserting Row
				rs = db.insert(table, null, values);
				db.close(); // Closing database connection
			}
			catch (Exception ex)
			{
				Log.e(TAG, "[addTableRowByContentValues] EXCEPTION: " + ex.toString());
			}
		}//end of synchronized

		return rs;
	}

	public <T> int updateTableRow(T valueObject,
	                              String table,
	                              String keyField,
	                              Object keyValue)
	{
		if (valueObject == null
				|| table == null || table.length() == 0
				|| keyField == null || keyField.length() == 0
				|| keyValue == null)
		{
			return -1;
		}

		int rs = -1;

		synchronized (synchronizedLock)
		{
			//Log.i(TAG, String.format("[updateTableRow] ------%s -------", table));
			//Log.i(TAG, "[updateTableRow] " + valueObject.getClass().getName());
			//---
			ContentValues values = new ContentValues();

			for (Field field : valueObject.getClass().getFields())
			{
				try
				{
					Type type = field.getGenericType();
					if (type instanceof ParameterizedType)
						continue; //not process yet
					//--
					type = field.getType();

					if (field.getName().equals(keyField))
						continue;
					//--
					if (type.equals(String.class) && field.get(valueObject) != null)
						values.put(field.getName(), field.get(valueObject).toString());
					else if (type.equals(Long.TYPE))
						values.put(field.getName(), field.getLong(valueObject));
					else if (type.equals(Date.class))
					{
						if(field.get(valueObject) != null)
							values.put(field.getName(),
									Utilities.DateToString((Date) field.get(valueObject)));
						else
							values.putNull(field.getName());
					}
					else if (type.equals(Integer.TYPE))
						values.put(field.getName(), field.getInt(valueObject));
					else if (type.equals(Double.TYPE))
						values.put(field.getName(), field.getDouble(valueObject));
				}
				catch (IllegalAccessException e)
				{
					e.printStackTrace();
					Log.e(TAG, "[updateTableRow] IllegalAccessException: " + e.toString());
				}
			}

//			Log.i(TAG, "Key [" + keyField + "] = " + keyValue);
//			for(String key: values.keySet())
//			{
//				Log.i(TAG, "[" + key + "] = " + values.get(key));
//			}
			try
			{
				SQLiteDatabase db = this.getWritableDatabase();
				// updating row
				rs = db.update(table, values, keyField + " = ?",
						new String[]{String.valueOf(keyValue)});

				db.close();
			}
			catch (Exception ex)
			{
				Log.e(TAG, "[updateTableRow] Exception: " + ex.toString());
			}
		}//end of synchronized

		return rs;
	}

	public <T> int updateTableRow(T valueObject, String table,
	                              String keyField, Object keyValue,
	                              List<String> exclusiveFields)
	{
		if (valueObject == null
				|| table == null || table.length() == 0
				|| keyField == null || keyField.length() == 0
				|| keyValue == null)
		{
			return -1;
		}

		int rs = -1;

		synchronized (synchronizedLock)
		{
			//Log.i(TAG, String.format("[updateTableRow] ------%s -------", table));
			//Log.i(TAG, "[updateTableRow] " + valueObject.getClass().getName());
			//---
			ContentValues values = new ContentValues();

			for (Field field : valueObject.getClass().getFields())
			{
				try
				{
					Type type = field.getGenericType();
					if (type instanceof ParameterizedType)
						continue; //not process yet
					//--
					type = field.getType();

					if (field.getName().equals(keyField))
						continue;
					if (exclusiveFields != null && exclusiveFields.contains(field.getName()))
						continue;
					//--
					if (type.equals(String.class) && field.get(valueObject) != null)
						values.put(field.getName(), field.get(valueObject).toString());
					else if (type.equals(Long.TYPE))
						values.put(field.getName(), field.getLong(valueObject));
					else if (type.equals(Date.class) && field.get(valueObject) != null)
						values.put(field.getName(), Utilities.DateToString((Date) field.get(valueObject)));
					else if (type.equals(Integer.TYPE))
						values.put(field.getName(), field.getInt(valueObject));
					else if (type.equals(Double.TYPE))
						values.put(field.getName(), field.getDouble(valueObject));
				}
				catch (IllegalAccessException e)
				{
					e.printStackTrace();
					Log.e(TAG, "[updateTableRow] IllegalAccessException: " + e.toString());
				}
			}

//			Log.i(TAG, "Key [" + keyField + "] = " + keyValue);
//			for(String key: values.keySet())
//			{
//				Log.i(TAG, "[" + key + "] = " + values.get(key));
//			}
			try
			{
				SQLiteDatabase db = this.getWritableDatabase();
				// updating row
				rs = db.update(table, values, keyField + " = ?",
						new String[]{String.valueOf(keyValue)});

				db.close();
			}
			catch (Exception ex)
			{
				Log.e(TAG, "[updateTableRow] Exception: " + ex.toString());
			}
		}//end of synchronized

		return rs;
	}

	public int updateTableRow(ContentValues valueObject,
	                          String table,
	                          String keyField,
	                          Object keyValue)
	{
		if (valueObject == null
				|| table == null || table.length() == 0
				|| keyField == null || keyField.length() == 0
				|| keyValue == null)
		{
			return -1;
		}

		int rs = -1;
		synchronized (synchronizedLock)
		{
			//Log.i(TAG, String.format("[updateTableRow] ------%s -------", table));
			//---
//			Log.i(TAG, "Key [" + keyField + "] = " + keyValue);
//			for(String key: valueObject.keySet())
//			{
//				Log.i(TAG, "[" + key + "] = " + valueObject.get(key));
//			}
			try
			{
				SQLiteDatabase db = this.getWritableDatabase();
				// updating row
				rs = db.update(table, valueObject, keyField + " = ?",
						new String[]{String.valueOf(keyValue)});

				db.close();
			}
			catch (Exception ex)
			{
				Log.e(TAG, "[updateTableRow] Exception: " + ex.toString());
			}
		}//synchronized

		return rs;
	}

	public int updateTableRows(ContentValues valueObject,
	                          String table,
	                          String whereClause,
	                          String[] whereArgs)
	{
		if (valueObject == null
				|| table == null || table.length() == 0)
		{
			return -1;
		}

		int rs = -1;
		synchronized (synchronizedLock)
		{
			try
			{
				SQLiteDatabase db = this.getWritableDatabase();
				// updating row
				rs = db.update(table, valueObject,
						whereClause,
						whereArgs);

				db.close();
			}
			catch (Exception ex)
			{
				Log.e(TAG, "[updateTableRows] Exception: " + ex.toString());
			}
		}//synchronized

		return rs;
	}

	public boolean checkDataRowExists(String table, String keyField, Object value)
	{
		if (value == null)
			throw new NullPointerException("Value cannot null");
		int count = 0;
		synchronized (synchronizedLock)
		{
			try
			{
				String sql = String.format("select * from %s where %s=?", table, keyField);
				SQLiteDatabase db = this.getReadableDatabase();

				Cursor cursor = db.rawQuery(sql, new String[]{value.toString()});

				if (cursor == null)
					return false;

				count = cursor.getCount();

				cursor.close();
				db.close();
			}
			catch (Exception ex)
			{
				Log.e(TAG, "[checkDataRowExists] Exception: " + ex.toString());
			}
		}//end of synchronized

		return (count > 0);
	}

	public <T> List<T> getTableRows(String query, String[] paramValues, Class<T> cls)
	{
		List<T> result = null;
		synchronized (synchronizedLock)
		{
			try
			{
				//Log.d(TAG, "[getTableRows] " + query);
				SQLiteDatabase db = this.getReadableDatabase();
				Cursor cursor = db.rawQuery(query, paramValues);

				if (cursor == null)
					return null;

				if (cursor.moveToFirst())
				{
					result = new ArrayList<T>();
					do
					{
						try
						{
							T xx = cls.newInstance();
							for (Field field : xx.getClass().getFields())
							{
								try
								{
									if (cursor.getColumnIndex(field.getName()) == -1) //not found
										continue;
									//--
									Type type = field.getGenericType();
									if (type instanceof ParameterizedType)
										continue; //not process yet
									//--
									type = field.getType();

									if (cursor.isNull(cursor.getColumnIndex(field.getName())))
										continue;

									if (type.equals(String.class))
										field.set(xx, cursor.getString(cursor.getColumnIndex(field.getName())));
									else if (type.equals(Long.TYPE))
										field.set(xx, cursor.getLong(cursor.getColumnIndex(field.getName())));
									else if (type.equals(Date.class))
									{
										field.set(xx, Utilities.StringToDate(
												cursor.getString(
														cursor.getColumnIndex(field.getName()))
										));
									}
									else if (type.equals(Integer.TYPE))
										field.set(xx, cursor.getInt(cursor.getColumnIndex(field.getName())));
									else if (type.equals(Double.TYPE))
										field.set(xx, cursor.getDouble(cursor.getColumnIndex(field.getName())));
								}
								catch (Exception ex)
								{
									Log.e(TAG, String.format("[getTableRows] field: %s. %s",
											field.getName(), ex.getMessage()));
								}
							}
							result.add(xx);
						}
						catch (Exception ex)
						{
							Log.e(TAG, "[getTableRows] " + ex.getMessage());
						}
					}
					while (cursor.moveToNext());
				}
				else
				{
					//Log.d(TAG, "[getTableRows] No data found");
				}

				cursor.close();
				db.close();
			}
			catch (Exception ex)
			{
				Log.e(TAG, "[getTableRows] EXCEPTION: " + ex.toString());
			}
		}//end of synchronized

		return result;
	}

	public synchronized long addOrUpdateDriver(DriverInfoDTO driver)
	{
		boolean x = checkDataRowExists("DriverInfo", "DriverId", driver.DriverId);
		long rs;
		if (x)
			rs = updateTableRow(driver, "DriverInfo", "DriverId", driver.DriverId);
		else
			rs = addTableRow(
					driver,
					"DriverInfo"
					);

		return rs;
	}

	public synchronized long addOrUpdateCurrentLocation(GpsCurrentLocationDTO xx)
	{
		boolean x = checkDataRowExists("GpsCurrentLocation", "JournalId", xx.JournalID);
		long rs;
		if (x)
			rs = updateTableRow(xx, "GpsCurrentLocation", "JournalId", xx.JournalID);
		else
			rs = addTableRow(xx, "GpsCurrentLocation");

		return rs;
	}

	public synchronized GpsCurrentLocationDTO getCurrentLocation(long journalId)
	{
		String sql = String.format("select * from GpsCurrentLocation where JournalId = ?");
		List<GpsCurrentLocationDTO> locations = getTableRows(
				sql,
				new String[]{String.valueOf(journalId)},
				GpsCurrentLocationDTO.class
		);
		if (locations != null && locations.size() > 0)
			return locations.get(0);
		return null;
	}

	public synchronized long addOrUpdateRuntimeValue(String name, String value)
	{
		if (name == null)
			return -1;
		long rs = 0;
		try
		{
			boolean x = checkDataRowExists("RuntimeValue", "name", name);

			if (x)
			{
				ContentValues xx = new ContentValues();
				xx.put("value", value);
				rs = updateTableRow(xx, "RuntimeValue", "name", name);
			}
			else
			{
				ContentValues xx = new ContentValues();
				xx.put("name", name);
				xx.put("value", value);
				rs = addTableRowByContentValues(xx, "RuntimeValue");
			}
		}
		catch (Exception ex)
		{
			Log.e(TAG, "[addOrUpdateRuntimeValue] EXCEPTION: " + ex.getMessage());
		}

		return rs;
	}

	public synchronized JournalDTO getNewestJournal()
	{
		List<JournalDTO> journals = getTableRows(
				"select * from Journal order by JournalId desc limit 1;",
				null,
				JournalDTO.class
		);

		if (journals == null || journals.size() == 0)
			return null;
		return journals.get(0);
	}

	public synchronized long addJournal(JournalDTO value)
	{
		long rs = addTableRowWithExclusive(value, "Journal", Arrays.asList("JournalId"));
		return rs;
	}

	public synchronized long updateJournal(JournalDTO value)
	{
		long rs = updateTableRow(value, "Journal", "JournalId", value.JournalId);
		return rs;
	}

	public synchronized DriverInfoDTO getDriverInfo(int driverId)
	{
		String sql = String.format("select * from DriverInfo where DriverId = ?");
		List<DriverInfoDTO> drivers = getTableRows(
				sql,
				new String[]{String.valueOf(driverId)},
				DriverInfoDTO.class
		);
		if (drivers != null && drivers.size() > 0)
			return drivers.get(0);
		return null;
	}

	public synchronized long addOrUpdateOrder(TaxiOrderDTO order)
	{
		boolean x = checkDataRowExists("TaxiOrder", "OrderId", order.OrderId);
		long rs;
		if (x)
			rs = updateTableRow(order, "TaxiOrder", "OrderId", order.OrderId);
		else
			rs = addTableRow(
					order,
					"TaxiOrder"
			);

		return rs;
	}

	public synchronized List<TaxiOrderDTO> getWaitingOrders(long journalId,
															int driverId)
	{
		Log.d(TAG, String.format("[getWaitingOrders] journalId: %d, driverId: %d",
				journalId, driverId));
		String sql = String.format("select * from TaxiOrder " +
				"where JournalId = ? " +
				"and OrderStatus = 1 "
//				"and OrderId not in (select orderId from OrderCancelled " +
//				"where JournalId = ? and DriverId = ?)"
		);
		List<TaxiOrderDTO> orders = getTableRows(
				sql,
				new String[]{
						String.valueOf(journalId),
				},
				TaxiOrderDTO.class
		);

		return orders;
	}

	public synchronized int updateAllOrder2Missing(long journalId)
	{
		ContentValues xx = new ContentValues();
		xx.put("OrderStatus", -1);

		return updateTableRows(
				xx,
				"TaxiOrder",
				"JournalId = ? and ConfirmedDate is null",
				new String[]{String.valueOf(journalId)}
		);
	}

	public synchronized long addCancelledOrder(int orderId, int journalId, int driverId)
	{
//		String CREATE_TABLE_ORDER_CANCELLED = "create table OrderCancelled (\n" +
//				"\tOrderId integer,\n" +
//				"\tJournalId integer,\n" +
//				"\tDriverId integer,\n" +
//				"\tCreatedDate datetime\n" +
//				");";
		ContentValues xx = new ContentValues();
		xx.put("OrderId", orderId);
		xx.put("JournalId", journalId);
		xx.put("DriverId", driverId);
		xx.put("CreatedDate", Utilities.DateToString(new Date()));
		long rs = addTableRowByContentValues(xx, "OrderCancelled");

		return rs;
	}

	public String getRuntimeValue(String name)
	{
		String result = "";
		synchronized (synchronizedLock)
		{
			try
			{
				String sql = "select * from RuntimeValue where Name=?";
				//Log.d(TAG, "[getRuntimeValues] " + sql);
				SQLiteDatabase db = this.getReadableDatabase();
				Cursor cursor = db.rawQuery(sql, new String[]{name});

				if (cursor == null)
					return null;

				if (cursor.moveToFirst())
				{
					result = cursor.getString(cursor.getColumnIndex("Value"));
				}
				else
				{
					Log.d(TAG, "[getRuntimeValue] No data found");
				}
				cursor.close();
				db.close();
			}
			catch (Exception ex)
			{
				Log.e(TAG, "[getRuntimeValue] Exception: " + ex.toString());
			}
		}//end of synchronized

		return result;
	}

	public long updateLoginRemember(int driverId)
	{
		return addOrUpdateRuntimeValue("LoginRemember", String.valueOf(driverId));
	}
}
