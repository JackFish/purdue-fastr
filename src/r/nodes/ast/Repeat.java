package r.nodes.ast;


public class Repeat extends Loop {
    public Repeat(ASTNode body) {
        super(body);
    }

    @Override
    public void accept(Visitor v) {
        v.visit(this);
    }
}
