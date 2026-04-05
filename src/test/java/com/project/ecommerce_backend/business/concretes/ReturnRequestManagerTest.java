package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.business.abstracts.InvoiceService;
import com.project.ecommerce_backend.business.abstracts.OrderService;
import com.project.ecommerce_backend.business.abstracts.OrderStatusService;
import com.project.ecommerce_backend.business.abstracts.ProductService;
import com.project.ecommerce_backend.business.dtos.requests.return_request.AddReturnRequest;
import com.project.ecommerce_backend.business.dtos.requests.return_request.UpdateReturnStatusRequest;
import com.project.ecommerce_backend.business.dtos.responses.return_request.ReturnRequestResponse;
import com.project.ecommerce_backend.business.helpers.CacheHelper;
import com.project.ecommerce_backend.core.constants.Messages;
import com.project.ecommerce_backend.core.exceptions.types.BusinessException;
import com.project.ecommerce_backend.core.exceptions.types.NotFoundException;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.core.utils.mapper.ModelMapperService;
import com.project.ecommerce_backend.core.utils.result.DataResult;
import com.project.ecommerce_backend.entities.concretes.*;
import com.project.ecommerce_backend.entities.enums.OrderStatusEnum;
import com.project.ecommerce_backend.entities.enums.ReturnStatusEnum;
import com.project.ecommerce_backend.repositories.abstracts.ReturnRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReturnRequestManagerTest {

    @Mock private ReturnRequestRepository returnRequestRepository;
    @Mock private OrderService orderService;
    @Mock private ProductService productService;
    @Mock private OrderStatusService orderStatusService;
    @Mock private InvoiceService invoiceService;
    @Mock private ModelMapperService modelMapperService;
    @Mock private MessageService messageService;
    @Mock private CacheHelper cacheHelper;
    @Mock private ModelMapper modelMapper;

    private ReturnRequestManager returnRequestManager;

    private final Long userId = 1L;
    private final Long returnId = 10L;
    private ReturnRequest returnRequest;
    private OrderItem orderItem;
    private Order order;

    @BeforeEach
    void setUp() {
        returnRequestManager = new ReturnRequestManager(
                returnRequestRepository,
                orderService,
                productService,
                orderStatusService,
                invoiceService,
                modelMapperService,
                messageService,
                cacheHelper,
                14
        );

        order = new Order();
        order.setId(1L);
        order.setUDate(OffsetDateTime.now());
        OrderStatus status = new OrderStatus();
        status.setStatusName(OrderStatusEnum.DELIVERED);
        order.setOrderStatus(status);

        orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setOrder(order);
        ProductItem productItem = new ProductItem();
        productItem.setId(1L);
        orderItem.setProductItem(productItem);
        orderItem.setQuantity(1);

        returnRequest = new ReturnRequest();
        returnRequest.setId(returnId);
        returnRequest.setOrder(order);
        returnRequest.setOrderItem(orderItem);
        returnRequest.setStatus(ReturnStatusEnum.PENDING_APPROVAL);
    }

    private void mockSecurityContext(String role) {
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);

        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);

        lenient().doReturn(Collections.singletonList(new SimpleGrantedAuthority(role)))
                .when(authentication).getAuthorities();

        SecurityContextHolder.setContext(securityContext);
    }

    // --- GET ALL RETURNS TESTS ---

    @Test
    void getAllReturns_ShouldReturnList() {
        Pageable pageable = Pageable.unpaged();
        Page<ReturnRequest> page = new PageImpl<>(Collections.singletonList(returnRequest));

        when(returnRequestRepository.findAll(pageable)).thenReturn(page);
        when(modelMapperService.getMapper()).thenReturn(modelMapper);
        when(modelMapper.map(returnRequest, ReturnRequestResponse.class)).thenReturn(new ReturnRequestResponse());
        when(messageService.getMessage(Messages.ReturnRequest.RETURN_REQUESTS_SUCCESSFULLY_LISTED))
                .thenReturn("Returns listed");

        DataResult<?> result = returnRequestManager.getAllReturns(pageable);

        assertTrue(result.isSuccess());
        assertEquals("Returns listed", result.getMessage());
    }

    // --- GET RETURN BY ID FOR ADMIN TESTS ---

    @Test
    void getReturnByIdForAdmin_ShouldReturnDetail_WhenAdmin() {
        mockSecurityContext("ROLE_ADMIN");

        when(returnRequestRepository.findById(returnId)).thenReturn(Optional.of(returnRequest));
        when(modelMapperService.getMapper()).thenReturn(modelMapper);
        when(modelMapper.map(returnRequest, ReturnRequestResponse.class)).thenReturn(new ReturnRequestResponse());
        when(messageService.getMessage(Messages.ReturnRequest.RETURN_REQUEST_DETAIL_SUCCESSFULLY_LISTED))
                .thenReturn("Detail listed");

        DataResult<ReturnRequestResponse> result = returnRequestManager.getReturnByIdForAdmin(returnId);

        assertTrue(result.isSuccess());
        assertEquals("Detail listed", result.getMessage());
    }

    @Test
    void getReturnByIdForAdmin_ShouldReturnDetail_WhenSellerAuthorized() {
        mockSecurityContext("ROLE_SELLER");
        when(cacheHelper.getAuthenticatedUserId()).thenReturn(userId);

        when(returnRequestRepository.existsByIdAndOrderItemProductItemProductSupplierId(returnId, userId)).thenReturn(true);
        when(returnRequestRepository.findById(returnId)).thenReturn(Optional.of(returnRequest));
        when(modelMapperService.getMapper()).thenReturn(modelMapper);
        when(modelMapper.map(returnRequest, ReturnRequestResponse.class)).thenReturn(new ReturnRequestResponse());

        DataResult<ReturnRequestResponse> result = returnRequestManager.getReturnByIdForAdmin(returnId);

        assertTrue(result.isSuccess());
    }

    @Test
    void getReturnByIdForAdmin_ShouldThrowNotFoundException_WhenSellerUnauthorized() {
        mockSecurityContext("ROLE_SELLER");
        when(cacheHelper.getAuthenticatedUserId()).thenReturn(userId);

        when(returnRequestRepository.existsByIdAndOrderItemProductItemProductSupplierId(returnId, userId)).thenReturn(false);
        when(messageService.getMessage(Messages.ReturnRequest.RETURN_REQUEST_NOT_AUTHORIZED))
                .thenReturn("Not authorized");

        NotFoundException exception = assertThrows(NotFoundException.class, 
                () -> returnRequestManager.getReturnByIdForAdmin(returnId));
        assertEquals("Not authorized", exception.getMessage());
    }

    @Test
    void getReturnByIdForAdmin_ShouldThrowNotFoundException_WhenReturnNotFound() {
        mockSecurityContext("ROLE_ADMIN");

        when(returnRequestRepository.findById(returnId)).thenReturn(Optional.empty());
        when(messageService.getMessageWithParams(Messages.ReturnRequest.RETURN_REQUEST_NOT_FOUND, returnId))
                .thenReturn("Return not found");

        NotFoundException exception = assertThrows(NotFoundException.class, 
                () -> returnRequestManager.getReturnByIdForAdmin(returnId));
        assertEquals("Return not found", exception.getMessage());
    }

    // --- ADD RETURN REQUEST TESTS ---

    @Test
    void add_ShouldCreateRequest_WhenValid() {
        mockSecurityContext("ROLE_USER");
        when(cacheHelper.getAuthenticatedUserId()).thenReturn(userId);

        AddReturnRequest request = new AddReturnRequest();
        request.setOrderId(1L);
        request.setOrderItemId(1L);

        when(orderService.findOrderItemByIdAndOrderIdAndCustomerId(1L, 1L, userId)).thenReturn(orderItem);
        when(returnRequestRepository.existsByOrderIdAndOrderItemId(1L, 1L)).thenReturn(false);
        when(returnRequestRepository.save(any(ReturnRequest.class))).thenReturn(returnRequest);
        when(modelMapperService.getMapper()).thenReturn(modelMapper);
        when(modelMapper.map(returnRequest, ReturnRequestResponse.class)).thenReturn(new ReturnRequestResponse());
        when(messageService.getMessage(Messages.ReturnRequest.RETURN_REQUEST_SUCCESSFULLY_CREATED))
                .thenReturn("Return created");

        DataResult<ReturnRequestResponse> result = returnRequestManager.add(request);

        assertTrue(result.isSuccess());
        assertEquals("Return created", result.getMessage());
    }

    @Test
    void add_ShouldThrowBusinessException_WhenReturnAlreadyExists() {
        mockSecurityContext("ROLE_USER");
        when(cacheHelper.getAuthenticatedUserId()).thenReturn(userId);

        AddReturnRequest request = new AddReturnRequest();
        request.setOrderId(1L);
        request.setOrderItemId(1L);

        when(orderService.findOrderItemByIdAndOrderIdAndCustomerId(1L, 1L, userId)).thenReturn(orderItem);
        when(returnRequestRepository.existsByOrderIdAndOrderItemId(1L, 1L)).thenReturn(true);
        when(messageService.getMessage(Messages.ReturnRequest.RETURN_REQUEST_ALREADY_EXISTS))
                .thenReturn("Return already exists");

        BusinessException exception = assertThrows(BusinessException.class, () -> returnRequestManager.add(request));
        assertEquals("Return already exists", exception.getMessage());
    }

    @Test
    void add_ShouldThrowBusinessException_WhenOrderNotDelivered() {
        mockSecurityContext("ROLE_USER");
        when(cacheHelper.getAuthenticatedUserId()).thenReturn(userId);

        AddReturnRequest request = new AddReturnRequest();
        order.getOrderStatus().setStatusName(OrderStatusEnum.SHIPPED);

        when(orderService.findOrderItemByIdAndOrderIdAndCustomerId(any(), any(), any())).thenReturn(orderItem);
        when(returnRequestRepository.existsByOrderIdAndOrderItemId(any(), any())).thenReturn(false);
        when(messageService.getMessage(Messages.ReturnRequest.RETURN_NOT_POSSIBLE_FOR_ORDER_STATUS))
                .thenReturn("Return not possible");

        BusinessException exception = assertThrows(BusinessException.class, () -> returnRequestManager.add(request));
        assertEquals("Return not possible", exception.getMessage());
    }

    @Test
    void add_ShouldThrowBusinessException_WhenPeriodExpired() {
        mockSecurityContext("ROLE_USER");
        when(cacheHelper.getAuthenticatedUserId()).thenReturn(userId);

        AddReturnRequest request = new AddReturnRequest();
        order.setUDate(OffsetDateTime.now().minusDays(20)); // Expired (limit is 14)

        when(orderService.findOrderItemByIdAndOrderIdAndCustomerId(any(), any(), any())).thenReturn(orderItem);
        when(returnRequestRepository.existsByOrderIdAndOrderItemId(any(), any())).thenReturn(false);
        when(messageService.getMessageWithParams(eq(Messages.ReturnRequest.RETURN_PERIOD_EXPIRED), any()))
                .thenReturn("Period expired");

        BusinessException exception = assertThrows(BusinessException.class, () -> returnRequestManager.add(request));
        assertEquals("Period expired", exception.getMessage());
    }

    @Test
    void add_ShouldThrowBusinessException_WhenPrincipalInvalid() {
        when(cacheHelper.getAuthenticatedUserId()).thenThrow(new BusinessException("Invalid principal"));

        AddReturnRequest request = new AddReturnRequest();
        
        BusinessException exception = assertThrows(BusinessException.class, () -> returnRequestManager.add(request));
        assertEquals("Invalid principal", exception.getMessage());
    }

    // --- UPDATE RETURN STATUS TESTS ---

    @Test
    void updateReturnStatus_ShouldUpdateAndTriggerSideEffects_WhenCompleted() {
        mockSecurityContext("ROLE_ADMIN");
        UpdateReturnStatusRequest request = new UpdateReturnStatusRequest();
        request.setNewStatus(ReturnStatusEnum.COMPLETED);

        Invoice invoice = new Invoice();
        invoice.setId(1L);
        order.setInvoice(invoice);

        when(returnRequestRepository.findById(returnId)).thenReturn(Optional.of(returnRequest));
        when(returnRequestRepository.save(returnRequest)).thenReturn(returnRequest);
        when(modelMapperService.getMapper()).thenReturn(modelMapper);
        when(modelMapper.map(returnRequest, ReturnRequestResponse.class)).thenReturn(new ReturnRequestResponse());
        when(messageService.getMessage(Messages.ReturnRequest.RETURN_REQUEST_STATUS_SUCCESSFULLY_UPDATED))
                .thenReturn("Status updated");

        DataResult<ReturnRequestResponse> result = returnRequestManager.updateReturnStatus(returnId, request);

        assertTrue(result.isSuccess());
        verify(productService).increaseStock(1L, 1);
        verify(invoiceService).markInvoiceAsRefunded(1L);
        verify(orderService).updateOrderStatus(eq(1L), any());
    }

    @Test
    void updateReturnStatus_ShouldUpdateWithoutSideEffects_WhenRejected() {
        mockSecurityContext("ROLE_SELLER");
        when(cacheHelper.getAuthenticatedUserId()).thenReturn(userId);
        when(returnRequestRepository.existsByIdAndOrderItemProductItemProductSupplierId(returnId, userId)).thenReturn(true);

        UpdateReturnStatusRequest request = new UpdateReturnStatusRequest();
        request.setNewStatus(ReturnStatusEnum.REJECTED);

        when(returnRequestRepository.findById(returnId)).thenReturn(Optional.of(returnRequest));
        when(returnRequestRepository.save(returnRequest)).thenReturn(returnRequest);
        when(modelMapperService.getMapper()).thenReturn(modelMapper);
        when(modelMapper.map(returnRequest, ReturnRequestResponse.class)).thenReturn(new ReturnRequestResponse());

        DataResult<ReturnRequestResponse> result = returnRequestManager.updateReturnStatus(returnId, request);

        assertTrue(result.isSuccess());
        verify(productService, never()).increaseStock(any(), anyInt());
        verify(invoiceService, never()).markInvoiceAsRefunded(any());
    }

    @Test
    void updateReturnStatus_ShouldThrowBusinessException_WhenAlreadyFinalized() {
        mockSecurityContext("ROLE_ADMIN");
        UpdateReturnStatusRequest request = new UpdateReturnStatusRequest();
        returnRequest.setStatus(ReturnStatusEnum.COMPLETED); // Already finalized

        when(returnRequestRepository.findById(returnId)).thenReturn(Optional.of(returnRequest));
        when(messageService.getMessage(Messages.ReturnRequest.RETURN_REQUEST_ALREADY_FINALIZED))
                .thenReturn("Already finalized");

        BusinessException exception = assertThrows(BusinessException.class, 
                () -> returnRequestManager.updateReturnStatus(returnId, request));
        assertEquals("Already finalized", exception.getMessage());
    }

    @Test
    void updateReturnStatus_ShouldThrowNotFoundException_WhenUnauthorized() {
        mockSecurityContext("ROLE_SELLER");
        when(cacheHelper.getAuthenticatedUserId()).thenReturn(userId);

        UpdateReturnStatusRequest request = new UpdateReturnStatusRequest();

        when(returnRequestRepository.existsByIdAndOrderItemProductItemProductSupplierId(returnId, userId)).thenReturn(false);
        when(messageService.getMessage(Messages.ReturnRequest.RETURN_REQUEST_NOT_AUTHORIZED))
                .thenReturn("Not authorized");

        NotFoundException exception = assertThrows(NotFoundException.class, 
                () -> returnRequestManager.updateReturnStatus(returnId, request));
        assertEquals("Not authorized", exception.getMessage());
    }
}