package com.project.ecommerce_backend.business.abstracts;

import com.project.ecommerce_backend.business.dtos.requests.category.AddCategoryRequest;
import com.project.ecommerce_backend.business.dtos.requests.category.UpdateCategoryRequest;
import com.project.ecommerce_backend.business.dtos.responses.category.*;
import com.project.ecommerce_backend.business.dtos.responses.common.SliceResponseDTO;
import com.project.ecommerce_backend.core.utils.result.DataResult;
import com.project.ecommerce_backend.core.utils.result.Result;
import com.project.ecommerce_backend.entities.concretes.Category;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;

public interface CategoryService {

    DataResult<CategoryDetailResponse> getById(Long id);

    SliceResponseDTO<ListCategoryResponse> fetchCategorySliceData(Pageable pageable);

    DataResult<Slice<ListCategoryResponse>> getAllWithPagination(Pageable pageable);

    DataResult<List<CategoryTreeResponse>> getAllAsTree();

    List<Category> getByIdsAsEntity(List<Long> ids);

    DataResult<AddCategoryResponse> add(AddCategoryRequest addCategoryRequest);

    DataResult<UpdateCategoryResponse> update(UpdateCategoryRequest updateCategoryRequest);

    Result delete(Long id);

}