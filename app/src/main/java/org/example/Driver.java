package org.example;

import org.example.doop.DoopConventions;
import org.example.doop.DoopRenamer;
import soot.*;
import soot.jimple.*;
import soot.options.Options;
import soot.shimple.Shimple;
import soot.shimple.ShimpleBody;

import java.io.File;
import java.util.Collections;
import java.util.Objects;

public class Driver {

    public enum ValueType {
        VAR,
        STRING
    }

    private static final String EXAMPLE_DIR = System.getProperty("user.dir") + File.separator + ".." + File.separator + "example";
    private static final String TMP_DIR = System.getProperty("user.dir") + File.separator + ".." + File.separator + "tmp";
    private static final String OUTPUT_DIR = System.getProperty("user.dir") + File.separator + ".." + File.separator + "output";

    private final String example;
    private final String mainClass;

    public Driver(String example, String mainClass) {
        this.example = example;
        this.mainClass = mainClass;
    }

    public void setupSoot() {
        G.reset();

        Options.v().set_src_prec(Options.src_prec_only_class);
        Options.v().set_output_dir(OUTPUT_DIR);
        Options.v().set_process_dir(Collections.singletonList(EXAMPLE_DIR + File.separator + this.example));
        Options.v().set_main_class(this.mainClass);

        applyDoopSettings();

        Scene.v().loadNecessaryClasses();

        // We need to transform the bodies of application classes to match the naming convention of Doop
        transformApplicationClasses();
    }

    private void applyDoopSettings() {
        Options.v().setPhaseOption("jb", "use-original-names:true");
        Options.v().setPhaseOption("jb", "model-lambdametafactory:false");

        Options.v().set_via_shimple(true);
        Options.v().set_keep_line_number(true);
        Options.v().set_full_resolver(true);
        Options.v().set_allow_phantom_refs(true);

        DoopConventions.setSeparator();
    }

    private void transformApplicationClasses() {
        // We are only interested in application classes
        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            for (SootMethod sootMethod : sootClass.getMethods()) {
                // System.out.println(sootMethod.getSubSignature());
                transformToDoopBody(sootMethod);
            }
        }
    }

    /*
     * This method is adapted from Doop - FactGenerator: generate
     */
    private void transformToDoopBody(SootMethod sootMethod) {
        if (sootMethod.isPhantom()) {
            return;
        }

        if (!(sootMethod.isAbstract() || sootMethod.isNative())) {
            if (!sootMethod.hasActiveBody()) {
                sootMethod.retrieveActiveBody();
            }

            Body b0 = sootMethod.getActiveBody();
            try {
                if (b0 != null) {
                    Body b = Shimple.v().newBody(b0);
                    sootMethod.setActiveBody(b);
                    DoopRenamer.transform(b);
                }
            } catch (RuntimeException ex) {
                System.err.println("Doop Body transformation failed for method " + sootMethod.getSignature());
                ex.printStackTrace();
                throw ex;
            }
        }
    }


    public void optimizeInverseVariables(String cls, String method, String assignee, String assignor, ValueType assignorType) throws DootException {
        SootClass sootClass = Scene.v().getSootClass(cls);
        SootMethod sootMethod = sootClass.getMethod(method);
        Body body = sootMethod.getActiveBody();
        if (!(body instanceof ShimpleBody)) {
            throw new DootException("Method " + method + " should be converted to Shimple body before optimization");
        }

        Value assigorValue = null;
        if (assignorType == ValueType.VAR) {
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

    public void transformAllToJimpleBodies() throws DootException {
        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            for (SootMethod sootMethod : sootClass.getMethods()) {
                transformToJimpleBody(sootMethod);
            }
        }
    }

    private void transformToJimpleBody(SootMethod sootMethod) throws DootException {
        if (sootMethod.isPhantom()) {
            return;
        }

        if (!(sootMethod.isAbstract() || sootMethod.isNative())) {
            if (!sootMethod.hasActiveBody()) {
                sootMethod.retrieveActiveBody();
            }

            Body b0 = sootMethod.getActiveBody();
            try {
                if (b0 != null) {
                    if (b0 instanceof JimpleBody) {
                        return;
                    }
                    else if (b0 instanceof ShimpleBody) {
                        Body b = ((ShimpleBody) b0).toJimpleBody();
                        sootMethod.setActiveBody(b);
                    }
                    else {
                        throw new DootException("Unsupported body type for Jimple body transformation");
                    }
                }
            } catch (RuntimeException ex) {
                System.err.println("Jimple Body transformation failed for method " + sootMethod.getSignature());
                ex.printStackTrace();
                throw ex;
            } catch (DootException e) {
                throw e;
            }
        }
    }

    public void invokeDoop() {

    }
}









