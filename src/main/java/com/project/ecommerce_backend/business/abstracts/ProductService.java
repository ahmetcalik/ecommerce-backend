package com.project.ecommerce_backend.business.abstracts;

import com.project.ecommerce_backend.business.dtos.requests.product.AddProductRequest;
import com.project.ecommerce_backend.business.dtos.requests.product.UpdateProductRequest;
import com.project.ecommerce_backend.business.dtos.responses.common.SliceResponseDTO;
import com.project.ecommerce_backend.business.dtos.responses.product.AddProductResponse;
import com.project.ecommerce_backend.business.dtos.responses.product.ListProductsResponse;
import com.project.ecommerce_backend.business.dtos.responses.product.ProductDetailResponse;
import com.project.ecommerce_backend.core.utils.result.DataResult;
import com.project.ecommerce_backend.core.utils.result.Result;
import com.project.ecommerce_backend.entities.concretes.ProductItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface ProductService {

    DataResult<ProductDetailResponse> getById(Long id);

    SliceResponseDTO<ListProductsResponse> fetchProductSliceData(Pageable pageable, Long categoryId, Long supplierId, String nameSearch);

    DataResult<Slice<ListProductsResponse>> getAllWithPagination(Pageable pageable, Long categoryId, Long supplierId, String nameSearch);

    DataResult<AddProductResponse> add(AddProductRequest addProductRequest);

    DataResult<ProductDetailResponse> update(Long id, UpdateProductRequest updateProductRequest);

    Result delete(Long id);

    boolean existsByCategoryId(Long categoryId);

    ProductItem getProductItemById(Long id);

    void checkAndReduceStock(Long productItemId, int quantity);

    void increaseStock(Long productItemId, int quantity);

}