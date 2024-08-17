/*
 * Copyright 2013-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.chain;

import java.util.List;

import org.teasoft.bee.osql.chain.Select;
import org.teasoft.bee.osql.chain.UnionSelect;
import org.teasoft.honey.osql.core.AbstractToSqlForChain;
//import org.teasoft.honey.osql.core.HoneyContext;
import org.teasoft.honey.osql.core.K;

/**
 * @author Kingstar
 * @since  1.3
 * @since  2.4.0
 */
public class UnionSelectImpl extends AbstractToSqlForChain implements UnionSelect {

	private static final String L_PARENTHESES = "(";
	private static final String R_PARENTHESES = ")";
	private static final String ONE_SPACE = " ";
	
//	private StringBuffer sql = new StringBuffer(); //fixed bug.
	
	public UnionSelectImpl() {}

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
//		pvList = new ArrayList<>();//??
		getPvList().addAll((List)subSelect1.getPvList());   //接口要加Select
		getPvList().addAll((List)subSelect2.getPvList());   //接口要加Select
		return union(subSelect1.toSQL(), subSelect2.toSQL());
	}

	@Override
	public UnionSelect union(String subSelect1, String subSelect2) {
		return useUnionSelect(K.union, subSelect1, subSelect2);
	}

	@Override
	public UnionSelect unionAll(Select subSelect1, Select subSelect2) {
//		pvList = new ArrayList<>();//??
		getPvList().addAll((List)subSelect1.getPvList());   //接口要加Select
		getPvList().addAll((List)subSelect2.getPvList());   //接口要加Select
		return unionAll(subSelect1.toSQL(), subSelect2.toSQL());
	}

	@Override
	public UnionSelect unionAll(String subSelect1, String subSelect2) {
		return useUnionSelect(K.unionAll, subSelect1, subSelect2);
	}
	
	@Override
	public UnionSelect unionAll(String[] subSelects) { //无法使用占位符
		return useUnionSelect(K.unionAll, subSelects);
	}
	
	private UnionSelect useUnionSelect(String keyword, String[] subSelects) {
        if(subSelects==null || subSelects.length==0) {
        	//do nothing
        }else if (subSelects.length == 1) {
			sql.append(subSelects[0]);
		} else {
			sql.append(L_PARENTHESES);
			sql.append(subSelects[0]);
			sql.append(R_PARENTHESES);

			for (int j = 1; j < subSelects.length; j++) {
				sql.append(ONE_SPACE);
				sql.append(keyword);
				sql.append(ONE_SPACE);

				sql.append(L_PARENTHESES);
				sql.append(subSelects[j]);
				sql.append(R_PARENTHESES);
			}
		}
		return this;
	}
	
}
