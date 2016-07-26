package vn.dungtin.mldriver;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import okhttp3.Response;
import vn.dungtin.db.MLDBHandler;
import vn.dungtin.db.dto.DriverInfoDTO;
import vn.dungtin.db.dto.JournalDTO;
import vn.dungtin.db.dto.TaxiOrderDTO;
import vn.dungtin.service.GpsTracking;
import vn.dungtin.servicestack.ConfigureInfo;
import vn.dungtin.servicestack.DataResultListener;
import vn.dungtin.servicestack.DriverInfo;
import vn.dungtin.servicestack.Error;

/**
 * Created by buumb on 5/25/2016.
 */
public class Login extends Activity
{
	private final String TAG = "Login";
	private EditText txtDriverID;
	private EditText txtCarNumber;
	private EditText txtDriverPhone;
	private EditText txtDriverPwd;
	private Button btnLogin;
	private CheckBox chkRemember;

	//private Handler theHandler;


	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);
		//--
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		//--
		txtDriverID = (EditText)findViewById(R.id.txtDriverID);
//		txtDriverID.setText("4190");
		txtCarNumber = (EditText)findViewById(R.id.txtCarNumber);
//		txtCarNumber.setText("1234");
		txtDriverPhone = (EditText)findViewById(R.id.txtDriverPhone);
//		txtDriverPhone.setText("0912345678");
		txtDriverPwd = (EditText)findViewById(R.id.txtDriverPwd);
//		txtDriverPwd.setText("4190");

		chkRemember = (CheckBox)findViewById(R.id.chkRemember);
		//theHandler = new Handler();

		btnLogin = (Button)findViewById(R.id.btnLogin);
		btnLogin.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				setLoginButtonInfo(
						"Đang xử lý...",
						false
				);
				DriverInfoDTO driver = new DriverInfoDTO();
				driver.PhoneNumber = txtDriverPhone.getText().toString();
				driver.Password = txtDriverPwd.getText().toString();
				driver.CarNumber = txtCarNumber.getText().toString();
				driver.CreatedDate = new Date();
				driver.StaffCardNumber = txtDriverID.getText().toString();
				driver.Status =0; //unknown
				//---
				WorkingSession.getInstance().setCurrentDriver(driver);
				//--
				DataProvider dp = new DataProvider();
				dp.doLoginAsync(
						driver.StaffCardNumber,
						driver.CarNumber,
						driver.PhoneNumber,
						driver.Password,
						loginResult
				);

			}
		});

	}

	@Override
	protected void onPause()
	{
		Log.d(TAG, "[onPause]...");
		super.onPause();
	}

	@Override
	protected void onResume()
	{
		Log.d(TAG, "[onResume]...");
		super.onResume();

		MLDBHandler mldbHandler = new MLDBHandler(Login.this);
		Log.d(TAG, "[onResume] get LoginRemember from DB...");
		String value = mldbHandler.getRuntimeValue("LoginRemember");
		Log.d(TAG, "[onResume] result: " + value);
		int id = 0;
		try
		{
			Log.d(TAG, "[onResume] parsing to int...");
			id = Integer.parseInt(value);
			Log.d(TAG, "[onResume] result: " + id);
		}
		catch (Exception ex)
		{
			Log.e(TAG, "[onResume] Could not parse LoginRemember to int. ");
			ex.printStackTrace();
		}
		if(id <= 0)
		{
			chkRemember.setChecked(false);
			return;
		}
		Log.d(TAG, "[onResume] get Driver from DB by Id: " + id);
		DriverInfoDTO driver = mldbHandler.getDriverInfo(id);
		if(driver == null)
		{
			Log.d(TAG, "[onResume] No driver found.");
			chkRemember.setChecked(false);
			return;
		}
		Log.d(TAG, "[onResume] Update to UI...");
		txtDriverID.setText(driver.StaffCardNumber);
		txtCarNumber.setText(driver.CarNumber);
		txtDriverPhone.setText(driver.PhoneNumber);
		txtDriverPwd.setText(driver.Password);
		chkRemember.setChecked(true);
	}

	private void setLoginButtonInfo(final String text, final boolean enabled)
	{
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				btnLogin.setText(text);
				btnLogin.setEnabled(enabled);
			}
		});
	}

	private DataResultListener loginResult = new DataResultListener()
	{
		@Override
		public void onFailed(String caller, final Exception ex)
		{
			Log.e(TAG, "[loginResult.onFailed] " + ex.getMessage());
			WorkingSession.showErrorDialog(
					Login.this,
					"Lỗi đăng nhập",
					"Không thể đăng nhập vào lúc này, xin thử lại sau",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							setLoginButtonInfo("Đăng nhập", true);
						}
					}
			);
		}

		@Override
		public void onSuccess(String caller, final Response response)
		{

			try
			{
				//Gson gson = new Gson();
				String result = response.body().string();
				Log.d(TAG, "[loginResult.onSuccess] " + result);
				//--
				AtomicReference<DriverInfoDTO> arDriver = new AtomicReference<>();
				AtomicReference<TaxiOrderDTO> arCurentOrder = new AtomicReference<>();
				AtomicReferenceArray<TaxiOrderDTO> arWaitingOrders =
						new AtomicReferenceArray<TaxiOrderDTO>(100);
				AtomicReference<Error> arError = new AtomicReference<>();
				AtomicReference<ConfigureInfo> arConfig = new AtomicReference<>();

				if(!WorkingSession.getInstance().parseResultData(
						result, arDriver, arCurentOrder, arWaitingOrders, arConfig, arError ))
				{
					WorkingSession.showErrorDialog(
							Login.this,
							"Lỗi đăng nhập",
							"Không thể đăng nhập vào lúc này, xin thử lại sau" +
							arError.get() != null? "\n" + arError.get().Message : "",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									setLoginButtonInfo("Đăng nhập", true);
								}
							}
					);
					return;
				}
				if(arDriver.get() == null)
				{
					WorkingSession.showErrorDialog(
							Login.this,
							"Lỗi đăng nhập",
							"Không thể lấy thông tin lái xe, xin liên hệ bộ phận hỗ trợ",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									setLoginButtonInfo("Đăng nhập", true);
								}
							}
					);
					return;
				}
				//--
				DriverInfoDTO driver = WorkingSession.getInstance().getCurrentDriver();
				driver.Status = arDriver.get().Status;
				driver.DriverId = arDriver.get().DriverId;
				driver.FullName = arDriver.get().FullName;
				driver.ClientId = arDriver.get().ClientId;
				driver.Status = arDriver.get().Status;
				//--
				MLDBHandler mldbHandler = new MLDBHandler(Login.this);
				long xx;
				xx = mldbHandler.addOrUpdateDriver(driver);
				if(chkRemember.isChecked())
					mldbHandler.addOrUpdateRuntimeValue("LoginRemember", String.valueOf(driver.DriverId));
                else
                    mldbHandler.addOrUpdateRuntimeValue("LoginRemember", "0");
                //--
				JournalDTO jv = new JournalDTO();

				jv.Timestamp = new Date();
				jv.DriverId = driver.DriverId;
				jv.DeviceId = Utilities.getDeviceInfo();
				jv.Latitude = 0; //missed here
				jv.Longitude = 0; //missed here
				Log.d(TAG, "Add new journal...");
				xx = mldbHandler.addJournal(jv);
				jv.JournalId = xx;
				WorkingSession.getInstance().setCurrentJournal(jv);
				//--
				if(arConfig.get() != null)
				{
					ConfigureInfo ci = arConfig.get();
					if(ci.OrderDisplayPeriod > 0)
					{
						mldbHandler.addOrUpdateRuntimeValue("OrderDisplayPeriod", String.valueOf(ci.OrderDisplayPeriod));
						mldbHandler.addOrUpdateRuntimeValue("PullOrderPeriod", String.valueOf(ci.PullOrderPeriod));
						mldbHandler.addOrUpdateRuntimeValue("UpdateLocationPeriod", String.valueOf(ci.UpdateLocationPeriod));
					}
					else
					{
						mldbHandler.addOrUpdateRuntimeValue("OrderDisplayPeriod", String.valueOf(10));
						mldbHandler.addOrUpdateRuntimeValue("PullOrderPeriod", String.valueOf(7));
						mldbHandler.addOrUpdateRuntimeValue("UpdateLocationPeriod", String.valueOf(10));
					}
				}


				int i=0;
//				TaxiOrderDTO orderDTO;
//				while ( (orderDTO = arWaitingOrders.get(i)) != null)
//				{
//					orderDTO.DriverId = driver.DriverId;
//					orderDTO.JournalId = (int)jv.JournalId;
//					xx = mldbHandler.addOrUpdateOrder(orderDTO);
//					i++;
//				}
				//--
				if(arCurentOrder.get() != null)
				{
					TaxiOrderDTO ot = arCurentOrder.get();
					ot.JournalId = (int)WorkingSession.getInstance().getCurrentJournal().JournalId;
					ot.DriverId = WorkingSession.getInstance().getCurrentDriver().DriverId;
					WorkingSession.getInstance().setCurrentOrder(ot);
					mldbHandler.addOrUpdateOrder(ot);
					//--
					Intent next = new Intent(Login.this, PickupCustomer.class);
					startActivity(next);
				}
//				else if(i>0)
//				{
//					Intent next = new Intent(Login.this, CustomerWaitingList.class);
//					startActivity(next);
//				}
				else {
					//--
					Intent next = new Intent(Login.this, CustomerWaitingList.class);
					startActivity(next);
				}
				//--
				if(!IsMyServiceRunning())
				{
					Log.d(TAG, "Start GpsTracking service...");
					Intent intent = new Intent(Login.this, GpsTracking.class);
					startService(intent);
				}
				//--
				finish();
			}
			catch (Exception ex)
			{
				Log.e(TAG, "[doLogin.onSuccess] " + ex.getMessage());
				ex.printStackTrace();
				WorkingSession.showErrorDialog(
						Login.this,
						"Lỗi hệ thống",
						ex.getMessage(),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								setLoginButtonInfo("Đăng nhập", true);
							}
						}
				);
			}
			//--
//			DataProvider dp = new DataProvider();
//			dp.doGetDriverInfoAsync(getDriverInfoResult);


		}

		@Override
		public DataResultListener setUserData(Object data)
		{
			return this;
		}

		@Override
		public Object getUserData()
		{
			return null;
		}
	};

//	private DataResultListener doInitializeRequestResult = new DataResultListener()
//	{
//		@Override
//		public void onFailed(String caller, Exception ex)
//		{
//			Log.e(TAG, "[doInitializeRequestResult.onFailed] " + ex.getMessage());
//			WorkingSession.showErrorDialog(
//					Login.this,
//					"Lỗi hệ thống",
//					"Không thể kết nối đến máy chủ, xin thử lại sau",
//					new DialogInterface.OnClickListener() {
//						@Override
//						public void onClick(DialogInterface dialog, int which) {
//							setLoginButtonInfo("Đăng nhập", true);
//						}
//					}
//			);
//		}
//
//		@Override
//		public void onSuccess(String caller, Response response)
//		{
//			/*
//			{
//				"Driver":
//				{
//
//				},
//				"CurrentOrder":
//				{
//
//				},
//				"WaitingOrders":[],
//				Config: {  DisplayOrderPeriod: 10, UpdateLocationPeriod: 10, PullOrderPeriod: 600000 }
//			}
//			 */
//			try
//			{
//				Gson gson = new Gson();
//				String result = response.body().string();
//				Log.d(TAG, result);
//				//--
//				AtomicReference<DriverInfoDTO> x = new AtomicReference<>();
//				AtomicReference<TaxiOrderDTO> y = new AtomicReference<>();
//				AtomicReferenceArray<TaxiOrderDTO> z =
//						new AtomicReferenceArray<TaxiOrderDTO>(1000);
//				AtomicReference<Error> v = new AtomicReference<>();
//				AtomicReference<ConfigureInfo> c = new AtomicReference<>();
//
//				if(!WorkingSession.getInstance().parseResultData(
//						result, x, y, z, c, v ))
//				{
//
//				}
//
//				//--
//				DriverInfoDTO driver = WorkingSession.getInstance().getCurrentDriver();
//				driver.Status = x.get().Status;
//				driver.DriverId = x.get().DriverId;
//				driver.FullName = x.get().FullName;
//				driver.ClientId = x.get().ClientId;
//				driver.Status = x.get().Status;
//				//--
//				MLDBHandler mldbHandler = new MLDBHandler(Login.this);
//				long xx;
//				xx = mldbHandler.addOrUpdateDriver(driver);
//				//--
//				JournalDTO jv = new JournalDTO();
//
//				jv.Timestamp = new Date();
//				//jv.JournalId = Calendar.getInstance().getTimeInMillis();
//				jv.DriverId = driver.DriverId;
//				jv.DeviceId = Utilities.getDeviceInfo();
//				jv.Latitude = 0; //missed here
//				jv.Longitude = 0; //missed here
//				Log.d(TAG, "Add new journal...");
//				xx = mldbHandler.addJournal(jv);
//				jv.JournalId = xx;
//				WorkingSession.getInstance().setCurrentJournal(jv);
//				//--
//				int i=0;
//				TaxiOrderDTO w;
//				while ( (w = z.get(i)) != null)
//				{
//					long rs = mldbHandler.addOrUpdateOrder(w);
//					i++;
//				}
//				//--
//				if(y.get() != null)
//				{
//					WorkingSession.getInstance().setCurrentOrder(y.get());
//					//--
//					Intent next = new Intent(Login.this, PickupCustomer.class);
//					startActivity(next);
//				}
////				else if(i>0)
////				{
////					Intent next = new Intent(Login.this, CustomerWaitingList.class);
////					startActivity(next);
////				}
//				else {
//					//--
//					Intent next = new Intent(Login.this, CustomerWaitingList.class);
//					startActivity(next);
//				}
//				//--
//				if(!IsMyServiceRunning())
//				{
//					Log.d(TAG, "Start GpsTracking service...");
//					Intent intent = new Intent(Login.this, GpsTracking.class);
//					startService(intent);
//				}
//				//--
//				finish();
//			}
//			catch (Exception ex)
//			{
//				Log.e(TAG, "[getDriverInfoResult.onSuccess] " + ex.getMessage());
//				ex.printStackTrace();
//				WorkingSession.showErrorDialog(
//						Login.this,
//						"Lỗi hệ thống",
//						"Không thể lấy thông tin lái xe.\n" + ex.getMessage(),
//						new DialogInterface.OnClickListener() {
//							@Override
//							public void onClick(DialogInterface dialog, int which) {
//								setLoginButtonInfo("Đăng nhập", true);
//							}
//						}
//				);
//			}
//		}
//
//		private Object userData;
//
//		@Override
//		public DataResultListener setUserData(Object data)
//		{
//			userData = data;
//			return this;
//		}
//
//		@Override
//		public Object getUserData()
//		{
//			return userData;
//		}
//	};

	private boolean IsMyServiceRunning()
	{
		//--
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (ActivityManager.RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE))
		{
			if (GpsTracking.class.getName().equals(
					service.service.getClassName()))
			{
				return true;
			}

		}
		return false;
	}
}