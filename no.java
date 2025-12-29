import java.nio.file.*;
import java.io.*;
import java.util.*;

/* Error Codes:
0 - Incorrect Input
5 - 7 - Syntax Error
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
    static int result = 0;

    public static void main(String[] args) throws IOException, InterruptedException{
        // Checks if the file inputted is correct
        if(args[0].indexOf(".hydro") == -1){
            System.out.print("Incorrect input");
            System.exit(0);
        }

        String contents = Files.readString(Path.of(args[0])).trim() + " ";

        String tokens[] = {"_return", "int_lit", "plus", "minus", "mult", "div", "open_para", "close_para", "semi"};

        ArrayList <Tokenss> tokenList = tokenization(contents, tokens);


        //Syntax Check
        if(!checkSyntax(tokenList)){
            System.out.print("Syntax Error");
            System.exit(7);
        }


        //Solving Paranthesis using recursino
        if(check_paran(tokenList))
            solvePara(tokenList, tokens);


        //Calculation
        mult_or_div(tokenList, tokens);
        add_or_sub(tokenList, tokens);
        result = Integer.parseInt(tokenList.get(1).value);

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

    private static void printList(ArrayList<Tokenss> list){
        for(int i = 0; i < list.size(); i++){
            System.out.println(list.get(i).value + " " + list.get(i).type);
        }
    }

    // Tokenization
    public static ArrayList<Tokenss> tokenization(String content, String tokens[]) {
        ArrayList<Tokenss> list = new ArrayList<Tokenss>();
        String str = "";

        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);

            if (Character.isLetterOrDigit(ch))
                str += ch;
            else{
                if (!str.isEmpty()){
                    if (str.equals("return"))
                        list.add(new Tokenss(str, tokens[0]));
                    else if (isNumber(str))
                        list.add(new Tokenss(str, tokens[1]));
                    else {
                        System.out.println("Syntax Error");
                        System.exit(5);
                    }
                    str = "";
                }

                if (!Character.isWhitespace(ch)) {
                    String type = "";
                    if (ch == '+') type = tokens[2];
                    else if (ch == '-') type = tokens[3];
                    else if (ch == '*') type = tokens[4];
                    else if (ch == '/') type = tokens[5];
                    else if (ch == '(') type = tokens[6];
                    else if (ch == ')') type = tokens[7];
                    else if (ch == ';') type = tokens[8];
                    else {
                        System.out.println("Syntax Error");
                        System.exit(6);
                    }
                    list.add(new Tokenss(String.valueOf(ch), type));
                }
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


    //To check the syntax against the laws of our language
    private static boolean checkSyntax(ArrayList<Tokenss> list){
        int c = 0;

        for(int i = 0; i < list.size() - 1; i++){
            //Two operators or operands can't be one after the other
            if(list.get(i).type.equals(list.get(i + 1).type) && !(list.get(i).type.equals("open_para") || list.get(i).type.equals("close_para")))
                return false;

            //Checking the paranthesis count
            if(list.get(i).type.equals("open_para"))
                c++;
            if(list.get(i).type.equals("close_para"))
                c--;
        }

        if(!(list.get(0).type.equals("_return") && list.get(list.size() - 1).type.equals("semi")))
            return false;

        if(c != 0)
            return false;

        return true;
    }


    //Check if Paranthesis are present
    private static boolean check_paran(ArrayList<Tokenss> list){
        for(int i = 0; i < list.size(); i++){
            if(list.get(i).type.equals("open_para"))
                return true;
        }

        return false;
    }


    //Solve Paranthesis
    private static void solvePara(ArrayList<Tokenss> list, String[] tokens){
        int pos = 0;
        for(int i = 0; i < list.size(); i++){
            if(list.get(i).type.equals("close_para"))
                pos = i;
        }

        int pos1 = 0;
        for(int i = pos - 1; i >= 0; i--){
            if(list.get(i).type.equals("open_para"))
                pos1 = i;
        }

        //Creating a subexpression to solve
        ArrayList<Tokenss> sublist = new ArrayList<>();
        for(int i = pos1 + 1; i < pos; i++){
            sublist.add(list.get(i));
        }

        mult_or_div(sublist, tokens);
        add_or_sub(sublist, tokens);

        //Modify the list
        list.set(pos1, sublist.get(0));

        int count = pos - pos1;
        for(int i = 0; i < count; i++)
            list.remove(pos1 + 1);

        if(check_paran(list))
            solvePara(list, tokens);
        else
            return;
    }


    //Solving Operator Precendence without binary trees :) - Solves * and /
    private static void mult_or_div(ArrayList<Tokenss> tokenList, String[] tokens){
        int count = 0;
        int val = 0;

        while(count < tokenList.size()){
            if(tokenList.get(count).type.equals("mult") && tokenList.get(count - 1).type.equals("int_lit") && tokenList.get(count + 1).type.equals("int_lit")){
                val = Integer.parseInt(tokenList.get(count - 1).value) * Integer.parseInt(tokenList.get(count + 1).value);
                resObjUpdation(val, tokens[1], tokenList, count);
                count = 0;
                continue;
            }
            if(tokenList.get(count).type.equals("div") && tokenList.get(count - 1).type.equals("int_lit") && tokenList.get(count + 1).type.equals("int_lit")){
                val = Integer.parseInt(tokenList.get(count - 1).value) / Integer.parseInt(tokenList.get(count + 1).value);
                resObjUpdation(val, tokens[1], tokenList, count);
                count = 0;
                continue;
            }
            count++;
        }
    }


    // Evaluating + and -
    private static void add_or_sub(ArrayList<Tokenss> tokenList, String[] tokens){
        int val = 0;
        int i = 0;

        while(i < tokenList.size()){
            if(i > 0 && i + 1 < tokenList.size()){
                if(tokenList.get(i).type.equals("plus") && tokenList.get(i + 1).type.equals("int_lit") && tokenList.get(i - 1).type.equals("int_lit")){
                    val = Integer.parseInt(tokenList.get(i - 1).value) + Integer.parseInt(tokenList.get(i + 1).value);
                    resObjUpdation(val, tokens[1], tokenList, i);
                    i = 0;
                }
                else if(tokenList.get(i).type.equals("minus") && tokenList.get(i + 1).type.equals("int_lit") && tokenList.get(i - 1).type.equals("int_lit")){
                    val = Integer.parseInt(tokenList.get(i - 1).value) - Integer.parseInt(tokenList.get(i + 1).value);
                    resObjUpdation(val, tokens[1], tokenList, i);
                    i = 0;
                }
            }

            i++;
        }
    }


    //Resultant Object for operations result
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