package org.qbicc.plugin.opt;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.qbicc.graph.atomic.AccessModes.SingleUnshared;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qbicc.graph.And;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.Comp;
import org.qbicc.graph.Or;
import org.qbicc.graph.Value;
import org.qbicc.graph.schedule.Schedule;
import org.qbicc.test.AbstractCompilerTestCase;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.MethodBody;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.LocalVariableElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.generic.BaseTypeSignature;
import org.qbicc.type.generic.ClassSignature;
import org.qbicc.type.generic.MethodSignature;

public final class TestSimpleOptBasicBlockBuilderBooleanLogic extends AbstractCompilerTestCase {

    ExecutableElement element;

    @BeforeEach
    public void setUpEach() {
        final DefinedTypeDefinition.Builder typeBuilder = DefinedTypeDefinition.Builder.basic();
        typeBuilder.setName("TestClass");
        typeBuilder.setDescriptor(ClassTypeDescriptor.synthesize(bootClassContext, "TestClass"));
        typeBuilder.setModifiers(ClassFile.ACC_SUPER | ClassFile.ACC_PUBLIC);
        typeBuilder.setSignature(ClassSignature.synthesize(bootClassContext, null, List.of()));
        typeBuilder.setSimpleName("TestClass");
        typeBuilder.setInitializer((index, enclosing, builder) -> builder.build(), 0);
        final DefinedTypeDefinition enclosingType = typeBuilder.build();
        final MethodElement.Builder builder = MethodElement.builder("testMethod", MethodDescriptor.VOID_METHOD_DESCRIPTOR, 0);
        builder.setEnclosingType(enclosingType);
        builder.setSignature(MethodSignature.VOID_METHOD_SIGNATURE);
        builder.setModifiers(ClassFile.ACC_STATIC);
        builder.setParameters(List.of());
        builder.setMethodBodyFactory((index, e) -> {
            final BasicBlockBuilder bbb = BasicBlockBuilder.simpleBuilder(ts, e);
            BasicBlock emptyBlock = bbb.unreachable();
            bbb.finish();
            return MethodBody.of(
                emptyBlock,
                Schedule.forMethod(emptyBlock),
                null,
                List.of()
            );
        }, 0);
        element = builder.build();
    }

    private LocalVariableElement createLocalVar(String name, ValueType type) {
        final LocalVariableElement.Builder builder = LocalVariableElement.builder(name, BaseTypeDescriptor.V, 0);
        builder.setEnclosingType(element.getEnclosingType());
        builder.setType(type);
        builder.setSignature(BaseTypeSignature.V);
        builder.setTypeParameterContext(element.getEnclosingType());
        return builder.build();
    }

    @Test
    public void testDeMorgansAnd2Or() {
        final BasicBlockBuilder bbb = makeBlockBuilder();
        Value v1 = bbb.load(bbb.localVariable(createLocalVar("v1", ts.getBooleanType())), SingleUnshared);
        Value v2 = bbb.load(bbb.localVariable(createLocalVar("v2", ts.getBooleanType())), SingleUnshared);
        final Value res = bbb.and(bbb.complement(v1), bbb.complement(v2));
        assertTrue(res instanceof Comp comp
            && comp.getInput() instanceof Or or
            && or.getLeftInput().equals(v1)
            && or.getRightInput().equals(v2)
        );
    }

    @Test
    public void testDeMorgansOr2And() {
        final BasicBlockBuilder bbb = makeBlockBuilder();
        Value v1 = bbb.load(bbb.localVariable(createLocalVar("v1", ts.getBooleanType())), SingleUnshared);
        Value v2 = bbb.load(bbb.localVariable(createLocalVar("v2", ts.getBooleanType())), SingleUnshared);
        final Value res = bbb.or(bbb.complement(v1), bbb.complement(v2));
        assertTrue(res instanceof Comp comp
            && comp.getInput() instanceof And and
            && and.getLeftInput().equals(v1)
            && and.getRightInput().equals(v2)
        );
    }

    @Test
    public void testDistributiveAnd() {
        final BasicBlockBuilder bbb = makeBlockBuilder();
        Value v1 = bbb.load(bbb.localVariable(createLocalVar("v1", ts.getBooleanType())), SingleUnshared);
        Value v2 = bbb.load(bbb.localVariable(createLocalVar("v2", ts.getBooleanType())), SingleUnshared);
        Value v3 = bbb.load(bbb.localVariable(createLocalVar("v3", ts.getBooleanType())), SingleUnshared);
        final Value res = bbb.or(bbb.and(v1, v2), bbb.and(v1, v3));
        assertTrue(res instanceof And and
            && and.getLeftInput().equals(v1)
            && and.getRightInput() instanceof Or or
            && or.inputsEqual(v2, v3)
        );
    }

    private BasicBlockBuilder makeBlockBuilder() {
        final BasicBlockBuilder bbb = new SimpleOptBasicBlockBuilder(ctxt, BasicBlockBuilder.simpleBuilder(ts, element));
        bbb.startMethod(List.of());
        bbb.begin(new BlockLabel());
        return bbb;
    }
}
