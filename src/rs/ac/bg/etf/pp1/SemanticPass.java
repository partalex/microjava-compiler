package rs.ac.bg.etf.pp1;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;

import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Scope;
import rs.etf.pp1.symboltable.concepts.Struct;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class SemanticPass extends VisitorAdaptor {
    private boolean errorDetected = false;
    private Obj currentMethod = null;
    private int formalParamCount = 0;
    private Struct currentType = null;
    int numberOfVars;

    private ArrayList<Struct> actPartsPassed;
    private Collection<Obj> actPartsRequired;
    private final Logger log = Logger.getLogger(getClass());
    private int breakCount;
    private int continueCount;
    private boolean isArray;

    private int lastNamespaceLevel = 0;
    private Obj currentNamespace;
    private boolean inFor;

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

    public void visit(ProgramName programName) {
        programName.obj = Tab.insert(Obj.Prog, programName.getProgramName(), Tab.noType);
        Tab.openScope();
    }

    public void visit(Program program) {
        program.obj = program.getProgramName().obj;
        MyTab.tempHelp = Tab.insert(Obj.Var, "#", Tab.intType); // TODO - check if this is ok
        numberOfVars = Tab.currentScope.getnVars();
        Obj mainMeth = Tab.find("main");
        if (mainMeth != Tab.noObj && mainMeth.getKind() == Obj.Meth && mainMeth.getType() == Tab.noType && mainMeth.getLevel() == 0)
            report_info("Main already exist.", program);
        else report_error("Main does not exist.", program);
        Tab.chainLocalSymbols(program.obj);
        Tab.closeScope();
    }

    public Obj findSymbol(String symbolName, Obj namespace) {
        for (Obj symbol : namespace.getLocalSymbols()) {
            if (symbol.getName().equals(symbolName)) {
                return symbol;
            }
        }
        return null; // Symbol not found
    }

    public void visit(OptNamespaceClass optNamespaceClass) {
        Obj typeNode = Tab.find(optNamespaceClass.getNamespace());
        if (typeNode == Tab.noObj) {
            report_error("Type not found " + optNamespaceClass.getNamespace() + " in symbol table!", null);
            optNamespaceClass.obj = typeNode;
        } else {
            if (typeNode.getKind()==Obj.Type) {
                currentType = typeNode.getType();
                Obj temp = findSymbol(optNamespaceClass.getDesignatorName().getDesignatorName(), typeNode);
                if (temp==null) {
                    report_info(Tab.currentScope().getOuter().getLocals().toString(), optNamespaceClass);
                    report_error("Error: Field " + optNamespaceClass.getDesignatorName().getDesignatorName() +
                                    " is not in namespace " + optNamespaceClass.getNamespace()+"!", null);
                    return;
                }
                currentType = temp.getType();
                optNamespaceClass.obj = temp;
                if (optNamespaceClass.obj.getKind()!=Obj.Meth) {
                    optNamespaceClass.obj.setLevel(0);
                }

            } else {
                report_error("Error: Name " + optNamespaceClass.getNamespace()+ " is not namespace!", null);
                optNamespaceClass.obj = typeNode;;
            }
        }
    }

    public void visit(OptNamespaceEmptyClass optNamespaceEmptyClass) {
        optNamespaceEmptyClass.obj = Tab.find(optNamespaceEmptyClass.getDesignatorName().getDesignatorName());
        int currentAdress = 0;
        Scope scope = Tab.currentScope().getOuter();

    }


    public void visit(TypeClass typeClass) {
        Obj typeNode = Tab.find(typeClass.getTypeName());

        if (typeNode == Tab.noObj) {
            report_error("Type not found " + typeClass.getTypeName() + " in symbol table!", null);
            typeClass.struct = Tab.noType;
        } else {
            if (Obj.Type == typeNode.getKind()) {
                currentType = typeNode.getType();
                typeClass.struct = currentType;
            } else {
                report_error("Error: Name " + typeClass.getTypeName() + " is not type!", null);
                typeClass.struct = Tab.noType;
            }
        }
    }

    public void visit(TermManyClass termManyClass) {

        if (termManyClass.getTerm().struct != Tab.intType || termManyClass.getFactor().struct != Tab.intType)
            report_error("Error: multiplication is not Int type!", termManyClass);
        termManyClass.struct = termManyClass.getTerm().struct;
    }

    public void visit(TermOneClass termOneClass) {
        termOneClass.struct = termOneClass.getFactor().struct;
        if (termOneClass.getFactor().struct.getKind()==3) {
            termOneClass.struct = termOneClass.getFactor().struct.getElemType();
        }
    }


    public void visit(MethodTypeName methodTypeName) {
        currentMethod = Tab.insert(Obj.Meth, methodTypeName.getMethodName(), currentType);
        methodTypeName.obj = currentMethod;
        Tab.openScope();
    }

    public void visit(MethodDecl methodDec) {
        currentMethod.setLevel(formalParamCount);
        Tab.chainLocalSymbols(currentMethod);
        Tab.closeScope();
        methodDec.obj = currentMethod;
        currentMethod = null;
        formalParamCount = 0;

        if (currentNamespace != null)
            Tab.insert(Obj.Var, currentNamespace.getName(), currentNamespace.getType());
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


    private boolean checkTypes(Struct leftType, Struct rightType) {
        if (leftType == Tab.noType && rightType.getKind() == Struct.Array)
            return true;

        if (leftType.getKind() == Struct.Array && rightType.getKind() == Struct.Array) {
            leftType = leftType.getElemType();
            rightType = rightType.getElemType();
        }

       else if (leftType.getKind() == Struct.Array) {
            leftType = leftType.getElemType();
        }


        if (leftType != rightType) {
            return false;
        }
        else{
            return true;
        }
    }

    public void visit(StatementReturnClass statementReturnClass) {
        if (statementReturnClass.getOptExpr() instanceof OptExprEmptyClass && currentMethod.getType() != Tab.noType)
            report_error("Error: method require return value ", statementReturnClass);
        if (statementReturnClass.getOptExpr() instanceof OptExprOneClass &&
                !checkTypes(((OptExprOneClass) statementReturnClass.getOptExpr()).getExpr().struct, currentMethod.getType()))
            report_error("Error: returned value and required return type are different ", statementReturnClass);
    }

    private boolean checkDesignType(Designator designator) {
        int localKind = designator.obj.getKind();
        return localKind == Obj.Var || localKind == Obj.Elem || localKind == Obj.Fld;
    }

    public void visit(StatementReadClass statementReadClass) {
        Designator designator = statementReadClass.getDesignator();
        Struct tempType = Tab.noType;
        if (checkDesignType(designator))
            if (designator.obj.getType().getKind()==3) {
                 tempType = designator.obj.getType().getElemType();
            }
            else {
                tempType = designator.obj.getType();
            }
            if (tempType == MyTab.intType || tempType == MyTab.charType || tempType == MyTab.boolType) {
                report_info("read()", statementReadClass);
                return;
            }
        report_error("Error: read has no good parameters", statementReadClass);
    }


    public void visit(StatementPrintClass statementPrintClass) {
        Struct kind = statementPrintClass.getExpr().struct;
        if (kind != Tab.intType && kind != Tab.charType && kind != MyTab.boolType)
            report_error("Error: print must have Int/Char/Bool", statementPrintClass);
    }

    public void visit(FactorParenParsClass factorParenParsClass) {
        factorParenParsClass.struct = factorParenParsClass.getDesignator().obj.getType();
        if (factorParenParsClass.struct.getKind()==3) {
            factorParenParsClass.struct = factorParenParsClass.struct.getElemType();
        }
        if (factorParenParsClass.getOptFactorParenPars() instanceof OptFactorEmptyClass)
            return;
        if (factorParenParsClass.getDesignator().obj.getKind() != Obj.Meth) {
            report_error("Error: not a function ", factorParenParsClass);
            return;
        }

        actPartsRequired = factorParenParsClass.getDesignator().obj.getLocalSymbols();
        if (!checkParams())
            report_error("Error: bad parameters for calling method ", factorParenParsClass);

        factorParenParsClass.struct = factorParenParsClass.getDesignator().obj.getType();

        actPartsRequired = null;
        actPartsPassed = null;
    }

    private boolean checkParams() {
        if (actPartsPassed.size() == actPartsRequired.size()) {
            int i = 0;
            for (Obj required : actPartsRequired) {
                if (required.getType() != actPartsPassed.get(i))
                    if (required.getType().getKind() != Struct.Array && actPartsPassed.get(i).getKind() != Struct.Array)
                        return false;
                i++;
            }
        }
        return true;
    }

    public void visit(ActParsOneClass actParsOneClass) {
        if (actPartsPassed == null)
            actPartsPassed = new ArrayList<>();
        actPartsPassed.add(actParsOneClass.getExpr().struct);
    }

    public void visit(ActParsManyClass actParsManyClass) {
        if (actPartsPassed == null)
            actPartsPassed = new ArrayList<>();
        actPartsPassed.add(actParsManyClass.getExpr().struct);
    }

    public void visit(ConditionManyClass conditionManyClass) {
        if (conditionManyClass.getCondition().struct == MyTab.boolType && conditionManyClass.getCondTerm().struct == MyTab.boolType)
            conditionManyClass.struct = MyTab.boolType;
        else
            report_error("Bad condition type", conditionManyClass);
    }

    public void visit(ConditionOneClass conditionOneClass) {
        conditionOneClass.struct = conditionOneClass.getCondTerm().struct;
    }

    public void visit(CondTermManyClass condTermManyClass) {
        if (condTermManyClass.getCondFact().struct == MyTab.boolType && condTermManyClass.getCondTerm().struct == MyTab.boolType)
            condTermManyClass.struct = MyTab.boolType;
    }

    public void visit(CondTermOneClass condTermOneClass) {
        condTermOneClass.struct = condTermOneClass.getCondFact().struct;
    }

    public void visit(CondFactOneClass condFactOneClass) {
        condFactOneClass.struct = condFactOneClass.getExpr().struct;
        if (condFactOneClass.struct != MyTab.boolType)
            report_error("Error: not Bool", condFactOneClass);
    }

    public void visit(CondFactManyClass condFactManyClass) {
        if (checkTypes(condFactManyClass.getExpr().struct, condFactManyClass.getExpr1().struct))
            if (condFactManyClass.getExpr().struct.getKind() == Struct.Array &&
                    !(condFactManyClass.getRelop() instanceof RelopEqualstoClass || condFactManyClass.getRelop() instanceof RelopDifferentClass)) {
                report_error("Error: array can only be compared by == and !=", condFactManyClass);
                condFactManyClass.struct = Tab.noType;
            } else
                condFactManyClass.struct = MyTab.boolType;
        else {
            report_error("Error: types are incompatible", condFactManyClass);
            condFactManyClass.struct = Tab.noType;
        }
    }

    public void visit(AddTermManyClass addTermManyClass) {
        addTermManyClass.struct = addTermManyClass.getTerm().struct;
        if (
                (addTermManyClass.getAddTerm() instanceof AddTermManyClass &&
                        addTermManyClass.getAddTerm().struct != Tab.intType)
                        || addTermManyClass.getTerm().struct != Tab.intType) {
            report_error("Error: can not sum non Int value", addTermManyClass.getParent());
            addTermManyClass.struct = Tab.noType;
        }
    }

    public void visit(Expr expr) {
        expr.struct = expr.getTerm().struct;
        if (expr.getOptMinus() instanceof OptMin) if (expr.struct != Tab.intType) {
            report_error("Error: expression must be an int type", expr);
            expr.struct = Tab.noType;
        }
        if (!(expr.getAddTerm() instanceof AddTermEmptyClass) && expr.getTerm().struct != Tab.intType)
            report_error("Error: can not sum non Int value", expr.getParent());
    }

    public void visit(FactorConstValClass factorConstValClass) {
        factorConstValClass.struct = factorConstValClass.getConstVal().struct;
    }

    public void visit(FactorParenExprClass factorParenExprClass) {
        factorParenExprClass.struct = factorParenExprClass.getExpr().struct;
    }

    public void visit(FactorNewTypeExprClass factorNewTypeExprClass) {
        factorNewTypeExprClass.struct = new Struct(Struct.Array, factorNewTypeExprClass.getType().struct);
        if (factorNewTypeExprClass.getExpr().struct != Tab.intType)
            report_error("Error: inside [] must be an Int ", factorNewTypeExprClass);

    }

    String getDesignName(Designator design) {
        String designName="";
        if (design.getOptNamespace() instanceof OptNamespaceEmptyClass) {
            designName = ((OptNamespaceEmptyClass) design.getOptNamespace()).getDesignatorName().getDesignatorName();
        }
        else {
            designName = ((OptNamespaceClass) design.getOptNamespace()).getDesignatorName().getDesignatorName();
        }

        return designName;
    }

    public void visit(Designator design) {
        boolean isDesignatorEmpty = design.getOptDesignatorPart() instanceof OptDesignatorPartEmptyClass;
        if (design.getOptNamespace() instanceof OptNamespaceClass) {
            design.obj = design.getOptNamespace().obj;
        }
        else {
            String designName = getDesignName(design);
            design.obj = Tab.find(designName);
        }
        if (isDesignatorEmpty) {
            try {
                if (design.obj.getKind() == Obj.Con)
                    report_info("Access to const " + design.obj.getName(), design);
                else if (design.obj.getKind() == Obj.Var)
                    report_info("Access to variable " + design.obj.getName(), design);
                return;
            }
            catch (NullPointerException e) {
                report_error("Error: designator is not defined", design);
                return;
            }
        } else {
            report_info("Access to" + design.obj.getType().getKind(), design);
        }
    }

    public void visit(OptDesignatorPartManyClass optDesignatorPartManyClass) {
        optDesignatorPartManyClass.obj = optDesignatorPartManyClass.getDesigPart().obj;
    }

    public void visit(DesigPart desigPart) {
        Obj firstLeft = getFirstLeft((OptDesignatorPartManyClass) desigPart.getParent());

        if (firstLeft == Tab.noObj) desigPart.obj = Tab.noObj;
        else {
            if (desigPart.getExpr().struct != Tab.intType)
                report_error("Error: inside [] muse be an Int", desigPart);

            desigPart.obj = new Obj(Obj.Elem, "elem", firstLeft.getType());
        }
    }

    private Obj getFirstLeft(OptDesignatorPartManyClass optDesignatorPartManyClass) {
        if (optDesignatorPartManyClass.getOptDesignatorPart() instanceof OptDesignatorPartEmptyClass) {
            SyntaxNode parent = optDesignatorPartManyClass.getParent();
            while (parent instanceof OptDesignatorPartManyClass)
                parent = parent.getParent();
            String name = getDesignName((Designator) parent);
            return Tab.find(name);
        } else
            return optDesignatorPartManyClass.getOptDesignatorPart().obj;

    }

    public void visit(DesignatorStatementPlusClass designatorStatementPlusClass) {
        if (!checkDesignType(designatorStatementPlusClass.getDesignator()) || designatorStatementPlusClass.getDesignator().obj.getType() != Tab.intType)
            report_error("Error: plus plus require int type", designatorStatementPlusClass);
    }

    public void visit(DesignatorStatementMinusClass designatorStatementMinusClass) {
        if (!checkDesignType(designatorStatementMinusClass.getDesignator()) || designatorStatementMinusClass.getDesignator().obj.getType() != Tab.intType)
            report_error("Error: plus plus require int type", designatorStatementMinusClass);
    }

    public void visit(DesignatorStatementParamsClass designatorStatementParamsClass) {

        if (designatorStatementParamsClass.getDesignator().obj.getKind() != Obj.Meth)
            report_error("method does not exist" + designatorStatementParamsClass.getDesignator().obj.getName(), designatorStatementParamsClass);
        else {
            actPartsRequired = designatorStatementParamsClass.getDesignator().obj.getLocalSymbols();
            if (badParams())
                report_error("Error: bad parameters " + designatorStatementParamsClass.getDesignator().obj.getName(), designatorStatementParamsClass);
            else
                report_info("Method call " + designatorStatementParamsClass.getDesignator().obj.getName(), designatorStatementParamsClass);
        }
        actPartsPassed = null;
        actPartsRequired = null;
    }

    private boolean badParams() {

        if (actPartsPassed!=null && actPartsRequired!=null && actPartsPassed.size() == actPartsRequired.size()) {
            int i = 0;
            for (Obj required : actPartsRequired) {
                if (required.getType() != actPartsPassed.get(i))
                    if (required.getType().getKind() != Struct.Array && actPartsPassed.get(i).getKind() != Struct.Array) {
                        return true;
                    }
                i++;
            }
        }
        return false;
    }

    public void visit(DesignatorStatementAssignClass designatorStatementAssignClass) {
        checkDesignType(designatorStatementAssignClass.getDesignator()); // dodaj if

        Struct tempL = designatorStatementAssignClass.getDesignator().obj.getType();
        report_info("levi tip "+ tempL.getKind(),designatorStatementAssignClass);
        Struct tempR = designatorStatementAssignClass.getExpr().struct;
        report_info("desni tip "+ tempR.getKind(),designatorStatementAssignClass);
        if (!checkTypes(tempL, tempR))
            report_error("Error: can not assign type", designatorStatementAssignClass);

    }

    public void visit(MethodTypeVoidClass methodTypeVoidClass) {
        methodTypeVoidClass.struct = Tab.noType;
        currentType = Tab.noType;
    }

    private boolean tryToDefine(String name, SyntaxNode info) {

        if (Tab.currentScope.findSymbol(name) == null)
            return true;
        report_error("Error: name is already defined in this scope " + name, info);
        return false;
    }

    public void visit(ConstDeclPart constDeclPart) {
        if (tryToDefine(constDeclPart.getName(), constDeclPart))
            if (constDeclPart.getConstVal().struct == currentType)
                constDeclPart.obj = Tab.insert(Obj.Con, constDeclPart.getName(), currentType);
            else report_error("Error: incorrect type for defining const", constDeclPart);

    }

    public void visit(ConstDecl constDecl) {
//        currentType = constDecl.getType().struct;
        currentType = constDecl.getType().struct;
        if (currentType != Tab.intType && currentType != Tab.charType && currentType != MyTab.boolType)
            report_error("Error: const must be Int|Char|Bool", constDecl.getType());
        currentType = null;
    }

    public void visit(OptionalArrayClass optionalArrayClass) {
        isArray = true;
    }

    public void visit(VarDeclPart varDeclPart) {
        if (tryToDefine(varDeclPart.getName(), varDeclPart)) {
            if (isArray) {
                varDeclPart.obj = Tab.insert(Obj.Var, varDeclPart.getName(), new Struct(Struct.Array, currentType));
                isArray = false;
            } else varDeclPart.obj = Tab.insert(Obj.Var, varDeclPart.getName(), currentType);
        }
    }

    @Override
    public void visit(NamespaceFinished namespaceFinished) {
        Iterator<Obj> iterator = Tab.currentScope().getLocals().symbols().iterator();
        int sum = 0;
        while (iterator.hasNext()) {
            Obj next = iterator.next();
            sum+=next.getLocalSymbols().size();
        }
        for (int i = 0; i < sum; i++) {
            Tab.insert(Obj.Var, "#"+i, Tab.noType);

        }
    }

    public void visit(VarDecl varDecl) {
        currentType = varDecl.getType().struct;
    }

    public void visit(FormPars formPars) {
        Struct type = formPars.getType().struct;
        if (isArray)
            type = new Struct(Struct.Array, type);
        Tab.insert(Obj.Var, formPars.getName(), type);
        formalParamCount++;
        isArray = false;
    }

    public void visit(OptActParsEmptyClass optActParsEmptyClass) {
        actPartsPassed = new ArrayList<>();
    }

    private Obj findInCurrentScope(String localSymbol) {
        Obj result = Tab.currentScope.findSymbol(localSymbol);
        if (result == null)
            result = Tab.noObj;
        return result;
    }

    public void visit(NamespaceName namespaceName) {
        Obj namespaceObj = findInCurrentScope(namespaceName.getNamespaceName());

        if (namespaceObj == Tab.noObj) {
            namespaceName.obj = currentNamespace = Tab.insert(Obj.Type, namespaceName.getNamespaceName(), new Struct(Struct.None));
            namespaceName.obj.setLevel(lastNamespaceLevel++);
        } else
            report_error("Duplicate namespace", namespaceName);
        Tab.openScope();
    }

    public void visit(Namespace namespace) {
        namespace.obj = namespace.getNamespaceName().obj;
        Tab.chainLocalSymbols(namespace.obj);
        Tab.closeScope();
        currentNamespace = null;
    }

    public void visit(StatementForClass statementForClass) {
        inFor = true;
        breakCount++;
        continueCount++;
    }

}