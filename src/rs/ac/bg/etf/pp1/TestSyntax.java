package rs.ac.bg.etf.pp1;

import java_cup.runtime.Symbol;

import java.io.*;

public class TestSyntax {
    public static void main(String[] args) throws IOException {

        Reader br = null;
        File sourceCode = new File("test/program.mj");
        br = new BufferedReader(new FileReader(sourceCode));
        Yylex lexer = new Yylex(br);

        Symbol symbol;
        do {
            symbol = lexer.next_token();
            System.out.println(symbol.value);

        } while (symbol.sym != sym.EOF);
    }
}
