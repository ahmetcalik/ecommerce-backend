package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.business.abstracts.BinLookupService;
import com.project.ecommerce_backend.business.abstracts.CustomerService;
import com.project.ecommerce_backend.business.abstracts.PaymentMethodService;
import com.project.ecommerce_backend.business.abstracts.PaymentTypeService;
import com.project.ecommerce_backend.business.abstracts.SubscriptionService;
import com.project.ecommerce_backend.business.dtos.requests.payment.AddPaymentMethodRequest;
import com.project.ecommerce_backend.business.dtos.requests.payment.UpdatePaymentMethodRequest;
import com.project.ecommerce_backend.business.dtos.responses.payment.ListPaymentMethodResponse;
import com.project.ecommerce_backend.business.dtos.responses.payment.PaymentMethodDetailResponse;
import com.project.ecommerce_backend.business.helpers.CacheHelper;
import com.project.ecommerce_backend.core.exceptions.types.BusinessException;
import com.project.ecommerce_backend.core.exceptions.types.NotFoundException;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.core.utils.mapper.ModelMapperService;
import com.project.ecommerce_backend.entities.concretes.Customer;
import com.project.ecommerce_backend.entities.concretes.PaymentMethod;
import com.project.ecommerce_backend.entities.concretes.PaymentType;
import com.project.ecommerce_backend.repositories.abstracts.PaymentMethodRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentMethodManagerTest {

    @Mock private PaymentMethodRepository paymentMethodRepository;
    @Mock private CustomerService customerService;
    @Mock private BinLookupService binLookupService;
    @Mock private SubscriptionService subscriptionService;
    @Mock private PaymentTypeService paymentTypeService;
    @Mock private ModelMapperService modelMapperService;
    @Mock private MessageService messageService;
    @Mock private PaymentMethodService self;
    @Mock private CacheHelper cacheHelper;
    @Mock private ModelMapper modelMapper;

    @InjectMocks
    private PaymentMethodManager paymentMethodManager;

    private Long customerId = 1L;
    private Long paymentMethodId = 10L;

    // 1. getAll TESTS
    @Test
    void getAll_shouldReturnList() {
        PaymentMethod method = new PaymentMethod();
        List<PaymentMethod> methods = Collections.singletonList(method);

        when(cacheHelper.getAuthenticatedUserId()).thenReturn(customerId);
        when(paymentMethodRepository.findAllByCustomerId(customerId)).thenReturn(methods);
        when(modelMapperService.getMapper()).thenReturn(modelMapper);
        when(modelMapper.map(method, ListPaymentMethodResponse.class)).thenReturn(new ListPaymentMethodResponse());

        var result = paymentMethodManager.getAll();

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
    }

    // 2. getById TESTS
    @Test
    void getById_whenAuthorized_shouldReturnDetail() {
        PaymentMethod method = new PaymentMethod();
        when(cacheHelper.getAuthenticatedUserId()).thenReturn(customerId);
        when(self.getByIdAndCustomerId(paymentMethodId, customerId)).thenReturn(method);
        when(modelMapperService.getMapper()).thenReturn(modelMapper);
        when(modelMapper.map(method, PaymentMethodDetailResponse.class)).thenReturn(new PaymentMethodDetailResponse());

        var result = paymentMethodManager.getById(paymentMethodId);

        assertTrue(result.isSuccess());
    }

    // 3. add TESTS
    @Test
    void add_whenValid_shouldAddMethod() {
        AddPaymentMethodRequest request = new AddPaymentMethodRequest();
        request.setCardNumber("1234567890123456");
        request.setExpiryMonth(12);
        request.setExpiryYear(2025);

        PaymentMethod savedMethod = new PaymentMethod();
        savedMethod.setId(paymentMethodId);

        when(cacheHelper.getAuthenticatedUserId()).thenReturn(customerId);
        when(customerService.getByIdAsEntity(customerId)).thenReturn(new Customer());
        when(paymentTypeService.getByName(any())).thenReturn(new PaymentType());
        when(paymentMethodRepository.existsByCustomerIdAndLastFourDigitsAndExpiryMonthAndExpiryYear(any(), any(), any(), any())).thenReturn(false);
        when(binLookupService.getCardFamilyByBinNumber(any())).thenReturn("Visa");
        when(paymentMethodRepository.save(any(PaymentMethod.class))).thenReturn(savedMethod);
        when(modelMapperService.getMapper()).thenReturn(modelMapper);
        when(modelMapper.map(savedMethod, PaymentMethodDetailResponse.class)).thenReturn(new PaymentMethodDetailResponse());

        var result = paymentMethodManager.add(request);

        assertTrue(result.isSuccess());
        verify(paymentMethodRepository).save(any(PaymentMethod.class));
    }

    @Test
    void add_whenDuplicate_shouldThrowBusinessException() {
        AddPaymentMethodRequest request = new AddPaymentMethodRequest();
        request.setCardNumber("1234567890123456");

        when(cacheHelper.getAuthenticatedUserId()).thenReturn(customerId);
        when(customerService.getByIdAsEntity(customerId)).thenReturn(new Customer());
        when(paymentTypeService.getByName(any())).thenReturn(new PaymentType());
        when(paymentMethodRepository.existsByCustomerIdAndLastFourDigitsAndExpiryMonthAndExpiryYear(any(), any(), any(), any())).thenReturn(true);
        when(messageService.getMessage(any())).thenReturn("Error");

        assertThrows(BusinessException.class, () -> paymentMethodManager.add(request));
    }

    // 4. update TESTS
    @Test
    void update_whenValid_shouldUpdateMethod() {
        UpdatePaymentMethodRequest request = new UpdatePaymentMethodRequest();
        PaymentMethod method = new PaymentMethod();

        when(cacheHelper.getAuthenticatedUserId()).thenReturn(customerId);
        when(self.getByIdAndCustomerId(paymentMethodId, customerId)).thenReturn(method);
        when(modelMapperService.getMapper()).thenReturn(modelMapper);
        
        // FIX: Add stubbing for void map method
        doNothing().when(modelMapper).map(eq(request), eq(method));
        
        when(paymentMethodRepository.save(method)).thenReturn(method);
        when(modelMapper.map(method, PaymentMethodDetailResponse.class)).thenReturn(new PaymentMethodDetailResponse());

        var result = paymentMethodManager.update(paymentMethodId, request);

        assertTrue(result.isSuccess());
        verify(paymentMethodRepository).save(method);
    }

    // 5. delete TESTS
    @Test
    void delete_whenSafe_shouldDeleteMethod() {
        PaymentMethod method = new PaymentMethod();

        when(subscriptionService.isPaymentMethodInUseByActiveSubscription(paymentMethodId)).thenReturn(false);
        when(cacheHelper.getAuthenticatedUserId()).thenReturn(customerId);
        when(self.getByIdAndCustomerId(paymentMethodId, customerId)).thenReturn(method);

        var result = paymentMethodManager.delete(paymentMethodId);

        assertTrue(result.isSuccess());
        verify(paymentMethodRepository).delete(method);
    }

    @Test
    void delete_whenInUse_shouldThrowBusinessException() {
        when(subscriptionService.isPaymentMethodInUseByActiveSubscription(paymentMethodId)).thenReturn(true);
        when(messageService.getMessage(any())).thenReturn("Error");

        assertThrows(BusinessException.class, () -> paymentMethodManager.delete(paymentMethodId));
    }

    // 6. getByIdAndCustomerId TESTS
    @Test
    void getByIdAndCustomerId_whenExists_shouldReturnMethod() {
        PaymentMethod method = new PaymentMethod();
        when(paymentMethodRepository.findByIdAndCustomerId(paymentMethodId, customerId)).thenReturn(Optional.of(method));

        var result = paymentMethodManager.getByIdAndCustomerId(paymentMethodId, customerId);

        assertNotNull(result);
    }

    @Test
    void getByIdAndCustomerId_whenMissing_shouldThrowNotFoundException() {
        when(paymentMethodRepository.findByIdAndCustomerId(paymentMethodId, customerId)).thenReturn(Optional.empty());
        when(messageService.getMessage(any())).thenReturn("Error");

        assertThrows(NotFoundException.class, () -> paymentMethodManager.getByIdAndCustomerId(paymentMethodId, customerId));
    }
}
