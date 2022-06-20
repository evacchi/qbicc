package org.qbicc.plugin.llvm;

import org.qbicc.machine.llvm.FunctionAttributes;
import org.qbicc.machine.llvm.FunctionDefinition;
import org.qbicc.machine.llvm.LLBasicBlock;
import org.qbicc.machine.llvm.LLBuilder;
import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.Linkage;
import org.qbicc.machine.llvm.Module;
import org.qbicc.machine.llvm.Types;

import java.util.List;

public abstract class LLVMPseudoIntrinsics {

    public static LLVMPseudoIntrinsics forReferenceAddressSpace(Module module, int referenceAddressSpace) {
        LLValue rawPtrType = Types.ptrTo(Types.i8, 0);
        if (referenceAddressSpace == 0) {
            return new LLVMPseudoIntrinsics(module, rawPtrType, rawPtrType) {
                @Override
                protected void buildCastPtrToRef(LLBuilder builder, LLValue val) {
                    builder.ret(rawPtrType, val);
                }

                @Override
                protected void buildCastRefToPtr(LLBuilder builder, LLValue val) {
                    builder.ret(rawPtrType, val);
                }
            };
        } else {
            LLValue collectedPtrType = Types.ptrTo(Types.i8, 1);
            return new LLVMPseudoIntrinsics(module, rawPtrType, collectedPtrType) {
                @Override
                protected void buildCastPtrToRef(LLBuilder builder, LLValue val) {
                    builder.ret(
                        collectedPtrType,
                        builder.addrspacecast(rawPtrType, val, collectedPtrType).asLocal("ref")
                    );
                }

                @Override
                protected void buildCastRefToPtr(LLBuilder builder, LLValue val) {
                    builder.ret(
                        rawPtrType,
                        builder.addrspacecast(collectedPtrType, val, rawPtrType).asLocal("ptr")
                    );
                }
            };
        }
    }


    private final Module module;

    private final LLValue rawPtrType;
    private final LLValue collectedPtrType;

    private LLValue castPtrToRef;
    private LLValue castPtrToRefType;

    private LLValue castRefToPtr;
    private LLValue castRefToPtrType;

    private LLVMPseudoIntrinsics(Module module, LLValue rawPtrType, LLValue collectedPtrType) {
        this.module = module;

        this.rawPtrType = rawPtrType;
        this.collectedPtrType = collectedPtrType;
    }

    private FunctionDefinition createCastPtrToRef() {
        FunctionDefinition func = module.define("qbicc.internal.cast_ptr_to_ref");
        LLBasicBlock block = func.createBlock();
        LLBuilder builder = LLBuilder.newBuilder(block);

        func.linkage(Linkage.PRIVATE);
        func.returns(collectedPtrType);
        func.attribute(FunctionAttributes.alwaysinline).attribute(FunctionAttributes.gcLeafFunction);
        LLValue val = func.param(rawPtrType).name("ptr").asValue();

        buildCastPtrToRef(builder, val);

        return func;
    }


    protected abstract void buildCastPtrToRef(LLBuilder builder, LLValue val);

    protected void _buildCastPtrToRef(LLBuilder builder, LLValue val) {
        if (rawPtrType.equals(collectedPtrType)) {
            builder.ret(
                collectedPtrType,
                val
            );
        } else {
            builder.ret(
                collectedPtrType,
                builder.addrspacecast(rawPtrType, val, collectedPtrType).asLocal("ref")
            );
        }
    }

    private FunctionDefinition createCastRefToPtr() {
        FunctionDefinition func = module.define("qbicc.internal.cast_ref_to_ptr");
        LLBasicBlock block = func.createBlock();
        LLBuilder builder = LLBuilder.newBuilder(block);

        func.linkage(Linkage.PRIVATE);
        func.returns(rawPtrType);
        func.attribute(FunctionAttributes.alwaysinline).attribute(FunctionAttributes.gcLeafFunction);
        LLValue val = func.param(collectedPtrType).name("ref").asValue();

        buildCastRefToPtr(builder, val);

        return func;
    }

    protected abstract void buildCastRefToPtr(LLBuilder builder, LLValue val);

    protected void _buildCastRefToPtr(LLBuilder builder, LLValue val) {
        if (rawPtrType.equals(collectedPtrType)) {
            builder.ret(
                rawPtrType,
                val
            );
        } else {
            builder.ret(
                rawPtrType,
                builder.addrspacecast(collectedPtrType, val, rawPtrType).asLocal("ptr")
            );
        }
    }

    private void ensureCastPtrToRef() {
        if (castPtrToRef == null) {
            castPtrToRef = createCastPtrToRef().asGlobal();
            castPtrToRefType = Types.function(collectedPtrType, List.of(rawPtrType), false);
        }
    }

    private void ensureCastRefToPtr() {
        if (castRefToPtr == null) {
            castRefToPtr = createCastRefToPtr().asGlobal();
            castRefToPtrType = Types.function(rawPtrType, List.of(collectedPtrType), false);
        }
    }

    public LLValue getRawPtrType() {
        return rawPtrType;
    }

    public LLValue getCollectedPtrType() {
        return collectedPtrType;
    }

    public LLValue getCastPtrToRef() {
        ensureCastPtrToRef();
        return castPtrToRef;
    }

    public LLValue getCastPtrToRefType() {
        ensureCastPtrToRef();
        return castPtrToRefType;
    }

    public LLValue getCastRefToPtr() {
        ensureCastRefToPtr();
        return castRefToPtr;
    }

    public LLValue getCastRefToPtrType() {
        ensureCastRefToPtr();
        return castRefToPtrType;
    }
}
