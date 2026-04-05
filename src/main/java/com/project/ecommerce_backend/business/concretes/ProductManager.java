package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.business.abstracts.*;
import com.project.ecommerce_backend.business.dtos.requests.product.AddProductImageRequest;
import com.project.ecommerce_backend.business.dtos.requests.product.AddProductRequest;
import com.project.ecommerce_backend.business.dtos.requests.product.UpdateProductRequest;
import com.project.ecommerce_backend.business.dtos.requests.product.AddProductItemRequest;
import com.project.ecommerce_backend.business.dtos.responses.common.SliceResponseDTO;
import com.project.ecommerce_backend.business.dtos.responses.product.AddProductResponse;
import com.project.ecommerce_backend.business.dtos.responses.product.ProductDetailResponse;
import com.project.ecommerce_backend.business.dtos.responses.product.ListProductsResponse;
import com.project.ecommerce_backend.business.helpers.CacheHelper;
import com.project.ecommerce_backend.core.constants.Messages;
import com.project.ecommerce_backend.core.exceptions.types.AuthorizationBusinessException;
import com.project.ecommerce_backend.core.exceptions.types.BusinessException;
import com.project.ecommerce_backend.core.exceptions.types.NotFoundException;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.core.utils.mapper.ModelMapperService;
import com.project.ecommerce_backend.core.utils.result.DataResult;
import com.project.ecommerce_backend.core.utils.result.Result;
import com.project.ecommerce_backend.core.utils.result.SuccessDataResult;
import com.project.ecommerce_backend.core.utils.result.SuccessResult;
import com.project.ecommerce_backend.entities.concretes.*;
import com.project.ecommerce_backend.repositories.abstracts.*;
import com.project.ecommerce_backend.repositories.concretes.specifications.ProductSpecification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;

/**
 * Sistemdeki ürün ekosisteminin ve envanter döngüsünün yönetiminden sorumlu merkezi iş mantığı servisidir.
 * Bu sınıf; ürünlerin renk ve beden gibi varyantlar üzerinden detaylandırılması, çoklu kategori hiyerarşisine dahil edilmesi, gelişmiş filtreleme algoritmalarıyla listelenmesi ve stok seviyelerinin asenkron ya da senkron süreçlerle korunması görevlerini üstlenir. Satıcı bazlı veri güvenliğini ön planda tutarak, ürün verilerini hem tekil hem de sayfalı listeler halinde Redis üzerinde yüksek performanslı önbellekleme stratejileriyle yönetir.
 */
@Service
public class ProductManager implements ProductService {

    private final ProductRepository productRepository;
    private final ProductItemRepository productItemRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductService self;
    private final SupplierService supplierService;
    private final CategoryService categoryService;
    private final ColourService colourService;
    private final SizeService sizeService;
    private final ModelMapperService modelMapperService;
    private final MessageService messageService;
    private final CacheHelper cacheHelper;

    public ProductManager(ProductRepository productRepository,
                          ProductItemRepository productItemRepository,
                          ProductCategoryRepository productCategoryRepository,
                          ProductImageRepository productImageRepository,
                          @Lazy ProductService self,
                          SupplierService supplierService,
                          @Lazy CategoryService categoryService,
                          ColourService colourService,
                          SizeService sizeService,
                          ModelMapperService modelMapperService,
                          MessageService messageService,
                          CacheHelper cacheHelper) {
        this.productRepository = productRepository;
        this.productItemRepository = productItemRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.productImageRepository = productImageRepository;
        this.self = self;
        this.supplierService = supplierService;
        this.categoryService = categoryService;
        this.colourService = colourService;
        this.sizeService = sizeService;
        this.modelMapperService = modelMapperService;
        this.messageService = messageService;
        this.cacheHelper = cacheHelper;
    }

    @Value("${app.tax.vat-rate}")
    private BigDecimal defaultVatRate;

    /**
     * Belirtilen kimlik numarasına sahip ürünün tüm detaylarını, varyantlarını, görsellerini ve kategori bilgilerini derinlemesine sorgulayarak sunar.
     * Veri çekme işlemi sırasında ürünün tüm ilişkili alt tabloları optimize edilerek getirilir ve elde edilen kapsamlı veri seti, tekrarlı erişimlerde veritabanı maliyetini düşürmek amacıyla ürün kimliği üzerinden Redis üzerinde saklanır.
     *
     * @param id Detayları görüntülenmek istenen ürünün benzersiz kimlik numarasıdır.
     * @return Ürünün tüm niteliklerini barındıran veri transfer nesnesini başarı geri bildirimiyle döner.
     * @throws NotFoundException Ürün bulunamadığında fırlatılır.
     */
    @Override
    @Cacheable(value = "product", key = "#id")
    public DataResult<ProductDetailResponse> getById(Long id) {
        Product product = this.productRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new NotFoundException(messageService.getMessageWithParams(
                        Messages.Product.PRODUCT_DOES_NOT_EXIST_WITH_GIVEN_ID, id)));

        ProductDetailResponse response = modelMapperService.getMapper().map(product, ProductDetailResponse.class);

        return new SuccessDataResult<>(response, messageService.getMessage(
                Messages.Product.PRODUCT_DETAIL_SUCCESSFULLY_LISTED));
    }

    /**
     * Dinamik arama kriterlerine ve hiyerarşik filtrelere göre ürün listesini, dil desteğiyle optimize edilmiş bir önbellek anahtarıyla sorgular.
     * Bu metot; kategori, satıcı ve isim bazlı karmaşık filtreleme mantığını koordine eder ve sayfalı sonuçları kullanıcının tercih ettiği dile göre Redis üzerinde saklayarak küresel pazarda hızlı bir listeleme deneyimi sağlar.
     *
     * @param pageable Sayfa boyutu ve sıralama tercihlerini barındıran nesne.
     * @param categoryId Filtrelenmek istenen kategori kimliğidir.
     * @param supplierId Belirli bir satıcıya ait ürünleri filtrelemek için kullanılır.
     * @param nameSearch İsim bazlı arama terimidir.
     * @return Filtrelenmiş ve sayfalanmış ürün listesini teknik bir veri transfer nesnesi içinde döner.
     */
    @Override
    @Cacheable(
            value = "product_lists",
            key = "#pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort.toString() + '_' + #categoryId + '_' + #supplierId + '_' + #nameSearch + '_' + T(org.springframework.context.i18n.LocaleContextHolder).getLocale().getLanguage()"
    )
    public SliceResponseDTO<ListProductsResponse> fetchProductSliceData(Pageable pageable, Long categoryId, Long supplierId, String nameSearch) {
        Specification<Product> spec = Specification.allOf(
                ProductSpecification.hasCategory(categoryId),
                ProductSpecification.hasSupplier(supplierId),
                ProductSpecification.nameContains(nameSearch)
        );
        Slice<Product> productSlice = productRepository.findAll(spec, pageable);
        List<ListProductsResponse> content = productSlice.getContent().stream()
                .map(product -> modelMapperService.getMapper().map(product, ListProductsResponse.class))
                .toList();
        return new SliceResponseDTO<>(
                content,
                productSlice.hasNext(),
                productSlice.getNumber(),
                productSlice.getSize(),
                productSlice.getNumberOfElements(),
                productSlice.isFirst(),
                productSlice.isLast()
        );
    }

    /**
     * Kullanıcı arayüzünde ürünlerin sayfalı olarak gösterilmesi için gerekli olan veri akışını yönetir.
     * Önbellekten gelen ham veriyi, Spring Data standartlarına uygun bir yapıya dönüştürerek sistemin genel çıktı standartlarına uyumluluk sağlar.
     */
    @Override
    public DataResult<Slice<ListProductsResponse>> getAllWithPagination(Pageable pageable, Long categoryId, Long supplierId, String nameSearch) {
        SliceResponseDTO<ListProductsResponse> cachedData = self.fetchProductSliceData(pageable, categoryId, supplierId, nameSearch);
        Slice<ListProductsResponse> responseSlice = new SliceImpl<>(cachedData.getContent(), pageable, cachedData.isHasNext());
        return new SuccessDataResult<>(responseSlice, messageService.getMessage(
                Messages.Product.PRODUCTS_SUCCESSFULLY_LISTED));
    }

    /**
     * Sisteme yeni bir ana ürün tanımlar ve bu ürüne bağlı varyantları, kategorileri ve görselleri atomik bir işlemle kaydeder.
     * Kayıt sırasında satıcı kimliği otomatik olarak atanır, vergi oranları sistem varsayılanlarına göre belirlenir ve işlem başarıyla tamamlandığında ilgili tüm ürün listesi önbellekleri temizlenerek yeni ürünün vitrinde anında görünmesi sağlanır.
     *
     * @param addProductRequest Ürün ismi, kategoriler ve varyant detaylarını barındıran kapsamlı talep nesnesidir.
     * @return Yeni oluşturulan ürünün profilini başarı mesajıyla birlikte döner.
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "product_lists", allEntries = true),
            @CacheEvict(value = "product_exists_by_category", allEntries = true)
    })
    public DataResult<AddProductResponse> add(AddProductRequest addProductRequest) {
        Supplier supplier = this.supplierService.getAuthenticatedSupplierAsEntity();
        checkIfProductWithNameExists(addProductRequest.getName());
        List<Category> categories = this.categoryService.getByIdsAsEntity(addProductRequest.getCategoryIds());

        Product product = this.modelMapperService.getMapper().map(addProductRequest, Product.class);
        product.setId(null);
        product.setSupplier(supplier);
        product.setVatRate(defaultVatRate);
        Product savedProduct = productRepository.save(product);

        saveProductCategories(savedProduct, categories);
        saveProductItems(savedProduct, addProductRequest.getItems());
        saveProductImages(savedProduct, addProductRequest.getImages());

        Product finalProduct = productRepository.findByIdWithDetails(savedProduct.getId())
                .orElseThrow(() -> new IllegalStateException(messageService.getMessageWithParams(
                        Messages.Product.PRODUCT_DOES_NOT_EXIST_WITH_GIVEN_ID, savedProduct.getId())));

        AddProductResponse response = modelMapperService.getMapper().map(finalProduct, AddProductResponse.class);

        return new SuccessDataResult<>(response, messageService.getMessage(
                Messages.Product.PRODUCT_SUCCESSFULLY_ADDED));
    }

    /**
     * Mevcut bir ürünün temel bilgilerini ve varyantlar ile görseller gibi tüm alt kırılımlarını güncelleyerek veritabanı ile önbellek arasındaki senkronizasyonu sağlar.
     * Güvenlik protokolü gereği sadece ürünün sahibi olan satıcının güncelleme yapmasına izin verilir; eski varyant ve kategori ilişkileri temizlenerek yeni veri setiyle güncel bir profil oluşturulur.
     *
     * @param id Güncellenecek ürünün kimliğidir.
     * @param updateProductRequest Ürünün yeni niteliklerini barındıran nesnedir.
     * @return Güncellenmiş ürünün son halini döner.
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "product_lists", allEntries = true),
            @CacheEvict(value = "product", key = "#id"),
            @CacheEvict(value = "product_item", allEntries = true),
            @CacheEvict(value = "product_exists_by_category", allEntries = true)
    })
    public DataResult<ProductDetailResponse> update(Long id, UpdateProductRequest updateProductRequest) {
        Product productToUpdate = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(messageService.getMessageWithParams(
                        Messages.Product.PRODUCT_DOES_NOT_EXIST_WITH_GIVEN_ID, id)));

        checkIfProductBelongsToAuthenticatedUser(productToUpdate);
        Supplier authenticatedSupplier = this.supplierService.getAuthenticatedSupplierAsEntity();
        checkIfProductWithNameExistsForUpdate(updateProductRequest.getName(), id);

        List<Category> categories = categoryService.getByIdsAsEntity(updateProductRequest.getCategoryIds());
        modelMapperService.getMapper().map(updateProductRequest, productToUpdate);
        productToUpdate.setSupplier(authenticatedSupplier);

        productCategoryRepository.deleteByProductId(id);
        productItemRepository.deleteByProductId(id);
        productImageRepository.deleteByProductId(id);

        Product updatedProduct = productRepository.save(productToUpdate);

        saveProductCategories(updatedProduct, categories);
        saveProductItems(updatedProduct, updateProductRequest.getItems());
        saveProductImages(updatedProduct, updateProductRequest.getImages());

        ProductDetailResponse response = modelMapperService.getMapper().map(updatedProduct, ProductDetailResponse.class);

        return new SuccessDataResult<>(response, messageService.getMessage(
                Messages.Product.PRODUCT_SUCCESSFULLY_UPDATED));
    }

    /**
     * Belirtilen ürünü pasif duruma getirerek satıştan kaldıran ve veritabanı kayıtlarını koruyan bir silme işlemi yürütür.
     * İşlem öncesinde yetki kontrolü yapılır ve silinen ürünün artık arama sonuçlarında yer almaması için ilgili tüm önbellek alanları temizlenir.
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "product_lists", allEntries = true),
            @CacheEvict(value = "product", key = "#id"),
            @CacheEvict(value = "product_item", allEntries = true),
            @CacheEvict(value = "product_exists_by_category", allEntries = true)
    })
    public Result delete(Long id) {
        Product productToDelete = this.productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(messageService.getMessageWithParams(
                        Messages.Product.PRODUCT_DOES_NOT_EXIST_WITH_GIVEN_ID, id)));

        checkIfProductBelongsToAuthenticatedUser(productToDelete);

        productToDelete.setIsActive(false);
        this.productRepository.save(productToDelete);

        return new SuccessResult(messageService.getMessage(
                Messages.Product.PRODUCT_SUCCESSFULLY_DELETED));
    }

    /**
     * Belirli bir ürün varyantını kimlik numarasına göre sorgulayarak getirir.
     * Bu sorgu sonuçları, ürün kalemleri üzerinden yapılan stok kontrolleri ve sipariş eşleştirmeleri gibi operasyonlarda performans kazanmak adına Redis üzerinde saklanır.
     */
    @Override
    @Cacheable(value = "product_item", key = "#id")
    public ProductItem getProductItemById(Long id) {
        return this.productItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(messageService.getMessageWithParams(
                        Messages.Product.PRODUCT_ITEM_NOT_FOUND, id)));
    }

    /**
     * Belirli bir kategori altında aktif satışta olan herhangi bir ürünün bulunup bulunmadığını kontrol eder.
     * Bu kontrol, özellikle kategorilerin silinmesi veya pasifleştirilmesi süreçlerinde veri bütünlüğünü korumak ve kategori-ürün ilişkisini doğrulamak için kullanılır; sonuçlar hız kazanmak adına kategori bazlı önbelleğe alınır.
     */
    @Override
    @Cacheable(value = "product_exists_by_category", key = "#categoryId")
    public boolean existsByCategoryId(Long categoryId) {
        return this.productRepository.existsByProductCategories_CategoryId(categoryId);
    }

    /**
     * Belirli bir ürün varyantının stok miktarını, talep edilen adet kadar güvenli bir şekilde düşürür.
     * Bu işlem özellikle sipariş onay süreçlerinde envanter tutarlılığını sağlamak için kullanılır; stok yetersizliği durumunda işlemi iptal ederek ticari hataların önüne geçer.
     *
     * @param productItemId Stoğu düşürülecek varyantın kimliğidir.
     * @param quantity Düşürülecek miktardır.
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "product_item", key = "#productItemId"),
            @CacheEvict(value = "product_lists", allEntries = true)
    })
    public void checkAndReduceStock(Long productItemId, int quantity) {
        ProductItem productItem = this.productItemRepository.findById(productItemId)
                .orElseThrow(() -> new NotFoundException(messageService.getMessageWithParams(
                        Messages.Product.PRODUCT_ITEM_NOT_FOUND, productItemId)));

        checkStockAvailability(productItem, quantity);

        productItem.setQuantityInStock(productItem.getQuantityInStock() - quantity);
        this.productItemRepository.save(productItem);
    }

    /**
     * İptal edilen siparişlerdeki veya iade süreçlerindeki ürün miktarlarını envantere geri kazandırır.
     * Negatif miktar girişine karşı korumalıdır ve güncel stok bilgisinin sistem genelinde anında yansıması için ilgili önbellekleri temizler.
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "product_item", key = "#productItemId"),
            @CacheEvict(value = "product_lists", allEntries = true)
    })
    public void increaseStock(Long productItemId, int quantity) {
        ProductItem productItem = this.productItemRepository.findById(productItemId)
                .orElseThrow(() -> new NotFoundException(messageService.getMessageWithParams(
                        Messages.Product.PRODUCT_ITEM_NOT_FOUND, productItemId)));

        checkPositiveQuantity(quantity);

        productItem.setQuantityInStock(productItem.getQuantityInStock() + quantity);
        this.productItemRepository.save(productItem);
    }


    // --- YARDIMCI METOTLAR ---


    /**
     * Yeni bir ürünün ismiyle sistemde daha önce kayıt edilip edilmediğini kontrol eder.
     * Veri bütünlüğünü sağlamak adına, mükerrer ürün isimlerinin oluşturulmasını engelleyerek arama ve listeleme sonuçlarında karmaşa oluşmasını önler.
     */
    private void checkIfProductWithNameExists(String name) {
        if (this.productRepository.existsByName(name)) {
            throw new BusinessException(messageService.getMessage(
                    Messages.Product.PRODUCT_ALREADY_EXIST));
        }
    }

    /**
     * Ürünün çoklu kategori hiyerarşisine dahil edilmesini sağlar ve bu ilişkileri atomik olarak kaydeder.
     */
    private void saveProductCategories(Product product, List<Category> categories) {
        List<ProductCategory> productCategories = categories.stream().map(category -> {
            ProductCategoryId productCategoryId = new ProductCategoryId(product.getId(), category.getId());
            ProductCategory productCategory = new ProductCategory();
            productCategory.setId(productCategoryId);
            productCategory.setProduct(product);
            productCategory.setCategory(category);
            return productCategory;
        }).toList();

        this.productCategoryRepository.saveAll(productCategories);
        product.setProductCategories(new HashSet<>(productCategories));
    }

    /**
     * Bir ürüne bağlı renk ve beden gibi varyantları, ilgili tanımlayıcı verilerle eşleştirerek toplu bir şekilde sisteme işler.
     */
    private void saveProductItems(Product product, List<AddProductItemRequest> items) {
        List<ProductItem> productItems = items.stream().map(itemRequest -> {
            Colour colour = this.colourService.getByIdAsEntity(itemRequest.getColourId());
            Size size = this.sizeService.getByIdAsEntity(itemRequest.getSizeId());

            ProductItem productItem = this.modelMapperService.getMapper().map(itemRequest, ProductItem.class);
            productItem.setId(null);
            productItem.setProduct(product);
            productItem.setColour(colour);
            productItem.setSize(size);
            return productItem;
        }).toList();

        this.productItemRepository.saveAll(productItems);
        product.setProductItems(new HashSet<>(productItems));
    }

    /**
     * Ürünün çoklu görsellerini kaydederken, sistemde sadece bir adet ana görsel bulunmasını garanti eden mantıksal denetimi yürütür.
     */
    private void saveProductImages(Product product, List<AddProductImageRequest> images) {
        if (images == null || images.isEmpty()) {
            return;
        }

        long mainImageCount = images.stream().filter(AddProductImageRequest::isMainImage).count();
        if (mainImageCount > 1) {
            throw new BusinessException(messageService.getMessage(
                    Messages.Product.CANNOT_HAVE_MULTIPLE_MAIN_IMAGES));
        }

        List<ProductImage> productImages = images.stream().map(imageRequest -> {
            ProductImage productImage = this.modelMapperService.getMapper().map(imageRequest, ProductImage.class);
            productImage.setId(null);
            productImage.setProduct(product);
            return productImage;
        }).toList();

        this.productImageRepository.saveAll(productImages);
        product.setProductImages(new HashSet<>(productImages));
    }

    /**
     * Mevcut bir ürün güncellenirken, yeni ismin başka bir üründe halihazırda kullanımda olup olmadığını denetler.
     */
    private void checkIfProductWithNameExistsForUpdate(String name, Long id) {
        productRepository.findByNameAndIdNot(name, id).ifPresent(product -> {
            throw new BusinessException(messageService.getMessage(
                    Messages.Product.PRODUCT_ALREADY_EXIST));
        });
    }

    /**
     * Talep edilen ürün varyantının mevcut stok seviyesinin, karşılanmak istenen sipariş miktarını karşılayıp karşılamadığını analiz eder.
     */
    private void checkStockAvailability(ProductItem productItem, int quantity) {
        if (productItem.getQuantityInStock() < quantity) {
            throw new BusinessException(messageService.getMessageWithParams(
                    Messages.Product.INSUFFICIENT_STOCK, productItem.getProduct().getName()));
        }
    }

    /**
     * Envantere ürün iadesi veya girişi yapılırken, miktarın sıfırdan büyük bir tam sayı olduğunu doğrular.
     */
    private void checkPositiveQuantity(int quantity) {
        if (quantity <= 0) {
            throw new BusinessException(messageService.getMessage(
                    Messages.Product.QUANTITY_MUST_BE_POSITIVE));
        }
    }

    /**
     * İşlem yapan satıcının, üzerinde değişiklik yapmaya çalıştığı ürünün gerçek sahibi olup olmadığını denetleyen güvenlik bariyeridir.
     */
    private void checkIfProductBelongsToAuthenticatedUser(Product product) {
        Long authenticatedUserId = cacheHelper.getAuthenticatedUserId();
        if (!product.getCUser().equals(authenticatedUserId)) {
            throw new AuthorizationBusinessException(messageService.getMessage(
                    Messages.Auth.AUTHORIZATION_FAILED));
        }
    }

}
