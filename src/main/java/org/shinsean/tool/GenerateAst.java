package org.shinsean.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

// A class for automatically generating syntax tree classes for the parser.
public class GenerateAst {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: generate_ast <output directory>");
            System.exit(64);
        }

        // TODO: Organize the syntax class declaration order by their order in the grammar.

        String outputDir = args[0];
        defineAst(outputDir, "Expr", Arrays.asList(
                "Assign     : Token name, Expr value",
                "Binary     : Expr left, Token operator, Expr right",
                "Call       : Expr callee, Token paren, List<Expr> arguments",
                "Get        : Expr object, Token name",
                "Grouping   : Expr expression",
                "Literal    : Object value",
                "Logical    : Expr left, Token operator, Expr right",
                "Super      : Token keyword, Token method",
                "This       : Token keyword",
                "Set        : Expr object, Token name, Expr value",
                "Unary      : Token operator, Expr right",
                "Variable   : Token name"
        ));

        // TODO: Add support for else-if statements.
        defineAst(outputDir, "Stmt", Arrays.asList(
                "Block      : List<Stmt> statements",
                // TODO: I'm pretty sure the string concats here aren't necessary.
                //  I think they're only there in the reference impl because of condensing concerns.
                "Class      : Token name, Expr.Variable superclass," + " List<Stmt.Function> methods",
                "Expression : Expr expression",
                "Function   : Token name, List<Token> params," + " List<Stmt> body",
                "If         : Expr condition, Stmt thenBranch," + " Stmt elseBranch",
                "Print      : Expr expression",
                "Return     : Token keyword, Expr value",
                "Var        : Token name, Expr initializer",
                "While      : Expr condition, Stmt body"
        ));
    }

    private static void defineAst(
            String outputDir, String baseName, List<String> types)
            throws IOException {
        String path = outputDir + '/' + baseName + ".java";
        PrintWriter writer = new PrintWriter(path, "UTF-8");

        writer.println("package org.shinsean.lox;");
        writer.println();
        writer.println("import java.util.List;");
        writer.println();
        writer.println("abstract class " + baseName + " {");

        defineVisitor(writer, baseName, types);

        // The AST classes.
        for (String type : types) {
            // TODO: Save the type.split output as a single variable and reuse it.
            String className = type.split(":")[0].trim();
            String fields = type.split(":")[1].trim();
            defineType(writer, baseName, className, fields);
        }

        // The base accept() method.
        writer.println();
        // TODO: Move this so that it is written above the nested static classes.
        writer.println("    abstract <R> R accept(Visitor<R> visitor);");

        writer.println("}");
        writer.close();
    }

    private static void defineVisitor(
            PrintWriter writer, String baseName, List<String> types) {
        writer.println("    interface Visitor<R> {");

        for (String type : types) {
            String typeName = type.split(":")[0].trim();
            writer.println("        R visit" + typeName + baseName + "(" +
                    typeName + " " + baseName.toLowerCase() + ");");
        }

        writer.println("    }");
    }

    private static void defineType(
            PrintWriter writer, String baseName,
            String className, String fieldList) {
        writer.println("    static class " + className + " extends " + baseName + " {");

        // constructor.
        writer.println("        " + className + "(" + fieldList + ") {");

        // Store parameters in fields.
        String[] fields = fieldList.split(", ");
        for (String field : fields) {
            String name = field.split(" ")[1];
            writer.println("            this." + name + " = " + name + ";");
        }

        writer.println("        }");

        // Visitor pattern.
        writer.println();
        writer.println("        @Override");
        writer.println("        <R> R accept(Visitor<R> visitor) {");
        writer.println("            return visitor.visit" +
                className + baseName + "(this);");
        writer.println("        }");

        // TODO: Move the initial variable definitions to be above the class constructor.
        // Fields.
        writer.println();
        for (String field : fields) {
            writer.println("        final " + field + ";");
        }

        writer.println("    }");
        // TODO: Enable this later to make the output Expr.java look a lot cleaner.
        // writer.println();
    }
}
