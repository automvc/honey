/*
 * Copyright 2016-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.name;

import org.teasoft.bee.osql.NameTranslate;

/**
 * return original name
 * 返回原名称
 * @author Kingstar
 * @since  1.5
 */
public class OriginalName implements NameTranslate{

	@Override
	public String toTableName(String entityName) {
		return entityName;
	}

	@Override
	public String toColumnName(String fieldName) {
		return fieldName;
	}

	@Override
	public String toEntityName(String tableName) {
		return tableName;
	}

	@Override
	public String toFieldName(String columnName) {
		return columnName;
	}

}
