package org.shinsean.lox;

import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO: Change from static import to regular import and insert "TokenType."
//  in front of all variables used from the TokenType enum.
// Do this after the language is completed.
import static org.shinsean.lox.TokenType.*;

public class Scanner {
    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("and", AND);
        keywords.put("class", CLASS);
        keywords.put("else", ELSE);
        keywords.put("false", FALSE);
        keywords.put("for", FOR);
        keywords.put("fun", FUN);
        keywords.put("if", IF);
        keywords.put("nil", NIL);
        keywords.put("or", OR);
        keywords.put("print", PRINT);
        keywords.put("return", RETURN);
        keywords.put("super", SUPER);
        keywords.put("this", THIS);
        keywords.put("true", TRUE);
        keywords.put("var", VAR);
        keywords.put("while", WHILE);
    }

    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;

    // Not sure if I can add a "public" keyword here and in front of scanTokens
    // My understanding is that if I don't add a scoping keyword, it is package-private
    // by default, meaning it is public but only to classes in its package.
    Scanner(String source) {
        this.source = source;
    }

    List<Token> scanTokens() {
        while (!isAtEnd()) {
            // We are at the beginning of the next lexeme.
            start = current;
            scanToken();
        }

        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    private void scanToken() {
        // Note: In YAJLox, we discard all comment, whitespace, and newline lexemes.
        // If we were making a transpiler and wanted to keep these, I think we could just
        // add them as tokens but just don't do anything with them except write them as they are
        // when parsing and compiling.

        char c = advance();
        switch (c) {
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case '-': addToken(MINUS); break;
            case '+': addToken(PLUS); break;
            case ';': addToken(SEMICOLON); break;
            case '*': addToken(STAR); break;
            case '!':
                addToken(match('=') ? BANG_EQUAL : BANG);
                break;
            case '=':
                addToken(match('=') ? EQUAL_EQUAL : EQUAL);
                break;
            case '<':
                addToken(match('=') ? LESS_EQUAL : LESS);
                break;
            case '>':
                addToken(match('=') ? GREATER_EQUAL : GREATER);
                break;
            // TODO: Add support for multi-line /* ... */ comments.
            case '/':
                if (match('/')) {
                    // A comment goes until the end of the line.
                    // Essentially, this while loop checks if the current unconsumed character
                    // (or next character, depending on viewpoint) is either
                    // the end of line character or if the current unconsumed character has hit the end
                    // of the source code. If not, it keeps advancing the current int by 1.
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else {
                    addToken(SLASH);
                }
                break;
            // I'm gonna need a Java refresher, but my understanding is that
            // since there are no break cases for these, whenever it hits the ' ' or '\r' case,
            // it just drops down and keeps executing whatever code is written in the cases below
            // until it hits a break. This works out fine for us since we intend this behavior.
            case ' ':
            case '\r':
            case '\t':
                // Ignore whitespace.
                break;

            case '\n':
                line++;
                break;

            case '"': string(); break;

            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    Lox.error(line, "Unexpected character.");
                    break;
                }
            // Note: YAJLox will continue lexing after an error raise, but will
            // not execute code if it detects at least one error.
        }
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) advance();

        // String.substring not being end-inclusive works in our favor here
        // since current should always be an unconsumed char
        String text = source.substring(start, current);
        TokenType type = keywords.get(text);
        if (type == null) type = IDENTIFIER;
        addToken(type);
    }

    private void number() {
        while (isDigit(peek())) advance();

        // Look for a fractional part.
        if (peek() == '.' && isDigit(peekNext())) {
            // Consume the "." and increment current.
            advance();
        }

        while (isDigit(peek())) advance();

        // We use Java's built-in string to double conversion to handle this.
        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++;
            advance();
        }

        if (isAtEnd()) {
            Lox.error(line, "Unterminated string.");
            return;
        }

        // Make sure to consume the closing " char and increment current
        // for the next loop.
        advance();

        // Trim the surrounding double quotes since those aren't actually part
        // of the string itself.
        // TODO: To support escape sequences like "\n", we would unescape them here.
        //  Not sure what that would actually look like as of this moment in time.
        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }

    private boolean match(char expected) {
        // TODO: Restructure these conditionals to make it more clear on which cases an incrementation happens.
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        current++;
        return true;
    }

    /**
     * A function that conducts a 1-character lookahead in the current line being lexed.
     * @return  a char that is either the end of code char ('\0') or the current unconsumed char
     */
    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    /**
     * A function that conducts a 2-character lookahead in the current line being lexed.
     * @return  a char that is either the end of code char ('\0') or the next unconsumed char after
     *      the current unconsumed char
     */
    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    /**
     * A function that returns whether a character is between a - z, between A - Z, or is an underscore.
     * @param c     a char to be checked to see if it is an alphabet character
     * @return  true if a char is between a - z, between A - Z, or is an underscore and false otherwise
     */
    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
               (c >= 'A' && c <= 'Z') ||
               (c == '_');
    }

    /**
     * A function that returns whether a character is between a - z, between A - Z,
     * is an underscore, or is a digit.
     * @param c     a char to be checked to see if it is an alphanumeric character
     * @return  true if a char is between a - z, between A - Z, is an underscore, or is a digit and false otherwise
     */
    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private boolean isDigit(char c) {
        // We use this instead of Character.isDigit() as Character.isDigit() also counts as a digit
        // things like Devanagari digits, full-width numbers, and other miscellaneous non-Arabic numbers.
        return c >= '0' && c <= '9';
    }

    /**
     * A helper function that checks if the scanner has reached the end of the source code.
     * @return  a boolean that is true if the scanner has reached the end of the
     *      source code and false otherwise
     */
    private boolean isAtEnd() {
        return current >= source.length();
    }

    /**
     * Returns the char in the source code at the position current and increments current by 1.
     * @return  a char that is the character in the source code at the position current
     */
    private char advance() {
        // Note: This returns the character at current first, and then increments
        // current by 1 and saves that to current.
        return source.charAt(current++);
    }

    /**
     * Takes a TokenType constant and adds the current token to the tokens ArrayList.
     * Meant for adding tokens without literal values.
     * @param type      a TokenType constant
     */
    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        // Creates a substring of the source code from the start to current indices, not end-inclusive.
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }
}
