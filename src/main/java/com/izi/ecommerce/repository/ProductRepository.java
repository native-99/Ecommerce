package com.izi.ecommerce.repository;

import com.izi.ecommerce.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query(
            value = """
            SELECT * FROM product
            WHERE lower(name) LIKE lower(concat('%', :name, '%'))
            """,
            countQuery = """
            SELECT count(*) FROM product
            WHERE lower(name) LIKE lower(concat('%', :name, '%'))
            """,
            nativeQuery = true
    )
    Page<Product> findByNameAndPageable(@Param("name") String name, Pageable pageable);

    @Query(
            value = """
            SELECT DISTINCT p.* FROM product p
            JOIN product_category pc ON p.product_id = pc.product_id
            JOIN category c ON pc.category_id = c.category_id
            WHERE lower(c.name) = lower(:categoryName)
            """,
            countQuery = """
            SELECT count(DISTINCT p.product_id) FROM product p
            JOIN product_category pc ON p.product_id = pc.product_id
            JOIN category c ON pc.category_id = c.category_id
            WHERE lower(c.name) = lower(:categoryName)
            """,
            nativeQuery = true
    )
    Page<Product> findByCategoryName(@Param("categoryName") String categoryName, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            value = """
            SELECT * FROM product
            WHERE product_id = :id
            """,
            nativeQuery = true
    )
    Optional<Product> findByIdWithPessimisticLock(@Param("id") Long id);
}
