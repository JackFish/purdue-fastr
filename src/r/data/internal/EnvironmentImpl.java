package r.data.internal;

import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;

import r.*;
import r.Convert.ConversionStatus;
import r.builtins.*;
import r.data.*;
import r.errors.*;
import r.nodes.*;


public class EnvironmentImpl extends BaseObject implements REnvironment {

    final MaterializedFrame frame;

    public EnvironmentImpl(MaterializedFrame frame) {
        this.frame = frame;
    }

    @Override
    public MaterializedFrame frame() {
        return frame;
    }

    @Override
    public String pretty() {
        Utils.check(frame != null);
        return "<environment: " + frame + "(" + this + ")>";
    }

    @Override
    public String typeOf() {
        return REnvironment.TYPE_STRING;
    }

    @Override
    public RAny stripAttributes() {
        Utils.nyi();
        return null;
    }

    @Override
    public RRaw asRaw() {
        Utils.nyi();
        return null;
    }

    @Override
    public RLogical asLogical() {
        Utils.nyi();
        return null;
    }

    @Override
    public RInt asInt() {
        Utils.nyi();
        return null;
    }

    @Override
    public RDouble asDouble() {
        Utils.nyi();
        return null;
    }

    @Override
    public RComplex asComplex() {
        Utils.nyi();
        return null;
    }

    @Override
    public RString asString() {
        Utils.nyi();
        return null;
    }

    @Override
    public RList asList() {
        Utils.nyi();
        return null;
    }

    @Override
    public RRaw asRaw(ConversionStatus warn) {
        Utils.nyi();
        return null;
    }

    @Override
    public RLogical asLogical(ConversionStatus warn) {
        Utils.nyi();
        return null;
    }

    @Override
    public RInt asInt(ConversionStatus warn) {
        Utils.nyi();
        return null;
    }

    @Override
    public RDouble asDouble(ConversionStatus warn) {
        Utils.nyi();
        return null;
    }

    @Override
    public RComplex asComplex(ConversionStatus warn) {
        Utils.nyi();
        return null;
    }

    @Override
    public RString asString(ConversionStatus warn) {
        Utils.nyi();
        return null;
    }

    @Override
    public void ref() {
    }

    @Override
    public boolean isShared() {
        return false; // never copy
    }

    @Override
    public boolean isTemporary() {
        return true; // can modify
    }

    @Override
    public void assign(RSymbol name, RAny value, boolean inherits, ASTNode ast) {
        if (!inherits) {
            RFrameHeader.localWrite(frame, name, value);
            return;
        } else {
            RFrameHeader.reflectiveInheritsWrite(frame, name, value);
        }
    }

    @Override
    public void delayedAssign(RSymbol name, RPromise value, ASTNode ast) {
        RFrameHeader.localWriteNoRef(frame, name, value);
    }

    @Override
    public RAny get(RSymbol name, boolean inherits) {
        if (!inherits) {
            return (RAny) RFrameHeader.localRead(frame, name);
        } else {
            return Utils.cast(RFrameHeader.read(frame, name));
        }
    }

    @Override
    public Object localGetNotForcing(RSymbol name) {
        return RFrameHeader.localReadNotForcing(frame, name);
    }

    @Override
    public boolean exists(RSymbol name, boolean inherits) {
        if (!inherits) {
            return RFrameHeader.localExists(frame, name);
        } else {
            return RFrameHeader.exists(frame, name);
        }
    }

    @Override
    public RCallable match(RSymbol name) {
        return RFrameHeader.match(frame, name);
    }

    public static RSymbol[] removeHidden(RSymbol[] symbols) { // FIXME: unnecessary copying
        ArrayList<RSymbol> nonHidden = new ArrayList<RSymbol>(symbols.length);

        for (RSymbol s : symbols) {
            if (!s.isHidden()) {
                nonHidden.add(s);
            }
        }
        return nonHidden.toArray(new RSymbol[nonHidden.size()]);
    }

    @Override
    public RSymbol[] ls(boolean includingHidden) { // FIXME: maybe could speed-up by propagating the filtering further
        RSymbol[] symbols =  RFrameHeader.listSymbols(frame);
        if (!includingHidden) {
            return removeHidden(symbols);
        } else {
            return symbols;
        }
    }

    public static Object readFromTopLevel(RSymbol symbol) {
        return symbol.getValue();
    }

    // a custom environment has no function (no read set or write set)
    // it always has an extension
    public static class Custom extends EnvironmentImpl implements REnvironment {

        public Custom(MaterializedFrame frame) {
            super(frame);
            assert Utils.check(frame != null);
        }

        // NOTE: rootEnvironment only needs to be set when parentFrame is null
        // NOTE: rootEnvironment == null means the global environment
        public static Custom create(MaterializedFrame parentFrame, REnvironment rootEnvironment, boolean hash, int hashSize) {

            RFrameHeader header = new RFrameHeader(new DummyFunction(), parentFrame, null);
            MaterializedFrame newFrame = Truffle.getRuntime().createMaterializedFrame(header);
            if (hash) {
                RFrameHeader.installHashedExtension(newFrame, hashSize);
            } else {
                RFrameHeader.installExtension(newFrame);
            }
            RFrameHeader.setRootEnvironment(newFrame, rootEnvironment);
            return new Custom(newFrame);
        }

        public static MaterializedFrame createForList(MaterializedFrame parentFrame, RList list) {

            RFrameHeader header = new RFrameHeader(new DummyFunction(), parentFrame, null);
            MaterializedFrame newFrame = Truffle.getRuntime().createMaterializedFrame(header);
            int size = list.size();
            RFrameHeader.installHashedExtension(newFrame, size);
            RArray.Names names = list.names();
            if (names != null) {
                RSymbol[] symbols = names.sequence();
                for (int i = 0; i < size; i++) {
                    RSymbol s = symbols[i];
                    if (s != RSymbol.NA_SYMBOL && s != RSymbol.EMPTY_SYMBOL) {
                        RFrameHeader.localWrite(newFrame, s, list.getRAnyRef(i));
                    }
                }
            }

            return newFrame;
        }

        @Override
        public void assign(RSymbol name, RAny value, boolean inherits, ASTNode ast) {
            if (!inherits) {
                RFrameHeader.customLocalWrite(frame, name, value);
                return;
            } else {
                RFrameHeader.customReflectiveInheritsWrite(frame, name, value);
            }
        }

        @Override
        public void delayedAssign(RSymbol name, RPromise value, ASTNode ast) {
            RFrameHeader.customLocalWriteNoRef(frame, name, value);
        }

        @Override
        public RAny get(RSymbol name, boolean inherits) {
            if (!inherits) {
                return (RAny) RFrameHeader.customLocalRead(frame, name);
            } else {
                return Utils.cast(RFrameHeader.customRead(frame, name));
            }
        }

        @Override
        public Object localGetNotForcing(RSymbol name) {
            return RFrameHeader.customLocalReadNoForcing(frame, name);
        }

        @Override
        public boolean exists(RSymbol name, boolean inherits) {
            if (!inherits) {
                return RFrameHeader.customLocalExists(frame, name);
            } else {
                return RFrameHeader.customExists(frame, name);
            }
        }

        @Override
        public RCallable match(RSymbol name) {
            Utils.nyi("generic match");
            return null;
        }
    }

    public static class Global extends EnvironmentImpl implements REnvironment {

        public Global() {
            super(null);
        }

        @Override
        public void assign(RSymbol name, RAny value, boolean inherits, ASTNode ast) {
            RFrameHeader.writeToTopLevelCondRef(name, value);
        }

        @Override
        public void delayedAssign(RSymbol name, RPromise value, ASTNode ast) {
            RFrameHeader.writeToTopLevelNoRef(name, value);
        }

        @Override
        public RAny get(RSymbol name, boolean inherits) {
            if (!inherits) {
                return Utils.cast(readFromTopLevel(name));
            } else {
                RAny res = Utils.cast(readFromTopLevel(name));
                if (res != null) {
                    return res;
                }
                // builtins
                return Primitives.getBuiltIn(name, null);
            }
        }

        @Override
        public Object localGetNotForcing(RSymbol name) {
            return name.getValueNoForce();
        }

        @Override
        public boolean exists(RSymbol name, boolean inherits) {
            if (!inherits) {
                return readFromTopLevel(name) != null;
            } else {
                RAny res = Utils.cast(readFromTopLevel(name));
                if (res != null) {
                    return true;
                }
                return Primitives.hasCallFactory(name, null);
            }
        }

        @Override
        public RCallable match(RSymbol name) {
            Object res = readFromTopLevel(name);
            if (res != null && res instanceof RCallable) {
                return (RCallable) res;
            }
            // builtins
            return Primitives.getBuiltIn(name, null);
        }

        @Override
        public RSymbol[] ls(boolean includingHidden) {
            return RSymbol.listUsedSymbols(includingHidden);
        }

        @Override
        public String pretty() {
            return "<environment: R_GlobalEnv>";
        }
    }

    public static class Empty extends EnvironmentImpl implements REnvironment {

        public Empty() {
            super(null);
        }

        @Override
        public void assign(RSymbol name, RAny value, boolean inherits, ASTNode ast) {
            throw RError.getAssignEmpty(ast);
        }

        @Override
        public void delayedAssign(RSymbol name, RPromise value, ASTNode ast) {
            throw RError.getAssignEmpty(ast);
        }

        @Override
        public RAny get(RSymbol name, boolean inherits) {
            return null;
        }

        @Override
        public Object localGetNotForcing(RSymbol name) {
            return null;
        }

        @Override
        public boolean exists(RSymbol name, boolean inherits) {
            return false;
        }

        @Override
        public RCallable match(RSymbol name) {
            return null;
        }

        @Override
        public RSymbol[] ls(boolean includingHidden) {
            return RSymbol.EMPTY_SYMBOL_ARRAY;
        }

        @Override
        public String pretty() {
            return "<environment: R_EmptyEnv>";
        }
    }

    @Override
    public Attributes attributes() {
        return null;
    }

    @Override
    public Attributes attributesRef() {
        return null;
    }

    @Override
    public RArray setAttributes(Attributes attributes) {
        Utils.nyi();
        return null;
    }

    @Override
    public boolean dependsOn(RAny value) {
        return false;
    }

}
