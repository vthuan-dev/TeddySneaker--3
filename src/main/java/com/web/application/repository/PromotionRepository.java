package com.web.application.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.web.application.entity.Promotion;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

	@Query(nativeQuery = true, value = "SELECT * FROM promotion p " + "WHERE p.coupon_code LIKE CONCAT('%',?1,'%') "
			+ "AND p.name LIKE CONCAT('%',?2,'%') " + "AND p.is_public LIKE CONCAT('%',?3,'%') "
			+ "AND p.is_active LIKE CONCAT('%',?4,'%')")
	Page<Promotion> adminGetListPromotion(String code, String name, String publish, String active, Pageable pageable);

	@Query(nativeQuery = true, value = "SELECT * FROM promotion p WHERE p.is_active = 1 AND p.is_public = 1 AND expired_at > GETDATE()")
	Promotion checkHasPublicPromotion();

	Optional<Promotion> findByCouponCode(String code);

	@Query(nativeQuery = true, value = "SELECT  * FROM promotion p WHERE p.is_active = 'true' AND p.coupon_code = ?1")
	Promotion checkPromotion(String code);

	@Query(nativeQuery = true, value = "SELECT * FROM promotion p WHERE p.expired_at > GETDATE() AND p.is_active = 1")
	List<Promotion> getAllValidPromotion();

}
