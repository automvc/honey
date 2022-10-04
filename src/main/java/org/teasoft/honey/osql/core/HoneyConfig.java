package org.teasoft.honey.osql.core;

import java.io.InputStream;
import java.sql.Connection;

import org.teasoft.bee.osql.BeeVersion;
import org.teasoft.bee.osql.DatabaseConst;
import org.teasoft.bee.osql.Properties;
import org.teasoft.bee.osql.annotation.SysValue;
import org.teasoft.bee.osql.dialect.DbFeatureRegistry;
import org.teasoft.bee.osql.exception.ConfigWrongException;
import org.teasoft.honey.distribution.ds.Router;
import org.teasoft.honey.logging.LoggerFactory;
import org.teasoft.honey.osql.constant.DbConfigConst;
import org.teasoft.honey.osql.dialect.LimitOffsetPaging;
import org.teasoft.honey.osql.dialect.sqlserver.SqlServerFeature2012;
import org.teasoft.honey.util.HoneyVersion;
import org.teasoft.honey.util.StringUtils;

/**
 * 配置类.Config for Bee.
 * @author Kingstar
 * @since  1.0
 */
public final class HoneyConfig {

	private static HoneyConfig honeyConfig = null;
	static {
		honeyConfig = new HoneyConfig();
		
		honeyConfig.init(); // just run one time
		
		printVersion();
	}

	private HoneyConfig() {
	}
	
//	{   //放在这,会报异常.
//		System.err.println("--------in HoneyConfig block");
//		honeyConfig.init(); // just run one time
//	}
	
	private static void printVersion() {
		Logger.info("[Bee] -------- Bee    " + BeeVersion.version+" -------- ");
		Logger.info("[Bee] -------- Honey  " + HoneyVersion.version+" -------- ");
		try {
			Class.forName("org.teasoft.beex.util.BeeExtVersion");
		} catch (Exception e) {
			Logger.debug("[Bee] ========= Bee    buildId  " + BeeVersion.buildId);
			Logger.debug("[Bee] ========= Honey  buildId  " + HoneyVersion.buildId);
		}
	}

	public static HoneyConfig getHoneyConfig() {
		return honeyConfig;
	}

	private void init() {
		SysValueProcessor.process(honeyConfig);
		
//		#1.base main and Override with active, 2.rebase to active(other file)
//		#1 : main file + other file; 2 : just active file(other file);    if do not set , will use mail file.
		Properties beeActiveProp=null;
		if (type == 1 || type == 2) {
			if (StringUtils.isBlank(active)) {
				String msg="The value of bee.profiles.active is empty!";
				Logger.error(msg,new ConfigWrongException(msg));
			} else {
				String fileName = "/bee-{active}.properties".replace("{active}", active);
				beeActiveProp = new BeeActiveProp(fileName);
				if(type==1) {//use the key in active override the main file.
					SysValueProcessor.process(honeyConfig,beeActiveProp);
				}else if(type==2) {
					_setHoneyConfig();//rebase
					SysValueProcessor.process(honeyConfig,beeActiveProp);
				}
			}
		}
		
		if(isAndroid || isHarmony) {//V1.17
			dbName=DatabaseConst.SQLite;
			DbFeatureRegistry.register(DatabaseConst.SQLite, new LimitOffsetPaging());
		}
		
		HoneyContext.initLoad();
	}
	
	/**
	 * 使用指定路径的bee.properties进行配置.set the folder path of bee.properties
	 * 若使用第三方框架管理配置,不建议在此处重置配置.
	 * @param folderPath bee.properties所在的路径. the folder path of bee.properties
	 * @since 1.9.8
	 */
	public void resetBeeProperties(String folderPath) {
		try {
			BeeProp.resetBeeProperties(folderPath);
			_setHoneyConfig();
			honeyConfig.init();
			LoggerFactory.setConfigRefresh(true);
			Logger.warn("[Bee] ========= reset the bee.properties with folderPath:" + folderPath);
		} catch (Exception e) {
			Logger.warn(e.getMessage());
		}
	}
	
	/**
	 * @since 1.17
	 */
	public void resetBeeProperties(InputStream inputStream) {
		try {
			BeeProp.resetBeeProperties(inputStream);
			_setHoneyConfig();
			honeyConfig.init();
			LoggerFactory.setConfigRefresh(true);
			Logger.warn("[Bee] ========= reset the bee.properties by inputStream");
		} catch (Exception e) {
			Logger.warn(e.getMessage());
		}
	}
	
	private static void _setHoneyConfig() {
		HoneyConfig.honeyConfig = new HoneyConfig();
	}
	
	//----------------------------- bee.profiles
	@SysValue("${bee.profiles.type}")
	public int type;
	
	@SysValue("${bee.profiles.active}")
	public String active;

	//----------------------------- bee.osql
	// 启动时动态获取
	@SysValue("${bee.osql.loggerType}")
	private String loggerType; //v1.8
	
	@SysValue("${bee.osql.sqlLoggerLevel}")
	public String sqlLoggerLevel; //v1.9.8
	
	@SysValue("${bee.osql.systemLoggerLevel}")
	public String systemLoggerLevel="info"; //v1.11
	
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
	
	@SysValue("${bee.osql.notCatchModifyDuplicateException}")
	public boolean notCatchModifyDuplicateException;
	
	@SysValue("${bee.osql.notShowModifyDuplicateException}")
	public boolean notShowModifyDuplicateException;
	
	@SysValue("${bee.osql.notSupportUnionQuery}")
	public boolean notSupportUnionQuery; //2.0
	
	@SysValue("${bee.osql.insertBatchSize}")
	int insertBatchSize = 10000; //不设置,默认10000
	
	@SysValue("${bee.osql.lang}")
	public String lang="CN";
	
	@SysValue("${bee.osql.openDefineColumn}")
	public boolean openDefineColumn;
	
	@SysValue("${bee.osql.openFieldTypeHandler}")
	public boolean openFieldTypeHandler;
	
	
	@SysValue("${bee.osql.showSQL}")   //属于 bee.osql
	public boolean showSQL = false;
	
	@SysValue("${bee.osql.showShardingSQL}")   //属于 bee.osql
	public boolean showShardingSQL = false;
	//----------------------------- showSql start

	@SysValue("${bee.osql.showSql.showType}")
	boolean showSql_showType;//v1.8

	@SysValue("${bee.osql.showSql.showExecutableSql}")
	public boolean showSql_showExecutableSql;//v1.8

	@SysValue("${bee.osql.showSql.donotPrintCurrentDate}")
	public boolean showSql_donotPrintCurrentDate; //v1.7.0
	//----------------------------- showSql end
	
	
	@SysValue("${bee.osql.naming.toLowerCaseBefore}")
	public boolean naming_toLowerCaseBefore = true; //default : to LowerCase before

	@SysValue("${bee.osql.naming.translateType}")
	public int naming_translateType = 1;
	
	@SysValue("${bee.osql.naming.useMoreTranslateType}") //V1.17
	public boolean naming_useMoreTranslateType;
	
	@SysValue("${bee.osql.naming.entity2tableMappingList}")
	public String naming_entity2tableMappingList;
	

	@SysValue("${bee.osql.moreTable.columnListWithStar}")
	boolean moreTable_columnListWithStar;

	@SysValue("${bee.osql.moreTable.twoTablesWithJoinOnStyle}")
	boolean moreTable_twoTablesWithJoinOnStyle;
	

	//----------------------------- selectJson start
	@SysValue("${bee.osql.selectJson.ignoreNull}")
	public boolean selectJson_ignoreNull = true;
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
	public boolean returnStringList_nullToEmptyString;
	
	private int databaseMajorVersion; //use in this class
	
	@SysValue("${bee.db.isAndroid}")
	public boolean isAndroid;
	@SysValue("${bee.db.androidDbName}")
	public String androidDbName;
	@SysValue("${bee.db.androidDbVersion}")
	public int androidDbVersion = 1; 
	
	@SysValue("${bee.db.isHarmony}")
	public boolean isHarmony;
	@SysValue("${bee.db.harmonyDbName}")
	public String harmonyDbName;
	@SysValue("${bee.db.harmonyDbVersion}")
	public int harmonyDbVersion=1;
	@SysValue("${bee.db.harmonyDbReadonly}")
	public boolean harmonyDbReadonly;
	
	
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
	
    @SysValue("${bee.db.schemaName}")
	String schemaName;
	
    @SysValue("${bee.db.jndiType}")
	boolean jndiType;
    @SysValue("${bee.db.jndiName}")
	String jndiName;
    
    @SysValue("${bee.db.pagingWithLimitOffset}")
	boolean pagingWithLimitOffset;

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
	@SysValue("${bee.osql.cache.prototype}")
	public int cache_prototype=1;
	
	@SysValue("${bee.osql.cache.useLevelTwo}")
	public boolean cache_useLevelTwo; 
	@SysValue("${bee.osql.cache.levelOneTolevelTwo}")
	public boolean cache_levelOneTolevelTwo; 
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
	
	@SysValue("${bee.distribution.genid.startYear}")
	public int genid_startYear = 0; //V1.17
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
	
	@SysValue("${bee.dosql.multiDS.sharding}")
	public boolean multiDS_sharding; //用于分库分表的分片
	
	//----------------------------- multiDs  end

	public String getDbName() {
		
		checkAndRefreshDbNameForSingleDs(); //单个DS
		//多DS时,在BeeFactory解析parseDbNameByDsMap时设置
		
		if (HoneyContext.isNeedRealTimeDb()) { // 支持同时使用多种数据库的,需要动态获取,才准确
			String dsName = Router.getDsName();
			if (dsName != null && HoneyContext.getDsName2DbName() != null) {
				String temp_dbName = HoneyContext.getDsName2DbName().get(dsName);
				if (temp_dbName == null) { //V1.17
//					Logger.warn("Did not find the dataSource name : " + dsName); //数据源池里没有,应该抛异常
				    throw new ConfigWrongException("Did not find the dataSource name : " + dsName);
				} else {
					return temp_dbName;
				}
			}
		}
		return dbName;
	}

	public void setDbName(String dbName) {
		this.dbName = dbName;
		Logger.info("[Bee] ========= reset the dbName in HoneyConfig is :" + dbName);
//		BeeFactory.getHoneyFactory().setDbFeature(BeeFactory.getHoneyFactory()._getDbDialectFeature());  //循环调用
		BeeFactory.getHoneyFactory().setDbFeature(null);
	}
	
	public int getDatabaseMajorVersion() {
		return databaseMajorVersion;
	}

	public void setDatabaseMajorVersion(int databaseMajorVersion) {
		this.databaseMajorVersion = databaseMajorVersion;
	}

	private static boolean alreadyPrintDbName = false;
	private static boolean changeDataSource = false;

	private static void checkAndRefreshDbNameForSingleDs() {
		//单库时, dbName是null或有更改Ds才要重新设置
		if ( !HoneyConfig.getHoneyConfig().multiDS_enable
				&& (HoneyConfig.getHoneyConfig().dbName == null || changeDataSource)) {

			Connection conn = null;
			try {
				conn = SessionFactory.getConnection();
				if (conn != null) {
					String newDbName=conn.getMetaData().getDatabaseProductName();
			
					HoneyConfig.getHoneyConfig().setDatabaseMajorVersion(0); //clear
					if(DatabaseConst.SQLSERVER.equalsIgnoreCase(newDbName)) { //V1.17 for SQL SERVER
						int majorVersion=conn.getMetaData().getDatabaseMajorVersion();
						HoneyConfig.getHoneyConfig().setDatabaseMajorVersion(majorVersion);
						if(majorVersion>=11) {
							DbFeatureRegistry.register(DatabaseConst.SQLSERVER, new SqlServerFeature2012());
						}
					}

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
				Logger.warn("Can not get the Connection when check the dbName.  \n"+e.getMessage(),e);
			} finally {
				try {
					if (conn != null) conn.close();
				} catch (Exception e2) {
					Logger.error(e2.getMessage(),e2);
				}
				
				if(alreadyPrintDbName && changeDataSource) { //alreadyPrintDbName只打印过
					changeDataSource=false;
					HoneyUtil.refreshSetParaAndResultTypeHandlerRegistry();  //里面有用到dbName
				}
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
		HoneyConfig.setChangeDataSource(true);
		this.url = url;
	}
	
    static void setChangeDataSource(boolean flag) {
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

	public String getSchemaName() {
		return schemaName;
	}

	public void setSchemaName(String schemaName) {
		this.schemaName = schemaName;
	}

	public String getLoggerType() {
		return loggerType;
	}

	public void setLoggerType(String loggerType) {
		this.loggerType = loggerType;
		LoggerFactory.setConfigRefresh(true);
	}
	
	//动态刷新ds
//	public void setDataSourceMap(Map<String, DataSource> dataSourceMap) {
//		BeeFactory.getInstance().setDataSourceMap(dataSourceMap);
//	}
	
//	context有:
//	public static void setDsName2DbName(Map<String, String> dsName2DbName) {
//		HoneyContext.dsName2DbName = dsName2DbName;
//	}

}
