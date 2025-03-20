package ch.sthomas.hack.start.service.geo.tif;

import ch.sthomas.hack.start.service.utils.ProcessUtils;

import org.geotools.api.referencing.FactoryException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.Hints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class TifParser {
    private static final Logger logger = LoggerFactory.getLogger(TifParser.class);
    private final Path path;

    public TifParser(final Path path) {
        this.path = path;
    }

    public GridCoverage2D parse() throws IOException {
        if (!Files.exists(path)) {
            logger.info("No tif file found at {}. Skipping", path);
            return null;
        }
        final var wkt =
                String.join(
                        "\n",
                        ProcessUtils.executeProcess(
                                        new ProcessBuilder(
                                                List.of(
                                                        "gdalsrsinfo",
                                                        "-o",
                                                        "wkt1",
                                                        path.toAbsolutePath().toString())))
                                .stdout());
        try {
            final var crs = CRS.parseWKT(wkt);
            final var hints = new Hints(Hints.DEFAULT_COORDINATE_REFERENCE_SYSTEM, crs);
            final var reader = new GeoTiffReader(path.toFile(), hints);
            return reader.read(null);
        } catch (final FactoryException e) {
            logger.error("Could not read GeoTIFF", e);
            return null;
        }
    }
}
