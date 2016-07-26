package vn.dungtin.mldriver;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.app.Fragment;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.Gson;

import org.w3c.dom.Text;

import java.util.Date;

import vn.dungtin.db.dto.TaxiOrderDTO;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link CustomerWaitingGreenItem.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link CustomerWaitingGreenItem#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CustomerWaitingGreenItem extends Fragment implements ICustomerWaitingItem
{
    private static final String TAG = "WaitingGreenItem";

    private OnFragmentInteractionListener mListener;


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param data Parameter
     * @return A new instance of fragment CustomerWaitingGreenItem.
     */
    public static CustomerWaitingGreenItem newInstance(
            TaxiOrderDTO data,
            int index)
    {
        CustomerWaitingGreenItem fragment = new CustomerWaitingGreenItem();
        fragment.setDataSource(data).setIndex(index);
        //--
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "[onCreate]...");

    }

    private View myView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        Log.d(TAG, "[onCreateView]...");
        if (container.getContext() instanceof OnFragmentInteractionListener)
        {
            mListener = (OnFragmentInteractionListener) container.getContext();
        }
        else
        {
            throw new RuntimeException(container.getContext().toString()
                    + " must implement OnFragmentInteractionListener");
        }
        //--

        View view = inflater.inflate(R.layout.item_cust_waiting_green, container, false);

        myView = view;

        updateDataView();


        return view;
    }

    private void updateDataView()
    {
        if(myView == null)
            return;

        TextView textView = (TextView)myView.findViewById(R.id.tvAddress);
        if(textView != null)
        {
            if(dataSource != null && dataSource.OrderAddress != null)
                textView.setText(dataSource.OrderAddress);
            else textView.setText("Không rõ");
        }
        textView = (TextView)myView.findViewById(R.id.tvPassageOfTime);
        if(textView != null)
        {
            if(dataSource != null)
                textView.setText(
                        calcPassageOfTime(dataSource.OrderDate) + " phút"
                );
            else
                textView.setText(
                        "1 phút"
                );
        }
        textView = (TextView)myView.findViewById(R.id.tvCarType);
        if(textView != null)
        {
            if(dataSource != null)
                textView.setText(
                        dataSource.TaxiType == 1 ? "4 chỗ" : (dataSource.TaxiType == 2 ? "7 chỗ" : "4 hoặc 7 chỗ")
                );
            else
                textView.setText("4 hoặc 7 chỗ");
        }

        textView = (TextView)myView.findViewById(R.id.tvOrderId);
        if(textView != null)
        {
            if(dataSource != null)
                textView.setText(
                        "Mã số: " + dataSource.OrderId
                );
            else
                textView.setText("4 hoặc 7 chỗ");
        }

        Button btnSelect = (Button)myView.findViewById(R.id.btnSelect);
        if(btnSelect != null)
        {
            btnSelect.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    if (mListener != null)
                    {
                        mListener.onSelectOrder(dataSource);
                    }
                }
            });
        }
    }


    @Override
    public void onAttach(Context context)
    {
        Log.d(TAG, "[onAttach]...");
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener)
        {
            mListener = (OnFragmentInteractionListener) context;
        }
        else
        {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach()
    {
        Log.d(TAG, "[onDetach]...");
        super.onDetach();
        mListener = null;
        myView = null;
    }

    @Override
    public TaxiOrderDTO getDataSource()
    {
        return dataSource;
    }

    private TaxiOrderDTO dataSource;
    @Override
    public ICustomerWaitingItem setDataSource(TaxiOrderDTO data)
    {
        dataSource = data;
        return this;
    }

    private int inputIndex;
    @Override
    public ICustomerWaitingItem setIndex(int index)
    {
        inputIndex = index;
        return this;
    }

    @Override
    public int getIndex()
    {
        return inputIndex;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener
    {
        void onSelectOrder(TaxiOrderDTO order);
    }

    private int calcPassageOfTime(Date orderDate)
    {
        int x = (int)Utilities.DateDifferenceSecond(new Date(), orderDate)/60;
        return x<= 0? 1: x;
    }



}
