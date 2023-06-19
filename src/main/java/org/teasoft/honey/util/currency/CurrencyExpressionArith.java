/*
 * Copyright 2016-2021 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.util.currency;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
/**
 * @author Kingstar
 * @since  1.11
 */
import java.util.Stack; 


public class CurrencyExpressionArith {
	
	private CurrencyExpressionArith() {
		
	}
	
	static String arith(List<String> list) {
		return arith(list, -1);
	}
	
	static String arith(List<String> list,int scale) {
		return arith(list, scale, null);
	}
    
//	计算逆波兰表达式的值
protected static String arith(List<String> list,int scale,RoundingMode divideRoundingMode) {
		Stack<String> v = new Stack<>();
		int len = list.size();
		String t = "";
		for (int i = 0; i < len; i++) {
			t = list.get(i);
			if (!isArithOperate(t)) {
				v.push(t); //数字入栈
			} else { //是符号不入栈, 从栈中拿出两个数运算,并将结果入栈
				String b = v.pop();//先出来的为第二个数
				String a = v.pop();
                
				if(scale!=-1 && divideRoundingMode!=null)
					v.push(CurrencyArithmetic.calculate(a, t, b,scale,divideRoundingMode));
				else if(scale!=-1)
				  v.push(CurrencyArithmetic.calculate(a, t, b,scale));
				else 
				 v.push(CurrencyArithmetic.calculate(a, t, b));
			}
		}
		return v.pop();
	}
    
    
	static boolean isArithOperate(String op) {
		return "+".equals(op) || "-".equals(op) || "*".equals(op) || "/".equals(op) || "%".equals(op);
	}
	
	static boolean isOperate(char op) { //包括  ()
		return op == '+' || op == '-' || op == '*' || op == '/' || op == '%' || op == '(' || op == ')';
	}
	
	private static boolean isArithOperate(char op) { 
		return op == '+' || op == '-' || op == '*' || op == '/' || op == '%';
	}
	
	
	static String inToPost(String exp){
		return inToPost(exp, true);
	}

    /**
     * 中缀转后缀
     * @param exp
     * @return
     */
	static String inToPost(String exp,boolean isNumberCalculate){
    	List<String> list=inToPostList(exp,isNumberCalculate);
//    	String t="";
    	StringBuffer sbu=new StringBuffer();
    	for (int i = 0; i < list.size(); i++) {
//    		t+=list.get(i)+"  ";
    		sbu.append(list.get(i));
    		sbu.append("  ");
		}
    	return sbu.toString();
    }
	
   private static boolean isEqualBrackets(String s) {
		char ch[]=s.toCharArray();
		int left=0;
		int right=0;
		for (int i = 0; i < ch.length; i++) {
			if(ch[i]=='(') left++;
			else if(ch[i]==')') right++;
		}
		
		return left==right;
	}
	static List<String> inToPostList(String s){
		return inToPostList(s, true);
		
	}
	
	//带变量的,从这调用
	static List<String> inToPostList(String s,boolean isNumberCalculate){
    	 Stack<Character> op = new Stack<>();  //堆栈用来放符号
    	 
    	 if(s==null) return null;
    	 s=s.replace(" ", "");
    	 s=s.replace("(-", "(0-");
    	 s=s.replace("(+", "(0+");
    	 s=s.replace("÷", "/");  //x不允许用,因变量可能包括有    若没有变量,可以由调用方转换
    	 if(s.startsWith("-")) s="0"+s;
    	 else if(s.startsWith("+")) s="0"+s;
    	 
//    	 检测左右括号数是否一样.
    	 if(! isEqualBrackets(s)) throw new RuntimeException("the number of '(' and ')' is not equal !");
    	 
         char[] arr = s.toCharArray();
         int len = arr.length;
//         String out = "";
         List<String> list=new ArrayList<>();
         String temp="";
         boolean isArithOp=false;
         for(int i = 0; i < len; i++){
             char ch = arr[i];
             if(ch == ' ') continue;

//             if(ch >= '0' && ch <= '9') {
//             if (  ((! isNumber) && ! isOperate(ch) )
//            	|| (   isNumber && (ch >= '0' && ch <= '9'))
//            		 ) { //不是操作符
            
              if(! isOperate(ch)) {
            	  
            	  if(isNumberCalculate && ! (ch >= '0' && ch <= '9')) 
            		  throw new  RuntimeException("there is some other char(not number) : " +ch);
            	  
                 temp+=(ch+"");
                 if( (i==len-1) || isOperate(arr[i+1]) ) {
                	 list.add(temp); 
//                	 System.out.println(list.toString());
                	 temp="";
                 }
                 isArithOp=false;//不是运算符
                 continue;
             }
             
             if(ch == '(') op.push(ch);  // 左括号直接入栈
//             System.out.println(op);

             if(ch == '+' || ch == '-'){
//            	 有连续两个运算符
            	 if(isArithOp) throw new RuntimeException("There are two consecutive operators: "+arr[i-1]+ch);
            	 isArithOp=true;
                 while(!op.empty() && (op.peek() != '(')) { //栈有 +-*/ 都要出栈,碰到(就停止
//                	 System.out.println("+-   是否有运行到???????");
                	 list.add(op.pop()+"");
//                	 System.out.println(list.toString());
                 }
                 op.push(ch);
//                 System.out.println(op);
                
                 continue;
             }

             if(ch == '*' || ch == '/' || ch == '%'){
//            	 有连续两个运算符
            	 if(isArithOp) throw new RuntimeException("There are two consecutive operators: "+arr[i-1]+ch);
            	 isArithOp=true;
                 while(!op.empty() && (op.peek() == '*' || op.peek() == '/' || op.peek() == '%' )) { //栈有同级符号出栈
//                	 System.out.println("* / 是否有运行到???????");
                	 list.add(op.pop()+"");
//                	 System.out.println(list.toString());////遇到*/号,栈有 同级元素*/ 都要出栈
                 }
                 op.push(ch);
//                 System.out.println(op);
                 continue;
             }

             if(ch == ')'){   // ')'不会入栈
                 while(!op.empty() && op.peek() != '(') { 
                	 list.add(op.pop()+"");//符号出栈,直到遇到( 左括号
//                	 System.out.println("符号出栈: "+list.toString()); 
                 }//end while
                 op.pop(); //对应"(" 出栈
                 continue;
             }
         }
         while(!op.empty()) {
        	 list.add(op.pop()+"");
//        	 System.out.println(list.toString());
         }
         
//         System.out.println(list);
         return list;
    }
	
	
	public static String preCheckExpression(String exp) {
		String s="";
		boolean notEmpty=false;
		if(!isEqualBrackets(exp)) {
			s+="the number of '(' and ')' is not equal !";
			notEmpty=true;
		}
		
		String str=illegal(exp);
		if(!"".equals(illegal(exp))) {
			if(notEmpty)s+="\n";
			s+=str;
		}
		
		return s;
	}
	
	private static String illegal(String exp) {
		char ch[] = exp.toCharArray();
		boolean isArithOp = false; //寻找连接运算符
		String illegalChar = "";
		String consecutiveArithStr = ""; //记录连接运算符
		int lastIndex=0;  //上次运行符的下标
		boolean moreThanOne=false;  //
		int len=ch.length;
		for (int i = 0; i < ch.length; i++) {

			if (!isOperate(ch[i])) {
				isArithOp = false;
				if(moreThanOne) consecutiveArithStr +="  ";  //切换状态时,加逗号
				moreThanOne=false;
				if (!(ch[i] >= '0' && ch[i] <= '9')) {
					illegalChar = illegalChar+ch[i];
					if( (i==len-1) || isOperate(ch[i+1]) ) {
						illegalChar=illegalChar+ ",";
					}
				}
			} else {

				if (isArithOperate(ch[i])) {
					if (isArithOp) {
						   if(!moreThanOne) {
						    consecutiveArithStr += ch[i - 1]+"" + ch[i]+"";
						    moreThanOne=true;
						   }else {
							 if(i-1==lastIndex)
								 consecutiveArithStr += ch[i]+"";
						   }
					}
					isArithOp = true;
					lastIndex=i;
				}
			}
		}
		
		boolean f1=false, f2=false;
		if (!"".equals(illegalChar)) {
			illegalChar="there are some other String(not number) : "+illegalChar;
			f1=true;
		}
		
		if(!"".equals(consecutiveArithStr)) {
			 consecutiveArithStr="There are two consecutive operators:"+consecutiveArithStr;
			 f2=true;
		} 
		
		if(f1 && f2) return illegalChar+"/n"+consecutiveArithStr;
		if(f2) return consecutiveArithStr;

		return illegalChar;
	}

}
