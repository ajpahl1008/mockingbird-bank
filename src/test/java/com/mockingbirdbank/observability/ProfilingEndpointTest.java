package com.mockingbirdbank.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProfilingEndpointTest {

    @TempDir Path tempDir;

    @Test
    void startThenStopWritesARealNonEmptyJfrFile() throws Exception {
        ProfilingEndpoint endpoint = new ProfilingEndpoint(tempDir.toString());

        Map<String, Object> startResult = endpoint.start();
        assertThat(startResult).containsEntry("started", true);

        // Give JFR a moment to actually capture something - thread/GC/class-loading events fire
        // continuously even on an otherwise idle JVM.
        Thread.sleep(500);

        Map<String, Object> stopResult = endpoint.stop();
        assertThat(stopResult).containsEntry("stopped", true);

        Path dumpFile = Path.of((String) stopResult.get("file"));
        assertThat(dumpFile).exists();
        assertThat(Files.size(dumpFile)).isGreaterThan(0);
    }

    @Test
    void statusReflectsWhetherARecordingIsActive() {
        ProfilingEndpoint endpoint = new ProfilingEndpoint(tempDir.toString());

        assertThat(endpoint.status()).containsEntry("recording", false);

        endpoint.start();
        assertThat(endpoint.status()).containsEntry("recording", true);

        endpoint.stop();
        assertThat(endpoint.status()).containsEntry("recording", false);
    }

    @Test
    void startingASecondRecordingWithoutStoppingTheFirstReturnsAnErrorInstead() {
        ProfilingEndpoint endpoint = new ProfilingEndpoint(tempDir.toString());
        endpoint.start();

        assertThat(endpoint.start()).containsKey("error");

        endpoint.stop();
    }

    @Test
    void stoppingWithNothingRunningReturnsAnErrorInsteadOfThrowing() {
        ProfilingEndpoint endpoint = new ProfilingEndpoint(tempDir.toString());

        assertThat(endpoint.stop()).containsKey("error");
    }
}
