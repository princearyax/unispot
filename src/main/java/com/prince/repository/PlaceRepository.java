package com.prince.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.prince.model.Place;
import com.prince.model.enums.Category;

@Repository
public interface PlaceRepository extends JpaRepository<Place, Long>{
    //find active places, ignore soft deleted
    //pageable: pagination , 10 at a time
    Page<Place> findByActiveTrue(Pageable pageable);

    //find by category
    Page<Place> findByCategoryAndActiveTrue(Category category, Pageable pageable);

    //custom jpql to search in desc. or name
    //calling lower hinders performance,,
    //  need fix
    @Query("SELECT p FROM Place p WHERE p.active = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Place> searchPlaces(@Param("keyword") String keyword, Pageable pageable);
}
