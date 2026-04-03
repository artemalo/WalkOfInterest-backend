package sfedu.ictis.woi.projection;

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

    // Coordinate
    Double getLat();
    Double getLon();
}