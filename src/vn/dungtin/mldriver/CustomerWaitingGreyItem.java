package vn.dungtin.mldriver;

import android.app.Fragment;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import vn.dungtin.db.dto.TaxiOrderDTO;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CustomerWaitingGreyItem#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CustomerWaitingGreyItem extends Fragment implements ICustomerWaitingItem
{
    public CustomerWaitingGreyItem()
    {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment CustomerWaitingGreenItem.
     */
    // TODO: Rename and change types and number of parameters
    public static CustomerWaitingGreyItem newInstance()
    {
        CustomerWaitingGreyItem fragment = new CustomerWaitingGreyItem();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.item_cust_waiting_grey, container, false);
//        TextView textView = new TextView(getActivity());
//        textView.setText(R.string.hello_blank_fragment);
        return view;
    }

    @Override
    public TaxiOrderDTO getDataSource()
    {
        return null;
    }

    @Override
    public CustomerWaitingGreyItem setDataSource(TaxiOrderDTO data)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public CustomerWaitingGreyItem setIndex(int index)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getIndex()
    {
        return 0;
    }

}
