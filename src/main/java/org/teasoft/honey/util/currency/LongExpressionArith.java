/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.util.currency;

import java.math.RoundingMode;
import java.util.List;
import java.util.Stack;

/**
 * @author Kingstar
 * @since  1.11
 */
public class LongExpressionArith {// extends CurrencyExpressionArith {

	private LongExpressionArith() {

	}

	static String arith(List<String> list) {
		return arith(list, -1, null);
	}

//	@Override
	static String arith(List<String> list, int scale, RoundingMode divideRoundingMode) {
		Stack<String> v = new Stack<>();
		int len = list.size();
		String t = "";
		for (int i = 0; i < len; i++) {
			t = list.get(i);
			if (!CurrencyExpressionArith.isArithOperate(t)) {
				v.push(t); // 数字入栈
			} else { // 是符号不入栈, 从栈中拿出两个数运算,并将结果入栈
				String b = v.pop();// 先出来的为第二个数
				String a = v.pop();

//				if(scale!=-1 && divideRoundingMode!=null)
//					v.push(CurrencyArithmetic.calculate(a, t, b,scale,divideRoundingMode));
//				else if(scale!=-1)
//				  v.push(CurrencyArithmetic.calculate(a, t, b,scale));
//				else 
				v.push(LongArithmetic.calculate(a, t, b));
			}
		}
		return v.pop();
	}
}
