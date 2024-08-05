package org.shinsean.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {
    private static final Interpreter interpreter = new Interpreter();
    static boolean hadError = false;
    static boolean hadRuntimeError = false;

    public static void main(String[] args) throws IOException {

        if (args.length > 1) {
            // If too many arguments are given, we exit with the exit status 64,
            // which specifies that the command was used incorrectly.
            System.out.println("Usage: jlox [script]");
            System.exit(64);
        } else if (args.length == 1) {
            // If you only pass a file path through terminal,
            // it'll run the file at the given path.
            runFile(args[0]);
        } else {
            // If no arguments are specified, the user will be prompted
            // with a helper interactive prompt to run a file.
            runPrompt();
        }
    }

    /**
     * This function runs a Lox file after being given the file's path.
     * @param path      a string that specifies that path to the desired file
     * @throws IOException
     */
    private static void runFile(String path) throws IOException {
        // This first creates a Path object from the String path, and then uses
        // "Files.readAllBytes" to read in all of the bytes in the file at the given path.
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        // A new String is then created by parsing the read-in bytes and decoding them
        // with the Java defaultCharset.
        // We then pass this into the run function.
        run(new String(bytes, Charset.defaultCharset()));

        // Indicate an error in the exit code.
        if (hadError) System.exit(65);
        if (hadRuntimeError) System.exit(70);
    }

    /**
     * This function starts an interactive prompt to allow code execution line-by-line.
     * @throws IOException
     */
    private static void runPrompt() throws IOException {
        // To my understanding, this creates a new terminal input stream.
        // This is functionally similar to using Scanner.
        InputStreamReader input = new InputStreamReader(System.in);
        // Uses a BufferedReader in conjunction with InputStreamReader.
        // My understanding is that InputStreamReader converts bytes into chars,
        // and BufferedReader then provides an efficient buffered stream of chars into the program.
        BufferedReader reader = new BufferedReader(input);

        // TODO: Add code that sets runTimeError to false after line execution similar to how hadError is set to false
        //  for consistency's sake (assuming that doesn't break anything).
        // This is basically an infinite while loop that breaks if
        // an empty line is entered.
        for (;;) {
            System.out.print("> ");
            // Reads a line from command line.
            String line = reader.readLine();
            if (line == null) break;
            run(line);
            hadError = false;
        }
    }

    /**
     * This function runs a Lox file after being passed in the raw code input.
     * @param source      a string that contains the raw Lox code
     * @throws IOException
     */
    private static void run(String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        Parser parser = new Parser(tokens);
        Expr expression = parser.parse();

        // Stop if there was a syntax error.
        if (hadError) return;

        interpreter.interpret(expression);

        // Code for printing out the AST.
        //System.out.println(new AstPrinter().print(expression));

        // Code for printing out the lexed tokens.
        //for (Token token : tokens) {
        //    System.out.println(token);
        //}

    }

    /**
     * Raises an error.
     * @param line      an int that indicates the line that caused an error
     * @param message   a String that is the error message
     */
    static void error(int line, String message) {
        report(line, "", message);
    }

    /**
     * Reports an error by printing to the console and sets the hadError flag to true.
     * @param line      an int that indicates the line that caused an error
     * @param where     a String that is the file that the error was raised in
     * @param message   a String that is the error message
     */
    private static void report(int line, String where, String message) {
        System.err.println("[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }

    // TODO: Move this method above report().
    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }

    static void runtimeError(RuntimeError error) {
        System.err.println(error.getMessage() +
                "\n[line " + error.token.line + "]");
        hadRuntimeError = true;
    }

}
