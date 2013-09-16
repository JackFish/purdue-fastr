package r.nodes.truffle;

import com.oracle.truffle.api.frame.*;

import r.data.*;
import r.nodes.*;

public class Constant extends BaseR {
    final RAny value;

    public Constant(ASTNode ast, RAny val) {
        super(ast);
        value = val;
    }

    @Override public final RAny execute(Frame frame) {
        return value;
    }

    public static RNode getNull() {
        return new Constant(null, RNull.getNull());
    }

    public RAny value() {
        return value;
    }

    @Override public String toString() {
        return "Constant(" + value + ")";
    }
}
