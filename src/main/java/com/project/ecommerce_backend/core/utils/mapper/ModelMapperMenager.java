package com.project.ecommerce_backend.core.utils.mapper;

import com.project.ecommerce_backend.business.dtos.responses.address.AddressDetailResponse;
import com.project.ecommerce_backend.business.dtos.responses.address.ListAddressResponse;
import com.project.ecommerce_backend.business.dtos.responses.cart.CartItemResponse;
import com.project.ecommerce_backend.business.dtos.responses.cart.CartResponse;
import com.project.ecommerce_backend.business.dtos.responses.category.AddCategoryResponse;
import com.project.ecommerce_backend.business.dtos.responses.category.CategoryDetailResponse;
import com.project.ecommerce_backend.business.dtos.responses.category.UpdateCategoryResponse;
import com.project.ecommerce_backend.business.dtos.responses.common.CategoryInfoResponse;
import com.project.ecommerce_backend.business.dtos.responses.common.ProductImageInfoResponse;
import com.project.ecommerce_backend.business.dtos.responses.common.SupplierInfoResponse;
import com.project.ecommerce_backend.business.dtos.responses.invoice.ListUserInvoicesResponse;
import com.project.ecommerce_backend.business.dtos.responses.invoice.UserInvoiceDetailResponse;
import com.project.ecommerce_backend.business.dtos.responses.order.*;
import com.project.ecommerce_backend.business.dtos.responses.payment.ListPaymentMethodResponse;
import com.project.ecommerce_backend.business.dtos.responses.payment.PaymentMethodDetailResponse;
import com.project.ecommerce_backend.business.dtos.responses.product.AddProductResponse;
import com.project.ecommerce_backend.business.dtos.responses.product.ListProductsResponse;
import com.project.ecommerce_backend.business.dtos.responses.product.ProductDetailResponse;
import com.project.ecommerce_backend.business.dtos.responses.product.ProductItemInfo;
import com.project.ecommerce_backend.business.dtos.responses.return_request.ReturnRequestResponse;
import com.project.ecommerce_backend.core.constants.Messages;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.entities.concretes.*;
import com.project.ecommerce_backend.repositories.abstracts.CustomerAddressRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Uygulamanın farklı katmanları arasındaki veri aktarım süreçlerini otomatize eden ve nesne dönüşüm kurallarını merkezi olarak yöneten servis yapılandırmasıdır.
 * Basit alan eşleştirmelerinin ötesine geçerek; kategori ağacı oluşturma, ürün görseli seçimi ve karmaşık sipariş detaylarının yapılandırılması gibi iş mantığı gerektiren dönüşüm senaryolarını özel dönüştürücüler (Converter) aracılığıyla yürütür. Bu sayede uygulama genelinde tutarlı ve hata toleranslı bir veri dönüşüm altyapısı sunar.
 */
@Service
@RequiredArgsConstructor
public class ModelMapperMenager implements ModelMapperService {

    private final ModelMapper modelMapper;
    private final MessageService messageService;
    private final CustomerAddressRepository customerAddressRepository;
    private static final String MASKED_CARD_PREFIX = "**** **** **** ";

    /**
     * Servis ayağa kalktıktan sonra tüm bağımlılıkların hazır olmasını takiben çalışan yapılandırma metodudur.
     * Eşleştirme stratejisini "STRICT" (Sıkı) olarak belirleyerek veri güvenliğini sağlar ve belirsiz alan eşleşmelerinden kaynaklanabilecek hataları önler. Ardından kategori, ürün, sepet, adres ve sipariş gibi tüm modüllerin özel eşleştirme kurallarını sisteme dahil eder.
     */
    @PostConstruct
    public void init() {
        this.modelMapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT)
                .setAmbiguityIgnored(true);

        configureCategoryMappings();
        configureProductMappings();
        configureShoppingCartMappings();
        configureAddressMappings();
        configurePaymentMethodMappings();
        configureOrderMappings();
        configureInvoiceMappings();
        configureReturnRequestMappings();
    }

    /**
     * Kategori hiyerarşisini ve kullanıcı deneyimi için kritik olan 'Breadcrumb' (kategori yolu) verisini dinamik olarak oluşturan dönüşüm mantığıdır.
     * Bir kategoriden yola çıkarak üst ebeveynlere doğru recursive (özyinelemeli) bir tarama yapar ve istemciye kök dizinden mevcut kategoriye kadar olan hiyerarşik yolu bir liste olarak sunar.
     */
    private void configureCategoryMappings() {
        Converter<Category, AddCategoryResponse> categoryToAddResponseConverter = context -> {
            Category source = context.getSource();
            AddCategoryResponse destination = new AddCategoryResponse();
            destination.setId(source.getId());
            destination.setName(source.getName());
            
            if (source.getParentCategory() != null) {
                destination.setParentCategoryId(source.getParentCategory().getId());
                destination.setParentCategoryName(source.getParentCategory().getName());
            }
            
            return destination;
        };

        Converter<Category, UpdateCategoryResponse> categoryToUpdateResponseConverter = context -> {
            Category source = context.getSource();
            UpdateCategoryResponse destination = new UpdateCategoryResponse();
            destination.setId(source.getId());
            destination.setName(source.getName());
            
            if (source.getParentCategory() != null) {
                destination.setParentCategoryId(source.getParentCategory().getId());
            }
            
            return destination;
        };

        modelMapper.addConverter(categoryToAddResponseConverter, Category.class, AddCategoryResponse.class);
        modelMapper.addConverter(categoryToUpdateResponseConverter, Category.class, UpdateCategoryResponse.class);

        Converter<Category, CategoryDetailResponse> categoryToDetailResponseConverter = context -> {
            Category source = context.getSource();
            CategoryDetailResponse destination = new CategoryDetailResponse();

            destination.setId(source.getId());
            destination.setName(source.getName());
            destination.setDescription(source.getDescription());
            destination.setIsActive(source.getIsActive());

            if (source.getCategories() != null) {
                destination.setSubCategories(source.getCategories().stream()
                        .map(subCat -> modelMapper.map(subCat, CategoryInfoResponse.class))
                        .toList());
            } else {
                destination.setSubCategories(new ArrayList<>());
            }

            List<CategoryInfoResponse> breadcrumb = new ArrayList<>();
            Category current = source;
            while (current != null) {
                breadcrumb.add(modelMapper.map(current, CategoryInfoResponse.class));
                current = current.getParentCategory();
            }
            Collections.reverse(breadcrumb);
            destination.setBreadCrumb(breadcrumb);

            return destination;
        };

        modelMapper.addConverter(categoryToDetailResponseConverter, Category.class, CategoryDetailResponse.class);
    }

    /**
     * Ürünleri, kullanıcı arayüzünde gösterilecek formatlara dönüştürmek için gerekli eşleştirme kurallarını tanımlar.
     * Bu metot, bir ürünün ana görselini bulma, en düşük fiyatını hesaplama ve kategori adını alma gibi işlemleri,
     * farklı yanıt DTO'ları için özel olarak ayarlanmış dönüştürücüler aracılığıyla yönetir.
     * Karmaşık mantıklar, daha okunabilir ve yönetilebilir olmaları için yardımcı metotlara bölünmüştür.
     */
    private void configureProductMappings() {
        Converter<Product, ListProductsResponse> productToListProductsResponseConverter = context -> {
            Product source = context.getSource();
            ListProductsResponse destination = new ListProductsResponse();
            destination.setId(source.getId());
            destination.setName(source.getName());
            destination.setMainImageUrl(getMainImageUrl(source.getProductImages()));
            destination.setPrice(getMinPrice(source.getProductItems()));
            destination.setCategoryName(getCategoryName(source.getProductCategories()));
            return destination;
        };

        Converter<Product, ProductDetailResponse> productToDetailResponseConverter = context -> {
            Product source = context.getSource();
            ProductDetailResponse destination = new ProductDetailResponse();
            destination.setId(source.getId());
            destination.setName(source.getName());
            destination.setDescription(source.getDescription());
            destination.setCDate(source.getCDate());
            destination.setUDate(source.getUDate());
            destination.setSupplier(mapSupplierToInfoResponse(source.getSupplier()));
            destination.setCategories(mapCategoriesToInfoResponses(source.getProductCategories()));
            destination.setImages(mapImagesToInfoResponses(source.getProductImages()));
            destination.setItems(mapItemsToInfo(source.getProductItems()));
            return destination;
        };

        Converter<Product, AddProductResponse> productToAddResponseConverter = context -> {
            Product source = context.getSource();
            AddProductResponse destination = new AddProductResponse();
            destination.setId(source.getId());
            destination.setName(source.getName());
            destination.setDescription(source.getDescription());
            destination.setCDate(source.getCDate());
            destination.setSupplier(mapSupplierToInfoResponse(source.getSupplier()));
            destination.setCategories(mapCategoriesToInfoResponses(source.getProductCategories()));
            destination.setItems(mapItemsToInfo(source.getProductItems()));
            return destination;
        };

        modelMapper.addConverter(productToListProductsResponseConverter, Product.class, ListProductsResponse.class);
        modelMapper.addConverter(productToDetailResponseConverter, Product.class, ProductDetailResponse.class);
        modelMapper.addConverter(productToAddResponseConverter, Product.class, AddProductResponse.class);
    }

    /** Bir ürünün görselleri arasından ana görsel olarak işaretlenmiş olanın URL'ini bulur. */
    private String getMainImageUrl(Set<ProductImage> productImages) {
        if (productImages != null) {
            return productImages.stream()
                    .filter(ProductImage::getIsMainImage)
                    .map(ProductImage::getImageUrl)
                    .findFirst().orElse(null);
        }
        return null;
    }

    /** Bir ürünün farklı varyantları arasındaki en düşük birim fiyatı bulur. */
    private BigDecimal getMinPrice(Set<ProductItem> productItems) {
        if (productItems != null && !productItems.isEmpty()) {
            return productItems.stream()
                    .map(ProductItem::getUnitPrice)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
        }
        return BigDecimal.ZERO;
    }

    /** Bir ürünün ait olduğu ilk kategorinin adını getirir. */
    private String getCategoryName(Set<ProductCategory> productCategories) {
        if (productCategories != null && !productCategories.isEmpty()) {
            return productCategories.stream()
                    .map(pc -> pc.getCategory().getName())
                    .findFirst().orElse(messageService.getMessage(Messages.Category.CATEGORY_NOT_FOUND));
        }
        return messageService.getMessage(Messages.Category.CATEGORY_NOT_FOUND);
    }

    /** Tedarikçi entity'sini, daha basit bir bilgi nesnesi olan SupplierInfoResponse'a dönüştürür. */
    private SupplierInfoResponse mapSupplierToInfoResponse(Supplier supplier) {
        if (supplier != null) {
            return modelMapper.map(supplier, SupplierInfoResponse.class);
        }
        return null;
    }

    /** Ürünün kategori ilişkilerini, Kategori bilgi DTO'larına dönüştürür. */
    private List<CategoryInfoResponse> mapCategoriesToInfoResponses(Set<ProductCategory> productCategories) {
        if (productCategories != null) {
            return productCategories.stream()
                    .map(pc -> modelMapper.map(pc.getCategory(), CategoryInfoResponse.class))
                    .toList();
        }
        return Collections.emptyList();
    }

    /** Ürünün görsellerini, görsel bilgi DTO'larına dönüştürür. */
    private List<ProductImageInfoResponse> mapImagesToInfoResponses(Set<ProductImage> productImages) {
        if (productImages != null) {
            return productImages.stream()
                    .map(img -> modelMapper.map(img, ProductImageInfoResponse.class))
                    .toList();
        }
        return Collections.emptyList();
    }

    /** Ürünün varyantlarını, varyant bilgi DTO'larına dönüştürür. */
    private List<ProductItemInfo> mapItemsToInfo(Set<ProductItem> productItems) {
        if (productItems != null) {
            return productItems.stream()
                    .map(item -> modelMapper.map(item, ProductItemInfo.class))
                    .toList();
        }
        return Collections.emptyList();
    }


    /**
     * Alışveriş sepeti ve içindeki ürünler için gerekli dönüşüm kurallarını belirler.
     * Bu metot, sepetteki her bir ürünün detaylarını ve ara toplamını hesaplar.
     * Ayrıca, sepetin tamamı için toplam ürün sayısı gibi özet bilgileri de oluşturur.
     * Karmaşık dönüşüm adımları, daha anlaşılır olmaları için kendi özel metotlarına ayrılmıştır.
     */
    private void configureShoppingCartMappings() {
        Converter<ShoppingCartItem, CartItemResponse> itemConverter = context -> {
            ShoppingCartItem source = context.getSource();
            CartItemResponse destination = new CartItemResponse();
            destination.setId(source.getId());
            destination.setQuantity(source.getQuantity());

            mapProductItemDetailsToCartItem(source.getProductItem(), destination);
            calculateAndSetSubTotal(destination);

            return destination;
        };

        Converter<ShoppingCart, CartResponse> cartConverter = context -> {
            ShoppingCart source = context.getSource();
            CartResponse destination = new CartResponse();
            destination.setId(source.getId());

            if (source.getCartItems() != null && !source.getCartItems().isEmpty()) {
                List<CartItemResponse> itemResponses = source.getCartItems().stream()
                        .map(item -> modelMapper.map(item, CartItemResponse.class))
                        .toList();
                destination.setItems(itemResponses);
                destination.setTotalItems(calculateTotalItems(itemResponses));
            } else {
                destination.setItems(Collections.emptyList());
                destination.setTotalItems(0);
            }

            return destination;
        };

        modelMapper.addConverter(itemConverter, ShoppingCartItem.class, CartItemResponse.class);
        modelMapper.addConverter(cartConverter, ShoppingCart.class, CartResponse.class);
    }

    /** Sepetteki bir ürünün ürün detaylarını doldurur. */
    private void mapProductItemDetailsToCartItem(ProductItem productItem, CartItemResponse destination) {
        if (productItem == null) {
            return;
        }

        destination.setProductItemId(productItem.getId());
        destination.setUnitPrice(productItem.getUnitPrice());

        if (productItem.getProduct() != null) {
            destination.setProductName(productItem.getProduct().getName());
            destination.setMainImageUrl(getMainImageUrl(productItem.getProduct().getProductImages()));
        }

        if (productItem.getColour() != null) {
            destination.setColourName(productItem.getColour().getColourName());
        }

        if (productItem.getSize() != null) {
            destination.setSizeName(productItem.getSize().getSizeName());
        }
    }

    /** Birim fiyat ve adete göre sepetteki bir ürünün ara toplamını hesaplar. */
    private void calculateAndSetSubTotal(CartItemResponse destination) {
        if (destination.getUnitPrice() != null && destination.getQuantity() != null) {
            destination.setSubTotal(destination.getUnitPrice().multiply(BigDecimal.valueOf(destination.getQuantity())));
        } else {
            destination.setSubTotal(BigDecimal.ZERO);
        }
    }

    /** Sepetteki tüm ürünlerin toplam adedini hesaplar. */
    private int calculateTotalItems(List<CartItemResponse> itemResponses) {
        return itemResponses.stream().mapToInt(CartItemResponse::getQuantity).sum();
    }


    /**
     * Müşteri adres bilgilerini, fiziksel adres ve sahiplik ilişkisi üzerinden ayrıştırarak yapılandıran dönüşüm mantığıdır.
     * Ham adres verilerini, müşterinin varsayılan gönderim veya fatura adresi tercihlerine göre işaretleyerek; liste ve detay görünümleri için optimize edilmiş, okunabilir yerleşim planları oluşturur.
     */
    private void configureAddressMappings() {

        this.modelMapper.typeMap(CustomerAddress.class, AddressDetailResponse.class).addMappings(mapper -> {
            mapper.map(src -> src.getAddress().getId(), AddressDetailResponse::setId);
            mapper.map(src -> src.getAddress().getTitle(), AddressDetailResponse::setTitle);
            mapper.map(src -> src.getAddress().getAddressLine1(), AddressDetailResponse::setAddressLine1);
            mapper.map(src -> src.getAddress().getAddressLine2(), AddressDetailResponse::setAddressLine2);
            mapper.map(src -> src.getAddress().getCity(), AddressDetailResponse::setCity);
            mapper.map(src -> src.getAddress().getPostalCode(), AddressDetailResponse::setPostalCode);
            mapper.map(src -> src.getAddress().getCountry().getCountryName(), AddressDetailResponse::setCountryName);
            mapper.map(CustomerAddress::getIsShippingAddress, AddressDetailResponse::setDefaultShipping);
            mapper.map(CustomerAddress::getIsBillingAddress, AddressDetailResponse::setDefaultBilling);
        });

        this.modelMapper.typeMap(CustomerAddress.class, ListAddressResponse.class).addMappings(mapper -> {
            mapper.map(src -> src.getAddress().getId(), ListAddressResponse::setId);
            mapper.map(src -> src.getAddress().getTitle(), ListAddressResponse::setTitle);
            mapper.map(src -> src.getAddress().getAddressLine1(), ListAddressResponse::setAddressLine1);
            mapper.map(src -> src.getAddress().getCity(), ListAddressResponse::setCity);
            mapper.map(src -> src.getAddress().getCountry().getCountryName(), ListAddressResponse::setCountryName);
            mapper.map(CustomerAddress::getIsShippingAddress, ListAddressResponse::setDefaultShipping);
            mapper.map(CustomerAddress::getIsBillingAddress, ListAddressResponse::setDefaultBilling);
        });
    }

    /**
     * Hassas ödeme verilerini kullanıcı deneyimini bozmadan güvenli hale getiren haritalama kurgusudur.
     * Kart numaralarının tamamı yerine sadece son dört hanesini kullanarak maskeleme yapar ve kart ailesi, son kullanma tarihi gibi bilgileri görsel arayüzlerin beklediği formatta sunar.
     */
    private void configurePaymentMethodMappings() {

        Converter<PaymentMethod, PaymentMethodDetailResponse> toDetailResponse = context -> {
            PaymentMethod source = context.getSource();
            PaymentMethodDetailResponse destination = new PaymentMethodDetailResponse();

            destination.setId(source.getId());
            destination.setCardHolderName(source.getCardHolderName());
            destination.setCardFamily(source.getCardFamily());
            destination.setExpiryMonth(source.getExpiryMonth());
            destination.setExpiryYear(source.getExpiryYear());
            destination.setMaskedCardNumber(MASKED_CARD_PREFIX + source.getLastFourDigits());

            return destination;
        };

        Converter<PaymentMethod, ListPaymentMethodResponse> toListResponse = context -> {
            PaymentMethod source = context.getSource();
            ListPaymentMethodResponse destination = new ListPaymentMethodResponse();

            destination.setId(source.getId());
            destination.setCardHolderName(source.getCardHolderName());
            destination.setCardFamily(source.getCardFamily());
            destination.setMaskedCardNumber(MASKED_CARD_PREFIX + source.getLastFourDigits());

            return destination;
        };

        modelMapper.addConverter(toDetailResponse, PaymentMethod.class, PaymentMethodDetailResponse.class);
        modelMapper.addConverter(toListResponse, PaymentMethod.class, ListPaymentMethodResponse.class);
    }

    /**
     * Siparişle ilgili varlıkları, farklı detay seviyelerindeki yanıt DTO'larına dönüştürmek için gereken kuralları yapılandırır.
     * Bu metot, sipariş oluşturma, listeleme ve detay görüntüleme gibi senaryolar için ayrı dönüştürücüler tanımlar.
     * Kod tekrarını önlemek ve okunabilirliği artırmak amacıyla, müşteri bilgileri, kargo detayları ve adres gibi
     * ortak verileri eşleyen mantıklar, kendi özel yardımcı metotlarına ayrılmıştır.
     */
    private void configureOrderMappings() {
        Converter<Order, AddOrderResponse> addOrderConverter = context -> {
            Order source = context.getSource();
            AddOrderResponse destination = new AddOrderResponse();
            mapBaseOrderDetails(source, destination);
            mapCustomerInfoToOrderResponse(source, destination);
            mapOrderStatusToResponse(source, destination);
            return destination;
        };
        modelMapper.addConverter(addOrderConverter, Order.class, AddOrderResponse.class);

        Converter<Order, OrderDetailResponse> orderDetailConverter = context -> {
            Order source = context.getSource();
            OrderDetailResponse destination = new OrderDetailResponse();
            mapBaseOrderDetails(source, destination);
            mapCustomerInfoToOrderResponse(source, destination);
            mapShippingInfoToOrderResponse(source, destination);
            mapOrderStatusToResponse(source, destination);
            destination.setShippingAddress(createAddressDetailResponse(source));
            destination.setItems(mapOrderItemsToResponse(source.getOrderItems()));
            return destination;
        };
        modelMapper.addConverter(orderDetailConverter, Order.class, OrderDetailResponse.class);

        Converter<Order, ListUserOrdersResponse> listUserOrdersConverter = context -> {
            Order source = context.getSource();
            ListUserOrdersResponse destination = new ListUserOrdersResponse();
            mapBaseOrderDetails(source, destination);
            mapOrderStatusToResponse(source, destination);
            return destination;
        };
        modelMapper.addConverter(listUserOrdersConverter, Order.class, ListUserOrdersResponse.class);

        Converter<Order, UserOrderDetailResponse> userOrderDetailConverter = context -> {
            Order source = context.getSource();
            UserOrderDetailResponse destination = new UserOrderDetailResponse();
            mapBaseOrderDetails(source, destination);
            mapShippingInfoToOrderResponse(source, destination);
            mapOrderStatusToResponse(source, destination);
            destination.setShippingAddress(createAddressDetailResponse(source));
            destination.setItems(mapOrderItemsToResponse(source.getOrderItems()));
            return destination;
        };
        modelMapper.addConverter(userOrderDetailConverter, Order.class, UserOrderDetailResponse.class);

        Converter<OrderItem, OrderItemResponse> orderItemConverter = context -> {
            OrderItem source = context.getSource();
            OrderItemResponse destination = new OrderItemResponse();
            destination.setQuantity(source.getQuantity());
            destination.setPriceAtOrder(source.getPriceAtOrder());
            destination.setVatRate(source.getVatRate());
            mapProductItemDetailsToOrderItemResponse(source.getProductItem(), destination);
            return destination;
        };
        modelMapper.addConverter(orderItemConverter, OrderItem.class, OrderItemResponse.class);
    }

    private void mapBaseOrderDetails(Order source, AddOrderResponse destination) {
        destination.setId(source.getId());
        destination.setOrderDate(source.getOrderDate());
        destination.setOrderTotal(source.getOrderTotal());
    }

    private void mapBaseOrderDetails(Order source, ListUserOrdersResponse destination) {
        destination.setId(source.getId());
        destination.setOrderDate(source.getOrderDate());
        destination.setOrderTotal(source.getOrderTotal());
    }

    private void mapBaseOrderDetails(Order source, OrderDetailResponse destination) {
        destination.setId(source.getId());
        destination.setOrderDate(source.getOrderDate());
        destination.setOrderTotal(source.getOrderTotal());
    }

    private void mapBaseOrderDetails(Order source, UserOrderDetailResponse destination) {
        destination.setId(source.getId());
        destination.setOrderDate(source.getOrderDate());
        destination.setOrderTotal(source.getOrderTotal());
    }

    private void mapCustomerInfoToOrderResponse(Order source, AddOrderResponse destination) {
        if (source.getCustomer() != null) {
            destination.setCustomerId(source.getCustomer().getId());
        }
    }

    private void mapCustomerInfoToOrderResponse(Order source, OrderDetailResponse destination) {
        if (source.getCustomer() != null) {
            destination.setCustomerId(source.getCustomer().getId());
            destination.setCustomerName(source.getCustomer().getContactName());
        }
    }

    private void mapShippingInfoToOrderResponse(Order source, OrderDetailResponse destination) {
        if (source.getShippingMethod() != null) {
            destination.setShippingMethodName(source.getShippingMethod().getName());
        }
    }

    private void mapShippingInfoToOrderResponse(Order source, UserOrderDetailResponse destination) {
        if (source.getShippingMethod() != null) {
            destination.setShippingMethodName(source.getShippingMethod().getName());
        }
    }

    private void mapOrderStatusToResponse(Order source, AddOrderResponse destination) {
        if (source.getOrderStatus() != null && source.getOrderStatus().getStatusName() != null) {
            destination.setOrderStatusName(source.getOrderStatus().getStatusName().name());
        }
    }

    private void mapOrderStatusToResponse(Order source, ListUserOrdersResponse destination) {
        if (source.getOrderStatus() != null && source.getOrderStatus().getStatusName() != null) {
            destination.setOrderStatusName(source.getOrderStatus().getStatusName().name());
        }
    }

    private void mapOrderStatusToResponse(Order source, OrderDetailResponse destination) {
        if (source.getOrderStatus() != null && source.getOrderStatus().getStatusName() != null) {
            destination.setOrderStatusName(source.getOrderStatus().getStatusName().name());
        }
    }

    private void mapOrderStatusToResponse(Order source, UserOrderDetailResponse destination) {
        if (source.getOrderStatus() != null && source.getOrderStatus().getStatusName() != null) {
            destination.setOrderStatusName(source.getOrderStatus().getStatusName().name());
        }
    }

    /** Bir siparişin gönderim adresini, veritabanından ek bilgilerle zenginleştirerek bir adres DTO'su oluşturur. */
    private AddressDetailResponse createAddressDetailResponse(Order order) {
        if (order.getShippingAddress() == null || order.getCustomer() == null) {
            return null;
        }

        Address shippingAddressEntity = order.getShippingAddress();
        AddressDetailResponse addressDto = new AddressDetailResponse();
        addressDto.setId(shippingAddressEntity.getId());
        addressDto.setTitle(shippingAddressEntity.getTitle());
        addressDto.setAddressLine1(shippingAddressEntity.getAddressLine1());
        addressDto.setAddressLine2(shippingAddressEntity.getAddressLine2());
        addressDto.setCity(shippingAddressEntity.getCity());
        addressDto.setPostalCode(shippingAddressEntity.getPostalCode());

        if (shippingAddressEntity.getCountry() != null) {
            addressDto.setCountryName(shippingAddressEntity.getCountry().getCountryName());
        }

        customerAddressRepository.findByAddressIdAndCustomerId(
                        shippingAddressEntity.getId(), order.getCustomer().getId())
                .ifPresent(customerAddress -> {
                    addressDto.setDefaultShipping(customerAddress.getIsShippingAddress());
                    addressDto.setDefaultBilling(customerAddress.getIsBillingAddress());
                });

        return addressDto;
    }

    /** Bir siparişe ait ürün kalemlerini yanıt DTO'larına dönüştürür. */
    private List<OrderItemResponse> mapOrderItemsToResponse(Set<OrderItem> orderItems) {
        if (orderItems == null) {
            return Collections.emptyList();
        }
        return orderItems.stream()
                .map(orderItem -> modelMapper.map(orderItem, OrderItemResponse.class))
                .toList();
    }

    /** Bir sipariş kalemindeki ürün varyantının detaylarını hedef DTO'ya aktarır. */
    private void mapProductItemDetailsToOrderItemResponse(ProductItem productItem, OrderItemResponse destination) {
        if (productItem == null) {
            return;
        }

        if (productItem.getProduct() != null) {
            destination.setProductId(productItem.getProduct().getId());
            destination.setProductName(productItem.getProduct().getName());
        }
        if (productItem.getColour() != null) {
            destination.setColourName(productItem.getColour().getColourName());
        }
        if (productItem.getSize() != null) {
            destination.setSizeName(productItem.getSize().getSizeName());
        }
    }


    /**
     * Satış sonrası mali belgelerin ve iade süreçlerinin yasal/operasyonel detaylarını yöneten dönüşüm katmanıdır.
     * Fatura numarası, vergi oranları ve ara toplamların sipariş verileriyle tutarlılığını sağlar.
     * İade taleplerinde ise sipariş kalemleri ve ürün bilgilerini birbirine bağlayarak operasyon ekipleri için anlaşılır bir veri seti üretir.
     */
    private void configureInvoiceMappings() {
        this.modelMapper.typeMap(Invoice.class, ListUserInvoicesResponse.class).addMappings(mapper -> {
            mapper.map(Invoice::getId, ListUserInvoicesResponse::setId);
            mapper.map(Invoice::getInvoiceNumber, ListUserInvoicesResponse::setInvoiceNumber);
            mapper.map(Invoice::getGrandTotal, ListUserInvoicesResponse::setGrandTotal);
            mapper.map(src -> src.getOrder().getId(), ListUserInvoicesResponse::setOrderId);
        });

        Converter<Invoice, UserInvoiceDetailResponse> invoiceToDetailResponseConverter = context -> {
            Invoice source = context.getSource();
            UserInvoiceDetailResponse destination = new UserInvoiceDetailResponse();

            destination.setId(source.getId());
            destination.setInvoiceNumber(source.getInvoiceNumber());
            destination.setInvoiceDate(source.getInvoiceDate());
            destination.setStatus(source.getStatus());
            destination.setSubTotal(source.getSubTotal());
            destination.setVatAmount(source.getVatAmount());
            destination.setInterestAmount(source.getInterestAmount());
            destination.setShippingFee(source.getShippingFee());
            destination.setGrandTotal(source.getGrandTotal());
            destination.setInstallmentCount(source.getInstallmentCount());

            if (source.getOrder() != null) {
                destination.setOrderId(source.getOrder().getId());
                destination.setOrderDate(source.getOrder().getOrderDate());
                if (source.getOrder().getOrderItems() != null) {
                    destination.setOrderItems(source.getOrder().getOrderItems().stream()
                            .map(item -> modelMapper.map(item, OrderItemResponse.class))
                            .toList());
                }
            }

            if (source.getPaymentMethod() != null) {
                destination.setCardFamily(source.getPaymentMethod().getCardFamily());
                destination.setMaskedCardNumber(MASKED_CARD_PREFIX + source.getPaymentMethod().getLastFourDigits());
            }

            return destination;
        };

        modelMapper.addConverter(invoiceToDetailResponseConverter, Invoice.class, UserInvoiceDetailResponse.class);
    }

    /**
     * Müşteri iade taleplerini ve süreç takibini yöneten dönüşüm katmanıdır.
     * İade edilmek istenen ürün kalemini, ilgili siparişle ilişkilendirerek operasyon ekiplerinin iade gerekçelerini ve ürün detaylarını tek bir model üzerinden izlemesine olanak tanır.
     */
    private void configureReturnRequestMappings() {
        this.modelMapper.typeMap(ReturnRequest.class, ReturnRequestResponse.class).addMappings(mapper -> {
            mapper.map(ReturnRequest::getId, ReturnRequestResponse::setId);
            mapper.map(ReturnRequest::getReason, ReturnRequestResponse::setReason);
            mapper.map(ReturnRequest::getStatus, ReturnRequestResponse::setStatus);
            mapper.map(src -> src.getOrder().getId(), ReturnRequestResponse::setOrderId);
            mapper.map(src -> src.getOrderItem().getProductItem().getProduct().getName(), ReturnRequestResponse::setProductName);
        });
    }

    @Override
    public ModelMapper getMapper() {
        return this.modelMapper;
    }
}