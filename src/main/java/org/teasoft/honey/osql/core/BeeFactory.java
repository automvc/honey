package org.teasoft.honey.osql.core;

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

	private static BeeFactory instance = new BeeFactory();
	private static HoneyFactory honeyFactory = null;

	static {
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

	private BeeFactory() {}

	public static BeeFactory getInstance() {
		return instance;
	}

	@Override
	public DataSource getDataSource() {
		if (HoneyContext.isDsMapConfigRefresh()) {
			Map<String, DataSource> map=ProcessDataSourceMap.refreshDataSourceMap();
			if(map!=null && map.size()>0) {
//				HoneyConfig.getHoneyConfig().dbName=null; //不需要,会重新解析的
				setDataSourceMap(map);
			}
			HoneyContext.setDsMapConfigRefresh(false);
		}

		if (super.getDataSourceMap() == null) {
			return super.getDataSource();
		} else {
			return _getDsFromDsMap();
		}
	}

	private DataSource _getDsFromDsMap() {
		String dsName = Router.getDsName();

		Logger.info(
				"[Bee] ========= the current DataSource name is :" + dsName + shardingIndex()); // V1.17
//		Logger.logSQL("使用logSQL, 会引发异常", ""); //因为在Config首先获取dbName时,要使用这个方法,而logSQL这个方法又要使用Config里的信息.
		DataSource ds = getDataSourceMap().get(dsName);
		if (ds == null) Logger
				.warn("Can not find the DataSource from DataSource Map with dsName: " + dsName);
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
		if (dsMap == null) return;
		ProcessDataSourceMap.parseDbNameByDsMap(dsMap);
	}

}
