package ch.sthomas.hack.start.model.feature.analyze;

import ch.sthomas.hack.start.model.points.PointData;

import jakarta.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public record GppLandRankingFeatureProperties(
        int rank, @Nullable List<PointData<Pair<Integer, String>>> gppToLandUsage) {}
