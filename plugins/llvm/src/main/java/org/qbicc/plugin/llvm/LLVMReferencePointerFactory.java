package org.qbicc.plugin.llvm;

import org.qbicc.machine.llvm.LLValue;

public interface LLVMReferencePointerFactory {
    LLValue makeReferencePointer();
    boolean isRawPointer();
}
