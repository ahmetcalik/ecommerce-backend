package com.project.ecommerce_backend.core.utils.mapper;

import com.project.ecommerce_backend.business.dtos.responses.address.AddressDetailResponse;
import com.project.ecommerce_backend.business.dtos.responses.address.ListAddressResponse;
import com.project.ecommerce_backend.business.dtos.responses.cart.CartItemResponse;
import com.project.ecommerce_backend.business.dtos.responses.cart.CartResponse;
import com.project.ecommerce_backend.business.dtos.responses.category.AddCategoryResponse;
import com.project.ecommerce_backend.business.dtos.responses.category.CategoryDetailResponse;
import com.project.ecommerce_backend.business.dtos.responses.category.UpdateCategoryResponse;
import com.project.ecommerce_backend.business.dtos.responses.invoice.ListUserInvoicesResponse;
import com.project.ecommerce_backend.business.dtos.responses.invoice.UserInvoiceDetailResponse;
import com.project.ecommerce_backend.business.dtos.responses.order.AddOrderResponse;
import com.project.ecommerce_backend.business.dtos.responses.order.ListUserOrdersResponse;
import com.project.ecommerce_backend.business.dtos.responses.order.OrderDetailResponse;
import com.project.ecommerce_backend.business.dtos.responses.order.OrderItemResponse;
import com.project.ecommerce_backend.business.dtos.responses.order.UserOrderDetailResponse;
import com.project.ecommerce_backend.business.dtos.responses.payment.ListPaymentMethodResponse;
import com.project.ecommerce_backend.business.dtos.responses.payment.PaymentMethodDetailResponse;
import com.project.ecommerce_backend.business.dtos.responses.product.AddProductResponse;
import com.project.ecommerce_backend.business.dtos.responses.product.ListProductsResponse;
import com.project.ecommerce_backend.business.dtos.responses.product.ProductDetailResponse;
import com.project.ecommerce_backend.business.dtos.responses.return_request.ReturnRequestResponse;
import com.project.ecommerce_backend.core.constants.Messages;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.entities.concretes.*;
import com.project.ecommerce_backend.entities.enums.OrderStatusEnum;
import com.project.ecommerce_backend.repositories.abstracts.CustomerAddressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModelMapperMenagerTest {

    @Mock
    private MessageService messageService;

    @Mock
    private CustomerAddressRepository customerAddressRepository;

    private ModelMapper modelMapper;

    @BeforeEach
    void setUp() {
        modelMapper = new ModelMapper();
        ModelMapperMenager modelMapperMenager = new ModelMapperMenager(modelMapper, messageService, customerAddressRepository);
        modelMapperMenager.init();
        assertNotNull(modelMapperMenager.getMapper());
    }

    @Test
    void testConfigureCategoryMappings_WithParentCategory() {
        Category parentCategory = new Category();
        parentCategory.setId(1L);
        parentCategory.setName("Electronics");

        Category category = new Category();
        category.setId(2L);
        category.setName("Smartphones");
        category.setParentCategory(parentCategory);

        AddCategoryResponse response = modelMapper.map(category, AddCategoryResponse.class);

        assertEquals(category.getId(), response.getId());
        assertEquals(category.getName(), response.getName());
        assertEquals(parentCategory.getId(), response.getParentCategoryId());
        assertEquals(parentCategory.getName(), response.getParentCategoryName());
    }

    @Test
    void testConfigureCategoryMappings_WithoutParentCategory() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Electronics");

        AddCategoryResponse response = modelMapper.map(category, AddCategoryResponse.class);

        assertEquals(category.getId(), response.getId());
        assertEquals(category.getName(), response.getName());
        assertNull(response.getParentCategoryId());
        assertNull(response.getParentCategoryName());
    }
    
    @Test
    void testConfigureCategoryMappings_UpdateWithParentCategory() {
        Category parentCategory = new Category();
        parentCategory.setId(1L);
        parentCategory.setName("Electronics");

        Category category = new Category();
        category.setId(2L);
        category.setName("Smartphones");
        category.setParentCategory(parentCategory);

        UpdateCategoryResponse response = modelMapper.map(category, UpdateCategoryResponse.class);

        assertEquals(category.getId(), response.getId());
        assertEquals(category.getName(), response.getName());
        assertEquals(parentCategory.getId(), response.getParentCategoryId());
    }

    @Test
    void testConfigureCategoryMappings_UpdateWithoutParentCategory() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Electronics");

        UpdateCategoryResponse response = modelMapper.map(category, UpdateCategoryResponse.class);

        assertEquals(category.getId(), response.getId());
        assertEquals(category.getName(), response.getName());
        assertNull(response.getParentCategoryId());
    }

    @Test
    void testConfigureCategoryMappings_CategoryToDetailResponse() {
        Category parent = new Category();
        parent.setId(1L);
        parent.setName("Parent");

        Category child = new Category();
        child.setId(2L);
        child.setName("Child");
        child.setParentCategory(parent);

        CategoryDetailResponse response = modelMapper.map(child, CategoryDetailResponse.class);

        assertEquals(2, response.getBreadCrumb().size());
        assertEquals("Parent", response.getBreadCrumb().get(0).getName());
        assertEquals("Child", response.getBreadCrumb().get(1).getName());
    }

    @Test
    void testCategoryDetail_withNullCategories() {
        Category category = new Category();
        category.setId(1L);
        category.setCategories(null);
        
        CategoryDetailResponse response = modelMapper.map(category, CategoryDetailResponse.class);
        
        assertNotNull(response.getSubCategories());
        assertTrue(response.getSubCategories().isEmpty());
    }

    @Test
    void testConfigureProductMappings_productToListProductsResponse() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Test Product");

        ProductImage mainImage = new ProductImage();
        mainImage.setIsMainImage(true);
        mainImage.setImageUrl("main_image_url");
        product.setProductImages(Collections.singleton(mainImage));

        ProductItem item1 = new ProductItem();
        item1.setUnitPrice(BigDecimal.TEN);
        ProductItem item2 = new ProductItem();
        item2.setUnitPrice(BigDecimal.ONE);
        product.setProductItems(Set.of(item1, item2));

        Category category = new Category();
        category.setName("Test Category");
        ProductCategory productCategory = new ProductCategory();
        productCategory.setCategory(category);
        product.setProductCategories(Collections.singleton(productCategory));

        ListProductsResponse response = modelMapper.map(product, ListProductsResponse.class);

        assertEquals("Test Product", response.getName());
        assertEquals("main_image_url", response.getMainImageUrl());
        assertEquals(BigDecimal.ONE, response.getPrice());
        assertEquals("Test Category", response.getCategoryName());
    }

    @Test
    void testConfigureProductMappings_productToDetailResponse() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setDescription("Description");
        product.setCDate(OffsetDateTime.now());
        product.setUDate(OffsetDateTime.now());

        Supplier supplier = new Supplier();
        supplier.setId(1L);
        supplier.setCompanyName("Supplier Name");
        product.setSupplier(supplier);

        ProductDetailResponse response = modelMapper.map(product, ProductDetailResponse.class);

        assertEquals("Test Product", response.getName());
        assertEquals("Description", response.getDescription());
        assertEquals("Supplier Name", response.getSupplier().getCompanyName());
    }

    @Test
    void testConfigureProductMappings_productWithNullCollections() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Null Collections Product");
        product.setProductImages(null);
        product.setProductItems(null);
        product.setProductCategories(null);
        product.setSupplier(null);

        ProductDetailResponse detailResponse = modelMapper.map(product, ProductDetailResponse.class);
        ListProductsResponse listResponse = modelMapper.map(product, ListProductsResponse.class);
        AddProductResponse addResponse = modelMapper.map(product, AddProductResponse.class);

        assertNull(detailResponse.getSupplier());
        assertTrue(detailResponse.getCategories().isEmpty());
        assertTrue(detailResponse.getImages().isEmpty());
        assertTrue(detailResponse.getItems().isEmpty());

        assertNull(listResponse.getMainImageUrl());
        assertEquals(BigDecimal.ZERO, listResponse.getPrice());
        
        assertNull(addResponse.getSupplier());
        assertTrue(addResponse.getCategories().isEmpty());
        assertTrue(addResponse.getItems().isEmpty());
    }

    @Test
    void testConfigureShoppingCartMappings_itemConverter() {
        Product product = new Product();
        product.setName("Test Product");

        ProductItem productItem = new ProductItem();
        productItem.setId(1L);
        productItem.setUnitPrice(BigDecimal.TEN);
        productItem.setProduct(product);

        ShoppingCartItem cartItem = new ShoppingCartItem();
        cartItem.setId(1L);
        cartItem.setQuantity(2);
        cartItem.setProductItem(productItem);

        CartItemResponse response = modelMapper.map(cartItem, CartItemResponse.class);

        assertEquals(1L, response.getId());
        assertEquals(2, response.getQuantity());
        assertEquals(BigDecimal.TEN, response.getUnitPrice());
        assertEquals("Test Product", response.getProductName());
        assertEquals(new BigDecimal("20"), response.getSubTotal());
    }

    @Test
    void testConfigureShoppingCartMappings_cartConverter() {
        ShoppingCart cart = new ShoppingCart();
        cart.setId(1L);

        ShoppingCartItem item1 = new ShoppingCartItem();
        item1.setQuantity(2);
        item1.setProductItem(new ProductItem());

        ShoppingCartItem item2 = new ShoppingCartItem();
        item2.setQuantity(3);
        item2.setProductItem(new ProductItem());

        cart.setCartItems(Set.of(item1, item2));

        CartResponse response = modelMapper.map(cart, CartResponse.class);

        assertEquals(1L, response.getId());
        assertEquals(2, response.getItems().size());
        assertEquals(5, response.getTotalItems());
    }

    @Test
    void testConfigureShoppingCartMappings_cartWithEmptyOrNullItems() {
        ShoppingCart cart = new ShoppingCart();
        cart.setId(1L);
        cart.setCartItems(null); 

        CartResponse response = modelMapper.map(cart, CartResponse.class);

        assertEquals(1L, response.getId());
        assertTrue(response.getItems().isEmpty());
        assertEquals(0, response.getTotalItems());
    }

    @Test
    void testConfigureShoppingCartMappings_itemWithNullProductItem() {
        ShoppingCartItem cartItem = new ShoppingCartItem();
        cartItem.setId(1L);
        cartItem.setQuantity(2);
        cartItem.setProductItem(null);

        CartItemResponse response = modelMapper.map(cartItem, CartItemResponse.class);

        assertEquals(1L, response.getId());
        assertEquals(2, response.getQuantity());
        assertNull(response.getProductName());
        assertEquals(BigDecimal.ZERO, response.getSubTotal());
    }
    
    @Test
    void testConfigureShoppingCartMappings_itemWithSubTotalZeroDueToMissingData() {
        ShoppingCartItem cartItem = new ShoppingCartItem();
        cartItem.setId(1L);
        cartItem.setQuantity(null);
        
        ProductItem productItem = new ProductItem();
        productItem.setUnitPrice(BigDecimal.TEN);
        cartItem.setProductItem(productItem);

        CartItemResponse response = modelMapper.map(cartItem, CartItemResponse.class);

        assertEquals(1L, response.getId());
        assertEquals(BigDecimal.ZERO, response.getSubTotal());
    }

    @Test
    void testConfigureAddressMappings_toDetailResponse() {
        Country country = new Country();
        country.setCountryName("Test Country");

        Address address = new Address();
        address.setId(1L);
        address.setTitle("Home");
        address.setAddressLine1("Line 1");
        address.setCountry(country);

        CustomerAddress customerAddress = new CustomerAddress();
        customerAddress.setAddress(address);
        customerAddress.setIsShippingAddress(true);
        customerAddress.setIsBillingAddress(false);

        AddressDetailResponse response = modelMapper.map(customerAddress, AddressDetailResponse.class);

        assertEquals("Home", response.getTitle());
        assertEquals("Line 1", response.getAddressLine1());
        assertEquals("Test Country", response.getCountryName());
        assertTrue(response.isDefaultShipping());
        assertFalse(response.isDefaultBilling());
    }

    @Test
    void testConfigureAddressMappings_toListResponse() {
        Country country = new Country();
        country.setCountryName("Test Country");

        Address address = new Address();
        address.setId(1L);
        address.setTitle("Work");
        address.setAddressLine1("Line 2");
        address.setCity("City");
        address.setCountry(country);

        CustomerAddress customerAddress = new CustomerAddress();
        customerAddress.setAddress(address);
        customerAddress.setIsShippingAddress(false);
        customerAddress.setIsBillingAddress(true);

        ListAddressResponse response = modelMapper.map(customerAddress, ListAddressResponse.class);

        assertEquals("Work", response.getTitle());
        assertEquals("Line 2", response.getAddressLine1());
        assertEquals("City", response.getCity());
        assertEquals("Test Country", response.getCountryName());
        assertFalse(response.isDefaultShipping());
        assertTrue(response.isDefaultBilling());
    }

    @Test
    void testConfigurePaymentMethodMappings_toDetailResponse() {
        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setId(1L);
        paymentMethod.setCardHolderName("John Doe");
        paymentMethod.setCardFamily("Visa");
        paymentMethod.setExpiryMonth(12);
        paymentMethod.setExpiryYear(2025);
        paymentMethod.setLastFourDigits("1234");

        PaymentMethodDetailResponse response = modelMapper.map(paymentMethod, PaymentMethodDetailResponse.class);

        assertEquals("John Doe", response.getCardHolderName());
        assertEquals("Visa", response.getCardFamily());
        assertEquals(paymentMethod.getExpiryMonth(), response.getExpiryMonth());
        assertEquals(paymentMethod.getExpiryYear(), response.getExpiryYear());
        assertEquals("**** **** **** 1234", response.getMaskedCardNumber());
    }

    @Test
    void testConfigurePaymentMethodMappings_toListResponse() {
        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setId(1L);
        paymentMethod.setCardHolderName("Jane Doe");
        paymentMethod.setCardFamily("Mastercard");
        paymentMethod.setLastFourDigits("5678");

        ListPaymentMethodResponse response = modelMapper.map(paymentMethod, ListPaymentMethodResponse.class);

        assertEquals("Jane Doe", response.getCardHolderName());
        assertEquals("Mastercard", response.getCardFamily());
        assertEquals("**** **** **** 5678", response.getMaskedCardNumber());
    }

    @Test
    void testConfigureOrderMappings_toAddOrderResponse() {
        Customer customer = new Customer();
        customer.setId(1L);

        OrderStatus status = new OrderStatus();
        status.setStatusName(OrderStatusEnum.PREPARING);

        Order order = new Order();
        order.setId(1L);
        order.setOrderDate(OffsetDateTime.now());
        order.setOrderTotal(BigDecimal.TEN);
        order.setCustomer(customer);
        order.setOrderStatus(status);

        AddOrderResponse response = modelMapper.map(order, AddOrderResponse.class);

        assertEquals(1L, response.getId());
        assertEquals(1L, response.getCustomerId());
        assertEquals(BigDecimal.TEN, response.getOrderTotal());
        assertEquals(OrderStatusEnum.PREPARING.name(), response.getOrderStatusName());
    }
    
    @Test
    void testConfigureOrderMappings_toAddOrderResponseWithNulls() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderTotal(BigDecimal.TEN);

        AddOrderResponse response = modelMapper.map(order, AddOrderResponse.class);

        assertEquals(1L, response.getId());
        assertNull(response.getCustomerId());
        assertNull(response.getOrderStatusName());
    }

    @Test
    void testConfigureOrderMappings_toOrderDetailResponse() {
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setContactName("Customer Name");

        ShippingMethod shippingMethod = new ShippingMethod();
        shippingMethod.setName("Express");

        OrderStatus status = new OrderStatus();
        status.setStatusName(OrderStatusEnum.SHIPPED);

        Address address = new Address();
        address.setId(1L);
        Country country = new Country();
        country.setCountryName("Country");
        address.setCountry(country);

        Order order = new Order();
        order.setId(1L);
        order.setCustomer(customer);
        order.setShippingMethod(shippingMethod);
        order.setOrderStatus(status);
        order.setShippingAddress(address);

        when(customerAddressRepository.findByAddressIdAndCustomerId(1L, 1L))
                .thenReturn(Optional.of(new CustomerAddress()));

        OrderDetailResponse response = modelMapper.map(order, OrderDetailResponse.class);

        assertEquals("Customer Name", response.getCustomerName());
        assertEquals("Express", response.getShippingMethodName());
        assertEquals(OrderStatusEnum.SHIPPED.name(), response.getOrderStatusName());
        assertNotNull(response.getShippingAddress());
    }

    @Test
    void testConfigureOrderMappings_withNullShippingAddressOrCustomer() {
        Order order = new Order();
        order.setId(1L);
        order.setCustomer(null);
        order.setShippingAddress(null);
        order.setOrderStatus(new OrderStatus()); 

        OrderDetailResponse response = modelMapper.map(order, OrderDetailResponse.class);

        assertNull(response.getShippingAddress());
        assertNull(response.getCustomerId());
    }

    @Test
    void testConfigureOrderMappings_toListUserOrdersResponse() {
        OrderStatus status = new OrderStatus();
        status.setStatusName(OrderStatusEnum.DELIVERED);

        Order order = new Order();
        order.setId(1L);
        order.setOrderDate(OffsetDateTime.now());
        order.setOrderTotal(BigDecimal.valueOf(100));
        order.setOrderStatus(status);

        ListUserOrdersResponse response = modelMapper.map(order, ListUserOrdersResponse.class);

        assertEquals(1L, response.getId());
        assertEquals(BigDecimal.valueOf(100), response.getOrderTotal());
        assertEquals(OrderStatusEnum.DELIVERED.name(), response.getOrderStatusName());
    }

    @Test
    void testConfigureOrderMappings_toUserOrderDetailResponse() {
        ShippingMethod shippingMethod = new ShippingMethod();
        shippingMethod.setName("Standard");

        OrderStatus status = new OrderStatus();
        status.setStatusName(OrderStatusEnum.PREPARING);

        Address address = new Address();
        address.setId(1L);
        Country country = new Country();
        country.setCountryName("Country");
        address.setCountry(country);

        Customer customer = new Customer();
        customer.setId(1L);

        Order order = new Order();
        order.setId(1L);
        order.setShippingMethod(shippingMethod);
        order.setOrderStatus(status);
        order.setShippingAddress(address);
        order.setCustomer(customer);
        order.setOrderItems(null);

        when(customerAddressRepository.findByAddressIdAndCustomerId(1L, 1L))
                .thenReturn(Optional.of(new CustomerAddress()));

        UserOrderDetailResponse response = modelMapper.map(order, UserOrderDetailResponse.class);

        assertEquals("Standard", response.getShippingMethodName());
        assertEquals(OrderStatusEnum.PREPARING.name(), response.getOrderStatusName());
        assertNotNull(response.getShippingAddress());
        assertTrue(response.getItems().isEmpty());
    }

    @Test
    void testConfigureOrderMappings_orderItemWithNullProductItem() {
        OrderItem orderItem = new OrderItem();
        orderItem.setQuantity(2);
        orderItem.setPriceAtOrder(BigDecimal.TEN);
        orderItem.setVatRate(BigDecimal.ONE);
        orderItem.setProductItem(null);

        OrderItemResponse response = modelMapper.map(orderItem, OrderItemResponse.class);

        assertEquals(2, response.getQuantity());
        assertNull(response.getProductName());
    }
    
    @Test
    void testConfigureOrderMappings_orderItemWithFullProductItem() {
        Product product = new Product();
        product.setId(10L);
        product.setName("Detailed Product");
        
        Colour colour = new Colour();
        colour.setColourName("Red");
        
        Size size = new Size();
        size.setSizeName("XL");
        
        ProductItem productItem = new ProductItem();
        productItem.setProduct(product);
        productItem.setColour(colour);
        productItem.setSize(size);
        
        OrderItem orderItem = new OrderItem();
        orderItem.setQuantity(1);
        orderItem.setProductItem(productItem);

        OrderItemResponse response = modelMapper.map(orderItem, OrderItemResponse.class);

        assertEquals(10L, response.getProductId());
        assertEquals("Detailed Product", response.getProductName());
        assertEquals("Red", response.getColourName());
        assertEquals("XL", response.getSizeName());
    }

    @Test
    void testConfigureInvoiceMappings_toListUserInvoicesResponse() {
        Order order = new Order();
        order.setId(1L);

        Invoice invoice = new Invoice();
        invoice.setId(10L);
        invoice.setOrder(order);

        ListUserInvoicesResponse response = modelMapper.map(invoice, ListUserInvoicesResponse.class);

        assertEquals(1L, response.getOrderId());
    }

    @Test
    void testConfigureInvoiceMappings_toUserInvoiceDetailResponse() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderDate(OffsetDateTime.now());
        
        OrderItem orderItem = new OrderItem();
        orderItem.setQuantity(1);
        order.setOrderItems(Set.of(orderItem));

        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setCardFamily("Visa");
        paymentMethod.setLastFourDigits("1111");

        Invoice invoice = new Invoice();
        invoice.setId(10L);
        invoice.setInvoiceNumber("INV-001");
        invoice.setGrandTotal(BigDecimal.TEN);
        invoice.setOrder(order);
        invoice.setPaymentMethod(paymentMethod);

        UserInvoiceDetailResponse response = modelMapper.map(invoice, UserInvoiceDetailResponse.class);

        assertEquals("INV-001", response.getInvoiceNumber());
        assertEquals(BigDecimal.TEN, response.getGrandTotal());
        assertEquals(1L, response.getOrderId());
        assertEquals("Visa", response.getCardFamily());
        assertEquals("**** **** **** 1111", response.getMaskedCardNumber());
        assertEquals(1, response.getOrderItems().size());
    }

    @Test
    void testConfigureInvoiceMappings_withNullOrderAndPaymentMethod() {
        Invoice invoice = new Invoice();
        invoice.setId(10L);
        invoice.setOrder(null);
        invoice.setPaymentMethod(null);

        UserInvoiceDetailResponse response = modelMapper.map(invoice, UserInvoiceDetailResponse.class);

        assertEquals(10L, response.getId());
        assertNull(response.getOrderId());
        assertNull(response.getCardFamily());
    }

    @Test
    void testConfigureReturnRequestMappings() {
        Order order = new Order();
        order.setId(1L);

        Product product = new Product();
        product.setName("Returned Product");

        ProductItem productItem = new ProductItem();
        productItem.setProduct(product);

        OrderItem orderItem = new OrderItem();
        orderItem.setProductItem(productItem);

        ReturnRequest returnRequest = new ReturnRequest();
        returnRequest.setId(1L);
        returnRequest.setOrder(order);
        returnRequest.setOrderItem(orderItem);

        ReturnRequestResponse response = modelMapper.map(returnRequest, ReturnRequestResponse.class);

        assertEquals(1L, response.getOrderId());
        assertEquals("Returned Product", response.getProductName());
    }

    @Test
    void testGetCategoryName_whenCategoryNotFound() {
        when(messageService.getMessage(Messages.Category.CATEGORY_NOT_FOUND)).thenReturn("Category Not Found");
        Product product = new Product();
        product.setProductCategories(new HashSet<>());

        ListProductsResponse response = modelMapper.map(product, ListProductsResponse.class);

        assertEquals("Category Not Found", response.getCategoryName());
    }
}