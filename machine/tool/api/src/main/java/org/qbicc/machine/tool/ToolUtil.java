package org.qbicc.machine.tool;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 *
 */
public final class ToolUtil {
    private ToolUtil() {
    }

    public static final List<Path> OS_PATH;

    static {
        final String pathValue = System.getenv("PATH");
        final List<Path> osPath;
        if (pathValue == null) {
            osPath = Collections.emptyList();
        } else {
            final String[] elements = pathValue.split(Pattern.quote(File.pathSeparator));
            final Path[] pathItems = new Path[elements.length];
            int j = 0;
            for (String element : elements) {
                if (!element.isEmpty()) {
                    final Path path = Path.of(element);
                    if (Files.isDirectory(path)) {
                        pathItems[j++] = path;
                    }
                }
            }
            osPath = List.of(Arrays.copyOf(pathItems, j));
        }
        OS_PATH = osPath;
    }

    public static Path findExecutable(String name) {
        return findExecutableOnPath(name, OS_PATH);
    }

    public static Path findExecutable(String name, List<Path> extraPaths) {
        Path found = findExecutableOnPath(name, extraPaths);
        if (found == null) {
            found = findExecutableOnPath(name, OS_PATH);
        }
        return found;
    }

    public static Path findExecutableOnPath(String name, List<Path> paths) {
        for (Path path : paths) {
            final Path exec = path.resolve(name);
            if (Files.isRegularFile(exec) && Files.isReadable(exec) && Files.isExecutable(exec)) {
                return exec;
            }
        }
        return null;
    }
}
