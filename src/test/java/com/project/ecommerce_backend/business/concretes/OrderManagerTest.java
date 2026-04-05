package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.business.abstracts.*;
import com.project.ecommerce_backend.business.dtos.events.OrderCreatedEvent;
import com.project.ecommerce_backend.business.dtos.requests.order.AddOrderRequest;
import com.project.ecommerce_backend.business.dtos.requests.order.UpdateOrderStatusRequest;
import com.project.ecommerce_backend.business.dtos.responses.installment.InstallmentOptionResponse;
import com.project.ecommerce_backend.business.dtos.responses.order.AddOrderResponse;
import com.project.ecommerce_backend.business.dtos.responses.order.ListUserOrdersResponse;
import com.project.ecommerce_backend.business.dtos.responses.order.OrderDetailResponse;
import com.project.ecommerce_backend.business.dtos.responses.order.UserOrderDetailResponse;
import com.project.ecommerce_backend.business.helpers.CacheHelper;
import com.project.ecommerce_backend.core.constants.Messages;
import com.project.ecommerce_backend.core.exceptions.types.BusinessException;
import com.project.ecommerce_backend.core.exceptions.types.NotFoundException;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.core.utils.mapper.ModelMapperService;
import com.project.ecommerce_backend.core.utils.result.DataResult;
import com.project.ecommerce_backend.core.utils.result.Result;
import com.project.ecommerce_backend.entities.concretes.*;
import com.project.ecommerce_backend.entities.enums.OrderStatusEnum;
import com.project.ecommerce_backend.repositories.abstracts.OrderItemRepository;
import com.project.ecommerce_backend.repositories.abstracts.OrderRepository;
import com.project.ecommerce_backend.repositories.abstracts.OrderTrackingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderManagerTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private CustomerService customerService;
    @Mock private ShoppingCartService shoppingCartService;
    @Mock private ProductService productService;
    @Mock private AddressService addressService;
    @Mock private ShippingMethodService shippingMethodService;
    @Mock private OrderStatusService orderStatusService;
    @Mock private PaymentMethodService paymentMethodService;
    @Mock private InvoiceService invoiceService;
    @Mock private InstallmentService installmentService;
    @Mock private OrderTrackingRepository orderTrackingRepository;
    @Mock private CarrierService carrierService;
    @Mock private ModelMapperService modelMapperService;
    @Mock private MessageService messageService;
    @Mock private KafkaProducerService kafkaProducerService;
    @Mock private CacheHelper cacheHelper;
    @Mock private ModelMapper modelMapper;

    @InjectMocks
    private OrderManager orderManager;

    private Long customerId = 1L;
    private Long orderId = 10L;
    private Order order;
    private OrderStatus preparingStatus;
    private OrderStatus shippedStatus;
    private OrderStatus deliveredStatus;
    private OrderStatus cancelledStatus;

    @BeforeEach
    void setUp() {
        preparingStatus = new OrderStatus();
        preparingStatus.setId(1L);
        preparingStatus.setStatusName(OrderStatusEnum.PREPARING);

        shippedStatus = new OrderStatus();
        shippedStatus.setId(2L);
        shippedStatus.setStatusName(OrderStatusEnum.SHIPPED);

        deliveredStatus = new OrderStatus();
        deliveredStatus.setId(3L);
        deliveredStatus.setStatusName(OrderStatusEnum.DELIVERED);

        cancelledStatus = new OrderStatus();
        cancelledStatus.setId(4L);
        cancelledStatus.setStatusName(OrderStatusEnum.CANCELLED);

        order = new Order();
        order.setId(orderId);
        order.setCustomer(new Customer());
        order.getCustomer().setId(customerId);
        order.setOrderStatus(preparingStatus);
    }

    // --- GET ORDER DETAIL BY ID TESTS ---

    @Test
    void getOrderDetailById_ShouldReturnDetail_WhenAuthorized() {
        when(cacheHelper.getAuthenticatedUserId()).thenReturn(customerId);
        when(orderRepository.findByIdAndCustomerIdWithDetails(orderId, customerId)).thenReturn(Optional.of(order));
        when(modelMapperService.getMapper()).thenReturn(modelMapper);
        when(modelMapper.map(order, UserOrderDetailResponse.class)).thenReturn(new UserOrderDetailResponse());
        when(messageService.getMessage(Messages.Order.ORDER_DETAIL_SUCCESSFULLY_LISTED)).thenReturn("Detail listed");

        DataResult<UserOrderDetailResponse> result = orderManager.getOrderDetailById(orderId);

        assertTrue(result.isSuccess());
        assertEquals("Detail listed", result.getMessage());
    }

    @Test
    void getOrderDetailById_ShouldReturnDetail_WhenSellerAuthorized() {
        when(cacheHelper.getAuthenticatedUserId()).thenReturn(customerId);
        when(orderRepository.findByIdAndCustomerIdWithDetails(orderId, customerId)).thenReturn(Optional.empty());
        when(orderRepository.existsByOrderIdAndOrderItemProductItemProductSupplierId(orderId, customerId)).thenReturn(true);
        when(orderRepository.findByIdWithDetails(orderId)).thenReturn(Optional.of(order));
        when(modelMapperService.getMapper()).thenReturn(modelMapper);
        when(modelMapper.map(order, UserOrderDetailResponse.class)).thenReturn(new UserOrderDetailResponse());

        DataResult<UserOrderDetailResponse> result = orderManager.getOrderDetailById(orderId);

        assertTrue(result.isSuccess());
    }

    @Test
    void getOrderDetailById_ShouldThrowNotFoundException_WhenUnauthorized() {
        when(cacheHelper.getAuthenticatedUserId()).thenReturn(customerId);
        when(orderRepository.findByIdAndCustomerIdWithDetails(orderId, customerId)).thenReturn(Optional.empty());
        when(orderRepository.existsByOrderIdAndOrderItemProductItemProductSupplierId(orderId, customerId)).thenReturn(false);
        when(messageService.getMessage(Messages.Order.ORDER_NOT_FOUND_OR_NOT_AUTHORIZED)).thenReturn("Not authorized");

        NotFoundException exception = assertThrows(NotFoundException.class, () -> orderManager.getOrderDetailById(orderId));
        assertEquals("Not authorized", exception.getMessage());
    }

    // --- GET ALL ORDERS TESTS ---

    @Test
    void getAllOrders_ShouldReturnList() {
        Pageable pageable = Pageable.unpaged();
        Slice<Order> slice = new PageImpl<>(Collections.singletonList(order));

        when(cacheHelper.getAuthenticatedUserId()).thenReturn(customerId);
        when(orderRepository.findSliceByCustomerId(customerId, pageable)).thenReturn(slice);
        when(modelMapperService.getMapper()).thenReturn(modelMapper);
        when(modelMapper.map(order, ListUserOrdersResponse.class)).thenReturn(new ListUserOrdersResponse());
        when(messageService.getMessage(Messages.Order.ORDER_SUCCESSFULLY_LISTED)).thenReturn("Orders listed");

        DataResult<List<ListUserOrdersResponse>> result = orderManager.getAllOrders(pageable);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
        assertEquals("Orders listed", result.getMessage());
    }

    // --- ADD ORDER TESTS ---

    @Test
    void add_ShouldCreateOrder_WhenValid() {
        AddOrderRequest request = new AddOrderRequest();
        request.setCustomerId(customerId);
        request.setExpectedTotalAmount(BigDecimal.TEN);
        request.setInstallmentCount(1);
        request.setPaymentMethodId(1L);

        ShoppingCart cart = new ShoppingCart();
        cart.setId(1L);
        ProductItem productItem = new ProductItem();
        productItem.setId(1L);
        productItem.setUnitPrice(BigDecimal.TEN);
        Product product = new Product();
        product.setVatRate(BigDecimal.ZERO);
        productItem.setProduct(product);
        ShoppingCartItem cartItem = new ShoppingCartItem();
        cartItem.setProductItem(productItem);
        cartItem.setQuantity(1);
        cart.setCartItems(Set.of(cartItem));

        ShippingMethod shippingMethod = new ShippingMethod();
        shippingMethod.setShippingPrice(BigDecimal.ZERO);

        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setId(1L);
        paymentMethod.setCardFamily("Visa");

        InstallmentOptionResponse installmentOption = new InstallmentOptionResponse();
        installmentOption.setTotalAmount(BigDecimal.TEN);

        when(customerService.getByIdAsEntity(customerId)).thenReturn(new Customer());
        when(shoppingCartService.getActiveCartByCustomerId(customerId)).thenReturn(cart);
        when(addressService.getByIdAndCustomerId(any(), any())).thenReturn(new Address());
        when(shippingMethodService.getByIdAsEntity(any())).thenReturn(shippingMethod);
        when(orderStatusService.getInitialStatus()).thenReturn(preparingStatus);
        when(paymentMethodService.getByIdAndCustomerId(any(), any())).thenReturn(paymentMethod);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(installmentService.getSpecificOption(any(), any(), anyInt())).thenReturn(installmentOption);
        when(modelMapperService.getMapper()).thenReturn(modelMapper);
        when(modelMapper.map(order, AddOrderResponse.class)).thenReturn(new AddOrderResponse());
        when(messageService.getMessage(Messages.Order.ORDER_SUCCESSFULLY_ADDED)).thenReturn("Order added");

        DataResult<AddOrderResponse> result = orderManager.add(request);

        assertTrue(result.isSuccess());
        assertEquals("Order added", result.getMessage());
        verify(kafkaProducerService).sendOrderCreatedEvent(any(OrderCreatedEvent.class));
        verify(shoppingCartService).clearCartById(cart.getId());
    }

    @Test
    void add_ShouldThrowBusinessException_WhenCartIsEmpty() {
        AddOrderRequest request = new AddOrderRequest();
        request.setCustomerId(customerId);

        ShoppingCart cart = new ShoppingCart();
        cart.setCartItems(Collections.emptySet());

        when(customerService.getByIdAsEntity(customerId)).thenReturn(new Customer());
        when(shoppingCartService.getActiveCartByCustomerId(customerId)).thenReturn(cart);
        when(addressService.getByIdAndCustomerId(any(), any())).thenReturn(new Address());
        when(shippingMethodService.getByIdAsEntity(any())).thenReturn(new ShippingMethod());
        when(orderStatusService.getInitialStatus()).thenReturn(preparingStatus);
        when(paymentMethodService.getByIdAndCustomerId(any(), any())).thenReturn(new PaymentMethod());
        when(messageService.getMessage(Messages.Order.ORDER_ERROR_EMPTY_CART)).thenReturn("Empty cart");

        BusinessException exception = assertThrows(BusinessException.class, () -> orderManager.add(request));
        assertEquals("Empty cart", exception.getMessage());
    }

    @Test
    void add_ShouldThrowBusinessException_WhenTotalAmountMismatch() {
        AddOrderRequest request = new AddOrderRequest();
        request.setCustomerId(customerId);
        request.setExpectedTotalAmount(BigDecimal.ONE); // Mismatch
        request.setInstallmentCount(1);

        ShoppingCart cart = new ShoppingCart();
        ProductItem productItem = new ProductItem();
        productItem.setUnitPrice(BigDecimal.TEN);
        Product product = new Product();
        product.setVatRate(BigDecimal.ZERO);
        productItem.setProduct(product);
        ShoppingCartItem cartItem = new ShoppingCartItem();
        cartItem.setProductItem(productItem);
        cartItem.setQuantity(1);
        cart.setCartItems(Set.of(cartItem));

        ShippingMethod shippingMethod = new ShippingMethod();
        shippingMethod.setShippingPrice(BigDecimal.ZERO);

        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setCardFamily("Visa");

        InstallmentOptionResponse installmentOption = new InstallmentOptionResponse();
        installmentOption.setTotalAmount(BigDecimal.TEN);

        when(customerService.getByIdAsEntity(customerId)).thenReturn(new Customer());
        when(shoppingCartService.getActiveCartByCustomerId(customerId)).thenReturn(cart);
        when(addressService.getByIdAndCustomerId(any(), any())).thenReturn(new Address());
        when(shippingMethodService.getByIdAsEntity(any())).thenReturn(shippingMethod);
        when(orderStatusService.getInitialStatus()).thenReturn(preparingStatus);
        when(paymentMethodService.getByIdAndCustomerId(any(), any())).thenReturn(paymentMethod);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(installmentService.getSpecificOption(any(), any(), anyInt())).thenReturn(installmentOption);
        when(messageService.getMessage(Messages.Errors.ORDER_TOTAL_DOES_NOT_MATCH)).thenReturn("Total mismatch");

        BusinessException exception = assertThrows(BusinessException.class, () -> orderManager.add(request));
        assertEquals("Total mismatch", exception.getMessage());
    }

    // --- SHIPPING ORDER TESTS ---

    @Test
    void shippingOrder_ShouldShip_WhenPreparing() {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setCarrierId(1L);
        request.setTrackingNumber("123");

        when(orderRepository.findByIdWithDetails(orderId)).thenReturn(Optional.of(order));
        when(orderStatusService.getInitialStatus()).thenReturn(preparingStatus);
        when(carrierService.getByIdAsEntity(1L)).thenReturn(new Carrier());
        when(orderStatusService.getShippedStatus()).thenReturn(shippedStatus);
        when(orderRepository.save(order)).thenReturn(order);
        when(modelMapperService.getMapper()).thenReturn(modelMapper);
        when(modelMapper.map(order, OrderDetailResponse.class)).thenReturn(new OrderDetailResponse());
        when(messageService.getMessage(Messages.Order.ORDER_SUCCESSFULLY_SHIPPED)).thenReturn("Shipped");

        DataResult<OrderDetailResponse> result = orderManager.shippingOrder(orderId, request);

        assertTrue(result.isSuccess());
        assertEquals("Shipped", result.getMessage());
        verify(orderTrackingRepository).save(any(OrderTracking.class));
    }

    @Test
    void shippingOrder_ShouldThrowBusinessException_WhenNotShippable() {
        order.setOrderStatus(shippedStatus); // Already shipped

        when(orderRepository.findByIdWithDetails(orderId)).thenReturn(Optional.of(order));
        when(orderStatusService.getInitialStatus()).thenReturn(preparingStatus);
        when(messageService.getMessage(Messages.Order.ORDER_IS_NOT_SHIPPABLE)).thenReturn("Not shippable");

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        BusinessException exception = assertThrows(BusinessException.class,
                () -> orderManager.shippingOrder(orderId, request));
        assertEquals("Not shippable", exception.getMessage());
    }

    // --- DELIVERY ORDER TESTS ---

    @Test
    void deliveryOrder_ShouldDeliver_WhenShipped() {
        order.setOrderStatus(shippedStatus);

        when(orderRepository.findByIdWithDetails(orderId)).thenReturn(Optional.of(order));
        when(orderStatusService.getShippedStatus()).thenReturn(shippedStatus);
        when(orderStatusService.getDeliveredStatus()).thenReturn(deliveredStatus);
        when(orderRepository.save(order)).thenReturn(order);
        when(modelMapperService.getMapper()).thenReturn(modelMapper);
        when(modelMapper.map(order, OrderDetailResponse.class)).thenReturn(new OrderDetailResponse());
        when(messageService.getMessage(Messages.Order.ORDER_SUCCESSFULLY_DELIVERED)).thenReturn("Delivered");

        DataResult<OrderDetailResponse> result = orderManager.deliveryOrder(orderId);

        assertTrue(result.isSuccess());
        assertEquals("Delivered", result.getMessage());
    }

    @Test
    void deliveryOrder_ShouldThrowBusinessException_WhenNotDeliverable() {
        order.setOrderStatus(preparingStatus); // Not shipped yet

        when(orderRepository.findByIdWithDetails(orderId)).thenReturn(Optional.of(order));
        when(orderStatusService.getShippedStatus()).thenReturn(shippedStatus);
        when(messageService.getMessage(Messages.Order.ORDER_IS_NOT_DELIVERABLE)).thenReturn("Not deliverable");

        BusinessException exception = assertThrows(BusinessException.class, () -> orderManager.deliveryOrder(orderId));
        assertEquals("Not deliverable", exception.getMessage());
    }

    // --- CANCEL ORDER TESTS ---

    @Test
    void cancelOrder_ShouldCancel_WhenPreparing() {
        order.setOrderItems(new HashSet<>());
        Invoice invoice = new Invoice();
        invoice.setId(1L);
        order.setInvoice(invoice);

        when(cacheHelper.getAuthenticatedUserId()).thenReturn(customerId);
        when(orderRepository.findByIdAndCustomerIdWithDetails(orderId, customerId)).thenReturn(Optional.of(order));
        when(orderStatusService.getInitialStatus()).thenReturn(preparingStatus);
        when(orderStatusService.getCancelledStatus()).thenReturn(cancelledStatus);
        when(messageService.getMessage(Messages.Order.ORDER_SUCCESSFULLY_CANCELLED)).thenReturn("Cancelled");

        Result result = orderManager.cancelOrder(orderId);

        assertTrue(result.isSuccess());
        assertEquals("Cancelled", result.getMessage());
        verify(invoiceService).cancelInvoice(1L);
    }

    @Test
    void cancelOrder_ShouldThrowBusinessException_WhenNotCancellable() {
        order.setOrderStatus(shippedStatus); // Cannot cancel shipped order

        when(cacheHelper.getAuthenticatedUserId()).thenReturn(customerId);
        when(orderRepository.findByIdAndCustomerIdWithDetails(orderId, customerId)).thenReturn(Optional.of(order));
        when(orderStatusService.getInitialStatus()).thenReturn(preparingStatus);
        when(messageService.getMessage(Messages.Order.ORDER_IS_NOT_CANCELLABLE)).thenReturn("Not cancellable");

        BusinessException exception = assertThrows(BusinessException.class, () -> orderManager.cancelOrder(orderId));
        assertEquals("Not cancellable", exception.getMessage());
    }

    // --- UPDATE ORDER STATUS TESTS ---

    @Test
    void updateOrderStatus_ShouldUpdate_WhenValidTransition() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(messageService.getMessage(Messages.Order.ORDER_STATUS_SUCCESSFULLY_UPDATED)).thenReturn("Status updated");

        Result result = orderManager.updateOrderStatus(orderId, shippedStatus);

        assertTrue(result.isSuccess());
        assertEquals(shippedStatus, order.getOrderStatus());
        assertEquals("Status updated", result.getMessage());
    }

    @Test
    void updateOrderStatus_ShouldThrowException_WhenInvalidTransition() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(messageService.getMessageWithParams(any(), any(), any(), any(), any())).thenReturn("Invalid transition");

        BusinessException exception = assertThrows(BusinessException.class, 
                () -> orderManager.updateOrderStatus(orderId, deliveredStatus)); // Cannot jump from PREPARING to DELIVERED
        assertEquals("Invalid transition", exception.getMessage());
    }

    // --- FIND ORDER ITEM TESTS ---

    @Test
    void findOrderItemByIdAndCustomerId_ShouldReturnItem_WhenFound() {
        OrderItem item = new OrderItem();
        when(orderItemRepository.findByIdAndOrderCustomerId(1L, customerId)).thenReturn(Optional.of(item));

        OrderItem result = orderManager.findOrderItemByIdAndCustomerId(1L, customerId);
        assertNotNull(result);
    }

    @Test
    void findOrderItemByIdAndCustomerId_ShouldThrowNotFound_WhenMissing() {
        when(orderItemRepository.findByIdAndOrderCustomerId(1L, customerId)).thenReturn(Optional.empty());
        when(messageService.getMessage(Messages.Order.ORDER_ITEM_NOT_FOUND_OR_NOT_AUTHORIZED)).thenReturn("Item not found");

        NotFoundException exception = assertThrows(NotFoundException.class, 
                () -> orderManager.findOrderItemByIdAndCustomerId(1L, customerId));
        assertEquals("Item not found", exception.getMessage());
    }

    // --- EXISTS BY SHIPPING ADDRESS ID TESTS ---

    @Test
    void existsByShippingAddressId_ShouldReturnTrue_WhenExists() {
        when(orderRepository.existsByShippingAddressId(1L)).thenReturn(true);
        assertTrue(orderManager.existsByShippingAddressId(1L));
    }

    // --- FIND BY ID AS ENTITY WITH DETAILS TESTS ---

    @Test
    void findByIdAsEntityWithDetails_ShouldReturnOrder_WhenFound() {
        when(orderRepository.findByIdWithDetails(orderId)).thenReturn(Optional.of(order));
        Order result = orderManager.findByIdAsEntityWithDetails(orderId);
        assertNotNull(result);
        assertEquals(orderId, result.getId());
    }
}