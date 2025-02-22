package org.qbicc.plugin.reflection;

import static org.qbicc.graph.atomic.AccessModes.*;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.ParameterValue;
import org.qbicc.graph.Value;
import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.graph.literal.BooleanLiteral;
import org.qbicc.graph.literal.ByteArrayLiteral;
import org.qbicc.graph.literal.FloatLiteral;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.schedule.Schedule;
import org.qbicc.interpreter.Thrown;
import org.qbicc.interpreter.Vm;
import org.qbicc.interpreter.VmArray;
import org.qbicc.interpreter.VmClass;
import org.qbicc.interpreter.VmClassLoader;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmPrimitiveClass;
import org.qbicc.interpreter.VmReferenceArray;
import org.qbicc.interpreter.VmString;
import org.qbicc.interpreter.VmThread;
import org.qbicc.interpreter.VmThrowableClass;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.plugin.layout.LayoutInfo;
import org.qbicc.plugin.patcher.Patcher;
import org.qbicc.plugin.reachability.ReachabilityRoots;
import org.qbicc.pointer.Pointer;
import org.qbicc.pointer.StaticFieldPointer;
import org.qbicc.pointer.StaticMethodPointer;
import org.qbicc.type.CompoundType;
import org.qbicc.type.FloatType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.InvokableType;
import org.qbicc.type.UnsignedIntegerType;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.annotation.AnnotationValue;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.MethodBody;
import org.qbicc.type.definition.MethodBodyFactory;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.classfile.ConstantPool;
import org.qbicc.type.definition.element.AnnotatedElement;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.Element;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.definition.element.NestedClassElement;
import org.qbicc.type.definition.element.ParameterElement;
import org.qbicc.type.definition.element.StaticFieldElement;
import org.qbicc.type.definition.element.StaticMethodElement;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;
import org.qbicc.type.generic.BaseTypeSignature;
import org.qbicc.type.generic.MethodSignature;
import org.qbicc.type.generic.TypeSignature;
import org.qbicc.type.methodhandle.MethodHandleKind;

/**
 * Hub for reflection support.
 */
public final class Reflection {
    private static final int IS_METHOD = 1 << 16;
    private static final int IS_CONSTRUCTOR = 1 << 17;
    private static final int IS_FIELD = 1 << 18;
    private static final int IS_TYPE = 1 << 19;
    private static final int CALLER_SENSITIVE = 1 << 20;
    private static final int TRUSTED_FINAL = 1 << 21;

    static final int KIND_SHIFT = 24;

    static final int KIND_GET_FIELD = MethodHandleKind.GET_FIELD.getId();
    static final int KIND_GET_STATIC = MethodHandleKind.GET_STATIC.getId();
    static final int KIND_PUT_FIELD = MethodHandleKind.PUT_FIELD.getId();
    static final int KIND_PUT_STATIC = MethodHandleKind.PUT_STATIC.getId();
    static final int KIND_INVOKE_VIRTUAL = MethodHandleKind.INVOKE_VIRTUAL.getId();
    static final int KIND_INVOKE_STATIC = MethodHandleKind.INVOKE_STATIC.getId();
    static final int KIND_INVOKE_SPECIAL = MethodHandleKind.INVOKE_SPECIAL.getId();
    static final int KIND_NEW_INVOKE_SPECIAL = MethodHandleKind.NEW_INVOKE_SPECIAL.getId();
    static final int KIND_INVOKE_INTERFACE = MethodHandleKind.INVOKE_INTERFACE.getId();

    static final int KIND_MASK = 0xf;

    private static final AttachmentKey<Reflection> KEY = new AttachmentKey<>();

    private final CompilationContext ctxt;

    private final Map<VmClass, VmObject> cpMap = new ConcurrentHashMap<>();
    private final Map<VmClass, VmReferenceArray> declaredFields = new ConcurrentHashMap<>();
    private final Map<VmClass, VmReferenceArray> declaredPublicFields = new ConcurrentHashMap<>();
    private final Map<VmClass, VmReferenceArray> declaredMethods = new ConcurrentHashMap<>();
    private final Map<VmClass, VmReferenceArray> declaredPublicMethods = new ConcurrentHashMap<>();
    private final Map<VmClass, VmReferenceArray> declaredConstructors = new ConcurrentHashMap<>();
    private final Map<VmClass, VmReferenceArray> declaredPublicConstructors = new ConcurrentHashMap<>();
    private final Map<VmObject, ConstantPool> cpObjs = new ConcurrentHashMap<>();
    private final Map<AnnotatedElement, VmArray> annotatedElements = new ConcurrentHashMap<>();
    private final Map<Element, VmObject> reflectionObjects = new ConcurrentHashMap<>();
    private final Map<LoadedTypeDefinition, VmArray> annotatedTypes = new ConcurrentHashMap<>();
    private final Map<InvokableType, CompoundType> functionCallStructures = new ConcurrentHashMap<>();
    private final Map<Element, Map<MethodHandleKind, Pointer>> dispatcherCache = new ConcurrentHashMap<>();
    private final VmArray noAnnotations;
    private final Vm vm;
    private final VmClass cpClass;
    private final VmClass classClass;
    private final VmClass fieldClass;
    private final VmClass methodClass;
    private final VmClass constructorClass;
    private final VmClass rmnClass;
    private final VmClass memberNameClass;
    private final VmThrowableClass linkageErrorClass;
    private final VmThrowableClass invocationTargetExceptionClass;
    private final VmClass byteClass;
    private final VmClass shortClass;
    private final VmClass integerClass;
    private final VmClass longClass;
    private final VmClass characterClass;
    private final VmClass floatClass;
    private final VmClass doubleClass;
    private final VmClass booleanClass;
    private final ConstructorElement cpCtor;
    private final ConstructorElement fieldCtor;
    private final ConstructorElement methodCtor;
    private final ConstructorElement ctorCtor;
    private final ConstructorElement rmnCtor;
    // byte refKind, Class<?> defClass, String name, Object(Class|MethodType) type
    private final ConstructorElement memberName4Ctor;

    // reflection fields
    // MemberName
    final FieldElement memberNameClazzField; // Class
    final FieldElement memberNameNameField; // String
    final FieldElement memberNameTypeField; // Object
    final FieldElement memberNameFlagsField; // int
    final FieldElement memberNameMethodField; // ResolvedMethodName
    final FieldElement memberNameIndexField; // int (injected)
    final FieldElement memberNameResolvedField; // boolean (injected)
    final FieldElement memberNameExactDispatcherField; // void (<static method>*)(void *retPtr, const void *argsPtr) (injected)
    // Field
    private final FieldElement fieldClazzField; // Class
    private final FieldElement fieldSlotField; // int
    private final FieldElement fieldNameField; // String
    private final FieldElement fieldTypeField; // Class
    // Method
    private final FieldElement methodClazzField; // Class
    private final FieldElement methodSlotField; // int
    private final FieldElement methodNameField; // String
    private final FieldElement methodReturnTypeField; // Class
    private final FieldElement methodParameterTypesField; // Class[]
    // Constructor
    private final FieldElement ctorClazzField; // Class
    private final FieldElement ctorSlotField; // int
    private final FieldElement ctorParameterTypesField; // Class[]
    // ResolvedMethodName (injected fields)
    private final FieldElement rmnIndexField; // int
    private final FieldElement rmnClazzField; // Class
    // MethodType
    private final FieldElement methodTypePTypesField; // Class[]
    private final FieldElement methodTypeRTypeField; // Class
    // MethodHandle
    final FieldElement methodHandleLambdaFormField;
    // LambdaForm
    final FieldElement lambdaFormMemberNameField;
    // MethodHandleAccessorFactory
    final MethodElement newMethodAccessorMethod;
    final MethodElement newConstructorAccessorMethod;

    // box type fields
    private final FieldElement byteValueField;
    private final FieldElement shortValueField;
    private final FieldElement integerValueField;
    private final FieldElement longValueField;
    private final FieldElement characterValueField;
    private final FieldElement floatValueField;
    private final FieldElement doubleValueField;
    private final FieldElement booleanValueField;

    // cached methods for method handles
    final MethodElement methodHandleNativesFindMethodHandleType;
    final MethodElement methodHandleNativesLinkMethod;
    final MethodElement methodHandleNativesResolve;
    // our injected ones
    final MethodElement methodHandleCheckType;

    private Reflection(CompilationContext ctxt) {
        this.ctxt = ctxt;
        ClassContext classContext = ctxt.getBootstrapClassContext();

        // Field injections (*early*)
        Patcher patcher = Patcher.get(ctxt);

        patcher.addField(classContext, "java/lang/invoke/MemberName", "index", BaseTypeDescriptor.I, this::resolveIndexField, 0, 0);
        patcher.addField(classContext, "java/lang/invoke/MemberName", "resolved", BaseTypeDescriptor.Z, this::resolveResolvedField, 0, 0);
        patcher.addField(classContext, "java/lang/invoke/MemberName", "exactDispatcher", BaseTypeDescriptor.V, this::resolveExactDispatcherField, 0, 0);

        patcher.addField(classContext, "java/lang/invoke/ResolvedMethodName", "index", BaseTypeDescriptor.I, this::resolveIndexField, 0, 0);
        patcher.addField(classContext, "java/lang/invoke/ResolvedMethodName", "clazz", ClassTypeDescriptor.synthesize(classContext, "java/lang/Class"), this::resolveClazzField, 0, 0);

        // VM
        vm = ctxt.getVm();
        noAnnotations = vm.newByteArray(new byte[2]);
        // ConstantPool
        LoadedTypeDefinition cpDef = classContext.findDefinedType("jdk/internal/reflect/ConstantPool").load();
        cpClass = cpDef.getVmClass();
        cpCtor = cpDef.getConstructor(0);
        vm.registerInvokable(cpDef.requireSingleMethod(me -> me.nameEquals("getIntAt0")), this::getIntAt0);
        vm.registerInvokable(cpDef.requireSingleMethod(me -> me.nameEquals("getLongAt0")), this::getLongAt0);
        vm.registerInvokable(cpDef.requireSingleMethod(me -> me.nameEquals("getFloatAt0")), this::getFloatAt0);
        vm.registerInvokable(cpDef.requireSingleMethod(me -> me.nameEquals("getDoubleAt0")), this::getDoubleAt0);
        vm.registerInvokable(cpDef.requireSingleMethod(me -> me.nameEquals("getUTF8At0")), this::getUTF8At0);
        // Class
        LoadedTypeDefinition classDef = classContext.findDefinedType("java/lang/Class").load();
        classClass = classDef.getVmClass();
        vm.registerInvokable(classDef.requireSingleMethod(me -> me.nameEquals("getRawAnnotations")), this::getClassRawAnnotations);
        vm.registerInvokable(classDef.requireSingleMethod(me -> me.nameEquals("getDeclaredFields0")), this::getClassDeclaredFields0);
        vm.registerInvokable(classDef.requireSingleMethod(me -> me.nameEquals("getDeclaredMethods0")), this::getClassDeclaredMethods0);
        vm.registerInvokable(classDef.requireSingleMethod(me -> me.nameEquals("getDeclaredConstructors0")), this::getClassDeclaredConstructors0);
        vm.registerInvokable(classDef.requireSingleMethod(me -> me.nameEquals("getSimpleBinaryName")), (thread, target, args) -> {
            if (target instanceof VmPrimitiveClass) {
                return null;
            }
            NestedClassElement enc = ((VmClass) target).getTypeDefinition().getEnclosingNestedClass();
            return enc == null ? null : vm.intern(enc.getName());
        });
        vm.registerInvokable(classDef.requireSingleMethod(me -> me.nameEquals("getConstantPool")), (thread, target, args) -> {
            // force the object to be created
            getConstantPoolForClass((VmClass) target);
            // return the CP object
            return cpObjs.get(target);
        });
        LoadedTypeDefinition fieldDef = classContext.findDefinedType("java/lang/reflect/Field").load();
        fieldClass = fieldDef.getVmClass();
        fieldCtor = fieldDef.requireSingleConstructor(ce -> ce.getDescriptor().getParameterTypes().size() == 8);
        LoadedTypeDefinition methodDef = classContext.findDefinedType("java/lang/reflect/Method").load();
        methodClass = methodDef.getVmClass();
        methodCtor = methodDef.requireSingleConstructor(ce -> ce.getDescriptor().getParameterTypes().size() == 11);
        LoadedTypeDefinition ctorDef = classContext.findDefinedType("java/lang/reflect/Constructor").load();
        constructorClass = ctorDef.getVmClass();
        ctorCtor = ctorDef.requireSingleConstructor(ce -> ce.getDescriptor().getParameterTypes().size() == 8);
        LoadedTypeDefinition mhDef = classContext.findDefinedType("java/lang/invoke/MethodHandle").load();
        methodHandleCheckType = mhDef.requireSingleMethod("checkType");
        methodHandleLambdaFormField = mhDef.findField("form");
        LoadedTypeDefinition mhnDef = classContext.findDefinedType("java/lang/invoke/MethodHandleNatives").load();
        vm.registerInvokable(mhnDef.requireSingleMethod(me -> me.nameEquals("init")), this::methodHandleNativesInit);
        methodHandleNativesResolve = mhnDef.requireSingleMethod(me -> me.nameEquals("resolve"));
        vm.registerInvokable(methodHandleNativesResolve, this::methodHandleNativesResolve);
        vm.registerInvokable(mhnDef.requireSingleMethod(me -> me.nameEquals("objectFieldOffset")), this::methodHandleNativesObjectFieldOffset);
        vm.registerInvokable(mhnDef.requireSingleMethod(me -> me.nameEquals("staticFieldBase")), this::methodHandleNativesStaticFieldBase);
        vm.registerInvokable(mhnDef.requireSingleMethod(me -> me.nameEquals("staticFieldOffset")), this::methodHandleNativesStaticFieldOffset);
        vm.registerInvokable(mhnDef.requireSingleMethod(me -> me.nameEquals("verifyConstants")), (thread, target, args) -> Boolean.TRUE);
        LoadedTypeDefinition lfDef = classContext.findDefinedType("java/lang/invoke/LambdaForm").load();
        lambdaFormMemberNameField = lfDef.findField("vmentry");
        LoadedTypeDefinition nativeCtorAccImplDef = classContext.findDefinedType("jdk/internal/reflect/NativeConstructorAccessorImpl").load();
        vm.registerInvokable(nativeCtorAccImplDef.requireSingleMethod(me -> me.nameEquals("newInstance0")), this::nativeConstructorAccessorImplNewInstance0);
        LoadedTypeDefinition nativeMethodAccImplDef = classContext.findDefinedType("jdk/internal/reflect/NativeMethodAccessorImpl").load();
        vm.registerInvokable(nativeMethodAccImplDef.requireSingleMethod(me -> me.nameEquals("invoke0")), this::nativeMethodAccessorImplInvoke0);
        // MemberName
        LoadedTypeDefinition memberNameDef = classContext.findDefinedType("java/lang/invoke/MemberName").load();
        vm.registerInvokable(memberNameDef.requireSingleMethod(me -> me.nameEquals("vminfoIsConsistent")), (thread, target, args) -> Boolean.TRUE);
        memberNameClazzField = memberNameDef.findField("clazz");
        memberNameNameField = memberNameDef.findField("name");
        memberNameTypeField = memberNameDef.findField("type");
        memberNameFlagsField = memberNameDef.findField("flags");
        memberNameMethodField = memberNameDef.findField("method");
        memberNameIndexField = memberNameDef.findField("index");
        memberNameResolvedField = memberNameDef.findField("resolved");
        memberNameExactDispatcherField = memberNameDef.findField("exactDispatcher");
        MethodDescriptor memberName4Desc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of(
            BaseTypeDescriptor.B,
            classDef.getDescriptor(),
            ClassTypeDescriptor.synthesize(classContext, "java/lang/String"),
            ClassTypeDescriptor.synthesize(classContext, "java/lang/Object")
        ));
        memberNameClass = memberNameDef.getVmClass();
        memberName4Ctor = memberNameDef.resolveConstructorElement(memberName4Desc);
        // MethodHandleNatives
        methodHandleNativesLinkMethod = mhnDef.requireSingleMethod(me -> me.nameEquals("linkMethod"));
        methodHandleNativesFindMethodHandleType = mhnDef.requireSingleMethod(me -> me.nameEquals("findMethodHandleType"));
        // Field
        fieldClazzField = fieldDef.findField("clazz");
        fieldSlotField = fieldDef.findField("slot");
        fieldNameField = fieldDef.findField("name");
        fieldTypeField = fieldDef.findField("type");
        // Method
        methodClazzField = methodDef.findField("clazz");
        methodSlotField = methodDef.findField("slot");
        methodNameField = methodDef.findField("name");
        methodReturnTypeField = methodDef.findField("returnType");
        methodParameterTypesField = methodDef.findField("parameterTypes");
        // Constructor
        ctorClazzField = ctorDef.findField("clazz");
        ctorSlotField = ctorDef.findField("slot");
        ctorParameterTypesField = ctorDef.findField("parameterTypes");

        // ResolvedMethodName
        LoadedTypeDefinition rmnDef = classContext.findDefinedType("java/lang/invoke/ResolvedMethodName").load();
        rmnCtor = rmnDef.getConstructor(0);
        rmnClass = rmnDef.getVmClass();
        rmnIndexField = rmnDef.findField("index");
        rmnClazzField = rmnDef.findField("clazz");

        // MethodHandleAccessorFactory
        LoadedTypeDefinition mhAccFactDef = classContext.findDefinedType("jdk/internal/reflect/MethodHandleAccessorFactory").load();
        newMethodAccessorMethod = mhAccFactDef.requireSingleMethod("newMethodAccessor");
        newConstructorAccessorMethod = mhAccFactDef.requireSingleMethod("newConstructorAccessor");

        // Exceptions & errors
        LoadedTypeDefinition leDef = classContext.findDefinedType("java/lang/LinkageError").load();
        linkageErrorClass = (VmThrowableClass) leDef.getVmClass();
        LoadedTypeDefinition iteDef = classContext.findDefinedType("java/lang/reflect/InvocationTargetException").load();
        invocationTargetExceptionClass = (VmThrowableClass) iteDef.getVmClass();

        // MethodType
        LoadedTypeDefinition mtDef = classContext.findDefinedType("java/lang/invoke/MethodType").load();
        methodTypeRTypeField = mtDef.findField("rtype");
        methodTypePTypesField = mtDef.findField("ptypes");

        // box types
        LoadedTypeDefinition byteDef = classContext.findDefinedType("java/lang/Byte").load();
        byteClass = byteDef.getVmClass();
        byteValueField = byteDef.findField("value");
        LoadedTypeDefinition shortDef = classContext.findDefinedType("java/lang/Short").load();
        shortClass = shortDef.getVmClass();
        shortValueField = shortDef.findField("value");
        LoadedTypeDefinition integerDef = classContext.findDefinedType("java/lang/Integer").load();
        integerClass = integerDef.getVmClass();
        integerValueField = integerDef.findField("value");
        LoadedTypeDefinition longDef = classContext.findDefinedType("java/lang/Long").load();
        longClass = longDef.getVmClass();
        longValueField = longDef.findField("value");
        LoadedTypeDefinition characterDef = classContext.findDefinedType("java/lang/Character").load();
        characterClass = characterDef.getVmClass();
        characterValueField = characterDef.findField("value");
        LoadedTypeDefinition floatDef = classContext.findDefinedType("java/lang/Float").load();
        floatClass = floatDef.getVmClass();
        floatValueField = floatDef.findField("value");
        LoadedTypeDefinition doubleDef = classContext.findDefinedType("java/lang/Double").load();
        doubleClass = doubleDef.getVmClass();
        doubleValueField = doubleDef.findField("value");
        LoadedTypeDefinition booleanDef = classContext.findDefinedType("java/lang/Boolean").load();
        booleanClass = booleanDef.getVmClass();
        booleanValueField = booleanDef.findField("value");
    }

    public static Reflection get(CompilationContext ctxt) {
        Reflection instance = ctxt.getAttachment(KEY);
        if (instance == null) {
            instance = new Reflection(ctxt);
            Reflection appearing = ctxt.putAttachmentIfAbsent(KEY, instance);
            if (appearing != null) {
                instance = appearing;
            }
        }
        return instance;
    }

    public void makeAvailableForRuntimeReflection(LoadedTypeDefinition ltd) {
        ReachabilityRoots.get(ctxt).registerReflectiveClass(ltd);
    }

    /**
     * This method must be called during the ADD phase by an attached thread
     * after the Vm is fully booted
     * @param ce The ConstructorElement to make available for runtime reflective invocation
     */
    public void makeAvailableForRuntimeReflection(ConstructorElement ce) {
        VmClass c = ce.getEnclosingType().load().getVmClass();
        getClassDeclaredConstructors(c, true);
        getClassDeclaredConstructors(c, false);

        // Create a DirectConstructorHandleAccessor and store it in the Constructor.
        VmObject ctor = getConstructor(ce);
        VmObject accessor = (VmObject)vm.invokeExact(newConstructorAccessorMethod, null, List.of(ctor));
        MethodElement sca = constructorClass.getTypeDefinition().requireSingleMethod("setConstructorAccessor", 1);
        vm.invokeExact(sca, ctor, List.of(accessor));
    }

    /**
     * This method must be called during the ADD phase by an attached thread
     * after the Vm is fully booted.
     * @param me The MethodElement to make available for runtime reflective invocation
     */
    public void makeAvailableForRuntimeReflection(MethodElement me) {
        VmClass c = me.getEnclosingType().load().getVmClass();
        getClassDeclaredMethods(c, true);
        getClassDeclaredMethods(c, false);

        // Create a DirectMethodHandleAccessor and store it in the Method.
        VmObject method = getMethod(me);
        VmObject accessor = (VmObject)vm.invokeExact(newMethodAccessorMethod, null, List.of(method, Boolean.FALSE));
        MethodElement sma = methodClass.getTypeDefinition().requireSingleMethod("setMethodAccessor", 1);
        vm.invokeExact(sma, method, List.of(accessor));
    }

    /**
     * This method must be called during the ADD phase by an attached thread
     * after the Vm is fully booted.
     *
     * @param fe The FieldElement to make available for runtime reflective invocation
     */
    public void makeAvailableForRuntimeReflection(FieldElement fe) {
        VmClass c = fe.getEnclosingType().load().getVmClass();
        getClassDeclaredFields(c, true);
        getClassDeclaredFields(c, false);
        VmObject field = getField(fe);
        MethodElement afa = fieldClass.getTypeDefinition().requireSingleMethod("acquireFieldAccessor", 1);
        vm.invokeExact(afa, field, List.of(Boolean.TRUE));
        vm.invokeExact(afa, field, List.of(Boolean.FALSE));
    }

    /**
     * Ensure field accessors are generated for all reflectively accessed fields.
     * We can do this in an ADD postHook because it will not generate any newly reachable methods.
     */
    public void generateFieldAccessors() {
        ReachabilityRoots roots = ReachabilityRoots.get(ctxt);
        for (FieldElement f: roots.getReflectiveFields()) {
            makeAvailableForRuntimeReflection(f);
        }
    }

    /**
     * Transfer the VMReferenceArrays built during the ADD phase to the ReflectionData
     * instances that will be serialized as part of the initial runtime heap.
     */
    public void transferToReflectionData()  {
        // Transfer the reflected objects to the build-time heap by storing them in the VmClass's ReflectionData instances.
        LoadedTypeDefinition rdDef = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Class$ReflectionData").load();
        LayoutInfo rdLayout = Layout.get(ctxt).getInstanceLayoutInfo(rdDef);
        long rdIndex = classClass.indexOf(classClass.getTypeDefinition().findField("qbiccReflectionData"));

        long dfi = rdLayout.getMember(rdDef.findField("declaredFields")).getOffset();
        declaredFields.forEach((c, a) -> {
            c.getMemory().loadRef(rdIndex, SinglePlain).getMemory().storeRef(dfi, a, SinglePlain);
        });
        long dpfi = rdLayout.getMember(rdDef.findField("declaredPublicFields")).getOffset();
        declaredPublicFields.forEach((c, a) -> {
            c.getMemory().loadRef(rdIndex, SinglePlain).getMemory().storeRef(dpfi, a, SinglePlain);
        });
        long dmi = rdLayout.getMember(rdDef.findField("declaredMethods")).getOffset();
        declaredMethods.forEach((c, a) -> {
            c.getMemory().loadRef(rdIndex, SinglePlain).getMemory().storeRef(dmi, a, SinglePlain);
        });
        long dpmi = rdLayout.getMember(rdDef.findField("declaredPublicMethods")).getOffset();
        declaredPublicMethods.forEach((c, a) -> {
            c.getMemory().loadRef(rdIndex, SinglePlain).getMemory().storeRef(dpmi, a, SinglePlain);
        });
        long dci = rdLayout.getMember(rdDef.findField("declaredConstructors")).getOffset();
        declaredConstructors.forEach((c, a) -> {
            c.getMemory().loadRef(rdIndex, SinglePlain).getMemory().storeRef(dci, a, SinglePlain);
        });
        long dpci = rdLayout.getMember(rdDef.findField("publicConstructors")).getOffset();
        declaredPublicConstructors.forEach((c, a) -> {
            c.getMemory().loadRef(rdIndex, SinglePlain).getMemory().storeRef(dpci, a, SinglePlain);
        });
    }

    static MethodDescriptor erase(final ClassContext classContext, final MethodDescriptor descriptor) {
        TypeDescriptor erasedRetType = erase(classContext, descriptor.getReturnType());
        List<TypeDescriptor> erasedTypes = erase(classContext, descriptor.getParameterTypes());
        return MethodDescriptor.synthesize(classContext, erasedRetType, erasedTypes);
    }

    static TypeDescriptor erase(ClassContext classContext, final TypeDescriptor type) {
        if (type instanceof BaseTypeDescriptor btd) {
            return btd;
        } else {
            return ClassTypeDescriptor.synthesize(classContext, "java/lang/Object");
        }
    }

    static List<TypeDescriptor> erase(final ClassContext classContext, final List<TypeDescriptor> types) {
        if (types.isEmpty()) {
            return List.of();
        } else if (types.size() == 1) {
            return List.of(erase(classContext, types.get(0)));
        } else {
            ArrayList<TypeDescriptor> list = new ArrayList<>(types.size());
            for (TypeDescriptor type : types) {
                list.add(erase(classContext, type));
            }
            return list;
        }
    }

    static boolean isErased(MethodDescriptor descriptor) {
        return isErased(descriptor.getReturnType()) && isErased(descriptor.getParameterTypes());
    }

    static boolean isErased(final TypeDescriptor typeDescriptor) {
        return typeDescriptor instanceof BaseTypeDescriptor ||
            typeDescriptor instanceof ClassTypeDescriptor ctd && ctd.packageAndClassNameEquals("java/lang", "Object");
    }

    static boolean isErased(final List<TypeDescriptor> types) {
        for (TypeDescriptor type : types) {
            if (! isErased(type)) {
                return false;
            }
        }
        return true;
    }

    ConstantPool getConstantPoolForClass(VmClass vmClass) {
        return cpObjs.computeIfAbsent(cpMap.computeIfAbsent(vmClass, this::makePoolObj), Reflection::makePool);
    }

    VmArray getAnnotations(AnnotatedElement element) {
        VmArray bytes = annotatedElements.get(element);
        if (bytes != null) {
            return bytes;
        }
        List<Annotation> annotations = element.getVisibleAnnotations();
        if (annotations.isEmpty()) {
            annotatedElements.putIfAbsent(element, noAnnotations);
            return noAnnotations;
        }
        VmClass vmClass = element.getEnclosingType().load().getVmClass();
        ConstantPool constantPool = getConstantPoolForClass(vmClass);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        synchronized (constantPool) {
            int cnt = annotations.size();
            // big-endian
            os.write(cnt >>> 8);
            os.write(cnt);
            for (Annotation annotation : annotations) {
                annotation.deparseTo(os, constantPool);
            }
        }
        VmArray appearing = annotatedElements.putIfAbsent(element, vm.newByteArray(os.toByteArray()));
        if (appearing != null) {
            bytes = appearing;
        }
        return bytes;
    }

    private VmObject makePoolObj(final Object ignored) {
        return vm.newInstance(cpClass, cpCtor, List.of());
    }

    private static ConstantPool makePool(final Object ignored) {
        return new ConstantPool();
    }

    private Object getIntAt0(final VmThread vmThread, final VmObject vmObject, final List<Object> objects) {
        ConstantPool constantPool = cpObjs.get(vmObject);
        assert constantPool != null; // the method would not be reachable otherwise
        synchronized (constantPool) {
            return Integer.valueOf(constantPool.getIntConstant(((Number) objects.get(0)).intValue()));
        }
    }

    private Object getLongAt0(final VmThread vmThread, final VmObject vmObject, final List<Object> objects) {
        ConstantPool constantPool = cpObjs.get(vmObject);
        assert constantPool != null; // the method would not be reachable otherwise
        synchronized (constantPool) {
            return Long.valueOf(constantPool.getLongConstant(((Number) objects.get(0)).intValue()));
        }
    }

    private Object getFloatAt0(final VmThread vmThread, final VmObject vmObject, final List<Object> objects) {
        ConstantPool constantPool = cpObjs.get(vmObject);
        assert constantPool != null; // the method would not be reachable otherwise
        synchronized (constantPool) {
            return Float.valueOf(Float.intBitsToFloat(constantPool.getIntConstant(((Number) objects.get(0)).intValue())));
        }
    }

    private Object getDoubleAt0(final VmThread vmThread, final VmObject vmObject, final List<Object> objects) {
        ConstantPool constantPool = cpObjs.get(vmObject);
        assert constantPool != null; // the method would not be reachable otherwise
        synchronized (constantPool) {
            return Long.valueOf(constantPool.getLongConstant(((Number) objects.get(0)).intValue()));
        }
    }

    private Object getUTF8At0(final VmThread vmThread, final VmObject vmObject, final List<Object> objects) {
        ConstantPool constantPool = cpObjs.get(vmObject);
        assert constantPool != null; // the method would not be reachable otherwise
        synchronized (constantPool) {
            return vm.intern(constantPool.getUtf8Constant(((Number) objects.get(0)).intValue()));
        }
    }

    private Object getClassRawAnnotations(final VmThread vmThread, final VmObject vmObject, final List<Object> objects) {
        VmClass vmClass = (VmClass) vmObject;
        LoadedTypeDefinition def = vmClass.getTypeDefinition();
        VmArray bytes = annotatedTypes.get(def);
        if (bytes != null) {
            return bytes;
        }
        List<Annotation> annotations = def.getVisibleAnnotations();
        if (annotations.isEmpty()) {
            annotatedTypes.putIfAbsent(def, noAnnotations);
            return noAnnotations;
        }
        ConstantPool constantPool = getConstantPoolForClass(vmClass);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        synchronized (constantPool) {
            for (Annotation annotation : annotations) {
                annotation.deparseTo(os, constantPool);
            }
        }
        VmArray appearing = annotatedTypes.putIfAbsent(def, vm.newByteArray(os.toByteArray()));
        if (appearing != null) {
            bytes = appearing;
        }
        return bytes;
    }

    private Object getClassDeclaredFields0(final VmThread vmThread, final VmObject vmObject, final List<Object> objects) {
        return getClassDeclaredFields((VmClass) vmObject, ((Boolean) objects.get(0)).booleanValue());
    }

    private VmObject getField(FieldElement field) {
        VmObject vmObject = reflectionObjects.get(field);
        if (vmObject != null) {
            return vmObject;
        }
        // field might get mutated; note it here so we don't cache it unduly
        if (! field.isStatic()) {
            field.setModifierFlags(ClassFile.I_ACC_NOT_REALLY_FINAL);
        }
        VmClass declaringClass = field.getEnclosingType().load().getVmClass();
        vmObject = vm.newInstance(fieldClass, fieldCtor, Arrays.asList(
            declaringClass,
            vm.intern(field.getName()),
            vm.getClassForDescriptor(declaringClass.getClassLoader(), field.getTypeDescriptor()),
            Integer.valueOf(field.getModifiers() & 0x1fff),
            Boolean.FALSE,
            Integer.valueOf(field.getIndex()),
            vm.intern(field.getTypeSignature().toString()),
            getAnnotations(field)
        ));
        VmObject appearing = reflectionObjects.putIfAbsent(field, vmObject);
        return appearing != null ? appearing : vmObject;
    }

    private VmReferenceArray getClassDeclaredFields(final VmClass vmClass, final boolean publicOnly) {
        VmReferenceArray result;
        Map<VmClass, VmReferenceArray> map = publicOnly ? this.declaredPublicFields : this.declaredFields;
        result = map.get(vmClass);
        if (result != null) {
            return result;
        }
        int total = 0;
        LoadedTypeDefinition def = vmClass.getTypeDefinition();
        for (int i = 0; i < def.getFieldCount(); i++) {
            if (def.hasAllModifiersOf(ClassFile.I_ACC_NO_REFLECT)) {
                continue;
            }
            if (! publicOnly || def.getField(i).isPublic()) {
                total += 1;
            }
        }
        VmReferenceArray pubFields = vm.newArrayOf(fieldClass, total);
        int pubIdx = 0;
        for (int i = 0; i < def.getFieldCount(); i++) {
            if (def.hasAllModifiersOf(ClassFile.I_ACC_NO_REFLECT)) {
                continue;
            }
            FieldElement field = def.getField(i);
            if (! publicOnly || field.isPublic()) {
                pubFields.getArray()[pubIdx++] = getField(field);
            }
        }
        VmReferenceArray appearing = map.putIfAbsent(vmClass, pubFields);
        return appearing != null ? appearing : pubFields;
    }

    private Object getClassDeclaredMethods0(final VmThread vmThread, final VmObject vmObject, final List<Object> objects) {
        return getClassDeclaredMethods((VmClass) vmObject, ((Boolean) objects.get(0)).booleanValue());
    }

    private VmObject getMethod(MethodElement method) {
        VmObject vmObject = reflectionObjects.get(method);
        if (vmObject != null) {
            return vmObject;
        }
        VmClass declaringClass = method.getEnclosingType().load().getVmClass();
        VmClassLoader classLoader = declaringClass.getClassLoader();
        MethodDescriptor desc = method.getDescriptor();
        List<TypeDescriptor> paramTypes = desc.getParameterTypes();
        VmReferenceArray paramTypesVal = vm.newArrayOf(classClass, paramTypes.size());
        VmObject[] paramTypesValArray = paramTypesVal.getArray();
        for (int j = 0; j < paramTypes.size(); j ++) {
            paramTypesValArray[j] = vm.getClassForDescriptor(classLoader, paramTypes.get(j));
        }
        // annotation default
        VmArray dv;
        AnnotationValue defaultValue = method.getDefaultValue();
        if (defaultValue == null) {
            dv = noAnnotations;
        } else {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ConstantPool cp = getConstantPoolForClass(declaringClass);
            synchronized (cp) {
                defaultValue.deparseValueTo(os, cp);
            }
            dv = vm.newByteArray(os.toByteArray());
        }
        vmObject = vm.newInstance(methodClass, methodCtor, Arrays.asList(
            declaringClass,
            vm.intern(method.getName()),
            paramTypesVal,
            vm.getClassForDescriptor(classLoader, method.getDescriptor().getReturnType()),
            // TODO: checked exceptions
            vm.newArrayOf(classClass, 0),
            Integer.valueOf(method.getModifiers() & 0x1fff),
            Integer.valueOf(method.getIndex()),
            vm.intern(method.getSignature().toString()),
            getAnnotations(method),
            // TODO: param annotations
            noAnnotations,
            dv
        ));
        VmObject appearing = reflectionObjects.putIfAbsent(method, vmObject);
        return appearing != null ? appearing : vmObject;
    }

    private VmReferenceArray getClassDeclaredMethods(final VmClass vmClass, final boolean publicOnly) {
        VmReferenceArray result;
        Map<VmClass, VmReferenceArray> map = publicOnly ? this.declaredPublicMethods : this.declaredMethods;
        result = map.get(vmClass);
        if (result != null) {
            return result;
        }
        int total = 0;
        LoadedTypeDefinition def = vmClass.getTypeDefinition();
        for (int i = 0; i < def.getMethodCount(); i++) {
            if (def.hasAllModifiersOf(ClassFile.I_ACC_NO_REFLECT)) {
                continue;
            }
            if (! publicOnly || def.getMethod(i).isPublic()) {
                total += 1;
            }
        }
        VmReferenceArray pubMethods = vm.newArrayOf(methodClass, total);
        int pubIdx = 0;
        VmObject[] pubMethodsArray = pubMethods.getArray();
        for (int i = 0; i < def.getMethodCount(); i++) {
            if (def.hasAllModifiersOf(ClassFile.I_ACC_NO_REFLECT)) {
                continue;
            }
            MethodElement method = def.getMethod(i);
            if (! publicOnly || method.isPublic()) {
                pubMethodsArray[pubIdx++] = getMethod(method);
            }
        }
        VmReferenceArray appearing = map.putIfAbsent(vmClass, pubMethods);
        return appearing != null ? appearing : pubMethods;
    }

    private Object getClassDeclaredConstructors0(final VmThread vmThread, final VmObject vmObject, final List<Object> objects) {
        return getClassDeclaredConstructors((VmClass) vmObject, ((Boolean) objects.get(0)).booleanValue());
    }

    private VmObject getConstructor(ConstructorElement constructor) {
        VmObject vmObject = reflectionObjects.get(constructor);
        if (vmObject != null) {
            return vmObject;
        }
        VmClass declaringClass = constructor.getEnclosingType().load().getVmClass();
        VmClassLoader classLoader = declaringClass.getClassLoader();
        MethodDescriptor desc = constructor.getDescriptor();
        List<TypeDescriptor> paramTypes = desc.getParameterTypes();
        VmReferenceArray paramTypesVal = vm.newArrayOf(classClass, paramTypes.size());
        VmObject[] paramTypesValArray = paramTypesVal.getArray();
        for (int j = 0; j < paramTypes.size(); j ++) {
            paramTypesValArray[j] = vm.getClassForDescriptor(classLoader, paramTypes.get(j));
        }
        vmObject = vm.newInstance(constructorClass, ctorCtor, Arrays.asList(
            declaringClass,
            paramTypesVal,
            // TODO: checked exceptions
            vm.newArrayOf(classClass, 0),
            Integer.valueOf(constructor.getModifiers() & 0x1fff),
            Integer.valueOf(constructor.getIndex()),
            vm.intern(constructor.getSignature().toString()),
            getAnnotations(constructor),
            // TODO: param annotations
            noAnnotations
        ));
        VmObject appearing = reflectionObjects.putIfAbsent(constructor, vmObject);
        return appearing != null ? appearing : vmObject;
    }

    private VmReferenceArray getClassDeclaredConstructors(final VmClass vmClass, final boolean publicOnly) {
        VmReferenceArray result;
        Map<VmClass, VmReferenceArray> map = publicOnly ? this.declaredPublicConstructors : this.declaredConstructors;
        result = map.get(vmClass);
        if (result != null) {
            return result;
        }
        int total = 0;
        LoadedTypeDefinition def = vmClass.getTypeDefinition();
        for (int i = 0; i < def.getConstructorCount(); i++) {
            if (def.hasAllModifiersOf(ClassFile.I_ACC_NO_REFLECT)) {
                continue;
            }
            if (! publicOnly || def.getConstructor(i).isPublic()) {
                total += 1;
            }
        }
        VmReferenceArray pubConstructors = vm.newArrayOf(constructorClass, total);
        int pubIdx = 0;
        VmObject[] pubConstructorsArray = pubConstructors.getArray();
        for (int i = 0; i < def.getConstructorCount(); i++) {
            if (def.hasAllModifiersOf(ClassFile.I_ACC_NO_REFLECT)) {
                continue;
            }
            ConstructorElement constructor = def.getConstructor(i);
            if (! publicOnly || constructor.isPublic()) {
                pubConstructorsArray[pubIdx++] = getConstructor(constructor);
            }
        }
        VmReferenceArray appearing = map.putIfAbsent(vmClass, pubConstructors);
        return appearing != null ? appearing : pubConstructors;
    }

    private Object methodHandleNativesInit(final VmThread thread, final VmObject ignored, final List<Object> args) {
        VmObject self = (VmObject) args.get(0);
        VmObject target = (VmObject) args.get(1);
        VmClass targetClass = target.getVmClass();
        // target may be a reflection method, field, or constructor
        LoadedTypeDefinition targetClassDef = targetClass.getTypeDefinition();
        String targetClassIntName = targetClassDef.getInternalName();
        if (targetClassIntName.equals("java/lang/reflect/Field")) {
            initMethodHandleField(self, target, false);
            return null;
        }
        if (targetClassIntName.equals("java/lang/reflect/Method")) {
            initMethodHandleMethod(self, target);
            return null;
        }
        if (targetClassIntName.equals("java/lang/reflect/Constructor")) {
            initMethodHandleCtor(self, target);
            return null;
        }
        throw new IllegalStateException("Unknown reflection object kind");
    }

    private void initMethodHandleMethod(final VmObject memberName, final VmObject methodTarget) {
        // First read values from the Method object

        // get the method's enclosing class
        VmClass clazzVal = (VmClass) methodTarget.getMemory().loadRef(methodTarget.indexOf(methodClazzField), SinglePlain);
        // get the method index
        int index = methodTarget.getMemory().load32(methodTarget.indexOf(methodSlotField), SinglePlain);

        // find the referenced method
        MethodElement refMethod = clazzVal.getTypeDefinition().getMethod(index);
        int flags = refMethod.getModifiers() & 0x1fff;

        flags |= IS_METHOD;
        MethodHandleKind kind;
        if (refMethod.isStatic()) {
            flags |= KIND_INVOKE_STATIC << KIND_SHIFT;
            kind = MethodHandleKind.INVOKE_STATIC;
        } else {
            flags |= KIND_INVOKE_SPECIAL << KIND_SHIFT;
            kind = MethodHandleKind.INVOKE_SPECIAL;
        }
        if (refMethod.hasAllModifiersOf(ClassFile.I_ACC_CALLER_SENSITIVE)) {
            flags |= CALLER_SENSITIVE;
        }

        // Now initialize the corresponding MemberName fields

        // Create a ResolvedMethodName for the resolved method

        VmObject rmn = vm.newInstance(rmnClass, rmnCtor, List.of());

        rmn.getMemory().storeRef(rmn.indexOf(rmnClazzField), refMethod.getEnclosingType().load().getVmClass(), SinglePlain);
        rmn.getMemory().store32(rmn.indexOf(rmnIndexField), refMethod.getIndex(), SinglePlain);

        // set the member name flags
        memberName.getMemory().store32(memberName.indexOf(memberNameFlagsField), flags, SinglePlain);
        // set the member name's field holder
        memberName.getMemory().storeRef(memberName.indexOf(memberNameClazzField), clazzVal, SinglePlain);
        // set the member name's method
        memberName.getMemory().storeRef(memberName.indexOf(memberNameMethodField), rmn, SinglePlain);
        // set the member name index
        memberName.getMemory().store32(memberName.indexOf(memberNameIndexField), refMethod.getIndex(), SinglePlain);
        // set the exactDispatcher (corresponds to OpenJDK setting vmtarget)
        generateDispatcher(memberName, refMethod, kind);
        // all done
        return;
    }

    private void initMethodHandleCtor(final VmObject memberName, final VmObject methodTarget) {
        // First read values from the Constructor object

        // get the ctor's enclosing class
        VmClass clazzVal = (VmClass) methodTarget.getMemory().loadRef(methodTarget.indexOf(ctorClazzField), SinglePlain);
        // get the ctor index
        int index = methodTarget.getMemory().load32(methodTarget.indexOf(ctorSlotField), SinglePlain);

        // find the referenced ctor
        ConstructorElement refCtor = clazzVal.getTypeDefinition().getConstructor(index);
        int flags = refCtor.getModifiers() & 0x1fff;

        flags |= IS_CONSTRUCTOR | KIND_NEW_INVOKE_SPECIAL << KIND_SHIFT;

        // Now initialize the corresponding MemberName fields

        // Create a ResolvedMethodName for the ctor
        VmObject rmn = vm.newInstance(rmnClass, rmnCtor, List.of());

        rmn.getMemory().storeRef(rmn.indexOf(rmnClazzField), refCtor.getEnclosingType().load().getVmClass(), SinglePlain);
        rmn.getMemory().store32(rmn.indexOf(rmnIndexField), refCtor.getIndex(), SinglePlain);

        // set the member name flags
        memberName.getMemory().store32(memberName.indexOf(memberNameFlagsField), flags, SinglePlain);
        // set the member name's field holder
        memberName.getMemory().storeRef(memberName.indexOf(memberNameClazzField), clazzVal, SinglePlain);
        // set the member name's "method"
        memberName.getMemory().storeRef(memberName.indexOf(memberNameMethodField), rmn, SinglePlain);
        // set the member name index
        memberName.getMemory().store32(memberName.indexOf(memberNameIndexField), refCtor.getIndex(), SinglePlain);
        // set the exactDispatcher (corresponds to OpenJDK setting vmtarget)
        generateDispatcher(memberName, refCtor, MethodHandleKind.NEW_INVOKE_SPECIAL);
        // all done
        return;
    }

    private void initMethodHandleField(final VmObject memberName, final VmObject fieldTarget, boolean isSetter) {
        // First read values from the Field object

        // get the field's enclosing class
        VmClass clazzVal = (VmClass) fieldTarget.getMemory().loadRef(fieldTarget.indexOf(fieldClazzField), SinglePlain);
        // get the field index
        int index = fieldTarget.getMemory().load32(fieldTarget.indexOf(fieldSlotField), SinglePlain);
        // get the field name
        VmString nameVal = (VmString) fieldTarget.getMemory().loadRef(fieldTarget.indexOf(fieldNameField), SinglePlain);
        // get the field type class
        VmClass fieldClazzVal = (VmClass) fieldTarget.getMemory().loadRef(fieldTarget.indexOf(fieldTypeField), SinglePlain);

        // find the referenced field
        FieldElement refField = clazzVal.getTypeDefinition().getField(index);
        int flags = refField.getModifiers() & 0x1fff;

        // set up flags
        flags |= IS_FIELD;
        MethodHandleKind kind;
        if (refField.isStatic()) {
            if (isSetter) {
                flags |= KIND_PUT_STATIC << KIND_SHIFT;
                kind = MethodHandleKind.PUT_STATIC;
            } else {
                flags |= KIND_GET_STATIC << KIND_SHIFT;
                kind = MethodHandleKind.GET_STATIC;
            }
        } else {
            if (isSetter) {
                flags |= KIND_PUT_FIELD << KIND_SHIFT;
                kind = MethodHandleKind.PUT_FIELD;
            } else {
                flags |= KIND_GET_FIELD << KIND_SHIFT;
                kind = MethodHandleKind.GET_FIELD;
            }
        }
        if (refField.isReallyFinal()) {
            flags |= TRUSTED_FINAL;
        }

        // Now initialize the corresponding MemberName fields

        // set the member name flags
        memberName.getMemory().store32(memberName.indexOf(memberNameFlagsField), flags, SinglePlain);
        // set the member name's field holder
        memberName.getMemory().storeRef(memberName.indexOf(memberNameClazzField), clazzVal, SinglePlain);
        // set the member name's name
        memberName.getMemory().storeRef(memberName.indexOf(memberNameNameField), nameVal, SinglePlain);
        // set the member name's type to the field type
        memberName.getMemory().storeRef(memberName.indexOf(memberNameTypeField), fieldClazzVal, SinglePlain);
        // set the member name index
        memberName.getMemory().store32(memberName.indexOf(memberNameIndexField), refField.getIndex(), SinglePlain);
        // set the exactDispatcher (corresponds to OpenJDK setting vmtarget)
        generateDispatcher(memberName, refField, kind);
        // all done
        return;
    }

    Object methodHandleNativesResolve(final VmThread thread, final VmObject ignored, final List<Object> args) {
        VmObject memberName = (VmObject) args.get(0);
        boolean resolvedFlag = (memberName.getMemory().load8(memberName.indexOf(memberNameResolvedField), SinglePlain) & 1) != 0;
        if (resolvedFlag) {
            return memberName;
        }
        VmClass caller = (VmClass) args.get(1);
        boolean speculativeResolve = args.get(3) instanceof Boolean bv ? bv.booleanValue() : (((Number) args.get(3)).intValue() & 1) != 0;
        VmClass clazz = (VmClass) memberName.getMemory().loadRef(memberName.indexOf(memberNameClazzField), SinglePlain);
        VmString name = (VmString) memberName.getMemory().loadRef(memberName.indexOf(memberNameNameField), SinglePlain);
        VmObject type = memberName.getMemory().loadRef(memberName.indexOf(memberNameTypeField), SinglePlain);
        int flags = memberName.getMemory().load32(memberName.indexOf(memberNameFlagsField), SinglePlain);
        int kind = (flags >> KIND_SHIFT) & KIND_MASK;
        if (clazz == null || name == null || type == null) {
            throw new Thrown(linkageErrorClass.newInstance("Null name or class"));
        }
        LoadedTypeDefinition typeDefinition = clazz.getTypeDefinition();
        ClassContext classContext = typeDefinition.getContext();
        // determine what kind of thing we're resolving
        if ((flags & IS_FIELD) != 0) {
            // find a field with the given name
            FieldElement resolved = typeDefinition.resolveField(((VmClass)type).getDescriptor(), name.getContent());
            if (resolved == null && ! speculativeResolve) {
                throw new Thrown(linkageErrorClass.newInstance("No such field: " + clazz.getName() + "#" + name.getContent()));
            }
            if (resolved == null) {
                return null;
            }
            memberName.getMemory().storeRef(memberName.indexOf(memberNameClazzField), resolved.getEnclosingType().load().getVmClass(), SinglePlain);
            int newFlags = resolved.getModifiers() & 0xffff | IS_FIELD;
            boolean isSetter = kind == KIND_PUT_STATIC || kind == KIND_PUT_FIELD;
            if (resolved.isStatic()) {
                if (isSetter) {
                    kind = KIND_PUT_STATIC;
                } else {
                    kind = KIND_GET_STATIC;
                }
            } else {
                if (isSetter) {
                    kind = KIND_PUT_FIELD;
                } else {
                    kind = KIND_GET_FIELD;
                }
            }
            newFlags |= kind << KIND_SHIFT;
            if (resolved.isReallyFinal()) {
                newFlags |= TRUSTED_FINAL;
            }
            memberName.getMemory().store32(memberName.indexOf(memberNameFlagsField), newFlags, SinglePlain);
            memberName.getMemory().store8(memberName.indexOf(memberNameResolvedField), 1, SinglePlain);
            memberName.getMemory().storeRef(memberName.indexOf(memberNameClazzField), resolved.getEnclosingType().load().getVmClass(), SinglePlain);
            memberName.getMemory().storeRef(memberName.indexOf(memberNameTypeField), type, SinglePlain);
            memberName.getMemory().store32(memberName.indexOf(memberNameIndexField), resolved.getIndex(), GlobalRelease);
            generateDispatcher(memberName, resolved, MethodHandleKind.forId(kind));
            return memberName;
        } else if ((flags & IS_TYPE) != 0) {
            throw new Thrown(linkageErrorClass.newInstance("Not sure what to do for resolving a type"));
        } else {
            // some kind of exec element
            MethodDescriptor desc = createFromMethodType(classContext, type);
            if (((flags & IS_CONSTRUCTOR) != 0)) {
                int idx = typeDefinition.findConstructorIndex(desc);
                if (idx == -1) {
                    if (! speculativeResolve) {
                        throw new Thrown(linkageErrorClass.newInstance("No such constructor: " + name.getContent() + ":" + desc.toString()));
                    }
                    return null;
                }
                if (kind != KIND_NEW_INVOKE_SPECIAL) {
                    throw new Thrown(linkageErrorClass.newInstance("Unknown handle kind"));
                }
                ConstructorElement resolved = typeDefinition.getConstructor(idx);
                memberName.getMemory().storeRef(memberName.indexOf(memberNameClazzField), resolved.getEnclosingType().load().getVmClass(), SinglePlain);
                int newFlags = resolved.getModifiers() & 0xffff | IS_CONSTRUCTOR | (kind << 24);
                memberName.getMemory().store32(memberName.indexOf(memberNameFlagsField), newFlags, SinglePlain);
                memberName.getMemory().store8(memberName.indexOf(memberNameResolvedField), 1, SinglePlain);
                memberName.getMemory().store32(memberName.indexOf(memberNameIndexField), resolved.getIndex(), GlobalRelease);
                // generate a dispatcher
                generateDispatcher(memberName, resolved, MethodHandleKind.forId(kind));
                return memberName;
            } else if (((flags & IS_METHOD) != 0)){
                // resolve
                MethodElement resolved;
                // todo: consider visibility, caller
                if (kind == KIND_INVOKE_STATIC) {
                    // use virtual algorithm to find static
                    resolved = typeDefinition.isInterface() ? typeDefinition.resolveMethodElementInterface(name.getContent(), desc) : typeDefinition.resolveMethodElementVirtual(name.getContent(), desc);
                    // todo: ICCE check...
                } else if (kind == KIND_INVOKE_INTERFACE) {
                    // interface also uses virtual resolution - against the target class
                    resolved = typeDefinition.isInterface() ? typeDefinition.resolveMethodElementInterface(name.getContent(), desc) : typeDefinition.resolveMethodElementVirtual(name.getContent(), desc);
                    // todo: ICCE check...
                } else if (kind == KIND_INVOKE_SPECIAL) {
                    resolved = typeDefinition.resolveMethodElementExact(name.getContent(), desc);
                    // todo: ICCE check...
                } else if (kind == KIND_INVOKE_VIRTUAL) {
                    resolved = typeDefinition.resolveMethodElementVirtual(name.getContent(), desc);
                    // todo: ICCE check...
                } else {
                    throw new Thrown(linkageErrorClass.newInstance("Unknown handle kind"));
                }
                if (resolved == null) {
                    if (! speculativeResolve) {
                        throw new Thrown(linkageErrorClass.newInstance("No such method: " + clazz.getName() + "#" + name.getContent() + ":" + desc.toString()));
                    }
                    return null;
                }
                memberName.getMemory().storeRef(memberName.indexOf(memberNameClazzField), resolved.getEnclosingType().load().getVmClass(), SinglePlain);
                int newFlags = resolved.getModifiers() & 0xffff | IS_METHOD | (kind << 24);
                memberName.getMemory().store32(memberName.indexOf(memberNameFlagsField), newFlags, SinglePlain);
                memberName.getMemory().store8(memberName.indexOf(memberNameResolvedField), 1, SinglePlain);
                memberName.getMemory().store32(memberName.indexOf(memberNameIndexField), resolved.getIndex(), GlobalRelease);
                generateDispatcher(memberName, resolved, MethodHandleKind.forId(kind));
                return memberName;
            } else {
                throw new Thrown(linkageErrorClass.newInstance("Unknown resolution request"));
            }
        }
    }

    private void generateDispatcher(final VmObject memberName, final Element element, final MethodHandleKind kind) {
        Pointer result = dispatcherCache.computeIfAbsent(element, Reflection::newMap).computeIfAbsent(kind, methodHandleKind ->
            switch (methodHandleKind) {
                case GET_FIELD, GET_STATIC ->
                    generateGetterDispatcher((FieldElement) element, kind);
                case PUT_FIELD, PUT_STATIC ->
                    generateSetterDispatcher((FieldElement) element, kind);
                case NEW_INVOKE_SPECIAL ->
                    generateNewInstanceDispatcher((ConstructorElement) element);
                case INVOKE_VIRTUAL, INVOKE_STATIC, INVOKE_SPECIAL, INVOKE_INTERFACE ->
                    generateInvokerDispatcher((MethodElement) element, kind);
            }
        );
        memberName.getMemory().storePointer(memberName.indexOf(memberNameExactDispatcherField), result, SinglePlain);
    }

    private static <K, V> ConcurrentHashMap<K, V> newMap(final Object ignored) {
        return new ConcurrentHashMap<>();
    }

    private Pointer generateGetterDispatcher(final FieldElement element, final MethodHandleKind kind) {
        DefinedTypeDefinition enclosingType = element.getEnclosingType();
        ClassContext classContext = enclosingType.getContext();
        MethodDescriptor desc;
        List<ParameterElement> dispatchParams;
        if (element.isStatic()) {
            desc = MethodDescriptor.synthesize(classContext, element.getTypeDescriptor(), List.of());
            dispatchParams = List.of();
        } else {
            desc = MethodDescriptor.synthesize(classContext, element.getTypeDescriptor(), List.of(enclosingType.getDescriptor()));
            ParameterElement.Builder pb = ParameterElement.builder("instance", enclosingType.getDescriptor(), 0);
            pb.setEnclosingType(enclosingType);
            pb.setTypeParameterContext(enclosingType);
            pb.setSignature(TypeSignature.synthesize(classContext, pb.getDescriptor()));
            dispatchParams = List.of(pb.build());
        }

        MethodElement.Builder builder = MethodElement.builder("dispatch_get_" + element.getName(), desc, 0);
        builder.setModifiers(ClassFile.ACC_PRIVATE | ClassFile.ACC_STATIC | ClassFile.I_ACC_NO_RESOLVE | ClassFile.I_ACC_NO_REFLECT | ClassFile.I_ACC_HIDDEN);
        builder.setEnclosingType(enclosingType);
        builder.setParameters(dispatchParams);
        builder.setSignature(MethodSignature.synthesize(classContext, desc));
        builder.setMethodBodyFactory(new MethodBodyFactory() {
            @Override
            public MethodBody createMethodBody(int index, ExecutableElement e) {
                BasicBlockBuilder bbb = classContext.newBasicBlockBuilder(e);
                List<ParameterValue> paramValues;
                if (element.isStatic()) {
                    paramValues = List.of();
                } else {
                    paramValues = List.of(bbb.parameter(e.getEnclosingType().load().getObjectType().getReference(), "p", 0));
                }
                bbb.startMethod(paramValues);
                // build the entry block
                BlockLabel entryLabel = new BlockLabel();
                bbb.begin(entryLabel);
                Value value;
                if (kind == MethodHandleKind.GET_FIELD) {
                    // instance field; dispatcher arg 0 is the instance
                    Value instance = paramValues.get(0);
                    // load the field value
                    ReadAccessMode mode = element.isVolatile() ? GlobalSeqCst : SinglePlain;
                    value = bbb.load(bbb.instanceFieldOf(bbb.referenceHandle(instance), element), mode);
                } else {
                    assert kind == MethodHandleKind.GET_STATIC;
                    // no instance, no structure
                    // load the field value
                    ReadAccessMode mode = element.isVolatile() ? GlobalSeqCst : SinglePlain;
                    value = bbb.load(bbb.staticField(element), mode);
                }
                bbb.return_(value);
                bbb.finish();
                BasicBlock entryBlock = BlockLabel.getTargetOf(entryLabel);
                Schedule schedule = Schedule.forMethod(entryBlock);
                return MethodBody.of(entryBlock, schedule, null, paramValues);
            }
        }, 0);
        StaticMethodElement dispatcher = (StaticMethodElement) builder.build();
        ctxt.enqueue(dispatcher);
        return StaticMethodPointer.of(dispatcher);
    }

    private Pointer generateSetterDispatcher(final FieldElement element, final MethodHandleKind kind) {
        DefinedTypeDefinition enclosingType = element.getEnclosingType();
        ClassContext classContext = enclosingType.getContext();
        MethodDescriptor desc;
        List<ParameterElement> dispatchParams;
        if (element.isStatic()) {
            desc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of(element.getTypeDescriptor()));
            ParameterElement.Builder pb = ParameterElement.builder("value", element.getTypeDescriptor(), 0);
            pb.setEnclosingType(enclosingType);
            pb.setTypeParameterContext(enclosingType);
            pb.setSignature(TypeSignature.synthesize(classContext, pb.getDescriptor()));
            dispatchParams = List.of(pb.build());
        } else {
            desc = MethodDescriptor.synthesize(classContext, element.getTypeDescriptor(), List.of(enclosingType.getDescriptor()));
            ParameterElement.Builder pb = ParameterElement.builder("instance", enclosingType.getDescriptor(), 0);
            pb.setEnclosingType(enclosingType);
            pb.setTypeParameterContext(enclosingType);
            pb.setSignature(TypeSignature.synthesize(classContext, pb.getDescriptor()));
            ParameterElement instanceParam = pb.build();
            pb = ParameterElement.builder("value", element.getTypeDescriptor(), 1);
            pb.setEnclosingType(enclosingType);
            pb.setTypeParameterContext(enclosingType);
            pb.setSignature(TypeSignature.synthesize(classContext, pb.getDescriptor()));
            dispatchParams = List.of(instanceParam, pb.build());
        }

        MethodElement.Builder builder = MethodElement.builder("dispatch_put_" + element.getName(), desc, 0);
        builder.setModifiers(ClassFile.ACC_PRIVATE | ClassFile.ACC_STATIC | ClassFile.I_ACC_NO_RESOLVE | ClassFile.I_ACC_NO_REFLECT | ClassFile.I_ACC_HIDDEN);
        builder.setEnclosingType(enclosingType);
        builder.setParameters(dispatchParams);
        builder.setSignature(MethodSignature.synthesize(classContext, desc));
        builder.setMethodBodyFactory(new MethodBodyFactory() {
            @Override
            public MethodBody createMethodBody(int index, ExecutableElement e) {
                BasicBlockBuilder bbb = classContext.newBasicBlockBuilder(e);
                List<ParameterValue> paramValues;
                if (element.isStatic()) {
                    paramValues = List.of(bbb.parameter(element.getType(), "p", 0));
                } else {
                    paramValues = List.of(bbb.parameter(element.getEnclosingType().load().getObjectType().getReference(), "p", 0), bbb.parameter(element.getType(), "p", 1));
                }
                bbb.startMethod(paramValues);
                // build the entry block
                BlockLabel entryLabel = new BlockLabel();
                bbb.begin(entryLabel);
                if (kind == MethodHandleKind.PUT_FIELD) {
                    Value instance = paramValues.get(0);
                    Value value = paramValues.get(1);
                    WriteAccessMode mode = element.isVolatile() ? GlobalSeqCst : SinglePlain;
                    bbb.store(bbb.instanceFieldOf(bbb.referenceHandle(instance), element), value, mode);
                } else {
                    assert kind == MethodHandleKind.PUT_STATIC;
                    Value value = paramValues.get(0);
                    WriteAccessMode mode = element.isVolatile() ? GlobalSeqCst : SinglePlain;
                    bbb.store(bbb.staticField(element), value, mode);
                }
                bbb.return_();
                bbb.finish();
                BasicBlock entryBlock = BlockLabel.getTargetOf(entryLabel);
                Schedule schedule = Schedule.forMethod(entryBlock);
                return MethodBody.of(entryBlock, schedule, null, paramValues);
            }
        }, 0);
        StaticMethodElement dispatcher = (StaticMethodElement) builder.build();
        ctxt.enqueue(dispatcher);
        return StaticMethodPointer.of(dispatcher);
    }

    private Pointer generateInvokerDispatcher(final MethodElement element, final MethodHandleKind kind) {
        if (element instanceof StaticMethodElement sme) {
            ctxt.enqueue(element);
            // just dispatch directly to the method itself - nice!
            return StaticMethodPointer.of(sme);
        }
        // generate a static dispatch helper method
        DefinedTypeDefinition enclosingType = element.getEnclosingType();
        TypeDescriptor enclosingDesc = enclosingType.getDescriptor();
        ClassContext classContext = enclosingType.getContext();
        MethodDescriptor origDesc = element.getDescriptor();
        List<TypeDescriptor> origParamTypes = origDesc.getParameterTypes();
        List<TypeDescriptor> newParamTypes = new ArrayList<>(origParamTypes.size() + 1);
        newParamTypes.add(enclosingDesc);
        newParamTypes.addAll(origParamTypes);
        MethodDescriptor newDesc = MethodDescriptor.synthesize(classContext, origDesc.getReturnType(), newParamTypes);
        ParameterElement.Builder pb;
        List<ParameterElement> origParams = element.getParameters();
        List<ParameterElement> dispatchParams = new ArrayList<>(origParams.size() + 1);
        // add a first parameter for the receiver
        pb = ParameterElement.builder("this", enclosingDesc, 0);
        pb.setSignature(TypeSignature.synthesize(classContext, enclosingDesc));
        pb.setEnclosingType(enclosingType);
        pb.setTypeParameterContext(enclosingType);
        dispatchParams.add(pb.build());
        // add each callee parameter
        int origParamCnt = origParamTypes.size();
        for (int i = 0; i < origParamCnt; i ++) {
            pb = ParameterElement.builder(origParams.get(i).getName(), origParamTypes.get(i), i + 1);
            pb.setSignature(TypeSignature.synthesize(classContext, origParamTypes.get(i)));
            pb.setEnclosingType(enclosingType);
            pb.setTypeParameterContext(enclosingType);
            dispatchParams.add(pb.build());
        }
        // optimize a few special cases
        final MethodHandleKind resolvedKind;
        if (kind != MethodHandleKind.INVOKE_SPECIAL && (element.isPrivate() || element.isFinal() || enclosingType.isFinal())) {
            // devirtualize
            resolvedKind = MethodHandleKind.INVOKE_SPECIAL;
        } else {
            resolvedKind = kind;
        }
        String kindStr = switch (resolvedKind) {
            case INVOKE_VIRTUAL -> "virtual_";
            case INVOKE_SPECIAL -> "special_";
            case INVOKE_INTERFACE -> "interface_";
            default -> throw new IllegalStateException();
        };
        MethodElement.Builder builder = MethodElement.builder("dispatch_" + kindStr + element.getName(), newDesc, 0);
        builder.setModifiers(ClassFile.ACC_PRIVATE | ClassFile.ACC_STATIC | ClassFile.I_ACC_NO_RESOLVE | ClassFile.I_ACC_NO_REFLECT | ClassFile.I_ACC_HIDDEN);
        builder.setEnclosingType(enclosingType);
        builder.setParameters(dispatchParams);
        builder.setSignature(MethodSignature.synthesize(classContext, newDesc));
        builder.setMethodBodyFactory(new MethodBodyFactory() {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            @Override
            public MethodBody createMethodBody(int index, ExecutableElement e) {
                BasicBlockBuilder bbb = classContext.newBasicBlockBuilder(e);
                InvokableType type = e.getType();
                int pcnt = type.getParameterCount();
                List<ParameterValue> paramValues = new ArrayList<>(pcnt);
                for (int i = 0; i < pcnt; i ++) {
                    paramValues.add(bbb.parameter(type.getParameterType(i), "p", i));
                }
                bbb.startMethod(paramValues);
                // build the entry block
                BlockLabel entryLabel = new BlockLabel();
                bbb.begin(entryLabel);
                List<Value> values = (List) paramValues.subList(1, paramValues.size());
                switch (resolvedKind) {
                    case INVOKE_VIRTUAL -> bbb.tailCall(bbb.virtualMethodOf(paramValues.get(0), element), values);
                    case INVOKE_SPECIAL -> bbb.tailCall(bbb.exactMethodOf(paramValues.get(0), element), values);
                    case INVOKE_INTERFACE -> bbb.tailCall(bbb.interfaceMethodOf(paramValues.get(0), element), values);
                    default -> throw new IllegalStateException();
                }
                bbb.finish();
                BasicBlock entryBlock = BlockLabel.getTargetOf(entryLabel);
                Schedule schedule = Schedule.forMethod(entryBlock);
                return MethodBody.of(entryBlock, schedule, null, paramValues);
            }
        }, 0);
        StaticMethodElement dispatcher = (StaticMethodElement) builder.build();
        ctxt.enqueue(dispatcher);
        return StaticMethodPointer.of(dispatcher);
    }

    private Pointer generateNewInstanceDispatcher(final ConstructorElement element) {
        // generate a static dispatch helper method
        DefinedTypeDefinition enclosingType = element.getEnclosingType();
        TypeDescriptor enclosingDesc = enclosingType.getDescriptor();
        ClassContext classContext = enclosingType.getContext();
        MethodDescriptor origDesc = element.getDescriptor();
        List<TypeDescriptor> origParamTypes = origDesc.getParameterTypes();
        List<TypeDescriptor> newParamTypes = new ArrayList<>(origParamTypes.size() + 1);
        newParamTypes.add(enclosingDesc);
        newParamTypes.addAll(origParamTypes);
        MethodDescriptor newDesc = MethodDescriptor.synthesize(classContext, origDesc.getReturnType(), newParamTypes);
        ParameterElement.Builder pb;
        List<ParameterElement> origParams = element.getParameters();
        List<ParameterElement> dispatchParams = new ArrayList<>(origParams.size() + 1);
        // add a first parameter for the receiver
        pb = ParameterElement.builder("this", enclosingDesc, 0);
        pb.setSignature(TypeSignature.synthesize(classContext, enclosingDesc));
        pb.setEnclosingType(enclosingType);
        pb.setTypeParameterContext(enclosingType);
        dispatchParams.add(pb.build());
        // add each callee parameter
        int origParamCnt = origParamTypes.size();
        for (int i = 0; i < origParamCnt; i ++) {
            pb = ParameterElement.builder(origParams.get(i).getName(), origParamTypes.get(i), i + 1);
            pb.setSignature(TypeSignature.synthesize(classContext, origParamTypes.get(i)));
            pb.setEnclosingType(enclosingType);
            pb.setTypeParameterContext(enclosingType);
            dispatchParams.add(pb.build());
        }
        MethodElement.Builder builder = MethodElement.builder("dispatch_init", newDesc, 0);
        builder.setModifiers(ClassFile.ACC_PRIVATE | ClassFile.ACC_STATIC | ClassFile.I_ACC_NO_RESOLVE | ClassFile.I_ACC_NO_REFLECT | ClassFile.I_ACC_HIDDEN);
        builder.setEnclosingType(enclosingType);
        builder.setParameters(dispatchParams);
        builder.setSignature(MethodSignature.synthesize(classContext, newDesc));
        builder.setMethodBodyFactory(new MethodBodyFactory() {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            @Override
            public MethodBody createMethodBody(int index, ExecutableElement e) {
                BasicBlockBuilder bbb = classContext.newBasicBlockBuilder(e);
                InvokableType type = e.getType();
                int pcnt = type.getParameterCount();
                List<ParameterValue> paramValues = new ArrayList<>(pcnt);
                for (int i = 0; i < pcnt; i ++) {
                    paramValues.add(bbb.parameter(type.getParameterType(i), "p", i));
                }
                bbb.startMethod(paramValues);
                // build the entry block
                BlockLabel entryLabel = new BlockLabel();
                bbb.begin(entryLabel);
                List<Value> values = (List) paramValues.subList(1, paramValues.size());
                bbb.tailCall(bbb.constructorOf(paramValues.get(0), element), values);
                bbb.finish();
                BasicBlock entryBlock = BlockLabel.getTargetOf(entryLabel);
                Schedule schedule = Schedule.forMethod(entryBlock);
                return MethodBody.of(entryBlock, schedule, null, paramValues);
            }
        }, 0);
        StaticMethodElement dispatcher = (StaticMethodElement) builder.build();
        ctxt.enqueue(dispatcher);
        return StaticMethodPointer.of(dispatcher);
    }

    MethodDescriptor createFromMethodType(ClassContext classContext, VmObject methodType) {
        VmClass mtClass = methodType.getVmClass();
        if (! mtClass.getName().equals("java.lang.invoke.MethodType")) {
            throw new Thrown(linkageErrorClass.newInstance("Type argument is of wrong class"));
        }
        VmClass rtype = (VmClass) methodType.getMemory().loadRef(methodType.indexOf(methodTypeRTypeField), SinglePlain);
        if (rtype == null) {
            throw new Thrown(linkageErrorClass.newInstance("MethodType has null return type"));
        }
        VmReferenceArray ptypes = (VmReferenceArray) methodType.getMemory().loadRef(methodType.indexOf(methodTypePTypesField), SinglePlain);
        if (ptypes == null) {
            throw new Thrown(linkageErrorClass.newInstance("MethodType has null param types"));
        }
        int pcnt = ptypes.getLength();
        VmObject[] ptypesArray = ptypes.getArray();
        ArrayList<TypeDescriptor> paramTypes = new ArrayList<>(pcnt);
        for (int i = 0; i < pcnt; i ++) {
            VmClass clazz = (VmClass) ptypesArray[i];
            paramTypes.add(clazz.getDescriptor());
        }
        return MethodDescriptor.synthesize(classContext, rtype.getDescriptor(), paramTypes);
    }

    private Object methodHandleNativesObjectFieldOffset(final VmThread thread, final VmObject ignored, final List<Object> args) {
        VmObject name = (VmObject) args.get(0);
        // assume resolved
        VmClass clazz = (VmClass) name.getMemory().loadRef(name.indexOf(memberNameClazzField), SinglePlain);
        int idx = name.getMemory().load32(name.indexOf(memberNameIndexField), SinglePlain);
        if (idx == 0) {
            // todo: breakpoint
            idx = 0;
        }
        FieldElement field = clazz.getTypeDefinition().getField(idx);
        LayoutInfo layoutInfo;
        if (field.isStatic()) {
            throw new Thrown(linkageErrorClass.newInstance("Wrong field kind"));
        } else {
            layoutInfo = Layout.get(ctxt).getInstanceLayoutInfo(field.getEnclosingType());
        }
        return Long.valueOf(layoutInfo.getMember(field).getOffset());
    }

    private Object methodHandleNativesStaticFieldBase(final VmThread thread, final VmObject ignored, final List<Object> args) {
        return null;
    }

    private Object methodHandleNativesStaticFieldOffset(final VmThread thread, final VmObject ignored, final List<Object> args) {
        VmObject name = (VmObject) args.get(0);
        // assume resolved
        VmClass clazz = (VmClass) name.getMemory().loadRef(name.indexOf(memberNameClazzField), SinglePlain);
        int idx = name.getMemory().load32(name.indexOf(memberNameIndexField), SinglePlain);
        if (idx == 0) {
            // todo: breakpoint
            idx = 0;
        }
        StaticFieldElement field = (StaticFieldElement) clazz.getTypeDefinition().getField(idx);
        return StaticFieldPointer.of(field);
    }

    private Object nativeConstructorAccessorImplNewInstance0(final VmThread thread, final VmObject ignored, final List<Object> args) {
        VmObject ctor = (VmObject) args.get(0);
        VmReferenceArray argsArray = (VmReferenceArray) args.get(1);
        // unbox the hyper-boxed arguments
        List<Object> unboxed;
        if (argsArray == null) {
            unboxed = List.of();
        } else {
            int argCnt = argsArray.getLength();
            unboxed = new ArrayList<>(argCnt);
            VmObject[] argsArrayArray = argsArray.getArray();
            for (int i = 0; i < argCnt; i ++) {
                unboxed.add(unbox(argsArrayArray[i]));
            }
        }
        // the enclosing class
        VmClass clazz = (VmClass) ctor.getMemory().loadRef(ctor.indexOf(ctorClazzField), SinglePlain);
        int slot = ctor.getMemory().load32(ctor.indexOf(ctorSlotField), SinglePlain);
        ConstructorElement ce = clazz.getTypeDefinition().getConstructor(slot);
        ctxt.enqueue(ce);
        try {
            return vm.newInstance(clazz, ce, unboxed);
        } catch (Thrown thrown) {
            throw new Thrown(invocationTargetExceptionClass.newInstance(thrown.getThrowable()));
        }
    }

    private Object nativeMethodAccessorImplInvoke0(final VmThread thread, final VmObject ignored, final List<Object> args) {
        //    private static native Object invoke0(Method m, Object obj, Object[] args);
        VmObject method = (VmObject) args.get(0);
        VmObject receiver = (VmObject) args.get(1);
        VmReferenceArray argsArray = (VmReferenceArray) args.get(2);
        // unbox the hyper-boxed arguments
        List<Object> unboxed;
        if (argsArray == null) {
            unboxed = List.of();
        } else {
            int argCnt = argsArray.getLength();
            unboxed = new ArrayList<>(argCnt);
            VmObject[] argsArrayArray = argsArray.getArray();
            for (int i = 0; i < argCnt; i ++) {
                unboxed.add(unbox(argsArrayArray[i]));
            }
        }
        // the enclosing class
        VmClass clazz = (VmClass) method.getMemory().loadRef(method.indexOf(methodClazzField), SinglePlain);
        int slot = method.getMemory().load32(method.indexOf(methodSlotField), SinglePlain);
        MethodElement me = clazz.getTypeDefinition().getMethod(slot);
        ctxt.enqueue(me);
        try {
            if (me.isStatic()) {
                return box(vm.invokeExact(me, null, unboxed));
            } else {
                return box(vm.invokeVirtual(me, receiver, unboxed));
            }
        } catch (Thrown thrown) {
            throw new Thrown(invocationTargetExceptionClass.newInstance(thrown.getThrowable()));
        }
    }

    /**
     * Unbox one single hyper-boxed argument.
     *
     * @param boxed the hyper-boxed argument
     * @return the boxed equivalent
     */
    // todo: move to an autoboxing plugin?
    private Object unbox(final VmObject boxed) {
        if (boxed == null) {
            return null;
        }
        VmClass vmClass = boxed.getVmClass();
        if (vmClass == byteClass) {
            return Byte.valueOf((byte) boxed.getMemory().load8(boxed.indexOf(byteValueField), SinglePlain));
        } else if (vmClass == shortClass) {
            return Short.valueOf((short) boxed.getMemory().load16(boxed.indexOf(shortValueField), SinglePlain));
        } else if (vmClass == integerClass) {
            return Integer.valueOf(boxed.getMemory().load32(boxed.indexOf(integerValueField), SinglePlain));
        } else if (vmClass == longClass) {
            return Long.valueOf(boxed.getMemory().load64(boxed.indexOf(longValueField), SinglePlain));
        } else if (vmClass == characterClass) {
            return Character.valueOf((char) boxed.getMemory().load16(boxed.indexOf(characterValueField), SinglePlain));
        } else if (vmClass == floatClass) {
            return Float.valueOf(boxed.getMemory().loadFloat(boxed.indexOf(floatValueField), SinglePlain));
        } else if (vmClass == doubleClass) {
            return Double.valueOf(boxed.getMemory().loadDouble(boxed.indexOf(doubleValueField), SinglePlain));
        } else if (vmClass == booleanClass) {
            return Boolean.valueOf((boxed.getMemory().load8(boxed.indexOf(booleanValueField), SinglePlain) & 1) != 0);
        } else {
            // plain object
            return boxed;
        }
    }

    private VmObject box(final Object unboxed) {
        if (unboxed == null || unboxed instanceof VmObject) {
            return (VmObject)unboxed;
        } else if (unboxed instanceof Boolean bv) {
            return vm.box(ctxt.getBootstrapClassContext(), ctxt.getLiteralFactory().literalOf(bv.booleanValue()));
        } else if (unboxed instanceof Byte bv) {
            return vm.box(ctxt.getBootstrapClassContext(), ctxt.getLiteralFactory().literalOf(bv.byteValue()));
        } else if (unboxed instanceof Character cv) {
            return vm.box(ctxt.getBootstrapClassContext(), ctxt.getLiteralFactory().literalOf(cv.charValue()));
        } else if (unboxed instanceof Short sv) {
            return vm.box(ctxt.getBootstrapClassContext(), ctxt.getLiteralFactory().literalOf(sv.shortValue()));
        } else if (unboxed instanceof Integer iv) {
            return vm.box(ctxt.getBootstrapClassContext(), ctxt.getLiteralFactory().literalOf(iv.intValue()));
        } else if (unboxed instanceof Float fv) {
            return vm.box(ctxt.getBootstrapClassContext(), ctxt.getLiteralFactory().literalOf(fv.floatValue()));
        } else if (unboxed instanceof Long lv) {
            return vm.box(ctxt.getBootstrapClassContext(), ctxt.getLiteralFactory().literalOf(lv.longValue()));
        } else if (unboxed instanceof Double dv) {
            return vm.box(ctxt.getBootstrapClassContext(), ctxt.getLiteralFactory().literalOf(dv.doubleValue()));
        } else {
            throw new UnsupportedOperationException("Unexpected value for hyperboxing");
        }
    }

    /**
     * Resolve the injected {@code index} field of {@code ResolvedMethodName} or {@code MemberName}.
     *
     * @param index the field index (ignored)
     * @param typeDef the type definition (ignored)
     * @param builder the field builder
     * @return the resolved field
     */
    private FieldElement resolveIndexField(final int index, final DefinedTypeDefinition typeDef, final FieldElement.Builder builder) {
        builder.setEnclosingType(typeDef);
        builder.setModifiers(ClassFile.ACC_PRIVATE | ClassFile.I_ACC_NO_REFLECT);
        builder.setSignature(BaseTypeSignature.I);
        return builder.build();
    }

    /**
     * Resolve the injected {@code resolved} field of {@code MemberName}.
     *
     * @param index the field index (ignored)
     * @param typeDef the type definition (ignored)
     * @param builder the field builder
     * @return the resolved field
     */
    private FieldElement resolveResolvedField(final int index, final DefinedTypeDefinition typeDef, final FieldElement.Builder builder) {
        builder.setEnclosingType(typeDef);
        builder.setModifiers(ClassFile.ACC_PRIVATE | ClassFile.I_ACC_NO_REFLECT);
        builder.setSignature(BaseTypeSignature.Z);
        return builder.build();
    }

    /**
     * Resolve the injected {@code exactDispatcher} field of {@code MemberName}.
     *
     * @param index the field index (ignored)
     * @param typeDef the type definition (ignored)
     * @param builder the field builder
     * @return the resolved field
     */
    private FieldElement resolveExactDispatcherField(final int index, final DefinedTypeDefinition typeDef, final FieldElement.Builder builder) {
        builder.setEnclosingType(typeDef);
        builder.setModifiers(ClassFile.ACC_PRIVATE | ClassFile.I_ACC_NO_REFLECT);
        builder.setSignature(BaseTypeSignature.V);
        builder.setTypeResolver(fe -> ctxt.getTypeSystem().getVoidType().getPointer());
        return builder.build();
    }

    /**
     * Resolve the injected {@code clazz} field of {@code ResolvedMethodName}.
     *
     * @param index the field index (ignored)
     * @param typeDef the type definition (ignored)
     * @param builder the field builder
     * @return the resolved field
     */
    private FieldElement resolveClazzField(final int index, final DefinedTypeDefinition typeDef, final FieldElement.Builder builder) {
        builder.setEnclosingType(typeDef);
        builder.setModifiers(ClassFile.ACC_PRIVATE | ClassFile.I_ACC_NO_REFLECT);
        builder.setSignature(TypeSignature.synthesize(typeDef.getContext(), builder.getDescriptor()));
        return builder.build();
    }

    public CompoundType getCallStructureType(final InvokableType functionType) {
        CompoundType compoundType = functionCallStructures.get(functionType);
        if (compoundType == null) {
            int parameterCount = functionType.getParameterCount();
            if (parameterCount == 0) {
                return null;
            }
            CompoundType.Builder builder = CompoundType.builder(ctxt.getTypeSystem());
            for (int i = 0; i < parameterCount; i ++) {
                builder.addNextMember(functionType.getParameterType(i));
            }
            compoundType = builder.build();
            CompoundType appearing = functionCallStructures.putIfAbsent(functionType, compoundType);
            if (appearing != null) {
                compoundType = appearing;
            }
        }
        return compoundType;
    }
}
