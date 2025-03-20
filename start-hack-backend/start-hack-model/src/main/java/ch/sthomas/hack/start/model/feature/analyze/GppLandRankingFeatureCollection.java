package ch.sthomas.hack.start.model.feature.analyze;

import ch.sthomas.hack.start.model.feature.GenericFeatureCollection;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GppLandRankingFeatureCollection
        extends GenericFeatureCollection<
                GppLandRankingFeature,
                GppLandRankingFeatureProperties,
                GppLandRankingFeatureCollection> {}
