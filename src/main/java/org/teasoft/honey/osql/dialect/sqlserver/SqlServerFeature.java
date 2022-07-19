package org.teasoft.honey.osql.dialect.sqlserver;

import org.teasoft.bee.osql.dialect.DbFeature;
import org.teasoft.honey.osql.core.HoneyContext;
import org.teasoft.honey.osql.core.HoneyUtil;
import org.teasoft.honey.util.StringUtils;

/**
* support paging for Sql Server  which version < 2012
 * 强烈推荐表加一个主键列名称为"id".
 * 默认分页排序使用:order by id.
 * @author Kingstar
 * @since  1.0
 */
public class SqlServerFeature extends AbstractSqlServerFeature implements DbFeature {
	
	private static String part1="select * from (select top ? row_number() over (order by id) as rownum,";
	private static String part2=") as table_ where table_.rownum >=?";
	
	private static String part1_1="select * from (";
	private static String part1_2=" top ? row_number() over (order by id) as rownum,";
	
	{
		if (HoneyUtil.isSqlKeyWordUpper()) {
			setStr();
		}
	}
	
	private static void setStr() {
		part1 = part1.toUpperCase();
		part2 = part2.toUpperCase();
		part1_1=part1_1.toUpperCase();
		part1_2=part1_2.toUpperCase();
	}

	// 2012之前的语法,start是从1开始
	public String toPageSql(String sql, int start, int size) {
		// sql server 2012之前的语法 start是从1开始数. start 为0或1, 相当于只取size
		if (start <= 1) return toPageSql(sql, size);

//		if (start == 0) start = 1;
		sql = HoneyUtil.deleteLastSemicolon(sql);
		sql = sql.trim();
		String sql2 = "";
		int index2 = sql.toLowerCase().indexOf("select distinct");
		String processSql = "";
		if (index2 < 0)
			processSql = part1;
		else
			processSql = part1_2;

		SqlServerPagingStruct struct = HoneyContext.getAndRemoveSqlServerPagingStruct(sql);
		if (struct != null) {
			boolean justChangePk = struct.isJustChangeOrderColumn();
			String pkName = struct.getOrderColumn();
			String orderType = struct.getOrderType().getName();
			if (justChangePk) {
				processSql = adjustSqlServerPagingPk10(processSql, pkName, orderType);
			} else {
				processSql = adjustSqlServerPaging10(processSql, pkName, orderType);
			}
		}

		if (index2 < 0) {
			sql2 = processSql.replace("?", start + size - 1 + "") + sql.substring(6)
					+ part2.replace("?", start + "");
		} else {
			sql2 = part1_1 + sql.substring(0, index2 + 15)
					+ processSql.replace("?", start + size - 1 + "")
					+ sql.substring(index2 + 15, sql.length()) + part2.replace("?", start + "");
		}

		return sql2;
	}

	// V1.17
	// 处理不可能有排序的情况, 只需调整自定义主键即可(如果有),也要加asc/desc
	private String adjustSqlServerPagingPk10(String processSql, String pkName, String orderType) {
		String replaceStr = " over (order by id) ";
		String ID = "id";
		if (HoneyUtil.isSqlKeyWordUpper()) {
			replaceStr = replaceStr.toUpperCase();
			ID = ID.toUpperCase();
			if ("desc".equals(orderType)) orderType = "DESC";
		}

		if (StringUtils.isBlank(pkName)) return processSql; // 没有改动
		String newStr = pkName;
		if ("desc".equalsIgnoreCase(orderType)) newStr = pkName + " " + orderType;
		return processSql.replace(replaceStr, replaceStr.replace(ID, newStr));
	}

	// V1.17
	// SqlServer 2012版之前的复杂分页语法需要判断
	private String adjustSqlServerPaging10(String processSql, String pkName, String orderType) {
		String replaceStr = " over (order by id) ";
		String ID = "id";
		String newStr = replaceStr;
		boolean orign=true;
		if (HoneyUtil.isSqlKeyWordUpper()) {
			replaceStr = replaceStr.toUpperCase();
			ID = ID.toUpperCase();
			if ("desc".equals(orderType)) orderType = "DESC";
		}
		if ("desc".equalsIgnoreCase(orderType)) { //可能只添加DESC(不改主键)
			newStr = newStr.replace(ID, ID + " " + orderType); // 只添加desc
			orign=false;
		}
		if(StringUtils.isNotBlank(pkName)) {
		   newStr = newStr.replace(ID, pkName);
		   orign=false;
		}
		
		if(orign) return processSql; //没有改动

		return processSql.replace(replaceStr, newStr);
	}

}
