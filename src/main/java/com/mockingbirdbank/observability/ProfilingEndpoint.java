package com.mockingbirdbank.observability;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import jdk.jfr.Configuration;
import jdk.jfr.Recording;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.stereotype.Component;

/**
 * On-demand CPU/allocation/lock profiling via {@code POST /actuator/profiling} (start) and {@code
 * DELETE /actuator/profiling} (stop - writes a real {@code .jfr} file, openable in JDK Mission
 * Control or VisualVM) with zero extra infrastructure: JDK Flight Recorder ships in every JDK 21
 * build this app already requires, so there's nothing new to install or run alongside it, unlike an
 * APM agent.
 *
 * <p>Deliberately request-triggered rather than always-on: JFR's "default" profile is low overhead,
 * but there's no reason to pay even that continuously in production when the point is "capture
 * what's happening right now while investigating a specific slow window."
 */
@Component
@Endpoint(id = "profiling")
public class ProfilingEndpoint {

    private final AtomicReference<Recording> active = new AtomicReference<>();
    private final Path outputDir;

    public ProfilingEndpoint(
            @Value("${mockingbird.profiling.output-dir:${java.io.tmpdir}/mockingbird-bank-jfr}")
                    String outputDir) {
        this.outputDir = Path.of(outputDir);
    }

    @ReadOperation
    public Map<String, Object> status() {
        Recording recording = active.get();
        if (recording == null) {
            return Map.of("recording", false);
        }
        return Map.of(
                "recording", true,
                "name", recording.getName(),
                "startTime", String.valueOf(recording.getStartTime()));
    }

    @WriteOperation
    public Map<String, Object> start() {
        if (active.get() != null) {
            return Map.of("error", "A recording is already running - stop it first.");
        }
        try {
            Files.createDirectories(outputDir);
            Recording recording = new Recording(Configuration.getConfiguration("default"));
            String name = "mockingbird-bank-" + Instant.now().toEpochMilli();
            recording.setName(name);
            recording.setDestination(outputDir.resolve(name + ".jfr"));
            recording.start();
            active.set(recording);
            return Map.of(
                    "started", true, "name", name, "file", recording.getDestination().toString());
        } catch (IOException | ParseException e) {
            throw new IllegalStateException("Could not start JFR recording", e);
        }
    }

    @DeleteOperation
    public Map<String, Object> stop() {
        Recording recording = active.getAndSet(null);
        if (recording == null) {
            return Map.of("error", "No recording is running.");
        }
        Path destination = recording.getDestination();
        recording.stop();
        recording.close();
        return Map.of("stopped", true, "file", String.valueOf(destination));
    }
}
