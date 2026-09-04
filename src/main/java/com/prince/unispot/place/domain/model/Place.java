package com.prince.unispot.place.domain.model;

import java.util.HashSet;
import java.util.Set;

import com.prince.unispot.core.domain.AuditableEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "places", indexes = {
    @Index(name = "idx_place_category", columnList = "category")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Place extends AuditableEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "place_seq")
    @SequenceGenerator(name = "place_seq", sequenceName = "place_id_seq", allocationSize = 10)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, length = 300)
    //no need @Size here, better in DTO validation, fail-fast
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Category category;
    //geometry data types for location

    //optimistic concurrency
    @Version
    private Integer version;



    @ElementCollection
    @CollectionTable(name = "place_images", joinColumns = @JoinColumn(name = "place_id"))
    @Column(name = "image_url")
    @Builder.Default
    private Set<String> imageUrls = new HashSet<>(); //prevents multiple bagfetch
}



// @ElementCollection //im storing these urls as list of items not an Entity with PK
// @CollectionTable(name = "place_images", joinColumns = @JoinColumn(name = "place_id")) //@joincol for FK, relation
// @Column(name = "image_url") //just two col now, one FK place_id and this image_url
// // private List<String> imageUrls = new ArrayList<>();
//not preferred if use delete, as no id, hibernate dlt and insert, no independent lifecyle, query, dirtycheck


//removing these to prevent jvm crash on delete as orphan removing
    //this wont' store FK
    // @OneToMany(mappedBy = "place", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true, fetch = FetchType.LAZY)
    // @Builder.Default //prevent NPE, use this initialise if i dont provide some in builder
    // private List<Review> reviews = new ArrayList<>(); 