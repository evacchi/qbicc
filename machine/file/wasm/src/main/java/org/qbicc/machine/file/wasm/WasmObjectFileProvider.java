package org.qbicc.machine.file.wasm;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.qbicc.machine.arch.ObjectType;
import org.qbicc.machine.file.bin.BinaryBuffer;
import org.qbicc.machine.object.ObjectFile;
import org.qbicc.machine.object.ObjectFileProvider;
import org.qbicc.machine.tool.ToolMessageHandler;
import org.qbicc.machine.tool.process.InputSource;
import org.qbicc.machine.tool.process.OutputDestination;

/**
 *
 */
public final class WasmObjectFileProvider implements ObjectFileProvider {
    public WasmObjectFileProvider() {}

    public ObjectFile openObjectFile(final Path path) throws IOException {
        ProcessBuilder pb = new ProcessBuilder();
        pb.command("wasm2wat", path.toString());
        Process start = pb.start();
        String result = new String(start.getInputStream().readAllBytes());

        return new WasmObjectFile(result);
    }

    public ObjectType getObjectType() {
        return ObjectType.WASM;
    }
}
