package rs.ac.bg.etf.pp1;

import java.util.ArrayList;
import java.util.List;

import rs.ac.bg.etf.pp1.ast.*;

public class CounterVisitor extends VisitorAdaptor {
    private int counter = 0;
    private final List<Integer> caseList = new ArrayList<>();
    private int switchCounter = 0;

    private boolean sameCase = false;

    public boolean getSameCase() {
        return sameCase;
    }

    public int getCaseNumber() {
        return counter;
    }

    public List<Integer> getCaseValues() {
        return caseList;
    }

//    public void visit(CaseC c) {
//        if (switchCounter == 1) {
//            counter++;
//            if (caseList.contains(c.getN3()))
//                sameCase = true;
//            caseList.add(c.getN3());
//        }
//    }

//    public void visit(Switch sw) {
//        switchCounter++;
//    }

//    public void visit(StatementSwitch ss) {
//        switchCounter--;
//    }
}
