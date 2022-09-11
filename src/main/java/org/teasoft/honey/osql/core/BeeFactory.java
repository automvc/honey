package org.teasoft.honey.osql.core;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.teasoft.bee.osql.BeeAbstractFactory;
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
		
		if(super.getDataSourceMap()==null){
		   return super.getDataSource();
		}else{
			return _getDsFromDsMap();
		}
	}
	
	private DataSource _getDsFromDsMap() {
		String dsName = Router.getDsName();
		Logger.info("[Bee] ========= the current DataSource name is :"+dsName); //V1.17
//		Logger.logSQL("========= the current DataSource name is :"+dsName,""); //V1.17
//		Logger.logSQL("使用logSQL, 会引发异常", "");
		return getDataSourceMap().get(dsName);
	}
	
	@Override
	protected void parseDbNameByDsMap() {
		
//		if(! HoneyContext.isNeedRealTimeDb()) return ;  //在设置DataSourceMap前,就要设置同时使用多种类型数据库时的配置信息
		
		Map<String, DataSource> dsMap = getDataSourceMap();
        if(dsMap==null) return ;
		Map<String, String> dsName2DbName=new LinkedHashMap<>();
		int i=0;
		String dbName="";
		for (Map.Entry<String, DataSource> entry : dsMap.entrySet()) {
			dsName2DbName.put(entry.getKey(), getDbName(entry.getValue()));
			if(i==0) {
				dbName=dsName2DbName.get(entry.getKey());
				i++;
			}
		}
		Logger.info("[Bee] Parse DataSourceMap: dataSource name to database name , result: "+dsName2DbName);
		HoneyConfig.getHoneyConfig().setDbName(dbName);
		HoneyContext.setDsName2DbName(dsName2DbName);
		HoneyUtil.refreshSetParaAndResultTypeHandlerRegistry();
	}
	
	private String getDbName(DataSource ds) {
		Connection conn = null;
		String dbName = null;
		try {
			conn = ds.getConnection();
			if (conn != null) {
				dbName = conn.getMetaData().getDatabaseProductName();
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
