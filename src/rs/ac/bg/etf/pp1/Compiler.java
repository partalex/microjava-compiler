package rs.ac.bg.etf.pp1;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import rs.ac.bg.etf.pp1.util.Log4JUtils;
import java_cup.runtime.Symbol;
import rs.ac.bg.etf.pp1.ast.Program;
import rs.etf.pp1.mj.runtime.Code;

import java.io.*;

public class Compiler {

    static {
        DOMConfigurator.configure(Log4JUtils.instance().findLoggerConfigFile());
        Log4JUtils.instance().prepareLogFile(Logger.getRootLogger());
    }

    public static void main(String[] args) throws Exception {

        Logger log = Logger.getLogger(Compiler.class);
        Reader bufferedReader = null;

        try {

            File sourceCode = new File(args[0]);

            log.info("Compiling source file: " + sourceCode.getAbsolutePath());

            bufferedReader = new BufferedReader(new FileReader(sourceCode));
            Yylex lexer = new Yylex(bufferedReader);

            MJParser mjParser = new MJParser(lexer);
            Symbol symbol = mjParser.parse();  //pocetak parsiranja

            if (mjParser.errorDetected)
                log.info("Postoji sintaksna greska");
            else {
                Program prog = (Program) (symbol.value);
                //Tab.init();
                MyTab.myInit();
                // ispis sintaksnog stabla
                log.info(prog.toString(""));
                log.info("===================================");


                // ispis prepoznatih programskih konstrukcija
                SemanticPass semanticPass = new SemanticPass();
                prog.traverseBottomUp(semanticPass);

                //log.info(" Print count calls = " + v.printCallCount);

                //log.info(" Deklarisanih promenljivih ima = " + v.varDeclCount);

                log.info("===================================");
                // Tab.dump();

                if (!mjParser.errorDetected && semanticPass.passed()) {
                    File objFile = new File(args[1]);
                    if (objFile.exists()) objFile.delete();

                    CodeGenerator codeGenerator = new CodeGenerator();
                    Code.dataSize = semanticPass.numberOfVars;
                    prog.traverseBottomUp(codeGenerator);

                    Code.mainPc = codeGenerator.getMainPc();
                    Code.write(new FileOutputStream(objFile));

                    log.info("Parsiranje uspesno zavrseno!");
                } else {
                    log.error("Parsiranje NIJE uspesno zavrseno!");
                }
                MyTab.dump();
            }
        } finally {
            if (bufferedReader != null) try {
                bufferedReader.close();
            } catch (IOException error) {
                log.error(error.getMessage(), error);
            }
        }
    }
}
