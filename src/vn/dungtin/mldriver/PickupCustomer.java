package vn.dungtin.mldriver;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.Date;

import okhttp3.Response;
import vn.dungtin.db.MLDBHandler;
import vn.dungtin.db.dto.TaxiOrderDTO;
import vn.dungtin.servicestack.DataResultListener;

public class PickupCustomer extends BaseActivity
{
    protected final String TAG = "PickupCustomer";
//    private Button btnAccept;
//    private Button btnCustNotFound;
//    private Button btnCancel;
//    private Button btnFinish;
    //--
    private LinearLayout viewOrderInfo;
//    private LinearLayout viewFinishOrder;
//    private LinearLayout viewAcceptOrder;

//    private MLDBHandler mldbHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pickupcustomer);
        //--
        InitHeaderBar();
        InitFooterOrder();
        //--
        Button btnMap = (Button)findViewById(R.id.btnMap);
        if(btnMap!=null)
        {
            btnMap.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    Intent next = new Intent(PickupCustomer.this, MyLocation.class);
                    startActivity(next);
                    finish();
                }
            });
        }
        //--
        viewOrderInfo = (LinearLayout)findViewById(R.id.viewOrderInfo);
        viewOrderInfo.setVisibility(View.INVISIBLE);
        //--
        updateOrderInfo();
    }

    @Override
    protected void onResume()
    {
        Log.d(TAG, "[onResume] ...");
        super.onResume();
        //--
        doRegisterBroadcast();
    }

    @Override
    protected void onPause()
    {
        Log.d(TAG, "[onPause] ...");
        super.onPause();
        doUnregisterBroadcast();
    }

    protected void updateOrderInfo()
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                TaxiOrderDTO order = WorkingSession.getInstance().getCurrentOrder();
                if (order == null)
                    return;
                TextView textView = (TextView) findViewById(R.id.tvAddress);
                textView.setText(
                        order.OrderAddress
                );
                textView = (TextView) findViewById(R.id.tvPhone);
                textView.setText(
                        Utilities.formatPhoneVN(order.OrderPhone)
                );
                int status = order.OrderStatus;
                textView = (TextView) findViewById(R.id.tvStatus);
                textView.setText(
                        status == 1 ? "Chưa đón" :
                                status == 2 ? "Đang đón" :
                                        status == 3 ? "Đã đón" :
                                                status == 1 ? "Xong" : "Không xác định"
                );
                if (status == 3) //--accepted
                {
                    showFinishView();
                }
                else
                {
                    showAcceptView();
                }
                viewOrderInfo.setVisibility(View.VISIBLE);
            }
        });

    }

    @Override
    protected void updateOrderStatus()
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                int status = WorkingSession.getInstance().getCurrentOrder().OrderStatus;
                TextView textView = (TextView) findViewById(R.id.tvStatus);
                textView.setText(
                        status == 1 ? "Chưa đón" :
                                status == 2 ? "Đang đón" :
                                        status == 3 ? "Đã đón" :
                                                status == 1 ? "Xong" : "Không xác định"
                );
            }
        });

    }

    @Override
    protected void doRegisterBroadcast()
    {
        try
        {
            Log.d(TAG, "[doRegisterBroadcast]...");
            registerReceiver(
                    orderChangedReceiver,
                    new IntentFilter(Declaration.MSG_ORDER_UPDATE)
            );
        }
        catch (Exception ex)
        {
            Log.e(TAG, "[doRegisterBroadcast] " + ex.getMessage());
            //ex.printStackTrace();
        }
    }

    @Override
    protected void doUnregisterBroadcast()
    {
        try
        {
            Log.d(TAG, "[doUnregisterBroadcast]...");
            unregisterReceiver(
                    orderChangedReceiver
            );
        }
        catch (Exception ex)
        {
            Log.e(TAG, "[doUnregisterBroadcast] " + ex.getMessage());
            //ex.printStackTrace();
        }
    }

//    private AlertDialog processDialog;
//
//    private void showProcessDialog()
//    {
//        if (processDialog != null && processDialog.isShowing())
//            return;
//
//        runOnUiThread(new Runnable()
//        {
//            @Override
//            public void run()
//            {
//                processDialog = new AlertDialog.Builder(PickupCustomer.this)
//                        .setMessage("Xin chờ...")
//                        .setTitle("Đang xử lý")
//                        .setCancelable(false)
//                        .create();
//                processDialog.show();
//            }
//        });
//
//    }
//
//    private void hideProcessDialog()
//    {
//        if (processDialog != null && !processDialog.isShowing())
//            return;
//        runOnUiThread(new Runnable()
//        {
//            @Override
//            public void run()
//            {
//                processDialog.dismiss();
//            }
//        });
//
//    }
//
//    private void ShowErrorDialog()
//    {
//        runOnUiThread(new Runnable()
//        {
//            @Override
//            public void run()
//            {
//                new AlertDialog.Builder(PickupCustomer.this)
//                        .setMessage("Không thể thực hiện thao tác này, xin thử lại.")
//                        .setTitle("Xảy ra lỗi")
//                        .setCancelable(false)
//                        .setNegativeButton("Đóng", new DialogInterface.OnClickListener()
//                        {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which)
//                            {
//                                dialog.dismiss();
//                            }
//                        })
//                        .create().show();
//            }
//        });
//
//    }

//    private void showFinishView()
//    {
//        runOnUiThread(new Runnable()
//        {
//            @Override
//            public void run()
//            {
//                viewAcceptOrder.setVisibility(View.GONE);
//                viewFinishOrder.setVisibility(View.VISIBLE);
//            }
//        });
//    }

//    private void goToHome()
//    {
//        Intent next = new Intent(PickupCustomer.this, Main.class);
//        next.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        startActivity(next);
//        finish();
//    }



    private BroadcastReceiver orderChangedReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (WorkingSession.getInstance().getCurrentOrder() == null)
            {
                WorkingSession.showErrorDialog(
                        PickupCustomer.this,
                        "Thông báo",
                        "Khách đã huỷ chuyến",
                        new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                goToWaitingList();
                            }
                        }
                );
                return;
            }
            updateOrderInfo();
        }
    };
}

//
//public enum TaxiOrderStatus
//{
//	Available = 0,
//	GuestWaiting,
//	DriverConfirmed,
//	DriverAccepted,
//	Finish,
//}