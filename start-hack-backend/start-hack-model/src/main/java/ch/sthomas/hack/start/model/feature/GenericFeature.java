package ch.sthomas.hack.start.model.feature;

import ch.sthomas.hack.start.model.feature.annotations.DeserializableJtsGeometry;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.locationtech.jts.geom.Geometry;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GenericFeature<P, T extends GenericFeature<P, T>> {

    @JsonInclude private String id;

    @JsonInclude private String type;

    @JsonInclude private P properties;

    @JsonInclude private Geometry geometry;

    public String getId() {
        return id;
    }

    public T setId(final String id) {
        this.id = id;
        //noinspection unchecked
        return (T) this;
    }

    public P getProperties() {
        return properties;
    }

    public T setProperties(final P properties) {
        this.properties = properties;
        //noinspection unchecked
        return (T) this;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    @DeserializableJtsGeometry
    public T setGeometry(final Geometry geometry) {
        this.geometry = geometry;
        //noinspection unchecked
        return (T) this;
    }

    public String getType() {
        return type;
    }

    public T setType(final String type) {
        this.type = type;
        //noinspection unchecked
        return (T) this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("id", id)
                .append("type", type)
                .append("properties", properties)
                .append("geometry", geometry)
                .toString();
    }
}
