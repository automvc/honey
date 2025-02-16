/*
 * Copyright 2016-2021 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.util.currency;

import static org.teasoft.honey.util.currency.CurrencyExpressionArith.*;

import java.math.RoundingMode;

/**
 * Bee的用于金融的表达式计算器.
 * 可以使用高精度计算更加方便,提高了Java的计算能力.
 * @author Kingstar
 * @since  1.11-E
 */
public class BeeFinancialCalculator {

	private BeeFinancialCalculator() {

	}

	public static String preCheckExpression(String exp) {
		return CurrencyExpressionArith.preCheckExpression(exp);
	}

	public static String calculate(String expression) {
		return arith(inToPostList(expression));
	}

	public static String calculate(String expression, int scale) {
		return arith(inToPostList(expression), scale);
	}

	public static String calculate(String expression, int scale, RoundingMode divideRoundingMode) {
		return arith(inToPostList(expression), scale, divideRoundingMode);
	}

}
