package org.shinsean.lox;

public class Return extends RuntimeException {
    final Object value;

    Return(Object value) {
        // Since this is a recursive treewalking interpreter, we utilize
        // RuntimeException with its stack trace and other features stripped off
        // as a control flow tool.
        super(null, null, false, false);
        this.value = value;
    }
}
