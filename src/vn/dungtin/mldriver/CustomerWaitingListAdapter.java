package vn.dungtin.mldriver;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Date;
import java.util.List;

import vn.dungtin.db.dto.TaxiOrderDTO;

/**
 * Created by buumb on 6/3/2016.
 */
public class CustomerWaitingListAdapter extends ArrayAdapter<TaxiOrderDTO>
{
	private int itemLayoutResId;

	public CustomerWaitingListAdapter(Context context, int resItemLayout, List<TaxiOrderDTO> dataSource)
	{
		super(context, resItemLayout, dataSource);
		itemLayoutResId = resItemLayout;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		if(convertView == null)
		{
			LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(
					Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(itemLayoutResId, parent, false);
		}

		convertView.setBackground(null);
		//--
		TaxiOrderDTO order = getItem(position);
		TextView textView = (TextView)convertView.findViewById(R.id.tvAddress);
		if(textView != null)
			textView.setText(order.OrderAddress);
		textView = (TextView)convertView.findViewById(R.id.tvPassageTime);
		if(textView != null)
			textView.setText(
					calcPassageOfTime(order.OrderDate) + " phút"
			);
		textView = (TextView)convertView.findViewById(R.id.tvCarType);
		if(textView != null)
			textView.setText(
					order.TaxiType == 1? "4 chỗ" : (order.TaxiType == 2? "7 chỗ": "N/A")
			);
//		textView = (TextView)convertView.findViewById(R.id.tvPhone);
//		if(textView != null)
//			textView.setText(
//					order.OrderPhone
//			);

		return convertView;
	}

	private int calcPassageOfTime(Date orderDate)
	{
		int x = (int)Utilities.DateDifferenceSecond(new Date(), orderDate)/60;
		return x<= 0? 1: x;
	}
}
