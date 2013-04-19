package r.nodes.truffle;

import r.Truffle.*;

import r.data.*;
import r.errors.RError;
import r.nodes.ASTNode;

// implements the assignment part of an array update
//
// for vector update xSymbol [ indexNode ] <- newValueNode
//   lhs is xSymbol
//   rhs is newValueNode
//
//   UpdateVariable will call assignmentNode.execute( currentValue(xSymbol), newValue )
//     assignmentNode.execute returns the new value of x
//   UpdateVariable then assigns newValueOfX to xSymbol

public class UpdateArrayAssignment extends BaseR {

    public static UpdateArrayAssignment create(ASTNode orig, RSymbol lhs, RNode rhs, AssignmentNode assignment) {
        if (rhs instanceof Constant) {
            return new UpdateArrayAssignment.Const(orig, lhs, rhs, assignment);
        } else {
            return new UpdateArrayAssignment(orig, lhs, rhs, assignment);
        }
    }

    /**
     * UpdateVariable node that performs the assignment itself and returns the lhs.
     * <p/>
     * If the assignment is in-place the returned value is the same as the lhs argument, otherwise the returned value is
     * the new value.
     */
    public abstract static class AssignmentNode extends BaseR {

        /** Standard constructor. */
        public AssignmentNode(ASTNode orig) {
            super(orig);
        }

        /**
         * Override this method to define the assignment operation.
         * 
         * @param frame
         *            Frame of the execute method.
         * @param lhs
         *            Left hand side (assign to)
         * @param rhs
         *            Right hand side (assign from)
         * @return Left hand side object after assignment.
         */
        @Override public abstract RAny execute(Frame frame, RAny lhs, RAny rhs);

        /** Calls to execute method on frame only are *not* supported. */
        @Override public Object execute(Frame frame) {
            assert (false) : " calls to execute(frame) method for AssignmentNode is not supported.";
            return null;
        }

    }

    /** LHS of the assignment operator == what to update. */
    final RSymbol lhs;

    /** RHS of the assignment operator == new values. */
    @Child RNode rhs;

    /** UpdateVariable node performing the assignment itself. */
    @Child AssignmentNode assignment;

    protected UpdateArrayAssignment(ASTNode orig, RSymbol lhs, RNode rhs, AssignmentNode assignment) {
        super(orig);
        this.lhs = lhs;
        this.rhs = adoptChild(rhs);
        this.assignment = (AssignmentNode) adoptChild(assignment);
    }

    /** Copy constructor for the replacement calls. */
    protected UpdateArrayAssignment(UpdateArrayAssignment other) {
        super(other.getAST());
        this.lhs = other.lhs;
        this.rhs = adoptChild(other.rhs);
        this.assignment = (AssignmentNode) adoptChild(other.assignment);
    }

    /**
     * Default execution, rewrites itself either to a top level assignment, or determines the frameslot and replaces to
     * the LocalAssignment.
     */
    @Override public Object execute(Frame frame) {
        try {
            throw new UnexpectedResultException(null);
        } catch (UnexpectedResultException e) {
            if (frame != null) {
                int frameSlot = RFrameHeader.findVariable(frame, lhs);
                return replace(new Local(this, frameSlot)).execute(frame);
            } else {
                return replace(new TopLevel(this)).execute(frame);
            }
        }
    }

    /** Basic assignment for const rhs values. */
    public static class Const extends UpdateArrayAssignment {

        protected Const(ASTNode orig, RSymbol lhs, RNode rhs, AssignmentNode assignment) {
            super(orig, lhs, rhs, assignment);
            assert (rhs instanceof Constant) : "for non-constant RHS use UpdateVariable class";
        }

        protected Const(UpdateArrayAssignment other) {
            super(other);
        }

        /**
         * Default execution, rewrites itself either to a top level assignment, or determines the frameslot and replaces
         * to the LocalAssignment.
         */
        @Override public Object execute(Frame frame) {
            try {
                throw new UnexpectedResultException(null);
            } catch (UnexpectedResultException e) {
                if (frame != null) {
                    int frameSlot = RFrameHeader.findVariable(frame, lhs);
                    return replace(new ConstLocal(this, frameSlot, (RAny) rhs.execute(frame))).execute(frame);
                } else {
                    return replace(new ConstTopLevel(this, (RAny) rhs.execute(frame))).execute(frame);
                }
            }
        }
    }

    /**
     * Assigns a local variable.
     * <p/>
     * The assignment already knows its frameslot. If the frameslot is null, the node rewrites itself to the general
     * assignment. If the frame is null, the node rewrites itself to the top level assignment.
     */
    protected static class Local extends UpdateArrayAssignment {

        public static enum Failure {
            NULL_FRAME, NULL_SLOT
        }

        /** Frameslot of the lhs variable. */
        final int frameSlot;

        /** Copy constructor from the assignment node and a frameslot Specification. */
        protected Local(UpdateArrayAssignment other, int frameSlot) {
            super(other);
            this.frameSlot = frameSlot;
        }

        @Override public Object execute(Frame frame) {
            try {
                if (frame == null) { // FIXME: this won't happen unless with eval
                    throw new UnexpectedResultException(Failure.NULL_FRAME);
                }
                if (frameSlot == -1) { // FIXME: this won't happen unless with eval
                    throw new UnexpectedResultException(Failure.NULL_SLOT);
                }
                RAny rhsValue = (RAny) rhs.execute(frame);
                RAny lhsValue = (RAny) frame.getObject(frameSlot);
                if (lhsValue == null) {
                    // TODO maybe turn this to decompile for smaller methods?
                    lhsValue = RFrameHeader.readViaWriteSetSlowPath(frame, lhs);
                    if (lhsValue == null) { throw RError.getUnknownVariable(getAST(), lhs); }
                    lhsValue.ref(); // reading from parent, hence need to copy on update
                    // ref once will make it shared unless it is stateless (like int sequence)
                }
                RAny newLhs = assignment.execute(frame, lhsValue, rhsValue);
                if (lhsValue != newLhs) {
                    RFrameHeader.writeAtRef(frame, frameSlot, newLhs);
                }
                return rhsValue;
            } catch (UnexpectedResultException e) {
                switch ((Failure) e.getResult()) {
                case NULL_FRAME:
                    return replace(new TopLevel(this)).execute(frame);
                case NULL_SLOT:
                    return replace(new UpdateArrayAssignment(this)).execute(frame);
                }
            }
            return null;
        }
    }

    protected static class ConstLocal extends Local {

        final RAny rhsVal;

        protected ConstLocal(UpdateArrayAssignment other, int slot, RAny rhs) {
            super(other, slot);
            rhsVal = rhs;
        }

        @Override public Object execute(Frame frame) {
            try {
                if (frame == null) { // FIXME: this won't happen unless with eval
                    throw new UnexpectedResultException(Failure.NULL_FRAME);
                }
                if (frameSlot == -1) { // FIXME: this won't happen unless with eval
                    throw new UnexpectedResultException(Failure.NULL_SLOT);
                }
                RAny lhsValue = (RAny) frame.getObject(frameSlot);
                if (lhsValue == null) {
                    // TODO maybe turn this to decompile for smaller methods?
                    lhsValue = RFrameHeader.readViaWriteSetSlowPath(frame, lhs);
                    if (lhsValue == null) { throw RError.getUnknownVariable(getAST(), lhs); }
                    lhsValue.ref(); // reading from parent, hence need to copy on update
                    // ref once will make it shared unless it is stateless (like int sequence)
                }
                RAny newLhs = assignment.execute(frame, lhsValue, rhsVal);
                if (lhsValue != newLhs) {
                    RFrameHeader.writeAtRef(frame, frameSlot, newLhs);
                }
                return rhsVal;
            } catch (UnexpectedResultException e) {
                switch ((Failure) e.getResult()) {
                case NULL_FRAME:
                    return replace(new TopLevel(this)).execute(frame);
                case NULL_SLOT:
                    return replace(new Const(this)).execute(frame);
                }
            }
            return null;
        }
    }

    protected static class TopLevel extends UpdateArrayAssignment {

        protected TopLevel(UpdateArrayAssignment other) {
            super(other);
        }

        @Override public Object execute(Frame frame) {
            try {
                if (frame != null) { // FIXME: this won't happen unless with eval
                    throw new UnexpectedResultException(null);
                }
                RAny rhsValue = (RAny) rhs.execute(frame);
                RAny lhsValue = lhs.getValue();
                if (lhsValue == null) { throw RError.getUnknownVariable(getAST(), lhs); }
                RAny newLhs = assignment.execute(frame, lhsValue, rhsValue);
                if (lhsValue != newLhs) {
                    RFrameHeader.writeToTopLevelRef(lhs, newLhs);
                }
                return rhsValue;
            } catch (UnexpectedResultException e) {
                // FIXME: if this could be reached, the rewrite could lead to unbounded rewriting
                return replace(new UpdateArrayAssignment(this)).execute(frame);
            }
        }
    }

    protected static class ConstTopLevel extends TopLevel {

        final RAny rhsVal;

        protected ConstTopLevel(UpdateArrayAssignment other, RAny rhs) {
            super(other);
            rhsVal = rhs;
        }

        @Override public Object execute(Frame frame) {
            try {
                if (frame != null) { // FIXME: this won't happen unless with eval
                    throw new UnexpectedResultException(null);
                }
                RAny lhsValue = lhs.getValue();
                if (lhsValue == null) { throw RError.getUnknownVariable(getAST(), lhs); }
                RAny newLhs = assignment.execute(frame, lhsValue, rhsVal);
                if (lhsValue != newLhs) {
                    RFrameHeader.writeToTopLevelRef(lhs, newLhs);
                }
                return rhsVal;
            } catch (UnexpectedResultException e) {
                // FIXME: if this could be reached, the rewrite could lead to unbounded rewriting
                return replace(new Const(this)).execute(frame);
            }
        }

    }

}
