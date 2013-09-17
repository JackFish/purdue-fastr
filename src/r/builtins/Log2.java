package r.builtins;

import r.nodes.ast.*;

/**
 * "log2"
 * 
 * <pre>
 * x -- a numeric or complex vector.
 * </pre>
 */
// TODO: complex numbers
final class Log2 extends MathBase {

    static final CallFactory _ = new Log2("log2");

    private Log2(String name) {
        super(name);
    }

    final double rLOG2 = 1 / Math.log(2.0);

    @Override double op(ASTNode ast, double value) {
        return Math.log(value) * rLOG2;
    }
}
