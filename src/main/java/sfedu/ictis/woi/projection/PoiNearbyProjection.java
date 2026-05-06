package sfedu.ictis.woi.projection;


public interface PoiNearbyProjection {
    Long getId();
    String getName();
    String getCategoryName();
    String getSubcategoryName();
    Double getLat();
    Double getLon();
    Double getDistanceMeters();
}