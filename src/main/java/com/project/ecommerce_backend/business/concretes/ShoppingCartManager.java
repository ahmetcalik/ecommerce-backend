package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.business.abstracts.CustomerService;
import com.project.ecommerce_backend.business.abstracts.ProductService;
import com.project.ecommerce_backend.business.abstracts.ShoppingCartService;
import com.project.ecommerce_backend.business.dtos.requests.cart.AddCartItemRequest;
import com.project.ecommerce_backend.business.dtos.requests.cart.UpdateCartItemRequest;
import com.project.ecommerce_backend.business.dtos.responses.cart.CartAndSessionResponse;
import com.project.ecommerce_backend.business.dtos.responses.cart.CartResponse;
import com.project.ecommerce_backend.core.constants.Messages;
import com.project.ecommerce_backend.core.exceptions.types.NotFoundException;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.core.security.details.CustomerDetails;
import com.project.ecommerce_backend.core.utils.mapper.ModelMapperService;
import com.project.ecommerce_backend.core.utils.result.Result;
import com.project.ecommerce_backend.core.utils.result.SuccessResult;
import com.project.ecommerce_backend.entities.concretes.*;
import com.project.ecommerce_backend.repositories.abstracts.ShoppingCartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Alışveriş sepeti ekosistemini yöneten, misafir ve kayıtlı kullanıcılar için esnek sepet yapılandırmaları sunan merkezi iş mantığı servisidir.
 * Bu sınıf; sepet içeriğine ürün ekleme, miktar güncelleme ve ürün çıkarma işlemlerini koordine ederken, aynı zamanda KDV ve ara toplam gibi mali kalemlerin anlık hesaplamasını üstlenir. Misafir kullanıcıların giriş yapması durumunda sepetlerin birleştirilmesi sürecini yönetir ve veri tutarlılığını sağlamak amacıyla hem oturum (Session) hem de müşteri bazlı önbellekleme stratejilerini eş zamanlı olarak yürütür.
 */
@Service
@RequiredArgsConstructor
public class ShoppingCartManager implements ShoppingCartService {

    private final ShoppingCartRepository shoppingCartRepository;
    private final CustomerService customerService;
    private final ProductService productService;
    private final MessageService messageService;
    private final ModelMapperService modelMapperService;

    /**
     * Aktif oturuma ait sepetin içeriğini, ürün detaylarını ve hesaplanmış toplam tutarlarını bir arada sunar.
     * Metot çalıştırıldığında, öncelikle kullanıcının kimlik doğrulaması kontrol edilir; eğer kullanıcı giriş yapmışsa hesabına bağlı sepet, yapmamışsa oturum numarası üzerinden misafir sepeti sorgulanarak sonuçlar mali dökümle birlikte geri döndürülür.
     *
     * @param sessionId Misafir kullanıcılar için benzersiz oturum numarasıdır.
     * @return Sepet içeriğini ve oturum bilgilerini barındıran veri transfer nesnesini döner.
     */
    @Override
    @Transactional
    public CartAndSessionResponse getCartDetails(UUID sessionId) {
        ShoppingCart cart = getCartForCurrentUser(sessionId);
        CartResponse cartResponse = mapCartToResponse(cart);
        return new CartAndSessionResponse(cartResponse, cart.getSessionId());
    }

    /**
     * Sepete yeni bir ürün ekler veya mevcut ürünün miktarını artırarak sepetin mali yapısını yeniden hesaplar.
     * İşlem sırasında ürünün stok tanımı doğrulanır; eğer ürün sepette zaten mevcutsa miktar güncellenir, aksi durumda yeni bir sepet kalemi oluşturulur. Güncelleme sonrası hem oturum bazlı hem de eğer mevcutsa müşteri bazlı önbellek kayıtları geçersiz kılınarak verinin en güncel haliyle sunulması garanti edilir.
     *
     * @param sessionId İşlemin yapıldığı oturum kimliğidir.
     * @param request Eklenecek ürünün kimlik bilgisini ve miktarını içeren nesnedir.
     * @return Güncel sepet dökümünü ve oturum bilgisini döner.
     */
    @Caching(evict = {
            @CacheEvict(value = "shoppingCart", key = "'session:' + #sessionId"),
            @CacheEvict(value = "shoppingCart", key = "'customer:' + #root.target.getCustomerIdFromSession(#sessionId)", condition = "#root.target.getCustomerIdFromSession(#sessionId) != null")
    })
    @Override
    @Transactional
    public CartAndSessionResponse addItemToCart(UUID sessionId, AddCartItemRequest request) {
        ShoppingCart cart = getCartForCurrentUser(sessionId);
        ProductItem productItem = productService.getProductItemById(request.getProductItemId());
        addOrUpdateItemInCart(cart, productItem, request.getQuantity());
        ShoppingCart updatedCart = shoppingCartRepository.save(cart);
        CartResponse cartResponse = mapCartToResponse(updatedCart);
        return new CartAndSessionResponse(cartResponse, updatedCart.getSessionId());
    }

    /**
     * Sepetteki ürünlerin miktarlarını kullanıcının talebi doğrultusunda yeniden düzenler.
     * Belirtilen sepet kalemi üzerinde miktar değişikliği yapıldıktan sonra sepetin toplam tutarları tekrar hesaplanır ve ilgili tüm önbellek alanları temizlenerek tutarlılık sağlanır.
     */
    @Caching(evict = {
            @CacheEvict(value = "shoppingCart", key = "'session:' + #sessionId"),
            @CacheEvict(value = "shoppingCart", key = "'customer:' + #root.target.getCustomerIdFromSession(#sessionId)", condition = "#root.target.getCustomerIdFromSession(#sessionId) != null")
    })
    @Override
    @Transactional
    public CartAndSessionResponse updateItemQuantity(UUID sessionId, Long itemId, UpdateCartItemRequest request) {
        ShoppingCart cart = getCartForCurrentUser(sessionId);
        ShoppingCartItem itemToUpdate = findCartItemOrThrow(cart, itemId);
        itemToUpdate.setQuantity(request.getQuantity());
        ShoppingCart updatedCart = shoppingCartRepository.save(cart);
        CartResponse cartResponse = mapCartToResponse(updatedCart);
        return new CartAndSessionResponse(cartResponse, updatedCart.getSessionId());
    }

    /**
     * Sepet içerisinden belirli bir ürün kalemini kalıcı olarak çıkartır ve sepetin mali dökümünü yeniden hesaplar.
     * Bu işlem, aktif oturuma ait sepet içerisindeki ilgili kalemi tespit ederek listeden kaldırır ve veritabanı güncellendikten sonra sepetin ara toplam ve vergi tutarlarını güncel içerik üzerinden tekrar oluşturur. İşlem sonucunda verinin her iki kullanıcı senaryosunda da güncel kalması için hem oturum bazlı hem de eğer sepet bir üyeye aitse müşteri bazlı önbellek kayıtları otomatik olarak temizlenir.
     *
     * @param sessionId İşlemin yürütüldüğü oturuma ait benzersiz kimlik numarasıdır.
     * @param itemId Sepetten çıkartılacak olan spesifik ürün kaleminin kimliğidir.
     * @return Güncel ürün listesini ve hesaplanmış yeni toplam tutarları içeren yanıt nesnesini döner.
     */
    @Caching(evict = {
            @CacheEvict(value = "shoppingCart", key = "'session:' + #sessionId"),
            @CacheEvict(value = "shoppingCart", key = "'customer:' + #root.target.getCustomerIdFromSession(#sessionId)", condition = "#root.target.getCustomerIdFromSession(#sessionId) != null")
    })
    @Override
    @Transactional
    public CartAndSessionResponse removeItemFromCart(UUID sessionId, Long itemId) {
        ShoppingCart cart = getCartForCurrentUser(sessionId);
        ShoppingCartItem itemToRemove = findCartItemOrThrow(cart, itemId);
        cart.getCartItems().remove(itemToRemove);
        ShoppingCart updatedCart = shoppingCartRepository.save(cart);
        CartResponse cartResponse = mapCartToResponse(updatedCart);
        return new CartAndSessionResponse(cartResponse, updatedCart.getSessionId());
    }

    /**
     * Misafir olarak sepete eklenen ürünleri, kullanıcı giriş yaptığı anda müşterinin kalıcı sepetine transfer eder.
     * Bu süreçte her iki sepetteki aynı ürünler tespit edilerek miktarlar birleştirilir ve transfer işlemi tamamlandıktan sonra misafir sepeti sistemden tamamen kaldırılarak oturum ve müşteri bazlı tüm önbellek kayıtları güncellenir.
     *
     * @param sessionId Birleştirilecek olan misafir sepetinin oturum numarasıdır.
     * @param customerId Ürünlerin aktarılacağı ana müşteri kimliğidir.
     */
    @Caching(evict = {
            @CacheEvict(value = "shoppingCart", key = "'session:' + #sessionId"), // Misafir sepetini temizler (silinecektir)
            @CacheEvict(value = "shoppingCart", key = "'customer:' + #customerId") // Kullanıcı sepetini temizler (güncellenecektir)
    })
    @Override
    @Transactional
    public void mergeCarts(UUID sessionId, Long customerId) {
        Optional<ShoppingCart> guestCartOpt = shoppingCartRepository.findFirstBySessionId(sessionId);

        if (guestCartOpt.isEmpty() || guestCartOpt.get().getCartItems().isEmpty()) {
            return;
        }

        ShoppingCart guestCart = guestCartOpt.get();
        ShoppingCart userCart = getOrCreateCartForUser(customerId);

        guestCart.getCartItems().forEach(guestItem -> transferItemToUserCart(guestItem, userCart));

        shoppingCartRepository.save(userCart);
        shoppingCartRepository.delete(guestCart);
    }

    /**
     * Sipariş oluşturma gibi kritik süreçlerde kullanılmak üzere müşterinin aktif sepet nesnesini doğrudan sunar.
     * Bu metot, veritabanı maliyetini düşürmek amacıyla müşteri kimliği üzerinden özelleşmiş bir önbellek anahtarı kullanarak sonuçları Redis hafızasından servis eder.
     */
    @Override
    @Cacheable(value = "shoppingCart", key = "'customer:' + #customerId")
    public ShoppingCart getActiveCartByCustomerId(Long customerId) {
        return getOrCreateCartForUser(customerId);
    }

    /**
     * Siparişin başarıyla tamamlanmasının ardından ilgili müşterinin sepet içeriğini tamamen temizleyerek yeni alışverişler için hazır hale getirir.
     */
    @CacheEvict(value = "shoppingCart", key = "'customer:' + #cartId")
    @Override
    @Transactional
    public void clearCartById(Long cartId) {
        ShoppingCart cart = shoppingCartRepository.findById(cartId)
                .orElseThrow(() -> new NotFoundException(messageService.getMessage(Messages.Cart.CART_NOT_FOUND)));
        cart.getCartItems().clear();
        shoppingCartRepository.save(cart);
    }

    /**
     * Mevcut oturuma ait sepetin içeriğini, tüm ürün kalemlerini çıkartarak tamamen temizler.
     * Bu işlem, aktif oturum üzerinden ilgili sepet nesnesini tespit eder ve sepet kalemlerini veritabanından silerken eş zamanlı olarak oturum bazlı önbellek kaydını da geçersiz kılarak verinin sıfırlanmasını sağlar. İşlem sonunda kullanıcıya sepetin başarıyla boşaltıldığına dair uluslararasılaştırma destekli bir geri bildirim mesajı iletilir.
     *
     * @param sessionId Temizlenecek sepetin bağlı olduğu benzersiz oturum numarasıdır.
     * @return İşlemin başarıyla tamamlandığını bildiren sonuç nesnesini döner.
     */
    @CacheEvict(value = "shoppingCart", key = "'session:' + #sessionId")
    @Override
    @Transactional
    public Result clearCart(UUID sessionId) {
        ShoppingCart cart = getCartForCurrentUser(sessionId);
        cart.getCartItems().clear();
        shoppingCartRepository.save(cart);
        return new SuccessResult(messageService.getMessage(Messages.Cart.CART_CLEARED_SUCCESSFULLY));
    }

    // --- YARDIMCI METOTLAR ---

    /**
     * Önbellek temizleme işlemleri sırasında oturum numarası üzerinden ilgili müşterinin kimlik bilgisini tespit eder.
     * Bu metot özellikle SpEL ifadeleri içerisinde kullanılarak, misafir sepeti üzerinden işlem yapılsa dahi eğer sepet bir kullanıcıya aitse o kullanıcının önbelleğinin de senkronize bir şekilde güncellenmesini sağlar.
     */
    public Long getCustomerIdFromSession(UUID sessionId) {
        return shoppingCartRepository.findFirstBySessionId(sessionId)
                .flatMap(cart -> Optional.ofNullable(cart.getCustomer()))
                .map(Customer::getId)
                .orElse(null);
    }

    /**
     * Ham sepet verisini, ara toplam, KDV ve genel toplam gibi finansal dökümlerle zenginleştirerek kullanıcı arayüzüne uygun bir yapıya dönüştürür.
     */
    private CartResponse mapCartToResponse(ShoppingCart cart) {
        CartResponse response = modelMapperService.getMapper().map(cart, CartResponse.class);
        Set<ShoppingCartItem> items = cart.getCartItems();
        BigDecimal subTotal = calculateTotalSubTotal(items);
        BigDecimal vatAmount = calculateTotalVatAmount(items);
        response.setTotalSubTotal(subTotal);
        response.setTotalVatAmount(vatAmount);
        response.setGrandTotal(subTotal.add(vatAmount));
        return response;
    }

    /**
     * Sepet içerisindeki ürünlerin birim fiyatları ve miktarları üzerinden vergi hariç ara toplamı belirleyen hesaplama mekanizmasıdır.
     */
    private BigDecimal calculateTotalSubTotal(Set<ShoppingCartItem> items) {
        return items.stream()
                .map(item -> item.getProductItem().getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Ürün bazlı tanımlanmış KDV oranlarını kullanarak sepetin toplam vergi yükünü hesaplayan mali denetim metodudur.
     */
    private BigDecimal calculateTotalVatAmount(Set<ShoppingCartItem> items) {
        return items.stream()
                .map(item -> {
                    BigDecimal itemSubTotal = item.getProductItem().getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    BigDecimal vatRate = item.getProductItem().getProduct().getVatRate();
                    return itemSubTotal.multiply(vatRate);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Sepete ürün ekleme veya güncelleme isteklerinde, ürünün sepette halihazırda bulunup bulunmadığını kontrol ederek miktar artırımı ya da yeni kayıt ekleme kararını verir.
     */
    private void addOrUpdateItemInCart(ShoppingCart cart, ProductItem productItem, int quantityToAdd) {
        cart.getCartItems().stream()
                .filter(item -> item.getProductItem().getId().equals(productItem.getId()))
                .findFirst()
                .ifPresentOrElse(
                        existingItem -> existingItem.setQuantity(existingItem.getQuantity() + quantityToAdd),
                        () -> cart.getCartItems().add(ShoppingCartItem.builder()
                                .cart(cart)
                                .productItem(productItem)
                                .quantity(quantityToAdd)
                                .build())
                );
    }

    /**
     * Sepet içerisinde işlem yapılmak istenen spesifik bir ürün kalemini arar ve bulamazsa sistem genelinde tanımlı hata mesajıyla bir istisna fırlatır.
     */
    private ShoppingCartItem findCartItemOrThrow(ShoppingCart cart, Long itemId) {
        return cart.getCartItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(messageService.getMessage(Messages.Cart.CART_ITEM_NOT_FOUND_IN_CART)));
    }

    /**
     * Aktif kullanıcının kimlik doğrulama durumuna göre doğru sepet nesnesine yönlendirme yapar.
     * Kullanıcı sisteme giriş yapmışsa müşteriye özel sepeti, giriş yapmamışsa oturum bazlı misafir sepetini getirmek üzere ilgili alt metotları koordine eder.
     */
    private ShoppingCart getCartForCurrentUser(UUID sessionId) {
        return getAuthenticatedUser()
                .map(userDetails -> getOrCreateCartForUser(userDetails.getId()))
                .orElseGet(() -> getOrCreateGuestCart(sessionId));
    }

    /**
     * Müşteri kimliğiyle eşleşen bir sepet kaydı arar; eğer müşteri ilk kez alışveriş yapıyorsa veritabanında müşteriye özel yeni bir sepet alanı oluşturarak kalıcı hale getirir.
     */
    private ShoppingCart getOrCreateCartForUser(Long customerId) {
        return shoppingCartRepository.findByCustomerId(customerId)
                .orElseGet(() -> shoppingCartRepository.save(ShoppingCart.builder()
                        .customer(customerService.getByIdAsEntity(customerId))
                        .build()));
    }

    /**
     * Misafir kullanıcılar için mevcut oturum numarasıyla bir sepet sorgular; eğer oturuma ait bir kayıt yoksa yeni bir oturum numarası üreterek geçici bir sepet oluşturur.
     */
    private ShoppingCart getOrCreateGuestCart(UUID sessionId) {
        if (sessionId != null) {
            return shoppingCartRepository.findFirstBySessionId(sessionId)
                    .orElseGet(() -> shoppingCartRepository.save(ShoppingCart.builder().sessionId(sessionId).build()));
        }
        return shoppingCartRepository.save(ShoppingCart.builder().sessionId(UUID.randomUUID()).build());
    }

    /**
     * Güvenlik bağlamından aktif kullanıcının kimlik bilgilerini doğrudan ayrıştırarak kimlik doğrulama durumunu analiz eder.
     * Anonim kullanıcıları filtreleyerek sadece doğrulanmış müşteri detaylarını güvenli bir sarmalayıcı içinde sunar.
     */
    private Optional<CustomerDetails> getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty();
        }
        return Optional.of((CustomerDetails) authentication.getPrincipal());
    }

    /**
     * Misafir sepetinden kullanıcı sepetine ürün transferi yapılırken, mevcut varyantların çakışmamasını ve miktarların doğru toplanmasını sağlayan veri aktarım mantığıdır.
     */
    private void transferItemToUserCart(ShoppingCartItem guestItem, ShoppingCart userCart) {
        ProductItem productItem = guestItem.getProductItem();
        int quantity = guestItem.getQuantity();

        userCart.getCartItems().stream()
                .filter(item -> item.getProductItem().getId().equals(productItem.getId()))
                .findFirst()
                .ifPresentOrElse(
                        existingUserItem -> existingUserItem.setQuantity(existingUserItem.getQuantity() + quantity),
                        () -> {
                            ShoppingCartItem newUserItem = ShoppingCartItem.builder()
                                    .cart(userCart)
                                    .productItem(productItem)
                                    .quantity(quantity)
                                    .build();
                            userCart.getCartItems().add(newUserItem);
                        }
                );
    }
}