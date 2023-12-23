package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;

import java.util.*;

public class CodeGenerator extends VisitorAdaptor {

    private int mainPc;
    private int pc = Code.pc;

    private final Stack<Integer> whereAmI = new Stack<>();
    private boolean isInfor = false;
    private final Stack<List<Integer>> breakStack = new Stack<>();
    private final Stack<List<Integer>> continueStack = new Stack<>();

    private final Stack<List<Integer>> jumpStack = new Stack<>();
    private final Stack<Integer> forStack = new Stack<>();
    private final Integer inNowhere = 0;
    private boolean isInGlobalDeclartaion = false;

    {
        whereAmI.push(inNowhere);
    }

    private final Stack<Integer> ifStack = new Stack<>();

    private final Stack<Integer> elseStack = new Stack<>();

    private static final int inIfCond = 1;

    private static final int inForLoop = 2;

    public int getMainPc() {
        return mainPc;
    }

    public void visit(StatementPrintClass statementPrintClass) {
        Code.load(new Obj(Obj.Con, "width", Tab.intType, 1, 0));
        if (statementPrintClass.getExpr().struct == Tab.intType
                || statementPrintClass.getExpr().struct == MyTab.boolType) {
            Code.put(Code.print);
        } else {
            Code.put(Code.bprint);
        }
    }

    public void visit(StatementReadClass statementReadClass) {
        Code.put(Code.read);
        if (statementReadClass.getDesignator().obj.getType().getKind() == 3) {
            Code.put(Code.astore);
        } else {
            Code.store(statementReadClass.getDesignator().getOptNamespace().obj);
        }


    }

    public void visit(StatementIfClass statementIfClass) {
        Code.fixup(ifStack.pop());
    }

    public void visit(StatementIfElseClass statementIfElseClass) {
        Code.fixup(elseStack.pop());
    }

    public void visit(If i) {
        whereAmI.push(inIfCond);
    }

    public void visit(Else els) {

        Code.put(Code.jmp);
        Code.put2(0);
        elseStack.push(Code.pc - 2);

        Code.fixup(ifStack.pop());
    }

    private int constValue(SyntaxNode type) {
        if (type instanceof ConstValNumClass)
            return ((ConstValNumClass) type).getN1();
        if (type instanceof ConstValCharClass)
            return ((ConstValCharClass) type).getC1().charAt(1);
        if (type instanceof ConstValBooCllass)
            if (((ConstValBooCllass) type).getB1().equals("true"))
                return 1;
            else
                return 0;
        return -1;
    }

    public void visit(ConstDeclPart constDeclPart) {
        constDeclPart.obj.setAdr(constValue(constDeclPart.getConstVal()));
        Code.load(constDeclPart.obj);
    }

    public void visit(StatementBreakClass statementBreakClass) {
        Code.putJump(0);
        breakStack.peek().add(Code.pc - 2);
    }

    public void visit(StatementContinueClass statementContinueClass) {
        Code.putJump(0);
        continueStack.peek().add(Code.pc - 2);
    }

    private final List<Integer> andList = new ArrayList<>();
    private final List<Integer> orList = new ArrayList<>();

    private void or() {
        Code.put(Code.jmp);
        if (whereAmI.peek() == inForLoop) // TODO - ako bude pucalo ovo ce Milena da sredi
            Code.put2(forStack.peek() - Code.pc + 1);
        else {
            Code.put2(0);
            orList.add(Code.pc - 2);
        }

        andList.forEach(Code::fixup);
        andList.clear();
    }

    public void visit(ConditionStatement conditionStatement) {
        condition(0);
        if (whereAmI.peek() == inIfCond)
            whereAmI.pop();
    }

    private void and() {
        andList.add(Code.pc - 2);
    }

    public void visit(DesignatorStatementPlusClass designatorStatementPlusClass) {
        if (isInfor)
            return;
        //Code.load(designatorStatementPlusClass.getDesignator().obj);
        Code.put(Code.const_1);
        Code.put(Code.add);
        Code.store(designatorStatementPlusClass.getDesignator().obj);
    }

    public void visit(DesignatorStatementMinusClass designatorStatementMinusClass) {
        if (isInfor) return;
        Code.put(Code.const_1);
        Code.put(Code.sub);
        Code.store(designatorStatementMinusClass.getDesignator().obj);
    }

    public void visit(OptMin neg) {
        //Code.put(Code.neg);
    }

    public void visit(DesignatorStatementAssignClass designatorStatementAssignClass) {
        if (isInfor) return;
        // ono sto je u akumulatoru smesta se na trazenu lokaciju
        if (designatorStatementAssignClass.getDesignator().obj.getType().getKind() == 3 && designatorStatementAssignClass.getDesignator().getOptDesignatorPart() instanceof OptDesignatorPartManyClass)
            if (designatorStatementAssignClass.getDesignator().obj.getType() == Tab.charType)
                Code.put(Code.bastore);
            else
                Code.put(Code.astore);
        else
            Code.store(designatorStatementAssignClass.getDesignator().obj);
    }

    private void condition(int adr) {
        if (whereAmI.peek() == inIfCond) {
            Code.put(Code.jmp);
            Code.put2(0);

            ifStack.push(Code.pc - 2);

            orList.forEach(o -> {
                Code.fixup(o); // postavlja then
            });
        }

        orList.clear();
    }

    public void visit(ConditionOneClass conditionOneClass) {
        or();
    }

    public void visit(ConditionManyClass conditionManyClass) {
        or();
    }

    public void visit(CondTermManyClass condTermManyClass) {
        and();
    }

    public void visit(CondTermOneClass condTermOneClass) {
        and();
    }

    public void visit(CondFactManyClass condFactManyClass) {
        SyntaxNode type = condFactManyClass.getRelop();

        if (type instanceof RelopEqualstoClass)
            Code.putFalseJump(Code.eq, 0);
        if (type instanceof RelopDifferentClass)
            Code.putFalseJump(Code.ne, 0);
        if (type instanceof RelopLessClass)
            Code.putFalseJump(Code.lt, 0);
        if (type instanceof RelopElessClass)
            Code.putFalseJump(Code.le, 0);
        if (type instanceof RelopGreaterClass)
            Code.putFalseJump(Code.gt, 0);
        if (type instanceof RelopEgreaterClass)
            Code.putFalseJump(Code.ge, 0);
    }

    public void visit(CondFactOneClass condFactOneClass) {
        Code.put(Code.const_1);
        Code.putFalseJump(Code.eq, 0);
    }

    public void visit(FactorConstValClass factorConstValClass) { // za bezimene konstante 5 'a' false
        Obj con = Tab.insert(Obj.Con, "$", factorConstValClass.struct);
        con.setAdr(constValue(factorConstValClass.getConstVal()));
        Code.load(con);
    }

    public void visit(AddTermManyClass addTermManyClass) {
        SyntaxNode op = addTermManyClass.getAddop();
        if (op instanceof AddopPlusClass)
            Code.put(Code.add);
        if (op instanceof AddopMinusClass)
            Code.put(Code.sub);
    }

    public void visit(TermManyClass termManyClass) {
        SyntaxNode type = termManyClass.getMulop();

        if (type instanceof MulopMulClass)
            Code.put(Code.mul);
        if (type instanceof MulopDivClass)
            Code.put(Code.div);
        if (type instanceof MulopModClass)
            Code.put(Code.rem);
    }

    public void visit(Expr expr) {
        if (expr.getOptMinus() instanceof OptMin)
            Code.put(Code.neg);
    }

    public void visit(FactorNewTypeExprClass factorNewTypeExprClass) {
        Code.put(Code.newarray);
        if (factorNewTypeExprClass.getType().struct == Tab.charType)
            Code.put(0);
        else
            Code.put(1);
    }

    public void visit(MethodDecl methodDecl) {
        Code.put(Code.exit);
        Code.put(Code.return_);
    }

    public void visit(FactorParenParsClass factorParenParsClass) {
        if (factorParenParsClass.getOptFactorParenPars() instanceof OptFactorParenParsClass)
            setFunCall(factorParenParsClass.getDesignator());
    }

    private void setFunCall(Designator designator) {
        if (designator.getOptDesignatorPart() instanceof OptDesignatorPartEmptyClass) {
            Code.put(Code.call);
            Code.put2(designator.obj.getAdr() - Code.pc + 1);
        } else {

            Obj secondToLast = penultimate(designator);
            if (secondToLast.getKind() == Obj.Elem)
                Code.load(MyTab.tempHelp);
            else
                Code.load(secondToLast);
        }
    }

    private Obj penultimate(Designator designator) {
        if (((OptDesignatorPartManyClass) designator.getOptDesignatorPart()).getOptDesignatorPart() instanceof OptDesignatorPartEmptyClass)
            return designator.obj;
        else
            return ((OptDesignatorPartManyClass) designator.getOptDesignatorPart()).getOptDesignatorPart().obj;
    }

    public void visit(OptDesignatorPartManyClass optDesignatorPartManyClass) {
        Obj o;
        if (optDesignatorPartManyClass.getParent() instanceof Designator)
            o = ((Designator) optDesignatorPartManyClass.getParent()).obj;
        else
            o = ((OptDesignatorPartManyClass) optDesignatorPartManyClass.getParent()).obj;
    }

    public void visit(MethodTypeName methodTypeName) {

        if (methodTypeName.getMethodName().equals("main")) {
            mainPc = Code.pc;
        }

        methodTypeName.obj.setAdr(Code.pc);

        int formalParamCnt = methodTypeName.obj.getLevel();
        int localCnt = methodTypeName.obj.getLocalSymbols().size();

        Code.put(Code.enter);
        Code.put(formalParamCnt);
        Code.put(localCnt);
    }

    public void visit(DesignatorStatementParamsClass designatorStatementParamsClass) {
        if (isInfor) return;
        setFunCall(designatorStatementParamsClass.getDesignator());

        if (designatorStatementParamsClass.getDesignator().obj.getType() != Tab.noType)
            Code.put(Code.pop);

    }

    public void visit(VarDeclPart varDeclPart) {
        SyntaxNode syntaxNode = varDeclPart.getParent();
        while (syntaxNode instanceof VarDeclList
//                || sn instanceof VarListComma
        )
            syntaxNode = syntaxNode.getParent();
    }

    public void visit(EndOfGlobal endOfGlobal) {
        isInGlobalDeclartaion = true;
    }

    public void visit(OptNamespaceEmptyClass optNamespaceEmptyClass) {
        if (!isInGlobalDeclartaion) {
            if (optNamespaceEmptyClass.obj.getKind() == Obj.Meth) {
                Code.put(Code.load_n);
            }
        }
        // da li je poslednji?
        if (((Designator) optNamespaceEmptyClass.getParent()).getOptDesignatorPart() instanceof OptDesignatorPartEmptyClass) {
            SyntaxNode parent = optNamespaceEmptyClass.getParent().getParent();
            if (!(parent.getParent() instanceof DesignatorStatementOpt && isInfor)
                    && !(parent instanceof DesignatorStatementAssignClass) // ako je dodela vrednosti nije mi potrebno da dohvatam vrednost, ali ako ima delove, onda jeste
                    && !(parent instanceof DesignatorStatementParamsClass)
                    && !(parent instanceof FactorParenParsClass && ((FactorParenParsClass) parent).getOptFactorParenPars() instanceof OptFactorParenParsClass) // poziv funkcije u izrazu
                    && !(parent instanceof StatementReadClass))
                Code.load(optNamespaceEmptyClass.obj);
        } else
            Code.load(optNamespaceEmptyClass.obj);
    }

    public void visit(OptNamespaceClass optNamespaceClass) {
        if (!isInGlobalDeclartaion) {
            if (optNamespaceClass.obj.getKind() == Obj.Meth) {
                Code.put(Code.load_n);
            }
        }
        if (optNamespaceClass.obj.getKind() == Obj.Meth) {
            return;
        }
        // da li je poslednji?
        if (((Designator) optNamespaceClass.getParent()).getOptDesignatorPart() instanceof OptDesignatorPartEmptyClass) {
            SyntaxNode parent = optNamespaceClass.getParent().getParent();
            if (!(parent instanceof DesignatorStatementAssignClass) // ako je dodela vrednosti nije mi potrebno da dohvatam vrednost, ali ako ima delove, onda jeste
                    && !(parent instanceof DesignatorStatementParamsClass)
                    && !(parent instanceof FactorParenParsClass && ((FactorParenParsClass) parent).getOptFactorParenPars() instanceof OptFactorParenParsClass) // poziv funkcije u izrazu
                    && !(parent instanceof StatementReadClass)) ;
            Code.load(optNamespaceClass.obj);
        } else
            Code.load(optNamespaceClass.obj);

    }

    public void visit(DesigPart desigPart) {
        if (desigPart.getParent().getParent() instanceof Designator) {
            SyntaxNode desig = desigPart.getParent().getParent();
            SyntaxNode parent = desig.getParent();
            if (!(parent instanceof DesignatorStatementAssignClass) // ako je dodela vrednosti nije mi potrebno da dohvatam vrednost, ali ako ima delove, onda jeste
                    && !(parent instanceof DesignatorStatementParamsClass)
                    && !(parent instanceof FactorParenParsClass && ((FactorParenParsClass) parent).getOptFactorParenPars() instanceof OptFactorParenParsClass) // poziv funkcije u izrazu
                    && !(parent instanceof StatementReadClass))
                if (desigPart.obj.getType() == Tab.charType)
                    Code.put(Code.baload);
                else
                    Code.put(Code.aload);
        } else {
            if (desigPart.obj.getType() == Tab.charType)
                Code.put(Code.baload);
            else
                Code.put(Code.aload);

        }
    }

    // TODO - #############################################################
    // StatementForClass
    public void visit(StartOfCondition startOfCondition) {
        forStack.push(Code.pc);
        isInfor = true;
    }

    public void visit(EndOfCondition endOfCondition) {
        isInfor = false;
    }

    public void visit(For forTerm) {
        whereAmI.push(inForLoop);
        breakStack.push(new ArrayList<Integer>());
        continueStack.push(new ArrayList<Integer>());
        jumpStack.push(new ArrayList<Integer>());
    }

    @Override
    public void visit(CondFactOptionalOneClass condFactOptionalOneClass) {
        jumpStack.peek().add(Code.pc - 2);
    }

    public void visit(StatementForClass statementForClass) {
        continueStack.pop().forEach(Code::fixup);
        statementForClass.getDesignatorStatementOpt1().traverseBottomUp(this);
        Code.put(Code.jmp);
        Code.put2(forStack.pop() - Code.pc + 1);
        jumpStack.pop().forEach(Code::fixup);
        breakStack.pop().forEach(Code::fixup);
        whereAmI.pop();
    }


    // TODO - #############################################################
}

