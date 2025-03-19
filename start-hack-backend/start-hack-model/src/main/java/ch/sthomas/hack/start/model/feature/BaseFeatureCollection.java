package ch.sthomas.hack.start.model.feature;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseFeatureCollection
        extends GenericFeatureCollection<BaseFeature, Map<String, Object>, BaseFeatureCollection> {}
