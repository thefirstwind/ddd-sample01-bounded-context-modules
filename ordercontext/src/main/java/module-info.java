module com.thefirstwind.dddmodules.ordercontext {
    requires com.thefirstwind.dddmodules.sharedkernel;
    exports com.thefirstwind.dddmodules.ordercontext.service;
    exports com.thefirstwind.dddmodules.ordercontext.model;
    exports com.thefirstwind.dddmodules.ordercontext.repository;
    provides com.thefirstwind.dddmodules.ordercontext.service.OrderService
      with com.thefirstwind.dddmodules.ordercontext.service.CustomerOrderService;
}