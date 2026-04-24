package sfedu.ictis.woi.model.entity.id;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
public class PoisLanguesId implements Serializable {
    private Long poi;
    private String langue;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PoisLanguesId that = (PoisLanguesId) o;
        return Objects.equals(poi, that.poi) && Objects.equals(langue, that.langue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(poi, langue);
    }
}