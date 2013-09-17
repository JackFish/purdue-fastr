package r.builtins;

import r.data.*;
import r.nodes.ast.*;

/**
 * "as.raw"
 * 
 * <pre>
 * x -- An object.
 * </pre>
 */
final class AsRaw extends AsBase {
    static final CallFactory _ = new AsRaw("as.raw", new String[]{"x"}, new String[]{"x"});

    private AsRaw(String name, String[] params, String[] required) {
        super(name, params, required);
    }

    @Override RAny genericCast(ASTNode ast, RAny arg) {
        return AsBase.genericAsRaw(ast, arg);
    }

    @Override public RAny getEmpty() {
        return RInt.EMPTY;
    }

}
