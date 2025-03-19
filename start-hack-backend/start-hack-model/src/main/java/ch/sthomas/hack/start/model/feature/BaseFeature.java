package ch.sthomas.hack.start.model.feature;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseFeature extends GenericFeature<Map<String, Object>, BaseFeature> {

    @JsonIgnore
    public <V> V getProperty(final String key) {
        //noinspection unchecked
        return (V) getProperties().get(key);
    }
}
