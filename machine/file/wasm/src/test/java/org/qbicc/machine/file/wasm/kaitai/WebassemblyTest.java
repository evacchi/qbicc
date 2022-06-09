package org.qbicc.machine.file.wasm.kaitai;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class WebassemblyTest {

    @Test
    void probe() throws IOException {
        String f = "/Users/evacchi/Devel/rh/fun/qbicc/machine/file/wasm/src/test/resources/probe.wasm";
        Webassembly webassembly = Webassembly.fromFile(f);
    }

    @Test
    void linkingSect1() throws IOException {
        String f = "/Users/evacchi/Devel/rh/fun/qbicc/machine/file/wasm/src/test/resources/CalendarDataProvider.o";
        Webassembly webassembly = Webassembly.fromFile(f);
    }

    @Test
    void linkingSect2() throws IOException {
        String f = "/Users/evacchi/Devel/rh/fun/qbicc/machine/file/wasm/src/test/resources/FileSystemNotFoundException.o";
        Webassembly webassembly = Webassembly.fromFile(f);
    }

    @Test
    void linkingSect3() throws IOException {
        String f = "/Users/evacchi/Devel/rh/fun/qbicc/machine/file/wasm/src/test/resources/StringIndexOutOfBoundsException.o";
        Webassembly webassembly = Webassembly.fromFile(f);
    }

    @Test
    void string() throws IOException {
        String f = "/Users/evacchi/Devel/rh/fun/qbicc/machine/file/wasm/src/test/resources/String.o";
        Webassembly webassembly = Webassembly.fromFile(f);
    }
}