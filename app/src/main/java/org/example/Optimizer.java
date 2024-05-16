package org.example;

import org.apache.log4j.Logger;
import soot.*;
import soot.jimple.AssignStmt;
import soot.jimple.DefinitionStmt;
import soot.jimple.StringConstant;
import soot.shimple.ShimpleBody;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public class Optimizer {
    private static final Logger logger = Logger.getLogger(Optimizer.class);
    private final Parser parser;

    private final Path doopDir;

    public Optimizer(Path doopDir) {
        this.parser = new Parser();

        this.doopDir = doopDir;
    }

    public void optimize() throws DootException, IOException {
        BufferedReader bufferedReader = new BufferedReader(
                new FileReader(doopDir + File.separator + "last-analysis/ValuePairToOptimizeTyped.csv"));

        String line = bufferedReader.readLine();
        while (line != null) {
            optimizeOnce(line);
            line = bufferedReader.readLine();
        }
    }

    private void optimizeOnce(String line) throws DootException {
        logger.debug("Optimizing value pair: " + line);
        Parser.MustEqualTyped mustEqualTyped = parser.parseMustEqualTyped(line);
        if (Objects.equals(mustEqualTyped.assigneeClass, mustEqualTyped.assignorClass) &&
                Objects.equals(mustEqualTyped.assigneeSubMethodSig, mustEqualTyped.assignorSubMethodSig)) {
            // No inlining
            optimizeInverseVariables(mustEqualTyped.assigneeClass, mustEqualTyped.assigneeSubMethodSig,
                    mustEqualTyped.assignee, mustEqualTyped.assignor, mustEqualTyped.assignorType);
        }
    }

    private void optimizeInverseVariables(String className, String subMethodSig, String assignee, String assignor,
                                          Parser.ValueType assignorType) throws DootException {
        SootClass sootClass = Scene.v().getSootClass(className);
        SootMethod sootMethod = sootClass.getMethod(subMethodSig);
        Body body = sootMethod.getActiveBody();
        if (!(body instanceof ShimpleBody)) {
            throw new DootException("Method " + subMethodSig +
                    " should be converted to Shimple body before optimization");
        }

        Value assigorValue = null;
        if (assignorType == Parser.ValueType.VAR) {
            assigorValue = getVariableByName(body, assignor);
        }

        for (Unit unit : body.getUnits()) {
            if (unit instanceof AssignStmt) {
                Value leftOp = ((AssignStmt) unit).getLeftOp();
                if (leftOp instanceof Local) {
                    String leftOpName = ((Local) leftOp).getName();
                    if (Objects.equals(leftOpName, assignee)) {
                        switch (assignorType) {
                            case VAR:
                                ((AssignStmt) unit).setRightOp(assigorValue);
                                break;
                            case STRING:
                                ((AssignStmt) unit).setRightOp(StringConstant.v(assignor));
                                break;
                            default:
                                throw new DootException("Undefined assignor ValueType");
                        }
                        return;
                    }
                }
            }
        }

        throw new DootException("No optimization opportunity found");
    }

    private Value getVariableByName(Body body, String varName) throws DootException {
        for (Unit unit : body.getUnits()) {
            if (unit instanceof DefinitionStmt) {
                for (ValueBox valueBox : unit.getDefBoxes()) {
                    Value value = valueBox.getValue();
                    if ((value instanceof Local) && (((Local) value).getName() == varName)) {
                        return value;
                    }
                }
            }
        }

        throw new DootException("Varaible " + varName + " not found in method " + body.getMethod().getSignature());
    }
}
