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
import com.sellora.core.presentation.exceptions.UnauthorizedException;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

  private final ProductRepository productRepository;
  private final StoreRepository storeRepository;
  private final CategoryRepository categoryRepository;
  private final GroupBuySessionRepository groupBuySessionRepository;
  private final ProductReviewRepository productReviewRepository; // Додай це
  private final UserFavoriteRepository userFavoriteRepository;

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

  public Page<ProductResponseDto> filterProducts(
    String keyword, BigDecimal minPrice, BigDecimal maxPrice, Long categoryId,
    String status, Long storeId, String groupMode, boolean onlyFavorites,
    int page, int size, String sortBy, String sortDir) {

    Long currentUserId = getCurrentUserIdOrNull();

    if (onlyFavorites && currentUserId == null) {
      throw new UnauthorizedException("Для перегляду улюблених товарів потрібно авторизуватись");
    }

    List<Long> categoryIdsToSearch = null;
    if (categoryId != null) {
      categoryIdsToSearch = categoryRepository.findCategoryTreeIds(categoryId);
    }

    Specification<Product> spec = Specification.where(ProductSpecification.hasTitle(keyword))
      .and(ProductSpecification.priceGreaterThanOrEqual(minPrice))
      .and(ProductSpecification.priceLessThanOrEqual(maxPrice))
      .and(ProductSpecification.hasCategoryIdIn(categoryIdsToSearch))
      .and(ProductSpecification.hasStatus(status))
      .and(ProductSpecification.hasStoreId(storeId))
      .and(ProductSpecification.hasGroupSession(groupMode));

    if (onlyFavorites) {
      spec = spec.and(ProductSpecification.isFavorite(currentUserId));
    }

    Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
    Pageable pageable = PageRequest.of(page, size, sort);

    Page<Product> productsPage = productRepository.findAll(spec, pageable);

    return mapToProductResponseDtoPage(productsPage, currentUserId);
  }

  public Page<ProductResponseDto> getProductsByMerchant(Long merchantId, int page, int size, String sortBy, String sortDir) {
    Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
    Pageable pageable = PageRequest.of(page, size, sort);

    Page<Product> productsPage = productRepository.findAllByMerchantId(merchantId, pageable);
    return mapToProductResponseDtoPage(productsPage, getCurrentUserIdOrNull());
  }

  private Page<ProductResponseDto> mapToProductResponseDtoPage(Page<Product> productsPage, Long currentUserId) {
    List<Product> products = productsPage.getContent();
    if (products.isEmpty()) {
      return Page.empty(productsPage.getPageable());
    }

    List<Long> productIds = products.stream().map(Product::getId).toList();

    final Set<Long> favoriteIds = (currentUserId != null)
      ? userFavoriteRepository.findFavoriteProductIdsByUserAndProducts(currentUserId, productIds)
      : new HashSet<>();

    Set<Long> categoryIds = products.stream().map(Product::getCategoryId).collect(Collectors.toSet());
    Map<Long, String> categoryNames = categoryRepository.findAllById(categoryIds).stream()
      .collect(Collectors.toMap(Category::getId, Category::getName));

    Set<Long> merchantIds = products.stream().map(Product::getMerchantId).collect(Collectors.toSet());
    Map<Long, String> storeNames = storeRepository.findAllById(merchantIds).stream()
      .collect(Collectors.toMap(Store::getId, Store::getName));

    return productsPage.map(product -> new ProductResponseDto(
      product.getId(), product.getTitle(), product.getDescription(),
      product.getStandardPrice(), product.getGroupPrice(), product.getGroupTargetSize(),
      product.getStockQuantity(),
      product.getCategoryId(),
      categoryNames.get(product.getCategoryId()),
      product.getMerchantId(),
      storeNames.get(product.getMerchantId()),
      product.getAttributes(), product.getImages(), product.getStatus(),
      favoriteIds.contains(product.getId()),
      null,
      product.getRating(),
      product.getReviewsCount(),
      null
    ));
  }

  public ProductResponseDto getProductById(Long id, boolean full) {
    Product product = productRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Товар не знайдено"));

    String storeName = null;
    String categoryName = null;
    boolean isFavorite = false;
    String userActiveSessionUuid = null;
    List<com.sellora.core.presentation.dtos.ReviewWithUserDto> reviews = null; // НОВЕ

    if (full) {
      storeName = storeRepository.findById(product.getMerchantId()).map(Store::getName).orElse(null);
      categoryName = categoryRepository.findById(product.getCategoryId()).map(Category::getName).orElse(null);
      // Завантажуємо відгуки тільки на сторінці самого товару
      reviews = productReviewRepository.findReviewsWithUserEmailByProductId(id);
    }

    Long currentUserId = getCurrentUserIdOrNull();
    if (currentUserId != null) {
      isFavorite = userFavoriteRepository.existsByUserIdAndProductId(currentUserId, id);
      userActiveSessionUuid = groupBuySessionRepository.findActiveSessionUuidForUserAndProduct(currentUserId, id).orElse(null);
    }

    return new ProductResponseDto(
      product.getId(), product.getTitle(), product.getDescription(),
      product.getStandardPrice(), product.getGroupPrice(), product.getGroupTargetSize(),
      product.getStockQuantity(), product.getCategoryId(), categoryName,
      product.getMerchantId(), storeName, product.getAttributes(), product.getImages(), product.getStatus(),
      isFavorite,
      userActiveSessionUuid,
      product.getRating(),       // <--- ДОДАНО
      product.getReviewsCount(), // <--- ДОДАНО
      reviews                    // <--- ДОДАНО
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
