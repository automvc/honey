/*
 * Copyright 2013-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.chain;

import org.teasoft.bee.osql.chain.Select;
import org.teasoft.bee.osql.chain.UnionSelect;

/**
 * @author Kingstar
 * @since  1.3
 */
public class UnionSelectImpl extends AbstractSelectToSql implements UnionSelect {

	private static final String L_PARENTHESES = "(";
	private static final String R_PARENTHESES = ")";
	private static final String ONE_SPACE = " ";

	public UnionSelectImpl(){
	}
	private UnionSelect useUnionSelect(String keyword, String subSelect1, String subSelect2) {

		sql.append(L_PARENTHESES);
		sql.append(subSelect1);
		sql.append(R_PARENTHESES);

		sql.append(ONE_SPACE);
		sql.append(keyword);
		sql.append(ONE_SPACE);

		sql.append(L_PARENTHESES);
		sql.append(subSelect2);
		sql.append(R_PARENTHESES);
		return this;
	}

	@Override
	public UnionSelect union(Select subSelect1, Select subSelect2) {
		return union(subSelect1.toSQL(true), subSelect2.toSQL(true));
	}

	@Override
	public UnionSelect union(String subSelect1, String subSelect2) {
		return useUnionSelect("union", subSelect1, subSelect2);
	}

	@Override
	public UnionSelect unionAll(Select subSelect1, Select subSelect2) {
		return unionAll(subSelect1.toSQL(true), subSelect2.toSQL(true));
	}

	@Override
	public UnionSelect unionAll(String subSelect1, String subSelect2) {
		return useUnionSelect("union all", subSelect1, subSelect2);
	}
}
