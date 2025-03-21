package ch.sthomas.hack.start.model.points;

import java.util.Map;

public record TimeLctStatData(Map<String, Integer> lctFrequencies) implements TimeStatData {}
