package org.qbicc.machine.tool.clang;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Disabled;
import org.qbicc.machine.arch.Platform;
import org.qbicc.machine.object.ObjectFile;
import org.qbicc.machine.object.ObjectFileProvider;
import org.qbicc.machine.tool.ToolInvoker;
import org.qbicc.machine.tool.ToolMessageHandler;
import org.qbicc.machine.tool.ToolProvider;
import org.qbicc.machine.tool.ToolUtil;
import org.qbicc.machine.tool.process.InputSource;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class TestSimpleCompile {
    @Test
    @Disabled
    public void testSimpleCompile() throws Exception {
        final Path objectFilePath = Files.createTempFile("temp", ".o");
        Platform plaf = Platform.parse("wasm-wasm-wasi");
        Optional<ObjectFileProvider> of = ObjectFileProvider.findProvider(plaf.getObjectType(), getClass().getClassLoader());
        assumeTrue(of.isPresent());
        Path clang = ToolUtil.findExecutable("emcc");
        assumeTrue(clang != null);
        final Iterable<ClangToolChainImpl> tools = ToolProvider.findAllTools(ClangToolChainImpl.class, plaf, c -> true,
            TestSimpleCompile.class.getClassLoader(), List.of(clang));
        final Iterator<ClangToolChainImpl> iterator = tools.iterator();
        assumeTrue(iterator.hasNext());
        final ClangToolChainImpl gccCompiler = iterator.next();
        final ClangCCompilerInvoker ib = gccCompiler.newCompilerInvoker();
        ib.setOutputPath(objectFilePath);
        ib.setMessageHandler(new ToolMessageHandler() {
            public void handleMessage(final ToolInvoker invoker, final Level level, final String file, final int line, final int column, final String message) {
                if (level == Level.ERROR) {
                    throw new IllegalStateException("Unexpected error: " + message);
                }
            }
        });
        ib.setSource(InputSource.from("extern int foo; int foo = 0x123456;"));
        ib.invoke();
        assertNotNull(objectFilePath);
        ObjectFile objectFile = of.get().openObjectFile(objectFilePath);
        assertEquals(0x123456, objectFile.getSymbolValueAsInt("foo"));
    }
}
