/*
 * Copyright 2016-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.name;

/**
 * @author Kingstar
 * @since  1.5
 */
public class UpperCaseUnderScoreAndCamelName extends UnderScoreAndCamelName{

	@Override
	public String toTableName(String entityName) {
		return super.toTableName(entityName).toUpperCase();
	}

	@Override
	public String toColumnName(String fieldName) {
		return super.toColumnName(fieldName).toUpperCase();
	}
	
	
}
