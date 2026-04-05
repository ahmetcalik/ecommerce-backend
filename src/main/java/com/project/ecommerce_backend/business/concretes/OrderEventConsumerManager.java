package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.business.abstracts.InstallmentService;
import com.project.ecommerce_backend.business.abstracts.InvoiceService;
import com.project.ecommerce_backend.business.abstracts.OrderService;
import com.project.ecommerce_backend.business.abstracts.PaymentMethodService;
import com.project.ecommerce_backend.business.abstracts.ProductService;
import com.project.ecommerce_backend.business.dtos.events.OrderCreatedEvent;
import com.project.ecommerce_backend.business.dtos.responses.installment.InstallmentOptionResponse;
import com.project.ecommerce_backend.core.configurations.KafkaConfiguration;
import com.project.ecommerce_backend.entities.concretes.Order;
import com.project.ecommerce_backend.entities.concretes.OrderItem;
import com.project.ecommerce_backend.entities.concretes.PaymentMethod;
import com.project.ecommerce_backend.entities.concretes.ShippingMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Kafka üzerinden gelen sipariş olaylarını tüketen (consume) ve bu olaylara bağlı asenkron iş akışlarını koordine eden servis katmanıdır.
 * Bu sınıf; bir sipariş başarıyla oluşturulduktan sonra gerçekleşmesi gereken stok düşürme ve fatura üretme gibi kritik süreçleri, birbirinden bağımsız "Consumer Group" yapılandırmalarıyla yönetir. Dağıtık sistemlerdeki veri tutarlılığını sağlamak adına işlemleri "Transactional" olarak yürütür ve olası hata durumlarında merkezi hata yönetim mekanizması (DefaultErrorHandler) ile entegre çalışarak yeniden deneme (Retry) ve hatalı mesaj kuyruğu (DLT) süreçlerini otomatik olarak işletir.
 */
@Service
public class OrderEventConsumerManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderEventConsumerManager.class);

    private final ProductService productService;
    private final InvoiceService invoiceService;
    private final OrderService orderService;
    private final PaymentMethodService paymentMethodService;
    private final InstallmentService installmentService;

    public OrderEventConsumerManager(ProductService productService,
                                     InvoiceService invoiceService,
                                     OrderService orderService,
                                     PaymentMethodService paymentMethodService,
                                     InstallmentService installmentService) {
        this.productService = productService;
        this.invoiceService = invoiceService;
        this.orderService = orderService;
        this.paymentMethodService = paymentMethodService;
        this.installmentService = installmentService;
    }

    /**
     * Yeni bir sipariş oluşturulduğunda stok güncellemelerini yapmak üzere Kafka'daki ilgili konuyu (topic) dinler.
     * Bu metodun öncelikli sorumluluğu, sipariş edilen her bir ürün kalemi için envanter kontrolünü yapıp stok miktarlarını güvenli bir şekilde düşürmektir. Mikroservis mimarisindeki "Fan-out" prensibi gereği, fatura oluşturma sürecinden bağımsız bir grup kimliği (groupId) ile çalışır; böylece stok güncelleme işlemi başarısız olsa dahi fatura oluşturma süreci etkilenmez ve sistem hataya dayanıklı (fault-tolerant) kalır.
     *
     * @param event Kafka'dan gelen ve sipariş edilen ürün detaylarını barındıran veri paketidir.
     * @throws IllegalStateException Stok düşürme sırasında bir hata oluşursa, merkezi hata yöneticisinin Retry/DLT süreçlerini başlatması için fırlatılır.
     */
    @KafkaListener(
            topics = KafkaConfiguration.TOPIC_ORDER_CREATED,
            groupId = "${spring.kafka.consumer.group-id}.stock"
    )

    @Transactional("transactionManager")
    public void handleOrderCreatedEventForStock(OrderCreatedEvent event) {
        LOGGER.info("[Stock Consumer] Kafka'dan 'ORDER_CREATED' olayı alındı. OrderID: {}", event.getOrderId());

        try {
            for (OrderCreatedEvent.OrderItemDetail item : event.getItems()) {
                LOGGER.debug("[Stock Consumer] Stok düşülüyor... ProductItemID: {}, Adet: {}",
                        item.getProductItemId(), item.getQuantity());
                productService.checkAndReduceStock(item.getProductItemId(), item.getQuantity());
            }
            LOGGER.info("[Stock Consumer] Sipariş {} için stoklar başarıyla güncellendi.", event.getOrderId());

        } catch (Exception e) {
            LOGGER.error("Sipariş {} için stok güncellenirken kritik hata oluştu.", event.getOrderId(), e);
            throw new IllegalStateException("Stok güncelleme işlemi başarısız oldu", e);
        }
    }

    /**
     * Sipariş oluşturma olayını takiben, finansal ve yasal süreçler için gerekli faturayı üretmek üzere ilgili konuyu dinler.
     * Bu süreçte; siparişin tüm kalemleri analiz edilerek ara toplam, KDV ve ödeme yöntemine (taksitli/peşin) bağlı vade farkları gibi mali kalemler yeniden hesaplanır. Farklı bir grup kimliği kullanılarak, stok süreciyle paralel ancak izole bir şekilde çalışması sağlanır; böylece sistemin ölçeklenebilirliği artırılırken muhasebe ve depo süreçlerinin asenkron bütünlüğü garanti altına alınır.
     *
     * @param event Fatura üretimi için gerekli olan müşteri, ödeme ve sipariş referanslarını barındıran olaydır.
     * @throws IllegalStateException Fatura oluşturma hatasında, mesajın güvenli bir şekilde yeniden işlenmesi veya DLT'ye aktarılması için fırlatılır.
     */
    @KafkaListener(
            topics = KafkaConfiguration.TOPIC_ORDER_CREATED,
            groupId = "${spring.kafka.consumer.group-id}.invoice"
    )
    @Transactional("transactionManager")
    public void handleOrderCreatedEventForInvoice(OrderCreatedEvent event) {
        LOGGER.info("[Invoice Consumer] Kafka'dan 'ORDER_CREATED' olayı alındı. OrderID: {}", event.getOrderId());

        try {
            Order order = orderService.findByIdAsEntityWithDetails(event.getOrderId());
            PaymentMethod paymentMethod = paymentMethodService.getByIdAndCustomerId(
                    event.getPaymentMethodId(),
                    event.getCustomerId()
            );

            ShippingMethod shippingMethod = order.getShippingMethod();
            Set<OrderItem> orderItems = order.getOrderItems();

            BigDecimal productSubTotal = orderItems.stream()
                    .map(item -> item.getPriceAtOrder().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal productVatAmount = orderItems.stream()
                    .map(item -> {
                        BigDecimal itemSubTotal = item.getPriceAtOrder().multiply(BigDecimal.valueOf(item.getQuantity()));
                        return itemSubTotal.multiply(item.getVatRate());
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal amountForInstallment = productSubTotal.add(productVatAmount);
            Integer installmentCount = event.getInstallmentCount();

            BigDecimal interestAmount = BigDecimal.ZERO;
            if (installmentCount != null && installmentCount > 1) {
                InstallmentOptionResponse calculatedOption = installmentService.getSpecificOption(
                        amountForInstallment,
                        paymentMethod.getCardFamily(),
                        installmentCount
                );
                interestAmount = calculatedOption.getTotalAmount().subtract(amountForInstallment);
            }

            invoiceService.createInvoiceForOrder(
                    order,
                    paymentMethod,
                    productSubTotal,
                    productVatAmount,
                    interestAmount,
                    installmentCount,
                    shippingMethod
            );

            LOGGER.info("[Invoice Consumer] Sipariş {} için fatura başarıyla oluşturuldu.", event.getOrderId());
        } catch (Exception e) {
            LOGGER.error("Sipariş {} için fatura oluşturma sürecinde hata.", event.getOrderId(), e);
            throw new IllegalStateException("Fatura oluşturma işlemi başarısız oldu", e);
        }
    }
}