package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

import java.util.*;

public class CodeGenerator extends VisitorAdaptor {

    private int mainPc;
    private final List<Integer> andList = new ArrayList<>();
    private final List<Integer> orList = new ArrayList<>();

    private final Stack<WhereAmI> whereAmI = new Stack<>();

    private enum WhereAmI {inNowhere, inFor, inIf}

    private int unpackingCnt = 0;

    {
        whereAmI.push(WhereAmI.inNowhere);
    }

    private final Stack<List<Integer>> breakStack = new Stack<>();
    private final Stack<List<Integer>> continueStack = new Stack<>();
    private String currentNamespace;
    private final Stack<Integer> ifStack = new Stack<>();

    static class ForInfo {
        int forEnd;
        int condition;
        int forBody;
        int increment;
    }

    private final Stack<ForInfo> forStack = new Stack<>();

    private final Stack<Integer> elseStack = new Stack<>();
    private boolean isInIf;


    public int getMainPc() {
        return mainPc;
    }

    @Override
    public void visit(DesignStmManyStart visitor) {
        unpackingCnt = 0;
    }

    @Override
    public void visit(DesignStmPartOne visitor) {
        ++unpackingCnt;
    }

    @Override
    public void visit(DesignStmPartMany visitor) {

        SyntaxNode designStmMany = visitor.getParent();
        while (designStmMany instanceof DesignStmPartMany || designStmMany instanceof DesignStmPartOne)
            designStmMany = designStmMany.getParent();
        Designator designator = ((DesignStmMany) designStmMany).getDesignator1();
        Code.load(designator.obj);
        Code.loadConst(unpackingCnt++);
        Code.put(Code.aload);

        if (visitor.getDesignator().obj.getKind() == Obj.Elem)
            Code.put(Code.astore);
        else
            Code.store(visitor.getDesignator().obj);
    }

    @Override
    public void visit(DesignStmMany visitor) {

        Obj objIndexStore = Tab.insert(Obj.Var, "indexStore", new Struct(Struct.Int));
        Code.loadConst(0);
        Code.store(objIndexStore);

        Obj objUnpackingCnt = Tab.insert(Obj.Var, "unpackingCnt", new Struct(Struct.Int));
        Code.loadConst(unpackingCnt);
        Code.store(objUnpackingCnt);

        Obj objLeftDesignSize = Tab.insert(Obj.Var, "leftDesignSize", new Struct(Struct.Int));
        Code.load(visitor.getDesignator().obj);
        Code.put(Code.arraylength);
        Code.store(objLeftDesignSize);

        Obj objRightDesignSize = Tab.insert(Obj.Var, "rightDesignSize", new Struct(Struct.Int));
        Code.load(visitor.getDesignator1().obj);
        Code.put(Code.arraylength);
        Code.store(objRightDesignSize);

        int testRuntime = Code.pc;
        Code.load(objUnpackingCnt);
        Code.load(objRightDesignSize);
        Code.putFalseJump(Code.ge, 0); // fix up testLimit
        int testLimit = Code.pc - 2;

        Code.load(objIndexStore);
        Code.load(objLeftDesignSize);
        Code.putFalseJump(Code.lt, 0);// fix up endCorrect
        int endCorrect = Code.pc - 2;

        // endRuntime
        Code.put(Code.trap);
        Code.put(1);

        Code.fixup(testLimit);
        Code.load(objIndexStore);
        Code.load(objLeftDesignSize);
        Code.putFalseJump(Code.lt, 0); // fix up endCorrect
        int endCorrect2 = Code.pc - 2;

//        testRuntime:
//            unpackingCnt >= objRightDesignSize
//            jmpF testLimit
//            unpackingCnt >= objLeftDesignSize
//            jmpF endCorrect
//        endRuntime:
//            ...code...
//        testLimit:
//            unpackingCnt < objLeftDesignSize
//            jmpF endCorrect
//        loop:

        // load 0                       -> OK
        // store indexStore             -> OK
        // load unpackingCnt            -> OK
        // store unpackingCnt           -> OK
        // load designator1.size        -> OK
        // testRuntime:                 -> OK
            // load unpackingCnt        -> OK
            // load objRightDesignSize  -> OK
            // >=                       -> OK
            // jmpF testLimit           -> OK
            // load unpackingCnt        -> OK
            // load objLeftDesignSize   -> OK
            // >=                       -> OK
            // jmpF endCorrect          -> OK
        // endRuntime:                  -> OK
            // put trap                 -> OK
            // put 1                    -> OK
        // testLimit:                   -> OK
            // load unpackingCnt        -> OK
            // load objLeftDesignSize   -> OK
            // >=                       -> OK
            // jmpF endCorrect          -> OK
        // loop:                        -> OK
            // load designator          -> OK
            // load indexStore          -> OK
            // load designator1         -> OK
            // load unpackingCnt        -> OK
            // aload                    -> OK
            // astore                   -> OK
            // load indexStore          -> OK
            // inc                      -> OK
            // store indexStore         -> OK
            // load unpackingCnt        -> OK
            // inc                      -> OK
            // store unpackingCnt       -> OK
            // jmp testRuntime
        // endCorrect

        Code.load(visitor.getDesignator().obj);
        Code.load(objIndexStore);
        Code.load(visitor.getDesignator1().obj);
        Code.load(objUnpackingCnt);
        Code.put(Code.aload);
        Code.put(Code.astore);
        Code.load(objIndexStore);
        Code.loadConst(1);
        Code.put(Code.add);
        Code.store(objIndexStore);
        Code.load(objUnpackingCnt);
        Code.loadConst(1);
        Code.put(Code.add);
        Code.store(objUnpackingCnt);
        Code.put(Code.jmp);
        Code.put2(testRuntime - Code.pc + 1);
        Code.fixup(endCorrect);
        Code.fixup(endCorrect2);

    }

    @Override
    public void visit(NamespaceName visitor) {
        currentNamespace = visitor.getName();
    }

    @Override
    public void visit(Namespace visitor) {
        currentNamespace = "";
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
//        if (!forStack.empty()) {
//            forStack.peek().forEnd = Code.pc - 2;
//        }
        if (whereAmI.peek() == WhereAmI.inFor)
            forStack.peek().forEnd = Code.pc - 2;
    }

    @Override
    public void visit(CondFactOne visitor) {
        Code.put(Code.const_1);
        Code.putFalseJump(Code.eq, 0);
        if (whereAmI.peek() == WhereAmI.inFor)
            forStack.peek().forEnd = Code.pc - 2;
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
        visitor.obj = MyTab.myFind(SemanticPass.prepareSymbol(visitor.getName(), currentNamespace));
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

    @Override

    public void visit(ScopeLocal visitor) {
        if (Tab.currentScope().findSymbol(visitor.obj.getName()) != null)
            if (visitor.obj.getKind() == Obj.Meth)
                Code.loadConst(0);

        if (visitor.obj.getKind() == Obj.Meth)
            return;

        if (visitor.obj.getType().getKind() == 3 && ((Designator) visitor.getParent()).getMatrixOpt() instanceof MatrixZero)
            return;

        SyntaxNode grandFather = visitor.getParent().getParent();
        if (grandFather instanceof DesignStmPartMany || grandFather instanceof DesignStmMany)
            return;
        if (((Designator) visitor.getParent()).getMatrixOpt() instanceof MatrixZero) {
            if (!(grandFather instanceof DesignStmAssign)
                    && !(grandFather instanceof DesignStmParen)
                    && !(grandFather instanceof FactorParenPars
                    && ((FactorParenPars) grandFather).getFactorParenParsOpt() instanceof ParenPars)
                    && !(grandFather instanceof StatementRead)
            )
                Code.load(visitor.obj);
        } else
            Code.load(visitor.obj);
    }

    @Override

    public void visit(ScopeNamespace visitor) {
        if (visitor.obj.getKind() == Obj.Meth)
            return;

        if (visitor.obj.getType().getKind() == 3 && ((Designator) visitor.getParent()).getMatrixOpt() instanceof MatrixZero)
            return;


        SyntaxNode grandFather = visitor.getParent().getParent();

        if ((grandFather instanceof DesignStmPartMany || grandFather instanceof DesignStmMany) && visitor.obj.getType().getKind() != Struct.Array)
            return;

        if (!(grandFather instanceof DesignStmAssign)
                && !(grandFather instanceof DesignStmParen)
                && !(grandFather instanceof FactorParenPars
                && ((FactorParenPars) grandFather).getFactorParenParsOpt() instanceof ParenPars)
                && !(grandFather instanceof StatementRead)
        )
            Code.load(visitor.obj);
        else
            Code.load(visitor.obj);

    }

    @Override
    public void visit(StatementPrint stmPrint) {
        if (stmPrint.getExpr().struct == Tab.intType || stmPrint.getExpr().struct == MyTab.boolType) {
            Code.loadConst(1);
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
        whereAmI.push(WhereAmI.inIf);
    }

    @Override
    public void visit(Else visitor) {

        Code.put(Code.jmp);
        Code.put2(0);
        elseStack.push(Code.pc - 2);

        Code.fixup(ifStack.pop());
    }

    @Override
    public void visit(For visitor) {
        forStack.push(new ForInfo());
        breakStack.push(new ArrayList<>());
        continueStack.push(new ArrayList<>());
        whereAmI.push(WhereAmI.inFor);
    }

    @Override
    public void visit(ForCondStart visitor) {
        forStack.peek().condition = Code.pc;
    }

    @Override
    public void visit(CondFactOptOne visitor) {
        Code.putJump(0);
        forStack.peek().forBody = Code.pc - 2;
        forStack.peek().increment = Code.pc;
    }

    @Override
    public void visit(ForCondEnd visitor) {

        Code.put(Code.jmp);
        Code.put2(forStack.peek().condition - Code.pc + 1);
        Code.fixup(forStack.peek().forBody);

        // for :
        // StatementFor
        // condition: <- save in local
        // test CondFactOpt
        // jump forEnd <- call fixup
        // jump forBody <- call fixup
        // increment: <- save in local
        // i++
        // jump condition <- read from local
        // forBody:
        // ...code...
        // StatementFor:
        // jump increment <- read from local
        // forEnd:
        //

    }

    @Override
    public void visit(StatementFor visitor) {
        continueStack.pop().forEach(Code::fixup);
        Code.put(Code.jmp);
        Code.put2(forStack.peek().increment - Code.pc + 1);
        Code.fixup(forStack.peek().forEnd);
        forStack.pop();
        breakStack.pop().forEach(Code::fixup);
        whereAmI.pop();
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
                    && !(parent instanceof StatementRead)
                    && !(parent instanceof DesignStmPartMany)
            )
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

    @Override
    public void visit(StatementBreak visitor) {
        Code.putJump(0);
        breakStack.peek().add(Code.pc - 2);
    }

    @Override
    public void visit(StatementContinue visitor) {
        Code.putJump(0);
        continueStack.peek().add(Code.pc - 2);
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
        Code.put(Code.jmp);
        Code.put2(0);
        ifStack.push(Code.pc - 2);
        for (Integer o : orList)
            Code.fixup(o);
        orList.clear();
        whereAmI.pop();
    }

}

