# hydrogen
A hobby programming language that is being built using java. It's a complete language in which I am building the compiler, generating the assembly code, and even calling the terminal commands

To run the file, you must download nasm either by using homebrew (`brew install nasm`) or doing it directly from their website. Then you must clone this repo and run the following commands in terminal:

`javac no.java`
`java no test.hydro`

no.java is the driver java file that is working as the compiler for the language. Taking it further, I have added my own assembler so the file is first being compiled to act as a compiler for the hydrogen programming language. Then we are running the command to execute the .hydro file (use test.hydro or big_test.hydro) to see the output.

At the moment, the programming language support variable functionaility with constants and returns the exit code. You define variables using var and follow java-like syntax with minimal changes. Refer to grammar.txt for more context.