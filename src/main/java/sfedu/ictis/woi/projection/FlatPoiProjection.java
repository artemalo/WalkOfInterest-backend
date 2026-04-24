package sfedu.ictis.woi.projection;


import sfedu.ictis.woi.model.entity.PoiStatus;

public interface FlatPoiProjection {
    // categories
    Integer getCatId();
    String getCatName();
    String getCatDescription();
    String getCatIcon();

    // subcategories
    Integer getSubId();
    String getSubName();
    Double getWeight();
    String getSubDescription();
    String getSubIcon();

    // pois
    Long getPoiId();
    String getPoiLang();
    String getPoiName();
    String getPoiDesc();
    Double getRate();
    Integer getCount();
    PoiStatus getStatus();
    Boolean getUserId();

    // Coordinate
    Double getLat();
    Double getLon();
}