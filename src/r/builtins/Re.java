package r.builtins;

import r.data.*;
import r.data.internal.*;
import r.errors.*;
import r.nodes.ast.*;
import r.nodes.exec.*;
import r.runtime.*;

public class Re extends CallFactory {

    static final CallFactory _ = new Re("Re", new String[]{"x"}, null);

    private Re(String name, String[] params, String[] required) {
        super(name, params, required);
    }

    @Override public RNode create(ASTNode call, RSymbol[] names, RNode[] exprs) {
        return new Builtin.Builtin1(call, names, exprs) {

            @Override public RAny doBuiltIn(Frame frame, RAny arg) {
                if (arg instanceof RComplex) {
                    final RComplex orig = (RComplex) arg;
                    return TracingView.ViewTrace.trace(new View.RDoubleProxy<RComplex>(orig) {

                        @Override public double getDouble(int i) {
                            return orig.getReal(i);
                        }

                        @Override
                        public void accept(ValueVisitor v) {
                            v.visit(this);
                        }

                    });
                } else if (arg instanceof RDouble || arg instanceof RInt || arg instanceof RLogical) {
                    return arg.asDouble();
                } else {
                    throw RError.getNonNumericArgumentFunction(ast);
                }
            }
        };
    }
}
