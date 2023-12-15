package rs.ac.bg.etf.pp1;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;

import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class SemanticPass extends VisitorAdaptor {

    private boolean errorDetected = false;
    private Obj currentMethod = null;
    private int formalParamCount = 0;

    private Struct currentType = null;
    private int numberOfVars;

    private final Logger log = Logger.getLogger(getClass());

    public void report_error(String message, SyntaxNode info) {
        errorDetected = true;
        StringBuilder msg = new StringBuilder(message);
        int line = (info == null) ? 0 : info.getLine();
        if (line != 0) msg.append(" on line ").append(line);
        log.error(msg.toString());
    }

    public void report_info(String message, SyntaxNode info) {
        StringBuilder msg = new StringBuilder(message);
        int line = (info == null) ? 0 : info.getLine();
        if (line != 0) msg.append(" on line ").append(line);
        log.info(msg.toString());
    }

    public boolean passed() {
        return !errorDetected;
    }

    public void visit(Program program) {
        program.obj = Tab.insert(Obj.Prog, program.getProgramName(), Tab.noType);
        Tab.openScope();
        numberOfVars = Tab.currentScope.getnVars();
        Obj mainMeth = Tab.find("main");
        if (mainMeth != Tab.noObj && mainMeth.getKind() == Obj.Meth && mainMeth.getType() == Tab.noType && mainMeth.getLevel() == 0)
            report_info("Main already exist.", program);
        else report_error("Main does not exist.", program);

        Tab.chainLocalSymbols(program.obj);
        Tab.closeScope();
    }


    public void visit(TypeNamespaceClass type) {
        Obj typeNode = Tab.find(type.getNamespace());
        if (typeNode == Tab.noObj) {
            report_error("Type not found " + type.getNamespace() + " in symbol table!", null);
            type.struct = Tab.noType;
        } else {
            if (Obj.Type == typeNode.getKind()) {
                currentType = typeNode.getType();
                type.struct = currentType;
            } else {
                report_error("Error: Name " + type.getNamespace() + " is not type!", null);
                type.struct = Tab.noType;
            }
        }
    }

    public void visit(TermManyClass termManyClass) {
        if (termManyClass.getTerm().struct != Tab.intType || termManyClass.getFactor().struct != Tab.intType)
            report_error("Error: multiplication is not int type!", termManyClass);
        termManyClass.struct = termManyClass.getTerm().struct;
    }

    public void visit(TermOneClass termOneClass) {
        termOneClass.struct = termOneClass.getFactor().struct;
    }

    public void visit(MethodDecl methodDecl) {
        currentMethod.setLevel(formalParamCount);
        Tab.chainLocalSymbols(currentMethod);
        Tab.closeScope();
        methodDecl.obj = currentMethod;
        currentMethod = null;
        formalParamCount = 0;
    }

    public void visit(ConstValNumClass constValNumClass) {
        constValNumClass.struct = Tab.intType;
    }

    public void visit(ConstValCharClass constValCharClass) {
        constValCharClass.struct = Tab.charType;
    }

    public void visit(ConstValBooCllass constValBooCllass) {
        constValBooCllass.struct = MyTab.boolType;
    }

}