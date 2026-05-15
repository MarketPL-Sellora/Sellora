//package com.sellora.core.application.usecases;
//
//import com.sellora.core.domain.entities.GroupBuySession;
//import com.sellora.core.domain.entities.GroupMember;
//import com.sellora.core.domain.entities.Product;
//import com.sellora.core.infrastructure.persistence.GroupBuySessionRepository;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
//import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
//
//import java.math.BigDecimal;
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@DataJpaTest
//class GroupBuySessionRepositoryTest {
//
//  @Autowired
//  private TestEntityManager entityManager;
//
//  @Autowired
//  private GroupBuySessionRepository sessionRepository;
//
//  @Test
//  void findAllByUserIdAndStatus_ReturnsCorrectSessions() {
//    // Arrange
//    Long userId = 1L;
//
//    Product product = new Product();
//    product.setTitle("Test Item");
//    product = entityManager.persistFlushFind(product);
//
//    GroupBuySession session = new GroupBuySession();
//    session.setUuid("unique-uuid-123");
//    session.setProductId(product.getId());
//    session.setStatus("ACTIVE");
//    session = entityManager.persistFlushFind(session);
//
//    GroupMember member = new GroupMember();
//    member.setSessionId(session.getId());
//    member.setUserId(userId);
//    entityManager.persistFlushFind(member);
//
//    // Act
//    List<GroupBuySession> result = sessionRepository.findAllByUserIdAndStatus(userId, "ACTIVE");
//
//    // Assert
//    assertThat(result).hasSize(1);
//    assertThat(result.get(0).getUuid()).isEqualTo("unique-uuid-123");
//  }
//
//  @Test
//  void countActiveSessionsForMerchant_ReturnsCorrectCount() {
//    // Arrange
//    Long merchantId = 5L;
//
//    Product product = new Product();
//    product.setMerchantId(merchantId);
//    product = entityManager.persistFlushFind(product);
//
//    GroupBuySession activeSession = new GroupBuySession();
//    activeSession.setProductId(product.getId());
//    activeSession.setStatus("ACTIVE");
//    entityManager.persist(activeSession);
//
//    GroupBuySession completedSession = new GroupBuySession();
//    completedSession.setProductId(product.getId());
//    completedSession.setStatus("COMPLETED");
//    entityManager.persist(completedSession);
//
//    entityManager.flush();
//
//    // Act
//    long count = sessionRepository.countActiveSessionsForMerchant(merchantId);
//
//    // Assert
//    assertThat(count).isEqualTo(1L);
//  }
//}
