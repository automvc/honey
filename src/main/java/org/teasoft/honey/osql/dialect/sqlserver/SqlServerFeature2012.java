/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.dialect.sqlserver;

import org.teasoft.bee.osql.dialect.DbFeature;
import org.teasoft.honey.osql.core.HoneyContext;
import org.teasoft.honey.osql.core.HoneyUtil;
import org.teasoft.honey.util.StringUtils;

/**
 * support paging for Sql Server  which version >=2012
 * 强烈推荐表加一个主键列名称为"id".
 * 默认分页排序使用:order by id;若select语句已带有order by则默认的order by id会被删除.
 * @author Kingstar
 * @since  1.17
 */
public class SqlServerFeature2012 extends AbstractSqlServerFeature implements DbFeature {
	
	private static String PAGING = " order by id offset #start row fetch next #size rows only";
	private static String DISTINCT ="distinct";
	private boolean initFlag=false;
	
	
	private void init(){ //不使用一般{}块,防止在HoneyConfig使用new SqlServerFeature2012()重复加载.
		if (HoneyUtil.isSqlKeyWordUpper()) {
			PAGING = PAGING.toUpperCase();
			DISTINCT=DISTINCT.toUpperCase();
		}
	}
	
	/**
	 * start从0开始.
	 * OFFSET从0开始
	 */
	public String toPageSql(String sql, int start, int size) {
		sql=HoneyUtil.deleteLastSemicolon(sql);
		
		if(!initFlag) {
			init();
			initFlag=true;
		}
		
		if(start<=0 && sql.indexOf(DISTINCT)>-1) return toPageSql(sql, size);
		
		String processSql=PAGING;
		
		SqlServerPagingStruct struct = HoneyContext.getAndRemoveSqlServerPagingStruct(sql);
		if (struct != null) {
			String pkName = struct.getOrderColumn();
			String orderType = struct.getOrderType().getName();
			boolean hasOrderBy=struct.isHasOrderBy();
			
			if (hasOrderBy) {//有排序,则删除默认的
				processSql = adjustSqlServerPaging11(processSql);
			}else {
				processSql = adjustSqlServerPagingPk11(processSql, pkName, orderType);
			}
		}
		
		
		if (HoneyUtil.isSqlKeyWordUpper()) {
			sql +=processSql.replace("#START", start+"").replace("#SIZE", size+"");
		}else {
			sql +=processSql.replace("#start", start+"").replace("#size", size+"");
		}
		
		return sql;
	}
	
	public String toPageSql(String sql, int size) {
//		if (sql.indexOf(DISTINCT) == -1) return toPageSql(sql, 0, size); //新语法太多限制.
		
		if(!initFlag) {
			init();
			initFlag=true;
		}
		return super.toPageSql(sql, size); //使用top n
	}
	
	// 处理不可能有排序的情况, 只需调整自定义主键即可(如果有)
	private String adjustSqlServerPagingPk11(String processSql, String pkName,String orderType) {
		
		if(StringUtils.isBlank(pkName)) return processSql;
		
		String ID = "id";
		if (HoneyUtil.isSqlKeyWordUpper()) {
			ID = ID.toUpperCase();
			orderType=orderType.toUpperCase();
		}
		
		if("desc".equalsIgnoreCase(orderType)) return processSql.replace(ID, pkName + " " + orderType);
		else return processSql.replace(ID, pkName);
	}
	
	// 已有排序,要去掉.
	private String adjustSqlServerPaging11(String processSql) {
		String str = " order by id ";
		if (HoneyUtil.isSqlKeyWordUpper()) {
			str = str.toUpperCase();
		}
		return processSql.replace(str, " ");
	}

}
