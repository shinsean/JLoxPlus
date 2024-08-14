# JLoxPlus
JLoxPlus, an interpreter written in Java implementing a superset of the Lox language specifications. This implementation initially drew from the reference Nystrom JLox implementation.

The bulk of the core logic is in the src/main/java/org/shinsean/lox package.

## Usage
The interpreter can either be executed live (by running the Lox.java file) or compiled and executed.

When executing a lox file using the interpreter, call/execute the interpreter and either pass in no command line args (which runs JLoxPlus in an interactive prompt mode) or pass in a filepath to a lox file as the first command line arg.