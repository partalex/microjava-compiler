# MicroJava Compiler

## Overview

This project is a **MicroJava Compiler** implemented in **Java**. It processes **MicroJava** code through four phases: lexical analysis, syntax analysis, code generation, and execution. The compiler is built using **JFlex** and **Java CUP** for lexical analysis and parsing, respectively.

### Phases of Compilation

1. **Lexical Analysis**: The input MicroJava code is tokenized using **JFlex**. This phase identifies tokens such as keywords, identifiers, operators, and literals.
2. **Syntax Analysis**: Using **Java CUP**, the compiler checks if the token sequence conforms to the grammar rules of MicroJava.
3. **Code Generation**: Once the syntax is verified, the compiler generates intermediate code or target code.
4. **Execution**: The compiled code is executed in a MicroJava runtime environment or virtual machine.

### Features

- **JFlex Integration**: The project uses **JFlex** to generate the lexical analyzer.
- **Java CUP Parser**: The syntax analysis is handled by **Java CUP**, generating a parser for MicroJava grammar.
- **Code Generation**: The compiler generates code that is executable in a virtual machine or similar runtime environment.
- **Error Handling**: Detailed error messages for both lexical and syntax errors.
- **Support for MicroJava Language**: Including key language features such as variables, control flow (if, for, while), and basic I/O operations.
