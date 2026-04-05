package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.business.abstracts.CategoryService;
import com.project.ecommerce_backend.business.abstracts.ProductService;
import com.project.ecommerce_backend.business.dtos.requests.category.AddCategoryRequest;
import com.project.ecommerce_backend.business.dtos.requests.category.UpdateCategoryRequest;
import com.project.ecommerce_backend.business.dtos.responses.category.*;
import com.project.ecommerce_backend.business.dtos.responses.common.SliceResponseDTO;
import com.project.ecommerce_backend.core.constants.Messages;
import com.project.ecommerce_backend.core.exceptions.types.BusinessException;
import com.project.ecommerce_backend.core.exceptions.types.NotFoundException;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.core.utils.mapper.ModelMapperService;
import com.project.ecommerce_backend.core.utils.result.*;
import com.project.ecommerce_backend.entities.concretes.Category;
import com.project.ecommerce_backend.repositories.abstracts.CategoryRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Sistemdeki ürün hiyerarşisinin mimarı olan, kategorilerin yaşam döngüsünü ve birbirleriyle olan ebeveyn-çocuk ilişkilerini yöneten merkezi iş mantığı servisidir.
 * Bu sınıf; kategorilerin ağaç yapısında düzenlenmesi, veritabanı performansını artırmak amacıyla sorgu sonuçlarının Redis üzerinde stratejik olarak önbelleğe alınması, hiyerarşik döngülerin (circular dependency) engellenmesi ve kategorilere bağlı alt birimlerin (ürün veya alt kategori) veri bütünlüğünün korunması süreçlerinden sorumludur.
 */
@Service
public class CategoryManager implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryService self;
    private final ProductService productService;
    private final ModelMapperService modelMapperService;
    private final MessageService messageService;

    public CategoryManager(CategoryRepository categoryRepository,
                           @Lazy CategoryService self,
                           ModelMapperService modelMapperService,
                           MessageService messageService,
                           @Lazy ProductService productService) {
        this.categoryRepository = categoryRepository;
        this.self = self;
        this.modelMapperService = modelMapperService;
        this.messageService = messageService;
        this.productService = productService;
    }

    /**
     * Belirtilen kimlik numarasına sahip kategorinin tüm teknik ve hiyerarşik detaylarını, ilişkili verileriyle birlikte analiz ederek sunar.
     * Sorgulama aşamasında kategorinin mevcut durumu kontrol edildikten sonra, elde edilen detay verisi performans kazanımı sağlamak amacıyla doğrudan kategori ID'si üzerinden Redis hafızasına işlenir.
     *
     * @param id Bilgileri görüntülenmek istenen kategorinin veritabanındaki eşsiz anahtarıdır.
     * @return Kategorinin detaylı profilini içeren CategoryDetailResponse nesnesini başarı geri bildirimiyle döner.
     * @throws NotFoundException İlgili ID ile eşleşen bir kategori kaydı bulunamadığında fırlatılır.
     */
    @Override
    @Cacheable(value = "category", key = "#id")
    public DataResult<CategoryDetailResponse> getById(Long id) {
        Category category = findCategoryByIdWithDetails(id);
        CategoryDetailResponse response = this.modelMapperService.getMapper().map(category, CategoryDetailResponse.class);

        return new SuccessDataResult<>(response, messageService.getMessage(
                Messages.Category.CATEGORY_DETAIL_SUCCESSFULLY_LISTED));
    }

    /**
     * Kategorilerin listelenmesi sürecinde kullanılan ham veriyi, Redis üzerinde sayfa numarası ve sıralama kriterlerine göre optimize edilmiş bir anahtar yapısıyla sorgular.
     * Bu metot, genellikle proxy üzerinden (self-invocation) çağrılarak önbellek mekanizmasının sayfalı listelerde de etkin çalışmasını sağlar; böylece kullanıcı her sayfa değiştirdiğinde veritabanına gitmek yerine doğrudan bellekten hızlı yanıt alır.
     *
     * @param pageable Sayfa indeksi ve boyutunu içeren standart Spring Data konfigürasyonudur.
     * @return Sayfalanmış kategori verisini barındıran teknik bir veri transfer nesnesi (SliceResponseDTO) döner.
     */
    @Override
    @Cacheable(value = "categories_lists",
            key = "#pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort.toString()")
    public SliceResponseDTO<ListCategoryResponse> fetchCategorySliceData(Pageable pageable) {
        Slice<ListCategoryResponse> slice = this.categoryRepository.getAllWithPagination(pageable);

        return new SliceResponseDTO<>(
                slice.getContent(),
                slice.hasNext(),
                slice.getNumber(),
                slice.getSize(),
                slice.getNumberOfElements(),
                slice.isFirst(),
                slice.isLast()
        );
    }

    /**
     * Kullanıcı arayüzlerinde kategorilerin sayfalı bir şekilde listelenmesi için gerekli olan veri akışını koordine eder.
     * Önbellekten gelen ham veriyi (SliceResponseDTO), Spring Data'nın SliceImpl yapısına geri dönüştürerek sistemin genel standartlarına uyumlu hale getirir ve kullanıcının önüne pürüzsüz bir liste olarak sunar.
     *
     * @param pageable Liste görünümü için gerekli sayfalama ve sıralama parametrelerini barındırır.
     * @return Başarıyla paketlenmiş sayfalı kategori listesini döner.
     */
    @Override
    public DataResult<Slice<ListCategoryResponse>> getAllWithPagination(Pageable pageable) {
        SliceResponseDTO<ListCategoryResponse> dto = self.fetchCategorySliceData(pageable);
        Slice<ListCategoryResponse> categories = new SliceImpl<>(dto.getContent(), pageable, dto.isHasNext());

        return new SuccessDataResult<>(categories, messageService.getMessage(
                Messages.Category.CATEGORIES_SUCCESSFULLY_LISTED));
    }

    /**
     * Mağazanın kategori yapısını, ana kategorilerden alt kategorilere doğru uzanan hiyerarşik bir ağaç (Tree) formunda inşa eder.
     * Önce tüm düz liste veritabanından çekilir, ardından bellek üzerinde ebeveyn-çocuk ilişkileri kurularak karmaşık bir ağaç yapısı oluşturulur; bu işlem işlemci yoğunluklu olduğu için "all" anahtarı ile Redis üzerinde tek bir parça olarak saklanır ve kategori yapısı değişene kadar oradan servis edilir.
     *
     * @return En üst seviyeden başlayarak tüm kategori hiyerarşisini içeren kök düğümleri (Root Nodes) listesi döner.
     */
    @Override
    @Cacheable(value = "categories_tree_lists", key = "'all'")
    public DataResult<List<CategoryTreeResponse>> getAllAsTree() {
        List<ListCategoryResponse> flatList = this.categoryRepository.getAll();
        List<CategoryTreeResponse> tree = buildCategoryTree(flatList);

        return new SuccessDataResult<>(tree, messageService.getMessage(
                Messages.Category.CATEGORY_SUCCESSFULLY_LISTED_AS_TREE));
    }

    /**
     * Verilen kimlik numarası listesine karşılık gelen tüm kategorileri veritabanından toplu halde (Bulk) entity formatında getirir.
     * Genellikle ürün ekleme veya güncelleme süreçlerinde birden fazla kategori eşleşmesi yapılması gerektiğinde kullanılır; talep edilen ID'lerden herhangi biri sistemde mevcut değilse veri bütünlüğünü korumak adına toplu bir hata fırlatılır.
     * * @param ids Sorgulanacak kategorilerin benzersiz kimlik numaralarından oluşan liste.
     * @return Veritabanında bulunan ve doğrulanan Category entity listesini döner.
     * @throws NotFoundException Talep edilen ID listesi ile bulunan kayıt sayısı eşleşmediğinde tetiklenir.
     */
    @Override
    public List<Category> getByIdsAsEntity(List<Long> ids) {
        List<Category> categories = this.categoryRepository.findAllById(ids);

        checkIfAllCategoriesExist(categories, ids);

        return categories;
    }

    /**
     * Sisteme yeni bir kategori tanımlar ve kategori hiyerarşisine doğru noktadan eklenmesini sağlar.
     * Kayıt öncesinde isim çakışması kontrolü yapılarak veri tekilliği korunur; eğer bir ebeveyn kategori belirtilmişse bu ilişki doğrulanır ve yeni kayıtla birlikte sistemdeki tüm kategori listesi ve ağaç yapısı önbellekleri (Cache Evict) temizlenerek verinin tazelenmesi garanti altına alınır.
     *
     * @param addCategoryRequest Yeni kategorinin ismini ve varsa üst kategori ID'sini barındıran talep nesnesidir.
     * @return Yeni oluşturulan kategorinin temel bilgilerini başarı mesajıyla birlikte döner.
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "categories_lists", allEntries = true),
            @CacheEvict(value = "categories_tree_lists", allEntries = true)
    })
    public DataResult<AddCategoryResponse> add(AddCategoryRequest addCategoryRequest) {

        checkIfCategoryWithGivenNameExists(addCategoryRequest.getName());

        Category category = this.modelMapperService.getMapper().map(addCategoryRequest, Category.class);
        category.setId(null);

        Category parentCategory = getParentCategoryById(addCategoryRequest.getParentCategoryId());
        category.setParentCategory(parentCategory);
        Category savedCategory = this.categoryRepository.save(category);

        AddCategoryResponse response = this.modelMapperService.getMapper().map(savedCategory, AddCategoryResponse.class);

        return new SuccessDataResult<>(response, messageService.getMessage(
                Messages.Category.CATEGORY_SUCCESSFULLY_ADDED));
    }

    /**
     * Mevcut bir kategorinin bilgilerini revize eder ve hiyerarşik konumunu yeniden düzenler.
     * Güncelleme sırasında en kritik adım olan "Döngüsel Bağımlılık" (Circular Dependency) kontrolü yapılarak, bir kategorinin kendisini veya kendi alt kategorilerinden birini ebeveyn olarak seçmesi engellenir; işlem sonunda ilgili kategorinin detayı ve tüm genel listeler önbellekten temizlenir.
     *
     * @param updateCategoryRequest Güncellenecek kategorinin ID'sini ve yeni niteliklerini barındıran nesnedir.
     * @return Güncellenmiş kategori bilgilerini, ilişkileri doğrulanmış şekilde döner.
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "category", key = "#updateCategoryRequest.id"),
            @CacheEvict(value = "categories_lists", allEntries = true),
            @CacheEvict(value = "categories_tree_lists", allEntries = true)
    })
    public DataResult<UpdateCategoryResponse> update(UpdateCategoryRequest updateCategoryRequest) {

        Category categoryToUpdate = findCategoryByIdWithDetails(updateCategoryRequest.getId());

        checkIfCategoryNameExistsForUpdate(updateCategoryRequest.getName(), updateCategoryRequest.getId());

        Category parentCategory = getParentCategoryById(updateCategoryRequest.getParentCategoryId());
        checkIfParentingCreatesCircularDependency(categoryToUpdate, parentCategory);

        this.modelMapperService.getMapper().map(updateCategoryRequest, categoryToUpdate);
        categoryToUpdate.setParentCategory(parentCategory);
        Category savedCategory = this.categoryRepository.save(categoryToUpdate);

        Category finalCategory = findCategoryByIdWithDetails(savedCategory.getId());

        UpdateCategoryResponse response = this.modelMapperService.getMapper().map(finalCategory, UpdateCategoryResponse.class);

        response.setParentCategoryId(Optional.ofNullable(finalCategory.getParentCategory())
                .map(Category::getId)
                .orElse(null));

        return new SuccessDataResult<>(response, messageService.getMessage(
                Messages.Category.CATEGORY_SUCCESSFULLY_UPDATED));
    }

    /**
     * Belirtilen kategoriyi pasif duruma getirerek sistemden (mantıksal olarak) kaldırır.
     * Güvenlik ve veri bütünlüğü gereği, eğer kategorinin altında aktif alt kategoriler veya bu kategoriye bağlı ürünler mevcutsa silme işlemi engellenir; başarılı silme işlemi sonrasında sistemdeki tüm kategori tabanlı önbellekler geçersiz kılınır.
     *
     * @param id Sistemden kaldırılacak olan kategorinin kimlik numarasıdır.
     * @return Silme işleminin başarı durumunu veya neden gerçekleştirilemediğini anlatan sonuç nesnesini döner.
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "category", key = "#id"),
            @CacheEvict(value = "categories_lists", allEntries = true),
            @CacheEvict(value = "categories_tree_lists", allEntries = true)
    })
    public Result delete(Long id) {

        Category categoryToDelete = findCategoryById(id);

        checkIfCategoryHasSubCategories(id);
        checkIfCategoryHasProducts(id);

        categoryToDelete.setIsActive(false);
        this.categoryRepository.save(categoryToDelete);

        return new SuccessResult(messageService.getMessage(
                Messages.Category.CATEGORY_SUCCESSFULLY_DELETED));
    }

    // --- YARDIMCI METOTLAR ---

    /**
     * Veritabanından gelen düz (flat) kategori verilerini, bellekte eşleştirerek iç içe geçmiş hiyerarşik bir ağaç yapısına (Category Tree) dönüştüren algoritmadır.
     * Bu süreç, kullanıcı arayüzlerinde (menüler veya filtreler) kategorilerin görsel olarak parent-child ilişkisiyle gösterilmesini sağlar.
     */
    private List<CategoryTreeResponse> buildCategoryTree(List<ListCategoryResponse> flatList) {
        Map<Long, CategoryTreeResponse> map = flatList.stream()
                .map(cat -> new CategoryTreeResponse(cat.getId(), cat.getName(), cat.getParentCategoryId(), new ArrayList<>()))
                .collect(Collectors.toMap(CategoryTreeResponse::getId, c -> c));

        List<CategoryTreeResponse> rootNodes = new ArrayList<>();

        map.values().forEach(node -> {
            Long parentId = node.getParentCategoryId();
            if (parentId == null) {
                rootNodes.add(node);
            } else {
                CategoryTreeResponse parent = map.get(parentId);
                if (parent != null) {
                    parent.getChildren().add(node);
                }
            }
        });
        return rootNodes;
    }

    /**
     * Belirtilen kimlik numarasının geçerli bir üst kategori (Parent) olup olmadığını kontrol eder.
     * Eğer bir üst kategori ID'si verilmişse ilgili kaydı doğrular; aksi takdirde kategorinin "Kök Kategori" (Root) olduğunu varsayarak null döner.
     */
    private Category getParentCategoryById(Long parentCategoryId) {
        if (parentCategoryId == null) {
            return null;
        }
        return findCategoryById(parentCategoryId);
    }

    /**
     * Toplu kategori sorgulamalarında, istenen tüm kayıtların veritabanında karşılığı olup olmadığını denetler.
     * Bu kontrol, sistemin "ya hep ya hiç" prensibiyle çalışmasını sağlayarak eksik veriyle işlem yapılmasının önüne geçer.
     */
    private void checkIfAllCategoriesExist(List<Category> foundCategories, List<Long> requestedIds) {
        if (foundCategories.size() != requestedIds.size()) {
            throw new NotFoundException(messageService.getMessage(
                    Messages.Category.SOME_CATEGORIES_DOES_NOT_EXIST_WITH_GIVEN_IDS));
        }
    }

    /**
     * Yeni bir kategori oluşturulurken, sistemde aynı isimle kayıtlı başka bir aktif kategorinin olup olmadığını denetler.
     * Bu kontrol, kullanıcı tarafında oluşabilecek karmaşıklığı önlemek ve SEO uyumlu benzersiz isimler oluşturmak için kritik bir iş kuralıdır.
     */
    private void checkIfCategoryWithGivenNameExists(String categoryName) {
        if (this.categoryRepository.existsByName(categoryName)) {
            throw new BusinessException(messageService.getMessage(
                    Messages.Category.CATEGORY_ALREADY_EXIST));
        }
    }

    /**
     * Güncelleme işlemleri sırasında, belirlenen yeni ismin başka bir kategori (kendi ID'si hariç) tarafından kullanılıp kullanılmadığını doğrular.
     */
    private void checkIfCategoryNameExistsForUpdate(String name, Long id) {
        if (this.categoryRepository.existsByNameAndIdNot(name, id)) {
            throw new BusinessException(messageService.getMessage(
                    Messages.Category.CATEGORY_ALREADY_EXIST));
        }
    }

    /**
     * Veri bütünlüğü protokolü gereği, bir kategorinin silinmeden önce altında aktif alt kategoriler barındırıp barındırmadığını denetler.
     */
    private void checkIfCategoryHasSubCategories(Long id) {
        if (this.categoryRepository.existsByParentCategoryId(id)) {
            throw new BusinessException(messageService.getMessage(
                    Messages.Category.CATEGORY_ERROR_HAS_SUB_CATEGORIES));
        }
    }

    /**
     * Ticari veri bütünlüğü kapsamında, bir kategorinin ürünlerle ilişkili olup olmadığını kontrol eder; ürün bağımlılığı olan kategorilerin silinmesi "Orphan Product" (sahipsiz ürün) oluşmasına neden olacağı için engellenir.
     */
    private void checkIfCategoryHasProducts(Long id) {
        if (this.productService.existsByCategoryId(id)) {
            throw new BusinessException(messageService.getMessage(
                    Messages.Category.CATEGORY_ERROR_HAS_PRODUCTS));
        }
    }

    /**
     * Kategori hiyerarşisinde mantıksal bir döngü oluşup oluşmadığını (Örn: Bir kategorinin kendi kendisinin çocuğu olması) derinlemesine kontrol ederek sistemin sonsuz döngüye girmesini önler.
     */
    private void checkIfParentingCreatesCircularDependency(Category child, Category potentialParent) {
        if (potentialParent == null) {
            return;
        }
        if (child.getId().equals(potentialParent.getId())) {
            throw new BusinessException(messageService.getMessage(
                    Messages.Category.CATEGORY_ERROR_CANNOT_BE_ITS_OWN_PARENT));
        }
        Category current = potentialParent;
        while (current != null) {
            if (current.getId().equals(child.getId())) {
                throw new BusinessException(messageService.getMessage(
                        Messages.Category.CATEGORY_ERROR_CIRCULAR_DEPENDENCY));
            }
            current = current.getParentCategory();
        }
    }

    /**
     * Veritabanı sorgularında "Don't Repeat Yourself" (DRY) prensibiyle çalışan, temel kategori arama ve "bulunamadı" istisnası fırlatma mekanizmasıdır.
     */
    private Category findCategoryById(Long id) {
        return this.categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(messageService.getMessageWithParams(
                        Messages.Category.CATEGORY_DOES_NOT_EXIST_WITH_GIVEN_ID, id)));
    }

    /**
     * Kategoriyi, veritabanından tüm detayları ve ilişkili (Lazy/Eager) alanlarıyla birlikte en geniş haliyle sorgular.
     * Özellikle güncelleme veya detay görüntüleme gibi, nesnenin tüm niteliklerine ihtiyaç duyulan senaryolarda tercih edilen derinlemesine bir sorgulama metodudur.
     */
    private Category findCategoryByIdWithDetails(Long id) {
        return this.categoryRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new NotFoundException(messageService.getMessageWithParams(
                        Messages.Category.CATEGORY_DOES_NOT_EXIST_WITH_GIVEN_ID, id)));
    }
}