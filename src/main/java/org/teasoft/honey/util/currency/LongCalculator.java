/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.util.currency;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.teasoft.honey.util.StringUtils;

/**
 * @author Kingstar
 * @since  1.11
 */
public class LongCalculator {

	private LongCalculator() {

	}

	public static String preCheckExpression(String exp) {
		return CurrencyExpressionArith.preCheckExpression(exp);
	}

	public static String calculate(String expression) {
		return LongExpressionArith.arith(CurrencyExpressionArith.inToPostList(expression));
	}

	// 表达式只包含一个变量 value==null,需要调用者判断
	public static String calculate(String expression, String value) {

		List<String> list = CurrencyExpressionArith.inToPostList(expression, false);
		if (list == null) return "";
		for (int i = 0; list != null && i < list.size(); i++) {
			String listV = list.get(i);
			if (StringUtils.isContainLetter(listV)) {
				list.set(i, value);
			}
		}
		return LongExpressionArith.arith(list);
	}

	// 只能是变量的值,不给循环引用
	public static String calculate(String expression, Map<String, String> pairKV) {
		List<String> list = CurrencyExpressionArith.inToPostList(expression, false);
		List<String> newList = new ArrayList<>();
		for (int i = 0; list != null && i < list.size(); i++) {
			String listV = list.get(i);
			String mapV = pairKV.get(listV);

			if (mapV != null) newList.add(mapV); // 变量的值,用map的替换
			else newList.add(listV);
		}

		return LongExpressionArith.arith(newList);
	}

}
