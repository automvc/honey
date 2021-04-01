package org.teasoft.honey.osql.core;

import java.sql.Connection;

import org.teasoft.bee.osql.annotation.SysValue;
import org.teasoft.honey.distribution.ds.Router;
import org.teasoft.honey.osql.constant.DbConfigConst;

/**
 * @author Kingstar
 * @since  1.0
 */
public final class HoneyConfig {

	private static HoneyConfig honeyConfig = null;
	static {
		honeyConfig = new HoneyConfig();
		honeyConfig.init(); // just run one time
	}

	private HoneyConfig() {}

	public static HoneyConfig getHoneyConfig() {

		return honeyConfig;
	}

	private void init() {
		SysValueProcessor.process(honeyConfig);
	}

	//----------------------------- bee.osql
	// 启动时动态获取
	@SysValue("${bee.osql.loggerType}")
	public String loggerType; //v1.8
	
	@SysValue("${bee.osql.logDonotPrintLevel}")
	public boolean logDonotPrintLevel = true; //v1.7.2

	@SysValue("${bee.osql.dateFormat}")
	public String dateFormat; //v1.7.2   use in DateUtil
	
	@SysValue("${bee.osql.sqlKeyWordCase}")
	public String sqlKeyWordCase;
	
	@SysValue("${bee.osql.notDeleteWholeRecords}")
	boolean notDeleteWholeRecords = true; //v1.7.2

	@SysValue("${bee.osql.notUpdateWholeRecords}")
	boolean notUpdateWholeRecords = true; //v1.7.2
	
	@SysValue("${bee.osql.insertBatchSize}")
	int insertBatchSize = 10000; //不设置,默认10000
	
	
	@SysValue("${bee.osql.showSQL}")   //属于 bee.osql
	public boolean showSQL = false;
	//----------------------------- showSQL start

	@SysValue("${bee.osql.showSQL.showType}")
	boolean showSQL_showType;//v1.8

	@SysValue("${bee.osql.showSQL.showExecutableSql}")
	boolean showSQL_showExecutableSql;//v1.8

	@SysValue("${bee.osql.showSQL.donotPrintCurrentDate}")
	public boolean showSQL_donotPrintCurrentDate; //v1.7.0
	//----------------------------- showSQL end
	
	
	@SysValue("${bee.osql.naming.toLowerCaseBefore}")
	public boolean naming_toLowerCaseBefore = true; //default : to LowerCase before

	@SysValue("${bee.osql.naming.translateType}")
	int naming_translateType = 1;
	
	@SysValue("${bee.osql.naming.entity2tableMappingList}")
	public String naming_entity2tableMappingList;
	

	@SysValue("${bee.osql.moreTable.columnListWithStar}")
	boolean moreTable_columnListWithStar;

	@SysValue("${bee.osql.moreTable.twoTablesWithJoinOnStyle}")
	boolean moreTable_twoTablesWithJoinOnStyle;
	

	//----------------------------- selectJson start
	@SysValue("${bee.osql.selectJson.ignoreNull}")
	boolean selectJson_ignoreNull = true;
	@SysValue("${bee.osql.selectJson.timestampWithMillisecond}")
	boolean selectJson_timestampWithMillisecond;

	@SysValue("${bee.osql.selectJson.dateWithMillisecond}")
	boolean selectJson_dateWithMillisecond;

	@SysValue("${bee.osql.selectJson.timeWithMillisecond}")
	boolean selectJson_timeWithMillisecond;
	//----------------------------- selectJson end

	@SysValue("${bee.osql.returnStringList.nullToEmptyString}")
	boolean returnStringList_nullToEmptyString;
	
	
	@SysValue("${bee.db.dbName}")
	String dbName;

	@SysValue("${" + DbConfigConst.DB_DRIVERNAME + "}")
	String driverName;

	@SysValue("${" + DbConfigConst.DB_URL + "}")
	String url;

	@SysValue("${" + DbConfigConst.DB_USERNAM + "}")
	String username;

	@SysValue("${" + DbConfigConst.DB_PWORD + "}")
	String password;

	//	@SysValue("${aaa}")


	//----------------------------- cache start
	@SysValue("${bee.osql.cache.timeout}")
	public int cache_timeout = 10000; //缓存保存时间(毫秒 ms)

	@SysValue("${bee.osql.cache.mapSize}")
	int cache_mapSize = 1000; //缓存集数据量大小

//	private String cacheType="FIFO";

	@SysValue("${bee.osql.cache.startDeleteRate}")
	public Double cache_startDeleteRate = 0.6; //when timeout use

	@SysValue("${bee.osql.cache.fullUsedRate}")
	Double cache_fullUsedRate = 0.8; //when add element in cache use

	@SysValue("${bee.osql.cache.fullClearRate}")
	Double cache_fullClearRate = 0.2; //when add element in cache use

	@SysValue("${bee.osql.cache.keyUseMD5}")
	boolean cache_keyUseMD5 = true;

	@SysValue("${bee.osql.cache.nocache}")
	boolean cache_nocache; //v1.7.2

	@SysValue("${bee.osql.cache.workResultSetSize}")
	int cache_workResultSetSize = 300;

	@SysValue("${bee.osql.cache.never}")
	String cache_never;

	@SysValue("${bee.osql.cache.forever}")
	String cache_forever;

	@SysValue("${bee.osql.cache.modifySyn}")
	String cache_modifySyn;
	//----------------------------- cache end

	//----------------------------- genid  start
	//v1.8
	@SysValue("${bee.distribution.genid.workerid}")
	public int genid_workerid = 0;

	@SysValue("${bee.distribution.genid.generatorType}")
	public int genid_generatorType = 1;

	@SysValue("${bee.distribution.genid.forAllTableLongId}")
	public boolean genid_forAllTableLongId;
	@SysValue("${bee.distribution.genid.includesEntityList}")
	public String genid_includesEntityList;
	@SysValue("${bee.distribution.genid.excludesEntityList}")
	public String genid_excludesEntityList;
	//----------------------------- genid  end
	
	//----------------------------- genid  pearFlowerId start	
	@SysValue("${bee.distribution.pearFlowerId.tolerateSecond}")
	public long pearFlowerId_tolerateSecond = 10;
	@SysValue("${bee.distribution.pearFlowerId.useHalfWorkId}")
	public boolean pearFlowerId_useHalfWorkId;
	@SysValue("${bee.distribution.pearFlowerId.switchWorkIdTimeThreshold}")
	public long pearFlowerId_switchWorkIdTimeThreshold = 120;
	@SysValue("${bee.distribution.pearFlowerId.randomNumBound}")
	public int pearFlowerId_randomNumBound = 2; //v1.8.15
	//----------------------------- genid  pearFlowerId end

	//----------------------------- multiDs  start
	@SysValue("${bee.dosql.multiDS.enable}")
	public boolean multiDS_enable;
	@SysValue("${bee.dosql.multiDS.type}")
	public int multiDS_type; //注意,系统会设初值0
	@SysValue("${bee.dosql.multiDS.defalutDS}")
	public String multiDS_defalutDS;
	@SysValue("${bee.dosql.multiDS.writeDB}")
	public String multiDS_writeDB; //multiDsType=1
	@SysValue("${bee.dosql.multiDS.readDB}")
	public String multiDS_readDB; //multiDsType=1
	@SysValue("${bee.dosql.multiDS.rDbRouteWay}")
	public int multiDS_rDbRouteWay; //注意,系统会设初值0  //multiDsType=1

	@SysValue("${bee.dosql.multiDS.matchEntityClassPath}")
	public String multiDS_matchEntityClassPath = ""; //multiDsType=2

	@SysValue("${bee.dosql.multiDS.matchTable}")
	public String multiDS_matchTable = ""; //multiDsType=2

	//	支持同时使用多种类型数据库的数据源.support different type muli-Ds at same time.
	@SysValue("${bee.dosql.multiDS.differentDbType}")
	public boolean multiDS_differentDbType;
	//----------------------------- multiDs  end

	public String getDbName() {
		checkAndInitDbName();
		if (HoneyContext.isNeedRealTimeDb()) { //支持同时使用多种数据库的,需要动态获取,才准确
			String dsName = Router.getDsName();
			if (dsName != null && HoneyContext.getDsName2DbName() != null) 
				return HoneyContext.getDsName2DbName().get(dsName);
		}
		return dbName;
	}

	public void setDbName(String dbName) {
		this.dbName = dbName;
		Logger.info("[Bee] ========= reset the dbName in HoneyConfig is :" + dbName);
//		BeeFactory.getHoneyFactory().setDbFeature(BeeFactory.getHoneyFactory()._getDbDialectFeature());  //循环调用
		BeeFactory.getHoneyFactory().setDbFeature(null);
	}
	
	private static boolean alreadyPrintDbName = false;
	private static boolean changeDataSource = false;

	private static void checkAndInitDbName() {
		if (HoneyConfig.getHoneyConfig().dbName == null || changeDataSource) {

			Connection conn = null;
			try {
				conn = SessionFactory.getConnection();
				if (conn != null) {
					String newDbName=conn.getMetaData().getDatabaseProductName();
					if (changeDataSource) {
						HoneyConfig.getHoneyConfig().setDbName(newDbName);
					} else {
						HoneyConfig.getHoneyConfig().dbName = newDbName;
					}
					
					String logMsg="[Bee] ========= get the dbName from the Connection is :" + HoneyConfig.getHoneyConfig().dbName;
					Logger.info(logMsg);
					alreadyPrintDbName = true;
				}
			} catch (Exception e) {
				Logger.error(e.getMessage());
			} finally {
				try {
					if (conn != null) conn.close();
				} catch (Exception e2) {
					Logger.error(e2.getMessage());
				}
				
				if(alreadyPrintDbName && changeDataSource) { //auto refresh the type map config
					changeDataSource=false;
					HoneyUtil.refreshTypeMapConfig();
				}
				changeDataSource=false;
			}
		} else {
			if (!alreadyPrintDbName) {
				Logger.info("[Bee] ========= get the dbName from HoneyConfig is :" + HoneyConfig.getHoneyConfig().dbName);
				alreadyPrintDbName = true;
			}
		}
	}

	public void setDriverName(String driverName) {
		this.driverName = driverName;
	}

	public void setUrl(String url) {
		changeDataSource=true;
		this.url = url;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getDriverName() {
		return driverName;
	}

	public String getUrl() {
		return url;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

}
