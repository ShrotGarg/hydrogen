import java.nio.file.*;
import java.io.*;
import java.util.*;

/* Error Codes:
0 - Incorrect Input
5,6,7 - Syntax Error
10 - Terminal Cmd Error
*/


class Tokenss {
    String value;
    String type;

    Tokenss(String v, String t){
        this.value = v;
        this.type = t;
    }
}


public class no{
    public static void main(String[] args) throws IOException, InterruptedException{
        // Checks if the file inputted is correct
        if(args[0].indexOf(".hydro") == -1){
            System.out.print("Incorrect input");
            System.exit(0);
        }

        String contents = Files.readString(Path.of(args[0]));
        contents = contents.substring(0, contents.length() - 1) + " ; ";

        String tokens[] = {"_return", "int_lit", "plus", "minus", "mult", "div", "semi"};

        ArrayList <Tokenss> tokenList = tokenization(contents, tokens);


        //Syntax Check
        if(!tokenList.get(0).type.equals("_return") || !tokenList.get(tokenList.size() - 1).type.equals("semi")){
            System.out.print("Invalid Syntax");
            System.exit(6);
        }

        for(int i = 0; i < tokenList.size() - 1; i++){
            if(tokenList.get(i).type.equals(tokenList.get(i + 1).type)){
                System.out.print("Syntax Error");
                System.exit(7);
            }
        }


        //Evaluating the expression in return
        int result = 0;
        int count = 0;


        //Solving Operator Precendence without binary trees :) - Solves * and /
        while(tokenList.size() != 3 && count < tokenList.size()){
            if(tokenList.get(count).type.equals("mult") && tokenList.get(count - 1).type.equals("int_lit") && tokenList.get(count + 1).type.equals("int_lit")){
                result = Integer.parseInt(tokenList.get(count - 1).value) * Integer.parseInt(tokenList.get(count + 1).value);
                resObjUpdation(result, tokens[1], tokenList, count);
                count = 0;
                continue;
            }
            if(tokenList.get(count).type.equals("div") && tokenList.get(count - 1).type.equals("int_lit") && tokenList.get(count + 1).type.equals("int_lit")){
                result = Integer.parseInt(tokenList.get(count - 1).value) / Integer.parseInt(tokenList.get(count + 1).value);
                resObjUpdation(result, tokens[1], tokenList, count);
                count = 0;
                continue;
            }
            count++;
        }

        // Evaluating + and -
        for(int i = 0; i < tokenList.size(); i++){
            if(tokenList.get(i).type.equals("int_lit"))
                result = Integer.parseInt(tokenList.get(i).value);
            else if(tokenList.get(i).type.equals("plus") && tokenList.get(i + 1).type.equals("int_lit") && tokenList.get(i - 1).type.equals("int_lit")){
                result = Integer.parseInt(tokenList.get(i - 1).value) + Integer.parseInt(tokenList.get(i + 1).value);
                resObjUpdation(result, tokens[1], tokenList, i);
                i--;
            }
            else if(tokenList.get(i).type.equals("minus") && tokenList.get(i + 1).type.equals("int_lit") && tokenList.get(i - 1).type.equals("int_lit")){
                result = Integer.parseInt(tokenList.get(i - 1).value) - Integer.parseInt(tokenList.get(i + 1).value);
                resObjUpdation(result, tokens[1], tokenList, i);
                i--;
            }
        }


        //Converting to Assembly Code
        String asm_code = "global _start\n" +
                          "\n" +
                          "_start:\n" +
                          "\tmov rax, 0x2000001\n" +
                          "\tmov rdi, " + Math.abs(result) + "\n";

        if(result < 0){
            asm_code += "\tneg rdi\n" + "\tsyscall";
        }
        else
            asm_code += "\tsyscall";

        FileWriter fout = new FileWriter("test.asm");
        BufferedWriter bout = new BufferedWriter(fout);
        PrintWriter pout = new PrintWriter(bout);

        pout.write(asm_code);

        pout.close();
        bout.close();
        fout.close();


        //Running terminal Commands
        runCommand("nasm", "-f", "macho64", "./test.asm", "-o", "test.o");

        Process sdkProcess = new ProcessBuilder("xcrun", "--show-sdk-path").start();
        String sdkpath = new BufferedReader(new InputStreamReader(sdkProcess.getInputStream())).readLine();
        runCommand("ld", "-o", "test", "test.o", "-lSystem", "-syslibroot", sdkpath, "-e",
                         "_start", "-arch", "x86_64");

        ProcessBuilder pb = new ProcessBuilder("zsh", "-c", "./test; echo $?");
        Process p = pb.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;

        while((line = reader.readLine()) != null){
            System.out.println("Program Exit Code: " + line);
        }

        p.waitFor();
    }


    // Tokenization
    public static ArrayList<Tokenss> tokenization(String content, String tokens[]){
        ArrayList<Tokenss> list = new ArrayList<Tokenss>();

        String str = "";
        for(int i = 0; i < content.length(); i++){
            char ch = content.charAt(i);

            if(Character.isWhitespace(ch)){
                Tokenss obj1 = null;
                if(str.equals("return"))
                    obj1 = new Tokenss(str, tokens[0]);
                else if(isNumber(str))
                    obj1 = new Tokenss(str, tokens[1]);
                else if(str.equals("+"))
                    obj1 = new Tokenss(str, tokens[2]);
                else if(str.equals("-"))
                    obj1 = new Tokenss(str, tokens[3]);
                else if(str.equals("*"))
                    obj1 = new Tokenss(str, tokens[4]);
                else if(str.equals("/"))
                    obj1 = new Tokenss(str, tokens[5]);
                else if(str.equals(";"))
                    obj1 = new Tokenss(str, tokens[6]);
                else{
                    System.out.print("Syntax Error");
                    System.exit(5);
                }

                if(obj1 != null)
                    list.add(obj1);

                str = "";
                continue;
            }
            else{
                str = str + ch;
            }
        }

        return list;
    }


    // To check if str is a number
    private static boolean isNumber(String str){
        for(int i = 0; i < str.length(); i++){
            if(!Character.isDigit(str.charAt(i)))
                return false;
        }
        return true;
    }


    private static Tokenss resObjUpdation(int res, String type, ArrayList<Tokenss> listTokens, int i){
        Tokenss obj = new Tokenss(String.valueOf(res), type);
        listTokens.set(i - 1, obj);
        listTokens.remove(i);
        listTokens.remove(i);
        return obj;
    }

    // To run the terminal commands
    public static void runCommand(String... cmd) throws IOException, InterruptedException{
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.inheritIO();
        Process obj = pb.start();
        if(obj.waitFor() != 0){
            System.out.println("Error at: " + cmd[0]);
            System.exit(10);
        }
    }
}