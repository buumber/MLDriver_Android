package vn.dungtin.servicestack;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by buumb on 5/27/2016.
 */
public abstract class NamedCallback implements Callback
{
	private String CallerName;
	public NamedCallback setCallerName(String callerName)
	{
		CallerName = callerName;
		return this;
	}
	public String getCallerName()
	{
		return CallerName;
	}

	public NamedCallback()
	{

	}

	protected DataResultListener theListener = null;

	public NamedCallback setDataResultListener(DataResultListener listener)
	{
		theListener = listener;
		return this;
	}

	public static NamedCallback build()
	{
		return new NamedCallback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				if (this.theListener != null)
					this.theListener.onFailed(this.getCallerName(), e);
				else
					e.printStackTrace();
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				if (this.theListener != null)
				{
					if (!response.isSuccessful())
						this.theListener.onFailed(this.getCallerName(),
								new IOException("Unexpected code " + response));
					else
						this.theListener.onSuccess(this.getCallerName(), response);
				}
				else if (!response.isSuccessful())
					throw new IOException("Unexpected code " + response);

			}
		};
	}
}
