package r.builtins;

import java.util.*;

import r.*;
import r.Convert;
import r.builtins.BuiltIn.NamedArgsBuiltIn.*;
import r.data.*;
import r.data.RComplex.RComplexUtils;
import r.data.RDouble.*;
import r.errors.*;
import r.nodes.*;
import r.nodes.truffle.*;

import com.oracle.truffle.runtime.*;

// FIXME: could be made much faster with direct access to the arrays (after materialization that is done anyway)
public class CumulativeSum {

    private static final String[] paramNames = new String[]{"x"};
    private static final int IX = 0;

    public static RComplex cumsum(RComplex x, Context context, ASTNode ast) {
        RComplex input = x.materialize();
        int size = x.size();
        double[] content = new double[2 * size];

        if (size > 0) {
            double raccum = 0;
            double iaccum = 0;
            for (int i = 0; i < size; i++) {
                double real = input.getReal(i);
                double imag = input.getImag(i);
                if (RComplexUtils.eitherIsNA(real, imag)) {
                    return finishComplexWithNAs(content, i);
                }
                raccum += real;
                iaccum += imag;
                content[2 * i] = raccum;
                content[2 * i + 1] = iaccum;
            }
        }
        return RComplex.RComplexFactory.getFor(content); // drop dimensions
    }

    public static RDouble cumsum(RDouble x, Context context, ASTNode ast) {
        RDouble input = x.materialize();
        int size = x.size();
        double[] content = new double[size];

        if (size > 0) {
            double accum = 0;
            for (int i = 0; i < size; i++) {
                double value = input.getDouble(i);
                if (RDoubleUtils.isNA(value)) {
                    return finishDoubleWithNAs(content, i);
                }
                accum += value;
                content[i] = accum;
            }
        }
        return RDouble.RDoubleFactory.getFor(content); // drop dimensions
    }

    private static RComplex finishComplexWithNAs(double[] content, int fromIndex) {
        Arrays.fill(content, 2 * fromIndex, content.length, RDouble.NA);
        return RComplex.RComplexFactory.getFor(content);
    }

    private static RDouble finishDoubleWithNAs(double[] content, int fromIndex) {
        Arrays.fill(content, fromIndex, content.length, RDouble.NA);
        return RDouble.RDoubleFactory.getFor(content);
    }

    public static RInt cumsum(RInt x, RContext context, ASTNode ast) {
        RInt input = x.materialize();
        int size = x.size();
        int[] content = new int[size];

        if (size > 0) {
            int accum = 0;
            for (int i = 0; i < size; i++) {
                int value = input.getInt(i);
                if (value == RInt.NA) {
                    return finishWithNAs(content, i);
                }
                accum = Arithmetic.ADD.op(context, ast, accum, value);
                if (accum == RInt.NA) {
                    context.warning(ast, RError.INTEGER_OVERFLOW);
                    return finishWithNAs(content, i);
                }
                content[i] = accum;
            }
        }
        return RInt.RIntFactory.getFor(content); // drop dimensions
    }

    private static RInt finishWithNAs(int[] content, int fromIndex) {
        for (int i = fromIndex; i < content.length; i++) {
            content[i] = RInt.NA;
        }
        return RInt.RIntFactory.getFor(content);
    }


    public static final CallFactory FACTORY = new CallFactory() {
        @Override
        public RNode create(ASTNode call, RSymbol[] names, RNode[] exprs) {

            AnalyzedArguments a = BuiltIn.NamedArgsBuiltIn.analyzeArguments(names, exprs, paramNames);
            final boolean[] provided = a.providedParams;

            if (!provided[IX]) {
                BuiltIn.missingArg(call, paramNames[IX]);
            }

            return new BuiltIn.BuiltIn1(call, names, exprs) {

                @Override
                public RAny doBuiltIn(RContext context, Frame frame, RAny x) {

                    if (x instanceof RDouble) {
                        return cumsum((RDouble) x, context, ast);
                    } else if (x instanceof RInt) {
                        return cumsum((RInt) x, context, ast);
                    } else if (x instanceof RLogical) {
                        return cumsum(((RLogical) x).asInt(), context, ast);
                    } else if (x instanceof RComplex) {
                        return cumsum((RComplex) x, context, ast);
                    } else if (x instanceof RRaw) {
                        return cumsum(((RRaw) x).asDouble(), context, ast);
                    } else if (x instanceof RNull) {
                        return RDouble.EMPTY;
                    }

                    if (x instanceof RString) {
                        RDouble res = cumsum(Convert.coerceToDoubleWarning(x, context, ast), context, ast);
                        return res;
                    } else {
                        Utils.nyi("unsupported type");
                        return null;
                    }
                }
            };
        }
    };
}