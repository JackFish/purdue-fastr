package r.nodes;

@Precedence(Operation.NOT_PRECEDENCE)
@PrettyName("!")
public class Not extends UnaryOperation {

    public Not(ASTNode operand) {
        super(operand);
    }

    @Override
    public void accept(Visitor v) {
        v.visit(this);
    }
}
