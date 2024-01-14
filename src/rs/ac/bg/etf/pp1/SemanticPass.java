package rs.ac.bg.etf.pp1;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;

import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

public class SemanticPass extends VisitorAdaptor {

    private final Logger log = Logger.getLogger(getClass());
    private boolean errorDetected;
    private Struct lastVarConstType = Tab.noType;
    private Struct lastMethodType = null;
    private String currNamespace = "";
    private int counterFormalParams;
    private int controlStructure = 0;
    private Collection<Obj> requiredParams;
    private ArrayList<Struct> passedParams;
    private final ArrayList<Obj> unresolvedLabels = new ArrayList<>();
    int nVars;

    public boolean passed() {
        return !errorDetected;
    }

    public void report_info(String message, SyntaxNode info) {
        StringBuilder msg = new StringBuilder(message);
        int line = (info == null) ? 0 : info.getLine();
        if (line != 0)
            msg.append(" on line ").append(line);
        log.info(msg.toString());
    }

    public void report_error(String message, SyntaxNode info) {
        errorDetected = true;
        StringBuilder msg = new StringBuilder(message);
        int line = (info == null) ? 0 : info.getLine();
        if (line != 0)
            msg.append(" on line").append(line);
        log.error(msg.toString());
    }

    @Override
    public void visit(ProgName visitor) {
        visitor.obj = Tab.insert(Obj.Prog, visitor.getName(), Tab.noType);
        Tab.openScope();
    }

    @Override
    public void visit(Program visitor) {

        // if unresolved labels exist report error
        if (!unresolvedLabels.isEmpty())
            for (Obj label : unresolvedLabels)
                report_error("Error: " + label.getName() + " is unresolved label", null);

        nVars = Tab.currentScope.getnVars();

        Obj mainMeth = Tab.find("main");
        if (mainMeth.getKind() == Obj.Meth
                && mainMeth != Tab.noObj
                && mainMeth.getLevel() == 0
                && mainMeth.getType() == Tab.noType)
            report_info("main already exist", visitor);
        else
            report_error("global void main() does not exist", visitor);

        Tab.chainLocalSymbols(visitor.getProgName().obj);
        Tab.closeScope();

    }

    @Override
    public void visit(Namespace visitor) {
        currNamespace = "";
    }

    @Override
    public void visit(NamespaceName visitor) {

        Obj namespaceFind = Tab.currentScope.findSymbol(visitor.getName());

        if (namespaceFind != null)
            report_error("Namespace " + visitor.getName() + " already exist", visitor);
        else
            visitor.obj = Tab.insert(Obj.Type, visitor.getName(), Tab.noType);

        currNamespace = visitor.getName();
    }

    private void setLastVarConstType(Struct type) {
        lastVarConstType = type;
    }

    public Struct getLastMethodType() {
        return lastMethodType;
    }

    public void setLastMethodType(Struct lastMethodType) {
        this.lastMethodType = lastMethodType;
    }

    private Struct getLastVarConstType() {
        return lastVarConstType;
    }

    @Override
    public void visit(Type visitor) {
        Obj typeNode = Tab.find(visitor.getName());

        if (typeNode == Tab.noObj) {
            report_error("Type " + visitor.getName() + " does not exist", visitor);
            visitor.struct = Tab.noType;
        } else {
            if (Obj.Type == typeNode.getKind()) {
                visitor.struct = typeNode.getType();
                setLastVarConstType(visitor.struct);
            } else {
                report_error("Type " + visitor.getName() + " is not type", visitor);
                visitor.struct = Tab.noType;
            }
        }
    }

    static public String prepareSymbol(String name, String prefix) {
        return !Objects.equals(prefix, "") ?
                prefix + "#" + name : name;
    }

    @Override
    public void visit(ConstDef visitor) {
        Obj typeNode = Tab.currentScope.findSymbol(prepareSymbol(visitor.getName(), currNamespace));

        if (typeNode != null)
            report_error("Const " + visitor.getName() + " already exist", visitor);
        else
            Tab.insert(Obj.Con, prepareSymbol(visitor.getName(), currNamespace), getLastVarConstType());
    }

    @Override
    public void visit(VarDeclPart visitor) {
        String varName;

        if (getLastMethodType() == null)
            varName = prepareSymbol(visitor.getName(), currNamespace);
        else
            varName = visitor.getName();

        Obj typeNode = Tab.currentScope.findSymbol(varName);

        if (typeNode != null)
            report_error("Variable " + visitor.getName() + " already exist", visitor);
        else {
            if (visitor.getArrayOpt() instanceof ArrayOne)
                Tab.insert(Obj.Var, varName, new Struct(Struct.Array, getLastVarConstType()));
            else
                Tab.insert(Obj.Var, varName, getLastVarConstType());
        }
    }

    @Override
    public void visit(MethodVoid visitor) {
        setLastMethodType(visitor.struct = Tab.noType);
    }

    public void visit(MethodTType visitor) {
        setLastMethodType(visitor.struct = visitor.getType().struct);
    }

    @Override
    public void visit(MethodName visitor) {
        Obj typeNode = Tab.currentScope.findSymbol(prepareSymbol(visitor.getName(), currNamespace));
        if (typeNode != null)
            report_error("Method " + visitor.getName() + " already exist", visitor);
        else
            visitor.obj = Tab.insert(Obj.Meth, prepareSymbol(visitor.getName(), currNamespace), getLastMethodType());
        Tab.openScope();
    }

    @Override
    public void visit(MethodDecl visitor) {
        visitor.obj = visitor.getMethodName().obj;
        visitor.obj.setLevel(counterFormalParams);
        Tab.chainLocalSymbols(visitor.obj);
        Tab.closeScope();
        counterFormalParams = 0;
        setLastMethodType(null);
    }

    @Override
    public void visit(FormPars visitor) {
        Struct type = visitor.getType().struct;

        if (visitor.getArrayOpt() instanceof ArrayOne)
            type = new Struct(Struct.Array, type);

        Tab.insert(Obj.Var, visitor.getName(), type);
        counterFormalParams++;
    }

    @Override
    public void visit(For visitor) {
        ++controlStructure;
    }

    @Override
    public void visit(StatementFor visitor) {
        --controlStructure;
    }

    @Override
    public void visit(StatementBreak visitor) {
        if (controlStructure == 0)
            report_error("Break statement must be inside for", visitor);
    }

    @Override
    public void visit(StatementContinue visitor) {
        if (controlStructure == 0)
            report_error("Continue statement must be inside for", visitor);
    }

    private boolean checkDesignatorType(Designator visitor) {
        return visitor.obj.getKind() == Obj.Var || visitor.obj.getKind() == Obj.Elem;
    }

    @Override
    public void visit(StatementRead visitor) {
        Designator design = visitor.getDesignator();
        if (checkDesignatorType(design))
            if (design.obj.getType() == MyTab.intType || design.obj.getType() == MyTab.charType || design.obj.getType() == MyTab.boolType) {
                report_info("read()", visitor);
                return;
            }
        report_error("Invalid read() argument", visitor);
    }

    @Override
    public void visit(StatementPrint visitor) {
        Struct kind = visitor.getExpr().struct;
        if (kind != Tab.intType && kind != Tab.charType && kind != MyTab.boolType)
            report_error("Error: Expression must be int, char or bool", visitor);
    }

    @Override
    public void visit(StatementReturn visitor) {
        if (visitor.getExprOpt() instanceof ExprZero && getLastMethodType() != Tab.noType)
            report_error("Error: Return type must be void", visitor);
        if (visitor.getExprOpt() instanceof ExprOne && !checkTypes(((ExprOne) visitor.getExprOpt()).getExpr().struct, getLastMethodType()))
            report_error("Error: Return type must be " + getLastMethodType().getKind(), visitor);
    }

    private boolean checkTypes(Struct leftType, Struct rightType) {
        if (leftType.getKind() == Struct.Array)
            leftType = leftType.getElemType();
        if (rightType.getKind() == Struct.Array)
            rightType = rightType.getElemType();

        return leftType == rightType;
    }

    private Obj getFirstLeft(MatrixMany visitor) {
        if (visitor.getMatrixOpt() instanceof MatrixZero) {
            SyntaxNode parent = visitor.getParent();
            while (parent instanceof MatrixMany)
                parent = parent.getParent();
            return getDesignatorName((Designator) parent);
        } else
            return visitor.getMatrixOpt().obj;
    }

    @Override
    public void visit(FactorMax visitor) {
        if (visitor.getDesignator().obj.getType().getKind() != Struct.Array)
            report_error("Error: Designator must be array", visitor);
        visitor.struct = Tab.intType;
    }

    private Obj getDesignatorName(Designator designator) {

        if (designator.getScope() instanceof ScopeLocal)
            return ((ScopeLocal) designator.getScope()).obj;
        else
            return ((ScopeNamespace) designator.getScope()).obj;
    }

    @Override
    public void visit(ArraySize visitor) {
        Obj firstLeft = getFirstLeft((MatrixMany) visitor.getParent());
        if (firstLeft == Tab.noObj)
            visitor.obj = Tab.noObj;
        else {
            if (visitor.getExpr().struct != Tab.intType)
                report_error("Error: inside [] muse be an Int", visitor);
            visitor.obj = new Obj(Obj.Elem, "elem", firstLeft.getType().getElemType());
        }
    }

    @Override
    public void visit(ScopeNamespace visitor) {
        Obj namespaceFind = Tab.find(visitor.getNamespace());

        if (namespaceFind == Tab.noObj)
            report_error("Namespace " + visitor.getName() + " does not exist", visitor);
        else {
            Obj idFind = Tab.find(prepareSymbol(visitor.getName(), visitor.getNamespace()));
            if (idFind == null)
                report_error("Ident " + visitor.getName() + " does not exist", visitor);
            else
                visitor.obj = idFind;
        }
    }

    @Override
    public void visit(ScopeLocal visitor) {
        String varName = visitor.getName();

        if (currNamespace != null)
            if (Tab.currentScope().findSymbol(varName) == null)
                varName = prepareSymbol(visitor.getName(), currNamespace);

        Obj idFind = Tab.find(varName);
        if (idFind == Tab.noObj)
            report_error("Ident " + visitor.getName() + " does not exist", visitor);
        else
            visitor.obj = idFind;
    }

    @Override
    public void visit(DesignStmAssign visitor) {
        checkDesignatorType(visitor.getDesignator());

        Struct tempL = visitor.getDesignator().obj.getType();
        Struct tempR = visitor.getExpr().struct;

        if (!checkTypes(tempL, tempR))
            report_error("Error: Types must be same", visitor);

    }

    private boolean isParamsCorrect() {
        if (passedParams != null && !requiredParams.isEmpty())
            if (passedParams.size() == requiredParams.size()) {
                int i = 0;
                for (Obj required : requiredParams) {
                    if (required.getType() != passedParams.get(i))
                        if (required.getType().getKind() != Struct.Array && passedParams.get(i).getKind() != Struct.Array)
                            return false;
                    i++;
                }
            }
        return true;
    }

    @Override
    public void visit(DesignStmParen visitor) {

        if (visitor.getDesignator().obj.getKind() != Obj.Meth) {
            report_error("ne postoji metoda " + visitor.getDesignator().obj.getName(), visitor);
        } else {
            requiredParams = visitor.getDesignator().obj.getLocalSymbols();
            if (!isParamsCorrect())
                report_error("Error: bad parameters in method call " + visitor.getDesignator().obj.getName(), visitor);
            else
                report_info("Function call" + visitor.getDesignator().obj.getName(), visitor);
        }
        passedParams = null;
        requiredParams = null;
    }

    @Override
    public void visit(Designator visitor) {

        Obj idFind = getDesignatorName(visitor);

        if (idFind == null)
            report_error("Identificator does not exist", visitor);
        else
            visitor.obj = idFind;

        if (visitor.obj.getType().getKind() == Struct.Array && visitor.getMatrixOpt() instanceof MatrixMany)
            visitor.obj = ((MatrixMany) (visitor.getMatrixOpt())).getArraySize().obj;

    }

    @Override
    public void visit(DesignStmPlus visitor) {
        if (!checkDesignatorType(visitor.getDesignator()) || visitor.getDesignator().obj.getType() != Tab.intType)
            report_error("Error: Designator must be int", visitor);
    }

    @Override
    public void visit(DesignStmMinus visitor) {
        if (!checkDesignatorType(visitor.getDesignator()) || visitor.getDesignator().obj.getType() != Tab.intType)
            report_error("Error: Designator must be int", visitor);
    }

    @Override

    public void visit(ConstNum visitor) {
        visitor.struct = Tab.intType;
    }

    @Override
    public void visit(ConstChar visitor) {
        visitor.struct = Tab.charType;
    }

    @Override
    public void visit(ConstBool visitor) {
        visitor.struct = MyTab.boolType;
    }

    @Override
    public void visit(TermMany visitor) {
        if (visitor.getTerm().struct != Tab.intType || visitor.getFactor().struct != Tab.intType)
            report_error("Error: Both terms must be int", visitor);
        visitor.struct = visitor.getTerm().struct;
    }

    @Override
    public void visit(TermOne visitor) {
        visitor.struct = visitor.getFactor().struct;
    }

    @Override
    public void visit(ExprMinus visitor) {
        visitor.struct = visitor.getTerm().struct;
        if (visitor.struct != Tab.intType) {
            report_error("Error: expression must be an int type", visitor);
            visitor.struct = Tab.noType;
        }
    }

    @Override
    public void visit(ExprTerm visitor) {
        visitor.struct = visitor.getTerm().struct;
    }

    @Override
    public void visit(ExprAddOp visitor) {
        visitor.struct = visitor.getTerm().struct;
        if ((visitor.getExpr() instanceof ExprAddOp && visitor.getExpr().struct != Tab.intType)
                || visitor.getTerm().struct != Tab.intType) {
            report_error("Error: can not sum non Int value", visitor.getParent());
            visitor.struct = Tab.noType;
        }
    }

    @Override
    public void visit(CondFactOne visitor) {
        visitor.struct = visitor.getExpr().struct;
        if (visitor.struct != MyTab.boolType)
            report_error("Еrror: expression must be a bool type", visitor);
    }

    @Override
    public void visit(CondFactMany visitor) {
        if (checkTypes(visitor.getExpr().struct, visitor.getExpr1().struct))
            if ((visitor.getExpr().struct.getKind() == Struct.Array)
                    && !(visitor.getRelOp() instanceof RelOpEqualsTo || visitor.getRelOp() instanceof RelOpDifferent)) {
                report_error("Error: can not compare array", visitor);
                visitor.struct = Tab.noType;
            } else
                visitor.struct = MyTab.boolType;
        else {
            report_error("Error: can not compare different types", visitor);
            visitor.struct = Tab.noType;
        }
    }

    @Override
    public void visit(CondTermMany visitor) {
        if (visitor.getCondFact().struct == MyTab.boolType && visitor.getCondTerm().struct == MyTab.boolType)
            visitor.struct = MyTab.boolType;
    }

    @Override
    public void visit(CondTermOne visitor) {
        visitor.struct = visitor.getCondFact().struct;
    }

    @Override
    public void visit(ConditionMany visitor) {
        if (visitor.getCondition().struct == MyTab.boolType && visitor.getCondTerm().struct == MyTab.boolType)
            visitor.struct = MyTab.boolType;
        else
            report_error("LOS TIP USLOVA", visitor);
    }

    @Override
    public void visit(ConditionOne visitor) {
        visitor.struct = visitor.getCondTerm().struct;
    }

    @Override
    public void visit(ActParsMany visitor) {
        if (passedParams == null)
            passedParams = new ArrayList<>();
        passedParams.add(visitor.getExpr().struct);
    }

    @Override
    public void visit(ActParsOne visitor) {
        if (passedParams == null)
            passedParams = new ArrayList<>();
        passedParams.add(visitor.getExpr().struct);
    }

    @Override
    public void visit(DesignStmMany visitor) {
        if (visitor.getDesignator().obj.getType().getKind() != Struct.Array)
            report_error("Error: Designator must be array", visitor);
        if (visitor.getDesignator1().obj.getType().getKind() != Struct.Array)
            report_error("Error: Designator must be array", visitor);
    }

    @Override
    public void visit(FactorParenPars visitor) {
        visitor.struct = visitor.getDesignator().obj.getType();
        if (visitor.getFactorParenParsOpt() instanceof ParenParsZero)
            return;

        if (visitor.getDesignator().obj.getKind() != Obj.Meth) {
            report_error("Error: " + getDesignatorName(visitor.getDesignator()).getName() + " not a method", visitor);
            return;
        }
        requiredParams = visitor.getDesignator().obj.getLocalSymbols();

        if (!isParamsCorrect())
            report_error("Error: bad parameters in method call " + visitor.getDesignator().obj.getName(), visitor);

        visitor.struct = visitor.getDesignator().obj.getType();

        requiredParams = null;
        passedParams = null;
    }

    @Override
    public void visit(FactorConstVal visitor) {
        visitor.struct = visitor.getConstVal().struct;
    }

    @Override
    public void visit(FactorNewArray visitor) {
        visitor.struct = new Struct(Struct.Array, visitor.getType().struct);
        if (visitor.getExpr().struct != Tab.intType)
            report_error("Error: inside [] muse be an Int", visitor);

    }

    @Override
    public void visit(FactorParenExpr visitor) {
        visitor.struct = visitor.getExpr().struct;
    }


}


