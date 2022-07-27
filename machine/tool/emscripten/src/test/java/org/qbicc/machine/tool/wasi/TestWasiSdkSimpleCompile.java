
package org.qbicc.machine.tool.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.qbicc.machine.arch.Platform;
import org.qbicc.machine.object.ObjectFile;
import org.qbicc.machine.object.ObjectFileProvider;
import org.qbicc.machine.tool.ToolInvoker;
import org.qbicc.machine.tool.ToolMessageHandler;
import org.qbicc.machine.tool.ToolProvider;
import org.qbicc.machine.tool.ToolUtil;
import org.qbicc.machine.tool.process.InputSource;
import org.qbicc.machine.tool.wasm.shared.WasmCCompilerInvoker;

/**
 *
 */
public class TestWasiSdkSimpleCompile {
    @Test
    public void testSimpleCompile() throws Exception {
        final Path objectFilePath = Files.createTempFile("temp", ".wasm");
        Platform plaf = Platform.parse("wasm-wasi");
        Optional<ObjectFileProvider> of = ObjectFileProvider.findProvider(plaf.getObjectType(), getClass().getClassLoader());
        assumeTrue(of.isPresent());
        Path clang = ToolUtil.findExecutable("clang");
        assumeTrue(clang != null);
        final Iterable<WasiToolChainImpl> tools = ToolProvider.findAllTools(WasiToolChainImpl.class, plaf, c -> true,
            TestWasiSdkSimpleCompile.class.getClassLoader(), List.of(clang));
        final Iterator<WasiToolChainImpl> iterator = tools.iterator();
        assumeTrue(iterator.hasNext());
        final WasiToolChainImpl gccCompiler = iterator.next();
        final WasmCCompilerInvoker ib = gccCompiler.newCompilerInvoker();
        ib.setOutputPath(objectFilePath);
        ib.setMessageHandler(new ToolMessageHandler() {
            public void handleMessage(final ToolInvoker invoker, final Level level, final String file, final int line, final int column, final String message) {
                if (level == Level.ERROR) {
                    throw new IllegalStateException("Unexpected error: " + message);
                }
            }
        });
        ib.setSource(InputSource.from("extern int foo; int foo = 0x12345678;"));
        ib.invoke();
        assertNotNull(objectFilePath);
        ObjectFile objectFile = of.get().openObjectFile(objectFilePath);
        assertEquals(0x12345678, objectFile.getSymbolValueAsInt("foo"));
    }
}
