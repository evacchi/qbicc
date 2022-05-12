package org.qbicc.machine.file.wasm;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import asmble.ast.Node;
import asmble.ast.SExpr;
import asmble.io.SExprToAst;
import asmble.io.StrToSExpr;
import io.kaitai.struct.KaitaiStruct;
import kotlin.Pair;
import org.qbicc.machine.arch.Cpu;
import org.qbicc.machine.arch.ObjectType;
import org.qbicc.machine.object.ObjectFile;
import org.qbicc.machine.object.Section;

/**
 * A Mach-O object file.
 */
public final class WasmObjectFile implements ObjectFile {

    private static final Pattern DATA = Pattern.compile("^\s*\\(data \\$([a-w0-9_]+) \\(i32.*\\) \"(.*)\"\\).*");
    Map<String, Integer> sizes = new HashMap<>();

    public WasmObjectFile(Path path) throws IOException {
        Webassembly webassembly = Webassembly.fromFile(path.toString());
        HashMap<Integer, String> indexSymbol = new HashMap<>();
        for (Webassembly.Section sect : webassembly.sections().sections()) {
            Webassembly.SectionHeader header = sect.header();
            KaitaiStruct struct = sect.payloadData();

            if (struct instanceof Webassembly.UnimplementedSection) {
                List<Webassembly.LinkingCustomType> linking = ((Webassembly.UnimplementedSection) struct).linking();
                if (linking == null) continue;

                for (Webassembly.LinkingCustomType linkingCustomType : linking) {
                    for (Webassembly.LinkingCustomSubsectionType subsection : linkingCustomType.subsections()) {
                        if (subsection.type() != 8) continue;

                        for (Webassembly.SyminfoType info : subsection.symbolTable().infos()) {
                            if (info.kind()!=1) continue;
                            String name = info.nameData();
                            VlqBase128Le idx = info.index();
                            if (idx == null) {
//                                System.out.printf("%s IS NULL", name);
                                continue;
                            }
                            Integer index = idx.value();
//                            System.out.print(index);
//                            System.out.print("   -> ");
//                            System.out.println(name);
                            indexSymbol.put(index, name);
                        }
                    }
                }


            }


        }
        for (Webassembly.Section sect : webassembly.sections().sections()) {
            Webassembly.SectionHeader header = sect.header();
            KaitaiStruct struct = sect.payloadData();


            if (struct instanceof Webassembly.DataSection) {


                ArrayList<Webassembly.DataSegmentType> entries = ((Webassembly.DataSection) struct).entries();
                for (int i = 0; i < entries.size(); i++) {
                    Webassembly.DataSegmentType data = entries.get(i);
//                    System.out.print(i);
//                    System.out.print(":::");
//                    System.out.println(indexSymbol.get(i));


                    sizes.put(indexSymbol.get(i), data.data().get(0));
                }

            }

        }

//        System.out.println(sizes);

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
        return new byte[] { getSize(name).byteValue() };
    }

    @Override
    public String getSymbolValueAsUtfString(String name, int nbytes) {
        return new String(getSymbolAsBytes(name, nbytes), StandardCharsets.UTF_8);
    }

    private Integer getSize(String name) {
        return sizes.getOrDefault(name, 0);
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
        return "__llvm_stackmaps";
    }

    @Override
    public void close() throws IOException {

    }
}
