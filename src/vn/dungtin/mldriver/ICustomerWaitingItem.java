package vn.dungtin.mldriver;

import vn.dungtin.db.dto.TaxiOrderDTO;

/**
 * Created by buumb on 6/10/2016.
 */
public interface ICustomerWaitingItem
{
    TaxiOrderDTO getDataSource();
    ICustomerWaitingItem setDataSource(TaxiOrderDTO data);
    ICustomerWaitingItem setIndex(int index);
    int getIndex();
}
