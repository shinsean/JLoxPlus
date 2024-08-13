package org.shinsean.lox;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

// TODO: Determine whether we're going to keep statically importing this or not.
import static org.shinsean.lox.TokenType.*;

class Parser {
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }

        return statements;
    }

    private Stmt declaration() {
        try {
            if (match(VAR)) return varDeclaration();
            if (match(FUN)) return function("function");
            if (match(CLASS)) return classDeclaration();

            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt classDeclaration() {
        Token name = consume(IDENTIFIER, "Expect class name.");
        consume(LEFT_BRACE, "Expect '{' before class body.");

        List<Stmt.Function> methods = new ArrayList<>();
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            // After entering the block statement by consuming the "{",
            // we call function to handle the parsing of each of the functions.
            methods.add(function("method"));
        }

        consume(RIGHT_BRACE, "Expect '}' after class body.");

        return new Stmt.Class(name, methods);
    }

    private Stmt statement() {
        if (match(FOR)) return forStatement();
        if (match(IF)) return ifStatement();
        if (match(PRINT)) return printStatement();
        if (match(RETURN)) return returnStatement();
        if (match(WHILE)) return whileStatement();
        if (match(LEFT_BRACE)) return new Stmt.Block(block());

        return expressionStatement();
    }

    private Stmt forStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'for'.");

        Stmt initializer;
        if (match(SEMICOLON)) {
            initializer = null;
        } else if (match(VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        Expr condition = null;
        if (!check(SEMICOLON)) {
            condition = expression();
        }
        consume(SEMICOLON, "Expect ';' after loop condition.");

        Expr increment = null;
        if (!check(RIGHT_PAREN)) {
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expect ')' after for clauses.");

        Stmt body = statement();

        /* We will use desugaring/caramelizing for loops into while loops to actually implement them.
        Functionally, the code "for (var i = 0; i < 10; i = i + 1) print i;" is equivalent to
        "{
            var i = 0
            while (i < 10) {
                print i;
                i = i + 1
            }
        }"
        So, whenever we have a for loop, we essentially turn it into a block statement that has been structured like
        the while loop. Using this, we roll up the body and the increment into a block statement, put that as the statement
        in a while loop rolled together with the condition, and roll the initializer and the while loop in a block statement.
        */

        if (increment != null) {
            body = new Stmt.Block(
                    Arrays.asList(
                            body,
                            new Stmt.Expression(increment)
                    )
            );
        }

        if (condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);

        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }

        return body;
    }

    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Expect '(', after 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after if condition.");

        Stmt thenBranch = statement();
        // Else statements are bound to nearest if statement.
        Stmt elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ':' after value.");
        return new Stmt.Print(value);
    }

    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;
        if (!check(SEMICOLON)) {
            value = expression();
        }

        consume(SEMICOLON, "Expect ';' after return value.");
        return new Stmt.Return(keyword, value);
    }

    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expect variable name.");

        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }

        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    private Stmt whileStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after condition.");
        Stmt body = statement();

        return new Stmt.While(condition, body);
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    // TODO: Rename the String kind parameter to String type or String funcType.
    private Stmt.Function function(String kind) {
        Token name = consume(IDENTIFIER, "Expect " + kind + " name.");
        consume(LEFT_PAREN, "Expect '(' after " + kind + " name.");
        List<Token> parameters = new ArrayList<>();

        if (!check(RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) {
                    // We intentionally don't throw the error for similar reasons we didn't
                    // throw the error in other places.
                    error(peek(), "Can't have more than 255 parameters.");
                }

                // Note: This being an IDENTIFIER instead of any of the literal types is intentional.
                // As this is a function declaration, you want your parameters to be named, because
                // you wouldn't be able to do anything with them otherwise. Function calls can use literals.
                parameters.add(consume(IDENTIFIER, "Expect parameter name."));
            } while (match(COMMA));
        }
        consume(RIGHT_PAREN, "Expect ')' after parameters.");

        // Note: This does mean that all function declarations must use block statements instead of just
        // any statement for the body trailing after the name and parameter declaration.
        // Consider changing this to a more general statement body. Though, that does have pros and cons.
        consume(LEFT_BRACE, "Expect '{' before " + kind + "body.");
        List<Stmt> body = block();

        return new Stmt.Function(name, parameters, body);
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        // The way this function works is that no matter what, the current token is
        // sent down the parsing chain and parsed fully.
        // Then, we check if the next token is an equal sign.
        // If so, we essentially assume that whatever was to the left of the equal sign
        // was a valid assignment target (e.g. variable expression, field expression).
        // Though, since we want to gracefully control the errors thrown, even
        // though we "assume" it, we still check to make sure that the expression on the
        // right was a variable expression. If it was valid, we then create a new
        // variable name to hold on to the name of the variable expression and then create a new
        // assignment expression node with it, discarding the variable expression node.
        // Then, in the interpreter, we can treat variable expression nodes and variable
        // assignment nodes differently.
        // However, if the next sign was not an equal sign, we just return whatever we evaluated.
        Expr expr = or();

        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            } else if (expr instanceof Expr.Get) {
                Expr.Get get = (Expr.Get)expr;
                return new Expr.Set(get.object, get.name, value);
            }

            // While we report the error if the left-hand side is not a valid assignment target,
            // since we can still keep parsing, we don't throw the error and enter panic mode to synchronize.
            error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    private Expr or() {
        Expr expr = and();

        while (match(OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr and() {
        Expr expr = equality();

        while(match(AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    // TODO: Create a helper method for handling the parsing of left-associative
    //  binary operators given a list of token types and an operand method.
    private Expr equality() {
        // Note: This explanatory note could go under any of these parsing functions,
        // but I'll put it here because why not.
        // Essentially, no matter what token is at current, expression gets called.
        // Then, no matter what token is at current, comparison() called. Only then does the parser
        // check if the next unconsumed token will make this a fully fledged equality. If so, it keeps building
        // the equality node until loop termination. However, if the next unconsumed token does not make
        // this a fully fledged equality, then the equality() function call functionally only serves to call comparison().
        // This pattern goes all the way up until literals, where they just return the literal value.
        // This is what Nystrom meant when he said that a rule can match anything at its precedent level or higher.
        // That's pretty cool.
        Expr expr = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            // We call previous() for operator because match() consumes
            // the currently unconsumed token.
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr comparison() {
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr term() {
        Expr expr = factor();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr factor() {
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return call();
    }

    private Expr call() {
        Expr expr = primary();

        while (true) {
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr);
            } else if (match(DOT)) {
                Token name = consume(IDENTIFIER,
                        "Expect property name after '.'.");
                expr = new Expr.Get(expr, name);
            } else {
                break;
            }
        }

        return expr;
    }

    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (arguments.size() >= 255) {
                    // We intentionally do not throw the error as throwing the error kicks
                    // the parser into panic mode to start recovery and sync, which we do
                    // need for this error. We can continue along, just having flagged this as an error.
                    error(peek(), "Can't have more than 255 arguments.");
                }
                arguments.add(expression());
            } while (match(COMMA));
        }

        Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments.");

        return new Expr.Call(callee, paren, arguments);
    }

    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        if (match(THIS)) return new Expr.This(previous());

        throw error(peek(), "Expect expression.");
    }

    /**
     * Checks if the currently unconsumed token matches the token types passed in as arguments.
     * If so, call advance(), which consumes the current token and sets current to point to the next
     * unconsumed token by incrementing it by 1. Then return true.
     * @param types     The TokenTypes to check if the currently unconsumed token matches
     * @return  true if there is a match between the type of the currently unconsumed token
     *  and the passed-in token types; false otherwise
     */
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    /**
     * Consumes the currently unconsumed token and increments counter if the currently unconsumed token
     * matches the TokenType of the parameter type
     * @param type      A TokenType to compare the type of the currently unconsumed token
     * @param message   A String that is the error message if the TokenType of type and the
     *                      currently unconsumed token do not match.
     * @return
     */
    private Token consume(TokenType type, String message) {
        // TODO: Consider swapping this to "check(type) == true"
        if (check(type)) return advance();

        throw error(peek(), message);
    }

    /**
     * Checks if the currently unconsumed token matches the given token type.
     * Does not consume the currently unconsumed token.
     * @param type      The TokenType that is checked against the type of the
     *                      currently unconsumed token.
     * @return  true if the TokenType of the argument and the currently unconsumed token
     *  match; false otherwise.
     */
    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    /**
     * Consumes the currently unconsumed token by returning it and then increments current by 1,
     * pointing it to the next unconsumed token unless you have reached the end of the sequence.
     * If end-of-sequence is reached, always return the last token.
     * @return  a Token that is the current unconsumed token
     */
    private Token advance() {
        // TODO: Rework this if statement to be more immediately clear in meaning.
        // Just to clarify, this piece of code increments current by 1 and then returns
        // the previous token, which was/is the "current" unconsumed token.
        if (!isAtEnd()) current++;
        return previous();
    }

    /**
     * Returns if current has reached the end of the token sequence.
     * @return  true if the currently unconsumed token is EOF; false otherwise
     */
    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    /**
     * Returns the current unconsumed token without consuming it.
     * @return      a Token that is the current unconsumed token
     */
    private Token peek() {
        return tokens.get(current);
    }

    /**
     * Returns the token before the currently unconsumed token without consuming it.
     * @return      a Token that is token before the current unconsumed token
     */
    private Token previous() {
        return tokens.get(current - 1);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            // If the currently being consumed token is a semicolon, we break out.
            if (previous().type == SEMICOLON) return;

            // If the current unconsumed token is the start of a statement, we break out.
            switch (peek().type) {
                // TODO: Replace intentional fall-through with actual statements.
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }

            advance();
        }
    }
}
