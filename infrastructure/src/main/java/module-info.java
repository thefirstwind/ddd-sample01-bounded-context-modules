module com.thefirstwind.dddmodules.infrastructure {
    requires transitive com.thefirstwind.dddmodules.sharedkernel;
    requires transitive com.thefirstwind.dddmodules.ordercontext;
    requires transitive com.thefirstwind.dddmodules.shippingcontext;
    provides com.thefirstwind.dddmodules.sharedkernel.events.EventBus
      with com.thefirstwind.dddmodules.infrastructure.events.SimpleEventBus;
    provides com.thefirstwind.dddmodules.ordercontext.repository.CustomerOrderRepository
      with com.thefirstwind.dddmodules.infrastructure.db.InMemoryOrderStore;
    provides com.thefirstwind.dddmodules.shippingcontext.repository.ShippingOrderRepository
      with com.thefirstwind.dddmodules.infrastructure.db.InMemoryOrderStore;
}
