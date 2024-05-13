package org.example;

import polyglot.ast.Do;

public class Parser {
    public enum ValueType {
        VAR,
        STRING
    }
    public class MustEqualTyped {
        public String assigneeClass;
        public String assigneeSubMethodSig;
        public String assignee;
        public String assignorClass;
        public String assignorSubMethodSig;
        public String assignor;
        ValueType assignorType;
    }

    private class Variable {
        public String className;
        public String subMethodSig;
        public String varName;
    }

    public MustEqualTyped parseMustEqualTyped(String line) throws DootException {
        MustEqualTyped mustEqualTyped = new MustEqualTyped();

        String[] elements = line.split("\t");
        if (elements.length != 3) {
            throw new DootException("Bad input for parsing MustEqualTyped");
        }

        Variable assignee = parseVariable(elements[1]);
        mustEqualTyped.assigneeClass = assignee.className;
        mustEqualTyped.assigneeSubMethodSig = assignee.subMethodSig;
        mustEqualTyped.assignee = assignee.varName;

        switch (elements[2]) {
            case "STRING":
                mustEqualTyped.assignorClass = assignee.className;
                mustEqualTyped.assignorSubMethodSig = assignee.subMethodSig;
                mustEqualTyped.assignor = elements[0];
                mustEqualTyped.assignorType = ValueType.STRING;
                break;
            case "VAR":
                Variable assignor = parseVariable(elements[0]);
                mustEqualTyped.assignorClass = assignor.className;
                mustEqualTyped.assignorSubMethodSig = assignor.subMethodSig;
                mustEqualTyped.assignor = assignor.varName;
                mustEqualTyped.assignorType = ValueType.VAR;
                break;
            default:
                throw new DootException("Undefined assignorType");
        }

        return mustEqualTyped;
    }

    private Variable parseVariable(String doopVariable) throws DootException {
        Variable variable = new Variable();

        String[] elements = doopVariable.split("/");
        if (elements.length != 2) {
            throw new DootException("Bad input for parsing Variable");
        }

        variable.varName = elements[1];

        String[] sigElements = elements[0].substring(1, elements[0].length()-1).split(":");
        if (sigElements.length != 2) {
            throw new DootException("Error parsing method signature");
        }

        variable.className = sigElements[0];
        variable.subMethodSig = sigElements[1].trim();

        return variable;
    }
}
