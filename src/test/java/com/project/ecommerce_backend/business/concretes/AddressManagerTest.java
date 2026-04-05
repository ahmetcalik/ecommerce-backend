package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.business.abstracts.*;
import com.project.ecommerce_backend.business.dtos.requests.address.AddressRequest;
import com.project.ecommerce_backend.business.dtos.responses.address.AddressDetailResponse;
import com.project.ecommerce_backend.business.dtos.responses.address.ListAddressResponse;
import com.project.ecommerce_backend.business.helpers.CacheHelper;
import com.project.ecommerce_backend.core.exceptions.types.BusinessException;
import com.project.ecommerce_backend.core.exceptions.types.NotFoundException;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.core.utils.mapper.ModelMapperService;
import com.project.ecommerce_backend.entities.concretes.*;
import com.project.ecommerce_backend.repositories.abstracts.AddressRepository;
import com.project.ecommerce_backend.repositories.abstracts.CustomerAddressRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressManagerTest {

    @Mock
    private AddressRepository addressRepository;
    @Mock
    private CustomerAddressRepository customerAddressRepository;
    @Mock
    private CustomerService customerService;
    @Mock
    private CountryService countryService;
    @Mock
    private OrderService orderService;
    @Mock
    private ModelMapperService modelMapperService;
    @Mock
    private MessageService messageService;
    @Mock
    private CacheHelper cacheHelper;

    @InjectMocks
    private AddressManager addressManager;

    @Mock
    private ModelMapper modelMapper;

    private final Long CUSTOMER_ID = 1L;

    // 1. getById TESTS
    @Test
    void getById_whenAddressBelongsToUser_shouldReturnAddressDetail() {
        Long addressId = 10L;
        CustomerAddress link = new CustomerAddress();
        AddressDetailResponse expectedResponse = new AddressDetailResponse();
        expectedResponse.setId(addressId);

        when(cacheHelper.getAuthenticatedUserId()).thenReturn(CUSTOMER_ID);
        when(modelMapperService.getMapper()).thenReturn(modelMapper);
        when(customerAddressRepository.findByAddressIdAndCustomerId(addressId, CUSTOMER_ID)).thenReturn(Optional.of(link));
        when(modelMapper.map(link, AddressDetailResponse.class)).thenReturn(expectedResponse);

        var result = addressManager.getById(addressId);

        assertTrue(result.isSuccess());
        assertEquals(addressId, result.getData().getId());
        verify(customerAddressRepository).findByAddressIdAndCustomerId(addressId, CUSTOMER_ID);
    }

    @Test
    void getById_whenAddressDoesNotBelongToUser_shouldThrowNotFoundException() {
        Long addressId = 11L;
        when(cacheHelper.getAuthenticatedUserId()).thenReturn(CUSTOMER_ID);
        when(customerAddressRepository.findByAddressIdAndCustomerId(addressId, CUSTOMER_ID)).thenReturn(Optional.empty());
        when(messageService.getMessageWithParams(any(), any())).thenReturn("Error");

        assertThrows(NotFoundException.class, () -> addressManager.getById(addressId));
    }

    // 2. getAll TESTS
    @Test
    void getAll_whenUserHasAddresses_shouldReturnAddressList() {
        Pageable pageable = Pageable.unpaged();
        ListAddressResponse addressResponse = new ListAddressResponse();
        Slice<ListAddressResponse> addressSlice = new PageImpl<>(Collections.singletonList(addressResponse));

        when(cacheHelper.getAuthenticatedUserId()).thenReturn(CUSTOMER_ID);
        when(modelMapperService.getMapper()).thenReturn(modelMapper);
        when(addressRepository.findSliceByCustomerIdAsDto(CUSTOMER_ID, pageable)).thenReturn(addressSlice);
        when(modelMapper.map(addressResponse, ListAddressResponse.class)).thenReturn(addressResponse);

        var result = addressManager.getAll(pageable);

        assertTrue(result.isSuccess());
        assertFalse(result.getData().isEmpty());
        assertEquals(1, result.getData().size());
    }

    // 3. add TESTS
    @Test
    void add_whenAddressDoesNotExist_shouldCreateNewAddressAndLinkToCustomer() {
        AddressRequest request = new AddressRequest();
        request.setAddressLine1("123 Main St");
        request.setCountryId(1L);
        request.setIsShippingAddress(true);
        request.setIsBillingAddress(false);

        Customer customer = new Customer();
        customer.setId(CUSTOMER_ID);
        Country country = new Country();
        country.setId(1L);
        Address newAddress = new Address();
        newAddress.setId(10L);
        CustomerAddress link = new CustomerAddress(new CustomerAddressId(CUSTOMER_ID, 10L), customer, newAddress, true, false);
        AddressDetailResponse expectedResponse = new AddressDetailResponse();
        expectedResponse.setId(10L);

        when(cacheHelper.getAuthenticatedUserId()).thenReturn(CUSTOMER_ID);
        when(modelMapperService.getMapper()).thenReturn(modelMapper);
        when(customerService.getByIdAsEntity(CUSTOMER_ID)).thenReturn(customer);
        when(addressRepository.findByAddressLine1AndAddressLine2AndCityAndPostalCodeAndCountryId(any(), any(), any(), any(), any())).thenReturn(Optional.empty());
        when(countryService.getByIdAsEntity(any())).thenReturn(country);
        when(modelMapper.map(request, Address.class)).thenReturn(newAddress);
        when(addressRepository.save(any(Address.class))).thenReturn(newAddress);
        when(customerAddressRepository.save(any(CustomerAddress.class))).thenReturn(link);
        when(modelMapper.map(link, AddressDetailResponse.class)).thenReturn(expectedResponse);

        var result = addressManager.add(request);

        assertTrue(result.isSuccess());
        assertEquals(expectedResponse.getId(), result.getData().getId());
        verify(addressRepository).save(any(Address.class));
        verify(customerAddressRepository).save(any(CustomerAddress.class));
    }

    @Test
    void add_whenAddressExistsAndAlreadyLinked_shouldThrowBusinessException() {
        AddressRequest request = new AddressRequest();
        request.setAddressLine1("789 Pine Ln");
        request.setCountryId(3L);
        request.setIsShippingAddress(false);
        request.setIsBillingAddress(false);

        Address existingAddress = new Address();
        existingAddress.setId(30L);

        when(cacheHelper.getAuthenticatedUserId()).thenReturn(CUSTOMER_ID);
        when(customerService.getByIdAsEntity(CUSTOMER_ID)).thenReturn(new Customer());
        when(addressRepository.findByAddressLine1AndAddressLine2AndCityAndPostalCodeAndCountryId(any(), any(), any(), any(), any())).thenReturn(Optional.of(existingAddress));
        when(customerAddressRepository.existsByCustomerIdAndAddressId(CUSTOMER_ID, 30L)).thenReturn(true);
        when(messageService.getMessage(any())).thenReturn("Error");

        assertThrows(BusinessException.class, () -> addressManager.add(request));
    }

    // 4. update TESTS
    @Test
    void update_whenUserUpdatesOwnAddress_shouldSucceed() {
        Long addressId = 15L;
        AddressRequest request = new AddressRequest();
        request.setCountryId(1L);
        request.setIsShippingAddress(true);
        request.setIsBillingAddress(true);

        Address addressToUpdate = new Address();
        addressToUpdate.setId(addressId);
        CustomerAddress link = new CustomerAddress(new CustomerAddressId(CUSTOMER_ID, addressId), new Customer(), addressToUpdate, false, false);
        AddressDetailResponse expectedResponse = new AddressDetailResponse();

        when(cacheHelper.getAuthenticatedUserId()).thenReturn(CUSTOMER_ID);
        when(modelMapperService.getMapper()).thenReturn(modelMapper);
        when(customerAddressRepository.findByAddressIdAndCustomerId(addressId, CUSTOMER_ID)).thenReturn(Optional.of(link));
        when(countryService.getByIdAsEntity(any())).thenReturn(new Country());
        doNothing().when(modelMapper).map(eq(request), eq(addressToUpdate));
        when(customerAddressRepository.save(any(CustomerAddress.class))).thenReturn(link);
        when(modelMapper.map(link, AddressDetailResponse.class)).thenReturn(expectedResponse);

        var result = addressManager.update(addressId, request);

        assertTrue(result.isSuccess());
        verify(addressRepository).save(addressToUpdate);
        verify(modelMapper).map(request, addressToUpdate);
    }

    @Test
    void update_whenUserUpdatesAddressNotBelongingToThem_shouldThrowNotFoundException() {
        Long addressId = 50L;
        AddressRequest request = new AddressRequest();
        request.setIsShippingAddress(false);
        request.setIsBillingAddress(false);

        when(cacheHelper.getAuthenticatedUserId()).thenReturn(CUSTOMER_ID);
        when(customerAddressRepository.findByAddressIdAndCustomerId(addressId, CUSTOMER_ID)).thenReturn(Optional.empty());
        when(messageService.getMessageWithParams(any(), any())).thenReturn("Error");

        assertThrows(NotFoundException.class, () -> addressManager.update(addressId, request));
    }

    // 5. delete TESTS
    @Test
    void delete_whenAddressIsInUseByAnOrder_shouldThrowBusinessException() {
        Long addressId = 20L;
        when(orderService.existsByShippingAddressId(addressId)).thenReturn(true);
        when(messageService.getMessage(any())).thenReturn("Error");

        assertThrows(BusinessException.class, () -> addressManager.delete(addressId));
        verify(customerAddressRepository, never()).delete(any());
    }

    @Test
    void delete_whenAddressIsOrphanedAfterDeletion_shouldDeleteAddressEntity() {
        Long addressId = 21L;
        Address address = new Address();
        address.setId(addressId);
        CustomerAddress linkToDelete = new CustomerAddress(null, null, address, false, false);

        when(cacheHelper.getAuthenticatedUserId()).thenReturn(CUSTOMER_ID);
        when(orderService.existsByShippingAddressId(addressId)).thenReturn(false);
        when(customerAddressRepository.findByAddressIdAndCustomerId(addressId, CUSTOMER_ID)).thenReturn(Optional.of(linkToDelete));
        when(customerAddressRepository.countByAddressId(addressId)).thenReturn(0L);

        addressManager.delete(addressId);

        verify(customerAddressRepository).delete(linkToDelete);
        verify(addressRepository).deleteById(addressId);
    }

    @Test
    void delete_whenDefaultAddressIsDeleted_shouldReassignNewDefault() {
        Long deletedAddressId = 22L;
        Long newDefaultAddressId = 23L;

        Address deletedAddress = new Address();
        deletedAddress.setId(deletedAddressId);

        Address newDefaultAddress = new Address();
        newDefaultAddress.setId(newDefaultAddressId);

        CustomerAddress linkToDelete = new CustomerAddress(null, null, deletedAddress, true, false);
        CustomerAddress newDefaultLink = new CustomerAddress(null, null, newDefaultAddress, false, false);

        when(cacheHelper.getAuthenticatedUserId()).thenReturn(CUSTOMER_ID);
        when(orderService.existsByShippingAddressId(deletedAddressId)).thenReturn(false);
        when(customerAddressRepository.findByAddressIdAndCustomerId(deletedAddressId, CUSTOMER_ID)).thenReturn(Optional.of(linkToDelete));
        when(customerAddressRepository.findPotentialNewDefaultAddresses(CUSTOMER_ID, deletedAddressId)).thenReturn(Collections.singletonList(newDefaultLink));

        ArgumentCaptor<CustomerAddress> captor = ArgumentCaptor.forClass(CustomerAddress.class);

        addressManager.delete(deletedAddressId);

        verify(customerAddressRepository).delete(linkToDelete);
        verify(customerAddressRepository).save(captor.capture());

        CustomerAddress savedLink = captor.getValue();
        assertTrue(savedLink.getIsShippingAddress());
        assertEquals(newDefaultAddressId, savedLink.getAddress().getId());
    }

    // 6. getByIdAsEntity TESTS
    @Test
    void getByIdAsEntity_whenAddressExists_shouldReturnAddress() {
        Long addressId = 30L;
        Address address = new Address();
        address.setId(addressId);
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(address));

        Address result = addressManager.getByIdAsEntity(addressId);

        assertEquals(addressId, result.getId());
    }

    // 7. getByIdAndCustomerId TESTS
    @Test
    void getByIdAndCustomerId_whenLinkExists_shouldReturnAddress() {
        Long addressId = 31L;
        Address address = new Address();
        address.setId(addressId);
        CustomerAddress link = new CustomerAddress(null, null, address, false, false);
        when(customerAddressRepository.findByAddressIdAndCustomerId(addressId, CUSTOMER_ID)).thenReturn(Optional.of(link));

        Address result = addressManager.getByIdAndCustomerId(addressId, CUSTOMER_ID);

        assertEquals(addressId, result.getId());
    }
}
