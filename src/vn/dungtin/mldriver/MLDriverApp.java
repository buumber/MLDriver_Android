package vn.dungtin.mldriver;

import android.app.Application;
import android.content.Context;

import microsoft.aspnet.signalr.client.Platform;
import microsoft.aspnet.signalr.client.http.android.AndroidPlatformComponent;

/**
 * Created by buumb on 5/27/2016.
 */
public class MLDriverApp extends Application
{

	private static Context context;

	public void onCreate()
	{
		super.onCreate();
		MLDriverApp.context = getApplicationContext();

		Platform.loadPlatformComponent(new AndroidPlatformComponent());
	}

	public static Context getAppContext()
	{
		return MLDriverApp.context;
	}
}