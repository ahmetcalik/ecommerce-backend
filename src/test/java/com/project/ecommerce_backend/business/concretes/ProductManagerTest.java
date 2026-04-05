package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.business.abstracts.*;
import com.project.ecommerce_backend.business.dtos.requests.product.AddProductImageRequest;
import com.project.ecommerce_backend.business.dtos.requests.product.AddProductItemRequest;
import com.project.ecommerce_backend.business.dtos.requests.product.AddProductRequest;
import com.project.ecommerce_backend.business.dtos.requests.product.UpdateProductRequest;
import com.project.ecommerce_backend.business.dtos.responses.common.SliceResponseDTO;
import com.project.ecommerce_backend.business.dtos.responses.product.AddProductResponse;
import com.project.ecommerce_backend.business.dtos.responses.product.ListProductsResponse;
import com.project.ecommerce_backend.business.dtos.responses.product.ProductDetailResponse;
import com.project.ecommerce_backend.business.helpers.CacheHelper;
import com.project.ecommerce_backend.core.constants.Messages;
import com.project.ecommerce_backend.core.exceptions.types.AuthorizationBusinessException;
import com.project.ecommerce_backend.core.exceptions.types.BusinessException;
import com.project.ecommerce_backend.core.exceptions.types.NotFoundException;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.core.security.abstracts.UserContextService;
import com.project.ecommerce_backend.core.utils.mapper.ModelMapperService;
import com.project.ecommerce_backend.core.utils.result.DataResult;
import com.project.ecommerce_backend.core.utils.result.Result;
import com.project.ecommerce_backend.entities.concretes.*;
import com.project.ecommerce_backend.repositories.abstracts.ProductCategoryRepository;
import com.project.ecommerce_backend.repositories.abstracts.ProductImageRepository;
import com.project.ecommerce_backend.repositories.abstracts.ProductItemRepository;
import com.project.ecommerce_backend.repositories.abstracts.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductManagerTest {

    @Mock private ProductRepository productRepository;
    @Mock private ProductItemRepository productItemRepository;
    @Mock private ProductCategoryRepository productCategoryRepository;
    @Mock private ProductImageRepository productImageRepository;
    @Mock private ProductService self;
    @Mock private SupplierService supplierService;
    @Mock private CategoryService categoryService;
    @Mock private ColourService colourService;
    @Mock private SizeService sizeService;
    @Mock private ModelMapperService modelMapperService;
    @Mock private MessageService messageService;
    @Mock private CacheHelper cacheHelper;
    @Mock private ModelMapper modelMapper;

    @InjectMocks
    private ProductManager productManager;

    private Product product;
    private Supplier supplier;
    private Category category;
    private ProductItem productItem;
    private Colour colour;
    private Size size;

    @BeforeEach
    void setUp() {
        supplier = new Supplier();
        supplier.setId(1L);

        category = new Category();
        category.setId(1L);

        product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setSupplier(supplier);
        product.setCUser(1L);
        product.setIsActive(true);

        colour = new Colour();
        colour.setId(1L);

        size = new Size();
        size.setId(1L);

        productItem = new ProductItem();
        productItem.setId(1L);
        productItem.setProduct(product);
        productItem.setColour(colour);
        productItem.setSize(size);
        productItem.setQuantityInStock(10);
    }

    // --- GET BY ID TESTS ---

    @Test
    void getById_ShouldReturnProductDetail_WhenProductExists() {
        ProductDetailResponse response = new ProductDetailResponse();
        response.setId(1L);

        when(productRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(product));
        when(modelMapperService.getMapper()).thenReturn(modelMapper);
        when(modelMapper.map(product, ProductDetailResponse.class)).thenReturn(response);
        when(messageService.getMessage(Messages.Product.PRODUCT_DETAIL_SUCCESSFULLY_LISTED))
                .thenReturn("Product detail listed");

        DataResult<ProductDetailResponse> result = productManager.getById(1L);

        assertTrue(result.isSuccess());
        assertEquals(1L, result.getData().getId());
        assertEquals("Product detail listed", result.getMessage());
    }

    @Test
    void getById_ShouldThrowNotFoundException_WhenProductDoesNotExist() {
        when(productRepository.findByIdWithDetails(99L)).thenReturn(Optional.empty());
        when(messageService.getMessageWithParams(Messages.Product.PRODUCT_DOES_NOT_EXIST_WITH_GIVEN_ID, 99L))
                .thenReturn("Product not found");

        NotFoundException exception = assertThrows(NotFoundException.class, () -> productManager.getById(99L));
        assertEquals("Product not found", exception.getMessage());
    }

    // --- FETCH PRODUCT SLICE DATA TESTS ---

    @Test
    void fetchProductSliceData_ShouldReturnSliceResponse() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(List.of(product));
        ListProductsResponse listResponse = new ListProductsResponse();

        when(productRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(modelMapperService.getMapper()).thenReturn(modelMapper);
        when(modelMapper.map(product, ListProductsResponse.class)).thenReturn(listResponse);

        SliceResponseDTO<ListProductsResponse> result = productManager.fetchProductSliceData(pageable, null, null, null);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getNumberOfElements());
    }

    // --- GET ALL WITH PAGINATION TESTS ---

    @Test
    void getAllWithPagination_ShouldReturnDataResult() {
        Pageable pageable = PageRequest.of(0, 10);
        SliceResponseDTO<ListProductsResponse> sliceResponse = new SliceResponseDTO<>(
                Collections.emptyList(), false, 0, 10, 0, true, true
        );

        when(self.fetchProductSliceData(pageable, null, null, null)).thenReturn(sliceResponse);
        when(messageService.getMessage(Messages.Product.PRODUCTS_SUCCESSFULLY_LISTED))
                .thenReturn("Products listed");

        DataResult<Slice<ListProductsResponse>> result = productManager.getAllWithPagination(pageable, null, null, null);

        assertTrue(result.isSuccess());
        assertEquals("Products listed", result.getMessage());
    }

    // --- ADD PRODUCT TESTS ---

    @Test
    void add_ShouldAddProduct_WhenValidRequest() {
        AddProductRequest request = new AddProductRequest();
        request.setName("New Product");
        request.setCategoryIds(List.of(1L));
        
        AddProductItemRequest itemRequest = new AddProductItemRequest();
        itemRequest.setColourId(1L);
        itemRequest.setSizeId(1L);
        request.setItems(List.of(itemRequest));
        
        AddProductImageRequest imageRequest = new AddProductImageRequest();
        imageRequest.setMainImage(true);
        request.setImages(List.of(imageRequest));

        Product savedProduct = new Product();
        savedProduct.setId(1L);

        when(supplierService.getAuthenticatedSupplierAsEntity()).thenReturn(supplier);
        when(productRepository.existsByName(request.getName())).thenReturn(false);
        when(categoryService.getByIdsAsEntity(request.getCategoryIds())).thenReturn(List.of(category));
        when(modelMapperService.getMapper()).thenReturn(modelMapper);
        when(modelMapper.map(request, Product.class)).thenReturn(product);
        when(productRepository.save(product)).thenReturn(savedProduct);
        
        when(colourService.getByIdAsEntity(1L)).thenReturn(colour);
        when(sizeService.getByIdAsEntity(1L)).thenReturn(size);
        when(modelMapper.map(itemRequest, ProductItem.class)).thenReturn(productItem);
        
        when(modelMapper.map(imageRequest, ProductImage.class)).thenReturn(new ProductImage());

        when(productRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(savedProduct));
        when(modelMapper.map(savedProduct, AddProductResponse.class)).thenReturn(new AddProductResponse());
        when(messageService.getMessage(Messages.Product.PRODUCT_SUCCESSFULLY_ADDED)).thenReturn("Product added");

        DataResult<AddProductResponse> result = productManager.add(request);

        assertTrue(result.isSuccess());
        verify(productRepository).save(product);
        verify(productCategoryRepository).saveAll(anyList());
        verify(productItemRepository).saveAll(anyList());
        verify(productImageRepository).saveAll(anyList());
    }

    @Test
    void add_ShouldThrowBusinessException_WhenNameExists() {
        AddProductRequest request = new AddProductRequest();
        request.setName("Existing Product");

        when(supplierService.getAuthenticatedSupplierAsEntity()).thenReturn(supplier);
        when(productRepository.existsByName(request.getName())).thenReturn(true);
        when(messageService.getMessage(Messages.Product.PRODUCT_ALREADY_EXIST)).thenReturn("Product already exists");

        BusinessException exception = assertThrows(BusinessException.class, () -> productManager.add(request));
        assertEquals("Product already exists", exception.getMessage());
    }

    @Test
    void add_ShouldThrowBusinessException_WhenMultipleMainImages() {
        AddProductRequest request = new AddProductRequest();
        request.setName("New Product");
        request.setCategoryIds(List.of(1L));
        
        AddProductImageRequest img1 = new AddProductImageRequest(); img1.setMainImage(true);
        AddProductImageRequest img2 = new AddProductImageRequest(); img2.setMainImage(true);
        request.setImages(List.of(img1, img2));
        request.setItems(Collections.emptyList()); // Initialize items list to avoid NPE

        when(supplierService.getAuthenticatedSupplierAsEntity()).thenReturn(supplier);
        when(productRepository.existsByName(request.getName())).thenReturn(false);
        when(categoryService.getByIdsAsEntity(any())).thenReturn(List.of(category));
        when(modelMapperService.getMapper()).thenReturn(modelMapper);
        when(modelMapper.map(request, Product.class)).thenReturn(product);
        when(productRepository.save(product)).thenReturn(product);
        when(messageService.getMessage(Messages.Product.CANNOT_HAVE_MULTIPLE_MAIN_IMAGES)).thenReturn("Multiple main images");

        BusinessException exception = assertThrows(BusinessException.class, () -> productManager.add(request));
        assertEquals("Multiple main images", exception.getMessage());
    }

    // --- UPDATE PRODUCT TESTS ---

    @Test
    void update_ShouldUpdateProduct_WhenValidRequest() {
        UpdateProductRequest request = new UpdateProductRequest();
        request.setName("Updated Product");
        request.setCategoryIds(List.of(1L));
        request.setItems(Collections.emptyList());
        request.setImages(Collections.emptyList());

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cacheHelper.getAuthenticatedUserId()).thenReturn(1L);
        when(supplierService.getAuthenticatedSupplierAsEntity()).thenReturn(supplier);
        when(productRepository.findByNameAndIdNot(request.getName(), 1L)).thenReturn(Optional.empty());
        when(categoryService.getByIdsAsEntity(any())).thenReturn(List.of(category));
        when(modelMapperService.getMapper()).thenReturn(modelMapper);
        
        // Use lenient() or doNothing() for void method to avoid strict stubbing issues
        doNothing().when(modelMapper).map(eq(request), eq(product));
        
        when(productRepository.save(product)).thenReturn(product);
        when(modelMapper.map(product, ProductDetailResponse.class)).thenReturn(new ProductDetailResponse());
        when(messageService.getMessage(Messages.Product.PRODUCT_SUCCESSFULLY_UPDATED)).thenReturn("Product updated");

        DataResult<ProductDetailResponse> result = productManager.update(1L, request);

        assertTrue(result.isSuccess());
        verify(productCategoryRepository).deleteByProductId(1L);
        verify(productItemRepository).deleteByProductId(1L);
        verify(productImageRepository).deleteByProductId(1L);
    }

    @Test
    void update_ShouldThrowAuthorizationException_WhenUserNotOwner() {
        UpdateProductRequest request = new UpdateProductRequest();
        product.setCUser(2L); // Different user

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cacheHelper.getAuthenticatedUserId()).thenReturn(1L);
        when(messageService.getMessage(Messages.Auth.AUTHORIZATION_FAILED)).thenReturn("Authorization failed");

        AuthorizationBusinessException exception = assertThrows(AuthorizationBusinessException.class, 
                () -> productManager.update(1L, request));
        assertEquals("Authorization failed", exception.getMessage());
    }

    @Test
    void update_ShouldThrowBusinessException_WhenNameExistsForOtherProduct() {
        UpdateProductRequest request = new UpdateProductRequest();
        request.setName("Existing Name");

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cacheHelper.getAuthenticatedUserId()).thenReturn(1L);
        when(supplierService.getAuthenticatedSupplierAsEntity()).thenReturn(supplier);
        when(productRepository.findByNameAndIdNot(request.getName(), 1L)).thenReturn(Optional.of(new Product()));
        when(messageService.getMessage(Messages.Product.PRODUCT_ALREADY_EXIST)).thenReturn("Product already exists");

        BusinessException exception = assertThrows(BusinessException.class, 
                () -> productManager.update(1L, request));
        assertEquals("Product already exists", exception.getMessage());
    }

    // --- DELETE PRODUCT TESTS ---

    @Test
    void delete_ShouldDeactivateProduct_WhenOwner() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cacheHelper.getAuthenticatedUserId()).thenReturn(1L);
        when(messageService.getMessage(Messages.Product.PRODUCT_SUCCESSFULLY_DELETED)).thenReturn("Product deleted");

        Result result = productManager.delete(1L);

        assertTrue(result.isSuccess());
        assertFalse(product.getIsActive());
        verify(productRepository).save(product);
    }

    @Test
    void delete_ShouldThrowAuthorizationException_WhenNotOwner() {
        product.setCUser(2L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cacheHelper.getAuthenticatedUserId()).thenReturn(1L);
        when(messageService.getMessage(Messages.Auth.AUTHORIZATION_FAILED)).thenReturn("Authorization failed");

        AuthorizationBusinessException exception = assertThrows(AuthorizationBusinessException.class, 
                () -> productManager.delete(1L));
        assertEquals("Authorization failed", exception.getMessage());
    }

    // --- GET PRODUCT ITEM BY ID TESTS ---

    @Test
    void getProductItemById_ShouldReturnItem_WhenExists() {
        when(productItemRepository.findById(1L)).thenReturn(Optional.of(productItem));

        ProductItem result = productManager.getProductItemById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getProductItemById_ShouldThrowNotFoundException_WhenMissing() {
        when(productItemRepository.findById(99L)).thenReturn(Optional.empty());
        when(messageService.getMessageWithParams(Messages.Product.PRODUCT_ITEM_NOT_FOUND, 99L))
                .thenReturn("Item not found");

        NotFoundException exception = assertThrows(NotFoundException.class, 
                () -> productManager.getProductItemById(99L));
        assertEquals("Item not found", exception.getMessage());
    }

    // --- EXISTS BY CATEGORY ID TESTS ---

    @Test
    void existsByCategoryId_ShouldReturnTrue_WhenExists() {
        when(productRepository.existsByProductCategories_CategoryId(1L)).thenReturn(true);
        assertTrue(productManager.existsByCategoryId(1L));
    }

    // --- CHECK AND REDUCE STOCK TESTS ---

    @Test
    void checkAndReduceStock_ShouldReduceStock_WhenSufficient() {
        when(productItemRepository.findById(1L)).thenReturn(Optional.of(productItem));

        productManager.checkAndReduceStock(1L, 5);

        assertEquals(5, productItem.getQuantityInStock());
        verify(productItemRepository).save(productItem);
    }

    @Test
    void checkAndReduceStock_ShouldThrowException_WhenInsufficient() {
        when(productItemRepository.findById(1L)).thenReturn(Optional.of(productItem));
        when(messageService.getMessageWithParams(eq(Messages.Product.INSUFFICIENT_STOCK), any()))
                .thenReturn("Insufficient stock");

        BusinessException exception = assertThrows(BusinessException.class, 
                () -> productManager.checkAndReduceStock(1L, 15));
        assertEquals("Insufficient stock", exception.getMessage());
    }

    // --- INCREASE STOCK TESTS ---

    @Test
    void increaseStock_ShouldIncreaseStock_WhenValid() {
        when(productItemRepository.findById(1L)).thenReturn(Optional.of(productItem));

        productManager.increaseStock(1L, 5);

        assertEquals(15, productItem.getQuantityInStock());
        verify(productItemRepository).save(productItem);
    }

    @Test
    void increaseStock_ShouldThrowException_WhenQuantityNegative() {
        when(productItemRepository.findById(1L)).thenReturn(Optional.of(productItem));
        when(messageService.getMessage(Messages.Product.QUANTITY_MUST_BE_POSITIVE))
                .thenReturn("Quantity must be positive");

        BusinessException exception = assertThrows(BusinessException.class, 
                () -> productManager.increaseStock(1L, -5));
        assertEquals("Quantity must be positive", exception.getMessage());
    }
}