package rs.ac.bg.etf.pp1;

import rs.etf.pp1.symboltable.concepts.Struct;
import rs.etf.pp1.symboltable.Tab;
import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.mj.runtime.Code;

import java.util.*;

public class CodeGenerator extends VisitorAdaptor {

    private final List<Integer> listOfAnds = new ArrayList<>();
    private int mainPc;
    private final List<Integer> listOfOrs = new ArrayList<>();

    private enum iAmInside {NOWHERE, FOR, IF}

    private final Stack<iAmInside> lastBlock = new Stack<>();
    private int counterOfUnpack = 0;

    {
        lastBlock.push(iAmInside.NOWHERE);
    }

    private final Stack<List<Integer>> breaks = new Stack<>();
    private final Stack<List<Integer>> continues = new Stack<>();
    private String currNamespace;
    private final Stack<Integer> stackFofIf = new Stack<>();

    static class ForInfo {
        int forEnd;
        int condition;
        int forBody;
        int increment;
    }

    private final Stack<ForInfo> stackForFor = new Stack<>();

    private final Stack<Integer> stackForElse = new Stack<>();

    public int getMainPc() {
        return mainPc;
    }

    @Override
    public void visit(DesignStmManyStart visitor) {
        counterOfUnpack = 0;
    }

    @Override
    public void visit(DesignStmPartOne visitor) {
        ++counterOfUnpack;
    }

    @Override
    public void visit(DesignStmPartMany visitor) {

        SyntaxNode designStmMany = visitor.getParent();
        while (designStmMany instanceof DesignStmPartMany || designStmMany instanceof DesignStmPartOne)
            designStmMany = designStmMany.getParent();
        Designator designator = ((DesignStmMany) designStmMany).getDesignator1();
        Code.load(designator.obj);
        Code.loadConst(counterOfUnpack++);
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
        Code.loadConst(counterOfUnpack);
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
        currNamespace = visitor.getName();
    }

    @Override
    public void visit(Namespace visitor) {
        currNamespace = "";
    }

    @Override
    public void visit(TermMany visitor) {
        SyntaxNode type = visitor.getMulOp();

        if (type instanceof MulOpMul)
            Code.put(Code.mul);
        else if (type instanceof MulOpDiv)
            Code.put(Code.div);
        else if (type instanceof MulOpMod)
            Code.put(Code.rem);
    }

    @Override
    public void visit(ExprAddOp visitor) {
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
        if (lastBlock.peek() == iAmInside.FOR)
            stackForFor.peek().forEnd = Code.pc - 2;
    }

    @Override
    public void visit(CondFactOne visitor) {
        Code.put(Code.const_1);
        Code.putFalseJump(Code.eq, 0);
        if (lastBlock.peek() == iAmInside.FOR)
            stackForFor.peek().forEnd = Code.pc - 2;
    }

    @Override
    public void visit(ExprMinus visitor) {
        Code.put(Code.neg);
    }

    @Override
    public void visit(ConditionOne visitor) {
        orTime();
    }

    @Override
    public void visit(ConditionMany visitor) {
        orTime();
    }

    private void orTime() {
        Code.put(Code.jmp);
        Code.put2(0);
        listOfOrs.add(Code.pc - 2);
        listOfAnds.forEach(Code::fixup);
        listOfAnds.clear();
    }

    private void andTime() {
        listOfAnds.add(Code.pc - 2);
    }

    @Override
    public void visit(CondTermMany visitor) {
        andTime();
    }

    @Override
    public void visit(CondTermOne visitor) {
        andTime();
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
        visitor.obj = AlexTab.myFind(SemanticPass.prepareSymbol(visitor.getName(), currNamespace));
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
        if (stmPrint.getExpr().struct == Tab.intType || stmPrint.getExpr().struct == AlexTab.boolType) {
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
        lastBlock.push(iAmInside.IF);
    }

    @Override
    public void visit(Else visitor) {

        Code.put(Code.jmp);
        Code.put2(0);
        stackForElse.push(Code.pc - 2);

        Code.fixup(stackFofIf.pop());
    }

    @Override
    public void visit(For visitor) {
        stackForFor.push(new ForInfo());
        breaks.push(new ArrayList<>());
        continues.push(new ArrayList<>());
        lastBlock.push(iAmInside.FOR);
    }

    @Override
    public void visit(ForCondStart visitor) {
        stackForFor.peek().condition = Code.pc;
    }

    @Override
    public void visit(CondFactOptOne visitor) {
        Code.putJump(0);
        stackForFor.peek().forBody = Code.pc - 2;
        stackForFor.peek().increment = Code.pc;
    }

    @Override
    public void visit(ForCondEnd visitor) {

        Code.put(Code.jmp);
        Code.put2(stackForFor.peek().condition - Code.pc + 1);
        Code.fixup(stackForFor.peek().forBody);

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
        continues.pop().forEach(Code::fixup);
        Code.put(Code.jmp);
        Code.put2(stackForFor.peek().increment - Code.pc + 1);
        Code.fixup(stackForFor.peek().forEnd);
        stackForFor.pop();
        breaks.pop().forEach(Code::fixup);
        lastBlock.pop();
    }

    @Override
    public void visit(StatementIf visitor) {
        Code.fixup(stackFofIf.pop());
    }

    @Override
    public void visit(StatementIfElse visitor) {
        Code.fixup(stackForElse.pop());
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
        breaks.peek().add(Code.pc - 2);
    }

    @Override
    public void visit(StatementContinue visitor) {
        Code.putJump(0);
        continues.peek().add(Code.pc - 2);
    }

    private void callFunction(Designator designator) {
        if (designator.getMatrixOpt() instanceof MatrixZero) {
            Code.put(Code.call);
            Code.put2(designator.obj.getAdr() - Code.pc + 1);
        } else {
            Obj secondToLast = oneBeforeLast(designator);
//            if (secondToLast.getKind() == Obj.Elem)
//                Code.load(MyTab.tempHelp);
//            else
            Code.load(secondToLast);
        }
    }

    @Override
    public void visit(DesignStmParen visitor) {
        callFunction(visitor.getDesignator());

        if (visitor.getDesignator().obj.getType() != Tab.noType)
            Code.put(Code.pop);

    }

    @Override
    public void visit(FactorParenPars visitor) {
        if (visitor.getFactorParenParsOpt() instanceof ParenPars)
            callFunction(visitor.getDesignator());
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
        stackFofIf.push(Code.pc - 2);
        for (Integer o : listOfOrs)
            Code.fixup(o);
        listOfOrs.clear();
        lastBlock.pop();
    }

    @Override
    public void visit(FactorMax visitor) {
        //
        // loop: while

        Obj arraySize = Tab.insert(Obj.Var, "arraySize", new Struct(Struct.Int));
        Code.load(visitor.getDesignator().obj);
        Code.put(Code.arraylength);
        Code.store(arraySize);

        Obj objMax = Tab.insert(Obj.Var, "max", new Struct(Struct.Int));
        Code.load(visitor.getDesignator().obj);
        Code.loadConst(0);
        Code.put(Code.aload);
        Code.store(objMax);

        Obj objIndex = Tab.insert(Obj.Var, "index", new Struct(Struct.Int));
        Code.loadConst(1);
        Code.store(objIndex);

        // continue: load objIndex
        // --------- load arraySize
        // --------- cmp lt
        // --------- objIndex < arraySize
        // --------- jmpFalse end
        // --------- load objMax
        // --------- load designator
        // --------- load objIndex
        // --------- aload
        // --------- cmp le ->>>>>>>>>>>> objMax > [1]
        // --------- jmpFalse incObjMax
        // --------- load designator
        // --------- load objIndex
        // --------- aload
        // --------- store objMax
        // incObjMax:load objIndex
        // --------- loadConst 1
        // --------- add
        // --------- store objIndex
        // --------- jmp continue
        // end: ---- load objMax

        int continuePc = Code.pc;
        Code.load(objIndex);
        Code.load(arraySize);
        Code.putFalseJump(Code.lt, 0);
        int endPc = Code.pc - 2;

        Code.load(objMax);
        Code.load(visitor.getDesignator().obj);
        Code.load(objIndex);
        Code.put(Code.aload);
        Code.putFalseJump(Code.le, 0);
        int incObjMax = Code.pc - 2;
        Code.load(visitor.getDesignator().obj);
        Code.load(objIndex);
        Code.put(Code.aload);
        Code.store(objMax);
        Code.fixup(incObjMax);
        Code.load(objIndex);
        Code.loadConst(1);
        Code.put(Code.add);
        Code.store(objIndex);
        Code.put(Code.jmp);
        Code.put2(continuePc - Code.pc + 1);
        Code.fixup(endPc);
        Code.load(objMax);

    }


}

