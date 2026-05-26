package com.sellora.core.application.usecases;

import com.sellora.core.domain.entities.Category;
import com.sellora.core.domain.entities.Product;
import com.sellora.core.domain.entities.Store;
import com.sellora.core.domain.specifications.ProductSpecification;
import com.sellora.core.infrastructure.persistence.*;
import com.sellora.core.presentation.dtos.CreateProductDto;
import com.sellora.core.presentation.dtos.ProductResponseDto;
import com.sellora.core.presentation.exceptions.BadRequestException;
import com.sellora.core.presentation.exceptions.ConflictException;
import com.sellora.core.presentation.exceptions.ForbiddenException;
import com.sellora.core.presentation.exceptions.ResourceNotFoundException;
import com.sellora.core.presentation.exceptions.UnauthorizedException; // Додано
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductService {

  private final ProductRepository productRepository;
  private final StoreRepository storeRepository;
  private final CategoryRepository categoryRepository;
  private final GroupBuySessionRepository groupBuySessionRepository;
  private final UserFavoriteRepository userFavoriteRepository; // НОВИЙ РЕПОЗИТОРІЙ

  // --- ХЕЛПЕР ДЛЯ БЕЗПЕЧНОГО ОТРИМАННЯ ID ---
  private Long getCurrentUserIdOrNull() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
      return (Long) auth.getPrincipal();
    }
    return null;
  }

  @Transactional
  public Product createProduct(CreateProductDto dto) {
    Long currentUserId = getCurrentUserIdOrNull();
    if (currentUserId == null) throw new UnauthorizedException("Потрібна авторизація");

    Store currentStore = storeRepository.findByOwnerId(currentUserId)
      .orElseThrow(() -> new BadRequestException("У вас немає активного магазину. Спочатку створіть магазин!"));

    if (dto.groupPrice().compareTo(dto.standardPrice()) >= 0) {
      throw new BadRequestException("Ціна групової покупки має бути меншою за стандартну ціну");
    }

    Product product = new Product();
    product.setTitle(dto.title());
    product.setDescription(dto.description());
    product.setStandardPrice(dto.standardPrice());
    product.setGroupPrice(dto.groupPrice());
    product.setGroupTargetSize(dto.groupTargetSize());
    product.setStockQuantity(dto.stockQuantity());
    product.setStatus(dto.stockQuantity() == 0 ? "OUT_OF_STOCK" : "ACTIVE");
    product.setCategoryId(dto.categoryId());
    product.setMerchantId(currentStore.getId());
    product.setAttributes(dto.attributes() != null ? dto.attributes() : Map.of());
    product.setImages(dto.images() != null ? dto.images() : List.of());

    return productRepository.save(product);
  }

  // --- ОНОВЛЕНИЙ МЕТОД ФІЛЬТРАЦІЇ ---
  public Page<ProductResponseDto> filterProducts(
    String keyword, BigDecimal minPrice, BigDecimal maxPrice, Long categoryId,
    String status, Long storeId, String groupMode, boolean onlyFavorites,
    int page, int size, String sortBy, String sortDir) {

    Long currentUserId = getCurrentUserIdOrNull();

    // Захист: якщо хочуть улюблені, але юзер не авторизований - кидаємо красиву 401
    if (onlyFavorites && currentUserId == null) {
      throw new UnauthorizedException("Для перегляду улюблених товарів потрібно авторизуватись");
    }

    Specification<Product> spec = Specification.where(ProductSpecification.hasTitle(keyword))
      .and(ProductSpecification.priceGreaterThanOrEqual(minPrice))
      .and(ProductSpecification.priceLessThanOrEqual(maxPrice))
      .and(ProductSpecification.hasCategoryId(categoryId))
      .and(ProductSpecification.hasStatus(status))
      .and(ProductSpecification.hasStoreId(storeId))
      .and(ProductSpecification.hasGroupSession(groupMode));

    // Якщо увімкнено фільтр - додаємо Specification з сабкверією
    if (onlyFavorites) {
      spec = spec.and(ProductSpecification.isFavorite(currentUserId));
    }

    Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
    Pageable pageable = PageRequest.of(page, size, sort);

    Page<Product> productsPage = productRepository.findAll(spec, pageable);

    // Використовуємо наш новий метод для швидкого мапінгу
    return mapToProductResponseDtoPage(productsPage, currentUserId);
  }

  public Page<ProductResponseDto> getProductsByMerchant(Long merchantId, int page, int size, String sortBy, String sortDir) {
    Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
    Pageable pageable = PageRequest.of(page, size, sort);

    Page<Product> productsPage = productRepository.findAllByMerchantId(merchantId, pageable);
    return mapToProductResponseDtoPage(productsPage, getCurrentUserIdOrNull());
  }

  // --- ХЕЛПЕР ДЛЯ ПАКЕТНОЇ ПЕРЕВІРКИ УЛЮБЛЕНИХ ---
  // --- ХЕЛПЕР ДЛЯ ПАКЕТНОЇ ПЕРЕВІРКИ УЛЮБЛЕНИХ ---
  private Page<ProductResponseDto> mapToProductResponseDtoPage(Page<Product> productsPage, Long currentUserId) {
    List<Long> productIds = productsPage.getContent().stream().map(Product::getId).toList();

    // ВИПРАВЛЕННЯ: Ініціалізуємо змінну один раз за допомогою тернарного оператора
    final Set<Long> favoriteIds = (currentUserId != null && !productIds.isEmpty())
      ? userFavoriteRepository.findFavoriteProductIdsByUserAndProducts(currentUserId, productIds)
      : new HashSet<>();

    // Мапінг DTO з підставленим isFavorite
    return productsPage.map(product -> new ProductResponseDto(
      product.getId(), product.getTitle(), product.getDescription(),
      product.getStandardPrice(), product.getGroupPrice(), product.getGroupTargetSize(),
      product.getStockQuantity(), product.getCategoryId(), null, product.getMerchantId(), null,
      product.getAttributes(), product.getImages(), product.getStatus(),
      favoriteIds.contains(product.getId())
    ));
  }

  public ProductResponseDto getProductById(Long id, boolean full) {
    Product product = productRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Товар не знайдено"));

    String storeName = null;
    String categoryName = null;
    boolean isFavorite = false;

    if (full) {
      storeName = storeRepository.findById(product.getMerchantId()).map(Store::getName).orElse(null);
      categoryName = categoryRepository.findById(product.getCategoryId()).map(Category::getName).orElse(null);
    }

    Long currentUserId = getCurrentUserIdOrNull();
    if (currentUserId != null) {
      isFavorite = userFavoriteRepository.existsByUserIdAndProductId(currentUserId, id);
    }

    return new ProductResponseDto(
      product.getId(), product.getTitle(), product.getDescription(),
      product.getStandardPrice(), product.getGroupPrice(), product.getGroupTargetSize(),
      product.getStockQuantity(), product.getCategoryId(), categoryName,
      product.getMerchantId(), storeName, product.getAttributes(), product.getImages(), product.getStatus(),
      isFavorite
    );
  }

  @Transactional
  public Product updateProduct(Long productId, com.sellora.core.presentation.dtos.UpdateProductDto dto) {
    Long currentUserId = getCurrentUserIdOrNull();
    if (currentUserId == null) throw new UnauthorizedException("Потрібна авторизація");
    Store currentStore = storeRepository.findByOwnerId(currentUserId)
      .orElseThrow(() -> new BadRequestException("У вас немає активного магазину"));

    if (dto.groupPrice().compareTo(dto.standardPrice()) >= 0) {
      throw new BadRequestException("Ціна групової покупки має бути меншою за стандартну ціну");
    }

    Product product = productRepository.findById(productId)
      .orElseThrow(() -> new ResourceNotFoundException("Товар не знайдено"));

    if (!product.getMerchantId().equals(currentStore.getId())) {
      throw new ForbiddenException("Ви не можете редагувати чужий товар");
    }

    if (!product.getCategoryId().equals(dto.categoryId()) && !categoryRepository.existsById(dto.categoryId())) {
      throw new BadRequestException("Вибраної категорії не існує");
    }

    product.setTitle(dto.title());
    product.setDescription(dto.description());
    product.setCategoryId(dto.categoryId());
    product.setStandardPrice(dto.standardPrice());
    product.setGroupPrice(dto.groupPrice());
    product.setGroupTargetSize(dto.groupTargetSize());
    product.setImages(dto.images() != null ? dto.images() : List.of());
    product.setAttributes(dto.attributes() != null ? dto.attributes() : Map.of());

    int newStock = dto.stockQuantity();
    product.setStockQuantity(newStock);
    if (newStock == 0) {
      product.setStatus("OUT_OF_STOCK");
    } else if (newStock > 0 && "OUT_OF_STOCK".equals(product.getStatus())) {
      product.setStatus("ACTIVE");
    }

    return productRepository.save(product);
  }

  @Transactional
  public void deleteProduct(Long productId) {
    Long currentUserId = getCurrentUserIdOrNull();
    if (currentUserId == null) throw new UnauthorizedException("Потрібна авторизація");
    Store currentStore = storeRepository.findByOwnerId(currentUserId)
      .orElseThrow(() -> new BadRequestException("У вас немає активного магазину"));

    Product product = productRepository.findById(productId)
      .orElseThrow(() -> new ResourceNotFoundException("Товар не знайдено"));

    if (!product.getMerchantId().equals(currentStore.getId())) {
      throw new ForbiddenException("Ви не можете видалити чужий товар");
    }

    if (groupBuySessionRepository.existsByProductIdAndStatus(productId, "ACTIVE")) {
      throw new ConflictException("Неможливо видалити товар: існують активні сесії групових покупок");
    }

    productRepository.delete(product);
  }

  @Transactional
  public Product updateProductStatus(Long productId, String newStatus) {
    Long currentUserId = getCurrentUserIdOrNull();
    if (currentUserId == null) throw new UnauthorizedException("Потрібна авторизація");
    Store currentStore = storeRepository.findByOwnerId(currentUserId)
      .orElseThrow(() -> new BadRequestException("У вас немає активного магазину"));

    Product product = productRepository.findById(productId)
      .orElseThrow(() -> new ResourceNotFoundException("Товар не знайдено"));

    if (!product.getMerchantId().equals(currentStore.getId())) {
      throw new ForbiddenException("Ви не можете редагувати чужий товар");
    }

    if ("ARCHIVED".equalsIgnoreCase(newStatus)) {
      product.setStatus("ARCHIVED");
    } else if ("ACTIVE".equalsIgnoreCase(newStatus)) {
      if (product.getStockQuantity() == 0) {
        throw new BadRequestException("Не можна активувати товар з кількістю 0 на складі");
      }
      product.setStatus("ACTIVE");
    }

    return productRepository.save(product);
  }
}
