package org.shinsean.lox;

import java.util.List;
import java.util.Map;

public class LoxClass implements LoxCallable {
    final String name;
    final LoxClass superclass;
    private final Map<String, LoxFunction> methods;

    LoxClass(String name, LoxClass superclass, Map<String, LoxFunction> methods) {
        this.name = name;
        this.superclass = superclass;
        this.methods = methods;
    }

    LoxFunction findMethod(String name) {
        // Check if the class has the requested method.
        // If so, return it.
        if (methods.containsKey(name)) {
            return methods.get(name);
        }

        // If the class did not have the requested method directly, check its superclass.
        // If the superclass has it, return it. If it does not, it will call the
        // superclass's superclass and recursively check until it finds it or fails to and returns null.
        if (superclass != null) {
            return superclass.findMethod(name);
        }

        return null;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        LoxInstance instance = new LoxInstance(this);

        LoxFunction initializer = findMethod("init");
        if (initializer != null) {
            initializer.bind(instance).call(interpreter, arguments);
        }

        return instance;
    }

    @Override
    public int arity() {
        LoxFunction initializer = findMethod("init");
        if (initializer == null) return 0;
        return initializer.arity();
    }

    @Override
    public String toString() {
        return name;
    }
}
