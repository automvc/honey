/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//import org.teasoft.bee.osql.Serializer;
import org.teasoft.bee.osql.chain.UnionSelect;
import org.teasoft.bee.osql.dialect.DbFeature;
import org.teasoft.bee.sharding.ShardingPageStruct;
import org.teasoft.honey.sharding.ShardingUtil;
import org.teasoft.honey.sharding.engine.decorate.OrderBySqlDecorator;
import org.teasoft.honey.sharding.engine.decorate.PagingSqlDecorator;

/**
 * @author AiTeaSoft
 * @since  2.0
 */
public class OrderByPagingRewriteSql {
	
   public static List<String[]> createSqlsAndInit(String sql) {
		
		List<String[]> list=new ArrayList<>();
		List<String> tabNameList=HoneyContext.getListLocal(StringConst.TabNameListLocal);
		List<String> tabSuffixList=HoneyContext.getListLocal(StringConst.TabSuffixListLocal);
		Map<String, String> tab2DsMap=HoneyContext.getCustomMapLocal(StringConst.ShardingTab2DsMap);
		
		List<PreparedValue> listValue = HoneyContext.justGetPreparedValue(sql);
		
		ShardingPageStruct shardingPage=HoneyContext.getCurrentShardingPage();
		
		if(shardingPage==null) {
			SimpleRewriteSql._createSql(list, tabSuffixList, sql, listValue, tabNameList,tab2DsMap);
		}else {  //shardingPage!=null  要处理分页
			
			List<String> dsNameList= HoneyContext.getListLocal(StringConst.DsNameListLocal);
			
			//ReWriteSQL  
			//a)rewrite paging sql
			sql=rewritePaingSql(sql);

			//只涉及一个Ds,且支持用union all语法
			if(dsNameList.size()==1 && ! HoneyUtil.isNotSupportUnionQuery() && !ShardingUtil.isMoreTableQuery()) {//一库多表,即多个表个在同一个库中. //还要支持union all
				shardingPage.setPagingType(1); //"MoreTablesInSameDsUseUnionAll"
				
                //b)sql语句替换表下标
				String sqls[]=createShardingSql(tabSuffixList, sql);
				
				//c)union all  
				//d)生成新的复合查询语句；
				String newSql=createUnionAllSql(sqls);
				
				//e)加排序子句；
				newSql=OrderBySqlDecorator.addOrderBy(newSql);

				//f)加分页,取指定页的一页记录
				newSql=PagingSqlDecorator.addPaging(newSql);
				
				//g)调整参数缓存
				//g) adjust PreparedValue
//				List newListValue =copyObject(listValue); 
//				for (int j = 1; j < sqls.length; j++) {
//					newListValue.addAll(listValue); //
//				}
//				HoneyContext.setPreparedValue(newSql, newListValue);
				
				List<PreparedValue> newListValue =new ArrayList<>();
				newListValue.addAll(listValue);
				for (int j = 1; j < sqls.length; j++) {
//					listValue.addAll(listValue); 
					newListValue.addAll(listValue);
				}
				HoneyContext.setPreparedValue(newSql, newListValue);
				
				list.add(new String[] { newSql }); //合成一条sql了， sql也不用加随机前缀 V2.2
				list.add(new String[] { dsNameList.get(0) });
			}else {
				Logger.warn("Involved many dataSource or not supported union all!! ");
//				是否还要按库分,然后,不同库的使用union?  取决于union all 和IO 哪个用的时间更少!!
				//其它的,应该可以重用.  只不过涉及分页的,sql重新使用新的扩大的分页参数生成
				
				shardingPage.setPagingType(2); //ManyDs
				SimpleRewriteSql._createSql(list, tabSuffixList, sql, listValue, tabNameList,tab2DsMap);
				
				//合并结果后,要重新排序(在List排),然后再取需要的页的数据.  
				//两种情况,都要放缓存. 查询时,也要能从缓存中,取出. todo
			}
		}
		
		
		return list;
	}
	
//   @SuppressWarnings("rawtypes")
//	private static List copyObject(List<PreparedValue> obj) {
//		try {
//			Serializer jdks = new JdkSerializer();
//			return (List)jdks.unserialize(jdks.serialize(obj));
//		} catch (Exception e) {
//			Logger.debug(e.getMessage(), e);
//		}
//		return obj;
//	}
	
	@SuppressWarnings("rawtypes")
	public static List<String[]> createSqlsForFullSelect(String sql, Class entityClass) {
		String tableName = _toTableName(entityClass);
		String baseTableName = tableName.replace(StringConst.ShardingTableIndexStr, "");

		List<PreparedValue> listValue = HoneyContext.justGetPreparedValue(sql);

		// rewrite paging sql
		// a)依据原本没分页的语句，用扩大的分页码重新生成分页语句；
		ShardingPageStruct shardingPage = HoneyContext.getCurrentShardingPage();
		if (shardingPage != null) {
			sql = rewritePaingSql(sql);
			shardingPage.setPagingType(3); // FullSelectPage
		}

		List<String[]> list = new ArrayList<>();
		SimpleRewriteSql._createSqlsForFull(list, sql, listValue, baseTableName);

		return list;
	}
	
	@SuppressWarnings("rawtypes")
	private static String _toTableName(Class entityClass){
		return NameTranslateHandle.toTableName(entityClass.getName());
	}

	private static DbFeature getDbFeature() {
		return BeeFactory.getHoneyFactory().getDbFeature();
	}
	
    private static int firstRecordIndex() {
    	return ShardingUtil.firstRecordIndex();
    }
    
	private static String rewritePaingSql(String sql) {
		ShardingPageStruct shardingPage = HoneyContext.getCurrentShardingPage();
		if (shardingPage == null) return sql;
		int start = shardingPage.getStart();
		int size = shardingPage.getSize();
		if (start != -1 && start != firstRecordIndex()) {// 不是查首页,要改写分页语句
			String beforeSql = shardingPage.getBeforeSql();
			sql = getDbFeature().toPageSql(beforeSql, firstRecordIndex(), start + size - 1);
		}

		return sql;
	}
   
   private static String[] createShardingSql(List<String> tabSuffixList,String sql) {
		String sqls[]= new String[tabSuffixList.size()];
		for (int i = 0; i < tabSuffixList.size(); i++) {
			sqls[i]=sql.replace(StringConst.ShardingTableIndexStr,tabSuffixList.get(i)); //eg: 占位符替换成下标等
		}
		return sqls;
   }
   
   private static String createUnionAllSql(String sqls[]) {
	   UnionSelect unionSelect=BeeFactoryHelper.getUnionSelect();
		String newSql=unionSelect.unionAll(sqls).toSQL();
		newSql=K.select+" * "+K.from+K.space+"("+newSql +") _union_select";
		
		return newSql;
   }

}
