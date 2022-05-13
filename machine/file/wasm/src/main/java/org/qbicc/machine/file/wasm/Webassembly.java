package org.qbicc.machine.file.wasm;// This is a generated file! Please edit source .ksy file and use kaitai-struct-compiler to rebuild

import io.kaitai.struct.ByteBufferKaitaiStream;
import io.kaitai.struct.KaitaiStruct;
import io.kaitai.struct.KaitaiStream;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.ArrayList;
import java.nio.charset.Charset;


/**
 * WebAssembly is a web standard that defines a binary format and a corresponding
 * assembly-like text format for executable code in Web pages. It is meant to
 * enable executing code nearly as fast as running native machine code.
 * @see <a href="https://github.com/WebAssembly/design/blob/master/BinaryEncoding.md">Source</a>
 */
public class Webassembly extends KaitaiStruct {
    public static Webassembly fromFile(String fileName) throws IOException {
        return new Webassembly(new ByteBufferKaitaiStream(fileName));
    }

    public enum ElemType {
        ANYFUNC(112);

        private final long id;
        ElemType(long id) { this.id = id; }
        public long id() { return id; }
        private static final Map<Long, ElemType> byId = new HashMap<Long, ElemType>(1);
        static {
            for (ElemType e : ElemType.values())
                byId.put(e.id(), e);
        }
        public static ElemType byId(long id) { return byId.get(id); }
    }

    public enum ValueType {
        F64(124),
        F32(125),
        I64(126),
        I32(127);

        private final long id;
        ValueType(long id) { this.id = id; }
        public long id() { return id; }
        private static final Map<Long, ValueType> byId = new HashMap<Long, ValueType>(4);
        static {
            for (ValueType e : ValueType.values())
                byId.put(e.id(), e);
        }
        public static ValueType byId(long id) { return byId.get(id); }
    }

    public enum ConstructorType {
        EMPTY_BLOCK(64),
        FUNC(96),
        ANYFUNC(112),
        F64(124),
        F32(125),
        I64(126),
        I32(127);

        private final long id;
        ConstructorType(long id) { this.id = id; }
        public long id() { return id; }
        private static final Map<Long, ConstructorType> byId = new HashMap<Long, ConstructorType>(7);
        static {
            for (ConstructorType e : ConstructorType.values())
                byId.put(e.id(), e);
        }
        public static ConstructorType byId(long id) { return byId.get(id); }
    }

    public enum KindType {
        FUNCTION_KIND(0),
        TABLE_KIND(1),
        MEMORY_KIND(2),
        GLOBAL_KIND(3);

        private final long id;
        KindType(long id) { this.id = id; }
        public long id() { return id; }
        private static final Map<Long, KindType> byId = new HashMap<Long, KindType>(4);
        static {
            for (KindType e : KindType.values())
                byId.put(e.id(), e);
        }
        public static KindType byId(long id) { return byId.get(id); }
    }

    public enum MutabilityFlag {
        IMMUTABLE(0),
        MUTABLE(1);

        private final long id;
        MutabilityFlag(long id) { this.id = id; }
        public long id() { return id; }
        private static final Map<Long, MutabilityFlag> byId = new HashMap<Long, MutabilityFlag>(2);
        static {
            for (MutabilityFlag e : MutabilityFlag.values())
                byId.put(e.id(), e);
        }
        public static MutabilityFlag byId(long id) { return byId.get(id); }
    }

    public enum PayloadType {
        CUSTOM_PAYLOAD(0),
        TYPE_PAYLOAD(1),
        IMPORT_PAYLOAD(2),
        FUNCTION_PAYLOAD(3),
        TABLE_PAYLOAD(4),
        MEMORY_PAYLOAD(5),
        GLOBAL_PAYLOAD(6),
        EXPORT_PAYLOAD(7),
        START_PAYLOAD(8),
        ELEMENT_PAYLOAD(9),
        CODE_PAYLOAD(10),
        DATA_PAYLOAD(11),
        DATA_COUNT_PAYLOAD(12);

        private final long id;
        PayloadType(long id) { this.id = id; }
        public long id() { return id; }
        private static final Map<Long, PayloadType> byId = new HashMap<Long, PayloadType>(13);
        static {
            for (PayloadType e : PayloadType.values())
                byId.put(e.id(), e);
        }
        public static PayloadType byId(long id) { return byId.get(id); }
    }

    public Webassembly(KaitaiStream _io) {
        this(_io, null, null);
    }

    public Webassembly(KaitaiStream _io, KaitaiStruct _parent) {
        this(_io, _parent, null);
    }

    public Webassembly(KaitaiStream _io, KaitaiStruct _parent, Webassembly _root) {
        super(_io);
        this._parent = _parent;
        this._root = _root == null ? this : _root;
        _read();
    }
    private void _read() {
        this.magic = this._io.readBytes(4);
        if (!(Arrays.equals(magic(), new byte[] { 0, 97, 115, 109 }))) {
            throw new KaitaiStream.ValidationNotEqualError(new byte[] { 0, 97, 115, 109 }, magic(), _io(), "/seq/0");
        }
        this.version = this._io.readU4le();
        this.sections = new Sections(this._io, this, _root);
    }
    public static class DataSegmentType extends KaitaiStruct {
        public static DataSegmentType fromFile(String fileName) throws IOException {
            return new DataSegmentType(new ByteBufferKaitaiStream(fileName));
        }

        public DataSegmentType(KaitaiStream _io) {
            this(_io, null, null);
        }

        public DataSegmentType(KaitaiStream _io, Webassembly.DataSection _parent) {
            this(_io, _parent, null);
        }

        public DataSegmentType(KaitaiStream _io, Webassembly.DataSection _parent, Webassembly _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.index = new VlqBase128Le(this._io);
            this.offset = new ArrayList<Integer>();
            {
                int _it;
                int i = 0;
                do {
                    _it = this._io.readU1();
                    this.offset.add(_it);
                    i++;
                } while (!(_it == 11));
            }
            this.size = new VlqBase128Le(this._io);
            data = new ArrayList<Integer>(((Number) (size().value())).intValue());
            for (int i = 0; i < size().value(); i++) {
                this.data.add(this._io.readU1());
            }
        }
        private VlqBase128Le index;
        private ArrayList<Integer> offset;
        private VlqBase128Le size;
        private ArrayList<Integer> data;
        private Webassembly _root;
        private Webassembly.DataSection _parent;
        public VlqBase128Le index() { return index; }
        public ArrayList<Integer> offset() { return offset; }
        public VlqBase128Le size() { return size; }
        public ArrayList<Integer> data() { return data; }
        public Webassembly _root() { return _root; }
        public Webassembly.DataSection _parent() { return _parent; }
    }
    public static class CodeSection extends KaitaiStruct {
        public static CodeSection fromFile(String fileName) throws IOException {
            return new CodeSection(new ByteBufferKaitaiStream(fileName));
        }

        public CodeSection(KaitaiStream _io) {
            this(_io, null, null);
        }

        public CodeSection(KaitaiStream _io, Webassembly.Section _parent) {
            this(_io, _parent, null);
        }

        public CodeSection(KaitaiStream _io, Webassembly.Section _parent, Webassembly _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.count = new VlqBase128Le(this._io);
            bodies = new ArrayList<FunctionBodyType>(((Number) (count().value())).intValue());
            for (int i = 0; i < count().value(); i++) {
                this.bodies.add(new FunctionBodyType(this._io, this, _root));
            }
        }
        private VlqBase128Le count;
        private ArrayList<FunctionBodyType> bodies;
        private Webassembly _root;
        private Webassembly.Section _parent;
        public VlqBase128Le count() { return count; }
        public ArrayList<FunctionBodyType> bodies() { return bodies; }
        public Webassembly _root() { return _root; }
        public Webassembly.Section _parent() { return _parent; }
    }
    public static class ImportEntry extends KaitaiStruct {
        public static ImportEntry fromFile(String fileName) throws IOException {
            return new ImportEntry(new ByteBufferKaitaiStream(fileName));
        }

        public ImportEntry(KaitaiStream _io) {
            this(_io, null, null);
        }

        public ImportEntry(KaitaiStream _io, Webassembly.ImportSection _parent) {
            this(_io, _parent, null);
        }

        public ImportEntry(KaitaiStream _io, Webassembly.ImportSection _parent, Webassembly _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.moduleLen = new VlqBase128Le(this._io);
            this.moduleStr = new String(this._io.readBytes(moduleLen().value()), Charset.forName("UTF-8"));
            this.fieldLen = new VlqBase128Le(this._io);
            this.fieldStr = new String(this._io.readBytes(fieldLen().value()), Charset.forName("UTF-8"));
            this.kind = Webassembly.KindType.byId(this._io.readU1());
            {
                KindType on = kind();
                if (on != null) {
                    switch (kind()) {
                    case FUNCTION_KIND: {
                        this.type = new VlqBase128Le(this._io);
                        break;
                    }
                    case TABLE_KIND: {
                        this.type = new TableType(this._io, this, _root);
                        break;
                    }
                    case MEMORY_KIND: {
                        this.type = new MemoryType(this._io, this, _root);
                        break;
                    }
                    case GLOBAL_KIND: {
                        this.type = new GlobalType(this._io, this, _root);
                        break;
                    }
                    }
                }
            }
        }
        private VlqBase128Le moduleLen;
        private String moduleStr;
        private VlqBase128Le fieldLen;
        private String fieldStr;
        private KindType kind;
        private KaitaiStruct type;
        private Webassembly _root;
        private Webassembly.ImportSection _parent;
        public VlqBase128Le moduleLen() { return moduleLen; }
        public String moduleStr() { return moduleStr; }
        public VlqBase128Le fieldLen() { return fieldLen; }
        public String fieldStr() { return fieldStr; }
        public KindType kind() { return kind; }
        public KaitaiStruct type() { return type; }
        public Webassembly _root() { return _root; }
        public Webassembly.ImportSection _parent() { return _parent; }
    }
    public static class Sections extends KaitaiStruct {
        public static Sections fromFile(String fileName) throws IOException {
            return new Sections(new ByteBufferKaitaiStream(fileName));
        }

        public Sections(KaitaiStream _io) {
            this(_io, null, null);
        }

        public Sections(KaitaiStream _io, Webassembly _parent) {
            this(_io, _parent, null);
        }

        public Sections(KaitaiStream _io, Webassembly _parent, Webassembly _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.sections = new ArrayList<Section>();
            {
                int i = 0;
                while (!this._io.isEof()) {
                    this.sections.add(new Section(this._io, this, _root));
                    i++;
                }
            }
        }
        private ArrayList<Section> sections;
        private Webassembly _root;
        private Webassembly _parent;
        public ArrayList<Section> sections() { return sections; }
        public Webassembly _root() { return _root; }
        public Webassembly _parent() { return _parent; }
    }
    public static class ExportSection extends KaitaiStruct {
        public static ExportSection fromFile(String fileName) throws IOException {
            return new ExportSection(new ByteBufferKaitaiStream(fileName));
        }

        public ExportSection(KaitaiStream _io) {
            this(_io, null, null);
        }

        public ExportSection(KaitaiStream _io, Webassembly.Section _parent) {
            this(_io, _parent, null);
        }

        public ExportSection(KaitaiStream _io, Webassembly.Section _parent, Webassembly _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.count = new VlqBase128Le(this._io);
            entries = new ArrayList<ExportEntryType>(((Number) (count().value())).intValue());
            for (int i = 0; i < count().value(); i++) {
                this.entries.add(new ExportEntryType(this._io, this, _root));
            }
        }
        private VlqBase128Le count;
        private ArrayList<ExportEntryType> entries;
        private Webassembly _root;
        private Webassembly.Section _parent;
        public VlqBase128Le count() { return count; }
        public ArrayList<ExportEntryType> entries() { return entries; }
        public Webassembly _root() { return _root; }
        public Webassembly.Section _parent() { return _parent; }
    }
    public static class MemoryType extends KaitaiStruct {
        public static MemoryType fromFile(String fileName) throws IOException {
            return new MemoryType(new ByteBufferKaitaiStream(fileName));
        }

        public MemoryType(KaitaiStream _io) {
            this(_io, null, null);
        }

        public MemoryType(KaitaiStream _io, KaitaiStruct _parent) {
            this(_io, _parent, null);
        }

        public MemoryType(KaitaiStream _io, KaitaiStruct _parent, Webassembly _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.limits = new ResizableLimitsType(this._io, this, _root);
        }
        private ResizableLimitsType limits;
        private Webassembly _root;
        private KaitaiStruct _parent;
        public ResizableLimitsType limits() { return limits; }
        public Webassembly _root() { return _root; }
        public KaitaiStruct _parent() { return _parent; }
    }
    public static class FuncType extends KaitaiStruct {
        public static FuncType fromFile(String fileName) throws IOException {
            return new FuncType(new ByteBufferKaitaiStream(fileName));
        }

        public FuncType(KaitaiStream _io) {
            this(_io, null, null);
        }

        public FuncType(KaitaiStream _io, Webassembly.TypeSection _parent) {
            this(_io, _parent, null);
        }

        public FuncType(KaitaiStream _io, Webassembly.TypeSection _parent, Webassembly _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.form = Webassembly.ConstructorType.byId(this._io.readU1());
            this.paramCount = this._io.readU1();
            if (paramCount() > 0) {
                paramTypes = new ArrayList<ValueType>(((Number) (paramCount())).intValue());
                for (int i = 0; i < paramCount(); i++) {
                    this.paramTypes.add(Webassembly.ValueType.byId(this._io.readU1()));
                }
            }
            this.returnCount = this._io.readU1();
            if (returnCount() == 1) {
                this.returnType = Webassembly.ValueType.byId(this._io.readU1());
            }
        }
        private ConstructorType form;
        private int paramCount;
        private ArrayList<ValueType> paramTypes;
        private int returnCount;
        private ValueType returnType;
        private Webassembly _root;
        private Webassembly.TypeSection _parent;
        public ConstructorType form() { return form; }
        public int paramCount() { return paramCount; }
        public ArrayList<ValueType> paramTypes() { return paramTypes; }
        public int returnCount() { return returnCount; }
        public ValueType returnType() { return returnType; }
        public Webassembly _root() { return _root; }
        public Webassembly.TypeSection _parent() { return _parent; }
    }
    public static class TableSection extends KaitaiStruct {
        public static TableSection fromFile(String fileName) throws IOException {
            return new TableSection(new ByteBufferKaitaiStream(fileName));
        }

        public TableSection(KaitaiStream _io) {
            this(_io, null, null);
        }

        public TableSection(KaitaiStream _io, Webassembly.Section _parent) {
            this(_io, _parent, null);
        }

        public TableSection(KaitaiStream _io, Webassembly.Section _parent, Webassembly _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.count = new VlqBase128Le(this._io);
            entries = new ArrayList<TableType>(((Number) (count().value())).intValue());
            for (int i = 0; i < count().value(); i++) {
                this.entries.add(new TableType(this._io, this, _root));
            }
        }
        private VlqBase128Le count;
        private ArrayList<TableType> entries;
        private Webassembly _root;
        private Webassembly.Section _parent;
        public VlqBase128Le count() { return count; }
        public ArrayList<TableType> entries() { return entries; }
        public Webassembly _root() { return _root; }
        public Webassembly.Section _parent() { return _parent; }
    }
    public static class SectionHeader extends KaitaiStruct {
        public static SectionHeader fromFile(String fileName) throws IOException {
            return new SectionHeader(new ByteBufferKaitaiStream(fileName));
        }

        public SectionHeader(KaitaiStream _io) {
            this(_io, null, null);
        }

        public SectionHeader(KaitaiStream _io, Webassembly.Section _parent) {
            this(_io, _parent, null);
        }

        public SectionHeader(KaitaiStream _io, Webassembly.Section _parent, Webassembly _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.id = Webassembly.PayloadType.byId(this._io.readU1());
            this.payloadLen = new VlqBase128Le(this._io);
        }
        private PayloadType id;
        private VlqBase128Le payloadLen;
        private Webassembly _root;
        private Webassembly.Section _parent;
        public PayloadType id() { return id; }
        public VlqBase128Le payloadLen() { return payloadLen; }
        public Webassembly _root() { return _root; }
        public Webassembly.Section _parent() { return _parent; }
    }
    public static class Section extends KaitaiStruct {
        public static Section fromFile(String fileName) throws IOException {
            return new Section(new ByteBufferKaitaiStream(fileName));
        }

        public Section(KaitaiStream _io) {
            this(_io, null, null);
        }

        public Section(KaitaiStream _io, Webassembly.Sections _parent) {
            this(_io, _parent, null);
        }

        public Section(KaitaiStream _io, Webassembly.Sections _parent, Webassembly _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.header = new SectionHeader(this._io, this, _root);
            {
                PayloadType on = header().id();
                if (on != null) {
                    switch (header().id()) {
                    case START_PAYLOAD: {
                        this.payloadData = new StartSection(this._io, this, _root);
                        break;
                    }
                    case TYPE_PAYLOAD: {
                        this.payloadData = new TypeSection(this._io, this, _root);
                        break;
                    }
                    case IMPORT_PAYLOAD: {
                        this.payloadData = new ImportSection(this._io, this, _root);
                        break;
                    }
                    case TABLE_PAYLOAD: {
                        this.payloadData = new TableSection(this._io, this, _root);
                        break;
                    }
                    case GLOBAL_PAYLOAD: {
                        this.payloadData = new GlobalSection(this._io, this, _root);
                        break;
                    }
                    case FUNCTION_PAYLOAD: {
                        this.payloadData = new FunctionSection(this._io, this, _root);
                        break;
                    }
                    case ELEMENT_PAYLOAD: {
                        this.payloadData = new ElementSection(this._io, this, _root);
                        break;
                    }
                    case DATA_COUNT_PAYLOAD: {
                        this.payloadData = new DataCountSection(this._io, this, _root);
                        break;
                    }
                    case EXPORT_PAYLOAD: {
                        this.payloadData = new ExportSection(this._io, this, _root);
                        break;
                    }
                    case MEMORY_PAYLOAD: {
                        this.payloadData = new MemorySection(this._io, this, _root);
                        break;
                    }
                    case CODE_PAYLOAD: {
                        this.payloadData = new CodeSection(this._io, this, _root);
                        break;
                    }
                    case DATA_PAYLOAD: {
                        this.payloadData = new DataSection(this._io, this, _root);
                        break;
                    }
                    case CUSTOM_PAYLOAD: {
                        this.payloadData = new UnimplementedSection(this._io, this, _root);
                        break;
                    }
                    }
                }
            }
        }
        private SectionHeader header;
        private KaitaiStruct payloadData;
        private Webassembly _root;
        private Webassembly.Sections _parent;
        public SectionHeader header() { return header; }
        public KaitaiStruct payloadData() { return payloadData; }
        public Webassembly _root() { return _root; }
        public Webassembly.Sections _parent() { return _parent; }
    }
    public static class DataSection extends KaitaiStruct {
        public static DataSection fromFile(String fileName) throws IOException {
            return new DataSection(new ByteBufferKaitaiStream(fileName));
        }

        public DataSection(KaitaiStream _io) {
            this(_io, null, null);
        }

        public DataSection(KaitaiStream _io, Webassembly.Section _parent) {
            this(_io, _parent, null);
        }

        public DataSection(KaitaiStream _io, Webassembly.Section _parent, Webassembly _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.count = new VlqBase128Le(this._io);
            entries = new ArrayList<DataSegmentType>(((Number) (count().value())).intValue());
            for (int i = 0; i < count().value(); i++) {
                this.entries.add(new DataSegmentType(this._io, this, _root));
            }
        }
        private VlqBase128Le count;
        private ArrayList<DataSegmentType> entries;
        private Webassembly _root;
        private Webassembly.Section _parent;
        public VlqBase128Le count() { return count; }
        public ArrayList<DataSegmentType> entries() { return entries; }
        public Webassembly _root() { return _root; }
        public Webassembly.Section _parent() { return _parent; }
    }
    public static class ExportEntryType extends KaitaiStruct {
        public static ExportEntryType fromFile(String fileName) throws IOException {
            return new ExportEntryType(new ByteBufferKaitaiStream(fileName));
        }

        public ExportEntryType(KaitaiStream _io) {
            this(_io, null, null);
        }

        public ExportEntryType(KaitaiStream _io, Webassembly.ExportSection _parent) {
            this(_io, _parent, null);
        }

        public ExportEntryType(KaitaiStream _io, Webassembly.ExportSection _parent, Webassembly _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.fieldLen = new VlqBase128Le(this._io);
            this.fieldStr = new String(this._io.readBytes(fieldLen().value()), Charset.forName("UTF-8"));
            this.kind = Webassembly.KindType.byId(this._io.readU1());
            this.index = new VlqBase128Le(this._io);
        }
        private VlqBase128Le fieldLen;
        private String fieldStr;
        private KindType kind;
        private VlqBase128Le index;
        private Webassembly _root;
        private Webassembly.ExportSection _parent;
        public VlqBase128Le fieldLen() { return fieldLen; }
        public String fieldStr() { return fieldStr; }
        public KindType kind() { return kind; }
        public VlqBase128Le index() { return index; }
        public Webassembly _root() { return _root; }
        public Webassembly.ExportSection _parent() { return _parent; }
    }
    public static class FunctionBodyDataType extends KaitaiStruct {
        public static FunctionBodyDataType fromFile(String fileName) throws IOException {
            return new FunctionBodyDataType(new ByteBufferKaitaiStream(fileName));
        }

        public FunctionBodyDataType(KaitaiStream _io) {
            this(_io, null, null);
        }

        public FunctionBodyDataType(KaitaiStream _io, Webassembly.FunctionBodyType _parent) {
            this(_io, _parent, null);
        }

        public FunctionBodyDataType(KaitaiStream _io, Webassembly.FunctionBodyType _parent, Webassembly _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.localCount = new VlqBase128Le(this._io);
            locals = new ArrayList<LocalEntryType>(((Number) (localCount().value())).intValue());
            for (int i = 0; i < localCount().value(); i++) {
                this.locals.add(new LocalEntryType(this._io, this, _root));
            }
            this.code = this._io.readBytesFull();
        }
        private VlqBase128Le localCount;
        private ArrayList<LocalEntryType> locals;
        private byte[] code;
        private Webassembly _root;
        private Webassembly.FunctionBodyType _parent;
        public VlqBase128Le localCount() { return localCount; }
        public ArrayList<LocalEntryType> locals() { return locals; }
        public byte[] code() { return code; }
        public Webassembly _root() { return _root; }
        public Webassembly.FunctionBodyType _parent() { return _parent; }
    }
    public static class GlobalSection extends KaitaiStruct {
        public static GlobalSection fromFile(String fileName) throws IOException {
            return new GlobalSection(new ByteBufferKaitaiStream(fileName));
        }

        public GlobalSection(KaitaiStream _io) {
            this(_io, null, null);
        }

        public GlobalSection(KaitaiStream _io, Webassembly.Section _parent) {
            this(_io, _parent, null);
        }

        public GlobalSection(KaitaiStream _io, Webassembly.Section _parent, Webassembly _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.count = new VlqBase128Le(this._io);
            globals = new ArrayList<GlobalVariableType>(((Number) (count().value())).intValue());
            for (int i = 0; i < count().value(); i++) {
                this.globals.add(new GlobalVariableType(this._io, this, _root));
            }
        }
        private VlqBase128Le count;
        private ArrayList<GlobalVariableType> globals;
        private Webassembly _root;
        private Webassembly.Section _parent;
        public VlqBase128Le count() { return count; }
        public ArrayList<GlobalVariableType> globals() { return globals; }
        public Webassembly _root() { return _root; }
        public Webassembly.Section _parent() { return _parent; }
    }
    public static class ElementSection extends KaitaiStruct {
        public static ElementSection fromFile(String fileName) throws IOException {
            return new ElementSection(new ByteBufferKaitaiStream(fileName));
        }

        public ElementSection(KaitaiStream _io) {
            this(_io, null, null);
        }

        public ElementSection(KaitaiStream _io, Webassembly.Section _parent) {
            this(_io, _parent, null);
        }

        public ElementSection(KaitaiStream _io, Webassembly.Section _parent, Webassembly _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.count = new VlqBase128Le(this._io);
            entries = new ArrayList<ElemSegmentType>(((Number) (count().value())).intValue());
            for (int i = 0; i < count().value(); i++) {
                this.entries.add(new ElemSegmentType(this._io, this, _root));
            }
        }
        private VlqBase128Le count;
        private ArrayList<ElemSegmentType> entries;
        private Webassembly _root;
        private Webassembly.Section _parent;
        public VlqBase128Le count() { return count; }
        public ArrayList<ElemSegmentType> entries() { return entries; }
        public Webassembly _root() { return _root; }
        public Webassembly.Section _parent() { return _parent; }
    }
    public static class TypeSection extends KaitaiStruct {
        public static TypeSection fromFile(String fileName) throws IOException {
            return new TypeSection(new ByteBufferKaitaiStream(fileName));
        }

        public TypeSection(KaitaiStream _io) {
            this(_io, null, null);
        }

        public TypeSection(KaitaiStream _io, Webassembly.Section _parent) {
            this(_io, _parent, null);
        }

        public TypeSection(KaitaiStream _io, Webassembly.Section _parent, Webassembly _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.count = this._io.readU1();
            entries = new ArrayList<FuncType>(((Number) (count())).intValue());
            for (int i = 0; i < count(); i++) {
                this.entries.add(new FuncType(this._io, this, _root));
            }
        }
        private int count;
        private ArrayList<FuncType> entries;
        private Webassembly _root;
        private Webassembly.Section _parent;
        public int count() { return count; }
        public ArrayList<FuncType> entries() { return entries; }
        public Webassembly _root() { return _root; }
        public Webassembly.Section _parent() { return _parent; }
    }
    public static class FunctionBodyType extends KaitaiStruct {
        public static FunctionBodyType fromFile(String fileName) throws IOException {
            return new FunctionBodyType(new ByteBufferKaitaiStream(fileName));
        }

        public FunctionBodyType(KaitaiStream _io) {
            this(_io, null, null);
        }

        public FunctionBodyType(KaitaiStream _io, Webassembly.CodeSection _parent) {
            this(_io, _parent, null);
        }

        public FunctionBodyType(KaitaiStream _io, Webassembly.CodeSection _parent, Webassembly _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.bodySize = new VlqBase128Le(this._io);
            this._raw_data = this._io.readBytes(bodySize().value());
            KaitaiStream _io__raw_data = new ByteBufferKaitaiStream(_raw_data);
            this.data = new FunctionBodyDataType(_io__raw_data, this, _root);
        }
        private VlqBase128Le bodySize;
        private FunctionBodyDataType data;
        private Webassembly _root;
        private Webassembly.CodeSection _parent;
        private byte[] _raw_data;
        public VlqBase128Le bodySize() { return bodySize; }
        public FunctionBodyDataType data() { return data; }
        public Webassembly _root() { return _root; }
        public Webassembly.CodeSection _parent() { return _parent; }
        public byte[] _raw_data() { return _raw_data; }
    }
    public static class LocalEntryType extends KaitaiStruct {
        public static LocalEntryType fromFile(String fileName) throws IOException {
            return new LocalEntryType(new ByteBufferKaitaiStream(fileName));
        }

        public LocalEntryType(KaitaiStream _io) {
            this(_io, null, null);
        }

        public LocalEntryType(KaitaiStream _io, Webassembly.FunctionBodyDataType _parent) {
            this(_io, _parent, null);
        }

        public LocalEntryType(KaitaiStream _io, Webassembly.FunctionBodyDataType _parent, Webassembly _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.count = new VlqBase128Le(this._io);
            this.type = Webassembly.ValueType.byId(this._io.readU1());
        }
        private VlqBase128Le count;
        private ValueType type;
        private Webassembly _root;
        private Webassembly.FunctionBodyDataType _parent;
        public VlqBase128Le count() { return count; }
        public ValueType type() { return type; }
        public Webassembly _root() { return _root; }
        public Webassembly.FunctionBodyDataType _parent() { return _parent; }
    }
    public static class DataCountSection extends KaitaiStruct {
        public static DataCountSection fromFile(String fileName) throws IOException {
            return new DataCountSection(new ByteBufferKaitaiStream(fileName));
        }

        public DataCountSection(KaitaiStream _io) {
            this(_io, null, null);
        }

        public DataCountSection(KaitaiStream _io, Webassembly.Section _parent) {
            this(_io, _parent, null);
        }

        public DataCountSection(KaitaiStream _io, Webassembly.Section _parent, Webassembly _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.count = new VlqBase128Le(this._io);
        }
        private VlqBase128Le count;
        private Webassembly _root;
        private Webassembly.Section _parent;
        public VlqBase128Le count() { return count; }
        public Webassembly _root() { return _root; }
        public Webassembly.Section _parent() { return _parent; }
    }
    public static class ImportSection extends KaitaiStruct {
        public static ImportSection fromFile(String fileName) throws IOException {
            return new ImportSection(new ByteBufferKaitaiStream(fileName));
        }

        public ImportSection(KaitaiStream _io) {
            this(_io, null, null);
        }

        public ImportSection(KaitaiStream _io, Webassembly.Section _parent) {
            this(_io, _parent, null);
        }

        public ImportSection(KaitaiStream _io, Webassembly.Section _parent, Webassembly _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.count = this._io.readU1();
            if (count() > 0) {
                entries = new ArrayList<ImportEntry>(((Number) (count())).intValue());
                for (int i = 0; i < count(); i++) {
                    this.entries.add(new ImportEntry(this._io, this, _root));
                }
            }
        }
        private int count;
        private ArrayList<ImportEntry> entries;
        private Webassembly _root;
        private Webassembly.Section _parent;
        public int count() { return count; }
        public ArrayList<ImportEntry> entries() { return entries; }
        public Webassembly _root() { return _root; }
        public Webassembly.Section _parent() { return _parent; }
    }
    public static class GlobalVariableType extends KaitaiStruct {
        public static GlobalVariableType fromFile(String fileName) throws IOException {
            return new GlobalVariableType(new ByteBufferKaitaiStream(fileName));
        }

        public GlobalVariableType(KaitaiStream _io) {
            this(_io, null, null);
        }

        public GlobalVariableType(KaitaiStream _io, Webassembly.GlobalSection _parent) {
            this(_io, _parent, null);
        }

        public GlobalVariableType(KaitaiStream _io, Webassembly.GlobalSection _parent, Webassembly _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.type = new GlobalType(this._io, this, _root);
            this.init = new ArrayList<Integer>();
            {
                int _it;
                int i = 0;
                do {
                    _it = this._io.readU1();
                    this.init.add(_it);
                    i++;
                } while (!(_it == 11));
            }
        }
        private GlobalType type;
        private ArrayList<Integer> init;
        private Webassembly _root;
        private Webassembly.GlobalSection _parent;
        public GlobalType type() { return type; }
        public ArrayList<Integer> init() { return init; }
        public Webassembly _root() { return _root; }
        public Webassembly.GlobalSection _parent() { return _parent; }
    }
    public static class StartSection extends KaitaiStruct {
        public static StartSection fromFile(String fileName) throws IOException {
            return new StartSection(new ByteBufferKaitaiStream(fileName));
        }

        public StartSection(KaitaiStream _io) {
            this(_io, null, null);
        }

        public StartSection(KaitaiStream _io, Webassembly.Section _parent) {
            this(_io, _parent, null);
        }

        public StartSection(KaitaiStream _io, Webassembly.Section _parent, Webassembly _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.index = new VlqBase128Le(this._io);
        }
        private VlqBase128Le index;
        private Webassembly _root;
        private Webassembly.Section _parent;
        public VlqBase128Le index() { return index; }
        public Webassembly _root() { return _root; }
        public Webassembly.Section _parent() { return _parent; }
    }
    public static class TableType extends KaitaiStruct {
        public static TableType fromFile(String fileName) throws IOException {
            return new TableType(new ByteBufferKaitaiStream(fileName));
        }

        public TableType(KaitaiStream _io) {
            this(_io, null, null);
        }

        public TableType(KaitaiStream _io, KaitaiStruct _parent) {
            this(_io, _parent, null);
        }

        public TableType(KaitaiStream _io, KaitaiStruct _parent, Webassembly _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.elementType = Webassembly.ElemType.byId(this._io.readU1());
            this.limits = new ResizableLimitsType(this._io, this, _root);
        }
        private ElemType elementType;
        private ResizableLimitsType limits;
        private Webassembly _root;
        private KaitaiStruct _parent;
        public ElemType elementType() { return elementType; }
        public ResizableLimitsType limits() { return limits; }
        public Webassembly _root() { return _root; }
        public KaitaiStruct _parent() { return _parent; }
    }
    public static class FunctionSection extends KaitaiStruct {
        public static FunctionSection fromFile(String fileName) throws IOException {
            return new FunctionSection(new ByteBufferKaitaiStream(fileName));
        }

        public FunctionSection(KaitaiStream _io) {
            this(_io, null, null);
        }

        public FunctionSection(KaitaiStream _io, Webassembly.Section _parent) {
            this(_io, _parent, null);
        }

        public FunctionSection(KaitaiStream _io, Webassembly.Section _parent, Webassembly _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.count = new VlqBase128Le(this._io);
            types = new ArrayList<VlqBase128Le>(((Number) (count().value())).intValue());
            for (int i = 0; i < count().value(); i++) {
                this.types.add(new VlqBase128Le(this._io));
            }
        }
        private VlqBase128Le count;
        private ArrayList<VlqBase128Le> types;
        private Webassembly _root;
        private Webassembly.Section _parent;
        public VlqBase128Le count() { return count; }
        public ArrayList<VlqBase128Le> types() { return types; }
        public Webassembly _root() { return _root; }
        public Webassembly.Section _parent() { return _parent; }
    }
    public static class ResizableLimitsType extends KaitaiStruct {
        public static ResizableLimitsType fromFile(String fileName) throws IOException {
            return new ResizableLimitsType(new ByteBufferKaitaiStream(fileName));
        }

        public ResizableLimitsType(KaitaiStream _io) {
            this(_io, null, null);
        }

        public ResizableLimitsType(KaitaiStream _io, KaitaiStruct _parent) {
            this(_io, _parent, null);
        }

        public ResizableLimitsType(KaitaiStream _io, KaitaiStruct _parent, Webassembly _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.flags = this._io.readU1();
            this.initial = new VlqBase128Le(this._io);
            if (flags() == 1) {
                this.maximum = new VlqBase128Le(this._io);
            }
        }
        private int flags;
        private VlqBase128Le initial;
        private VlqBase128Le maximum;
        private Webassembly _root;
        private KaitaiStruct _parent;
        public int flags() { return flags; }
        public VlqBase128Le initial() { return initial; }
        public VlqBase128Le maximum() { return maximum; }
        public Webassembly _root() { return _root; }
        public KaitaiStruct _parent() { return _parent; }
    }
    public static class ElemSegmentType extends KaitaiStruct {
        public static ElemSegmentType fromFile(String fileName) throws IOException {
            return new ElemSegmentType(new ByteBufferKaitaiStream(fileName));
        }

        public ElemSegmentType(KaitaiStream _io) {
            this(_io, null, null);
        }

        public ElemSegmentType(KaitaiStream _io, Webassembly.ElementSection _parent) {
            this(_io, _parent, null);
        }

        public ElemSegmentType(KaitaiStream _io, Webassembly.ElementSection _parent, Webassembly _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.index = new VlqBase128Le(this._io);
            this.offset = new ArrayList<Integer>();
            {
                int _it;
                int i = 0;
                do {
                    _it = this._io.readU1();
                    this.offset.add(_it);
                    i++;
                } while (!(_it == 11));
            }
            this.numElem = new VlqBase128Le(this._io);
            elems = new ArrayList<VlqBase128Le>(((Number) (numElem().value())).intValue());
            for (int i = 0; i < numElem().value(); i++) {
                this.elems.add(new VlqBase128Le(this._io));
            }
        }
        private VlqBase128Le index;
        private ArrayList<Integer> offset;
        private VlqBase128Le numElem;
        private ArrayList<VlqBase128Le> elems;
        private Webassembly _root;
        private Webassembly.ElementSection _parent;
        public VlqBase128Le index() { return index; }
        public ArrayList<Integer> offset() { return offset; }
        public VlqBase128Le numElem() { return numElem; }
        public ArrayList<VlqBase128Le> elems() { return elems; }
        public Webassembly _root() { return _root; }
        public Webassembly.ElementSection _parent() { return _parent; }
    }
    public static class MemorySection extends KaitaiStruct {
        public static MemorySection fromFile(String fileName) throws IOException {
            return new MemorySection(new ByteBufferKaitaiStream(fileName));
        }

        public MemorySection(KaitaiStream _io) {
            this(_io, null, null);
        }

        public MemorySection(KaitaiStream _io, Webassembly.Section _parent) {
            this(_io, _parent, null);
        }

        public MemorySection(KaitaiStream _io, Webassembly.Section _parent, Webassembly _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.count = new VlqBase128Le(this._io);
            entries = new ArrayList<MemoryType>(((Number) (count().value())).intValue());
            for (int i = 0; i < count().value(); i++) {
                this.entries.add(new MemoryType(this._io, this, _root));
            }
        }
        private VlqBase128Le count;
        private ArrayList<MemoryType> entries;
        private Webassembly _root;
        private Webassembly.Section _parent;
        public VlqBase128Le count() { return count; }
        public ArrayList<MemoryType> entries() { return entries; }
        public Webassembly _root() { return _root; }
        public Webassembly.Section _parent() { return _parent; }
    }
    public static class UnimplementedSection extends KaitaiStruct {
        public static UnimplementedSection fromFile(String fileName) throws IOException {
            return new UnimplementedSection(new ByteBufferKaitaiStream(fileName));
        }

        public UnimplementedSection(KaitaiStream _io) {
            this(_io, null, null);
        }

        public UnimplementedSection(KaitaiStream _io, Webassembly.Section _parent) {
            this(_io, _parent, null);
        }

        public UnimplementedSection(KaitaiStream _io, Webassembly.Section _parent, Webassembly _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.nameLen = new VlqBase128Le(this._io);
            this.name = this._io.readBytes(nameLen().value());
            raw = new ArrayList<Integer>(((Number) (((_parent().header().payloadLen().value() - nameLen().value()) - 1))).intValue());
            for (int i = 0; i < ((_parent().header().payloadLen().value() - nameLen().value()) - 1); i++) {
                this.raw.add(this._io.readU1());
            }
        }
        private VlqBase128Le nameLen;
        private byte[] name;
        private ArrayList<Integer> raw;
        private Webassembly _root;
        private Webassembly.Section _parent;
        public VlqBase128Le nameLen() { return nameLen; }
        public byte[] name() { return name; }
        public ArrayList<Integer> raw() { return raw; }
        public Webassembly _root() { return _root; }
        public Webassembly.Section _parent() { return _parent; }
    }
    public static class GlobalType extends KaitaiStruct {
        public static GlobalType fromFile(String fileName) throws IOException {
            return new GlobalType(new ByteBufferKaitaiStream(fileName));
        }

        public GlobalType(KaitaiStream _io) {
            this(_io, null, null);
        }

        public GlobalType(KaitaiStream _io, KaitaiStruct _parent) {
            this(_io, _parent, null);
        }

        public GlobalType(KaitaiStream _io, KaitaiStruct _parent, Webassembly _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.contentType = Webassembly.ValueType.byId(this._io.readU1());
            this.mutability = Webassembly.MutabilityFlag.byId(this._io.readU1());
        }
        private ValueType contentType;
        private MutabilityFlag mutability;
        private Webassembly _root;
        private KaitaiStruct _parent;
        public ValueType contentType() { return contentType; }
        public MutabilityFlag mutability() { return mutability; }
        public Webassembly _root() { return _root; }
        public KaitaiStruct _parent() { return _parent; }
    }
    private byte[] magic;
    private long version;
    private Sections sections;
    private Webassembly _root;
    private KaitaiStruct _parent;
    public byte[] magic() { return magic; }
    public long version() { return version; }
    public Sections sections() { return sections; }
    public Webassembly _root() { return _root; }
    public KaitaiStruct _parent() { return _parent; }
}
