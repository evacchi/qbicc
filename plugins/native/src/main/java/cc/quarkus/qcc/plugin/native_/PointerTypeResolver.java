package cc.quarkus.qcc.plugin.native_;

import static cc.quarkus.qcc.runtime.CNative.*;

import java.util.List;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.literal.TypeIdLiteral;
import cc.quarkus.qcc.graph.literal.ValueArrayTypeIdLiteral;
import cc.quarkus.qcc.type.ArrayType;
import cc.quarkus.qcc.type.FunctionType;
import cc.quarkus.qcc.type.PointerType;
import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.annotation.Annotation;
import cc.quarkus.qcc.type.annotation.IntAnnotationValue;
import cc.quarkus.qcc.type.annotation.type.TypeAnnotation;
import cc.quarkus.qcc.type.annotation.type.TypeAnnotationList;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.DescriptorTypeResolver;
import cc.quarkus.qcc.type.descriptor.ArrayTypeDescriptor;
import cc.quarkus.qcc.type.descriptor.BaseTypeDescriptor;
import cc.quarkus.qcc.type.descriptor.ClassTypeDescriptor;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;
import cc.quarkus.qcc.type.generic.AnyTypeArgument;
import cc.quarkus.qcc.type.generic.BaseTypeSignature;
import cc.quarkus.qcc.type.generic.BoundTypeArgument;
import cc.quarkus.qcc.type.generic.ClassTypeSignature;
import cc.quarkus.qcc.type.generic.MethodSignature;
import cc.quarkus.qcc.type.generic.ParameterizedSignature;
import cc.quarkus.qcc.type.generic.ReferenceTypeSignature;
import cc.quarkus.qcc.type.generic.TypeArgument;
import cc.quarkus.qcc.type.generic.TypeSignature;
import cc.quarkus.qcc.type.generic.Variance;

/**
 * This type resolver is responsible for translating pointer types from reference types to actual pointer types.
 */
public class PointerTypeResolver implements DescriptorTypeResolver.Delegating {
    private final ClassContext classCtxt;
    private final CompilationContext ctxt;
    private final DescriptorTypeResolver delegate;

    public PointerTypeResolver(final ClassContext classCtxt, final DescriptorTypeResolver delegate) {
        this.classCtxt = classCtxt;
        ctxt = classCtxt.getCompilationContext();
        this.delegate = delegate;
    }

    public DescriptorTypeResolver getDelegate() {
        return delegate;
    }

    public ValueType resolveTypeFromDescriptor(TypeDescriptor descriptor, List<ParameterizedSignature> typeParamCtxt, TypeSignature signature, TypeAnnotationList visibleAnnotations, TypeAnnotationList invisibleAnnotations) {
        boolean restrict = false;
        for (TypeAnnotation typeAnnotation : visibleAnnotations) {
            Annotation annotation = typeAnnotation.getAnnotation();
            if (annotation.getDescriptor().getClassName().equals(Native.ANN_RESTRICT)) {
                restrict = true;
                break;
            }
        }
        // special handling for pointers and functions
        out: if (descriptor instanceof ClassTypeDescriptor) {
            ClassTypeDescriptor ctd = (ClassTypeDescriptor) descriptor;
            if (ctd.getPackageName().equals(Native.NATIVE_PKG)) {
                if (ctd.getClassName().equals(Native.PTR)) {
                    PointerType pointerType;
                    // check the signature
                    if (signature instanceof ClassTypeSignature) {
                        ClassTypeSignature sig = (ClassTypeSignature) signature;
                        if (! sig.getIdentifier().equals(Native.PTR)) {
                            ctxt.warning("Incorrect generic signature (expected a %s but got \"%s\")", ptr.class, sig);
                            break out;
                        }
                        List<TypeArgument> args = sig.getTypeArguments();
                        if (args.size() != 1) {
                            ctxt.warning("Incorrect number of generic signature arguments (expected a %s but got \"%s\")", ptr.class, sig);
                            break out;
                        }
                        TypeArgument typeArgument = args.get(0);
                        ValueType pointeeType;
                        visibleAnnotations = visibleAnnotations.onTypeArgument(0);
                        invisibleAnnotations = invisibleAnnotations.onTypeArgument(0);
                        if (typeArgument instanceof AnyTypeArgument) {
                            pointeeType = classCtxt.resolveTypeFromDescriptor(BaseTypeDescriptor.V, typeParamCtxt, BaseTypeSignature.V, visibleAnnotations, invisibleAnnotations);
                        } else {
                            assert typeArgument instanceof BoundTypeArgument;
                            BoundTypeArgument bound = (BoundTypeArgument) typeArgument;
                            if (bound.getVariance() == Variance.INVARIANT) {
                                // looks right, get the proper pointee type
                                ReferenceTypeSignature pointeeSig = bound.getBound();
                                // todo: use context to resolve type variable bounds
                                TypeDescriptor pointeeDesc = pointeeSig.asDescriptor(classCtxt);
                                pointeeType = classCtxt.resolveTypeFromDescriptor(pointeeDesc, typeParamCtxt, pointeeSig, visibleAnnotations, invisibleAnnotations);
                            } else {
                                ctxt.warning("Incorrect number of generic signature arguments (expected a %s but got \"%s\")", ptr.class, sig);
                                pointeeType = classCtxt.resolveTypeFromDescriptor(BaseTypeDescriptor.V, typeParamCtxt, BaseTypeSignature.V, visibleAnnotations, invisibleAnnotations);
                            }
                        }
                        pointerType = pointeeType.getPointer();
                    } else {
                        // todo: how can we cleanly get the location from here?
                        ctxt.warning("Generic signature type mismatch (expected a %s but got a %s)", ClassTypeSignature.class, signature.getClass());
                        pointerType = ctxt.getTypeSystem().getVoidType().getPointer();
                    }
                    return restrict ? pointerType.asRestrict() : pointerType;
                } else {
                    break out;
                }
            } else {
                break out;
            }
        } else if (descriptor instanceof ArrayTypeDescriptor) {
            TypeDescriptor elemDesc = ((ArrayTypeDescriptor) descriptor).getElementTypeDescriptor();
            if (elemDesc instanceof ArrayTypeDescriptor) {
                // check to see if the delegate gives us a reference array of "real" arrays.
                ValueType delegateResolved = getDelegate().resolveTypeFromDescriptor(descriptor, typeParamCtxt, signature, visibleAnnotations, invisibleAnnotations);
                if (delegateResolved instanceof ReferenceType) {
                    ReferenceType refResolved = (ReferenceType) delegateResolved;
                    TypeIdLiteral upperBound = refResolved.getUpperBound();
                    if (upperBound instanceof ValueArrayTypeIdLiteral) {
                        // it's a value array, but is the value an array type?
                        ValueType elementType = ((ValueArrayTypeIdLiteral) upperBound).getElementType();
                        if (elementType instanceof ArrayType) {
                            // yes, it's really an array of "real" arrays, so return a "real" array here as well.
                            return ctxt.getTypeSystem().getArrayType(elementType, detectArraySize(visibleAnnotations));
                        }
                    }
                }
                // else fall out
            } else if (elemDesc instanceof ClassTypeDescriptor) {
                // it's a class, not a primitive; see if it gets magically converted to a primitive reference array
                ValueType delegateResolved = getDelegate().resolveTypeFromDescriptor(descriptor, typeParamCtxt, signature, visibleAnnotations, invisibleAnnotations);
                if (delegateResolved instanceof ReferenceType) {
                    ReferenceType refResolved = (ReferenceType) delegateResolved;
                    TypeIdLiteral upperBound = refResolved.getUpperBound();
                    if (upperBound instanceof ValueArrayTypeIdLiteral) {
                        // aha! this means it's an array of native type and should be transformed to a "real" array type
                        return ctxt.getTypeSystem().getArrayType(((ValueArrayTypeIdLiteral) upperBound).getElementType(), detectArraySize(visibleAnnotations));
                    }
                }
                // else fall out
            }
        }
        if (restrict) {
            ctxt.error("Only pointers can have `restrict` qualifier (\"%s\" \"%s\")", descriptor, signature);
        }
        return getDelegate().resolveTypeFromDescriptor(descriptor, typeParamCtxt, signature, visibleAnnotations, invisibleAnnotations);
    }

    private int detectArraySize(final TypeAnnotationList visibleAnnotations) {
        int arraySize = 0;
        for (TypeAnnotation annotation : visibleAnnotations) {
            ClassTypeDescriptor desc = annotation.getAnnotation().getDescriptor();
            if (desc.getPackageName().equals(Native.NATIVE_PKG) && desc.getClassName().equals(Native.ANN_ARRAY_SIZE)) {
                arraySize = ((IntAnnotationValue) annotation.getAnnotation().getValue("value")).intValue();
            }
        }
        return arraySize;
    }

    public ValueType resolveTypeFromMethodDescriptor(final TypeDescriptor descriptor, final List<ParameterizedSignature> typeParamCtxt, final TypeSignature signature, final TypeAnnotationList visibleAnnotations, final TypeAnnotationList invisibleAnnotations) {
        return deArray(getDelegate().resolveTypeFromMethodDescriptor(descriptor, typeParamCtxt, signature, visibleAnnotations, invisibleAnnotations));
    }

    private FunctionType translateFunctionType(FunctionType functionType) {
        ValueType returnType = deArray(functionType.getReturnType());
        int cnt = functionType.getParameterCount();
        ValueType[] paramTypes = new ValueType[cnt];
        for (int i = 0; i < cnt; i ++) {
            paramTypes[i] = deArray(functionType.getParameterType(i));
        }
        return classCtxt.getTypeSystem().getFunctionType(returnType, paramTypes);
    }

    private ValueType deArray(ValueType orig) {
        return orig instanceof ArrayType ? deArray(((ArrayType) orig).getElementType()).getPointer() : orig;
    }
}
