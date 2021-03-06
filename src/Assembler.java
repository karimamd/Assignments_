import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

//out main class
//TODO : an error class to save all errors

/**
 * Created by ICY on 3/14/2017.
 */
public class Assembler {
    private final Map<String, OPERATION> opTable;
    private final Map<String, Integer> registerTable;
    //Q:is making symbol table final a correct move ? this means it can only be intialized in the constructor once
    //if we are going to do that then we need to put the constructor in pass 1
    private  Map<String, Integer> symbolTable;
    private static  List<String> directives;
    private static  List<Statement> statements;

    public Assembler() {

        opTable=Instruc.getOPERATIONTable();
        registerTable=Instruc.getRegisterTable();
        directives=Instruc.getDirectives();
        //statements=new ArrayList<>();
        symbolTable = new HashMap<>();
        symbolTable.put(null, 0);

    }
    public static void main(String args[])
    {
        Assembler A=new Assembler();

        //take file path and name as input
        Scanner scanner =new Scanner(System.in);
        System.out.println("please enter file path ended by file name and extention to initiate pass 1");
        String filePath=scanner.nextLine();
        statements=pass1(filePath);


    }



    public static List<Statement>  pass1(String filePath)
    {
        List<Statement> statements=new ArrayList<>();
        //an array list to save read lines to process them one by one
        List <String> linesList=new ArrayList<>();
        //initiate file and start reading
        File f=new File(filePath);
        try{
            linesList=readLines(f);
        }catch(IOException e)
        {
            e.printStackTrace();
            System.out.println("reading file failed");
        }
        //now we have lines in linesList time to parse
        Statement s;
        for(int i=0;i<linesList.size();i++)
        {
             s=characterParse(linesList.get(i));
             if(s!=null)
             {
                 statements.add(s);
             }
             else
             {
                 System.out.println("null statement returned in line no."+i+1);
             }

        }
        return statements;


    }
    //TODO: handling empty lines
    public static Statement characterParse(String line)
    {
        //didn't make them final as they were
        String label;
        String operation;
        String[] symbols;
        String _comment;
        String operandsAndComment;
        String directive;
        boolean extend;
        int _location;
        symbols=new String[2];
        //remove spaces from begining and end of line read
        line.trim();
        //check if line is a comment line
        int commentPosition=line.indexOf('.');
        if(commentPosition==0)
        {
            _comment=line.substring(1);
            return new Statement(_comment);
        }

        //check if there is a comment in the statement
        if(commentPosition>0)
        {
            _comment=line.substring(commentPosition+1);
            line=line.substring(0,commentPosition);
        }
        else
        {
            _comment=null;
        }


        if(line.indexOf(' ')>0)
        {
            //assume first mnemonic is a label
            label=line.substring(0,line.indexOf(' '));
            //make the string "line" contain rest of line without first word
            line=line.substring(line.indexOf(' ')+1).trim();


            //check if 1st substring is operation taking in consideration + may be available
            if(isDirective(label))
            {
                //if label is anything but END we can't use it without label
                if (!label.equalsIgnoreCase("END"))
                {
                    System.out.println("Error: can't use this directive without a label");
                }
                //if 1st statement and directive is not start
                //if 1st statement is a comment this doesn't work
                if(statements.size()==0&&!label.equalsIgnoreCase("START"))
                {
                    System.out.println("Error: can't use this directive in the first line");
                }
                //TODO : handling if storage directives come before end
                //make it a directive with no label
                directive=label;
                label=null;
                symbols[0]=directivesOperand(line);
                symbols[1]=null;
                if(symbols[0]=="FORMAT ERROR")
                {
                    System.out.println("Error: directive operand can't contain any non letters or non digits");
                }
                else if(symbols[0]=="DIGIT ERROR")
                {
                    System.out.println("Error: First char of operand must be a digit !");
                }
                return new Statement(label,directive,false,symbols,_comment);


            }


            else if(isOperation(label))
            {

                operation=label.toUpperCase();
                label=null;
                symbols=getOperands(line);
                extend=(label.charAt(0)=='+');
                //TODO : verilfy validity of operands
                //if operation contains '+' would it have benn removed?
                //java passes by value
                return new Statement(label,operation,extend,symbols,_comment);
            }

            else // treat as label and move to next word in line
            {
                //if string is empty and no operation or directive or first char is not a letter or a'+'
                if(line==""||!Character.isLetter(line.charAt(0))||line.charAt(0)!='+')
                {
                    System.out.println("Error: a label must be followed by a valid operation or  directive");
                }
                else //if it is an operation or directive
                {
                    //split operation from line if there is a space or take it full if no space
                    //no space means no operands i.e. format 1 operations or start or end directives are allowed only
                    //TODO: hit an error if otherwise
                    int spaceIndex=line.indexOf(' ');
                    if(spaceIndex>0)
                    {
                        operation=line.substring(0,spaceIndex);
                        line=line.substring(spaceIndex+1);
                    }
                    else //if no operands
                    {
                        operation=line;
                    }
                    //verify operation or directive
                    if(isDirective(operation))
                    {
                        directive=operation;
                        operation=null;
                        symbols[0]=directivesOperand(line);
                        symbols[1]=null;

                        //checking errors
                        if(symbols[0]=="FORMAT ERROR")
                        {
                            System.out.println("Error: directive operand can't contain any non letters or non digits");
                        }
                        else if(symbols[0]=="DIGIT ERROR")
                        {
                            System.out.println("Error: First char of operand must be a digit !");
                        }

                        //return a statement with a directive
                        return new Statement(label,directive,false,symbols,_comment);

                    }
                    else if(isOperation(operation)) //not a directive
                    {
                        directive=null;
                        symbols=getOperands(line);
                        extend=(label.charAt(0)=='+');
                        return new Statement(label,operation,extend,symbols,_comment);

                    }

                    else //not a directive or operation
                    {
                        System.out.println("Error: a label must be followed by a valid operation or  directive");
                        return null;
                    }
                }
            }



        }
        else //only case is empty line or format 1  or  START/ end directive or error
        {
            if(line=="")
            {
                System.out.println("Empty line");
                return null;
                //TODO: handle case of returning null statement
            }
            else if(isDirective(line))
            {
                if(line.equalsIgnoreCase("START")||line.equalsIgnoreCase("END"))
                {
                    directive=line;
                    return new Statement(null,directive,false,null,_comment);
                }
                return  null;
            }
            else if(isOperation(line))
            {
                operation=line;
                extend=(line.charAt(0)=='+');
                //TODO: hit error if it wasn't a format 1 instruction or RSUB because no operands found
                return new Statement(null,operation,extend,null,_comment);
            }
            else
            {
                System.out.println("Error: not an acceptable operation");
                return null;
            }
        }

      return null;

    }

    public static boolean isDirective(String label)
    {
        for(int i=0;i<directives.size();i++)
        {

                if (label.equalsIgnoreCase(directives.get(i)))
                {
                    return true;
                }

        }

        return false;
    }
    //must be one operand only
    public static String  directivesOperand(String restOfLine)
    {

        //TODO : make it check for hexa chars only else hit error and H only in last letter
        for (int i=0;i<restOfLine.length();i++)
        {
            if(!Character.isLetterOrDigit(restOfLine.charAt(i)))
            {
                return "FORMAT ERROR";
            }
        }

         if(!Character.isDigit(restOfLine.charAt(0)))
        {
            return "DIGIT ERROR";
        }
        //return operand
        return restOfLine;


    }


    public static boolean isOperation(String label)
    {
        label.toUpperCase();
        if(label.charAt(0)=='+')
        {
            label=label.substring(1);
        }
        //TODO : check if '+' is first char and not a format 4 valid instruction
        Map<String, OPERATION> opTable=Instruc.getOPERATIONTable();
        if (opTable.get(label)!=null)
        {
            return true;
        }

        return false;
    }


    public static String [] getOperands(String restOfLine)
    {

        String [] symbols=new String[2];
        int commaIndex=restOfLine.indexOf(',');
        //if 2 operands
        if(commaIndex>0)
        {
            symbols[0]=restOfLine.substring(0,commaIndex);
            symbols[1]=restOfLine.substring(commaIndex+1);
        }
        else //if 1 operand
        {
            symbols[0]=restOfLine;
            symbols[1]=null;
        }
        if(symbols[0]=="")
            symbols[0]=null;

        return symbols;
    }


    public static void pass2()
    {

    }
    public static List<String> readLines(File f)throws IOException
    {
        //file reader object takes a file
        FileReader fr=new FileReader(f);
        //buffer takes a file reader object
        BufferedReader br=new BufferedReader(fr);
        //variable to store each line read
        String line;
        //list of lines of program
        List<String>lines=new ArrayList<>();

        while((line=br.readLine())!=null)
        {
            lines.add(line);

        }
        //print read lines by their numbers
        for(int i=0;i<lines.size();i++)
        {
            System.out.println(i+" - "+lines.get(i));
        }
        //close readers stream
        br.close();
        fr.close();
        return lines;
    }


  /*  public static void main(String args[])
    {
        //must put file path and without .txt
        File f=new File("E:\\self learning\\CodeForces\\src\\test");
        //function must be called between try and catch
        try{
            readLines(f);
        }catch(IOException e)
        {
            e.printStackTrace();
        }
    }

*/

}
