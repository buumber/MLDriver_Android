package vn.dungtin.mldriver;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import okhttp3.Response;
import vn.dungtin.db.MLDBHandler;
import vn.dungtin.db.dto.DriverInfoDTO;
import vn.dungtin.db.dto.JournalDTO;
import vn.dungtin.db.dto.TaxiOrderDTO;
import vn.dungtin.service.GpsTracking;
import vn.dungtin.servicestack.DataResultListener;
import vn.dungtin.servicestack.Error;

public class ChangePassword extends BaseActivity
{
    private final String TAG = "ChangePassword";
    private EditText txtCurrent;
    private EditText txtNew;
    private EditText txtConfirm;
    private Button btnSubmit;
    private Handler uiHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.change_password);
        //--
        InitHeaderBar();
        //--
        uiHandler = new Handler();
        //--
        txtCurrent = (EditText)findViewById(R.id.txtCurrent);
        txtNew = (EditText)findViewById(R.id.txtNew);
        txtConfirm = (EditText)findViewById(R.id.txtConfirm);
        //--
        btnSubmit = (Button)findViewById(R.id.btnSubmit);
        btnSubmit.setOnClickListener(onBtnSubmitClick);
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

    private View.OnClickListener onBtnSubmitClick = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            if(txtCurrent.getText().length() == 0)
            {
                WorkingSession.showErrorDialog(
                        ChangePassword.this,
                        "Lỗi",
                        "Xin nhập mật khẩu hiện tại",
                        null
                );
                return;
            }

            if(txtNew.getText().length() == 0 || txtConfirm.getText().length() == 0)
            {
                WorkingSession.showErrorDialog(
                        ChangePassword.this,
                        "Lỗi",
                        "Hãy nhập mật khẩu mới và nhập lại 1 lần nữa \nđể đảm bảo là bạn đã nhớ chính xác",
                        null
                );
                return;
            }

            if((txtNew.getText().length() != txtConfirm.getText().length()) ||
                (!txtNew.getText().toString().equals(txtConfirm.getText().toString())))
            {
                WorkingSession.showErrorDialog(
                        ChangePassword.this,
                        "Lỗi",
                        "Mật khẩu mới bạn nhập 2 lần không giống nhau.\n" +
                                "Hãy nhập lại chính xác cùng 1 mật khẩu cho 2 lần",
                        new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                uiHandler.post(clearNewPwdFields);
                            }
                        }
                );
                return;
            }

            if(!txtNew.getText().toString().matches("[a-zA-Z0-9.? ]*"))
            {
                WorkingSession.showErrorDialog(
                        ChangePassword.this,
                        "Không được phép",
                        "Mật khẩu chỉ được phép là chữ cái hoặc số.",
                        new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                uiHandler.post(clearNewPwdFields);
                            }
                        }
                );
                return;
            }

            //--
            setSubmitButtonInfo("Đang xử lý...", false);
            //--
            DataProvider dp = new DataProvider();
            dp.doChangePwdAsync(
                    WorkingSession.getInstance().getCurrentDriver().ClientId,
                    WorkingSession.getInstance().getCurrentDriver().StaffCardNumber,
                    txtCurrent.getText().toString(),
                    txtNew.getText().toString(),
                    changePwdResult
            );
        }
    };

    private Runnable clearNewPwdFields = new Runnable()
    {
        @Override
        public void run()
        {
            txtNew.setText("");
            txtConfirm.setText("");
            txtNew.requestFocus();
        }
    };

    private void setSubmitButtonInfo(final String text, final boolean enabled)
    {
        uiHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                btnSubmit.setText(text);
                btnSubmit.setEnabled(enabled);
            }
        });
    }

    private DataResultListener changePwdResult = new DataResultListener()
    {
        @Override
        public void onFailed(String caller, final Exception ex)
        {
            Log.e(TAG, "[changePwdResult.onFailed] " + ex.getMessage());

            WorkingSession.showErrorDialog(
                    ChangePassword.this,
                    "Không thể đổi mật khẩu",
                    "Không thể đổi mật khẩu vào lúc này, xin thử lại sau",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            setSubmitButtonInfo("Xác nhận", true);
                        }
                    }
            );
        }

        @Override
        public void onSuccess(String caller, final Response response)
        {

            try
            {
                Gson gson = new Gson();
                String result = response.body().string();
                Log.d(TAG, "[changePwdResult.onSuccess] " + result);

                if (result.equals("true")) {
                    WorkingSession.showErrorDialog(
                            ChangePassword.this,
                            "Thành công",
                            "Mật khẩu đã được đổi.\nHãy đăng nhập lại để tiếp tục sử dụng.",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    doAfterChangePasswordSuccessed();
                                }
                            }
                    );
                }
                else
                {
                    WorkingSession.showErrorDialog(
                            ChangePassword.this,
                            "Không thể đổi mật khẩu",
                            "Mật khẩu hiện tại không đúng.",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    setSubmitButtonInfo("Xác nhận", true);
                                }
                            }
                    );
                }
            }
            catch (Exception ex)
            {
                Log.e(TAG, "[changePwdResult.onSuccess] " + ex.getMessage());
                ex.printStackTrace();
                WorkingSession.showErrorDialog(
                        ChangePassword.this,
                        "Lỗi của chương trình",
                        ex.getMessage(),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                setSubmitButtonInfo("Xác nhận", true);
                            }
                        }
                );

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

    private void doAfterChangePasswordSuccessed()
    {
        mldbHandler.updateLoginRemember(0);
        final DataProvider dp = new DataProvider();
        dp.doLogoutAsync(
                WorkingSession.getInstance().getCurrentDriver().ClientId,
                new DataResultListener() {
                    @Override
                    public void onFailed(String caller, Exception ex) {

                    }

                    @Override
                    public void onSuccess(String caller, Response response) {
//                        dp.doClearSessionAsync(
//                                WorkingSession.getInstance().getCurrentDriver().ClientId,
//                                null
//                        );
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

        WorkingSession.getInstance()
                .setCurrentDriver(null)
                .setCurrentJournal(null);
        //--
        Intent next = new Intent(ChangePassword.this, Login.class);
        next.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(next);
        finish();
    }
}
