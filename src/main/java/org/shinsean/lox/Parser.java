package org.shinsean.lox;

import java.util.List;

// TODO: Determine whether we're going to keep statically importing this or not.
import static org.shinsean.lox.TokenType.*;

class Parser {
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    Expr parse() {
        try {
            return expression();
        } catch (ParseError error) {
            return null;
        }
    }

    private Expr expression() {
        return equality();
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

        return primary();
    }

    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

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


}