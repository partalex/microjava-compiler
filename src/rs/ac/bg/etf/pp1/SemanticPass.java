package rs.ac.bg.etf.pp1;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;

import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;

public class SemanticPass extends VisitorAdaptor {

    boolean errorDetected = false;
    int printCallCount = 0;
    Obj currentMethod = null;
    boolean returnFound = false;
    int nVars;

    Logger log = Logger.getLogger(getClass());

    public void report_error(String message, SyntaxNode info) {
        errorDetected = true;
        StringBuilder msg = new StringBuilder(message);
        int line = (info == null) ? 0 : info.getLine();
        if (line != 0)
            msg.append(" na liniji ").append(line);
        log.error(msg.toString());
    }

    public void report_info(String message, SyntaxNode info) {
        StringBuilder msg = new StringBuilder(message);
        int line = (info == null) ? 0 : info.getLine();
        if (line != 0)
            msg.append(" na liniji ").append(line);
        log.info(msg.toString());
    }

    public void visit(ProgramConstDecl programConstDecl) {
        programConstDecl.obj = Tab.insert(Obj.Prog, programConstDecl.getProgram(), Tab.noType);
        Tab.openScope();
        nVars = Tab.currentScope.getnVars();
        Obj mainMeth = Tab.find("main");
        if (mainMeth != Tab.noObj
                && mainMeth.getKind() == Obj.Meth
                && mainMeth.getType() == Tab.noType
                && mainMeth.getLevel() == 0)
            report_info("Postoji ispravan main.", programConstDecl);
        else
            report_error("Ne postoji void main() globalna funkcija.", programConstDecl);

        Tab.chainLocalSymbols(programConstDecl.obj);
        Tab.closeScope();
    }

    public void visit(ProgramVarDecl programVarDecl) {
        programVarDecl.obj = Tab.insert(Obj.Prog, programVarDecl.getProgram(), Tab.noType);
        Tab.openScope();
        nVars = Tab.currentScope.getnVars();
        Obj mainMeth = Tab.find("main");
        if (mainMeth != Tab.noObj
                && mainMeth.getKind() == Obj.Meth
                && mainMeth.getType() == Tab.noType
                && mainMeth.getLevel() == 0)
            report_info("Postoji ispravan main.", programVarDecl);
        else
            report_error("Ne postoji void main() globalna funkcija.", programVarDecl);

        Tab.chainLocalSymbols(programVarDecl.obj);
        Tab.closeScope();
    }

}