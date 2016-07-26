package vn.dungtin.mldriver;

import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.CookieJar;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import vn.dungtin.db.dto.GpsCurrentLocationDTO;
import vn.dungtin.servicestack.AuthProvider;
import vn.dungtin.servicestack.DataResultListener;
import vn.dungtin.servicestack.LocationInfo;
import vn.dungtin.servicestack.NamedCallback;

/**
 * Created by buumb on 6/2/2016.
 */
public class DataProvider
{
	private static CookieJar cookieJar =
			new PersistentCookieJar(new SetCookieCache(), new SharedPrefsCookiePersistor(
					MLDriverApp.getAppContext()
			));

	private final OkHttpClient okHttpClient = new OkHttpClient.Builder()
			.cookieJar(cookieJar)
			.build();
	private static final MediaType JSON
			= MediaType.parse("application/json; charset=utf-8");

	public DataProvider()
	{

	}

	public DataProvider resetCookieJar()
	{
		cookieJar = new PersistentCookieJar(new SetCookieCache(), new SharedPrefsCookiePersistor(
				MLDriverApp.getAppContext()
		));

		return this;
	}

	public void doLoginAsync(
			String driverId,
	        String carNumber,
	        String driverPhone,
	        String password,
	        DataResultListener listener
	)
	{
		String url = MLDriverApp.getAppContext().getString(R.string.ws_url)
				+ "/api/auth";

		//--request data: {"Provider":"driver","Username":"4026,1234,0951231231234","Password":"4026"}
		Gson gson = new Gson();
		AuthProvider ap = new AuthProvider();
		ap.Provider="driver";
		ap.UserName = String.format("%s,%s,%s", driverId, carNumber, driverPhone);
		ap.Password = password;

		String postString = gson.toJson(ap);
		RequestBody body = RequestBody.create(JSON, postString);
		Request request = new Request.Builder()
				.url(url)
				.post(body)
				.build();

		okHttpClient.newCall(request).enqueue(
				NamedCallback.build().setCallerName("doLoginAsync")
				.setDataResultListener(listener)
		);
	}

	public void doLogoutAsync(
			String driverClientId,
			DataResultListener listener
	)
	{
		if(driverClientId == null || driverClientId.length() == 0) {
			if(listener != null)
				listener.onSuccess("doLogoutAsync", null);
			return;
		}

		String url = MLDriverApp.getAppContext().getString(R.string.ws_url)
				+ "/api/4779306B-017E-4F2F-8D47-9CF394380128"; //DriverUpdateStatusRequest

		//
		RequestBody body = RequestBody.create(JSON, "{\"Status\":0}");
		Request request = new Request.Builder()
				.url(url)
				.addHeader("DriverClientID", driverClientId)
				.post(body)
				.build();

		okHttpClient.newCall(request).enqueue(
				NamedCallback.build().setCallerName("doLogoutAsync")
						.setDataResultListener(listener)
		);
		//{"SessionId":null,"UserName":null,
		// "ReferrerUrl":null,
		// "ResponseStatus":{"ErrorCode":null,"Message":null,"StackTrace":null,"Errors":null}}
	}

	public void doClearSessionAsync(
			String driverClientId,
			DataResultListener listener
	)
	{
		if(driverClientId == null || driverClientId.length() == 0) {
			if(listener != null)
				listener.onSuccess("doClearSessionAsync", null);
			return;
		}

		String url = MLDriverApp.getAppContext().getString(R.string.ws_url)
				+ "/api/auth/logout";

		//
		RequestBody body = RequestBody.create(JSON, "{\"Status\":0}");
		Request request = new Request.Builder()
				.url(url)
				.addHeader("DriverClientID", driverClientId)
				.post(body)
				.build();

		okHttpClient.newCall(request).enqueue(
				NamedCallback.build().setCallerName("doClearSessionAsync")
						.setDataResultListener(listener)
		);
		//{"SessionId":null,"UserName":null,
		// "ReferrerUrl":null,
		// "ResponseStatus":{"ErrorCode":null,"Message":null,"StackTrace":null,"Errors":null}}
	}

	public void doInitializeRequestAsync(
			DataResultListener listener
	)
	{
		String url = MLDriverApp.getAppContext().getString(R.string.ws_url)
				+ "/api/7D2858D7-57AB-42CD-B4FE-6C0813A9E836"; //DriverInitializeRequest

		RequestBody body = RequestBody.create(JSON, "");
		Request request = new Request.Builder()
				.url(url)
				.post(body)
				.build();

		okHttpClient.newCall(request).enqueue(
				NamedCallback.build().setCallerName("doGetDriverInfoAsync")
						.setDataResultListener(listener)
		);
	}

	public void doUpdateLocationAsync(
			String driverClientId,
			GpsCurrentLocationDTO location,
			DataResultListener listener
	)
	{
		String url = MLDriverApp.getAppContext().getString(R.string.ws_url)
				+ "/api/AAFD52D7-3CBA-4056-85E5-AECA5978C428"; //DriverUpdateLocationRequest
		//{"coords":{"latitude":0,"longitude":0,"accuracy":0,
		//          "altitude":0,"altitudeAccuracy":0,"heading":0,"speed":0}}
		//{"coords":{"latitude":10.8128751,"longitude":106.6688367,"accuracy":27,"altitude":0,"altitudeAccuracy":0,"heading":null,"speed":null}}

		LocationInfo li = new LocationInfo();
		li.coords.Accuracy = (int)location.Accuracy;
		li.coords.Altitude = location.Altitude;
		li.coords.Latitude = location.Latitude;
		li.coords.Longitude = location.Longitude;
		li.coords.Speed = location.Speed;

		RequestBody body = RequestBody.create(
				JSON,
				new Gson().toJson(li));
		Request request = new Request.Builder()
				.url(url)
				.addHeader("DriverClientID", driverClientId)
				.post(body)
				.build();

		okHttpClient.newCall(request).enqueue(
				NamedCallback.build().setCallerName("doUpdateLocationAsync")
						.setDataResultListener(listener)
		);
	}

	public void doGetWaitingOrderAsync(
			String driverClientId,
			DataResultListener listener
	)
	{
		String url = MLDriverApp.getAppContext().getString(R.string.ws_url)
				+ "/api/E94EF87F-5570-410A-9299-59B2D8B7EBBC"; //DriverGetWaitingOrderRequest


		RequestBody body = RequestBody.create(
				JSON,
				"");
		Request request = new Request.Builder()
				.url(url)
				.addHeader("DriverClientID", driverClientId)
				.post(body)
				.build();

		okHttpClient.newCall(request).enqueue(
				NamedCallback.build().setCallerName("doGetWaitingOrderAsync")
						.setDataResultListener(listener)
		);

		//{"WaitingOrders":[
		//  {"OrderId":10,
		// "OrderAddress":"96 Yên Thế, Phường 2, Tân Bình, Hồ Chí Minh, Vietnam",
		// "OrderLat":10.81289,
		// "OrderLng":106.6688,
		// "OrderLogAccuracy":null,
		// "OrderPhone":"0936549876",
		// "OrderStatus":1,
		// "OrderDate":"2016-06-02T20:29:19.1644817+07:00",
		// "TaxiType":2}
		// ]}
	}

	public void doConfirmWaitingOrderAsync(
			String driverClientId,
			int orderId,
			DataResultListener listener
	)
	{
		String url = MLDriverApp.getAppContext().getString(R.string.ws_url)
				+ "/api/DB930C88-4BB3-4CF6-92FE-37ADF6EEDC9D"; //DriverConfirmOrderRequest

		RequestBody body = RequestBody.create(
				JSON,
				String.format("{\"OrderId\":%d}", orderId));
		Request request = new Request.Builder()
				.url(url)
				.addHeader("DriverClientID", driverClientId)
				.post(body)
				.build();

		okHttpClient.newCall(request).enqueue(
				NamedCallback.build().setCallerName("doConfirmWaitingOrderAsync")
						.setDataResultListener(listener)
		);

		//{"Driver":{"DriverStatus":2},
		// "CurrentOrder":{
		//      "OrderId":2,
		//      "OrderAddress":"90/3 Hẻm 90, Phường 2, Tân Bình, Hồ Chí Minh, Vietnam",
		//      "OrderLat":10.81287,
		//      "OrderLng":106.6689,
		//      "OrderLogAccuracy":null,
		//      "OrderPhone":"0934567896",
		//      "OrderStatus":2,
		//      "OrderDate":"2016-06-01T14:58:53.7062433+07:00",
		//      "ConfirmedDate":"2016-06-01T14:59:02.1124626+07:00"}}

	}

	public void doAcceptOrderAsync(
			String driverClientId,
			int orderId,
			DataResultListener listener
	)
	{
		String url = MLDriverApp.getAppContext().getString(R.string.ws_url)
				+ "/api/D14E5BB7-1139-446F-BC84-CFE53B976341"; //DriverAcceptOrderRequest

		RequestBody body = RequestBody.create(
				JSON,
				String.format("{\"OrderId\":%d}", orderId));
		Request request = new Request.Builder()
				.url(url)
				.addHeader("DriverClientID", driverClientId)
				.post(body)
				.build();

		okHttpClient.newCall(request).enqueue(
				NamedCallback.build().setCallerName("doAcceptOrderAsync")
						.setDataResultListener(listener)
		);

		//{"Driver":{"DriverStatus":3},
		// "Order":{
		//      "OrderId":2,
		//      "OrderAddress":"90/3 Hẻm 90, Phường 2, Tân Bình, Hồ Chí Minh, Vietnam",
		//      "OrderLat":10.81287,
		//      "OrderLng":106.6689,
		//      "OrderLogAccuracy":null,
		//      "OrderPhone":"0934567896",
		//      "OrderStatus":3,
		//      "OrderDate":"2016-06-01T14:58:53.7062433+07:00",
		//      "ConfirmedDate":"2016-06-01T14:59:02.1124626+07:00",
		//      "AcceptedDate":"2016-06-01T14:59:20.5343766+07:00",
		//      "TaxiType":1}}

	}

	public void doFinishOrderAsync(
			String driverClientId,
			int orderId,
			DataResultListener listener
	)
	{
		String url = MLDriverApp.getAppContext().getString(R.string.ws_url)
				+ "/api/6FC12390-4323-44D5-B8BD-04310625A4D7"; //DriverFinishOrderRequest

		RequestBody body = RequestBody.create(
				JSON,
				String.format("{\"OrderId\":%d}", orderId));
		Request request = new Request.Builder()
				.url(url)
				.addHeader("DriverClientID", driverClientId)
				.post(body)
				.build();

		okHttpClient.newCall(request).enqueue(
				NamedCallback.build().setCallerName("doFinishOrderAsync")
						.setDataResultListener(listener)
		);

		//{"Driver":{"DriverStatus":1},
		// "CurrentOrder":{"OrderId":0}}

	}

	public void doCustomerNotFoundAsync(
			String driverClientId,
			DataResultListener listener
	)
	{
		String url = MLDriverApp.getAppContext().getString(R.string.ws_url)
				+ "/api/9B1553F6-AFEF-4908-92DC-BB1D04F01D5A"; //DriverReportWrongCurrentOrderRequest

		RequestBody body = RequestBody.create(
				JSON,
				"");
		Request request = new Request.Builder()
				.url(url)
				.addHeader("DriverClientID", driverClientId)
				.post(body)
				.build();

		okHttpClient.newCall(request).enqueue(
				NamedCallback.build().setCallerName("doCustomerNotFoundAsync")
						.setDataResultListener(listener)
		);

		//{"Driver":{"DriverStatus":1},"CurrentOrder":{"OrderId":0}}

	}

	public void doCancelOrderdAsync(
			String driverClientId,
			DataResultListener listener
	)
	{
		String url = MLDriverApp.getAppContext().getString(R.string.ws_url)
				+ "/api/7E269F13-4CFB-4FC7-AE55-970A9AEAE03D"; //DriverUnconfirmOrderRequest

		RequestBody body = RequestBody.create(
				JSON,
				"");
		Request request = new Request.Builder()
				.url(url)
				.addHeader("DriverClientID", driverClientId)
				.post(body)
				.build();

		okHttpClient.newCall(request).enqueue(
				NamedCallback.build().setCallerName("doCancelOrderdAsync")
						.setDataResultListener(listener)
		);

		//{"Driver":{"DriverStatus":1},"CurrentOrder":{"OrderId":0}}

	}

	public void doCreateOrderdAsync(
			String driverClientId,
			DataResultListener listener
	)
	{
		String url = MLDriverApp.getAppContext().getString(R.string.ws_url)
				+ "/api/4FAA42BB-9803-4A18-8464-4D80BB9CFAA4"; //DriverCreateOrderRequest

		RequestBody body = RequestBody.create(
				JSON,
				"");
		Request request = new Request.Builder()
				.url(url)
				.addHeader("DriverClientID", driverClientId)
				.post(body)
				.build();

		okHttpClient.newCall(request).enqueue(
				NamedCallback.build().setCallerName("doCreateOrderdAsync")
						.setDataResultListener(listener)
		);

		/*
		{
			"Driver":
			{
				"DriverStatus":3
			},
			"CurrentOrder":
			{
				"OrderId":12,
				"OrderAddress":"96 Yên Thế, Phường 2, Tân Bình, Hồ Chí Minh, Việt Nam",
				"OrderLat":10.8129,"OrderLng":106.6688,
				"OrderLogAccuracy":0,
				"OrderPhone":"",
				"OrderStatus":3,
				"OrderDate":"2016-06-06T15:10:01.2680121+07:00",
				"ConfirmedDate":"2016-06-06T15:10:01.2680121+07:00"
			}
		}
		 */

	}

	public void doChangePwdAsync(
			String driverClientId,
			String driverCardNumber,
			String oldPwd,
			String newPwd,
			DataResultListener listener
	)
	{
		String url = MLDriverApp.getAppContext().getString(R.string.ws_url)
				+ "/api/DC58EDF2-B4A7-424D-A294-8344E04B768B"; //DriverChangePassworRequest

		//{"StaffCode":0,"OldPassword":"String","NewPassword":"String"}
		String input = String.format("{\"StaffCard\":%s,\"OldPassword\":\"%s\",\"NewPassword\":\"%s\"}",
				driverCardNumber, oldPwd, newPwd);
		RequestBody body = RequestBody.create(
				JSON,
				input);
		Request request = new Request.Builder()
				.url(url)
				.addHeader("DriverClientID", driverClientId)
				.post(body)
				.build();

		okHttpClient.newCall(request).enqueue(
				NamedCallback.build().setCallerName("doChangePwdAsync")
						.setDataResultListener(listener)
		);

	}
}
