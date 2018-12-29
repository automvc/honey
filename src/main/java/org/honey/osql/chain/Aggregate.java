package org.honey.osql.chain;

import org.bee.osql.FunctionType;

/**
 * @author Kingstar
 * @since  1.3
 */
public class Aggregate {
	private static String L_PARENTHESES = "(";
	private static String R_PARENTHESES = ")";

	public static String selectWithFun(String fieldForFun, FunctionType functionType) {
		return functionType.getName() + L_PARENTHESES + fieldForFun + R_PARENTHESES; // eg. sum(price)
	}

	public static String max(String field) {
		return selectWithFun(field, FunctionType.MAX);
	}

	public String min(String field) {
		return selectWithFun(field, FunctionType.MIN);
	}

	public String sum(String field) {
		return selectWithFun(field, FunctionType.SUM);
	}

	public String avg(String field) {
		return selectWithFun(field, FunctionType.AVG);
	}

	public String count(String field) {
		return selectWithFun(field, FunctionType.COUNT);
	}
}
