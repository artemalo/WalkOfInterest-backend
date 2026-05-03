package sfedu.ictis.woi.service.selection;

import lombok.Setter;
import sfedu.ictis.woi.model.dto.CategoryDTO;
import sfedu.ictis.woi.model.dto.PoiDTO;
import sfedu.ictis.woi.model.dto.SubCategoryDTO;


public final class Candidate {
    private final PoiDTO poi;
    private final SubCategoryDTO sub;
    private final CategoryDTO cat;

    private final double lat;
    private final double lon;

    private final double detourMeters;

    @Setter
    private double score;

    public Candidate(PoiDTO poi, SubCategoryDTO sub, CategoryDTO cat, double detourMeters) {
        this.poi = poi;
        this.sub = sub;
        this.cat = cat;
        this.lat = poi.getLat();
        this.lon = poi.getLon();
        this.detourMeters = detourMeters;
    }

    public PoiDTO poi() { return poi; }
    public SubCategoryDTO sub() { return sub; }
    public CategoryDTO cat() { return cat; }
    public double lat() { return lat; }
    public double lon() { return lon; }
    public double detourMeters() { return detourMeters; }
    public double score() { return score; }

    public Long id() { return poi.getId(); }
}