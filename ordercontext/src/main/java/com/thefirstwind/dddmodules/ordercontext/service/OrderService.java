package com.thefirstwind.dddmodules.ordercontext.service;

import com.thefirstwind.dddmodules.ordercontext.model.CustomerOrder;
import com.thefirstwind.dddmodules.ordercontext.repository.CustomerOrderRepository;
import com.thefirstwind.dddmodules.sharedkernel.service.ApplicationService;

public interface OrderService extends ApplicationService {
    void placeOrder(CustomerOrder order);

    void setOrderRepository(CustomerOrderRepository orderRepository);
}
