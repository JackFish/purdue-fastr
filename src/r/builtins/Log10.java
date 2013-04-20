package r.builtins;

import r.nodes.*;

// TODO: complex numbers
final class Log10 extends MathBase {

    static final CallFactory _ = new Log10("log10");

    private Log10(String name) {
        super(name);
    }

    @Override double op(ASTNode ast, double value) {
        return Math.log10(value);
    }

}
