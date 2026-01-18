package com.signalgraph.orchestrator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Formatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * SignalGraph Orchestrator — Java ingestion skeleton
 *
 * WHY: Provide a minimal, dependency-free orchestrator that demonstrates deterministic
 * file discovery, checksum-based job metadata, and a clear subprocess contract with the
 * Python intelligence layer. The implementation favors clarity and auditability for
 * interview review.
 */
public class Main {
    public static void main(String[] args) throws IOException {
        // WHY: Keep CLI behavior explicit and easy to test. Avoid hidden framework magic.
        if (args.length == 0) {
            System.out.println("SignalGraph Orchestrator (Java) - skeleton");
            System.out.println("Usage: java -cp <jar> com.signalgraph.orchestrator.Main ingest <data-path>");
            return;
        }

        String cmd = args[0];
        if ("ingest".equals(cmd)) {
            String path = args.length > 1 ? args[1] : "data/";
            System.out.printf("[orchestrator] ingesting files from %s\n", path);
            runIngest(Paths.get(path));
        } else {
            System.out.printf("Unknown command: %s\n", cmd);
        }
    }

    // Deterministic file discovery: list files sorted by filename
    private static List<Path> discoverFiles(Path dir) throws IOException {
        List<Path> files = new ArrayList<>();
        if (!Files.exists(dir)) {
            System.out.printf("[orchestrator] warning: path does not exist: %s\n", dir);
            return files;
        }
        try (var stream = Files.list(dir)) {
            files = stream.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .collect(Collectors.toList());
        }
        return files;
    }

    // Compute SHA-256 checksum for file bytes.
    private static String sha256(Path p) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] data = Files.readAllBytes(p);
            byte[] digest = md.digest(data);
            try (Formatter fmt = new Formatter()) {
                for (byte b : digest) {
                    fmt.format("%02x", b);
                }
                return fmt.toString();
            }
        } catch (Exception e) {
            throw new IOException("Failed to compute checksum: " + e.getMessage(), e);
        }
    }

    private static boolean isTextFile(Path p) {
        String name = p.getFileName().toString().toLowerCase();
        return name.endsWith(".txt") || name.endsWith(".md") || name.endsWith(".text");
    }

    // Run the python intelligence CLI for a single file. Returns true on success.
    private static boolean runPythonForFile(Path file, Path outJson) throws IOException, InterruptedException {
        // WHY: Use an explicit subprocess contract to keep boundaries clear and auditable.
        List<String> cmd = new ArrayList<>();
        cmd.add("python3");
        cmd.add("intelligence-python/main.py");
        cmd.add("--file");
        cmd.add(file.toString());
        cmd.add("--output");
        cmd.add(outJson.toString());

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(Paths.get("./").toFile()); // run from repo root
        pb.redirectErrorStream(true);

        Process proc = pb.start();
        String stdout;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
            stdout = r.lines().collect(Collectors.joining("\n"));
        }
        int exit = proc.waitFor();
        System.out.printf("[orchestrator] python exit=%d\n", exit);
        if (!stdout.isBlank()) {
            System.out.println("[orchestrator][python] output:\n" + stdout);
        }
        return exit == 0;
    }

    private static void runIngest(Path dataDir) {
        try {
            List<Path> files = discoverFiles(dataDir);
            System.out.printf("[orchestrator] discovered %d files\n", files.size());

            Path graphsDir = Paths.get("data/graphs");
            if (!Files.exists(graphsDir)) {
                Files.createDirectories(graphsDir);
            }

            for (Path f : files) {
                try {
                    String checksum = sha256(f);
                    String ingestId = UUID.nameUUIDFromBytes((f.toString() + checksum).getBytes()).toString();
                    Instant now = Instant.now();

                    System.out.printf("[orchestrator] file=%s checksum=%s ingest_id=%s\n", f, checksum, ingestId);

                    if (!isTextFile(f)) {
                        System.out.printf("[orchestrator] skipping non-text file: %s\n", f.getFileName());
                        continue;
                    }

                    // Read file text deterministically
                    String text = Files.readString(f, StandardCharsets.UTF_8);

                    // Prepare output path for intelligence JSON
                    Path outJson = graphsDir.resolve(ingestId + ".json");

                    // WHY: For V1 we call the Python CLI per-file and expect it to write its output to outJson.
                    boolean ok = runPythonForFile(f, outJson);

                    // If python did not produce output, create a minimal metadata JSON
                    if (!ok || !Files.exists(outJson)) {
                        String meta = "{\n" +
                                "  \"ingest_id\": \"" + ingestId + "\",\n" +
                                "  \"file\": \"" + f.toString() + "\",\n" +
                                "  \"checksum\": \"" + checksum + "\",\n" +
                                "  \"created_at\": \"" + now.toString() + "\",\n" +
                                "  \"note\": \"python processing failed or produced no output in V1\"\n" +
                                "}\n";
                        Files.writeString(outJson, meta, StandardCharsets.UTF_8);
                        System.out.printf("[orchestrator] wrote fallback metadata to %s\n", outJson);
                    } else {
                        System.out.printf("[orchestrator] intelligence output written to %s\n", outJson);
                    }

                } catch (Exception e) {
                    System.out.printf("[orchestrator] error processing file %s: %s\n", f, e.getMessage());
                }
            }

        } catch (IOException e) {
            System.out.printf("[orchestrator] ingestion failed: %s\n", e.getMessage());
        }
    }
}
