package cc.quarkus.qcc.type;

/**
 * An object type whose elements are references.
 */
public final class ReferenceArrayObjectType extends ArrayObjectType {
    private final ReferenceType elementType;

    ReferenceArrayObjectType(final TypeSystem typeSystem, final boolean const_, final ClassObjectType objectClass, final ReferenceType elementType) {
        super(typeSystem, elementType.hashCode(), const_, objectClass);
        this.elementType = elementType;
    }

    ReferenceArrayObjectType constructConst() {
        return new ReferenceArrayObjectType(typeSystem, true, getSuperClassType(), elementType);
    }

    public long getSize() throws IllegalStateException {
        return 0;
    }

    public boolean isSubtypeOf(final ObjectType other) {
        return this == other
            || other instanceof ClassObjectType && isSubtypeOf((ClassObjectType) other)
            || other instanceof ReferenceArrayObjectType && isSubtypeOf((ReferenceArrayObjectType) other);
    }

    public boolean isSubtypeOf(final ClassObjectType other) {
        return other.getSuperClassType() == null; // j.l.O
    }

    public boolean isSubtypeOf(final ReferenceArrayObjectType other) {
        return this == other
            || elementType.getUpperBound().isSubtypeOf(other.elementType.getUpperBound());
    }

    public ReferenceType getElementType() {
        return elementType;
    }

    public StringBuilder toFriendlyString(final StringBuilder b) {
        return elementType.toFriendlyString(b.append("ref_array").append('.'));
    }
}
