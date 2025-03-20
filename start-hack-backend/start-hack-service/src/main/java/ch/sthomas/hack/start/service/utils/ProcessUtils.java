package ch.sthomas.hack.start.service.utils;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.pivovarit.function.exception.WrappedException;

import jakarta.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class ProcessUtils {

    private static final Logger logger = LoggerFactory.getLogger(ProcessUtils.class);

    private ProcessUtils() {}

    public sealed interface ProcessResult permits ProcessFinishedResult, ProcessDidNotFinishResult {
        List<String> stdout();

        List<String> stderr();
    }

    public record ProcessFinishedResult(List<String> stdout, List<String> stderr)
            implements ProcessResult {}

    public record ProcessDidNotFinishResult(List<String> stdout, List<String> stderr)
            implements ProcessResult {}

    /**
     * Executes a process and returns results from the process.
     *
     * @throws IOException If the process could not be started
     */
    @SuppressWarnings("java:S2095")
    public static ProcessResult executeProcess(final ProcessBuilder builder) throws IOException {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Running command '{}'", String.join(" ", builder.command()));
            }
            final var process = builder.start();
            final var stdoutFuture =
                    Executors.newSingleThreadExecutor(threadFactory("process-stdout"))
                            .submit(
                                    () ->
                                            processOutputLines(
                                                    process::inputReader, Level.DEBUG, "stdout"));
            final var stderrFuture =
                    Executors.newSingleThreadExecutor(threadFactory("process-stderr"))
                            .submit(
                                    () ->
                                            processOutputLines(
                                                    process::errorReader, Level.DEBUG, "stderr"));
            final var didProcessFinish = process.waitFor(10, TimeUnit.SECONDS);

            final var stdout = stdoutFuture.get().map(String.class::cast).toList();
            final var stderr = stderrFuture.get().map(String.class::cast).toList();

            if (!didProcessFinish) {
                return new ProcessDidNotFinishResult(stdout, stderr);
            }
            final var exitValue = process.exitValue();
            logger.debug("Process finished with exit value {}", exitValue);
            return new ProcessFinishedResult(stdout, stderr);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WrappedException(e);
        } catch (final ExecutionException e) {
            throw new WrappedException(e);
        }
    }

    @NotNull
    private static Stream<?> processOutputLines(
            final Supplier<BufferedReader> readerSupplier,
            final Level level,
            final String outputStreamName) {
        final var reader = readerSupplier.get();
        //noinspection SimplifyStreamApiCallChains
        return reader.lines()
                .map(
                        line -> {
                            logger.atLevel(level).log("{}: {}", outputStreamName, line);
                            return line;
                        });
    }

    private static @NotNull ThreadFactory threadFactory(final String name) {
        return new ThreadFactoryBuilder().setNameFormat(name).build();
    }
}
