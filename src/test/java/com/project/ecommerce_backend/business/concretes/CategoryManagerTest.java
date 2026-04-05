package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.business.abstracts.CategoryService;
import com.project.ecommerce_backend.business.abstracts.ProductService;
import com.project.ecommerce_backend.business.dtos.requests.category.AddCategoryRequest;
import com.project.ecommerce_backend.business.dtos.requests.category.UpdateCategoryRequest;
import com.project.ecommerce_backend.business.dtos.responses.category.*;
import com.project.ecommerce_backend.business.dtos.responses.common.SliceResponseDTO;
import com.project.ecommerce_backend.core.exceptions.types.BusinessException;
import com.project.ecommerce_backend.core.exceptions.types.NotFoundException;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.core.utils.mapper.ModelMapperService;
import com.project.ecommerce_backend.entities.concretes.Category;
import com.project.ecommerce_backend.repositories.abstracts.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryManagerTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CategoryService self;
    @Mock
    private ProductService productService;
    @Mock
    private ModelMapperService modelMapperService;
    @Mock
    private MessageService messageService;
    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private CategoryManager categoryManager;

    // 1. getById TESTS
    @Test
    void getById_whenCategoryExists_shouldReturnCategoryDetail() {
        Long id = 1L;
        Category category = new Category();
        category.setId(id);
        CategoryDetailResponse expectedResponse = new CategoryDetailResponse();
        expectedResponse.setId(id);

        when(categoryRepository.findByIdWithDetails(id)).thenReturn(Optional.of(category));
        when(modelMapperService.getMapper()).thenReturn(modelMapper);
        when(modelMapper.map(category, CategoryDetailResponse.class)).thenReturn(expectedResponse);

        var result = categoryManager.getById(id);

        assertTrue(result.isSuccess());
        assertEquals(id, result.getData().getId());
    }

    @Test
    void getById_whenCategoryDoesNotExist_shouldThrowNotFoundException() {
        Long id = 1L;
        when(categoryRepository.findByIdWithDetails(id)).thenReturn(Optional.empty());
        when(messageService.getMessageWithParams(any(), any())).thenReturn("Error");

        assertThrows(NotFoundException.class, () -> categoryManager.getById(id));
    }

    // 2. fetchCategorySliceData TESTS
    @Test
    void fetchCategorySliceData_shouldReturnSliceResponse() {
        Pageable pageable = Pageable.unpaged();
        ListCategoryResponse responseItem = new ListCategoryResponse();
        Slice<ListCategoryResponse> slice = new PageImpl<>(Collections.singletonList(responseItem));

        when(categoryRepository.getAllWithPagination(pageable)).thenReturn(slice);

        var result = categoryManager.fetchCategorySliceData(pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    // 3. getAllWithPagination TESTS
    @Test
    void getAllWithPagination_shouldReturnDataResult() {
        Pageable pageable = Pageable.unpaged();
        ListCategoryResponse responseItem = new ListCategoryResponse();
        SliceResponseDTO<ListCategoryResponse> dto = new SliceResponseDTO<>(
                Collections.singletonList(responseItem), false, 0, 10, 1, true, true
        );

        when(self.fetchCategorySliceData(pageable)).thenReturn(dto);

        var result = categoryManager.getAllWithPagination(pageable);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().getContent().size());
    }

    // 4. getAllAsTree TESTS
    @Test
    void getAllAsTree_shouldReturnTreeStructure() {
        ListCategoryResponse parent = new ListCategoryResponse();
        parent.setId(1L);
        parent.setName("Parent");

        ListCategoryResponse child = new ListCategoryResponse();
        child.setId(2L);
        child.setName("Child");
        child.setParentCategoryId(1L);

        List<ListCategoryResponse> flatList = Arrays.asList(parent, child);

        when(categoryRepository.getAll()).thenReturn(flatList);

        var result = categoryManager.getAllAsTree();

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size()); // Only 1 root node
        assertEquals(1, result.getData().get(0).getChildren().size()); // Root has 1 child
        assertEquals(2L, result.getData().get(0).getChildren().get(0).getId());
    }

    // 5. getByIdsAsEntity TESTS
    @Test
    void getByIdsAsEntity_whenAllExist_shouldReturnList() {
        List<Long> ids = Arrays.asList(1L, 2L);
        Category c1 = new Category(); c1.setId(1L);
        Category c2 = new Category(); c2.setId(2L);
        List<Category> categories = Arrays.asList(c1, c2);

        when(categoryRepository.findAllById(ids)).thenReturn(categories);

        var result = categoryManager.getByIdsAsEntity(ids);

        assertEquals(2, result.size());
    }

    @Test
    void getByIdsAsEntity_whenSomeMissing_shouldThrowNotFoundException() {
        List<Long> ids = Arrays.asList(1L, 2L);
        Category c1 = new Category(); c1.setId(1L);
        List<Category> categories = Collections.singletonList(c1);

        when(categoryRepository.findAllById(ids)).thenReturn(categories);
        when(messageService.getMessage(any())).thenReturn("Error");

        assertThrows(NotFoundException.class, () -> categoryManager.getByIdsAsEntity(ids));
    }

    // 6. add TESTS
    @Test
    void add_whenNameUnique_shouldAddCategory() {
        AddCategoryRequest request = new AddCategoryRequest();
        request.setName("New Category");
        Category category = new Category();
        AddCategoryResponse response = new AddCategoryResponse();

        when(categoryRepository.existsByName(request.getName())).thenReturn(false);
        when(modelMapperService.getMapper()).thenReturn(modelMapper);
        when(modelMapper.map(request, Category.class)).thenReturn(category);
        when(categoryRepository.save(category)).thenReturn(category);
        when(modelMapper.map(category, AddCategoryResponse.class)).thenReturn(response);

        var result = categoryManager.add(request);

        assertTrue(result.isSuccess());
        verify(categoryRepository).save(category);
    }

    @Test
    void add_whenNameExists_shouldThrowBusinessException() {
        AddCategoryRequest request = new AddCategoryRequest();
        request.setName("Existing");

        when(categoryRepository.existsByName(request.getName())).thenReturn(true);
        when(messageService.getMessage(any())).thenReturn("Error");

        assertThrows(BusinessException.class, () -> categoryManager.add(request));
    }

    // 7. update TESTS
    @Test
    void update_whenValid_shouldUpdateCategory() {
        UpdateCategoryRequest request = new UpdateCategoryRequest();
        request.setId(1L);
        request.setName("Updated Name");

        Category category = new Category();
        category.setId(1L);
        UpdateCategoryResponse response = new UpdateCategoryResponse();

        when(categoryRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameAndIdNot(request.getName(), 1L)).thenReturn(false);
        when(modelMapperService.getMapper()).thenReturn(modelMapper);
        
        // FIX: Add stubbing for void map method
        doNothing().when(modelMapper).map(eq(request), eq(category));

        when(categoryRepository.save(category)).thenReturn(category);
        when(modelMapper.map(category, UpdateCategoryResponse.class)).thenReturn(response);

        var result = categoryManager.update(request);

        assertTrue(result.isSuccess());
        verify(categoryRepository).save(category);
    }

    @Test
    void update_whenCircularDependency_shouldThrowBusinessException() {
        UpdateCategoryRequest request = new UpdateCategoryRequest();
        request.setId(1L);
        request.setParentCategoryId(1L); // Self-parenting

        Category category = new Category();
        category.setId(1L);

        when(categoryRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category)); // getParentCategoryById
        when(messageService.getMessage(any())).thenReturn("Error");

        assertThrows(BusinessException.class, () -> categoryManager.update(request));
    }

    // 8. delete TESTS
    @Test
    void delete_whenSafe_shouldDeleteCategory() {
        Long id = 1L;
        Category category = new Category();
        category.setId(id);

        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByParentCategoryId(id)).thenReturn(false);
        when(productService.existsByCategoryId(id)).thenReturn(false);

        var result = categoryManager.delete(id);

        assertTrue(result.isSuccess());
        assertFalse(category.getIsActive());
        verify(categoryRepository).save(category);
    }

    @Test
    void delete_whenHasSubCategories_shouldThrowBusinessException() {
        Long id = 1L;
        Category category = new Category();
        category.setId(id);

        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByParentCategoryId(id)).thenReturn(true);
        when(messageService.getMessage(any())).thenReturn("Error");

        assertThrows(BusinessException.class, () -> categoryManager.delete(id));
    }
}
