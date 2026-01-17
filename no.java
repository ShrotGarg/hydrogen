import java.nio.file.*;
import java.io.*;
import java.util.*;

/* Error Codes:
0 - Incorrect Input
5 - 8 - Syntax Error
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
    static boolean return_found = false;
    static String cachedSdkPath = null;

    public static void main(String[] args) throws IOException, InterruptedException{
        long start = System.nanoTime();
        // Checks if the file inputted is correct
        if(args[0].indexOf(".hydro") == -1){
            System.out.print("Incorrect input");
            System.exit(0);
        }

        String contents = Files.readString(Path.of(args[0])).trim() + " ";
        String tokens[] = {"_return", "int_lit", "plus", "minus", "mult", "div", "open_para", "close_para", "_variable", "var_name", "_equal", "_and", "_or", "_not", "equals", "less", "equals_less", "more", "equals_more", "not_equals", "power", "_if", "_elif", "_else", "open_brac", "close_brac", "semi"}; //26
        ArrayList <Tokenss> tokenList = tokenization(contents, tokens);
        HashMap <String, Tokenss> map = new HashMap<>();

        //To evaluate code that has variables
        ArrayList<ArrayList<Tokenss>> sents = new ArrayList<>();
        ArrayList<Tokenss> temp = new ArrayList<>();

        //Seperate each '; { } ' into seperate sentences
        for(int i = 0; i < tokenList.size(); i++){
            if(tokenList.get(i).type.equals(tokens[26]) || tokenList.get(i).type.equals(tokens[24]) || tokenList.get(i).type.equals(tokens[25])){
                temp.add(tokenList.get(i));
                sents.add(new ArrayList<>(temp));
                temp.clear();
            }
            else
                temp.add(tokenList.get(i));
        }
        temp.clear();

        checkSyntax(sents, map, tokens);
        compile(0, sents.size(), sents, map, tokens);

        //To reduce runtime
        StringBuilder asm_code = new StringBuilder();

        //Converting to Assembly Code
        asm_code.append("global _start\n\n").append("_start:\n").append("\tmov rax, 0x2000001\n").append("\tmov rdi, ").append(Math.abs(result)).append("\n");
        if(result < 0)
            asm_code.append("\tneg rdi\n").append("\tsyscall");
        else
            asm_code.append("\tsyscall");


        FileWriter fout = new FileWriter("test.asm");
        BufferedWriter bout = new BufferedWriter(fout);
        PrintWriter pout = new PrintWriter(bout);

        pout.write(asm_code.toString());

        pout.close();
        bout.close();
        fout.close();

        //Running terminal Commands
        String sdkpath = getSDKPath();
        runCommand("nasm", "-f", "macho64", "test.asm", "-o", "test.o");
        runCommand("ld", "-o", "test", "test.o", "-lSystem", "-syslibroot", sdkpath, "-e", "_start", "-arch", "x86_64");
        Process p = new ProcessBuilder("./test").start();
        p.waitFor();

        System.out.println("Program Exit Code: " + p.exitValue());

        long end = System.nanoTime();
        long time = (end - start)/1000000;
        System.out.print("Time taken: " + time + "ms");
    }


    //Helper Function to debug code
    private static void printList(ArrayList<Tokenss> list){
        for(int i = 0; i < list.size(); i++)
            System.out.println(list.get(i).value + " " + list.get(i).type);
    }


    //Tokenization
    public static ArrayList<Tokenss> tokenization(String content, String tokens[]) {
        ArrayList<Tokenss> list = new ArrayList<Tokenss>();
        String str = "";

        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);

            if (Character.isLetterOrDigit(ch) || ch == '_') str += ch;
            else{
                if (!str.isEmpty()){
                    if (str.equals("return")) list.add(new Tokenss(str, tokens[0]));
                    else if (isNumber(str)) list.add(new Tokenss(str, tokens[1]));
                    else if(str.equals("var")) list.add(new Tokenss(str,tokens[8]));
                    else if(str.equals("and")) list.add(new Tokenss(str, tokens[11]));
                    else if (str.equals("or")) list.add(new Tokenss(str, tokens[12]));
                    else if(str.equals("not")) list.add(new Tokenss(str, tokens[13]));
                    else if(str.equals("equals")) list.add(new Tokenss(str, tokens[14]));
                    else if(str.equals("equals_less")) list.add(new Tokenss(str, tokens[16]));
                    else if(str.equals("equals_more")) list.add(new Tokenss(str, tokens[18]));
                    else if(str.equals("not_equals")) list.add(new Tokenss(str, tokens[19]));
                    else if(str.equals("if")) list.add(new Tokenss(str, tokens[21]));
                    else if(str.equals("elif")) list.add(new Tokenss(str, tokens[22]));
                    else if(str.equals("else")) list.add(new Tokenss(str, tokens[23]));
                    else list.add(new Tokenss(str, tokens[9]));
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
                    else if (ch == '=') type = tokens[10];
                    else if (ch == '<') type = tokens[15];
                    else if (ch == '>') type = tokens[17];
                    else if (ch == '^') type = tokens[20];
                    else if (ch == '{') type = tokens[24];
                    else if (ch == '}') type = tokens[25];
                    else if (ch == ';') type = tokens[26];
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


    //To check if str is a number
    private static boolean isNumber(String str){
        for(int i = 0; i < str.length(); i++){
            if(!Character.isDigit(str.charAt(i)))
                return false;
        }
        return true;
    }


    //To check the syntax
    private static boolean checkSyntax(ArrayList<ArrayList<Tokenss>> sents, HashMap<String, Tokenss> map, String[] tokens){
        for(int i = 0; i < sents.size(); i++){
            for(int j = 0; j < sents.get(i).size() - 1; j++){
                //No consecutive operator/operand
                if(sents.get(i).get(j).type.equals(sents.get(i).get(j + 1).type) && "{}()".indexOf(sents.get(i).get(j).value) != -1)
                    return false;
            }

            int c = 0;
            for(int j = 0; j < sents.get(i).size(); j++){
                //Bracket Check
                if(sents.get(i).get(j).type.equals(tokens[6]) || sents.get(i).get(j).type.equals(tokens[24]))
                    c++;
                else if(sents.get(i).get(j).type.equals(tokens[7]) || sents.get(i).get(j).type.equals(tokens[25]))
                    c--;
            }
            if(c != 0)
                return false;

            //End with semicolon or { OR start and end with only }
            if(!sents.get(i).get(sents.get(i).size() - 1).type.equals(tokens[26]) && !sents.get(i).get(sents.get(i).size() - 1).type.equals(tokens[24]))
                return false;
            else if(!sents.get(i).get(0).type.equals(sents.get(i).get(sents.get(i).size() - 1).type) && sents.get(i).get(0).type.equals(tokens[25]))
                return false;

            //No declaration allowed if the variable is already declared
            if(sents.get(i).get(0).type.equals(tokens[8]))
                if(map.containsKey(sents.get(i).get(1).value))
                    return false;

            //Undeclared Variable is used in assignment
            if(sents.get(i).get(0).type.equals(tokens[9]))
                if(!map.containsKey(sents.get(i).get(0).value))
                    return false;

        }
        return true;
    }


    //Parsing of Tokens / Compiler Logic
    private static void compile(int i, int end, ArrayList<ArrayList<Tokenss>> sents, HashMap<String, Tokenss> map, String[] tokens){
        boolean condition = false;

        for(; i < end; i++){
            if(return_found)
                return ;

            //Condition: If
            if(sents.get(i).get(0).type.equals(tokens[21])){
                ArrayList<Tokenss> temp = new ArrayList<>();

                for(int j = 1; j < sents.get(i).size() - 1; j++){
                    Tokenss t = sents.get(i).get(j);
                    if(map.containsKey(t.value)) temp.add(map.get(t.value));
                    else temp.add(t);
                }

                evaluation(temp, tokens);
                int outcome = Integer.parseInt(temp.get(0).value);
                int closing = find_the_bracket(i, sents, tokens);

                if(outcome == 1){
                    condition = true;
                    compile(i + 1, closing, sents, map, tokens);
                }
                else
                    condition = false;

                i = closing;
            }

            //Condition: Elif
            else if(sents.get(i).get(0).type.equals(tokens[22])){
                ArrayList<Tokenss> temp = new ArrayList<>();

                for(int j = 1; j < sents.get(i).size() - 1; j++){
                    Tokenss t = sents.get(i).get(j);
                    if(map.containsKey(t.value)) temp.add(map.get(t.value));
                    else temp.add(t);
                }

                evaluation(temp, tokens);
                int closing = find_the_bracket(i, sents, tokens);

                if(!condition){
                    int outcome = Integer.parseInt(temp.get(0).value);
                    if(outcome == 1){
                        condition = true;
                        compile(i + 1, closing, sents, map, tokens);
                    }
                    else
                        condition = false;
                }

                i = closing;
            }

            //Condition: Else
            else if(sents.get(i).get(0).type.equals(tokens[23])){
                int closing = find_the_bracket(i, sents, tokens);

                if(!condition)
                    compile(i + 1, closing, sents, map, tokens);

                i = closing;
            }


            //Variable Declaration
            else if(sents.get(i).get(0).type.equals(tokens[8]) && sents.get(i).get(1).type.equals(tokens[9]) && sents.get(i).get(2).type.equals(tokens[10]))
                variable_declaration(sents, i, map, tokens);

            //Variable Assignment/Replacing the variables with their values
            else if(sents.get(i).get(0).type.equals(tokens[9]) && sents.get(i).get(1).type.equals(tokens[10]))
                variable_assignment(sents, i, map, tokens);

            //Return Expression
            else if(sents.get(i).get(0).type.equals(tokens[0])){
                return_expr(sents, i, map, tokens);
                return_found = true;
                return; // As soon it hits return, break out and return that exit code
            }

            //Skipping if it encounters { or }
            else if(sents.get(i).get(0).type.equals(tokens[25]) || sents.get(i).get(0).type.equals(tokens[24]))
                continue;

            //Syntax Error Otherwise
            else{
                System.out.println("Syntax Error");
                System.exit(8);
            }
        }
    }


    //Variable Declaration
    private static void variable_declaration(ArrayList<ArrayList<Tokenss>> sents, int i, HashMap<String, Tokenss> map, String tokens[]){
        ArrayList<Tokenss> temp = new ArrayList<>();

        for(int j = 3; j < sents.get(i).size(); j++){
            Tokenss t = sents.get(i).get(j);

            if(map.containsKey(t.value))
                temp.add(map.get(t.value));
            else
                temp.add(t);
        }

        evaluation(temp, tokens);
        map.put(sents.get(i).get(1).value, temp.get(0));
    }


    //Variable Assignment/Replacing
    private static void variable_assignment(ArrayList<ArrayList<Tokenss>> sents, int i, HashMap<String, Tokenss> map, String[] tokens){
        ArrayList<Tokenss> temp = new ArrayList<>();

        for(int j = 2; j < sents.get(i).size(); j++){
            Tokenss t = sents.get(i).get(j);

            if(map.containsKey(t.value))
                temp.add(map.get(t.value));
            else
                temp.add(t);
        }

        evaluation(temp, tokens);
        map.put(sents.get(i).get(0).value, temp.get(0));
    }


    //Return Keyword Evaluation
    private static void return_expr(ArrayList<ArrayList<Tokenss>> sents, int i, HashMap<String, Tokenss> map, String[] tokens){
        ArrayList<Tokenss> temp = new ArrayList<>();

        int pos = 1;
        if(sents.get(i).get(1).type.equals("_not"))
            pos = 2;

        for(int j = pos; j < sents.get(i).size() - 1; j++){
            Tokenss t = sents.get(i).get(j);

            // Replacing Variables with their Literal values
            if(map.containsKey(t.value))
                temp.add(map.get(t.value));
            else
                temp.add(t);
            }

        //Evaluation
        evaluation(temp, tokens);
        result = Integer.parseInt(temp.get(0).value);
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
        int pos = -1;
        for(int i = 0; i < list.size(); i++){
            if(list.get(i).type.equals("close_para")){
                pos = i;
                break;
            }
        }

        int pos1 = -1;
        for(int i = pos - 1; i >= 0; i--){
            if(list.get(i).type.equals("open_para")){
                pos1 = i;
                break;
            }
        }

        if(pos1 == pos && pos == -1)
            return;

        //Creating a subexpression to solve
        ArrayList<Tokenss> sublist = new ArrayList<>();
        for(int i = pos1 + 1; i < pos; i++){
            sublist.add(list.get(i));
        }

        mult_or_div(sublist, tokens);
        add_or_sub(sublist, tokens);

        //Modify the list
        int range = pos - pos1 + 1;
        for(int i = 0; i < range; i++)
            list.remove(pos1);
        list.add(pos1, sublist.get(0));

        if(check_paran(list))
            solvePara(list, tokens);
        else
            return;
    }


    //Solve exponents
    private static void solvePower(ArrayList<Tokenss> list, String[] tokens){
        int i = 1;
        while(i < list.size()){
            if(list.get(i).type.equals(tokens[20])){
                int base = Integer.parseInt(list.get(i - 1).value);
                int power = Integer.parseInt(list.get(i + 1).value);
                int val = (int)(Math.pow(base, power));
                resObjUpdation(val, tokens[1], list, i);
                i--;
            }
            i++;
        }
    }


    //Solving Operator Precendence without binary trees :) - Solves * and /
    private static void mult_or_div(ArrayList<Tokenss> tokenList, String[] tokens){
        int count = 1;
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


    //Evaluating + and -
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


    //Sovling Expressions that have and, or, not
    private static void solveLogExpr(ArrayList<Tokenss> temp, String[] tokens){
        for(int i = 1; i < temp.size(); i++){
            if(temp.get(i).type.equals("_and") && temp.get(i + 1).type.equals("int_lit") && temp.get(i - 1).type.equals("int_lit")){
                if(Integer.parseInt(temp.get(i + 1).value) > 0 && Integer.parseInt(temp.get(i - 1).value) > 0)
                    resObjUpdation(1, tokens[1], temp, i);
                else
                    resObjUpdation(0, tokens[1], temp, i);
                i = 0;
            }
            else if(temp.get(i).type.equals("_or") && temp.get(i + 1).type.equals("int_lit") && temp.get(i - 1).type.equals("int_lit")){
                if(Integer.parseInt(temp.get(i + 1).value) > 0 || Integer.parseInt(temp.get(i - 1).value) > 0)
                    resObjUpdation(1, tokens[1], temp, i);
                else
                    resObjUpdation(0, tokens[1], temp, i);
                i = 0;
            }
            else if(temp.get(i).type.equals("_not") && temp.get(i + 1).type.equals("int_lit")){
                if(Integer.parseInt(temp.get(i + 1).value) > 0){
                    temp.set(i,  new Tokenss(Integer.toString(0), tokens[1]));
                    temp.remove(i + 1);
                    i = 0;
                }
                else{
                    temp.set(i, new Tokenss(Integer.toString(1), tokens[1]));
                    temp.remove(i + 1);
                    i = 0;
                }
            }
        }
    }


    //Solving expressions that have equal, not equal, >=, <=, >, <
    private static void solveLogOper(ArrayList<Tokenss> temp, String[] tokens){
        for(int i = 1; i < temp.size(); i++){
            if(temp.get(i).type.equals("equals") && temp.get(i + 1).type.equals("int_lit") && temp.get(i - 1).type.equals("int_lit")){
                if(temp.get(i + 1).value.equals(temp.get(i - 1).value))
                    resObjUpdation(1, tokens[1], temp, i);
                else
                    resObjUpdation(0, tokens[1], temp, i);
                i = 0;
            }
            else if(temp.get(i).type.equals("equals_more") && temp.get(i + 1).type.equals("int_lit") && temp.get(i - 1).type.equals("int_lit")){
                if(Integer.parseInt(temp.get(i - 1).value) >= Integer.parseInt((temp.get(i + 1).value)))
                    resObjUpdation(1, tokens[1], temp, i);
                else
                    resObjUpdation(0, tokens[1], temp, i);
                i = 0;
            }
            else if(temp.get(i).type.equals("equals_less") && temp.get(i + 1).type.equals("int_lit") && temp.get(i - 1).type.equals("int_lit")){
                if(Integer.parseInt(temp.get(i - 1).value) <= Integer.parseInt(temp.get(i + 1).value))
                    resObjUpdation(1, tokens[1], temp, i);
                else
                    resObjUpdation(0, tokens[1], temp, i);
                i = 0;
            }
            else if(temp.get(i).type.equals("not_equals") && temp.get(i + 1).type.equals("int_lit") && temp.get(i - 1).type.equals("int_lit")){
                if(Integer.parseInt(temp.get(i + 1).value) != Integer.parseInt(temp.get(i - 1).value))
                    resObjUpdation(1, tokens[1], temp, i);
                else
                    resObjUpdation(0, tokens[1], temp, i);
                i = 0;
            }
            else if(temp.get(i).type.equals("more") && temp.get(i + 1).type.equals("int_lit") && temp.get(i - 1).type.equals("int_lit")){
                if(Integer.parseInt(temp.get(i - 1).value) > Integer.parseInt(temp.get(i + 1).value))
                    resObjUpdation(1, tokens[1], temp, i);
                else
                    resObjUpdation(0, tokens[1], temp, i);
                i = 0;
            }
            else if(temp.get(i).type.equals("less") && temp.get(i + 1).type.equals("int_lit") && temp.get(i - 1).type.equals("int_lit")){
                if(Integer.parseInt(temp.get(i - 1).value) < Integer.parseInt(temp.get(i + 1).value))
                    resObjUpdation(1, tokens[1], temp, i);
                else
                    resObjUpdation(0, tokens[1], temp, i);
                i = 0;
            }
            else
                continue;
        }
    }


    //Evaluation Syntax
    private static void evaluation(ArrayList<Tokenss> tokenList, String tokens[]){
        solvePara(tokenList, tokens);
        solvePower(tokenList, tokens);
        mult_or_div(tokenList, tokens);
        add_or_sub(tokenList, tokens);

        boolean change = false;
        do{
            int before = tokenList.size();
            solveLogOper(tokenList, tokens);
            solveLogExpr(tokenList, tokens);
            change = (before == tokenList.size()) ? false : true;
        }while(change);
    }


    //Resultant Object for operations result
    private static Tokenss resObjUpdation(int res, String type, ArrayList<Tokenss> listTokens, int i){
        Tokenss obj = new Tokenss(String.valueOf(res), type);
        listTokens.set(i - 1, obj);
        listTokens.remove(i);
        listTokens.remove(i);
        return obj;
    }


    //Finding the Brackets for Conditional Statements
    private static int find_the_bracket(int pos, ArrayList<ArrayList<Tokenss>> sents, String[] tokens){
        int depth = 0;
        for(; pos < sents.size(); pos++){
            for(int i = 0; i < sents.get(pos).size(); i++){
                if(sents.get(pos).get(i).type.equals(tokens[24]))
                    depth++;
                if(sents.get(pos).get(i).type.equals(tokens[25]))
                    depth--;
            }
            if(depth == 0)
                return pos;
        }

        return -1;
    }


    //To run the terminal commands
    public static void runCommand(String... cmd) throws IOException, InterruptedException{
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.inheritIO();
        Process obj = pb.start();
        if(obj.waitFor() != 0){
            System.out.println("Error at: " + cmd[0]);
            System.exit(10);
        }
    }


    //To Reduce Run time by caching the SDK path
    private static String getSDKPath() throws IOException {
        if (cachedSdkPath != null) return cachedSdkPath;

        //Checking default place
        File defaultPath = new File("/Library/Developer/CommandLineTools/SDKs/MacOSX.sdk");
        if (defaultPath.exists()) {
            cachedSdkPath = defaultPath.getAbsolutePath();
            return cachedSdkPath;
        }

        //If not in default, ask the system
        Process process = Runtime.getRuntime().exec("xcrun --show-sdk-path");
        try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            cachedSdkPath = r.readLine();
        }

        return cachedSdkPath;
    }
}