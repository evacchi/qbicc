package cc.quarkus.qcc.graph.literal;

import cc.quarkus.qcc.constraint.Constraint;
import cc.quarkus.qcc.graph.ValueVisitor;
import cc.quarkus.qcc.type.ValueType;

/**
 * A zero-initializer literal of some type which can be used to initialize arrays and structures.
 */
public final class ZeroInitializerLiteral extends Literal {
    private final ValueType type;

    ZeroInitializerLiteral(final ValueType type) {
        this.type = type;
    }

    public ValueType getType() {
        return type;
    }

    public Constraint getConstraint() {
        return Constraint.equalTo(this);
    }

    public boolean equals(final Literal other) {
        return other instanceof ZeroInitializerLiteral && equals((ZeroInitializerLiteral) other);
    }

    public boolean equals(final ZeroInitializerLiteral other) {
        return other == this || other != null && type.equals(other.type);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public int hashCode() {
        return ZeroInitializerLiteral.class.hashCode();
    }

    public String toString() {
        return "{0}";
    }
}
