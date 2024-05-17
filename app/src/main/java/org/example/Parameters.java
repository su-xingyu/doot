package org.example;

import com.sun.istack.NotNull;
import com.sun.xml.ws.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Parameters {
    public String _example = "";
    public String _mainClass = "";
    public String _outputFormat = "class";
    public boolean _skipDoop = false;   // Only for test purpose
    public Path _doopDir = null;
    public boolean _optimize = false;
    public boolean _inline = false;
    public boolean _keepTmpDir = false;

    private static int shift(@NotNull String[] args, int index) throws DootException {
        if (args.length == index + 1) {
            throw new DootException("Option " + args[index] + " requires an argument");
        }

        return index + 1;
    }

    public void initFromArgs(@NotNull String[] args) throws DootException {
        processArgs(args);
        finishArgsProcessing();
    }

    private void finishArgsProcessing() throws DootException {
        if (_example.isEmpty() || _mainClass.isEmpty()) {
            throw new DootException("Please specify the name of example");
        }

        if (_doopDir == null || !Files.exists(_doopDir)) {
            throw new DootException("Please specify valid Doop directory");
        }

        if (!_inline && !_optimize) {
            throw new DootException("Please specify at least one operation from inline and optimization");
        }
    }

    private void processArgs(@NotNull String[] args) throws DootException {
        int i = 0, last_i;
        while (i < args.length) {
            last_i = processNextArg(args, i);
            if (last_i == -1) {
                throw new DootException("Bad argument: " + args[i]);
            }

            i = last_i + 1;
        }
    }

    private int processNextArg(@NotNull String[] args, int i) throws DootException {
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
            case "--skip-doop":
                _skipDoop = true;
                break;
            case "--doop-dir":
                i = shift(args, i);
                _doopDir = Paths.get(args[i]);
                break;
            case "--optimize":
                _optimize = true;
                break;
            case "--inline":
                _inline = true;
                break;
            case "--keep-tmp-dir":
                _keepTmpDir = true;
                break;
            default:
                return -1;
        }

        return i;
    }

}
