package vn.dungtin.service;

import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;

import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import okhttp3.Response;
import vn.dungtin.db.MLDBHandler;
import vn.dungtin.db.dto.DriverInfoDTO;
import vn.dungtin.db.dto.GpsCurrentLocationDTO;
import vn.dungtin.db.dto.JournalDTO;
import vn.dungtin.db.dto.TaxiOrderDTO;
import vn.dungtin.mldriver.DataProvider;
import vn.dungtin.mldriver.Declaration;
import vn.dungtin.mldriver.MLDriverApp;
import vn.dungtin.mldriver.WorkingSession;
import vn.dungtin.servicestack.ConfigureInfo;
import vn.dungtin.servicestack.DataResultListener;
import vn.dungtin.servicestack.Error;

public class GpsTracking extends Service implements
                                         GoogleApiClient.ConnectionCallbacks,
                                         GoogleApiClient.OnConnectionFailedListener,
                                         com.google.android.gms.location.LocationListener
{
    public GpsTracking()
    {
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    private final String TAG = "GpsTracking";

    private GoogleApiClient theGoogleApiClient;
    private LocationRequest mLocationRequest;
    // current currentLocation
    private Location currentLocation;
    // cached location
    private Location lastLocation;

    //--
    private UIThreadHandler uiThreadHandler;
    private MLDBHandler mldbHandler;

    private Timer scheduler;
    private TimerTask timerTask;
    private final int SCHEDULE_FIRST_RUN = 3 * 1000; //3s
    private int SCHEDULE_INTERVAL = 10 * 1000; //10s
    private final int SCHEDULE_LOW_INTERVAL = 45 * 1000; //45s

    private DataProvider dataProvider;

    private Timer orderGrabber;
    private TimerTask orderGrabberTask;
    private int SCHEDULE_ORDER_GRAB = 5 * 1000; //5s

    @Override
    public void onCreate()
    {
        Log.d(TAG, "onCreate...");
        HandlerThread uiThread = new HandlerThread("UIThreadHandler");//to prepare a looper
        uiThread.start();
        uiThreadHandler = new UIThreadHandler(uiThread.getLooper()); //to process UI such as AlertDialog,...
        //---
        mldbHandler = new MLDBHandler(getApplicationContext());
        dataProvider = new DataProvider();

        //---
        Log.d(TAG, "[onCreate] Setup connection to Google API...");
        theGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mLocationRequest = new LocationRequest();
        Log.d(TAG, "[onCreate] Create location request...");
        setHIGHLocationRequest();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        Log.d(TAG, "[onStartCommand] Starting...");
        Log.d(TAG, "[onStartCommand] Connecting to Google API...");
        theGoogleApiClient.connect();
        //--
        try
        {
            SCHEDULE_ORDER_GRAB = Integer.parseInt(
                    mldbHandler.getRuntimeValue("PullOrderPeriod")) * 1000;
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }

        try
        {
            SCHEDULE_INTERVAL = Integer.parseInt(
                    mldbHandler.getRuntimeValue("UpdateLocationPeriod")) * 1000;
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        //--
        registerScheduleTask();
        //--
        registerReceiver(
                forceGrabOrderReceiver,
                new IntentFilter(Declaration.MSG_FORCE_GRAB_ORDER)
        );

        return START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        Log.d(TAG, "onDestroy...");
        stopGPS();
        //--
        if (scheduler != null)
            scheduler.cancel();
        if (orderGrabber != null)
            orderGrabber.cancel();
        //--
        unregisterReceiver(forceGrabOrderReceiver);
    }

    private void registerScheduleTask()
    {
        Log.d(TAG, "[registerScheduleTask] SCHEDULE_FIRST_RUN");
        if (scheduler != null)
            scheduler.cancel();
        //--
        scheduler = new Timer();
        timerTask = new UpdateLocationTask();
        scheduler.schedule(
                timerTask,
                SCHEDULE_FIRST_RUN, //first run after 3s
                SCHEDULE_INTERVAL
        ); //next run each 10s after first run
        //---
        if (orderGrabber != null)
            orderGrabber.cancel();
        //--
        orderGrabber = new Timer();
        orderGrabberTask = new GrabOrderTask();
        orderGrabber.schedule(
                orderGrabberTask,
                SCHEDULE_ORDER_GRAB,
                SCHEDULE_ORDER_GRAB
        );
    }

    private void registerLOWScheduleTask()
    {
        Log.d(TAG, "[registerLOWScheduleTask] SCHEDULE_LOW_INTERVAL");
        if (scheduler != null)
            scheduler.cancel();
        //--
        scheduler = new Timer();
        timerTask = new UpdateLocationTask();
        scheduler.schedule(
                timerTask,
                SCHEDULE_LOW_INTERVAL, //first run after 45s
                SCHEDULE_LOW_INTERVAL
        ); //next run each 45s after first run
        //---
        if (orderGrabber != null)
            orderGrabber.cancel();
        //--
        if (WorkingSession.getInstance().getCurrentJournal() != null)
        {
            int x = mldbHandler.updateAllOrder2Missing(
                    WorkingSession.getInstance().getCurrentJournal().JournalId);
            Log.d(TAG, "[registerLOWScheduleTask] updateAllOrder2Missing: " + x);
        }
    }

    private boolean isTaskRunning = false;

    private class UpdateLocationTask extends TimerTask
    {
        private final String TAG = "GpsTrackingTimer";

        @Override
        public void run()
        {
            if (isTaskRunning)
                return;
            isTaskRunning = true;
            //Log.d(TAG, "------- let's go--------");
            try
            {
                doScheduleTask();
            }
            catch (Exception ex)
            {
                Log.e(TAG, "[timerTask] EXCEPTION at run(). " + ex.getMessage());
                isTaskRunning = false;
            }
        }
    }

    private void doScheduleTask()
    {
        Log.d(TAG, "doScheduleTask...");
        if (!isMyFriendActive())
        {
            if (mLocationRequest.getPriority() == LocationRequest.PRIORITY_HIGH_ACCURACY)
            {
//				setLOWLocationRequest();
//				if(theGoogleApiClient.isConnected())
//					restartLocationUpdate();
//				registerLOWScheduleTask();
                uiThreadHandler.sendEmptyMessage(UIThreadHandler.MSG_SERVICE_LOW_MODE);
            }
        }
        else
        {
            if (mLocationRequest.getPriority() == LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
            {
//				setHIGHLocationRequest();
//				if(theGoogleApiClient.isConnected())
//					restartLocationUpdate();
//				registerScheduleTask();
                uiThreadHandler.sendEmptyMessage(UIThreadHandler.MSG_SERVICE_HIGH_MODE);
            }
        }

        if (!theGoogleApiClient.isConnected())
        {
            Log.e(TAG, "[doScheduleTask] Google API not connect yet");
            uiThreadHandler.sendEmptyMessage(UIThreadHandler.MSG_GPS_NOT_ENABLED);
            isTaskRunning = false;
            return;
        }
        //--
        if (currentLocation == null)
        {
            Log.e(TAG, "[doScheduleTask] Location Service does not ready.");
            uiThreadHandler.sendEmptyMessage(UIThreadHandler.MSG_GPS_NOT_ENABLED);
            isTaskRunning = false;
            return;
        }

        if (WorkingSession.getInstance().getCurrentDriver() == null)
        {
            Log.e(TAG, "[doScheduleTask] " +
                       "WorkingSession.getInstance().getCurrentDriver() == null");
            isTaskRunning = false;
            return;
        }
        //--
        GpsCurrentLocationDTO gcl = new GpsCurrentLocationDTO();
        gcl.Accuracy = currentLocation.getAccuracy();
        gcl.Altitude = currentLocation.getAltitude();
        gcl.Bearing = currentLocation.getBearing();
        gcl.JournalID = WorkingSession.getInstance().getCurrentJournal().JournalId;
        gcl.Latitude = currentLocation.getLatitude();
        gcl.Longitude = currentLocation.getLongitude();
        gcl.Speed = currentLocation.getSpeed();
        //--
        Log.d(TAG, "[doScheduleTask] Update current driver location: " + gcl.toString());
        dataProvider.doUpdateLocationAsync(
                WorkingSession.getInstance().getCurrentDriver().ClientId,
                gcl,
                updateLocationResult
        );
    }

    @Override
    public void onConnected(Bundle connectionHint)
    {
        // Connected to Google Play services!
        // The good stuff goes here.
        Log.d(TAG, "Google API connected");

        lastLocation = LocationServices.FusedLocationApi.getLastLocation(
                theGoogleApiClient);
        if (lastLocation != null)
        {
            Log.d(TAG, String.format("Last location: (%s, %s)",
                                     String.valueOf(lastLocation.getLatitude()),
                                     String.valueOf(lastLocation.getLongitude())
            ));
        }

        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int cause)
    {
        // The connection has been interrupted.
        // Disable any UI components that depend on Google APIs
        // until onConnected() is called.
        Log.d(TAG, "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result)
    {
        Log.d(TAG, "onConnectionFailed");
        Log.d(TAG, "Unable to connect to Google API. Error code: " + result.getErrorCode());
        // This callback is important for handling errors that
        // may occur while attempting to connect with Google.

    }

    @Override
    public void onLocationChanged(Location location)
    {
        if (location != null)
        {
            this.currentLocation = location;
            mldbHandler.addOrUpdateRuntimeValue("GpsStatus", "OK");
            Gson gson = new Gson();
            Log.d(TAG, "onLocationChanged: " +
                       gson.toJson(this.currentLocation));
            if (uiThreadHandler != null)
            {
                Message msg = new Message();
                msg.what = UIThreadHandler.MSG_GPS_LOCATION_CHANGED;
                uiThreadHandler.sendMessage(msg);
            }
        }
    }

    protected void startLocationUpdates()
    {
        LocationServices.FusedLocationApi.requestLocationUpdates(theGoogleApiClient,
                                                                 mLocationRequest, this
        );
    }

    protected void restartLocationUpdate()
    {
        Log.d(TAG, "[restartLocationUpdate]...");
        LocationServices.FusedLocationApi.removeLocationUpdates(
                theGoogleApiClient, this);
        //--
        LocationServices.FusedLocationApi.requestLocationUpdates(theGoogleApiClient,
                                                                 mLocationRequest, this
        );
    }

    protected void setHIGHLocationRequest()
    {
        Log.d(TAG, "[setHIGHLocationRequest] interval: 5s, fastest-interval: 3s, " +
                   "PRIORITY_HIGH_ACCURACY");
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(3000);
        mLocationRequest.setSmallestDisplacement(1f); //update each 1m distance
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        //--

    }

    protected void setLOWLocationRequest()
    {
        Log.d(TAG, "[setLOWLocationRequest] interval: 3min, fastest-interval: 1min, " +
                   "PRIORITY_BALANCED_POWER_ACCURACY");
        mLocationRequest.setInterval(180 * 1000); //3 min
        mLocationRequest.setFastestInterval(60 * 1000); //1 min
        mLocationRequest.setSmallestDisplacement(20); //update each 20m distance
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        //--

    }

    public void stopGPS()
    {
        try
        {
            Log.d(TAG, "stopGPS...");
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    theGoogleApiClient, this);
            theGoogleApiClient.disconnect();
        }
        catch (Exception ex)
        {
            Log.e(TAG, "[stopGPS] " + ex);
        }

        mldbHandler.addOrUpdateRuntimeValue("GpsStatus", "Stop");
    }

    private final class UIThreadHandler extends Handler
    {
        private final String TAG = "GpsUIThreadHandler";
        public static final int MSG_GPS_LOCATION_CHANGED = 0x13;
        public static final int MSG_GPS_NOT_ENABLED = 0x14;
        public static final int MSG_SERVICE_LOW_MODE = 0x15;
        public static final int MSG_SERVICE_HIGH_MODE = 0x16;

        public UIThreadHandler(Looper lp)
        {
            super(lp);
        }

        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case UIThreadHandler.MSG_GPS_LOCATION_CHANGED:
                {
                    try
                    {
                        //Log.d(TAG, "MSG_GPS_LOCATION_CHANGED...");
                        Location x = currentLocation;
                        ShowToast(String.format("(%.4f, %.4f) accuracy: %.2f",
                                                x.getLatitude(),
                                                x.getLongitude(),
                                                x.getAccuracy()
                                  )
                        );
                        long rs;
                        JournalDTO journal = WorkingSession.getInstance().getCurrentJournal();
                        if (journal == null)
                        {
                            Log.e(TAG, "[MSG_GPS_LOCATION_CHANGED] " +
                                       "WorkingSession.getInstance().getCurrentJournal NULL");
                            break;
                        }
                        if (journal.Latitude == 0 && journal.Longitude == 0)
                        {
                            journal.Latitude = x.getLatitude();
                            journal.Longitude = x.getLongitude();
                            //--
                            rs = mldbHandler.updateJournal(journal);
                            Log.d(TAG, "[MSG_GPS_LOCATION_CHANGED] Update journal GPS: " + rs);
                        }
                        GpsCurrentLocationDTO gcl = new GpsCurrentLocationDTO();
                        gcl.Accuracy = x.getAccuracy();
                        gcl.Altitude = x.getAltitude();
                        gcl.Bearing = x.getBearing();
                        gcl.JournalID = journal.JournalId;
                        gcl.Latitude = x.getLatitude();
                        gcl.Longitude = x.getLongitude();
                        gcl.Speed = x.getSpeed();
                        gcl.UpdatedDate = new Date();
                        rs = mldbHandler.addOrUpdateCurrentLocation(gcl);
                        Log.d(TAG, "[MSG_GPS_LOCATION_CHANGED] Update GPS: " + rs);
                        //--send broadcast
                        Intent broadcast = new Intent(
                                Declaration.MSG_GPS_LOCATION_CHANGED);
                        sendBroadcast(broadcast);
                    }
                    catch (Exception ex)
                    {
                        ex.printStackTrace();
                        Log.e(TAG, ex.getMessage());
                    }
                    break;
                }
                case UIThreadHandler.MSG_GPS_NOT_ENABLED:
                {
                    //ShowToast("GPS not enabled");
                    if (!isGPSEnabled())
                    {
                        //--send broadcast
                        Intent broadcast = new Intent(
                                Declaration.MSG_GPS_OFF
                        );
                        sendBroadcast(broadcast);
                    }
                    break;
                }
                case UIThreadHandler.MSG_SERVICE_HIGH_MODE:
                    Log.d(TAG, "[UIThreadHandler.MSG_SERVICE_HIGH_MODE] registerScheduleTask();");

                    setHIGHLocationRequest();
                    if (theGoogleApiClient.isConnected())
                        restartLocationUpdate();
                    registerScheduleTask();
                    break;
                case UIThreadHandler.MSG_SERVICE_LOW_MODE:
                    Log.d(TAG, "[UIThreadHandler.MSG_SERVICE_LOW_MODE] registerScheduleTask();");

                    setLOWLocationRequest();
                    if (theGoogleApiClient.isConnected())
                        restartLocationUpdate();
                    registerLOWScheduleTask();
                    break;
            }

        }

        private Toast currentToast;

        private void ShowToast(String text)
        {
            if (isMyFriendActive())
            {
                currentToast = Toast.makeText(getApplicationContext(), text,
                                              Toast.LENGTH_SHORT
                );
                currentToast.show();
                //---
                uiThreadHandler.postDelayed(new Runnable()
                                            {
                                                @Override
                                                public void run()
                                                {
                                                    currentToast.cancel();
                                                }
                                            },
                                            700
                ); //700 milliseconds
            }
        }
    }

    private boolean isMyFriendActive()
    {
        ActivityManager am = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        // get the info from the currently running task
        List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);

        String activeName = taskInfo.get(0).topActivity.getClassName();
        Log.d(TAG, "CURRENT Activity: " + activeName);

        if (activeName.contains("vn.dungtin.mldriver"))
            return true;
        return false;
    }

    private DataResultListener updateLocationResult = new DataResultListener()
    {
        @Override
        public void onFailed(String caller, final Exception ex)
        {
            Log.e(TAG, "[updateLocationResult] " + ex.getMessage());

            try
            {
                if (WorkingSession.getInstance().getCurrentDriver() == null)
                    throw new Exception("No driver logged on");

                //---
                DriverInfoDTO driver = WorkingSession.getInstance().getCurrentDriver();
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
            catch (Exception exx)
            {
                Log.e(TAG, "[updateLocationResult] " + exx.getMessage());
                isTaskRunning = false;
            }
        }

        @Override
        public void onSuccess(String caller, final Response response)
        {
            //{"Driver":{"DriverStatus":1},"CurrentOrder":null,"WaitingOrders":[]}
            try
            {
                Gson gson = new Gson();
                String result = response.body().string();
                Log.d(TAG, "[updateLocationResult] " + result);
                //--
                int x = parseResultAndUpdateData(result);
                if (x > 0) //waiting order found
                {
                    //--send broadcast
                    Intent broadcast = new Intent(
                            Declaration.MSG_WAITING_ORDER_UPDATED
                    );
                    sendBroadcast(broadcast);
                }
            }
            catch (Exception ex)
            {
                Log.e(TAG, "[updateLocationResult] " + ex.getMessage());
                ex.printStackTrace();
            }

            isTaskRunning = false;
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

    private boolean isGrabbing = false;

    private class GrabOrderTask extends TimerTask
    {
        @Override
        public void run()
        {
            if (isGrabbing)
                return;
            if (WorkingSession.getInstance().getCurrentDriver() == null)
            {
                Log.e(TAG, "[orderGrabberTask] No logged driver found.");
                return;
            }
            isGrabbing = true;
            Log.d(TAG, "---------------[orderGrabberTask] GRAB WAITING ORDER....");
            try
            {
                DataProvider dp = new DataProvider();
                dp.doGetWaitingOrderAsync(
                        WorkingSession.getInstance().getCurrentDriver().ClientId,
                        getWaitingOrderResult
                );
            }
            catch (Exception ex)
            {
                Log.e(TAG, "[orderGrabberTask] " + ex.getMessage());
                isGrabbing = false;
            }
        }
    }

    ;

    private DataResultListener getWaitingOrderResult = new DataResultListener()
    {
        @Override
        public void onFailed(String caller, Exception ex)
        {
            Log.e(TAG, "[getWaitingOrderResult.onFailed] " + ex);
            if (WorkingSession.getInstance().getCurrentJournal() != null)
            {
                int x = mldbHandler.updateAllOrder2Missing(
                        WorkingSession.getInstance().getCurrentJournal().JournalId
                );
                Log.d(TAG, "[getWaitingOrderResult.onFailed] updateAllOrder2Missing: " + x);
            }
            isGrabbing = false;
        }

        @Override
        public void onSuccess(String caller, Response response)
        {

            try
            {
                //Gson gson = new Gson();
                String result = response.body().string();
                Log.d(TAG, result);
                //--
                int x = parseResultAndUpdateData(result);
                //--send broadcast
                Intent broadcast = new Intent(
                        Declaration.MSG_WAITING_ORDER_UPDATED
                );
                sendBroadcast(broadcast);
            }
            catch (Exception ex)
            {
                Log.e(TAG, "[getWaitingOrderResult.onSuccess] " + ex);
            }
            isGrabbing = false;
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

    private BroadcastReceiver forceGrabOrderReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            Log.d(TAG, "[forceGrabOrderReceiver.onReceive] " + intent.getAction());
            orderGrabberTask.run();
        }
    };

    private DataResultListener loginResult = new DataResultListener()
    {
        @Override
        public void onFailed(String caller, final Exception ex)
        {
            Log.e(TAG, "[loginResult.onFailed] " + ex.getMessage());
            isTaskRunning = false;
        }

        @Override
        public void onSuccess(String caller, final Response response)
        {
            //{"Driver":{"DriverId":10,"TaxiNo":1234,"StaffCard":4026,"DriverName":"Nguy?n Hoàng Dung"}}

            try
            {
                //Gson gson = new Gson();
                String result = response.body().string();
                Log.d(TAG, result);
                //--
                int x = parseResultAndUpdateData(result);
                if (x > 0) //waiting order found
                {
                    //--send broadcast
                    Intent broadcast = new Intent(
                            Declaration.MSG_WAITING_ORDER_UPDATED
                    );
                    sendBroadcast(broadcast);
                }
            }
            catch (Exception ex)
            {
                Log.e(TAG, "[doLogin] " + ex.getMessage());
                ex.printStackTrace();
            }

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

    private int parseResultAndUpdateData(String resultText)
    {
        AtomicReference<DriverInfoDTO> arDriver = new AtomicReference<>();
        AtomicReference<TaxiOrderDTO> arCurentOrder = new AtomicReference<>();
        AtomicReferenceArray<TaxiOrderDTO> arWaitingOrders =
                new AtomicReferenceArray<TaxiOrderDTO>(100);
        AtomicReference<Error> arError = new AtomicReference<>();
        AtomicReference<ConfigureInfo> arConfig = new AtomicReference<>();

        if (!WorkingSession.getInstance().parseResultData(
                resultText, arDriver, arCurentOrder, arWaitingOrders, arConfig, arError))
        {
            Log.e(TAG, "[parseResultAndUpdateData] Invalid data received.");
            int xx = mldbHandler.updateAllOrder2Missing(
                    WorkingSession.getInstance().getCurrentJournal().JournalId
            );
            Log.d(TAG, "[parseResultAndUpdateData] updateAllOrder2Missing: " + xx);
            Intent broadcast = new Intent(
                    Declaration.MSG_WAITING_ORDER_UPDATED
            );
            sendBroadcast(broadcast);
            return -1;
        }
        if (arDriver.get() == null)
        {
            Log.e(TAG, "[parseResultAndUpdateData] Could not get driver info, may be error occurred.");
            return -2;
        }

        DriverInfoDTO driver = WorkingSession.getInstance().getCurrentDriver();
        driver.Status = arDriver.get().Status;
        driver.DriverId = arDriver.get().DriverId;
        driver.FullName = arDriver.get().FullName;
        driver.ClientId = arDriver.get().ClientId;
        driver.Status = arDriver.get().Status;
        //--
        long xx;
        xx = mldbHandler.addOrUpdateDriver(driver);
        //--
        mldbHandler.updateAllOrder2Missing(
                WorkingSession.getInstance().getCurrentJournal().JournalId
        );
        //--
        int i = 0;
        TaxiOrderDTO orderDTO;

        while ((orderDTO = arWaitingOrders.get(i)) != null)
        {
            mldbHandler.addOrUpdateOrder(orderDTO);
            i++;
        }
        Log.d(TAG, "[parseResultAndUpdateData] Waiting orders: " + i);
        //--
        if (arCurentOrder.get() != null)
        {
            TaxiOrderDTO order = arCurentOrder.get();
            order.JournalId =
                    (int) WorkingSession.getInstance().getCurrentJournal().JournalId;
            order.DriverId =
                    WorkingSession.getInstance().getCurrentDriver().DriverId;

            WorkingSession.getInstance().setCurrentOrder(order);

        }
        else
            WorkingSession.getInstance().setCurrentOrder(null);

        if (i == 0) //no waiting order found
        {
            xx = mldbHandler.updateAllOrder2Missing(
                    WorkingSession.getInstance().getCurrentJournal().JournalId
            );
            Log.d(TAG, "[getWaitingOrderResult.onSuccess] updateAllOrder2Missing: " + xx);
        }
        return i;
    }

    private boolean isGPSEnabled()
    {
        Context ctx = MLDriverApp.getAppContext();
        LocationManager locationManager = (LocationManager) ctx.getSystemService(ctx.LOCATION_SERVICE);
        // getting GPS status
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
}
