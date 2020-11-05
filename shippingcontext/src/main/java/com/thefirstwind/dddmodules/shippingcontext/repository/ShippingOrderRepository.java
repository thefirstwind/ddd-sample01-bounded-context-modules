package com.thefirstwind.dddmodules.shippingcontext.repository;

import com.thefirstwind.dddmodules.shippingcontext.model.ShippableOrder;

import java.util.Optional;

public interface ShippingOrderRepository {
    Optional<ShippableOrder> findShippableOrder(int orderId);
}
