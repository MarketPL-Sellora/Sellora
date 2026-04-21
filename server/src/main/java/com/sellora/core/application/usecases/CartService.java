package com.sellora.core.application.usecases;

import com.sellora.core.domain.entities.Cart;
import com.sellora.core.domain.entities.CartItem;
import com.sellora.core.domain.entities.Product;
import com.sellora.core.infrastructure.persistence.CartItemRepository;
import com.sellora.core.infrastructure.persistence.CartRepository;
import com.sellora.core.infrastructure.persistence.ProductRepository;
import com.sellora.core.presentation.dtos.AddToCartDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartService {
  private final CartRepository cartRepository;
  private final CartItemRepository cartItemRepository;
  private final ProductRepository productRepository;

  @Transactional
  public CartItem addToCart(AddToCartDto dto, Long userId) {
    // 1. Шукаємо кошик юзера, або створюємо новий, якщо це перша покупка
    Cart cart = cartRepository.findByUserId(userId)
      .orElseGet(() -> {
        Cart newCart = new Cart();
        newCart.setUserId(userId);
        return cartRepository.save(newCart);
      });

    // 2. Отримуємо товар з бази (Happy Path: припускаємо, що він точно існує)
    Product product = productRepository.findById(dto.productId())
      .orElseThrow(() -> new RuntimeException("Product not found with id: " + dto.productId()));

    // 3. Перевіряємо, чи є вже цей товар у кошику
    return cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())
      .map(existingItem -> {
        // Якщо товар є — просто збільшуємо кількість (UPDATE)
        existingItem.setQuantity(existingItem.getQuantity() + dto.quantity());
        return cartItemRepository.save(existingItem);
      })
      .orElseGet(() -> {
        // Якщо товару немає — створюємо новий запис (INSERT)
        CartItem newItem = new CartItem();
        newItem.setCart(cart);
        newItem.setProduct(product);
        newItem.setQuantity(dto.quantity());
        return cartItemRepository.save(newItem);
      });
  }
}
