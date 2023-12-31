package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;

import java.util.*;

public class CodeGenerator extends VisitorAdaptor {

    private int mainPc;
    private final List<Integer> andList = new ArrayList<>();
    private final List<Integer> orList = new ArrayList<>();
    private int staticSizeCnt;

    private Stack<Integer> ifStack = new Stack<>();

    private Stack<Integer> elseStack = new Stack<>();
    private boolean isInIf;
    private boolean isInFor;
    private boolean isUnpackingStart;

    public int getMainPc() {
        return mainPc;
    }

    @Override
    public void visit(TermMany visitor) {
        SyntaxNode type = visitor.getMulOp();

        if (type instanceof MulOpMul)
            Code.put(Code.mul);
        if (type instanceof MulOpDiv)
            Code.put(Code.div);
        if (type instanceof MulOpMod)
            Code.put(Code.rem);
    }

    @Override
    public void visit(AddTermMany visitor) {
        SyntaxNode op = visitor.getAddOp();
        if (op instanceof AddOpPlus)
            Code.put(Code.add);
        if (op instanceof AddOpMinus)
            Code.put(Code.sub);
    }

    @Override
    public void visit(CondFactMany visitor) {
        SyntaxNode type = visitor.getRelOp();

        if (type instanceof RelOpEqualsTo)
            Code.putFalseJump(Code.eq, 0);
        if (type instanceof RelOpDifferent)
            Code.putFalseJump(Code.ne, 0);
        if (type instanceof RelOpLess)
            Code.putFalseJump(Code.lt, 0);
        if (type instanceof RelOpEless)
            Code.putFalseJump(Code.le, 0);
        if (type instanceof RelOpGreater)
            Code.putFalseJump(Code.gt, 0);
        if (type instanceof RelOpEgreater)
            Code.putFalseJump(Code.ge, 0);
    }

    @Override
    public void visit(Expr visitor) {
        if (visitor.getMinusOpt() instanceof Minus)
            Code.put(Code.neg);
    }

    @Override
    public void visit(ConditionOne visitor) {
        or();
    }

    @Override
    public void visit(ConditionMany visitor) {
        or();
    }

    private void or() {
        Code.put(Code.jmp);
        Code.put2(0);
        orList.add(Code.pc - 2);
        andList.forEach(Code::fixup);
        andList.clear();
    }

    private void and() {
        andList.add(Code.pc - 2);
    }

    @Override
    public void visit(CondTermMany visitor) {
        and();
    }

    @Override
    public void visit(CondTermOne visitor) {
        and();
    }

    @Override
    public void visit(MethodName visitor) {

        if ("main".equalsIgnoreCase(visitor.getName()))
            mainPc = Code.pc;

        visitor.obj.setAdr(Code.pc);

        int formalParamCnt = visitor.obj.getLevel();
        int localCnt = visitor.obj.getLocalSymbols().size();

        Code.put(Code.enter);
        Code.put(formalParamCnt);
        Code.put(localCnt);
    }

    @Override
    public void visit(MethodDecl visitor) {
        Code.put(Code.exit);
        Code.put(Code.return_);
    }

    @Override
    public void visit(ConstDef visitor) {
        visitor.obj.setAdr(constValue(visitor.getConstVal()));
        Code.load(visitor.obj);
    }

    private int constValue(SyntaxNode type) {
        if (type instanceof ConstNum)
            return ((ConstNum) type).getN1();
        if (type instanceof ConstChar)
            return ((ConstChar) type).getC1().charAt(1);
        if (type instanceof ConstBool)
            if (((ConstBool) type).getB1().equals("true"))
                return 1;
            else
                return 0;
        return -1;
    }

    @Override
    public void visit(FactorConstVal visitor) {
        Obj con = Tab.insert(Obj.Con, "$", visitor.struct);
        con.setAdr(constValue(visitor.getConstVal()));
        Code.load(con);
    }

    @Override
    public void visit(DesignStmAssign visitor) {
        if (visitor.getDesignator().obj.getKind() == Obj.Elem)
            if (visitor.getDesignator().obj.getType() == Tab.charType)
                Code.put(Code.bastore);
            else
                Code.put(Code.astore);
        else
            Code.store(visitor.getDesignator().obj);
    }

    public void visit(ScopeLocal visitor) {
        if (Tab.currentScope().findSymbol(visitor.obj.getName()) != null)
            if (visitor.obj.getKind() == Obj.Meth)
                Code.loadConst(0);

        if (visitor.obj.getKind() == Obj.Meth)
            return;

//        if (isUnpackingStart)
//            return;

        if (visitor.obj.getType().getKind() == 3 && ((Designator) visitor.getParent()).getMatrixOpt() instanceof MatrixZero)
            return;

        SyntaxNode parent = visitor.getParent().getParent();

//        if (parent instanceof DesignatorStatePartManyClass || parent instanceof DesignatorStatementManyClass) {
//            isUnpackingStart = true;
//            return;
//        }

//        if (((Designator) visitor.getParent()).getOptDesignatorPart() instanceof OptDesignatorPartEmptyClass) {
//            if (!(parent.getParent() instanceof DesignatorStatementOpt && isInfor)
//                    && !(parent instanceof DesignatorStatementAssignClass) // 2
//                    && !(parent instanceof DesignatorStatementParamsClass) // 1
//                    && !(parent instanceof FactorParenParsClass // 4
//                    && ((FactorParenParsClass) parent).getOptFactorParenPars() instanceof OptFactorParenParsClass) // 5
//                    && !(parent instanceof StatementReadClass)) // 3
//                Code.load(visitor.obj);

        if (!(parent instanceof DesignStmAssign) // 2
                && !(parent instanceof DesignStmParen) // 1
                && !(parent instanceof FactorParenPars // 4
                && ((FactorParenPars) parent).getFactorParenParsOpt() instanceof ParenPars) // 5
                && !(parent instanceof StatementRead) // 3
        )
            Code.load(visitor.obj);

        else
            Code.load(visitor.obj);
    }

// TODO ################################################ Start of Dumb code

    public void visit(ScopeNamespace visitor) {
        if (visitor.obj.getKind() == Obj.Meth)
            return;

//        if (isUnpackingStart)
//            return;

        if (visitor.obj.getType().getKind() == 3 && ((Designator) visitor.getParent()).getMatrixOpt() instanceof MatrixZero)
            return;

        SyntaxNode parent = visitor.getParent().getParent();
//        if (parent instanceof DesignatorStatePartManyClass || parent instanceof DesignatorStatementManyClass) {
//            isUnpackingStart = true;
//            return;
//        }

        if (!(parent instanceof DesignStmAssign) // 2
                && !(parent instanceof DesignStmParen) // 1
                && !(parent instanceof FactorParenPars // 4
                && ((FactorParenPars) parent).getFactorParenParsOpt() instanceof ParenPars) // 5
                && !(parent instanceof StatementRead) // 3
        )
            Code.load(visitor.obj);

    }

// TODO ################################################ End of Dumb code

    @Override
    public void visit(StatementPrint stmPrint) {
        if (stmPrint.getExpr().struct == Tab.intType || stmPrint.getExpr().struct == MyTab.boolType) {
            Code.loadConst(5);
            Code.put(Code.print);
        } else {
            Code.loadConst(1);
            Code.put(Code.bprint);
        }
    }

    @Override
    public void visit(StatementRead stmRead) {
        Code.put(Code.read);
        Code.store(stmRead.getDesignator().obj);
    }

    @Override
    public void visit(If visitor) {
        isInIf = true;
    }

    @Override
    public void visit(Else visitor) {

        Code.put(Code.jmp);
        Code.put2(0);
        elseStack.push(Code.pc - 2);

        Code.fixup(ifStack.pop());
    }

    @Override
    public void visit(StatementIf visitor) {
        Code.fixup(ifStack.pop());
    }

    @Override
    public void visit(StatementIfElse visitor) {
        Code.fixup(elseStack.pop());
    }

    @Override
    public void visit(MatrixMany visitor) {
        Obj obj;
        if (visitor.getParent() instanceof Designator)
            obj = ((Designator) visitor.getParent()).obj;
        else
            obj = ((MatrixMany) visitor.getParent()).obj;
    }

    @Override
    public void visit(FactorNewArray factorNewArray) {
        Code.put(Code.newarray);
        if (factorNewArray.getType().struct == Tab.charType)
            Code.put(0);
        else
            Code.put(1);
    }

    @Override
    public void visit(ArraySize visitor) {
        if (visitor.getParent().getParent() instanceof Designator) {
            SyntaxNode designator = visitor.getParent().getParent();
            SyntaxNode parent = designator.getParent();
            if (!(parent instanceof DesignStmAssign)
                    && !(parent instanceof DesignStmParen)
                    && !(parent instanceof FactorParenPars && ((FactorParenPars) parent).getFactorParenParsOpt() instanceof ParenPars)
                    && !(parent instanceof StatementRead))
                if (visitor.obj.getType() == Tab.charType)
                    Code.put(Code.baload);
                else
                    Code.put(Code.aload);
        } else {
            if (visitor.obj.getType() == Tab.charType)
                Code.put(Code.baload);
            else
                Code.put(Code.aload);
        }

    }

    private Obj oneBeforeLast(Designator visitor) {
        if (((MatrixMany) visitor.getMatrixOpt()).getMatrixOpt() instanceof MatrixZero)
            return visitor.getScope().obj;
        else
            return ((MatrixMany) visitor.getMatrixOpt()).getMatrixOpt().obj;
    }

    private void setFunCall(Designator designator) {
        if (designator.getMatrixOpt() instanceof MatrixZero) {
            Code.put(Code.call);
            Code.put2(designator.obj.getAdr() - Code.pc + 1);
        } else {
            Obj secondToLast = oneBeforeLast(designator);
            if (secondToLast.getKind() == Obj.Elem)
                Code.load(MyTab.tempHelp);
            else
                Code.load(secondToLast);
        }
    }

    @Override
    public void visit(DesignStmParen visitor) {
        setFunCall(visitor.getDesignator());

        if (visitor.getDesignator().obj.getType() != Tab.noType)
            Code.put(Code.pop);

    }

    @Override
    public void visit(FactorParenPars visitor) {
        if (visitor.getFactorParenParsOpt() instanceof ParenPars)
            setFunCall(visitor.getDesignator());
    }

    @Override
    public void visit(DesignStmPlus plusPlus) {
        Code.put(Code.const_1);
        Code.put(Code.add);
        Code.store(plusPlus.getDesignator().obj);
    }

    @Override
    public void visit(DesignStmMinus minusMinus) {
        Code.put(Code.const_1);
        Code.put(Code.sub);
        Code.store(minusMinus.getDesignator().obj);
    }

    @Override
    public void visit(StatementCondition visitor) {
        if (isInIf) {
            Code.put(Code.jmp);
            Code.put2(0);
            ifStack.push(Code.pc - 2);
            for (Integer o : orList)
                Code.fixup(o);
        }
        orList.clear();
    }


}

