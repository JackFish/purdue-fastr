package r.nodes;

import r.*;
import r.data.*;
import r.errors.*;

public abstract class AssignVariable extends ASTNode {

    final boolean isSuper;
    ASTNode rhs;

    AssignVariable(boolean isSuper, ASTNode expr) {
        this.isSuper = isSuper;
        rhs = updateParent(expr);
    }

    @Override
    public void visit_all(Visitor v) {
        getExpr().accept(v);
    }

    public ASTNode getExpr() {
        return rhs;
    }

    public boolean isSuper() {
        return isSuper;
    }

    public static ASTNode create(boolean isSuper, ASTNode lhs, ASTNode rhs) {
        if (lhs instanceof SimpleAccessVariable) {
            return writeVariable(isSuper, ((SimpleAccessVariable) lhs).symbol, rhs);
        } else if (lhs instanceof AccessVector) {
            return writeVector(isSuper, (AccessVector) lhs, rhs);
        } else if (lhs instanceof FieldAccess) {
            return writeField(isSuper, (FieldAccess) lhs, rhs);
        } else if (lhs instanceof FunctionCall) {
            return writeFunction(isSuper, (FunctionCall) lhs, rhs);
        } else if (lhs instanceof Constant) {
            throw RError.getUnknownObject(rhs); // TODO it's own exception
        }
        Utils.nyi();
        return null;
    }

    public static ASTNode writeVariable(boolean isSuper, RSymbol name, ASTNode rhs) {
        return new SimpleAssignVariable(isSuper, name, rhs);
    }

    public static ASTNode writeVector(boolean isSuper, AccessVector lhs, ASTNode rhs) {
        return new UpdateVector(isSuper, lhs, rhs);
    }

    public static ASTNode writeField(boolean isSuper, FieldAccess lhs, ASTNode rhs) {
        return new UpdateField(isSuper, lhs, rhs);
    }

    public static ASTNode writeFunction(boolean isSuper, FunctionCall lhs, ASTNode rhs) {
        // FIXME Probably we need a special node, for now all assign function should return value
        lhs.name = RSymbol.getSymbol(lhs.name.pretty() + "<-");
        if (lhs.args.size() > 0) {
            ASTNode first = lhs.args.first().getValue();
            if (!(first instanceof SimpleAccessVariable)) {
                Utils.nyi(); // TODO here we need to flatten complex assignments
            } else {
                lhs.args.add("value", rhs);
            }
        }
        lhs.isAssignment(true);
        lhs.isSuper(isSuper);
        return lhs;
    }
}
