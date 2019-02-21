package proj1;
import java.io.*;
import java.util.*;

public class Main {
	//prnt and po integers to prevent the conflict in assembly 
	public static int prnt=0;
	public static int po=0;
	public static String prog ="";
	
	public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException{
		File file = new File(args[0]);
		Scanner scan = new Scanner(file);
		prog=args[0].substring(0, args[0].length()-3);
		PrintStream out = new PrintStream( new File(prog + ".asm"));
		out.println("code segment");
		int nofLines =0; //number of lines in .co file

		HashSet<String> varNames = new HashSet<String>();		//set of variable names
		while(scan.hasNextLine()){
			//read all lines
			nofLines++;
			String line = scan.nextLine();
			if(line.length()!=0){
				int eq = line.indexOf("=");
				if(eq == -1){
					//if there is only the variable name on the line, then print the variable
					printVar(line,varNames,out,nofLines);
				}
				else if(eq==line.length()-1){
					//if there is no expression after assignment operator, then error message appears
					error(nofLines);
					scan.close();
					out.close();
					return;
				}
				else{
					boolean comma = false; //controls power function
					String name = line.substring(0, eq); //name of the variable
					line = line.substring(eq+1);
					//if there are more than one variable before = operator,then error
					if(name.contains(" ")){
						for(int i=name.indexOf(' ')+1;i<name.length();i++){
							if(name.charAt(i)!=' '){
								error(nofLines);
								scan.close();
								out.close();
								return;
							}
						}
						//the blank space between name and = is deleted
						name = name.substring(0, name.indexOf(' '));
					}
					
					//if any letter is upper case puts _ after it
					String newName="";
					for(int k=0;k<name.length();k++){
						newName+=name.charAt(k);
						if(name.charAt(k)<='Z'&&name.charAt(k)>='A'){
							newName+='_';
						}
					}
					//if variable does not defined before defines the variable
					name="var_"+newName;
					if(!varNames.contains(name)) {
						/*for a variable x in input file, creates two variable for its two parts
						 *var_xs and var_xi 
						 */
						varNames.add(name);
						out.print(name+"i dw ?\n");
						out.print(name+"s dw ?\n");
					}
					out.println("push offset "+ name+"s");
					out.println("push offset "+ name+"i");

					//turn the infix expression to postfix expression
					Queue<String> postfix = readInfix(line, nofLines);
					while(!postfix.isEmpty()){
						/*
						 * For all tokens in postfix expression calls the related function or pushes the values
						 */
						String s = postfix.poll();
						if(s.equals("*")){ //* means multiplication
							mult(out);
						}
						else if(s.equals("+")){ //+ means addition
							add(out);
						}
						else if(s.equals(",")){ //comma boolean is to control pow operation
							comma = true;
						}
						else if(s.equals("pow")){
							//pow&comma means inside of pow is calculated
							//pow&!comma means pow is just called
							if(comma) pow(out);
						}
						else if((s.charAt(0)<='z' && s.charAt(0)>='a')||(s.charAt(0)<='Z' && s.charAt(0)>='A')){
							//if it is a variable
							s="var_"+s;
							if(!varNames.contains(s)){
								//if variable is not defined before pushes 0
								out.println("push 0h");
								out.println("push 0h");
							}
							else{
								//else pushes its value
								out.println("push "+s+"s");
								out.println("push "+s+"i");
							}
						}
						else{
							//if it is a numeric value
							int l = s.length();
							for(int a=l;a<8;a++){
								//makes it 8 digit number while adding 0 to the front
								s = "0"+s;
							}
							out.print("push 0"+s.substring(4)+"h\n");
							out.print("push 0"+s.substring(0, 4)+"h\n");
						}
					}	
					//assing the last value to the variable
					out.print("pop dx\npop cx\npop bx\n");
					out.print("mov [bx],dx\npop bx\nmov[bx],cx\n");
				}
			}
		}
		//prints the end commands then closes the scanner and PrintStream
		out.print("int 20h\n");
		out.println("code ends");
		scan.close();
		out.close();
	}


	public static boolean checkPrn(String line){
		//checks whether the parenthesis are balanced, returns true if parenthesis are balanced, otherwise false
		Stack<Character> checkStack=new Stack<Character>();
		//A stack will be used to control if there is unnecessary or missing parenthesis.
		for(int i=0;i<line.length();i++){
			if(line.charAt(i)=='('){
				checkStack.push('(');
			}else if(line.charAt(i)==')'){
				if(checkStack.isEmpty()){
					//This means there is one missing (, so false is returned
					return false;
				}
				checkStack.pop();
			}
		}
		return checkStack.isEmpty();
	}

	public static Queue<String> readInfix(String line,int nofLines) throws FileNotFoundException{
		/*This method takes one String and return the postfix version of the infix expression in this String and 
		 * calls error message if there is error expression in this given line number		*/
		Queue<String> q = new LinkedList<String>();
		Stack<String> s = new Stack<String>();
		//To convert from infix to posfix, a stack will be used. The converted version will be kept in a queue
		boolean check=checkPrn(line);
		if(!check){
			//If the parenthesis are not balanced then error
			error(nofLines);
			return q;
		}
		for(int i=0;i<line.length();i++){
			String token="";
			if(line.charAt(i)<='9' && line.charAt(i)>='0'){
				//if sees a number finds all the numbers just after it
				if(line.length()-1!=i){
					int n=i+1;
					while((line.charAt(n)<='9' && line.charAt(n)>='0')||(line.charAt(n)<='f' && line.charAt(n)>='a')||(line.charAt(n)<='F' && line.charAt(n)>='A')){
						n++;
						if(n>=line.length()-1) break;
					}
					if(!(n>=line.length()-1)&&!(line.charAt(n)==' '||line.charAt(n)==')'||line.charAt(n)=='*'||line.charAt(n)=='+'||line.charAt(n)==',')) error(nofLines);
					else{
					    if(n==line.length()) token=line.substring(i);
						else if(n==line.length()-1) token = line.substring(i,n);
						else token=line.substring(i,n);
						q.add(token);
					}
					i=i+token.length()-1;
				}
				else{
					token+=line.charAt(i);
					q.add(token);
				}
			}else if(line.charAt(i)=='*'){
				//if * pushes to the stack
				s.push("*");
			}else if(line.charAt(i)=='('){
				//if ( pushes to the stack
				s.push("(");
			}else if(line.charAt(i)=='+'){
				while(!s.isEmpty()){
					/*
					 * since * has higher precedence over +
					 * if sees + pops * from stack,adds to queue
					 */
					if(s.peek().equals("*")){
						q.add(s.pop());
					}
					else if(s.peek().equals("+")||s.peek().equals("(")||s.peek().equals(",")){
						break;
					}
				}
				s.push("+");
			}
			else if(line.charAt(i)==','){
				//if sees , pops till the (
				//this ( is the  power operation's (
				while(!s.isEmpty()&&!s.peek().equals("(")){
					q.add(s.pop());
				}
				//then adds ,
				q.add(",");
			}else if(line.charAt(i)==')'){
				//if sees ), pops till the (
				while(!s.isEmpty()&&!s.peek().equals("(")){
					q.add(s.pop());
				}
				//then pops the ( 
				if(!s.isEmpty()&&s.peek().equals("(")) s.pop();
				//meaning of p at the of the stack is power operation
				if(!s.isEmpty() && s.peek().equals("p")){
					s.pop();
					q.add("pow");
				}
			}else if(line.charAt(i) == 'p'){
				//if sees p looks if it is pow(
				if(i+3<line.length() && line.charAt(i+1)=='o' && line.charAt(i+2) =='w' && line.charAt(i+3)=='('){ 
					s.push("p");
					q.add("pow");
					s.push("(");
					i=i+3;
				}	
			}else if((line.charAt(i)<='z' && line.charAt(i)>='a')||(line.charAt(i)<='Z' && line.charAt(i)>='A')){
				//if begins with a letter take the variable name
				token = "";
				token += line.charAt(i);
				for(int e=i+1;e<line.length();e++){
					if((line.charAt(e)<='z' && line.charAt(e)>='a')||(line.charAt(e)<='Z' && line.charAt(e)>='A')||
							(line.charAt(e)<='9' && line.charAt(e)>='0')){
						token += line.charAt(e);
					}
					else if(!(line.charAt(e)==' '||line.charAt(e)=='='||line.charAt(e)=='+'||line.charAt(e)=='*'
							||line.charAt(e)==')'||line.charAt(e)==',')){
						error(nofLines);
					}
					else break;
				}
				//change token's name according to the variable name rule
				i += token.length()-1;
				String nn="";
				for(int k=0;k<token.length();k++){
					nn+=token.charAt(k);
					if(token.charAt(k)<='Z'&&token.charAt(k)>='A'){
						nn+='_';
					}
				}
				q.add(nn);
			}
		}
		while(!s.isEmpty()){
			//transfers the elements from the stack to the queue
			q.add(s.pop());
		}
		int w=q.size();
		if(w==2||w==0){
			//if q has no elements or has only two it is an error
			error(nofLines);
		}
		if(q.peek().equals("*")||q.peek().equals("+")){
			//if queue's first element is an binary operator it is an error
			error(nofLines);
		}
		return q;
	}


	public static void error(int nofLines) throws FileNotFoundException{
		/* writes the assembly code to print error message with given line number*/
		PrintStream error = new PrintStream( new File(prog + ".asm"));
		//err is the error message
		String err = "Line "+ nofLines + ":Syntax Error";
		//number is the number of digits 
		int number=1;
		while(nofLines>=10){
			number++;
			nofLines/=10;
		}
		//assembly code of the error message
		error.print("code segment\nmov bx, msg1\nmov cx,"+(18+number)+"d\nmov ah,02h\nmore:\nmov dl,[bx]\n"
				+ "int 21h\ninc bx\ndec cx\njnz more\nint 20h\nmsg1:\ndb '" + err + "'\ncode ends\n");
		//after error message finishes the program
		System.exit(1);
	}

	public static void printVar(String line,HashSet<String> varNames,PrintStream out, int nofLines) throws FileNotFoundException{
		Queue<String> q=readInfix(line, nofLines);
		if(q.size()==1 && ((q.peek().charAt(0)<='z'&& q.peek().charAt(0)>='a')|| (q.peek().charAt(0)<='Z'&& q.peek().charAt(0)>='A'))){
			//if there is only one element in the queue and this element is a variable name, then the variable is printed
			String name=q.poll();
			name="var_"+name;
			if(!varNames.contains(name)){
				//if there is not a variable defined with this name default value 0 is printed
				out.println("mov bx,0h");
				printName(out);
				out.println("mov bx,0h");
				printName(out);
			}else{
				//if the variable is defined before, this variable is printed
				out.println("mov bx,"+name+"i");
				printName(out);
				out.println("mov bx,"+name+"s");
				printName(out);
			}
		}else{
			//else an expression will be printed
			boolean comma=false;
			//boolean comma is used to keep track of the commas appeared. This is done to help the pow method
			while(!q.isEmpty()){
				//this procedure is same with the procedure in main method
				String s = q.poll();
				if(s.equals("*")){
					mult(out);
				}
				else if(s.equals("+")){
					add(out);
				}
				else if(s.equals(",")){
					comma = true;
				}
				else if(s.equals("pow")){
					if(comma) pow(out);
				}
				else if((s.charAt(0)<='z' && s.charAt(0)>='a')||(s.charAt(0)<='Z' && s.charAt(0)>='A')){
					s="var_"+s;
					if(!varNames.contains(s)){
						out.println("push 0h");
						out.println("push 0h");
					}
					else{
						out.println("push "+s+"s");
						out.println("push "+s+"i");
					}
				}
				else{
					int l = s.length();
					for(int a=l;a<=8;a++){
						s = "0"+s;
					}
					out.print("push "+s.substring(0, 4)+"h\n");
					out.print("push "+s.substring(4)+"h\n");
				}
			}
			out.println("pop bx");
			printName(out);
			out.println("pop bx");
			printName(out);
		}
		//Below, new line is printed in Assembly
		out.println("mov ah,02h\nmov dl,0ah\nint 21h");
	}

	public static void printName(PrintStream out){
		prnt++;
		//prints the name of variable
		out.println("mov cx,4h\nmov ah,2h\nloop"+prnt+":\nmov dx,0fh\nrol bx,4h\nand dx,bx\ncmp dl,0ah\njae hexdigit"+prnt+"\n"+
				"add dl,'0'\njmp output"+prnt+"\nhexdigit"+prnt+":\nadd dl,'A'\nsub dl,0ah\noutput"+prnt+":\nint 21h\ndec cx\njnz loop"+prnt);
	}

	public static void add(PrintStream out){
		//writes the assembly code of addition
		out.print("pop ax\npop bx\npop cx\npop dx\nadd bx,dx\nadc ax,cx\npush bx\npush ax\n");
	}

	public static void mult(PrintStream out){	
		//writes the assembly code of multiplication
		out.print("pop bx\npop cx\npop di\npop si\nmov ax,1h\n"
				+ "mul cx\nmul si\npush ax\nmov ax,1h\nmul bx\n"
				+ "mul si\npush ax\nmov ax,1h\nmul cx\nmul di\n"
				+ "pop bx\nadd ax,bx\nadd ax,dx\npush ax\n");
	}

	public static void pow(PrintStream out){
		//makes the power operations via calling the mult method if necessary
		po++;
		int count=po;
		out.print("bx_"+count+" dw 0\npow_"+count+" dw 0\ndi_"+count+" dw 0\npop pow_"+count+"\npop pow_"+count+"\npop di_"+count
				+ "\npop bx_"+count+"\ncmp pow_"+count+",0h\nje zero"+count+"\njne notzero"+count+"\nzero"+count+":\npush 1h\npush 0h\n"
				+ "jmp fin"+count+"\nnotzero"+count+":\ncmp pow_"+count+",1h\n"+ "je one"+count+"\njne two"+count+"\n"
				+ "one"+count+":\npush bx_"+count+"\npush di_"+count+"\njmp fin"+count+"\ntwo"+count+":\npush bx_"+count+"\n"
				+ "push di_"+count+"\nfor"+count+":\npush bx_"+count+"\npush di_"+count+"\n");
		mult(out);
		out.print("\ndec pow_"+count+"\ncmp pow_"+count+",2h\njae for"+count+"\nfin"+count+":\nmov ax,1h\n");
	}
	
	
	
	
	
	
	
	
}