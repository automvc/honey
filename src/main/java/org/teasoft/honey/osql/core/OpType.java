/*
 * Copyright 2020-2025 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

/**
 * @author Kingstar
 * @since  2.5.2
 */
public enum OpType {

	OP2, LIKE, IN, BETWEEN, NOT_BETWEEN,
	OP2_TO_DATE,
	ONE,
	OP_WITH_FIELD,
	GROUP_BY, HAVING,
	ORDER_BY2, ORDER_BY3, ORDER_BY4,
	
	L_PARENTHESES,R_PARENTHESES,
	//update set
	SET, SET_ADD, SET_MULTIPLY, SET_ADD_FIELD, SET_MULTIPLY_FIELD, SET_WITH_FIELD
}
