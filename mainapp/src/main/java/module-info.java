module com.thefirstwind.dddmodules.mainapp {
    uses com.thefirstwind.dddmodules.sharedkernel.events.EventBus;
    uses com.thefirstwind.dddmodules.ordercontext.service.OrderService;
    uses com.thefirstwind.dddmodules.ordercontext.repository.CustomerOrderRepository;
    uses com.thefirstwind.dddmodules.shippingcontext.repository.ShippingOrderRepository;
    uses com.thefirstwind.dddmodules.shippingcontext.service.ShippingService;
    requires transitive com.thefirstwind.dddmodules.infrastructure;
}