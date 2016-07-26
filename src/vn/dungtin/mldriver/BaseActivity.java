package vn.dungtin.mldriver;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.w3c.dom.Text;

import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import okhttp3.Response;
import vn.dungtin.db.MLDBHandler;
import vn.dungtin.db.dto.DriverInfoDTO;
import vn.dungtin.db.dto.GpsCurrentLocationDTO;
import vn.dungtin.db.dto.TaxiOrderDTO;
import vn.dungtin.servicestack.ConfigureInfo;
import vn.dungtin.servicestack.DataResultListener;
import vn.dungtin.servicestack.Error;

public abstract class BaseActivity extends AppCompatActivity
{
	protected final String TAG = "BaseActivity";
	protected MLDBHandler mldbHandler;
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		if(WorkingSession.getInstance().getCurrentDriver() == null)
		{
			Intent next = new Intent(this, Login.class);
			next.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(next);
			finish();
		}
		//--
		mldbHandler = new MLDBHandler(this);
	}

	/**
	 * This function will be called when the driver accepted order
	 */
	protected abstract void updateOrderStatus();
	protected abstract void doRegisterBroadcast();
	protected abstract void doUnregisterBroadcast();

	protected void InitHeaderBar()
	{
		ImageButton btn = (ImageButton)findViewById(R.id.btnSetting);
		if(btn != null)
		{
			//---open setting window
		}
		btn = (ImageButton)findViewById(R.id.btnExit);
		if(btn != null)
		{

			btn.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					if(WorkingSession.getInstance().getCurrentOrder() != null)
					{
						Log.e("BaseActivity", "Cannot logout when have order!!!");
						WorkingSession.showErrorDialog(
								BaseActivity.this,
								"Không cho phép",
								"Bạn không thể đăng xuất khi đang chở hoặc đón khách",
								null
						);
						return;
					}
					final DataProvider dp = new DataProvider();
					dp.doLogoutAsync(
							WorkingSession.getInstance().getCurrentDriver().ClientId,
							new DataResultListener() {
								@Override
								public void onFailed(String caller, Exception ex) {

								}

								@Override
								public void onSuccess(String caller, Response response) {
									dp.doClearSessionAsync(
											WorkingSession.getInstance().getCurrentDriver().ClientId,
											null
									);

									WorkingSession.getInstance()
											.setCurrentDriver(null)
											.setCurrentJournal(null);
									//--
									Intent next = new Intent(BaseActivity.this, Login.class);
									next.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
											Intent.FLAG_ACTIVITY_NEW_TASK);
									startActivity(next);
									finish();
								}

								@Override
								public DataResultListener setUserData(Object data) {
									return null;
								}

								@Override
								public Object getUserData() {
									return null;
								}
							}
					);

				}
			});
		}
		btn = (ImageButton)findViewById(R.id.btnHome);
		if(btn != null)
		{
			btn.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					Intent next = new Intent(BaseActivity.this, Main.class);
					next.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(next);
					finish();
				}
			});
		}
		//--
		TextView textView = (TextView)findViewById(R.id.tvDriverID);
		if(textView != null)
			textView.setText(
					String.format("Mã NV: %s", WorkingSession.getInstance()
											.getCurrentDriver().StaffCardNumber
					)
			);
		textView = (TextView)findViewById(R.id.tvDriverName);
		if(textView != null) {
//			textView.setText(WorkingSession.getInstance()
//							.getCurrentDriver().FullName
//			);
			textView.setText(String.format("Số tài: %s",
					WorkingSession.getInstance().getCurrentDriver().CarNumber));
		}

	}

	protected LinearLayout viewFinishOrder;
	protected LinearLayout viewAcceptOrder;
	protected void InitFooterOrder()
	{
		viewAcceptOrder = (LinearLayout) findViewById(R.id.viewAcceptOrder);
		viewFinishOrder = (LinearLayout) findViewById(R.id.viewFinishOrder);
		if(viewFinishOrder != null)
			viewFinishOrder.setVisibility(View.GONE);
		if(viewAcceptOrder != null)
			viewAcceptOrder.setVisibility(View.GONE);
		//---
		Button btnAccept = (Button) findViewById(R.id.btnAccept);
		if(btnAccept != null)
		{
			btnAccept.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					DataProvider dp = new DataProvider();
					WorkingSession.getInstance().showProcessDialog(BaseActivity.this);
					dp.doAcceptOrderAsync(
							WorkingSession.getInstance().getCurrentDriver().ClientId,
							WorkingSession.getInstance().getCurrentOrder().OrderId,
							acceptOrderResult
					);
				}
			});
		}
		Button btnCustNotFound = (Button) findViewById(R.id.btnCustNotFound);
		if(btnCustNotFound != null)
		{
			btnCustNotFound.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					AlertDialog confirm = new AlertDialog.Builder(BaseActivity.this)
							.setMessage("Bạn có chắc là không tìm thấy khách?")
							.setTitle("Xác nhận")
							.setCancelable(false)
							.setNeutralButton("Chắc chắn", new DialogInterface.OnClickListener()
							{
								@Override
								public void onClick(DialogInterface dialog, int which)
								{
									dialog.dismiss();
									//--
									DataProvider dp = new DataProvider();
									WorkingSession.getInstance().showProcessDialog(BaseActivity.this);
									doUnregisterBroadcast();
									dp.doCustomerNotFoundAsync(
											WorkingSession.getInstance().getCurrentDriver().ClientId,
											customerNotFoundResult
									);
								}
							})
							.setNegativeButton("Không", new DialogInterface.OnClickListener()
							{
								@Override
								public void onClick(DialogInterface dialog, int which)
								{
									dialog.dismiss();
								}
							})
							.create();
					confirm.show();

				}
			});
		}
		Button btnCancel = (Button) findViewById(R.id.btnCancel);
		if(btnCancel != null)
		{
			btnCancel.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					AlertDialog confirm = new AlertDialog.Builder(BaseActivity.this)
							.setMessage("Bạn chắc là sẽ huỷ chuyến này chứ?")
							.setTitle("Xác nhận")
							.setCancelable(false)
							.setNeutralButton("Chắc chắn", new DialogInterface.OnClickListener()
							{
								@Override
								public void onClick(DialogInterface dialog, int which)
								{
									dialog.dismiss();
									//--
									DataProvider dp = new DataProvider();
									WorkingSession.getInstance().showProcessDialog(BaseActivity.this);
									doUnregisterBroadcast();
									dp.doCancelOrderdAsync(
											WorkingSession.getInstance().getCurrentDriver().ClientId,
											cancelOrderResult
									);
								}
							})
							.setNegativeButton("Không", new DialogInterface.OnClickListener()
							{
								@Override
								public void onClick(DialogInterface dialog, int which)
								{
									dialog.dismiss();
								}
							})
							.create();
					confirm.show();
				}
			});
		}
		Button btnFinish = (Button) findViewById(R.id.btnFinish);
		if(btnFinish != null)
		{
			btnFinish.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					AlertDialog confirm = new AlertDialog.Builder(BaseActivity.this)
							.setMessage("Xác nhận trả khách tại đây?")
							.setTitle("Trả khách")
							.setCancelable(false)
							.setNeutralButton("Chắc chắn", new DialogInterface.OnClickListener()
							{
								@Override
								public void onClick(DialogInterface dialog, int which)
								{
									dialog.dismiss();
									//--
									DataProvider dp = new DataProvider();
									WorkingSession.getInstance().showProcessDialog(BaseActivity.this);
									doUnregisterBroadcast();
									//--
									dp.doFinishOrderAsync(
											WorkingSession.getInstance().getCurrentDriver().ClientId,
											WorkingSession.getInstance().getCurrentOrder().OrderId,
											finishOrderResult
									);
								}
							})
							.setNegativeButton("Không", new DialogInterface.OnClickListener()
							{
								@Override
								public void onClick(DialogInterface dialog, int which)
								{
									dialog.dismiss();
								}
							})
							.create();
					confirm.show();
				}
			});
		}
	}

	private DataResultListener acceptOrderResult = new DataResultListener()
	{
		@Override
		public void onFailed(String caller, Exception ex)
		{
			Log.e(TAG, "[acceptOrderResult.onFailed] " + ex);
			WorkingSession.getInstance().hideProcessDialog(BaseActivity.this);
			//--
			WorkingSession.showErrorDialog(
					BaseActivity.this,
					"Xảy ra lỗi",
					"Không thể xác nhận đón khách, xin thử lại. \n" + ex.getMessage(),
					null
			);
			doRegisterBroadcast();
		}

		@Override
		public void onSuccess(String caller, Response response)
		{
			WorkingSession.getInstance().hideProcessDialog(BaseActivity.this);
			try
			{
				String result = response.body().string();
				Log.d(TAG, "[acceptOrderResult.onSuccess] " + result);
				//--
				AtomicReference<DriverInfoDTO> arDriver = new AtomicReference<>();
				AtomicReference<TaxiOrderDTO> arCurrentOrder = new AtomicReference<>();
				AtomicReferenceArray<TaxiOrderDTO> arWaitingOrders =
						new AtomicReferenceArray<TaxiOrderDTO>(100);
				AtomicReference<Error> arError = new AtomicReference<>();
				AtomicReference<ConfigureInfo> arConfig = new AtomicReference<>();

				if(!WorkingSession.getInstance().parseResultData(
						result, arDriver, arCurrentOrder, arWaitingOrders, arConfig, arError ))
				{
					WorkingSession.showErrorDialog(
							BaseActivity.this,
							"Xảy ra lỗi",
							"Không thể xác nhận đón khách, xin thử lại sau. [Code: 500]" +
									arError.get() != null? "\n" + arError.get().Message : "",
							null
					);
					return;
				}

				if(arDriver.get() == null)
				{
					WorkingSession.showErrorDialog(
							BaseActivity.this,
							"Xảy ra lỗi",
							"Không thể lấy thông tin lái xe, xin liên hệ bộ phận hỗ trợ",
							null
					);
					return;
				}

				WorkingSession.getInstance().getCurrentDriver().Status =
						arDriver.get().Status;
				mldbHandler.addOrUpdateDriver(
						WorkingSession.getInstance().getCurrentDriver());
				//--
				if(arCurrentOrder.get() == null)
				{
					WorkingSession.showErrorDialog(
							BaseActivity.this,
							"Xảy ra lỗi",
							"Không thể xác nhận đón khách, xin thử lại. [Code: 1000]",
							null
					);
				}

				TaxiOrderDTO currentSelectedOrder = WorkingSession.getInstance().getCurrentOrder();
				TaxiOrderDTO order = arCurrentOrder.get();
				currentSelectedOrder.OrderPhone =
						order.OrderPhone == null ? "" : order.OrderPhone;
				currentSelectedOrder.OrderStatus = order.OrderStatus;
				currentSelectedOrder.AcceptedDate = order.AcceptedDate;
				currentSelectedOrder.TaxiType = order.TaxiType;
				mldbHandler.addOrUpdateOrder(currentSelectedOrder);

				WorkingSession.getInstance().setCurrentOrder(currentSelectedOrder);
				showFinishView();
				updateOrderStatus();
			}
			catch (Exception ex)
			{
				Log.e(TAG, "[acceptOrderResult.onSuccess] " + ex);
				WorkingSession.showErrorDialog(
						BaseActivity.this,
						"Xảy ra lỗi",
						"Không thể xác nhận đón khách, xin thử lại. [Code:1001]\n" + ex.getMessage(),
						null
				);
			}
		}

		@Override
		public DataResultListener setUserData(Object data)
		{
			return null;
		}

		@Override
		public Object getUserData()
		{
			return null;
		}
	};

	private DataResultListener customerNotFoundResult = new DataResultListener()
	{
		@Override
		public void onFailed(String caller, Exception ex)
		{
			Log.e(TAG, "[customerNotFoundResult.onFailed] " + ex);

			WorkingSession.getInstance().hideProcessDialog(BaseActivity.this);
			//--
			WorkingSession.showErrorDialog(
					BaseActivity.this,
					"Xảy ra lỗi",
					"Không thể thực hiện thao tác này, xin thử lại. \n" + ex.getMessage(),
					null
			);
			doRegisterBroadcast();
		}

		@Override
		public void onSuccess(String caller, Response response)
		{
			//{"Driver":{"DriverStatus":1},"CurrentOrder":{"OrderId":0}}

			WorkingSession.getInstance().hideProcessDialog(BaseActivity.this);
			try
			{
				String result = response.body().string();
				Log.d(TAG, "[customerNotFoundResult.onSuccess] " + result);
				//--

				AtomicReference<DriverInfoDTO> arDriver = new AtomicReference<>();
				AtomicReference<TaxiOrderDTO> arCurrentOrder = new AtomicReference<>();
				AtomicReferenceArray<TaxiOrderDTO> arWaitingOrders =
						new AtomicReferenceArray<TaxiOrderDTO>(100);
				AtomicReference<Error> arError = new AtomicReference<>();
				AtomicReference<ConfigureInfo> arConfig = new AtomicReference<>();

				if(!WorkingSession.getInstance().parseResultData(
						result, arDriver, arCurrentOrder, arWaitingOrders, arConfig, arError ))
				{
					WorkingSession.showErrorDialog(
							BaseActivity.this,
							"Xảy ra lỗi",
							"Không thể thực hiện thao tác này, xin thử lại sau. [Code: 501]" +
									arError.get() != null? "\n" + arError.get().Message : "",
							null
					);
					return;
				}

				if(arDriver.get() == null)
				{
					WorkingSession.showErrorDialog(
							BaseActivity.this,
							"Xảy ra lỗi",
							"Không thể lấy thông tin lái xe, xin liên hệ bộ phận hỗ trợ",
							null
					);
					return;
				}

				WorkingSession.getInstance().getCurrentDriver().Status =
						arDriver.get().Status;
				mldbHandler.addOrUpdateDriver(
						WorkingSession.getInstance().getCurrentDriver());
				//--

				TaxiOrderDTO currentSelectedOrder = WorkingSession.getInstance().getCurrentOrder();

				currentSelectedOrder.OrderStatus = 5; //--not found
				mldbHandler.addOrUpdateOrder(currentSelectedOrder);

				WorkingSession.getInstance().setCurrentOrder(null);
				goToWaitingList();
			}
			catch (Exception ex)
			{
				Log.e(TAG, "[customerNotFoundResult.onSuccess] " + ex);
				WorkingSession.showErrorDialog(
						BaseActivity.this,
						"Xảy ra lỗi",
						"Không thể thực hiện thao tác này, xin thử lại sau. [Code: 1000]]\n" + ex.getMessage(),
						null
				);
			}
		}

		@Override
		public DataResultListener setUserData(Object data)
		{
			return null;
		}

		@Override
		public Object getUserData()
		{
			return null;
		}
	};

	private DataResultListener cancelOrderResult = new DataResultListener()
	{
		@Override
		public void onFailed(String caller, Exception ex)
		{
			Log.e(TAG, "[cancelOrderResult.onFailed] " + ex);
			WorkingSession.getInstance().hideProcessDialog(BaseActivity.this);
			//--
			WorkingSession.showErrorDialog(
					BaseActivity.this,
					"Xảy ra lỗi",
					"Không thể thực hiện thao tác này, xin thử lại. \n" + ex.getMessage(),
					null
			);
			doRegisterBroadcast();
		}

		@Override
		public void onSuccess(String caller, Response response)
		{
			//{"Order":{"OrderId":0,"OrderStatus":0}}

			WorkingSession.getInstance().hideProcessDialog(BaseActivity.this);
			try
			{
				String result = response.body().string();
				Log.d(TAG, "[cancelOrderResult.onSuccess] " + result);
				//--
				AtomicReference<DriverInfoDTO> arDriver = new AtomicReference<>();
				AtomicReference<TaxiOrderDTO> arCurrentOrder = new AtomicReference<>();
				AtomicReferenceArray<TaxiOrderDTO> arWaitingOrders =
						new AtomicReferenceArray<TaxiOrderDTO>(100);
				AtomicReference<Error> arError = new AtomicReference<>();
				AtomicReference<ConfigureInfo> arConfig = new AtomicReference<>();

				if(!WorkingSession.getInstance().parseResultData(
						result, arDriver, arCurrentOrder, arWaitingOrders, arConfig, arError ))
				{
					WorkingSession.showErrorDialog(
							BaseActivity.this,
							"Xảy ra lỗi",
							"Không thể thực hiện thao tác này, xin thử lại sau. [Code: 500]" +
									arError.get() != null? "\n" + arError.get().Message : "",
							null
					);
					return;
				}

				if(arDriver.get() == null)
				{
					WorkingSession.showErrorDialog(
							BaseActivity.this,
							"Xảy ra lỗi",
							"Không thể lấy thông tin lái xe, xin liên hệ bộ phận hỗ trợ",
							null
					);
					return;
				}

				WorkingSession.getInstance().getCurrentDriver().Status =
						arDriver.get().Status;
				mldbHandler.addOrUpdateDriver(
						WorkingSession.getInstance().getCurrentDriver());
				//--

				TaxiOrderDTO currentSelectedOrder = WorkingSession.getInstance().getCurrentOrder();

				currentSelectedOrder.OrderStatus = 6; //--cancelled
				mldbHandler.addOrUpdateOrder(currentSelectedOrder);
				mldbHandler.addCancelledOrder(
						currentSelectedOrder.OrderId,
						currentSelectedOrder.JournalId,
						WorkingSession.getInstance().getCurrentDriver().DriverId
				);

				WorkingSession.getInstance().setCurrentOrder(null);
				goToWaitingList();
			}
			catch (Exception ex)
			{
				Log.e(TAG, "[cancelOrderResult.onSuccess] " + ex);
				WorkingSession.showErrorDialog(
						BaseActivity.this,
						"Xảy ra lỗi",
						"Không thể thực hiện thao tác này, xin thử lại sau. [Code: 1000]]\n" + ex.getMessage(),
						null
				);
			}
		}

		@Override
		public DataResultListener setUserData(Object data)
		{
			return null;
		}

		@Override
		public Object getUserData()
		{
			return null;
		}
	};

	private DataResultListener finishOrderResult = new DataResultListener()
	{
		@Override
		public void onFailed(String caller, Exception ex)
		{
			Log.e(TAG, "[finishOrderResult.onFailed] " + ex);
			WorkingSession.getInstance().hideProcessDialog(BaseActivity.this);
			//--
			WorkingSession.showErrorDialog(
					BaseActivity.this,
					"Xảy ra lỗi",
					"Không thể thực hiện thao tác này, xin thử lại. \n" + ex.getMessage(),
					null
			);
			doRegisterBroadcast();
		}

		@Override
		public void onSuccess(String caller, Response response)
		{
			//{"Driver":{"DriverStatus":1},"CurrentOrder":{"OrderId":0}}

			WorkingSession.getInstance().hideProcessDialog(BaseActivity.this);
			try
			{
				Gson gson = new Gson();
				String result = response.body().string();
				Log.d(TAG, "[finishOrderResult.onSuccess] " + result);
				//--
				AtomicReference<DriverInfoDTO> arDriver = new AtomicReference<>();
				AtomicReference<TaxiOrderDTO> arCurrentOrder = new AtomicReference<>();
				AtomicReferenceArray<TaxiOrderDTO> arWaitingOrders =
						new AtomicReferenceArray<TaxiOrderDTO>(100);
				AtomicReference<Error> arError = new AtomicReference<>();
				AtomicReference<ConfigureInfo> arConfig = new AtomicReference<>();

				if(!WorkingSession.getInstance().parseResultData(
						result, arDriver, arCurrentOrder, arWaitingOrders, arConfig, arError ))
				{
					WorkingSession.showErrorDialog(
							BaseActivity.this,
							"Xảy ra lỗi",
							"Không thể thực hiện thao tác này, xin thử lại sau. [Code: 500]" +
									arError.get() != null? "\n" + arError.get().Message : "",
							null
					);
					return;
				}

				if(arDriver.get() == null)
				{
					WorkingSession.showErrorDialog(
							BaseActivity.this,
							"Xảy ra lỗi",
							"Không thể lấy thông tin lái xe, xin liên hệ bộ phận hỗ trợ",
							null
					);
					return;
				}

				WorkingSession.getInstance().getCurrentDriver().Status =
						arDriver.get().Status;
				mldbHandler.addOrUpdateDriver(
						WorkingSession.getInstance().getCurrentDriver());
				//--
				TaxiOrderDTO currentSelectedOrder = WorkingSession.getInstance().getCurrentOrder();

				currentSelectedOrder.OrderStatus = 4; //--finished
				currentSelectedOrder.FinishedDate = new Date();

				GpsCurrentLocationDTO currentLocationDTO = mldbHandler.getCurrentLocation(
						WorkingSession.getInstance().getCurrentJournal().JournalId);
				if(currentLocationDTO != null)
				{
					currentSelectedOrder.FinishedOrderLng = currentLocationDTO.Longitude;
					currentSelectedOrder.FinishedOrderLat = currentLocationDTO.Latitude;
				}

				mldbHandler.addOrUpdateOrder(currentSelectedOrder);

				WorkingSession.getInstance().setCurrentOrder(null);
				goToWaitingList();

			}
			catch (Exception ex)
			{
				Log.e(TAG, "[finishOrderResult.onSuccess] " + ex);
				WorkingSession.showErrorDialog(
						BaseActivity.this,
						"Xảy ra lỗi",
						"Không thể thực hiện thao tác này, xin thử lại sau. [Code: 1000]]\n" + ex.getMessage(),
						null
				);
			}
		}

		@Override
		public DataResultListener setUserData(Object data)
		{
			return null;
		}

		@Override
		public Object getUserData()
		{
			return null;
		}
	};

	protected void showFinishView()
	{
		if(viewAcceptOrder == null || viewFinishOrder == null)
			return;
		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				viewAcceptOrder.setVisibility(View.GONE);
				viewFinishOrder.setVisibility(View.VISIBLE);
			}
		});
	}

	protected void showAcceptView()
	{
		if(viewAcceptOrder == null || viewFinishOrder == null)
			return;
		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				viewAcceptOrder.setVisibility(View.VISIBLE);
				viewFinishOrder.setVisibility(View.GONE);
			}
		});
	}

	protected void goToWaitingList()
	{
		Intent next = new Intent(BaseActivity.this, CustomerWaitingList.class);
		startActivity(next);
		finish();
	}
}
