package com.thefirstwind.dddmodules.ordercontext.repository;

import com.thefirstwind.dddmodules.ordercontext.model.CustomerOrder;

public interface CustomerOrderRepository {
    void saveCustomerOrder(CustomerOrder order);
}
