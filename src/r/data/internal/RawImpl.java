package r.data.internal;

import r.*;
import r.Convert.*;
import r.data.*;


public class RawImpl extends NonScalarArrayImpl implements RRaw {

    final byte[] content;

    public RawImpl(int size) {
        content = new byte[size];
    }

    public byte[] getContent() {
        return content;
    }

    public RawImpl(byte[] values, int[] dimensions, Names names, Attributes attributes, boolean doCopy) {
        if (doCopy) {
            content = new byte[values.length];
            System.arraycopy(values, 0, content, 0, values.length);
        } else {
            content = values;
        }
        this.dimensions = dimensions;
        this.names = names;
        this.attributes = attributes;
    }

    public RawImpl(byte[] values, int[] dimensions) {
        this(values, dimensions, null, null, true);
    }

    public RawImpl(byte[] values) {
        this(values, null, null, null, true);
    }

    public RawImpl(RRaw r, boolean valuesOnly) {
        content = new byte[r.size()];
        for (int i = 0; i < content.length; i++) {
            content[i] = r.getRaw(i);
        }
        if (!valuesOnly) {
            dimensions = r.dimensions();
            names = r.names();
            attributes = r.attributes();
        }
    }

    public RawImpl(RRaw r, int[] dimensions, Names names, Attributes attributes) {
        content = new byte[r.size()];
        for (int i = 0; i < content.length; i++) {
            content[i] = r.getRaw(i);
        }
        this.dimensions = dimensions;
        this.names = names;
        this.attributes = attributes;
    }

    @Override
    public int size() {
        return content.length;
    }

    @Override
    public Object get(int i) {
        return content[i];
    }

    @Override
    public byte getRaw(int i) {
        return content[i];
    }

    @Override
    public RAny boxedGet(int i) {
        return RRawFactory.getScalar(getRaw(i));
    }

    @Override
    public boolean isNAorNaN(int i) {
        return false;
    }

    @Override
    public RArray set(int i, Object val) {
        return set(i, ((Byte) val).byteValue());
    }

    @Override
    public RRaw set(int i, byte val) {
        content[i] = val;
        return this;
    }

    @Override
    public RRaw asRaw() {
        return this;
    }

    @Override
    public RRaw asRaw(ConversionStatus warn) {
        return this;
    }

    @Override
    public RLogical asLogical() {
        return new RRaw.RLogicalView(this);
    }

    @Override
    public RLogical asLogical(ConversionStatus warn) {
        return asLogical();
    }

    @Override
    public RInt asInt() {
        return new RRaw.RIntView(this);
    }

    @Override
    public RInt asInt(ConversionStatus warn) {
        return asInt();
    }

    @Override
    public RDouble asDouble() {
        return new RRaw.RDoubleView(this);
    }

    @Override
    public RDouble asDouble(ConversionStatus warn) {
        return asDouble();
    }

    @Override
    public RComplex asComplex() {
        return new RRaw.RComplexView(this);
    }

    @Override
    public RComplex asComplex(ConversionStatus warn) {
        return asComplex();
    }

    @Override
    public RString asString() {
        return new RRaw.RStringView(this);
    }

    @Override
    public RString asString(ConversionStatus warn) {
        return asString();
    }

    @Override
    public RawImpl materialize() {
        return this;
    }

    private static final String EMPTY_STRING = RRaw.TYPE_STRING + "(0)";
    private static final String NAMED_EMPTY_STRING = "named " + EMPTY_STRING;

    @Override
    public String pretty() {
        StringBuilder str = new StringBuilder();
        if (dimensions != null) {
            str.append(arrayPretty());
        } else if (content.length == 0) {
            str.append((names() == null) ? EMPTY_STRING : NAMED_EMPTY_STRING);
        } else if (names() != null) {
            str.append(namedPretty());
        } else {
            str.append(Convert.raw2string(content[0]));
            for (int i = 1; i < content.length; i++) {
                str.append(", ");
                str.append(Convert.raw2string(content[i]));
            }
        }
        str.append(attributesPretty());
        return str.toString();
    }

    @Override
    public String typeOf() {
        return RRaw.TYPE_STRING;
    }

    @Override
    public RArray subset(RInt index) {
        return RRaw.RRawFactory.subset(this, index);
    }

    @Override
    public RawImpl doStrip() {
        return new RawImpl(content, null, null, null, false);
    }

    @Override
    public RawImpl doStripKeepNames() {
        return new RawImpl(content, null, names, null, false);
    }

}
