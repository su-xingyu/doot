package org.example;

import com.sun.xml.ws.util.StringUtils;

public class Parameters {
    public String _example = "";
    public String _mainClass = "";
    public String _outputFormat = "class";

    public void initFromArgs(String[] args) throws DootException {
        processArgs(args);
        finishArgsProcessing();
    }

    private void finishArgsProcessing() throws DootException {
        if (_example.isEmpty() || _mainClass.isEmpty()) {
            throw new DootException("Please specify the name of example");
        }
    }

    private void processArgs(String[] args) throws DootException {
        int i = 0, last_i;
        while (i < args.length) {
            last_i = processNextArg(args, i);
            if (last_i == -1) {
                throw new DootException("Bad argument: " + args[i]);
            }

            i = last_i + 1;
        }
    }

    private int processNextArg(String[] args, int i) throws DootException {
        switch (args[i]) {
            case "-e":
                i = shift(args, i);
                _example = args[i];
                _mainClass = StringUtils.capitalize(args[i]);
                break;
            case "-o":
                i = shift(args, i);
                _outputFormat = args[i];
                break;
            default:
                return -1;
        }

        return i;
    }

    private static int shift(String[] args, int index) throws DootException {
        if (args.length == index + 1) {
            throw new DootException("Option " + args[index] + " requires an argument");
        }

        return index + 1;
    }


}
