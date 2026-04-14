package br.jus.tjpi.agendatelefonica;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class PostgresDockerBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresDockerBootstrap.class);
    private static final String DB_HOST = "localhost";
    private static final int DB_PORT = 5432;
    private static final String CONTAINER_NAME = "pg-agenda";
    private static final String DB_NAME = "agenda_db";
    private static final String DB_PASSWORD = "TJPI123";
    private static final String POSTGRES_IMAGE = "postgres";

    private PostgresDockerBootstrap() {
    }

    static void startIfNeeded() {
        if (waitForPort(DB_HOST, DB_PORT, 2)) {
            return;
        }

        if (!isCommandAvailable("docker", "--version")) {
            LOGGER.warn("Docker command not available. Skipping Postgres container startup.");
            return;
        }

        if (!isCommandAvailable("docker", "info")) {
            LOGGER.warn("Docker daemon is unavailable. Skipping Postgres container startup.");
            return;
        }

        int exitCode;
        if (containerExists(CONTAINER_NAME)) {
            // Starts an existing container created previously with docker run.
            exitCode = run("docker", "start", CONTAINER_NAME);
        } else {
            // Mirrors the manual command used by the team.
            exitCode = run(
                    "docker", "run", "--name", CONTAINER_NAME,
                    "-e", "POSTGRES_PASSWORD=" + DB_PASSWORD,
                    "-e", "POSTGRES_DB=" + DB_NAME,
                    "-p", DB_PORT + ":5432",
                    "-d", POSTGRES_IMAGE
            );
        }

        if (exitCode != 0) {
            LOGGER.warn("Could not start Postgres container (exit code {}).", exitCode);
            return;
        }

        if (!waitForPort(DB_HOST, DB_PORT, 45)) {
            LOGGER.warn("Postgres did not become available at {}:{} within timeout.", DB_HOST, DB_PORT);
        }
    }

    private static boolean isCommandAvailable(String... command) {
        return run(false, command) == 0;
    }

    private static boolean containerExists(String containerName) {
        return run(false, "docker", "container", "inspect", containerName) == 0;
    }

    private static int run(String... command) {
        return run(command, true);
    }

    private static int run(boolean logFailureDetails, String... command) {
        return run(command, logFailureDetails);
    }

    private static int run(String[] command, boolean logFailureDetails) {
        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();

            String output = readAll(process.getInputStream());
            int exitCode = process.waitFor();

            if (exitCode != 0 && logFailureDetails) {
                String commandLine = String.join(" ", command);
                if (output.isBlank()) {
                    LOGGER.warn("Command failed (exit code {}): {}", exitCode, commandLine);
                } else {
                    LOGGER.warn("Command failed (exit code {}): {}. Output: {}", exitCode, commandLine, output.trim());
                }
            }

            return exitCode;
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            if (logFailureDetails) {
                LOGGER.warn("Command execution failed: {}", String.join(" ", command), ex);
            }
            return -1;
        }
    }

    private static String readAll(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    private static boolean waitForPort(String host, int port, int timeoutSeconds) {
        long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L);

        while (System.currentTimeMillis() < deadline) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), 1000);
                return true;
            } catch (IOException ex) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException interruptedEx) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }

        return false;
    }
}
