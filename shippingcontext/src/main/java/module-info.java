module com.thefirstwind.dddmodules.shippingcontext {
    requires com.thefirstwind.dddmodules.sharedkernel;
    exports com.thefirstwind.dddmodules.shippingcontext.service;
    exports com.thefirstwind.dddmodules.shippingcontext.model;
    exports com.thefirstwind.dddmodules.shippingcontext.repository;
    provides com.thefirstwind.dddmodules.shippingcontext.service.ShippingService
      with com.thefirstwind.dddmodules.shippingcontext.service.ParcelShippingService;
}
