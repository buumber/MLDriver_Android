package vn.dungtin.mldriver;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import okhttp3.Response;
import vn.dungtin.servicestack.DataResultListener;

public abstract class BaseFragmentActivity extends AppCompatActivity
{
	protected abstract void doRegisterBroadcast();
	protected abstract void doUnregisterBroadcast();

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
	}

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
						Log.e("BaseFragmentActivity", "Cannot logout when have order!!!");
						WorkingSession.showErrorDialog(
								BaseFragmentActivity.this,
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
									Intent next = new Intent(BaseFragmentActivity.this, Login.class);
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
					Intent next = new Intent(BaseFragmentActivity.this, Main.class);
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
		if(textView != null)
			textView.setText(String.format("Số tài: %s",
					WorkingSession.getInstance().getCurrentDriver().CarNumber));
	}
}
