package r.nodes.ast;

import r.data.*;
import r.nodes.ast.ArgumentList.*;

public abstract class Call extends ASTNode {

    ArgumentList args;

    public Call(ArgumentList alist) {
        args = alist;
    }

    @Override
    public void visit_all(Visitor v) {
        for (Entry e : args) {
            ASTNode n = e.getValue();
            if (n != null) {
                n.accept(v);
            }
        }
    }

    public ArgumentList getArgs() {
        return args;
    }

    public static ASTNode create(ASTNode call, ArgumentList args) {
        if (call instanceof SimpleAccessVariable) {
            SimpleAccessVariable ccall = (SimpleAccessVariable) call;
            return create(ccall.getSymbol(), args);
        }
        return null;
    }

    public static ASTNode create(RSymbol funName, ArgumentList args) {
        return new FunctionCall(funName, args);
    }

    public static ASTNode create(CallOperator op, ASTNode lhs, ArgumentList args) {
        return new AccessVector(lhs, args, op == CallOperator.SUBSET);
    }

    public enum CallOperator {
        SUBSET, SUBSCRIPT
    }
}
