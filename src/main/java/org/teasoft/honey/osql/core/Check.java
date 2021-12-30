/*
 * Copyright 2016-2021 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

/**
 * @author Kingstar
 * @since  1.9
 */
public class Check {

	public static boolean isNotValidExpression(String expression) {
		if (expression == null || expression.contains("--") || expression.contains("#")
				|| expression.contains("|") || expression.contains(";") || expression.contains("/*")) {
			return true;
		}
		return false;
	}

}
