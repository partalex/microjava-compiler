package rs.ac.bg.etf.pp1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;

import java_cup.runtime.Symbol;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import rs.ac.bg.etf.pp1.ast.Program;
import rs.ac.bg.etf.pp1.util.Log4JUtils;
import rs.etf.pp1.mj.runtime.Code;

public class MJParserTest {

    static {
        DOMConfigurator.configure(Log4JUtils.instance().findLoggerConfigFile());
        Log4JUtils.instance().prepareLogFile(Logger.getRootLogger());
    }

    public static void main(String[] args) throws Exception {

        Logger log = Logger.getLogger(MJParserTest.class);

        Reader br = null;
        try {
            File sourceCode = new File("test/program.mj");
            log.info("Compiling source file: " + sourceCode.getAbsolutePath());

            br = new BufferedReader(new FileReader(sourceCode));
            Yylex lexer = new Yylex(br);

            MJParser p = new MJParser(lexer);
            Symbol s = p.parse();  // start of parsing

            if (p.errorDetected)
                log.info("Syntax analysis NOT successful!");
            else {
                Program program = (Program) (s.value);
                //Tab.init();
                MyTab.myInit();
                // syntax tree
                log.info(program.toString(""));
                log.info("===================================");


                // print of the program structures
                SemanticPass v = new SemanticPass();
                program.traverseBottomUp(v);

//                 log.info(" Print count calls = " + v.printCallCount);

//                log.info("Number of declared count = " + v.varDeclCount);

                log.info("===================================");
                // Tab.dump();

                if (!p.errorDetected && v.passed()) {
                    File objFile = new File("test/program.obj");
                    if (objFile.exists()) objFile.delete();

                    CodeGenerator codeGenerator = new CodeGenerator();
                    Code.dataSize = v.numberOfVars;
                    program.traverseBottomUp(codeGenerator);

                    Code.mainPc = codeGenerator.getMainPc();
                    Code.write(Files.newOutputStream(objFile.toPath()));

                    log.info("Successfully compiled program!");
                } else {
                    log.error("Code is not generated because of semantic errors!");
                }
                MyTab.dump();
            }
        } finally {
            if (br != null) try {
                br.close();
            } catch (IOException e1) {
                log.error(e1.getMessage(), e1);
            }
        }

    }
}
