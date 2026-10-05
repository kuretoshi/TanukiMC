package net.neoforged.neodev;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipFile;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

@DisableCachingByDefault(because = "Checks the editable source tree before packaging")
abstract class VerifySourcePatches extends DefaultTask {
    @Inject
    public VerifySourcePatches() {}

    @InputFile
    public abstract RegularFileProperty getPatchedJar();

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getSources();

    @TaskAction
    public void verify() throws IOException {
        Path root = getSources().get().getAsFile().toPath();
        Set<String> expected = new HashSet<>();
        List<String> mismatches = new ArrayList<>();
        try (ZipFile archive = new ZipFile(getPatchedJar().get().getAsFile())) {
            var entries = archive.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().endsWith(".java")) {
                    continue;
                }
                expected.add(entry.getName());
                Path source = root.resolve(entry.getName());
                if (!Files.isRegularFile(source)) {
                    mismatches.add("Missing: " + entry.getName());
                    continue;
                }
                try (var input = archive.getInputStream(entry)) {
                    String patched = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                    if (!normalize(patched).equals(normalize(Files.readString(source)))) {
                        mismatches.add("Changed: " + entry.getName());
                    }
                }
            }
        }
        try (var sources = Files.walk(root)) {
            sources.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".java")).forEach(path -> {
                String relative = root.relativize(path).toString().replace('\\', '/');
                if (!expected.contains(relative)) {
                    mismatches.add("Unpatched: " + relative);
                }
            });
        }
        if (!mismatches.isEmpty()) {
            mismatches.sort(String::compareTo);
            throw new GradleException("Generated Minecraft sources differ from patches in " + mismatches.size()
                + " files:\n" + String.join("\n", mismatches.subList(0, Math.min(10, mismatches.size())))
                + "\nPreserve local edits with :youer:genPatches, or refresh stale sources with :youer:setup before packaging.");
        }
        getLogger().lifecycle("Verified {} Minecraft source files against patches", expected.size());
    }

    private static String normalize(String text) {
        return text.replace("\r\n", "\n");
    }
}
