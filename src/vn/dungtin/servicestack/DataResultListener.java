package vn.dungtin.servicestack;

import okhttp3.Response;

/**
 * Created by buumb on 5/27/2016.
 */
public interface DataResultListener
{
	void onFailed(final String caller, final Exception ex);

	void onSuccess(final String caller, final Response response);

	DataResultListener setUserData(Object data);

	Object getUserData();
}
