package ch.sthomas.hack.start.model.points;

public record TimeNumericStatData(double max, double min, double avg, long count)
        implements TimeStatData {}
