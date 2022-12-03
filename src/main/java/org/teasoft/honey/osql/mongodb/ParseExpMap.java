/*
 * Copyright 2016-2023 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.mongodb;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;


/**
 * 解析条件表达式
 * @author Jade
 * @since  2.0
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class ParseExpMap {

	private static final String OR = "or";
	private static final String AND = "and";

	public static Map parse(Stack s) {
		Stack helper=new Stack<>();
		while (!s.isEmpty()) {
			if (")".equals(s.peek())) {
				helper.push(s.pop());
				continue;
			}else if ("(".equals(s.peek())) {
				s.pop();
				if(!helper.isEmpty()) helper.pop();
				continue;
			}

			if (s.size() == 1) {
//				System.err.println("helper是否为空?? "+helper.isEmpty());
				return (Map) s.pop();
			}
			
			// 判断当前的
			Map c = (Map) s.pop(); //首个元素
			String peek=(String)s.peek(); //第二个元素
			
			if (AND.equals(peek) || OR.equals(peek)) {
				s.pop(); // 出 AND || OR
				 Map n=null;
				if(")".equals(s.peek())) {
					helper.push(c);
					helper.push(peek);
					helper.push(s.pop());  //")"
					continue;
				}else {
					n = op((Map) s.pop(), c, peek);
				}
				s.push(n);
			} else if ("(".equals(s.peek())) {
				s.pop(); // 出左括号
				if (!helper.isEmpty()) {
					helper.pop(); //出对应的右括号
					if (!helper.isEmpty()) {
						if (")".equals(helper.peek())) { //还有一重右括号,先放回s
							s.push(c);
							continue;
						}
						
						String op = (String) helper.pop();
						Map old = (Map) helper.pop();
						Map n = op(c, old, op);
						s.push(n);
					}else {
						 s.push(c);
					}
//					}
				}else {
				  s.push(c);
				}
			} 
		}
		return null;
	}

	public static Map op(Map a, Map b, String op) {
		List list = new ArrayList();
		list.add(a);
		list.add(b);
		Map n = EasyMapUtil.createMap("$" + op, list);
		return n;
	}


/*	
	public static void main(String[] args) {
		
		Map t1=EasyMapUtil.createMap("age", 1);
		Map t2=EasyMapUtil.createMap("name", "t2");
		Map t3=EasyMapUtil.createMap("name", "t3");
		
		Stack stack=new Stack<>();
		stack.push("(");
	    stack.push(t1);
		stack.push("and");
		stack.push(t2);
		stack.push(")");
		System.out.println(parse(stack));
		
		
		stack=new Stack<>();
		stack.push("(");
	    stack.push(t1);
		stack.push("or");
		stack.push(t2);
		stack.push(")");
		stack.push("and");  //diff
		stack.push(t3);
//		[(, {age=1}, or, {name=t2}, ), and, {name=t3}]
		System.out.println(parse(stack));
		
		
		stack=new Stack<>();
		stack.push("(");
	    stack.push(t1);
		stack.push("or");
		stack.push(t2);
		stack.push(")");
		stack.push("or"); //diff
		stack.push(t3);
//		[(, {age=1}, or, {name=t2}, ), or, {name=t3}]
		System.out.println(parse(stack));
		
		
		stack=new Stack<>();
		stack.push("(");
		stack.push("(");
	    stack.push(t1);
		stack.push("and");
		stack.push(t2);
		stack.push(")");
		stack.push(")");
//		[(, (, {age=1}, and, {name=t2}, ), )]
		System.out.println(parse(stack));
		
		stack=new Stack<>();
		stack.push("(");
		stack.push("(");
		stack.push("(");
	    stack.push(t1);
		stack.push("and");
		stack.push(t2);
		stack.push(")");
		stack.push(")");
		stack.push(")");
		System.out.println(parse(stack));
		
		
		Map t4=EasyMapUtil.createMap("name", "t4");
		Map t5=EasyMapUtil.createMap("name", "t5");
		stack=new Stack<>();
		stack.push("(");
	    stack.push(t1);
		stack.push("or");
		
		stack.push("(");  //diff
	    stack.push(t4);
		stack.push("and");
		stack.push(t5);
		stack.push(")");
		
		stack.push(")");
		stack.push("and");  //diff
		stack.push(t3);
//		[(, {age=1}, or, (, {name=t4}, and, {name=t5}, ), ), and, {name=t3}]
		System.out.println(parse(stack));
		
		
		//diff
		stack=new Stack<>();
		stack.push(t3);
		stack.push("and");  
		
		stack.push("(");
	    stack.push(t1);
		stack.push("or");
		
		stack.push("(");  
	    stack.push(t4);
		stack.push("and");
		stack.push(t5);
		stack.push(")");
		
		stack.push(")");

		System.out.println(parse(stack));
	}*/
	
}
