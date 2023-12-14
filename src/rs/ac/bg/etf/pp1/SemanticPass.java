package rs.ac.bg.etf.pp1;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;

import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class SemanticPass extends VisitorAdaptor {

    boolean errorDetected = false;

    Struct currentType = null;
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

    public boolean passed() {
        return !errorDetected;
    }

    public void visit(ProgramConstDeclClass programConstDeclClass) {
        programConstDeclClass.obj = Tab.insert(Obj.Prog, programConstDeclClass.getProgram(), Tab.noType);
        Tab.openScope();
        nVars = Tab.currentScope.getnVars();
        Obj mainMeth = Tab.find("main");
        if (mainMeth != Tab.noObj
                && mainMeth.getKind() == Obj.Meth
                && mainMeth.getType() == Tab.noType
                && mainMeth.getLevel() == 0)
            report_info("Postoji ispravan main.", programConstDeclClass);
        else
            report_error("Ne postoji void main() globalna funkcija.", programConstDeclClass);

        Tab.chainLocalSymbols(programConstDeclClass.obj);
        Tab.closeScope();
    }

    public void visit(ProgramVarDeclClass programVarDeclClass) {
        programVarDeclClass.obj = Tab.insert(Obj.Prog, programVarDeclClass.getProgram(), Tab.noType);
        Tab.openScope();
        nVars = Tab.currentScope.getnVars();
        Obj mainMeth = Tab.find("main");
        if (mainMeth != Tab.noObj
                && mainMeth.getKind() == Obj.Meth
                && mainMeth.getType() == Tab.noType
                && mainMeth.getLevel() == 0)
            report_info("Postoji ispravan main.", programVarDeclClass);
        else
            report_error("Ne postoji void main() globalna funkcija.", programVarDeclClass);

        Tab.chainLocalSymbols(programVarDeclClass.obj);
        Tab.closeScope();
    }

    public void visit(TypeNamespaceClass type) {
        Obj typeNode = Tab.find(type.getNamespace());
        if (typeNode == Tab.noObj) {
            report_error("Nije pronadjen tip " + type.getNamespace() + " u tabeli simbola!", null);
            type.struct = Tab.noType;
        } else {
            if (Obj.Type == typeNode.getKind()) {
                currentType = typeNode.getType();
                type.struct = currentType;
                //report_info("tip = " + type.getTypeName(), type);
            } else {
                report_error("Greska: Ime " + type.getNamespace() + " ne predstavlja tip!", null);
                type.struct = Tab.noType;
            }
        }
    }

    public void visit(TermManyClass termManyClass) {
        if (termManyClass.getTerm().struct != Tab.intType || termManyClass.getFactor().struct != Tab.intType)
            report_error("Greska: mnozenje nije tipa int", termManyClass);
        termManyClass.struct = termManyClass.getTerm().struct;
    }

    public void visit(TermOneClass termOneClass) {
        termOneClass.struct = termOneClass.getFactor().struct;
    }

}