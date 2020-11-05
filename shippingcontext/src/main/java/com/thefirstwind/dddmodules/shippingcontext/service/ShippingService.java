package com.thefirstwind.dddmodules.shippingcontext.service;

import com.thefirstwind.dddmodules.sharedkernel.service.ApplicationService;
import com.thefirstwind.dddmodules.shippingcontext.model.Parcel;
import com.thefirstwind.dddmodules.shippingcontext.repository.ShippingOrderRepository;

import java.util.Optional;

public interface ShippingService extends ApplicationService {
    void shipOrder(int orderId);

    void listenToOrderEvents();

    Optional<Parcel> getParcelByOrderId(int orderId);

    void setOrderRepository(ShippingOrderRepository orderRepository);
}
