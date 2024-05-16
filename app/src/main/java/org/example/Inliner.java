package org.example;

import soot.*;
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
    private final Path doopDir;

    public Inliner(Path doopDir) {
        this.doopDir = doopDir;
    }

    public void inline() throws IOException, DootException {
        BufferedReader bufferedReader = new BufferedReader(
                new FileReader(doopDir + File.separator + "last-analysis/MethodPairToInline.csv"));

        String line = bufferedReader.readLine();
        while (line != null) {
            // Since we only consider inline using 1-call-site-sensitivity+heap analysis, the order of inlining doesn't
            // matter
            inlineOnce(line);
            line = bufferedReader.readLine();
        }
    }

    private void inlineOnce(String line) throws DootException {
        String[] methodPair = line.split("\t");
        if (methodPair.length != 2) {
            throw new DootException("Bad input from inline analysis result");
        }

        inlineMethodPair(methodPair[0], methodPair[1]);
    }

    private void inlineMethodPair(String caller, String callee) {
        SootMethod callerMethod = Scene.v().getMethod(caller);
        SootMethod calleeMethod = Scene.v().getMethod(callee);

        if (!callerMethod.hasActiveBody()) {
            callerMethod.retrieveActiveBody();
        }
        if (!calleeMethod.hasActiveBody()) {
             calleeMethod.retrieveActiveBody();
        }

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
