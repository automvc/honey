package org.teasoft.honey.osql.core;

import java.sql.Connection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.teasoft.bee.ds.DataSourceBuilderFactory;
import org.teasoft.bee.osql.BeeAbstractFactory;
import org.teasoft.bee.osql.DatabaseConst;
import org.teasoft.bee.osql.Serializer;
import org.teasoft.honey.distribution.ds.Router;

/**
 * 
 * Bee工厂类.Bee Factory.
 * @author Kingstar
 * @since  1.0
 */
public class BeeFactory extends BeeAbstractFactory {

	private static BeeFactory instance=new BeeFactory();
	private static HoneyFactory honeyFactory = null;

	static{
		HoneyContext.initLoad();
	}
	
//	HoneyFactory 用于非spring,方法获取对象. 用spring时,由spring管理,不再需要HoneyFactory.
//	public void setHoneyFactory(HoneyFactory honeyFactory) {
//		BeeFactory.honeyFactory = honeyFactory;
//	}

	public static HoneyFactory getHoneyFactory() {
		if (honeyFactory == null) {
			honeyFactory = new HoneyFactory();
		}
		return honeyFactory;
	}

	private BeeFactory() {
	}
	
	public static BeeFactory getInstance(){
		return instance;
	}
	
	@Override
	public DataSource getDataSource() {
		if (HoneyContext.isDsMapRefresh()) refreshDataSourceMap();

		if (super.getDataSourceMap() == null) {
			return super.getDataSource();
		} else {
			return _getDsFromDsMap();
		}
	}
	
	private DataSource _getDsFromDsMap() {
		String dsName = Router.getDsName();
		
		Logger.info("[Bee] ========= the current DataSource name is :"+dsName+shardingIndex()); //V1.17
//		Logger.logSQL("使用logSQL, 会引发异常", ""); //因为在Config首先获取dbName时,要使用这个方法,而logSQL这个方法又要使用Config里的信息.
		DataSource ds=getDataSourceMap().get(dsName);
		if(ds==null) Logger.warn("Can not find the DataSource from DataSource Map with dsName: "+dsName);
		return ds;
	}
	
	private String shardingIndex() {
		Integer subThreadIndex = HoneyContext.getSqlIndexLocal();
		String index = "";
		if (subThreadIndex != null) {
			index = " (sharding " + subThreadIndex + ")";
		}
		return index;
	}
	
	@Override
	protected void parseDbNameByDsMap() {
		
//		if(! HoneyContext.isNeedRealTimeDb()) return ;  //在设置DataSourceMap前,就要设置同时使用多种类型数据库时的配置信息
		
		Map<String, DataSource> dsMap = getDataSourceMap();
        if(dsMap==null) return ;
        int i=0;
        String dbName="";
		Map<String, String> dsName2DbName=new LinkedHashMap<>();
		for (Map.Entry<String, DataSource> entry : dsMap.entrySet()) {
			dsName2DbName.put(entry.getKey(), getDbName(entry.getValue()));
			if(i==0) {
				dbName=dsName2DbName.get(entry.getKey());
				i++;
			}
			
		}
//		HoneyContext.setDsName2DbName(dsName2DbName);
		Logger.info("[Bee] Parse DataSourceMap: dataSource name to database name , result: "+dsName2DbName);
//		HoneyConfig.getHoneyConfig().dbName=dbName;
		HoneyConfig.getHoneyConfig().setDbName(dbName);
		HoneyContext.setDsName2DbName(dsName2DbName);
		HoneyUtil.refreshSetParaAndResultTypeHandlerRegistry();
	}
	
	private void refreshDataSourceMap() {
		
		List<Map<String, String>> dbsList=HoneyConfig.getHoneyConfig().getDbs();
		HoneyContext.setDsMapRefresh(false);
		
		if(dbsList==null || dbsList.size()==0) return ;
		
		System.err.println("----------------size:"+dbsList.size());
		System.err.println(dbsList);
		
		boolean extendFirst=HoneyConfig.getHoneyConfig().extendFirst;
		
		try {
			Class.forName("org.teasoft.beex.ds.DataSourceToolRegHandler");
		} catch (Exception e) {
			Logger.debug(e.getMessage(), e);
		}
		
		int size=dbsList.size();
		String dsNames[]=new String[size];
		Map<String, DataSource> dataSourceMap=new HashMap<>();
		Map<String, String> map;
		Map<String, String> base0=null;
		Map<String, String> copyMap=null;
		
		for (int i = 0; i < size; i++) {
			map=dbsList.get(i);
			dsNames[i]=map.get("dsName");
			map.remove("dsName");
			
			if(extendFirst) {
				if(i==0) base0=map;
				else {
					copyMap=(Map<String, String>)copyMap(base0);
					if(copyMap!=null) {
						copyMap.putAll(map);
						map=copyMap;
					}
				}
			}
			
			System.err.println(map);
			//判断ds tool类型 TODO
//			String dsToolType="druid";
			String dsToolType="Hikari";
			
			dataSourceMap.put(dsNames[i], DataSourceBuilderFactory.getDataSourceBuilder(dsToolType).build(map));
		
		}
		
		setDataSourceMap(dataSourceMap);
		System.err.println("----------------------完成更新ds Map ------------------------");
		
		
	}
	
	private Object copyMap(Map obj) {
		try {
			Serializer jdks = new JdkSerializer();
			return jdks.unserialize(jdks.serialize(obj));
		} catch (Exception e) {
			Logger.debug(e.getMessage(), e);
		}
		return null;
	}
	
	private String getDbName(DataSource ds) {
		Connection conn = null;
		String dbName = null;
		try {
			conn = ds.getConnection();
			if (conn != null) {
				dbName = conn.getMetaData().getDatabaseProductName();
				if (dbName.contains("Microsoft Access")) {
					Logger.debug("Transform the dbName:'"+dbName+"' to '"+DatabaseConst.MsAccess+"'");
					dbName = DatabaseConst.MsAccess;
				}
			}
		} catch (Exception e) {
			Logger.error(e.getMessage(),e);
		} finally {
			try {
				if (conn != null) conn.close();
			} catch (Exception e2) {
				Logger.error(e2.getMessage(),e2);
			}
		}
		
		return dbName;
	}

}
