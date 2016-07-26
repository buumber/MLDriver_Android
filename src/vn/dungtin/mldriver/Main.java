package vn.dungtin.mldriver;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import okhttp3.Response;
import vn.dungtin.db.MLDBHandler;
import vn.dungtin.db.dto.DriverInfoDTO;
import vn.dungtin.db.dto.TaxiOrderDTO;
import vn.dungtin.servicestack.ConfigureInfo;
import vn.dungtin.servicestack.DataResultListener;
import vn.dungtin.servicestack.Error;

/**
 * Created by buumb on 5/25/2016.
 */
public class Main extends BaseActivity
{
    private final String TAG = "Main";
    private Button btnMap;
    private ImageButton btnSetting;
    private Button btnCustomerList;
    private Button btnCustomerCatch;
    private Button btnCurrentOrder;
    private Button btnReport;

    private MLDBHandler mldbHandler;


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        //--
        InitHeaderBar();
        //--
        mldbHandler = new MLDBHandler(this);
        //--
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //--
        btnSetting = (ImageButton) findViewById(R.id.btnSetting);
        btnSetting.setOnClickListener(onSettingClick);
        //--
        btnMap = (Button) findViewById(R.id.btnMap);
        btnMap.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Intent next = new Intent(Main.this, MyLocation.class);
                startActivity(next);
            }
        });
        //--
        btnCustomerList = (Button) findViewById(R.id.btnCustomerList);
        btnCustomerList.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent next = new Intent(Main.this, CustomerWaitingList.class);
                startActivity(next);
            }
        });
        btnCurrentOrder = (Button) findViewById(R.id.btnCurrentOrder);
        btnCurrentOrder.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent next = new Intent(Main.this, PickupCustomer.class);
                next.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
                startActivity(next);
            }
        });
        btnCustomerCatch = (Button) findViewById(R.id.btnCustomerCatch);
        btnCustomerCatch.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                WorkingSession.getInstance().showProcessDialog(Main.this);
                DataProvider dp = new DataProvider();
                dp.doCreateOrderdAsync(
                        WorkingSession.getInstance().getCurrentDriver().ClientId,
                        createOrderResult
                );
            }
        });
        btnReport = (Button) findViewById(R.id.btnReport);
        //--

    }

    @Override
    protected void updateOrderStatus()
    {

    }

    @Override
    protected void doRegisterBroadcast()
    {

    }

    @Override
    protected void doUnregisterBroadcast()
    {

    }

    @Override
    protected void onResume()
    {
        Log.d(TAG, "[onResume]...");
        super.onResume();

        if(WorkingSession.getInstance().getCurrentOrder() != null)
        {
            btnCustomerList.setVisibility(View.GONE);
            btnCurrentOrder.setVisibility(View.VISIBLE);
            btnCustomerCatch.setEnabled(false);
        }
        else
        {
            btnCustomerList.setVisibility(View.VISIBLE);
            btnCurrentOrder.setVisibility(View.GONE);
            btnCustomerCatch.setEnabled(true);
        }
    }

    @Override
    protected void onPause()
    {
        Log.d(TAG, "[onPause]...");
        super.onPause();
    }

    private DataResultListener createOrderResult = new DataResultListener()
    {
        private void ShowErrorDialog()
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    new AlertDialog.Builder(Main.this)
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
            WorkingSession.getInstance().hideProcessDialog(Main.this);
            //--
            ShowErrorDialog();
        }

        @Override
        public void onSuccess(String caller, Response response)
        {

            WorkingSession.getInstance().hideProcessDialog(Main.this);
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
                    gotoPickupCustomer();
                }
                else
                {
                    ShowErrorDialog();
                    //return;
                }
//
//
//                JsonObject x = gson.fromJson(result, JsonObject.class);
//                JsonObject driver = x.getAsJsonObject("Driver");
//                WorkingSession.getInstance().getCurrentDriver().Status =
//                        driver.get("DriverStatus").getAsInt();
//                mldbHandler.addOrUpdateDriver(
//                        WorkingSession.getInstance().getCurrentDriver());
//                //--
//                JsonObject order = x.getAsJsonObject("CurrentOrder");
//                if (!order.has("OrderId"))
//                {
//                    ShowErrorDialog();
//                    return;
//                }
//
//                TaxiOrderDTO currentSelectedOrder = gson.fromJson(order, TaxiOrderDTO.class);
//
//                currentSelectedOrder.JournalId =
//                        (int) WorkingSession.getInstance().getCurrentJournal().JournalId;
//                currentSelectedOrder.DriverId =
//                        WorkingSession.getInstance().getCurrentDriver().DriverId;
//
//                mldbHandler.addOrUpdateOrder(currentSelectedOrder);
//
//                WorkingSession.getInstance().setCurrentOrder(currentSelectedOrder);
//                gotoPickupCustomer();
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

    private void gotoPickupCustomer()
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                Intent next = new Intent(Main.this, PickupCustomer.class);
                startActivity(next);
                //finish();
            }
        });

    }

    private View.OnClickListener onSettingClick = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            PopupMenu popup = new PopupMenu(Main.this, btnSetting);
            //Inflating the Popup using xml file
            popup.getMenuInflater().inflate(R.menu.setting_menu, popup.getMenu());

            //registering popup with OnMenuItemClickListener
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
            {
                @Override
                public boolean onMenuItemClick(MenuItem item)
                {
                    if(item.getItemId() == R.id.mnuChangePwd)
                    {
                        Intent next = new Intent(Main.this, ChangePassword.class);
                        startActivity(next);
                    }
                    return true;
                }
            });

            popup.show();//showing popup menu
        }
    };
}