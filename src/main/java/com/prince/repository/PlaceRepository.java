package com.prince.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.prince.model.Place;
import com.prince.model.enums.Category;


public interface PlaceRepository extends JpaRepository<Place, Long>{
    //find active places, ignore soft deleted
    //pageable: pagination , 10 at a time
    Page<Place> findByActiveTrue(Pageable pageable);

    //find by category
    Page<Place> findByCategoryAndActiveTrue(Category category, Pageable pageable);
}
