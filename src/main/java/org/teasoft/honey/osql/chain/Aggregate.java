/*
 * Copyright 2013-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.chain;

import org.teasoft.bee.osql.FunctionType;
import org.teasoft.honey.osql.core.FunAndOrderTypeMap;
import org.teasoft.honey.osql.util.NameCheckUtil;

/**
 * @author Kingstar
 * @since  1.3
 */
public class Aggregate {
	private static String L_PARENTHESES = "(";
	private static String R_PARENTHESES = ")";

	public static String selectWithFun( FunctionType functionType,String fieldForFun) {
		checkField(fieldForFun);
		return FunAndOrderTypeMap.transfer(functionType.getName()) + L_PARENTHESES + fieldForFun + R_PARENTHESES; // eg. sum(price)
	}

	public static String max(String field) {
		return selectWithFun(FunctionType.MAX,field);
	}

	public static String min(String field) {
		return selectWithFun(FunctionType.MIN,field);
	}

	public static String sum(String field) {
		return selectWithFun(FunctionType.SUM,field);
	}

	public static String avg(String field) {
		return selectWithFun(FunctionType.AVG,field);
	}

	public static String count(String field) {
		return selectWithFun(FunctionType.COUNT,field);
	}
	
	private static void checkField(String field){
//		if(CheckField.isIllegal(field)) {
//			throw new BeeErrorFieldException("The field: '"+field+ "' is illegal!");
//		}
		NameCheckUtil.checkName(field);
	}
}
