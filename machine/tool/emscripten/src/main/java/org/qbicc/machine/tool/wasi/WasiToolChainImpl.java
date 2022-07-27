package org.qbicc.machine.tool.wasi;

import java.nio.file.Path;

import io.smallrye.common.version.VersionScheme;
import org.qbicc.machine.arch.Platform;
import org.qbicc.machine.tool.wasm.shared.WasmCCompilerInvoker;
import org.qbicc.machine.tool.wasm.shared.WasmLinkerInvoker;
import org.qbicc.machine.tool.wasm.shared.WasmToolChain;

final class WasiToolChainImpl implements WasmToolChain {
    private final Path executablePath;
    private final Platform platform;
    private final String version;

    WasiToolChainImpl(final Path executablePath, final Platform platform, final String version) {
        this.executablePath = executablePath;
        this.platform = platform;
        this.version = version;
    }

    public String getImplementationName() {
        return "WASI-SDK (LLVM)";
    }

    Path getExecutablePath() {
        return executablePath;
    }

    public WasmCCompilerInvoker newCompilerInvoker() {
        return new WasiCCompilerInvokerImpl(this);
    }

    public WasmLinkerInvoker newLinkerInvoker() {
        return new WasiLinkerInvokerImpl(this);
    }

    public Platform getPlatform() {
        return platform;
    }

    public String getVersion() {
        return version;
    }

    public int compareVersionTo(final String version) {
        return VersionScheme.BASIC.compare(this.version, version);
    }
}
