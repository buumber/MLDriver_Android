package vn.dungtin.mldriver;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.google.gson.Gson;

import java.util.List;

import vn.dungtin.db.dto.TaxiOrderDTO;

/**
 * Created by buumb on 6/10/2016.
 */
public class CustomerWaitingPagerAdapter extends FragmentStatePagerAdapter
{
    private List<TaxiOrderDTO> dataSource;

    public CustomerWaitingPagerAdapter(FragmentManager fm, List<TaxiOrderDTO> source)
    {
        super(fm);
        dataSource = source;
    }

    @Override
    public Fragment getItem(int position)
    {
//        if(dataSource != null && dataSource.size() > position)
//            return CustomerWaitingGreenItem.newInstance(
//                    dataSource.get(position),
//                    position
//            );
        return null;
    }

    @Override
    public int getCount()
    {
        return (dataSource != null && dataSource.size() > 0)? dataSource.size(): 0;
    }
}
