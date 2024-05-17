package org.example;

import com.sun.istack.NotNull;
import com.sun.istack.Nullable;
import org.apache.log4j.Logger;
import soot.Body;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.toolkits.invoke.SiteInliner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Inliner {
    private static final Logger logger = Logger.getLogger(Inliner.class);

    private final Path doopResultDir;

    public Inliner(Path doopResultDir) {
        this.doopResultDir = doopResultDir;
    }

    public void inline() throws IOException, DootException {
        BufferedReader bufferedReader = new BufferedReader(
                new FileReader(doopResultDir + File.separator + "MethodPairToInline.csv"));

        String line = bufferedReader.readLine();
        while (line != null) {
            // Since we only consider inline using 1-call-site-sensitivity+heap analysis, the order of inlining doesn't
            // matter
            inlineOnce(line);
            line = bufferedReader.readLine();
        }
    }

    private void inlineOnce(@NotNull String line) throws DootException {
        logger.debug("Inlining method pair: " + line);
        String[] methodPair = line.split("\t");
        if (methodPair.length != 2) {
            throw new DootException("Bad input from inline analysis result");
        }

        inlineMethodPair(methodPair[0], methodPair[1]);
    }

    private void inlineMethodPair(@NotNull String caller, @Nullable String callee) {
        SootMethod callerMethod = Scene.v().getMethod(caller);
        SootMethod calleeMethod = Scene.v().getMethod(callee);

        if (!callerMethod.hasActiveBody()) {
            callerMethod.retrieveActiveBody();
        }
        if (!calleeMethod.hasActiveBody()) {
            calleeMethod.retrieveActiveBody();
        }

        // We cannot inline while traversing each unit. This will lead to concurrency issue. Need to store the
        // statements to be lined first
        List<Stmt> stmtToInline = new ArrayList<>();

        Body callerBody = callerMethod.getActiveBody();
        for (Unit unit : callerBody.getUnits()) {
            Stmt s = (Stmt) unit;
            if (s.containsInvokeExpr()) {
                InvokeExpr invS = s.getInvokeExpr();
                if (Objects.equals(invS.getMethod().toString(), callee)) {
                    stmtToInline.add(s);
                }
            }
        }

        for (Stmt s : stmtToInline) {
            SiteInliner.inlineSite(calleeMethod, s, callerMethod);
        }
    }
}
