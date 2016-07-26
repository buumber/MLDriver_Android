package vn.dungtin.mldriver;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import okhttp3.Response;
import vn.dungtin.db.MLDBHandler;
import vn.dungtin.db.dto.DriverInfoDTO;
import vn.dungtin.db.dto.JournalDTO;
import vn.dungtin.db.dto.TaxiOrderDTO;
import vn.dungtin.servicestack.ConfigureInfo;
import vn.dungtin.servicestack.DataResultListener;
import vn.dungtin.servicestack.Error;

public class CustomerWaitingList
        extends BaseFragmentActivity
        implements CustomerWaitingGreenItem.OnFragmentInteractionListener
{
    private final String TAG = "CustomerWaitingList";
    private Button btnCustomerCatch;
    private LinearLayout navigationGroup;
    private LinearLayout navigationGroup2;
    private ImageButton btnPrevious;
    private ImageButton btnNext;
    private TextView tvCountdown;
    //--
    private List<TaxiOrderDTO> dataSource;
    private Fragment currentFragment;
    private TaxiOrderDTO currentSelectedOrder;
    //--
    private UIThreadHandler uiThreadHandler;

    private MLDBHandler mldbHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.customerwaitinglist);
        //--
        if (WorkingSession.getInstance().getCurrentOrder() != null)
        {
            Intent next = new Intent(this, PickupCustomer.class);
            startActivity(next);
            finish();
            return;
        }
        //--
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //--
        InitHeaderBar();
        //--
        mldbHandler = new MLDBHandler(this);
        //--
        HandlerThread uiThread = new HandlerThread("UIThreadHandler");//to prepare a looper
        uiThread.start();
        uiThreadHandler = new UIThreadHandler(uiThread.getLooper());
        //--
        dataSource = new ArrayList<>();
        //--
        btnCustomerCatch = (Button) findViewById(R.id.btnCustomerCatch);
        btnCustomerCatch.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                WorkingSession.getInstance().showProcessDialog(CustomerWaitingList.this);
                DataProvider dp = new DataProvider();
                dp.doCreateOrderdAsync(
                        WorkingSession.getInstance().getCurrentDriver().ClientId,
                        createOrderResult
                );
            }
        });
        navigationGroup = (LinearLayout) findViewById(R.id.navigationGroup);
        navigationGroup.setVisibility(View.INVISIBLE);
        navigationGroup2 = (LinearLayout) findViewById(R.id.navigationGroup2);
        navigationGroup2.setVisibility(View.INVISIBLE);
        btnPrevious = (ImageButton) findViewById(R.id.btnPrevious);
        btnPrevious.setOnClickListener(btnPreviousClick);
        btnNext = (ImageButton) findViewById(R.id.btnNext);
        btnNext.setOnClickListener(btnNextClick);
        //--
        tvCountdown = (TextView)findViewById(R.id.tvCountdown);
        tvCountdown.setText("");
        //--
        try {
            COUNTDOWN_MAX = Integer.parseInt(
                    mldbHandler.getRuntimeValue("OrderDisplayPeriod")) + 1;
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        //--


    }

    @Override
    protected void onResume()
    {
        Log.d(TAG, "[onResume] ...");
        super.onResume();

        try {
            COUNTDOWN_MAX = Integer.parseInt(
                    mldbHandler.getRuntimeValue("OrderDisplayPeriod")) + 1;
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }

        doRegisterBroadcast();

        WorkingSession.getInstance().showProcessDialog(this);
        uiThreadHandler.sendEmptyMessage(UIThreadHandler.MSG_GET_ORDER_DB);
        //--
        //checkToRunOrderRoll();
    }

    @Override
    protected void onPause()
    {
        Log.d(TAG, "[onPause] ...");
        super.onPause();

        doUnregisterBroadcast();

        if(countdownTimer != null)
            countdownTimer.cancel();
    }

    private BroadcastReceiver waitingOrderBroadcast = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            Log.d(TAG, "[BroadcastReceiver.onReceive] " + intent.getAction());
            uiThreadHandler.sendEmptyMessage(UIThreadHandler.MSG_GET_ORDER_DB);
        }
    };


    @Override
    public void onSelectOrder(TaxiOrderDTO order)
    {
        currentSelectedOrder = order;
        uiThreadHandler.sendEmptyMessage(UIThreadHandler.MSG_CONFIRM_ORDER);
        if(countdownTimer != null)
            countdownTimer.cancel();
    }


    private final class UIThreadHandler extends Handler
    {
        private final String TAG = "CustomerWaitingHandler";
        public static final int MSG_GET_ORDER_DB = 0x13;
        public static final int MSG_FORCE_GRAB_ORDER = 0x14;
        public static final int MSG_CONFIRM_ORDER = 0x15;
        public static final int MSG_GO_NEXT_ORDER = 0x16;

        public UIThreadHandler(Looper lp)
        {
            super(lp);
        }

        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case UIThreadHandler.MSG_GET_ORDER_DB:
                {
                    //setSwipeRefreshLayoutOptions(true, false);
//					swipeRefreshLayout.setRefreshing(true);
//					swipeRefreshLayout.setEnabled(false);
                    getWaitingOrderFromDB();
                    //setSwipeRefreshLayoutOptions(false, true);
//					swipeRefreshLayout.setRefreshing(false);
//					swipeRefreshLayout.setEnabled(true);
                    break;
                }
                case UIThreadHandler.MSG_FORCE_GRAB_ORDER:
                {
                    //setSwipeRefreshLayoutOptions(true, false);
                    //swipeRefreshLayout.setEnabled(false);
                    Intent broadCast = new Intent(
                            "vn.dungtin.service.MSG_FORCE_GRAB_ORDER"
                    );
                    sendBroadcast(broadCast);
                    try
                    {
                        Thread.sleep(2000);
                    }
                    catch (Exception ex)
                    {
                        ex.printStackTrace();
                    }
                    //setSwipeRefreshLayoutOptions(false, true);
//					swipeRefreshLayout.setRefreshing(false);
//					swipeRefreshLayout.setEnabled(true);
                    break;
                }
                case UIThreadHandler.MSG_CONFIRM_ORDER:
                    if (currentSelectedOrder == null)
                        break;
                    WorkingSession.getInstance().showProcessDialog(CustomerWaitingList.this);

                    DataProvider dp = new DataProvider();
                    dp.doConfirmWaitingOrderAsync(
                            WorkingSession.getInstance().getCurrentDriver().ClientId,
                            currentSelectedOrder.OrderId,
                            confirmOrderResult
                    );
                    break;
                case MSG_GO_NEXT_ORDER:
                    Log.d(TAG, "[MSG_GO_NEXT_ORDER]...");
                    btnNextClick.onClick(null);
                    break;
            }
        }
    }

    private void showCountdown(final String value)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                tvCountdown.setText(value);
            }
        });
    }

    private class OrderRoll extends CountDownTimer
    {
        private final String TAG = "OrderRoll";
        /**
         * @param millisInFuture    The number of millis in the future from the call
         *                          to {@link #start()} until the countdown is done and {@link #onFinish()}
         *                          is called.
         * @param countDownInterval The interval along the way to receive
         *                          {@link #onTick(long)} callbacks.
         */
        public OrderRoll(long millisInFuture, long countDownInterval)
        {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished)
        {
            if(currentFragment != null && currentFragment instanceof CustomerWaitingGreenItem)
            {
                Log.d(TAG, String.format("[countDownTimer.onTick] Remain %d sec of orderId: %d",
                        millisUntilFinished / 1000,
                        ((ICustomerWaitingItem) currentFragment).getDataSource().OrderId));
            }
            showCountdown(String.format("%d",
                    (millisUntilFinished/1000) > 10?10:(millisUntilFinished/1000)));
        }

        @Override
        public void onFinish()
        {
            if(currentFragment != null && currentFragment instanceof CustomerWaitingGreenItem)
            {
                Log.d(TAG, String.format("[countDownTimer.onTick] Remain 0 sec of orderId: %d",
                        ((ICustomerWaitingItem) currentFragment).getDataSource().OrderId));
            }
            showCountdown("");
            uiThreadHandler.sendEmptyMessage(UIThreadHandler.MSG_GO_NEXT_ORDER);
        }
    }

    private  OrderRoll countdownTimer;
    private int COUNTDOWN_MAX = 11;
    private void checkToRunOrderRoll()
    {
        if(countdownTimer != null)
            countdownTimer.cancel();
        if (currentFragment == null || currentFragment instanceof CustomerWaitingGreyItem)
        {
            Log.d(TAG, "[checkToRunOrderRoll] currentFragment instanceof CustomerWaitingGreyItem");
            showCountdown("");
            return;
        }
        //showCountdown("10");
        countdownTimer = new OrderRoll(COUNTDOWN_MAX*1000, 1000);
        countdownTimer.start();
    }


    private void getWaitingOrderFromDB()
    {
        MLDBHandler mldbHandler = new MLDBHandler(CustomerWaitingList.this);
        List<TaxiOrderDTO> orders = mldbHandler.getWaitingOrders(
                WorkingSession.getInstance().getCurrentJournal().JournalId,
                WorkingSession.getInstance().getCurrentDriver().DriverId
        );
        WorkingSession.getInstance().hideProcessDialog(CustomerWaitingList.this);
        int lastCount = dataSource.size();
        Log.d(TAG, "[getWaitingOrderFromDB] Order count: " + lastCount);
        dataSource.clear();
        if (orders != null)
        {
            dataSource.addAll(orders);
            if (currentFragment instanceof CustomerWaitingGreyItem)
            {
                showGreenItem(dataSource.get(0), 0, true, true, SHOW_FRAGMENT_ANIMATION.Left_To_Right);
                checkToRunOrderRoll();
            }
            else
            {
                boolean reuse = false;
                //--find item in new list matched with current
                for (int i = 0; i < dataSource.size(); i++)
                {
                    TaxiOrderDTO x = dataSource.get(i);

                    if (currentFragment != null)
                    {
                        TaxiOrderDTO y = ((ICustomerWaitingItem) currentFragment).getDataSource();
                        Log.d(TAG, String.format("[getWaitingOrderFromDB] Compare Order: %d with %d",
                                y.OrderId, x.OrderId));
                        if(x.OrderId == y.OrderId)
                        {
                            Log.d(TAG, "[getWaitingOrderFromDB] matched order, refresh view with countdown");
                            //if(lastCount == 1 && dataSource.size() > 1)
                            //{
                                showGreenItem(x, i, true, false, SHOW_FRAGMENT_ANIMATION.Left_To_Right);
                                checkToRunOrderRoll();
                            //}
//                            else
//                                showGreenItem(x, i, false, false, SHOW_FRAGMENT_ANIMATION.Left_To_Right);
                            //--
                            reuse = true;
                            break;
                        }
                    }
                }
                if (!reuse)
                {
                    Log.d(TAG, "[getWaitingOrderFromDB] show new order");
                    showGreenItem(dataSource.get(0), 0, true, true, SHOW_FRAGMENT_ANIMATION.Left_To_Right);
                    checkToRunOrderRoll();
                }
            }
        }
        else
        {
            if(currentFragment == null || currentFragment instanceof CustomerWaitingGreenItem)
                showGreyItem(true);
        }

    }

    private enum SHOW_FRAGMENT_ANIMATION
    {
        Left_To_Right,
        Right_To_Left
    }

    private void showGreyItem(boolean playBeep)
    {
        currentFragment = CustomerWaitingGreyItem.newInstance();
        android.app.FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.animator.fade_in, R.animator.fade_out);
        transaction.replace(R.id.fragment_container, currentFragment);
        transaction.commit();
        //--
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                navigationGroup.setVisibility(View.INVISIBLE);
                navigationGroup2.setVisibility(View.INVISIBLE);
            }
        });

        if(playBeep)
            playBeep();
    }

    private void showGreenItem(
            TaxiOrderDTO data,
            int index,
            boolean playBeep,
            boolean useAnimation,
            SHOW_FRAGMENT_ANIMATION animationType)
    {
        currentFragment = CustomerWaitingGreenItem.newInstance(
                data,
                index);
        android.app.FragmentTransaction transaction = getFragmentManager().beginTransaction();
        if(useAnimation)
        {
            if(animationType == SHOW_FRAGMENT_ANIMATION.Left_To_Right)
                transaction.setCustomAnimations(R.animator.slide_left_enter, R.animator.slide_right_exit);
            else if(animationType == SHOW_FRAGMENT_ANIMATION.Right_To_Left)
                transaction.setCustomAnimations(R.animator.slide_right_enter, R.animator.slide_left_exit);
        }
        transaction.replace(R.id.fragment_container, currentFragment);
        transaction.commit();
        //--
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(dataSource.size() > 1) {
                    navigationGroup.setVisibility(View.VISIBLE);
                    navigationGroup2.setVisibility(View.VISIBLE);
                }
                else
                {
                    navigationGroup.setVisibility(View.INVISIBLE);
                    navigationGroup2.setVisibility(View.INVISIBLE);
                }
            }
        });


        if (playBeep)
        {
            playBeep();
        }

    }

    private void playBeep()
    {
        //--
        try
        {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            MediaPlayer mp = new MediaPlayer();//MediaPlayer.create(CustomerWaitingList.this, notification);
            mp.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
            mp.setDataSource(CustomerWaitingList.this, notification);
            mp.prepare();
            mp.start();

        }
        catch (IllegalArgumentException ex)
        {
            ex.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private DataResultListener confirmOrderResult = new DataResultListener()
    {
        private void ShowErrorDialog()
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    new AlertDialog.Builder(CustomerWaitingList.this)
                            .setMessage("Không thể đăng ký đón khách, xin chọn lại.")
                            .setTitle("Xảy ra lỗi")
                            .setCancelable(false)
                            .setNegativeButton("Đóng", new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    dialog.dismiss();
                                    uiThreadHandler.sendEmptyMessage(UIThreadHandler.MSG_GET_ORDER_DB);
                                }
                            })
                            .create().show();
                }
            });

        }

        @Override
        public void onFailed(String caller, Exception ex)
        {
            Log.e(TAG, "[confirmOrderResult.onFailed] " + ex);
            WorkingSession.getInstance().hideProcessDialog(CustomerWaitingList.this);
            //--
            ShowErrorDialog();
        }

        @Override
        public void onSuccess(String caller, Response response)
        {
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
            WorkingSession.getInstance().hideProcessDialog(CustomerWaitingList.this);
            try
            {
                Gson gson = new Gson();
                String result = response.body().string();
                Log.d(TAG, "[confirmOrderResult.onSuccess] " + result);
                //--
                JsonObject x = gson.fromJson(result, JsonObject.class);
                JsonObject driver = x.getAsJsonObject("Driver");
                WorkingSession.getInstance().getCurrentDriver().Status =
                        driver.get("DriverStatus").getAsInt();
                mldbHandler.addOrUpdateDriver(
                        WorkingSession.getInstance().getCurrentDriver());
                //--
                JsonObject order = x.getAsJsonObject("CurrentOrder");
                if (!order.has("OrderId"))
                {
                    ShowErrorDialog();
                    return;
                }

                currentSelectedOrder.OrderPhone =
                        order.get("OrderPhone").isJsonNull() ? "" : order.get("OrderPhone").getAsString();
                currentSelectedOrder.OrderStatus = order.get("OrderStatus").getAsInt();
                currentSelectedOrder.ConfirmedDate = Utilities.StringToDate(
                        order.get("ConfirmedDate").getAsString(),
                        WorkingSession.DATE_FORMAT
                );
                mldbHandler.addOrUpdateOrder(currentSelectedOrder);

                WorkingSession.getInstance().setCurrentOrder(currentSelectedOrder);

                uiThreadHandler.post(gotoPickupCustomer);
            }
            catch (Exception ex)
            {
                Log.e(TAG, "[confirmOrderResult.onSuccess] " + ex);
                ShowErrorDialog();
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

    private Runnable gotoPickupCustomer = new Runnable()
    {
        @Override
        public void run()
        {
            Intent next = new Intent(CustomerWaitingList.this, PickupCustomer.class);
            startActivity(next);
            finish();
        }
    };

    private DataResultListener createOrderResult = new DataResultListener()
    {
        private void ShowErrorDialog()
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    new AlertDialog.Builder(CustomerWaitingList.this)
                            .setMessage("Không thể đăng ký đón khách, xin thử lại.")
                            .setTitle("Xảy ra lỗi")
                            .setCancelable(false)
                            .setNegativeButton("Đóng", new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    dialog.dismiss();
                                }
                            })
                            .create().show();
                }
            });

        }

        @Override
        public void onFailed(String caller, Exception ex)
        {
            Log.e(TAG, "[createOrderResult.onFailed] " + ex);
            WorkingSession.getInstance().hideProcessDialog(CustomerWaitingList.this);
            //--
            ShowErrorDialog();
        }

        @Override
        public void onSuccess(String caller, Response response)
        {
            WorkingSession.getInstance().hideProcessDialog(CustomerWaitingList.this);
            try
            {
                //Gson gson = new Gson();
                String result = response.body().string();
                Log.d(TAG, "[createOrderResult.onSuccess] " + result);
                //--
                AtomicReference<DriverInfoDTO> arDriver = new AtomicReference<>();
                AtomicReference<TaxiOrderDTO> arCurentOrder = new AtomicReference<>();
                AtomicReferenceArray<TaxiOrderDTO> arWaitingOrders =
                        new AtomicReferenceArray<TaxiOrderDTO>(100);
                AtomicReference<Error> arError = new AtomicReference<>();
                AtomicReference<ConfigureInfo> arConfig = new AtomicReference<>();

                if (!WorkingSession.getInstance().parseResultData(
                        result, arDriver, arCurentOrder, arWaitingOrders, arConfig, arError))
                {
                    ShowErrorDialog();
                    return;
                }
                if (arDriver.get() == null)
                {
                    ShowErrorDialog();
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
                long xx;
                xx = mldbHandler.addOrUpdateDriver(driver);

                //--
                if (arCurentOrder.get() != null)
                {
                    //--
                    TaxiOrderDTO currentSelectedOrder = arCurentOrder.get();

                    currentSelectedOrder.JournalId =
                            (int) WorkingSession.getInstance().getCurrentJournal().JournalId;
                    currentSelectedOrder.DriverId =
                            WorkingSession.getInstance().getCurrentDriver().DriverId;

                    mldbHandler.addOrUpdateOrder(currentSelectedOrder);

                    WorkingSession.getInstance().setCurrentOrder(currentSelectedOrder);
                    //--
                    uiThreadHandler.post(gotoPickupCustomer);
                }
                else
                {
                    ShowErrorDialog();
                    //return;
                }
            }
            catch (Exception ex)
            {
                Log.e(TAG, "[createOrderResult.onSuccess] " + ex);
                ShowErrorDialog();
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

    private View.OnClickListener btnPreviousClick = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            if (currentFragment instanceof CustomerWaitingGreyItem)
                return;
            if(dataSource.size() <= 1)
                return;

            int itemIndex = ((ICustomerWaitingItem) currentFragment).getIndex();
            itemIndex = itemIndex == 0?dataSource.size()-1:itemIndex-1;
            currentSelectedOrder = null;
            showGreenItem(dataSource.get(itemIndex), itemIndex, true, true, SHOW_FRAGMENT_ANIMATION.Right_To_Left);
            checkToRunOrderRoll();
        }
    };

    private View.OnClickListener btnNextClick = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            if (currentFragment instanceof CustomerWaitingGreyItem)
                return;
            if(dataSource.size() <= 1) {
                Log.d(TAG, "[btnNextClick] Trying to get waiting order from DB...");
                MLDBHandler mldbHandler = new MLDBHandler(CustomerWaitingList.this);
                List<TaxiOrderDTO> orders = mldbHandler.getWaitingOrders(
                        WorkingSession.getInstance().getCurrentJournal().JournalId,
                        WorkingSession.getInstance().getCurrentDriver().DriverId
                );
                if(orders == null || orders.size() == 0) {
                    Log.d(TAG, "[btnNextClick] No waiting order from DB, show grey Item");
                    showGreyItem(true);
                }

                return;
            }

            int itemIndex = ((ICustomerWaitingItem) currentFragment).getIndex();
            itemIndex = itemIndex == dataSource.size()-1?0:itemIndex+1;
            currentSelectedOrder = null;
            showGreenItem(dataSource.get(itemIndex), itemIndex, true, true, SHOW_FRAGMENT_ANIMATION.Left_To_Right);
            checkToRunOrderRoll();
        }
    };

    @Override
    protected void doRegisterBroadcast()
    {
        try
        {
            registerReceiver(gpsNotEnabled,
                    new IntentFilter(Declaration.MSG_GPS_OFF));
            //--
        }
        catch (Exception ex)
        {
            Log.e(TAG, "[doRegisterBroadcast] gpsNotEnabled. " + ex.getMessage());
            ex.printStackTrace();
        }
        try
        {
            registerReceiver(waitingOrderBroadcast,
                    new IntentFilter("vn.dungtin.service.MSG_WAITING_ORDER_UPDATED"));
        }
        catch (Exception ex)
        {
            Log.e(TAG, "[doRegisterBroadcast] waitingOrderBroadcast. " + ex.getMessage());
            ex.printStackTrace();
        }

    }

    @Override
    protected void doUnregisterBroadcast()
    {
        try
        {
            Log.d(TAG, "[doUnregisterBroadcast]...");
            unregisterReceiver(
                    gpsNotEnabled
            );
        }
        catch (Exception ex)
        {
            Log.e(TAG, "[doUnregisterBroadcast] gpsNotEnabled. " + ex.getMessage());
            //ex.printStackTrace();
        }
        try
        {
            unregisterReceiver(waitingOrderBroadcast);
        }
        catch (Exception ex)
        {
            Log.e(TAG, "[doUnregisterBroadcast] waitingOrderBroadcast. " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private boolean alertIsShowing = false;
    private BroadcastReceiver gpsNotEnabled = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(!alertIsShowing) {
                alertIsShowing = true;
                showGPSSettingsAlert();
            }
        }
    };

    private void showGPSSettingsAlert()
    {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(CustomerWaitingList.this);

        // Setting Dialog Title
        alertDialog.setTitle("GPS chưa bật");

        // Setting Dialog Message
        alertDialog.setMessage("GPS chưa được bật. \n" +
                "Hãy bật GPS để ứng dụng vận hành đúng và chính xác");

        // On pressing Settings button
        alertDialog.setPositiveButton("Tôi muốn bật", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
                alertIsShowing = false;
            }
        });

        // on pressing cancel button
        alertDialog.setNegativeButton("Bỏ qua", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.cancel();
                alertIsShowing = false;
            }
        });

        // Showing Alert Message
        alertDialog.show();
    }
}
