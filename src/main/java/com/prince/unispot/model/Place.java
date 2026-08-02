package com.prince.unispot.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "places")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "place_seq")
    @SequenceGenerator(name = "place_seq", sequenceName = "place_id_seq", allocationSize = 10)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, length = 300)
    //no need @Size here, better in DTO validation, fail-fast
    private String description;

    //will do enum in later version
    @Column(nullable = false)
    private String category;
    //geometry data types for location

    //optimistic concurrency
    @Version
    private Integer version;

    //this wont' store FK
    @OneToMany(mappedBy = "place", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Review> reviews = new ArrayList<>(); 

    //storing cloudinary image urls directly for fast reads
    @ElementCollection //im storing these urls as list of items not an Entity with PK
    @CollectionTable(name = "place_images", joinColumns = @JoinColumn(name = "place_id")) //@joincol for FK, relation
    @Column(name = "image_url") //just two col now, one FK place_id and this image_url
    @Builder.Default //prevent NPE, use this initialise if i dont provide some in builder
    private List<String> imageUrls = new ArrayList<>();
    //not preferred if use delete, as no id, hibernate dlt and insert, no independent lifecyle, query, dirtycheck
}
