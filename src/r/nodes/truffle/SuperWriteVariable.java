package r.nodes.truffle;

import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.frame.*;

import r.data.*;
import r.data.RFunction.*;
import r.nodes.*;

public abstract class SuperWriteVariable extends BaseR {

    public final RSymbol symbol;
    @Child RNode expr;


    private SuperWriteVariable(ASTNode ast, RSymbol symbol, RNode expr) {
        super(ast);
        this.symbol = symbol;
        this.expr = adoptChild(expr);
    }

    public static SuperWriteVariable getUninitialized(ASTNode orig, RSymbol sym, RNode rhs) {
        return new SuperWriteVariable(orig, sym, rhs) {

            private Object replaceAndExecute(RNode node, String reason, Frame frame) {
                replace(node, reason);
                return node.execute(frame);
            }

            @Override
            public final Object execute(Frame frame) {
                try {
                    throw new UnexpectedResultException(null);
                } catch (UnexpectedResultException e) {
                    Frame enclosingFrame = (frame != null) ? RFrameHeader.enclosingFrame(frame) : null;

                    if (enclosingFrame == null) {
                        return replaceAndExecute(WriteVariable.getWriteTopLevel(ast, symbol, expr), "install WriteTopLevel from SuperWriteVariable", frame);
                    }

                    FrameSlot slot = RFrameHeader.findVariable(enclosingFrame, symbol);
                    if (slot != null) {
                        return replaceAndExecute(getWriteViaWriteSet(ast, symbol, expr, slot), "install WriteViaWriteSet from SuperWriteVariable", frame);
                    }

                    EnclosingSlot eslot = RFrameHeader.findEnclosingVariable(enclosingFrame, symbol);
                    if (eslot == null) {
                        return replaceAndExecute(getWriteToTopLevel(ast, symbol, expr), "install WriteToTopLevel from SuperWriteVariable", frame);
                    } else {
                        return replaceAndExecute(getWriteViaEnclosingSlot(ast, symbol, expr, eslot.hops, eslot.slot), "install WriteViaReadSet from SuperWriteVariable", frame);
                    }
                }
            }
        };
    }

    public static SuperWriteVariable getWriteViaWriteSet(ASTNode ast, RSymbol symbol, RNode expr, final FrameSlot slot) {
        return new SuperWriteVariable(ast, symbol, expr) {
            @Override
            public Object execute(Frame frame) {
                RAny value = (RAny) expr.execute(frame);
                Frame enclosing = RFrameHeader.enclosingFrame(frame);
                boolean done = RFrameHeader.superWriteViaWriteSet(enclosing, slot, symbol, value);
                assert done;
                return value;
            }
        };
    }

    public static SuperWriteVariable getWriteViaEnclosingSlot(ASTNode ast, RSymbol symbol, RNode expr, final int hops, final FrameSlot slot) {
        return new SuperWriteVariable(ast, symbol, expr) {
            @Override
            public final Object execute(Frame frame) {
                RAny value = (RAny) expr.execute(frame);
                Frame enclosing = RFrameHeader.enclosingFrame(frame);
                boolean done = RFrameHeader.superWriteViaEnclosingSlotAndTopLevel(enclosing, hops, slot, symbol, value);
                assert done;
                return value;
            }
        };
    }

    public static SuperWriteVariable getWriteToTopLevel(ASTNode ast, RSymbol symbol, RNode expr) {
        return new SuperWriteVariable(ast, symbol, expr) {

            int version;

            @Override
            public final Object execute(Frame frame) {
                RAny value = (RAny) expr.execute(frame);
                Frame enclosingFrame = RFrameHeader.enclosingFrame(frame);

                // TODO check if 'version' is enough, I think the good test has to be:
                // if (frame != oldFrame || version != symbol.getVersion()) {
                // (same as ReadVariable)

                if (version != symbol.getVersion()) {
                    if (!RFrameHeader.superWriteToExtensionEntry(enclosingFrame, symbol, value)) {
                        version = symbol.getVersion();
                        // oldFrame = frame;
                    }
                }
                RFrameHeader.superWriteToTopLevel(symbol, value);
                return value;
            }
        };
    }
}
