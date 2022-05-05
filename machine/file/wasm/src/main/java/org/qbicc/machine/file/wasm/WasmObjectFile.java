package org.qbicc.machine.file.wasm;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.qbicc.machine.arch.Cpu;
import org.qbicc.machine.arch.ObjectType;
import org.qbicc.machine.file.bin.BinaryBuffer;
import org.qbicc.machine.object.ObjectFile;
import org.qbicc.machine.object.Section;

/**
 * A Mach-O object file.
 */
public final class WasmObjectFile implements ObjectFile {

    private static final Pattern DATA = Pattern.compile("^\s*\\(data \\$([a-w0-9_]+) \\(i32.*\\) \"(.*)\"\\).*");
    Map<String, Byte> sizes = new HashMap<>();

    public WasmObjectFile(final String buffer) {
        for (String ln : buffer.split("\n")) {
            Matcher matcher = DATA.matcher(ln);
            if (matcher.find()) {
                String[] data = matcher.group(2).split("\\\\");
                String d = data[0].isEmpty()? data[1] : data[0];
                byte b = Byte.parseByte(d);
                System.out.printf("%20s   %d\n", matcher.group(1), b);
                sizes.put(matcher.group(1), b);
            }
        }
    }

    @Override
    public int getSymbolValueAsByte(String name) {
        return getSize(name);
    }

    @Override
    public int getSymbolValueAsInt(String name) {
        return getSize(name);
    }

    @Override
    public long getSymbolValueAsLong(String name) {
        return getSize(name);
    }

    @Override
    public byte[] getSymbolAsBytes(String name, int size) {
        return new byte[] { getSize(name) };
    }

    @Override
    public String getSymbolValueAsUtfString(String name, int nbytes) {
        return new String(getSymbolAsBytes(name, nbytes), StandardCharsets.UTF_8);
    }

    private Byte getSize(String name) {
        return sizes.getOrDefault(name, (byte) 1);
    }

    @Override
    public long getSymbolSize(String name) {
        return getSize(name);
    }

    @Override
    public ByteOrder getByteOrder() {
        return ByteOrder.LITTLE_ENDIAN;
    }

    @Override
    public Cpu getCpu() {
        return Cpu.WASM32;
    }

    @Override
    public ObjectType getObjectType() {
        return ObjectType.WASM;
    }

    @Override
    public Section getSection(String name) {
        return null;
    }

    @Override
    public String getRelocationSymbolForSymbolValue(String symbol) {
        return null;
    }

    @Override
    public String getStackMapSectionName() {
        return null;
    }

    @Override
    public void close() throws IOException {

    }
}
