# domain driven bounded contexts and modules

* https://github.com/thefirstwind/ddd-sample01-modules

[TOC]

## 1. DDD Bounded Contexts


Nowadays, software systems are not simple CRUD applications. Actually, the typical monolithic enterprise system consists of some legacy codebase and newly added features. However, it becomes harder and harder to maintain such systems with every change made. Eventually, it may become totally unmaintainable.

### 1.1. Bounded Context and Ubiquitous Language
To solve the addressed issue, DDD provides the concept of Bounded Context. A Bounded Context is a logical boundary of a domain where particular terms and rules apply consistently. Inside this boundary, all terms, definitions, and concepts form the Ubiquitous Language.

In particular, the main benefit of ubiquitous language is grouping together project members from different areas around a specific business domain.

Additionally, multiple contexts may work with the same thing. However, it may have different meanings inside each of these contexts.

``` plantuml
@startuml
left to right direction
card card as "Domain"{
    storage storage as "Bounded  Context"{
        card leaf1
        card leaf2
        card leaf3
    }
}
leaf1 -- leaf2
leaf1 -- leaf3
@enduml
```

### 1.2. Order Context
Let’s start implementing our application by defining the Order Context. This context contains two entities: OrderItem and CustomerOrder.

``` plantuml
@startuml
left to right direction
    storage storage as "Order  Context"{
        card leaf1 as "OrderItem"
        card leaf2 as "CustomerOrder"
    }
leaf1 -- leaf2
@enduml
```


The CustomerOrder entity is an aggregate root:

```java
public class CustomerOrder {
    private int orderId;
    private String paymentMethod;
    private String address;
    private List<OrderItem> orderItems;

    public float calculateTotalPrice() {
        return orderItems.stream().map(OrderItem::getTotalPrice)
          .reduce(0F, Float::sum);
    }
}
```
As we can see, this class contains the calculateTotalPrice business method. But, in a real-world project, it will probably be much more complicated — for instance, including discounts and taxes in the final price.

Next, let’s create the OrderItem class:

```java
public class OrderItem {
    private int productId;
    private int quantity;
    private float unitPrice;
    private float unitWeight;
}
```

We’ve defined entities, but also we need to expose some API to other parts of the application. Let’s create the CustomerOrderService class:

```java
public class CustomerOrderService implements OrderService {
    public static final String EVENT_ORDER_READY_FOR_SHIPMENT = "OrderReadyForShipmentEvent";

    private CustomerOrderRepository orderRepository;
    private EventBus eventBus;
 
    @Override
    public void placeOrder(CustomerOrder order) {
        this.orderRepository.saveCustomerOrder(order);
        Map<String, String> payload = new HashMap<>();
        payload.put("order_id", String.valueOf(order.getOrderId()));
        ApplicationEvent event = new ApplicationEvent(payload) {
            @Override
            public String getType() {
                return EVENT_ORDER_READY_FOR_SHIPMENT;
            }
        };
        this.eventBus.publish(event);
    }
}
```

Here, we have some important points to highlight. The placeOrder method is responsible for processing customer orders. After an order is processed, the event is published to the EventBus. We'll discuss the event-driven communication in the next chapters. This service provides the default implementation for the OrderService interface:

```java
public interface OrderService extends ApplicationService {
    void placeOrder(CustomerOrder order);

    void setOrderRepository(CustomerOrderRepository orderRepository);
}
```
Furthermore, this service requires the CustomerOrderRepository to persist orders:

```java
public interface CustomerOrderRepository {
    void saveCustomerOrder(CustomerOrder order);
}
```
What’s essential is that this interface is not implemented inside this context but will be provided by the Infrastructure Module, as we’ll see later.

### 1.3. Shipping Context
Now, let's define the Shipping Context. It will also be straightforward and contain three entities: Parcel, PackageItem, and ShippableOrder.

``` plantuml
@startuml
left to right direction
    storage storage as " Shipping  Context"{
        card leaf1 as "PackageItem"
        card leaf2 as "Parcel"
        card leaf3 as "ShippableOrder"
    }
leaf1 -- leaf2
leaf1 -- leaf3
@enduml
```


Let’s start with the ShippableOrder entity:

```java
public class ShippableOrder {
    private int orderId;
    private String address;
    private List<PackageItem> packageItems;
}
```
In this case, the entity doesn’t contain the paymentMethod field. That’s because, in our Shipping Context, we don’t care which payment method is used. The Shipping Context is just responsible for processing shipments of orders.

Also, the Parcel entity is specific to the Shipping Context:

```java
public class Parcel {
    private int orderId;
    private String address;
    private String trackingId;
    private List<PackageItem> packageItems;

    public float calculateTotalWeight() {
        return packageItems.stream().map(PackageItem::getWeight)
          .reduce(0F, Float::sum);
    }
 
    public boolean isTaxable() {
        return calculateEstimatedValue() > 100;
    }
 
    public float calculateEstimatedValue() {
        return packageItems.stream().map(PackageItem::getWeight)
          .reduce(0F, Float::sum);
    }
}
```
As we can see, it also contains specific business methods and acts as an aggregate root.

Finally, let’s define the ParcelShippingService:

```java
public class ParcelShippingService implements ShippingService {
    public static final String EVENT_ORDER_READY_FOR_SHIPMENT = "OrderReadyForShipmentEvent";
    private ShippingOrderRepository orderRepository;
    private EventBus eventBus;
    private Map<Integer, Parcel> shippedParcels = new HashMap<>();

    @Override
    public void shipOrder(int orderId) {
        Optional<ShippableOrder> order = this.orderRepository.findShippableOrder(orderId);
        order.ifPresent(completedOrder -> {
            Parcel parcel = new Parcel(completedOrder.getOrderId(), completedOrder.getAddress(), 
              completedOrder.getPackageItems());
            if (parcel.isTaxable()) {
                // Calculate additional taxes
            }
            // Ship parcel
            this.shippedParcels.put(completedOrder.getOrderId(), parcel);
        });
    }
 
    @Override
    public void listenToOrderEvents() {
        this.eventBus.subscribe(EVENT_ORDER_READY_FOR_SHIPMENT, new EventSubscriber() {
            @Override
            public <E extends ApplicationEvent> void onEvent(E event) {
                shipOrder(Integer.parseInt(event.getPayloadValue("order_id")));
            }
        });
    }
 
    @Override
    public Optional<Parcel> getParcelByOrderId(int orderId) {
        return Optional.ofNullable(this.shippedParcels.get(orderId));
    }
}
```
This service similarly uses the ShippingOrderRepository for fetching orders by id. More importantly, it subscribes to the OrderReadyForShipmentEvent event, which is published by another context. When this event occurs,  the service applies some rules and ships the order. For the sake of simplicity, we store shipped orders in a HashMap.

##  2. Context Maps
So far, we defined two contexts. However, we didn’t set any explicit relationships between them. For this purpose, DDD has the concept of Context Mapping. A Context Map is a visual description of relationships between different contexts of the system. This map shows how different parts coexist together to form the domain.

There are five main types of relationships between Bounded Contexts:

Partnership – a relationship between two contexts that cooperate to align the two teams with dependent goals
Shared Kernel – a kind of relationship when common parts of several contexts are extracted to another context/module to reduce code duplication
Customer-supplier – a connection between two contexts, where one context (upstream) produces data, and the other (downstream) consume it. In this relationship, both sides are interested in establishing the best possible communication
Conformist – this relationship also has upstream and downstream, however, downstream always conforms to the upstream’s APIs
Anticorruption layer – this type of relationship is widely used for legacy systems to adapt them to a new architecture and gradually migrate from the legacy codebase. The Anticorruption layer acts as an adapter to translate data from the upstream and protect from undesired changes
In our particular example, we'll use the Shared Kernel relationship. We won't define it in its pure form, but it will mostly act as a mediator of events in the system.

Thus, the SharedKernel module won’t contain any concrete implementations, only interfaces.

Let’s start with the EventBus interface:

```java
public interface EventBus {
    <E extends ApplicationEvent> void publish(E event);

    <E extends ApplicationEvent> void subscribe(String eventType, EventSubscriber subscriber);
 
    <E extends ApplicationEvent> void unsubscribe(String eventType, EventSubscriber subscriber);
}
```
This interface will be implemented later in our Infrastructure module.

Next, we create a base service interface with default methods to support event-driven communication:

```java
public interface ApplicationService {

    default <E extends ApplicationEvent> void publishEvent(E event) {
        EventBus eventBus = getEventBus();
        if (eventBus != null) {
            eventBus.publish(event);
        }
    }
 
    default <E extends ApplicationEvent> void subscribe(String eventType, EventSubscriber subscriber) {
        EventBus eventBus = getEventBus();
        if (eventBus != null) {
            eventBus.subscribe(eventType, subscriber);
        }
    }
 
    default <E extends ApplicationEvent> void unsubscribe(String eventType, EventSubscriber subscriber) {
        EventBus eventBus = getEventBus();
        if (eventBus != null) {
            eventBus.unsubscribe(eventType, subscriber);
        }
    }
 
    EventBus getEventBus();
 
    void setEventBus(EventBus eventBus);
}
```

So, service interfaces in bounded contexts extend this interface to have common event-related functionality.

##  3. Java 9 Modularity

Now, it’s time to explore how the Java 9 Module System can support the defined application structure.

The Java Platform Module System (JPMS) encourages to build more reliable and strongly encapsulated modules. As a result, these features can help to isolate our contexts and establish clear boundaries.

``` plantuml
@startuml

class class01 as "Main Module"{

}
class class02 as "Infrastructure Module"{

}
class class03 as "Shipping Context Module"{

}
class class04 as "Order Context Module"{

}
class class05 as "Shared Kernel Module"{

}

class01 --|> class02
class02 --|> class03
class02 --|> class04
class02 --|> class05
class03 --|> class05
class04 --|> class05

@enduml
```


Let's see our final module diagram:


### 3.1. SharedKernel Module
Let’s start with the SharedKernel module, which doesn't have any dependencies on other modules. So, the module-info.java looks like:

```
module com.baeldung.dddmodules.sharedkernel {
    exports com.baeldung.dddmodules.sharedkernel.events;
    exports com.baeldung.dddmodules.sharedkernel.service;
}
```
We export module interfaces, so they're available to other modules.

### 3.2. OrderContext Module
Next, let’s move our focus to the OrderContext module. It only requires interfaces defined in the SharedKernel module:
```java
module com.baeldung.dddmodules.ordercontext {
    requires com.baeldung.dddmodules.sharedkernel;
    exports com.baeldung.dddmodules.ordercontext.service;
    exports com.baeldung.dddmodules.ordercontext.model;
    exports com.baeldung.dddmodules.ordercontext.repository;
    provides com.baeldung.dddmodules.ordercontext.service.OrderService
      with com.baeldung.dddmodules.ordercontext.service.CustomerOrderService;
}
```
Also, we can see that this module exports the default implementation for the OrderService interface.

### 3.3. ShippingContext Module
Similarly to the previous module, let’s create the ShippingContext module definition file:
```java
module com.baeldung.dddmodules.shippingcontext {
    requires com.baeldung.dddmodules.sharedkernel;
    exports com.baeldung.dddmodules.shippingcontext.service;
    exports com.baeldung.dddmodules.shippingcontext.model;
    exports com.baeldung.dddmodules.shippingcontext.repository;
    provides com.baeldung.dddmodules.shippingcontext.service.ShippingService
      with com.baeldung.dddmodules.shippingcontext.service.ParcelShippingService;
}
```
In the same way, we export the default implementation for the ShippingService interface.

### 3.4. Infrastructure Module

Now it’s time to describe the Infrastructure module. This module contains the implementation details for the defined interfaces. We’ll start by creating a simple implementation for the EventBus interface:

```java
public class SimpleEventBus implements EventBus {
    private final Map<String, Set<EventSubscriber>> subscribers = new ConcurrentHashMap<>();

    @Override
    public <E extends ApplicationEvent> void publish(E event) {
        if (subscribers.containsKey(event.getType())) {
            subscribers.get(event.getType())
              .forEach(subscriber -> subscriber.onEvent(event));
        }
    }
 
    @Override
    public <E extends ApplicationEvent> void subscribe(String eventType, EventSubscriber subscriber) {
        Set<EventSubscriber> eventSubscribers = subscribers.get(eventType);
        if (eventSubscribers == null) {
            eventSubscribers = new CopyOnWriteArraySet<>();
            subscribers.put(eventType, eventSubscribers);
        }
        eventSubscribers.add(subscriber);
    }
 
    @Override
    public <E extends ApplicationEvent> void unsubscribe(String eventType, EventSubscriber subscriber) {
        if (subscribers.containsKey(eventType)) {
            subscribers.get(eventType).remove(subscriber);
        }
    }
}
```
Next, we need to implement the CustomerOrderRepository and ShippingOrderRepository interfaces. In most cases, the Order entity will be stored in the same table but used as a different entity model in bounded contexts.

It's very common to see a single entity containing mixed code from different areas of the business domain or low-level database mappings. For our implementation, we've split our entities according to the bounded contexts: CustomerOrder and ShippableOrder.

First, let’s create a class that will represent a whole persistent model:

```java
public static class PersistenceOrder {
    public int orderId;
    public String paymentMethod;
    public String address;
    public List<OrderItem> orderItems;

    public static class OrderItem {
        public int productId;
        public float unitPrice;
        public float itemWeight;
        public int quantity;
    }
}
```
We can see that this class contains all fields from both CustomerOrder and ShippableOrder entities.

To keep things simple, let’s simulate an in-memory database:

```java
public class InMemoryOrderStore implements CustomerOrderRepository, ShippingOrderRepository {
    private Map<Integer, PersistenceOrder> ordersDb = new HashMap<>();

    @Override
    public void saveCustomerOrder(CustomerOrder order) {
        this.ordersDb.put(order.getOrderId(), new PersistenceOrder(order.getOrderId(),
          order.getPaymentMethod(),
          order.getAddress(),
          order
            .getOrderItems()
            .stream()
            .map(orderItem ->
              new PersistenceOrder.OrderItem(orderItem.getProductId(),
                orderItem.getQuantity(),
                orderItem.getUnitWeight(),
                orderItem.getUnitPrice()))
            .collect(Collectors.toList())
        ));
    }
 
    @Override
    public Optional<ShippableOrder> findShippableOrder(int orderId) {
        if (!this.ordersDb.containsKey(orderId)) return Optional.empty();
        PersistenceOrder orderRecord = this.ordersDb.get(orderId);
        return Optional.of(
          new ShippableOrder(orderRecord.orderId, orderRecord.orderItems
            .stream().map(orderItem -> new PackageItem(orderItem.productId,
              orderItem.itemWeight,
              orderItem.quantity * orderItem.unitPrice)
            ).collect(Collectors.toList())));
    }
}
```
Here, we persist and retrieve different types of entities by converting persistent models to or from an appropriate type.

Finally, let’s create the module definition:

```java
module com.baeldung.dddmodules.infrastructure {
    requires transitive com.baeldung.dddmodules.sharedkernel;
    requires transitive com.baeldung.dddmodules.ordercontext;
    requires transitive com.baeldung.dddmodules.shippingcontext;
    provides com.baeldung.dddmodules.sharedkernel.events.EventBus
      with com.baeldung.dddmodules.infrastructure.events.SimpleEventBus;
    provides com.baeldung.dddmodules.ordercontext.repository.CustomerOrderRepository
      with com.baeldung.dddmodules.infrastructure.db.InMemoryOrderStore;
    provides com.baeldung.dddmodules.shippingcontext.repository.ShippingOrderRepository
      with com.baeldung.dddmodules.infrastructure.db.InMemoryOrderStore;
}
```
Using the provides with clause, we’re providing the implementation of a few interfaces that were defined in other modules.




Furthermore, this module acts as an aggregator of dependencies, so we use the requires transitive keyword. As a result, a module that requires the Infrastructure module will transitively get all these dependencies.


### 3.5. Main Module
To conclude, let’s define a module that will be the entry point to our application:

```java
module com.baeldung.dddmodules.mainapp {
    uses com.baeldung.dddmodules.sharedkernel.events.EventBus;
    uses com.baeldung.dddmodules.ordercontext.service.OrderService;
    uses com.baeldung.dddmodules.ordercontext.repository.CustomerOrderRepository;
    uses com.baeldung.dddmodules.shippingcontext.repository.ShippingOrderRepository;
    uses com.baeldung.dddmodules.shippingcontext.service.ShippingService;
    requires transitive com.baeldung.dddmodules.infrastructure;
}
```
As we’ve just set transitive dependencies on the Infrastructure module, we don't need to require them explicitly here.

On the other hand, we list these dependencies with the uses keyword. The uses clause instructs ServiceLoader, which we’ll discover in the next chapter, that this module wants to use these interfaces. However, it doesn’t require implementations to be available during compile-time.

## 4. Running the Application

Finally, we're almost ready to build our application. We'll leverage Maven for building our project. This makes it much easier to work with modules.

### 4.1. Project Structure
Our project contains five modules and the parent module. Let's take a look at our project structure:

```
ddd-modules (the root directory)
pom.xml
|-- infrastructure
    |-- src
        |-- main
            | -- java
            module-info.java
            |-- com.baeldung.dddmodules.infrastructure
    pom.xml
|-- mainapp
    |-- src
        |-- main
            | -- java
            module-info.java
            |-- com.baeldung.dddmodules.mainapp
    pom.xml
|-- ordercontext
    |-- src
        |-- main
            | -- java
            module-info.java
            |--com.baeldung.dddmodules.ordercontext
    pom.xml
|-- sharedkernel
    |-- src
        |-- main
            | -- java
            module-info.java
            |-- com.baeldung.dddmodules.sharedkernel
    pom.xml
|-- shippingcontext
    |-- src
        |-- main
            | -- java
            module-info.java
            |-- com.baeldung.dddmodules.shippingcontext
    pom.xml
```

### 4.2. Main Application
By now, we have everything except the main application, so let's define our main method:

```java
public static void main(String args[]) {
    Map<Class<?>, Object> container = createContainer();
    OrderService orderService = (OrderService) container.get(OrderService.class);
    ShippingService shippingService = (ShippingService) container.get(ShippingService.class);
    shippingService.listenToOrderEvents();

    CustomerOrder customerOrder = new CustomerOrder();
    int orderId = 1;
    customerOrder.setOrderId(orderId);
    List<OrderItem> orderItems = new ArrayList<OrderItem>();
    orderItems.add(new OrderItem(1, 2, 3, 1));
    orderItems.add(new OrderItem(2, 1, 1, 1));
    orderItems.add(new OrderItem(3, 4, 11, 21));
    customerOrder.setOrderItems(orderItems);
    customerOrder.setPaymentMethod("PayPal");
    customerOrder.setAddress("Full address here");
    orderService.placeOrder(customerOrder);
 
    if (orderId == shippingService.getParcelByOrderId(orderId).get().getOrderId()) {
        System.out.println("Order has been processed and shipped successfully");
    }
}
```
Let's briefly discuss our main method. In this method, we are simulating a simple customer order flow by using previously defined services. At first, we created the order with three items and provided the necessary shipping and payment information. Next, we submitted the order and finally checked whether it was shipped and processed successfully.

But how did we get all dependencies and why does the createContainer method return Map<Class<?>, Object>? Let's take a closer look at this method.

### 4.3. Dependency Injection Using ServiceLoader
In this project, we don't have any Spring IoC dependencies, so alternatively, we'll use the ServiceLoader API for discovering implementations of services. This is not a new feature — the ServiceLoader API itself has been around since Java 6.

We can obtain a loader instance by invoking one of the static load methods of the ServiceLoader class. The load method returns the Iterable type so that we can iterate over discovered implementations.

Now, let's apply the loader to resolve our dependencies:

```java
public static Map<Class<?>, Object> createContainer() {
    EventBus eventBus = ServiceLoader.load(EventBus.class).findFirst().get();

    CustomerOrderRepository customerOrderRepository = ServiceLoader.load(CustomerOrderRepository.class)
      .findFirst().get();
    ShippingOrderRepository shippingOrderRepository = ServiceLoader.load(ShippingOrderRepository.class)
      .findFirst().get();
 
    ShippingService shippingService = ServiceLoader.load(ShippingService.class).findFirst().get();
    shippingService.setEventBus(eventBus);
    shippingService.setOrderRepository(shippingOrderRepository);
    OrderService orderService = ServiceLoader.load(OrderService.class).findFirst().get();
    orderService.setEventBus(eventBus);
    orderService.setOrderRepository(customerOrderRepository);
 
    HashMap<Class<?>, Object> container = new HashMap<>();
    container.put(OrderService.class, orderService);
    container.put(ShippingService.class, shippingService);
 
    return container;
}
```
Here, we're calling the static load method for every interface we need, which creates a new loader instance each time. As a result, it won't cache already resolved dependencies — instead, it'll create new instances every time.

Generally, service instances can be created in one of two ways. Either the service implementation class must have a public no-arg constructor, or it must use a static provider method.

As a consequence, most of our services have no-arg constructors and setter methods for dependencies. But, as we've already seen, the InMemoryOrderStore class implements two interfaces: CustomerOrderRepository and ShippingOrderRepository.

However, if we request each of these interfaces using the load method, we'll get different instances of the InMemoryOrderStore. That is not desirable behavior, so let's use the provider method technique to cache the instance:

```java
public class InMemoryOrderStore implements CustomerOrderRepository, ShippingOrderRepository {
    private volatile static InMemoryOrderStore instance = new InMemoryOrderStore();

    public static InMemoryOrderStore provider() {
        return instance;
    }
}
```
We've applied the Singleton pattern to cache a single instance of the InMemoryOrderStore class and return it from the provider method.

If the service provider declares a provider method, then the ServiceLoader invokes this method to obtain an instance of a service. Otherwise, it will try to create an instance using the no-arguments constructor via Reflection. As a result, we can change the service provider mechanism without affecting our createContainer method.

And finally, we provide resolved dependencies to services via setters and return the configured services.

Finally, we can run the application.

### 5. Conclusion
In this article, we've discussed some critical DDD concepts: Bounded Context, Ubiquitous Language, and Context Mapping. While dividing a system into Bounded Contexts has a lot of benefits, at the same time, there is no need to apply this approach everywhere.

Next, we've seen how to use the Java 9 Module System along with Bounded Context to create strongly encapsulated modules.

Furthermore, we've covered the default ServiceLoader mechanism for discovering dependencies.


