package ch.sthomas.hack.start.model.points;

import ch.sthomas.hack.start.model.product.ModisProduct;

import java.time.Instant;

public record PointData<T>(Instant time, ModisProduct product, T data) {}
