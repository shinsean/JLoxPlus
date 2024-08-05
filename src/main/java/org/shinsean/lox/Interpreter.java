package org.shinsean.lox;

class Interpreter implements Expr.Visitor<Object> {

    // TODO: Reorder these methods to line up with the ordering in Expr.java

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        // Since a grouping node is basically just a container containing
        // another node inside of it, we just pass in the contained node
        // to evaluate to run it again recursively through the interpreter.
        return evaluate(expr.expression);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        // TODO: Test this theory later.
        // I'm not sure how we can use the TokenType enum values here like this without
        // having statically imported. Maybe it's because when tokens were created
        // by Scanner.java (which does import TokenTypes statically), the value was stored then and so
        // even if we don't import TokenTypes again, the case statements don't care since they are just
        // comparing against whatever was already stored in the tokens?
        switch (expr.operator.type) {
            case MINUS:
                return -(double)right;
            case BANG:
                return !isTruthy(right);
        }

        // Unreachable, since the parser would not have classed something as a unary unless
        // there was a negation boolean or negation number operator.
        return null;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        // Note: Evaluating the left operand first before the right operand is an intentional choice
        // here as we want to remain consistent with our design rule of having left-to-right precedence.
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG_EQUAL:
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);
            case GREATER:
                return (double)left > (double)right;
            case GREATER_EQUAL:
                return (double)left >= (double)right;
            case LESS:
                return (double)left < (double)right;
            case LESS_EQUAL:
                return (double)left <= (double)right;
            case MINUS:
                return (double)left - (double)right;
            case PLUS:
                // Situations like this where an operator like "+" does different things depending on the
                // type of the operands is why it's nice to have a function that can report types.
                if (left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right;
                }

                if (left instanceof String && right instanceof String) {
                    return (String)left + (String)right;
                }
            case SLASH:
                return (double)left / (double)right;
            case STAR:
                return (double)left * (double)right;
        }

        // Unreachable in theory for similar reasons noted in visitUnaryExpr().
        return null;
    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean)object;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        // Note: NaN == NaN in JLox will resolve to true, which is logical but does
        // not align with IEEE 754.

        // We first handle cases where a might be null, because if we called
        // "a.equals()" when a is null, that would throw an error in Java.
        if (a == null && b == null) return true;
        if (a == null) return false;

        return a.equals(b);
    }

    /**
     * As several AST expr nodes (built from our expr classes) can have other AST expr nodes
     * nested within them in the tree, this helper method allows the interpreter to handle node
     * nesting by recursively passing it to evaluate, which then runs the now un-nested expr
     * node again through the interpreter with the visit pattern.
     * @param expr      an AST expr node
     * @return  the value from running expr through an Interpreter object again
     */
    private Object evaluate(Expr expr) {
        // Can we use "this" when we never defined a constructor?
        return expr.accept(this);
    }
}
