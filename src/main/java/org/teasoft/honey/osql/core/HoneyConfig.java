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

	private HoneyConfig() {
	}

	public static HoneyConfig getHoneyConfig() {
		return honeyConfig;
	}

	private void init() {
		SysValueProcessor.process(honeyConfig);
	}
	
	/**
	 * 使用指定路径的bee.properties进行配置.
	 * 若使用第三方框架管理配置,不建议在此处重置配置.
	 * @param filePath bee.properties所在的路径
	 * @since 1.9.8
	 */
	public void resetBeeProperties(String filePath) {
		try {
			BeeProp.resetBeeProperties(filePath);
//			HoneyConfig.honeyConfig = new HoneyConfig();
			_setHoneyConfig();
			honeyConfig.init();
			Logger.warn("[Bee] ========= reset the bee.properties with filePath:" + filePath);
		} catch (Exception e) {
			Logger.warn(e.getMessage());
		}
	}
	
	private static void _setHoneyConfig() {
		HoneyConfig.honeyConfig = new HoneyConfig();
	}

	//----------------------------- bee.osql
	// 启动时动态获取
	@SysValue("${bee.osql.loggerType}")
	public String loggerType; //v1.8
	
	@SysValue("${bee.osql.sqlLoggerLevel}")
	public String sqlLoggerLevel; //v1.9.8
	
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
	//----------------------------- showSql start

	@SysValue("${bee.osql.showSql.showType}")
	boolean showSql_showType;//v1.8

	@SysValue("${bee.osql.showSql.showExecutableSql}")
	boolean showSql_showExecutableSql;//v1.8

	@SysValue("${bee.osql.showSql.donotPrintCurrentDate}")
	public boolean showSql_donotPrintCurrentDate; //v1.7.0
	//----------------------------- showSql end
	
	
	@SysValue("${bee.osql.naming.toLowerCaseBefore}")
	public boolean naming_toLowerCaseBefore = true; //default : to LowerCase before

	@SysValue("${bee.osql.naming.translateType}")
	public int naming_translateType = 1;
	
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

	@SysValue("${bee.osql.selectJson.longToString}")
	boolean selectJson_longToString = true;
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
	
    @SysValue("${bee.db.jndiType}")
	boolean jndiType;
    @SysValue("${bee.db.jndiName}")
	String jndiName;

	//----------------------------- cache start
	@SysValue("${bee.osql.cache.timeout}")
	public int cache_timeout = 30000; //缓存保存时间(毫秒 ms)

	@SysValue("${bee.osql.cache.mapSize}")
	int cache_mapSize = 20000; //缓存集数据量大小

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
	
	//V1.11
	@SysValue("${bee.osql.cache.useLevelTwo}")
	boolean cache_useLevelTwo; 
	@SysValue("${bee.osql.cache.levelOneTolevelTwo}")
	boolean cache_levelOneTolevelTwo; 
	@SysValue("${bee.osql.cache.levelTwoTimeout}")
	public int cache_levelTwoTimeout=180; //二级缓存保存时间(秒 second)
	
	@SysValue("${bee.osql.cache.levelTwoEntityList}")
	public String cache_levelTwoEntityList;
	
	//----------------------------- cache end

	//-----------------------------Redis cache start V1.11
	@SysValue("${bee.osql.cacheRedis.host}")
	public String cacheRedis_host;
	              
	@SysValue("${bee.osql.cacheRedis.port}")
	public Integer cacheRedis_port;
	
	@SysValue("${bee.osql.cacheRedis.password}")
	public String cacheRedis_password;
	
	@SysValue("${bee.osql.cacheRedis.connectionTimeout}")
	public Integer cacheRedis_connectionTimeout=10; //second
	
	@SysValue("${bee.osql.cacheRedis.soTimeout}")
	public Integer cacheRedis_soTimeout=10;  //second
	
	@SysValue("${bee.osql.cacheRedis.database}")
	public Integer cacheRedis_database;
	
	@SysValue("${bee.osql.cacheRedis.clientName}")
	public String cacheRedis_clientName;
	
	@SysValue("${bee.osql.cacheRedis.ssl}")
	public boolean cacheRedis_ssl;
	
	//-----------------------------Redis cache end
	

	//----------------------------- genid  start
	//v1.8
	@SysValue("${bee.distribution.genid.workerid}")
	public int genid_workerid = 0;

	@SysValue("${bee.distribution.genid.generatorType}")
	public int genid_generatorType = 1;

	@SysValue("${bee.distribution.genid.forAllTableLongId}")
	public boolean genid_forAllTableLongId;
	@SysValue("${bee.distribution.genid.replaceOldId}")
	public boolean genid_replaceOldId=true;
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
//		changeDataSource=true;
		HoneyConfig.setChangeDataSource(true);
		this.url = url;
	}
	
	private static void setChangeDataSource(boolean flag) {
		HoneyConfig.changeDataSource=true;
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
	
//	public void setDataSourceMap(Map<String, DataSource> dataSourceMap) {
//		BeeFactory.getInstance().setDataSourceMap(dataSourceMap);
//	}
	
//	context有:
//	public static void setDsName2DbName(Map<String, String> dsName2DbName) {
//		HoneyContext.dsName2DbName = dsName2DbName;
//	}

}
