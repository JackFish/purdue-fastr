package r.builtins;

import r.data.*;
import r.nodes.*;

final class AsInteger extends AsBase {
    static final CallFactory _ = new AsInteger("as.integer", new String[]{"x", "..."}, new String[]{});

    private AsInteger(String name, String[] params, String[] required) {
        super(name, params, required);
    }

    @Override public RAny genericCast(ASTNode ast, RAny arg) {
        return genericAsInt(ast, arg);
    }

    @Override public RAny getEmpty() {
        return RInt.EMPTY;
    }
}
