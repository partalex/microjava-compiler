package rs.ac.bg.etf.pp1;

import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.*;
import rs.etf.pp1.symboltable.structure.SymbolDataStructure;

import java.util.Collection;

public class MyTab extends Tab {
    public static final Struct boolType = new Struct(Struct.Bool);
    public static Obj tempHelp;

    public static void myInit() {
        init();
        currentScope.addToLocals(new Obj(Obj.Type, "bool", boolType));
    }

    public static void dump() {
        System.out.println("=====================SYMBOL TABLE DUMP=========================");
        //if (stv == null)
        MyDumpSymbolTableVisitor stv = new MyDumpSymbolTableVisitor();
        for (Scope s = currentScope; s != null; s = s.getOuter()) {
            s.accept(stv);
        }
        System.out.println(stv.getOutput());
    }

    public static Obj myFind(String name) {
        Obj resultObj = null;
        for (Scope s = currentScope; s != null; s = s.getOuter()) {
            SymbolDataStructure locals = s.getLocals();
            if (locals != null) {
                for (Obj obj : locals.symbols()) {
                    if (obj.getName().equals(name)) {
                        resultObj = obj;
                        break;
                    } else {
                        Collection<Obj> localsOfObj = obj.getLocalSymbols();
                        if (localsOfObj != null) {
                            for (Obj objOfObj : localsOfObj) {
                                if (objOfObj.getName().equals(name)) {
                                    resultObj = objOfObj;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        return (resultObj != null) ? resultObj : noObj;
    }

}