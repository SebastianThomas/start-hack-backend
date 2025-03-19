package ch.sthomas.hack.start.model.feature;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Collection;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GenericFeatureCollection<
        F extends GenericFeature<P, F>, P, T extends GenericFeatureCollection<F, P, T>> {

    private static final String TYPE = "FeatureCollection";

    private Collection<F> features;

    public Collection<F> getFeatures() {
        return features;
    }

    public T setFeatures(final Collection<F> features) {
        this.features = features;
        //noinspection unchecked
        return (T) this;
    }

    public String getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("#features", features.size())
                .toString();
    }
}
