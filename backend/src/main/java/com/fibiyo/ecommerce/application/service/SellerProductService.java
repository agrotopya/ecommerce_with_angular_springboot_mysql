package com.fibiyo.ecommerce.application.service;

import com.fibiyo.ecommerce.application.dto.*; // Gerekli DTO importları
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;


/**
 * Satıcıların kendi ürünleriyle ilgili gerçekleştirebileceği işlemler için servis arayüzü.
 * Bu işlemler ProductService içinde de olabilirdi ama ayırmak daha düzenli olabilir.
 */
public interface SellerProductService {

    /**
     * Mevcut satıcının bir ürününü oluşturur.
     * Ürün varsayılan olarak onaylanmamış ama aktif başlar.
     * @param productRequest Ürün bilgileri.
     * @return Oluşturulan ürünün bilgileri.
     */
    ProductResponse createProductBySeller(ProductRequest productRequest);

    /**
     * Mevcut satıcının belirli bir ürününü günceller.
     * @param productId Güncellenecek ürünün ID'si.
     * @param productRequest Yeni ürün bilgileri.
     * @return Güncellenmiş ürünün bilgileri.
     * @throws ResourceNotFoundException Ürün bulunamazsa.
     * @throws ForbiddenException Satıcı ürüne sahip değilse.
     */
    ProductResponse updateProductBySeller(Long productId, ProductRequest productRequest);

    /**
     * Mevcut satıcının belirli bir ürününü siler.
     * @param productId Silinecek ürünün ID'si.
     * @throws ResourceNotFoundException Ürün bulunamazsa.
     * @throws ForbiddenException Satıcı ürüne sahip değilse.
     */
    void deleteProductBySeller(Long productId);

    /**
     * Mevcut satıcının tüm ürünlerini sayfalı olarak getirir.
     * @param pageable Sayfalama bilgileri.
     * @return Satıcının ürünlerinin sayfası.
     */
    Page<ProductResponse> findProductsByCurrentSeller(Pageable pageable);

    /**
     * Mevcut satıcının belirli bir ürününün detayını getirir.
     * (Sahiplik kontrolü ile)
     * @param productId Ürün ID'si.
     * @return Ürün detayları.
     */
     ProductResponse findSellerProductById(Long productId);


    /**
     * Mevcut satıcının bir ürününün aktiflik durumunu ayarlar.
     * @param productId Ürün ID'si.
     * @param isActive Yeni aktiflik durumu.
     * @return Güncellenmiş ürün bilgileri.
     */
    ProductResponse setSellerProductActiveStatus(Long productId, boolean isActive);

    /**
     * Mevcut satıcının bir ürünü için görsel yükler ve ürün bilgisini günceller.
     * @param productId Ürün ID'si.
     * @param file Yüklenecek görsel dosyası.
     * @return Güncellenmiş ürün bilgileri.
     */
    ProductResponse updateProductImageBySeller(Long productId, MultipartFile file);

    /**
     * Mevcut satıcının isteği üzerine AI ile bir ürün görseli üretir/düzenler.
     * Kota kontrolü yapar ve kotayı düşürür.
     * @param request Prompt ve referans görsel bilgisini içerir.
     * @return Üretilen görsel(ler)in URL'sini ve kalan kotayı içeren yanıt.
     */
    AiImageGenerationResponse generateAiImageForSeller(AiImageGenerationRequest request);

    /**
     * AI ile üretilen bir görseli, belirli bir ürünün ana görseli olarak ayarlar.
     * @param request Ürün ID'sini ve seçilen AI görsel URL'sini içerir.
     * @return Güncellenmiş ürün bilgileri.
     */
    ProductResponse setAiImageAsProductImage(SetAiImageAsProductRequest request);

    /**
     * Mevcut satıcının bir ürününe yeni bir görsel ekler.
     * @param productId Ürün ID'si.
     * @param imageFile Yüklenecek görsel dosyası.
     * @param altText Görsel için alternatif metin.
     * @param isPrimarySet Bu görselin ana resim olup olmayacağı.
     * @return Eklenen görselin DTO'su.
     */
    ProductImageDto addProductImage(Long productId, MultipartFile imageFile, String altText, boolean isPrimarySet);

    // TODO: deleteProductImage ve setPrimaryProductImage için de interface metodları eklenecek.
}
