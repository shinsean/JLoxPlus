package org.shinsean.lox;

import java.util.List;

public class LoxFunction implements LoxCallable {
    private final Stmt.Function declaration;

    LoxFunction(Stmt.Function declaration) {
        this.declaration = declaration;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment environment = new Environment(interpreter.globals);
        for (int i = 0; i < declaration.params.size(); i++) {
            // Get the name of the function scope env variable from the parameter names
            // from the function declaration, and get the corresponding values from the
            // passed-in argument variables. This is safe to do because visitCallExpr
            // checks arity before calling "call()".
            environment.define(declaration.params.get(i).lexeme, arguments.get(i));
        }

        // Utilizing the Return runtime exception to handle returning and control flow.
        // If there is an explicit return value, the catch block is guaranteed to trigger
        // with returnValue containing the desired return value. If there is no explicit
        // return value, the catch block will not trigger, leading to the "return null;" to trigger.
        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch (Return returnValue) {
            return returnValue.value;
        }
        return null;
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public String toString() {
        return "<fn " + declaration.name.lexeme + ">";
    }
}
