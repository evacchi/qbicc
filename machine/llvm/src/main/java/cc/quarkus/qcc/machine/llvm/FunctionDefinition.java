package cc.quarkus.qcc.machine.llvm;

/**
 *
 */
public interface FunctionDefinition extends Function, BasicBlock {

    // overrides

    FunctionDefinition returns(LLValue returnType);

    FunctionDefinition linkage(Linkage linkage);

    FunctionDefinition visibility(Visibility visibility);

    FunctionDefinition dllStorageClass(DllStorageClass dllStorageClass);

    FunctionDefinition callingConvention(CallingConvention callingConvention);

    FunctionDefinition addressNaming(AddressNaming addressNaming);

    FunctionDefinition addressSpace(int addressSpace);

    FunctionDefinition alignment(int alignment);

    FunctionDefinition meta(String name, LLValue metadata);

    FunctionDefinition comment(String comment);

    // additional properties

    FunctionDefinition section(String section);

    FunctionDefinition preemption(RuntimePreemption preemption);
}
