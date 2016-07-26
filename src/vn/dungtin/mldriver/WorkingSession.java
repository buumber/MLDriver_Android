package vn.dungtin.mldriver;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import vn.dungtin.db.dto.DriverInfoDTO;
import vn.dungtin.db.dto.JournalDTO;
import vn.dungtin.db.dto.TaxiOrderDTO;
import vn.dungtin.servicestack.ConfigureInfo;
import vn.dungtin.servicestack.DriverInfo;
import vn.dungtin.servicestack.Error;
import vn.dungtin.servicestack.WaitingOrder;

/**
 * Created by buumb on 6/2/2016.
 */
public class WorkingSession {
    private final String TAG = "WorkingSession";
    private static WorkingSession theInstance;

    public static WorkingSession getInstance() {
        if (theInstance == null)
            theInstance = new WorkingSession();
        return theInstance;
    }

    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSZZZZZ";

    private DriverInfoDTO currentDriver;

    public DriverInfoDTO getCurrentDriver() {
        return currentDriver;
    }

    public WorkingSession setCurrentDriver(DriverInfoDTO driver) {
        currentDriver = driver;
        return this;
    }

    //---
    private JournalDTO currentJournal;

    public JournalDTO getCurrentJournal() {
        return currentJournal;
    }

    public WorkingSession setCurrentJournal(JournalDTO journal) {
        currentJournal = journal;
        return this;
    }

    //---
    private TaxiOrderDTO currentOrder;

    public TaxiOrderDTO getCurrentOrder() {
        return currentOrder;
    }

    public WorkingSession setCurrentOrder(TaxiOrderDTO order) {
        boolean orderRemoved = false;
        boolean orderChanged = false;
        if(currentOrder != null && order == null) // current order removed
            orderRemoved = true;
        if(currentOrder == null && order != null)
            orderChanged = true;
        else if(currentOrder != null)
            orderChanged = currentOrder.AreIdentical(order);
        //--
        currentOrder = order;

        if(orderRemoved || orderChanged)
        {
            Intent broadcast = new Intent(Declaration.MSG_ORDER_UPDATE);
            MLDriverApp.getAppContext().sendBroadcast(broadcast);
        }

        return this;
    }

    private AlertDialog processDialog;

    public void showProcessDialog(final Activity activity) {
        if (processDialog != null && processDialog.isShowing())
            return;

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                processDialog = new AlertDialog.Builder(activity)
                        .setMessage("Xin chờ...")
                        .setTitle("Đang xử lý")
                        .setCancelable(false)
                        .create();
                processDialog.show();
            }
        });

        try {
            Thread.sleep(1000);
        } catch (Exception ex) {

        }
    }

    public void hideProcessDialog(final Activity activity) {
        if (processDialog != null && !processDialog.isShowing())
            return;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                processDialog.dismiss();
            }
        });

    }

    public boolean parseResultData(String data,
                                   AtomicReference<DriverInfoDTO> driverInfo,
                                   AtomicReference<TaxiOrderDTO> currentOrder,
                                   AtomicReferenceArray<TaxiOrderDTO> waitingOrders,
                                   AtomicReference<ConfigureInfo> config,
                                   AtomicReference<Error> error) {
        /*
            {
				"Driver":
				{
					"DriverId":18,
					"StaffCard":4190,
					"DriverName":"Phạm Văn Thành",
					"DriverStatus":3,
					"DriverClientID":"eaf0e4ea-6f07-4dd6-992e-453d361cab76"
				},
				"CurrentOrder":
				{
					"OrderId":12,
					"OrderPhone":"",
					"OrderAddress":"96 Yên Th?, Phu?ng 2, Tân Bình, H? Chí Minh, Vi?t Nam",
					"OrderLat":10.8129,
					"OrderLng":106.6688,
					"OrderAccuracy":0,
					"OrderStatus":3,
					"OrderDate":"2016-06-06T18:13:13.1000932+07:00",
					"ConfirmedDate":"2016-06-06T18:13:13.1000932+07:00",
					"AcceptedDate":"2016-06-06T18:13:13.1000932+07:00",
					"TaxiType":0
				},
				"WaitingOrders":[
					{"OrderId":10,
					"OrderAddress":"96 Yên Thế, Phường 2, Tân Bình, Hồ Chí Minh, Vietnam",
					"OrderLat":10.81289,
					"OrderLng":106.6688,
					"OrderLogAccuracy":null,
					"OrderPhone":"0936549876",
					"OrderStatus":1,
					"OrderDate":"2016-06-02T20:29:19.1644817+07:00",
					"TaxiType":2}
				],
				Config: {  DisplayOrderPeriod: 10, UpdateLocationPeriod: 10, PullOrderPeriod: 600000 }
			}
			 */
        //{"Error":{"UserName":"4190,1234,0912345678","Code":-6,"Message":"Driver already logged in within 30 minutes."}}

        try {
            Gson gson = new Gson();

            Log.d(TAG, "[parseResultData] " + data);
            //--
            JsonObject x = gson.fromJson(data, JsonObject.class);

            JsonElement testResult = x.get("Error");
            if (testResult != null && !testResult.isJsonNull()) {
                Error err = gson.fromJson(testResult, Error.class);
                if (error != null)
                    error.set(err);

                return false;
            }

            JsonElement xObject = x.get("Driver");
            if (xObject != null && !xObject.isJsonNull()) {
                DriverInfo di = gson.fromJson(xObject, DriverInfo.class);
                //--
                DriverInfoDTO driver = new DriverInfoDTO(di);
                //--
                if (driverInfo != null)
                    driverInfo.set(driver);
            }
            //--

            xObject = x.get("CurrentOrder");
            if (xObject != null && !xObject.isJsonNull()) {
                TaxiOrderDTO xOrder = gson.fromJson(xObject, TaxiOrderDTO.class);
                JsonObject jo = xObject.getAsJsonObject();
                if(jo.has("OrderDate") && !jo.get("OrderDate").isJsonNull()) {
                    xOrder.OrderDate = Utilities.StringToDate(
                            jo.get("OrderDate").getAsString(),
                            WorkingSession.DATE_FORMAT
                    );
                }
                if(jo.has("ConfirmedDate") && !jo.get("ConfirmedDate").isJsonNull()) {
                    xOrder.ConfirmedDate = Utilities.StringToDate(
                            jo.get("ConfirmedDate").getAsString(),
                            WorkingSession.DATE_FORMAT
                    );
                }
                if(jo.has("AcceptedDate") && !jo.get("AcceptedDate").isJsonNull()) {
                    xOrder.AcceptedDate = Utilities.StringToDate(
                            jo.get("AcceptedDate").getAsString(),
                            WorkingSession.DATE_FORMAT
                    );
                }
                if (xOrder != null)
                    currentOrder.set(xOrder);
            }
            xObject = x.get("WaitingOrders");
            if (xObject != null && !xObject.isJsonNull() &&
                    waitingOrders != null)
            {
                JsonArray xArray = x.getAsJsonArray("WaitingOrders");
                JournalDTO journal = WorkingSession.getInstance().getCurrentJournal();
                if (xArray != null) {
                    int i = 0;
                    for (JsonElement element : xArray) {
                        WaitingOrder wo = gson.fromJson(element, WaitingOrder.class);
                        if (wo == null)
                            continue;
                        TaxiOrderDTO dto = new TaxiOrderDTO();
                        dto.AcceptedDate = null;
                        dto.ConfirmedDate = null;
                        dto.CreatedDate = new Date();
                        dto.DriverId = 0;
                        dto.FinishedDate = null;
                        dto.JournalId = journal == null? 0 : (int) journal.JournalId;
                        dto.OrderAddress = wo.OrderAddress == null? "": wo.OrderAddress;
                        dto.OrderDate = Utilities.StringToDate(
                                wo.OrderDate,
                                WorkingSession.DATE_FORMAT
                        );
                        dto.OrderId = wo.OrderId;
                        dto.OrderLat = wo.OrderLat;
                        dto.OrderLng = wo.OrderLng;
                        dto.OrderLogAccuracy = wo.OrderLogAccuracy;
                        dto.OrderPhone = wo.OrderPhone == null ? "" : wo.OrderPhone;
                        dto.TaxiType = wo.TaxiType;
                        dto.OrderStatus = wo.OrderStatus;
                        //--
                        waitingOrders.set(i++, dto);
                    }
                }
            }
            xObject = x.get("Config");
            if (xObject != null && !xObject.isJsonNull() &&
                    config != null)
            {
                ConfigureInfo xConfig = gson.fromJson(xObject, ConfigureInfo.class);
                //--
                config.set(xConfig);
            }

        } catch (Exception ex) {
            Log.e(TAG, "[parseResultData] " + ex.getMessage());
            ex.printStackTrace();
            return false;
        }

        return true;
    }

    public static void showErrorDialog(final Activity activity,
                                       final String title, final String message,
                                       final DialogInterface.OnClickListener okListener) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(activity)
                        .setMessage(message)
                        .setTitle(title)
                        .setCancelable(false)
                        .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                if (okListener != null)
                                    okListener.onClick(dialog, which);
                            }
                        })
                        .create().show();
            }
        });

    }

//    public static void showGPSSettingsAlert(final Context ctx)
//    {
//        AlertDialog.Builder alertDialog = new AlertDialog.Builder(ctx);
//
//        // Setting Dialog Title
//        alertDialog.setTitle("GPS chưa bật");
//
//        // Setting Dialog Message
//        alertDialog.setMessage("GPS chưa được bật. \n" +
//                "Hãy bật GPS để ứng dụng vận hành đúng và chính xác");
//
//        // On pressing Settings button
//        alertDialog.setPositiveButton("Tôi muốn bật", new DialogInterface.OnClickListener()
//        {
//            public void onClick(DialogInterface dialog, int which)
//            {
//                Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//                ctx.startActivity(intent);
//            }
//        });
//
//        // on pressing cancel button
//        alertDialog.setNegativeButton("Bỏ qua", new DialogInterface.OnClickListener()
//        {
//            public void onClick(DialogInterface dialog, int which)
//            {
//                dialog.cancel();
//            }
//        });
//
//        // Showing Alert Message
//        alertDialog.show();
//    }
}
