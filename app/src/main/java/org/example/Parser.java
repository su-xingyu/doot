package org.example;

import com.sun.istack.NotNull;

public class Parser {
    @NotNull
    public ValuePairToOptimizeTyped parseValuePairToOptimizeTyped(@NotNull String line) throws DootException {
        ValuePairToOptimizeTyped valuePairToOptimizeTyped = new ValuePairToOptimizeTyped();

        String[] elements = line.split("\t");
        if (elements.length != 3) {
            throw new DootException("Bad input for parsing ValuePairToOptimizeTyped");
        }

        DoopValue assignor = parseDoopValue(elements[0]);
        valuePairToOptimizeTyped.assignorClass = assignor.className;
        valuePairToOptimizeTyped.assignorSubMethodSig = assignor.subMethodSig;
        valuePairToOptimizeTyped.assignor = assignor.value;

        switch (elements[2]) {
            case "STRING":
                valuePairToOptimizeTyped.assignorType = ValueType.STRING;
                break;
            case "VAR":
                valuePairToOptimizeTyped.assignorType = ValueType.VAR;
                break;
            default:
                throw new DootException("Undefined assignorType");
        }

        DoopValue assignee = parseDoopValue(elements[1]);
        valuePairToOptimizeTyped.assigneeClass = assignee.className;
        valuePairToOptimizeTyped.assigneeSubMethodSig = assignee.subMethodSig;
        valuePairToOptimizeTyped.assignee = assignee.value;

        return valuePairToOptimizeTyped;
    }

    @NotNull
    private DoopValue parseDoopValue(@NotNull String doopVariable) throws DootException {
        DoopValue variable = new DoopValue();

        String[] elements = doopVariable.split("/");
        if (elements.length != 2) {
            throw new DootException("Bad input for parsing Variable");
        }

        variable.value = elements[1];

        String[] sigElements = elements[0].substring(1, elements[0].length() - 1).split(":");
        if (sigElements.length != 2) {
            throw new DootException("Error parsing method signature");
        }

        variable.className = sigElements[0];
        variable.subMethodSig = sigElements[1].trim();

        return variable;
    }

    public enum ValueType {
        VAR,
        STRING
    }

    public static class ValuePairToOptimizeTyped {
        public String assigneeClass;
        public String assigneeSubMethodSig;
        public String assignee;
        public String assignorClass;
        public String assignorSubMethodSig;
        public String assignor;
        ValueType assignorType;
    }

    private static class DoopValue {
        public String className;
        public String subMethodSig;
        public String value;
    }
}
