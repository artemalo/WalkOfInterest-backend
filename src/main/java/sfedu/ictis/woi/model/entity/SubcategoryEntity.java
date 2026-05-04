package sfedu.ictis.woi.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "subcategories")
@Getter
@Setter
public class SubcategoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "subcategory_key")
    private String key;

    @Column(name = "subcategory_value")
    private String value;

    @Column(name = "subcategory_name")
    private String name;

    private Double weight;
    private Double interest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private CategoryEntity category;

    @ManyToMany(mappedBy = "subcategories")
    private Set<PoiEntity> pois = new HashSet<>();
}