package org.qbicc.machine.file.wasm;

import java.nio.file.Path;

public class WasmObjectFileParsingException extends RuntimeException {
    public WasmObjectFileParsingException(Path path, Throwable cause) {
        super("Error parsing " + path, cause);
    }
}
