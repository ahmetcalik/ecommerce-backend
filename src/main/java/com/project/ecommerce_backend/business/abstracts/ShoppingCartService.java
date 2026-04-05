package com.project.ecommerce_backend.business.abstracts;

import com.project.ecommerce_backend.business.dtos.requests.cart.AddCartItemRequest;
import com.project.ecommerce_backend.business.dtos.requests.cart.UpdateCartItemRequest;
import com.project.ecommerce_backend.business.dtos.responses.cart.CartAndSessionResponse;
import com.project.ecommerce_backend.core.utils.result.Result;
import com.project.ecommerce_backend.entities.concretes.ShoppingCart;

import java.util.UUID;

public interface ShoppingCartService {

    CartAndSessionResponse getCartDetails(UUID sessionId);

    CartAndSessionResponse addItemToCart(UUID sessionId, AddCartItemRequest request);

    CartAndSessionResponse updateItemQuantity(UUID sessionId, Long itemId, UpdateCartItemRequest request);

    CartAndSessionResponse removeItemFromCart(UUID sessionId, Long itemId);

    void mergeCarts(UUID sessionId, Long customerId);

    ShoppingCart getActiveCartByCustomerId(Long customerId);

    void clearCartById(Long cartId);

    Result clearCart(UUID sessionId);

}