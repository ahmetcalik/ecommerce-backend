package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.business.abstracts.CustomerService;
import com.project.ecommerce_backend.business.abstracts.ProductService;
import com.project.ecommerce_backend.business.dtos.requests.cart.AddCartItemRequest;
import com.project.ecommerce_backend.business.dtos.requests.cart.UpdateCartItemRequest;
import com.project.ecommerce_backend.business.dtos.responses.cart.CartAndSessionResponse;
import com.project.ecommerce_backend.business.dtos.responses.cart.CartResponse;
import com.project.ecommerce_backend.core.exceptions.types.NotFoundException;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.core.security.details.CustomerDetails;
import com.project.ecommerce_backend.core.utils.mapper.ModelMapperService;
import com.project.ecommerce_backend.entities.concretes.*;
import com.project.ecommerce_backend.repositories.abstracts.ShoppingCartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShoppingCartManagerTest {

    @Mock
    private ShoppingCartRepository shoppingCartRepository;
    @Mock
    private CustomerService customerService;
    @Mock
    private ProductService productService;
    @Mock
    private MessageService messageService;
    @Mock
    private ModelMapperService modelMapperService;
    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private ShoppingCartManager shoppingCartManager;

    private UUID sessionId;
    private Long customerId;
    private Customer customer;
    private ShoppingCart cart;
    private ProductItem productItem;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
        customerId = 1L;
        customer = new Customer();
        customer.setId(customerId);

        cart = new ShoppingCart();
        cart.setId(1L);
        cart.setSessionId(sessionId);
        cart.setCustomer(customer);
        cart.setCartItems(new HashSet<>());

        productItem = new ProductItem();
        productItem.setId(10L);
        productItem.setUnitPrice(BigDecimal.TEN);
        Product product = new Product();
        product.setVatRate(BigDecimal.valueOf(0.18));
        productItem.setProduct(product);

        // Mock Security Context (Default: Anonymous)
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        
        // Use lenient() for all stubbings in setUp because they might be overridden or unused
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.isAuthenticated()).thenReturn(false);
        lenient().when(authentication.getPrincipal()).thenReturn("anonymousUser");

        SecurityContextHolder.setContext(securityContext);
    }

    private void mockAuthenticatedUser() {
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        CustomerDetails customerDetails = mock(CustomerDetails.class);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(customerDetails);
        when(customerDetails.getId()).thenReturn(customerId);
        SecurityContextHolder.setContext(securityContext);
    }

    // 1. getCartDetails TESTS
    @Test
    void getCartDetails_whenGuestUser_shouldReturnGuestCart() {
        when(shoppingCartRepository.findFirstBySessionId(sessionId)).thenReturn(Optional.of(cart));
        when(modelMapperService.getMapper()).thenReturn(modelMapper);
        when(modelMapper.map(cart, CartResponse.class)).thenReturn(new CartResponse());

        CartAndSessionResponse response = shoppingCartManager.getCartDetails(sessionId);

        assertNotNull(response);
        assertEquals(sessionId, response.getSessionId());
    }

    @Test
    void getCartDetails_whenAuthenticatedUser_shouldReturnUserCart() {
        mockAuthenticatedUser();
        when(shoppingCartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(cart));
        when(modelMapperService.getMapper()).thenReturn(modelMapper);
        when(modelMapper.map(cart, CartResponse.class)).thenReturn(new CartResponse());

        CartAndSessionResponse response = shoppingCartManager.getCartDetails(sessionId);

        assertNotNull(response);
        verify(shoppingCartRepository).findByCustomerId(customerId);
    }

    // 2. addItemToCart TESTS
    @Test
    void addItemToCart_whenNewItem_shouldAddNewItem() {
        AddCartItemRequest request = new AddCartItemRequest();
        request.setProductItemId(10L);
        request.setQuantity(2);

        when(shoppingCartRepository.findFirstBySessionId(sessionId)).thenReturn(Optional.of(cart));
        when(productService.getProductItemById(10L)).thenReturn(productItem);
        when(shoppingCartRepository.save(cart)).thenReturn(cart);
        when(modelMapperService.getMapper()).thenReturn(modelMapper);
        when(modelMapper.map(cart, CartResponse.class)).thenReturn(new CartResponse());

        var response = shoppingCartManager.addItemToCart(sessionId, request);

        assertNotNull(response);
        assertEquals(1, cart.getCartItems().size());
        assertEquals(2, cart.getCartItems().iterator().next().getQuantity());
    }

    @Test
    void addItemToCart_whenExistingItem_shouldIncreaseQuantity() {
        AddCartItemRequest request = new AddCartItemRequest();
        request.setProductItemId(10L);
        request.setQuantity(2);

        ShoppingCartItem existingItem = new ShoppingCartItem();
        existingItem.setProductItem(productItem);
        existingItem.setQuantity(1);
        cart.getCartItems().add(existingItem);

        when(shoppingCartRepository.findFirstBySessionId(sessionId)).thenReturn(Optional.of(cart));
        when(productService.getProductItemById(10L)).thenReturn(productItem);
        when(shoppingCartRepository.save(cart)).thenReturn(cart);
        when(modelMapperService.getMapper()).thenReturn(modelMapper);
        when(modelMapper.map(cart, CartResponse.class)).thenReturn(new CartResponse());

        var response = shoppingCartManager.addItemToCart(sessionId, request);

        assertNotNull(response);
        assertEquals(1, cart.getCartItems().size());
        assertEquals(3, cart.getCartItems().iterator().next().getQuantity());
    }

    // 3. updateItemQuantity TESTS
    @Test
    void updateItemQuantity_whenItemExists_shouldUpdateQuantity() {
        Long itemId = 5L;
        UpdateCartItemRequest request = new UpdateCartItemRequest();
        request.setQuantity(5);

        ShoppingCartItem item = new ShoppingCartItem();
        item.setId(itemId);
        item.setProductItem(productItem);
        item.setQuantity(1);
        cart.getCartItems().add(item);

        when(shoppingCartRepository.findFirstBySessionId(sessionId)).thenReturn(Optional.of(cart));
        when(shoppingCartRepository.save(cart)).thenReturn(cart);
        when(modelMapperService.getMapper()).thenReturn(modelMapper);
        when(modelMapper.map(cart, CartResponse.class)).thenReturn(new CartResponse());

        var response = shoppingCartManager.updateItemQuantity(sessionId, itemId, request);

        assertNotNull(response);
        assertEquals(5, item.getQuantity());
    }

    @Test
    void updateItemQuantity_whenItemMissing_shouldThrowNotFoundException() {
        Long itemId = 5L;
        UpdateCartItemRequest request = new UpdateCartItemRequest();
        request.setQuantity(5);

        when(shoppingCartRepository.findFirstBySessionId(sessionId)).thenReturn(Optional.of(cart));
        when(messageService.getMessage(any())).thenReturn("Error");

        assertThrows(NotFoundException.class, () -> shoppingCartManager.updateItemQuantity(sessionId, itemId, request));
    }

    // 4. removeItemFromCart TESTS
    @Test
    void removeItemFromCart_whenItemExists_shouldRemoveItem() {
        Long itemId = 5L;
        ShoppingCartItem item = new ShoppingCartItem();
        item.setId(itemId);
        item.setProductItem(productItem);
        cart.getCartItems().add(item);

        when(shoppingCartRepository.findFirstBySessionId(sessionId)).thenReturn(Optional.of(cart));
        when(shoppingCartRepository.save(cart)).thenReturn(cart);
        when(modelMapperService.getMapper()).thenReturn(modelMapper);
        when(modelMapper.map(cart, CartResponse.class)).thenReturn(new CartResponse());

        var response = shoppingCartManager.removeItemFromCart(sessionId, itemId);

        assertNotNull(response);
        assertTrue(cart.getCartItems().isEmpty());
    }

    // 5. mergeCarts TESTS
    @Test
    void mergeCarts_shouldTransferItems() {
        ShoppingCart guestCart = new ShoppingCart();
        guestCart.setId(2L);
        ShoppingCartItem guestItem = new ShoppingCartItem();
        guestItem.setProductItem(productItem);
        guestItem.setQuantity(2);
        guestCart.setCartItems(Set.of(guestItem));

        when(shoppingCartRepository.findFirstBySessionId(sessionId)).thenReturn(Optional.of(guestCart));
        when(shoppingCartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(cart));

        shoppingCartManager.mergeCarts(sessionId, customerId);

        verify(shoppingCartRepository).save(cart);
        verify(shoppingCartRepository).delete(guestCart);
        assertEquals(1, cart.getCartItems().size());
    }

    // 6. clearCart TESTS
    @Test
    void clearCart_shouldRemoveAllItems() {
        ShoppingCartItem item = new ShoppingCartItem();
        cart.getCartItems().add(item);

        when(shoppingCartRepository.findFirstBySessionId(sessionId)).thenReturn(Optional.of(cart));
        when(messageService.getMessage(any())).thenReturn("Success");

        var result = shoppingCartManager.clearCart(sessionId);

        assertTrue(result.isSuccess());
        assertTrue(cart.getCartItems().isEmpty());
        verify(shoppingCartRepository).save(cart);
    }
}
