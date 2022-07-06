package org.qbicc.plugin.lowering;

import java.util.List;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Value;
import org.qbicc.object.FunctionDeclaration;
import org.qbicc.object.ProgramModule;
import org.qbicc.type.FunctionType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.definition.element.ExecutableElement;

public class ThrowLoweringBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public ThrowLoweringBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    public BasicBlock throw_(final Value value) {
        TypeSystem ts = ctxt.getTypeSystem();
        ExecutableElement el = getDelegate().getRootElement();
        ProgramModule programModule = ctxt.getOrAddProgramModule(el);
        FunctionType abortSignature = ts.getFunctionType(ts.getVoidType(), List.of());
        FunctionDeclaration fd = programModule.declareFunction(null, "abort", abortSignature);
        return callNoReturn(pointerHandle(ctxt.getLiteralFactory().literalOf(fd)), List.of());
    }
}
