package com.sellora.core.application.usecases;

import com.sellora.core.domain.entities.Cart;
import com.sellora.core.domain.entities.CartItem;
import com.sellora.core.domain.entities.Product;
import com.sellora.core.infrastructure.persistence.CartItemRepository;
import com.sellora.core.infrastructure.persistence.CartRepository;
import com.sellora.core.infrastructure.persistence.ProductRepository;
import com.sellora.core.presentation.dtos.*;
import com.sellora.core.presentation.exceptions.BadRequestException;
import com.sellora.core.presentation.exceptions.ResourceNotFoundException;
import com.sellora.core.presentation.exceptions.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {
  private final CartRepository cartRepository;
  private final CartItemRepository cartItemRepository;
  private final ProductRepository productRepository;

  private Long getCurrentUserId() {
    Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (principal == null || principal.equals("anonymousUser")) {
      throw new UnauthorizedException("Потрібна авторизація");
    }
    return (Long) principal;
  }

  // Хелпер для розрахунку загальної суми кошика
  private BigDecimal calculateTotalAmount(Long cartId, Long targetProductId, Integer updatedQuantity) {
    List<CartItem> allItems = cartItemRepository.findByCartId(cartId);
    BigDecimal totalAmount = BigDecimal.ZERO;

    for (CartItem item : allItems) {
      int qty = (targetProductId != null && item.getProduct().getId().equals(targetProductId))
        ? updatedQuantity
        : item.getQuantity();
      BigDecimal itemPrice = item.getProduct().getStandardPrice() != null ? item.getProduct().getStandardPrice() : BigDecimal.ZERO;
      totalAmount = totalAmount.add(itemPrice.multiply(BigDecimal.valueOf(qty)));
    }
    return totalAmount;
  }

  private Cart getOrCreateCart(Long userId) {
    return cartRepository.findByUserId(userId).orElseGet(() -> {
      Cart newCart = new Cart();
      newCart.setUserId(userId);
      return cartRepository.save(newCart);
    });
  }

  @Transactional
  public AddToCartResponseDto addToCart(AddToCartDto dto) {
    Long userId = getCurrentUserId();
    Product product = productRepository.findById(dto.productId())
      .orElseThrow(() -> new ResourceNotFoundException("Товар не знайдено"));

    if (!"ACTIVE".equalsIgnoreCase(product.getStatus())) {
      throw new BadRequestException("Цей товар недоступний для покупки");
    }

    Cart cart = getOrCreateCart(userId);
    CartItem cartItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId()).orElse(null);

    int newQuantity = (cartItem != null ? cartItem.getQuantity() : 0) + dto.quantity();

    if (newQuantity > product.getStockQuantity()) {
      throw new BadRequestException("Недостатньо товару на складі. Доступно: " + product.getStockQuantity());
    }

    if (cartItem != null) {
      cartItem.setQuantity(newQuantity);
    } else {
      cartItem = new CartItem();
      cartItem.setCart(cart);
      cartItem.setProduct(product);
      cartItem.setQuantity(newQuantity);
    }
    cartItemRepository.save(cartItem);

    BigDecimal price = product.getStandardPrice() != null ? product.getStandardPrice() : BigDecimal.ZERO;
    BigDecimal subTotal = price.multiply(BigDecimal.valueOf(newQuantity));
    BigDecimal totalAmount = calculateTotalAmount(cart.getId(), product.getId(), newQuantity);

    return new AddToCartResponseDto(product.getId(), newQuantity, subTotal, totalAmount);
  }

  // --- 1. Оновлення кількості (PATCH) ---
  @Transactional
  public AddToCartResponseDto updateQuantity(Long productId, UpdateCartQuantityDto dto) {
    Long userId = getCurrentUserId();
    Product product = productRepository.findById(productId)
      .orElseThrow(() -> new ResourceNotFoundException("Товар не знайдено"));

    Cart cart = getOrCreateCart(userId);
    CartItem cartItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
      .orElseThrow(() -> new ResourceNotFoundException("Цього товару немає у вашому кошику"));

    // Валідація залишків (Тут ми не додаємо, а повністю перезаписуємо кількість)
    if (dto.newQuantity() > product.getStockQuantity()) {
      throw new BadRequestException("Недостатньо товару на складі. Доступно: " + product.getStockQuantity());
    }

    cartItem.setQuantity(dto.newQuantity());
    cartItemRepository.save(cartItem);

    BigDecimal price = product.getStandardPrice() != null ? product.getStandardPrice() : BigDecimal.ZERO;
    BigDecimal subTotal = price.multiply(BigDecimal.valueOf(dto.newQuantity()));
    BigDecimal totalAmount = calculateTotalAmount(cart.getId(), productId, dto.newQuantity());

    return new AddToCartResponseDto(productId, dto.newQuantity(), subTotal, totalAmount);
  }

  // --- 2. Видалення одного товару (DELETE /cart/{id}) ---
  @Transactional
  public void removeItem(Long productId) {
    Long userId = getCurrentUserId();
    if (!productRepository.existsById(productId)) {
      throw new ResourceNotFoundException("Товар не знайдено");
    }

    Cart cart = getOrCreateCart(userId);
    cartItemRepository.deleteByCartIdAndProductId(cart.getId(), productId);
  }

  // --- 3. Очищення всього кошика (DELETE /cart) ---
  @Transactional
  public void clearCart() {
    Long userId = getCurrentUserId();
    Cart cart = getOrCreateCart(userId);
    cartItemRepository.deleteAllByCartId(cart.getId());
  }

  // --- 4. Отримання кошика (GET /cart) ---
  @Transactional(readOnly = true)
  public CartResponseDto getCart() {
    Long userId = getCurrentUserId();
    Cart cart = cartRepository.findByUserId(userId).orElse(null);

    if (cart == null) {
      return new CartResponseDto(List.of(), BigDecimal.ZERO);
    }

    List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
    BigDecimal totalAmount = BigDecimal.ZERO;

    List<CartItemResponseDto> itemDtos = items.stream().map(item -> {
      Product product = item.getProduct();
      BigDecimal price = product.getStandardPrice() != null ? product.getStandardPrice() : BigDecimal.ZERO;
      BigDecimal subTotal = price.multiply(BigDecimal.valueOf(item.getQuantity()));

      String image = (product.getImages() != null && !product.getImages().isEmpty()) ? product.getImages().get(0) : null;

      return new CartItemResponseDto(
        product.getId(),
        product.getTitle(),
        image,
        price,
        item.getQuantity(),
        subTotal
      );
    }).toList();

    for (CartItemResponseDto dto : itemDtos) {
      totalAmount = totalAmount.add(dto.subTotal());
    }

    return new CartResponseDto(itemDtos, totalAmount);
  }
}
