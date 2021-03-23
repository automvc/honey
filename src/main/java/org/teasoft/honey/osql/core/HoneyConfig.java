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

	// 启动时动态获取
	@SysValue("${bee.log.loggerType}")
	public String loggerType; //v1.8
	
	@SysValue("${bee.osql.log.donotPrint.level}")
	public boolean log_donotPrint_level = true; //v1.7.2

	@SysValue("${bee.osql.date.format}")
	public String dateFormat; //v1.7.2   use in DateUtil

	//----------------------------- showSQL start
	@SysValue("${bee.osql.showSQL}")
	boolean showSQL = false;

	@SysValue("${bee.osql.showSQL.showType}")
	boolean showSQL_showType;//v1.8

	@SysValue("${bee.osql.showSQL.showExecutableSql}")
	boolean showSQL_executableSql;//v1.8

	@SysValue("${bee.osql.showSQL.donotPrint.currentDate}")
	public boolean showSQL_donotPrint_currentDate; //v1.7.0
	//----------------------------- showSQL end

	@SysValue("${bee.osql.donot.allowed.deleteWholeRecords}")
	boolean notDeleteWholeRecords = true; //v1.7.2

	@SysValue("${bee.osql.donot.allowed.updateWholeRecords}")
	boolean notUpdateWholeRecords = true; //v1.7.2

	@SysValue("${bee.osql.moreTable.columnListWithStar}")
	boolean moreTable_columnListWithStar;

	@SysValue("${bee.osql.moreTable.select.2tablesWithJoinOnStyle}")
	boolean moreTable_2tablesWithJoinOnStyle;

	@SysValue("${bee.databaseName}")
	public String dbName;
//	private boolean underScoreAndCamelTransform;//closed since v1.7

	@SysValue("${bee.osql.dbNaming.toLowerCaseBefore}")
	public boolean dbNamingToLowerCaseBefore = true; //default : to LowerCase before

	@SysValue("${bee.osql.naming.translate.type}")
	int namingTranslateType = 1;

	@SysValue("${bee.osql.sql.keyword.case}")
	public String sqlKeyWordCase = "";

	//----------------------------- selectJson start
	@SysValue("${bee.osql.selectJson.ignoreNull}")
	boolean selectJson_ignoreNull = true;
	@SysValue("${bee.osql.selectJson.timestamp.withMillisecond}")
	boolean selectJson_timestampWithMillisecond;

	@SysValue("${bee.osql.selectJson.date.withMillisecond}")
	boolean selectJson_dateWithMillisecond;

	@SysValue("${bee.osql.selectJson.time.withMillisecond}")
	boolean selectJson_timeWithMillisecond;
	//----------------------------- selectJson end
	
	@SysValue("${bee.osql.select.returnStringList.nullToEmptyString}")
	boolean returnStringList_nullToEmptyString;
	

	@SysValue("${" + DbConfigConst.DB_DRIVERNAME + "}")
	private String driverName;

	@SysValue("${" + DbConfigConst.DB_URL + "}")
	private String url;

	@SysValue("${" + DbConfigConst.DB_USERNAM + "}")
	private String username;

	@SysValue("${" + DbConfigConst.DB_PWORD + "}")
	private String password;

	//	@SysValue("${aaa}")

	@SysValue("${bee.osql.name.mapping.entity2table}")
	public String entity2tableMappingList;

	//	@SysValue("${bee.osql.select.batchSize}") //closed. the name is confused.  v1.9
	@SysValue("${bee.osql.insert.batchSize}")
	int insertBatchSize = 10000; //不设置,默认10000

	//----------------------------- cache start
	@SysValue("${bee.osql.cache.timeout}")
	int cache_timeout = 10000; //缓存保存时间(毫秒 ms)

	@SysValue("${bee.osql.cache.map.size}")
	int cache_mapSize = 1000; //缓存集数据量大小

//	private String cacheType="FIFO";

	@SysValue("${bee.osql.cache.startDeleteCache.rate}")
	double cache_startDeleteCacheRate = 0.6; //when timeout use

	@SysValue("${bee.osql.cache.fullUsed.rate}")
	double cache_fullUsedRate = 0.8; //when add element in cache use

	@SysValue("${bee.osql.cache.fullClearCache.rate}")
	double cache_fullClearCacheRate = 0.2; //when add element in cache use

	@SysValue("${bee.osql.cache.key.useMD5}")
	boolean cache_keyUseMD5 = true;

	@SysValue("${bee.osql.cache.nocache}")
	private boolean nocache; //v1.7.2

	@SysValue("${bee.osql.cache.work.resultSet.size}")
	int cache_workResultSetSize = 300;

	@SysValue("${bee.osql.cache.never}")
	String cache_neverCacheTableList;

	@SysValue("${bee.osql.cache.forever}")
	String cache_foreverCacheTableList;

	@SysValue("${bee.osql.cache.forever.modifySyn}")
	String cache_foreverCacheModifySynTableList;
	//----------------------------- cache end

	//----------------------------- genid  start
	//v1.8
	@SysValue("${bee.distribution.genid.workerid}")
	public int genid_workerid = 0;

	@SysValue("${bee.distribution.genid.idGeneratorType}")
	public int genid_idGeneratorType = 1;

	@SysValue("${bee.distribution.genid.forAllTableLongId}")
	public boolean genid_forAllTableLongId;
	@SysValue("${bee.distribution.genid.entityList.includes}")
	public String genid_entityList_includes;
	@SysValue("${bee.distribution.genid.entityList.excludes}")
	public String genid_entityList_excludes;

	@SysValue("${bee.distribution.PearFlowerId.tolerateSecond}")
	public long pearId_tolerateSecond = 10;
	@SysValue("${bee.distribution.PearFlowerId.useHalfWorkId}")
	public boolean pearId_useHalfWorkId;
	@SysValue("${bee.distribution.PearFlowerId.switchWorkId.timeThreshold}")
	public long pearId_switchWorkIdTimeThreshold = 120;
	@SysValue("${bee.distribution.PearFlowerId.randomNum.bound}")
	public int pearId_randomNumBound = 2; //v1.8.15
	//----------------------------- genid  end

	//----------------------------- multiDs  start
	@SysValue("${bee.dosql.multi-DS.enable}")
	public boolean enableMultiDs;
	@SysValue("${bee.dosql.multi-DS.type}")
	public int multiDsType; //注意,系统会设初值0
	@SysValue("${bee.dosql.multi-DS.defalut-DS}")
	public String multiDsDefalutDS;
	@SysValue("${bee.dosql.multi-DS.writeDB}")
	public String multiDs_writeDB; //multiDsType=1
	@SysValue("${bee.dosql.multi-DS.readDB}")
	public String multiDs_readDB; //multiDsType=1
	@SysValue("${bee.dosql.multi-DS.rDB.routeWay}")
	public int rDbRouteWay; //注意,系统会设初值0  //multiDsType=1

	@SysValue("${bee.dosql.multi-DS.match.entityClassPath}")
	public String multiDs_matchEntityClassPath = ""; //multiDsType=2

	@SysValue("${bee.dosql.multi-DS.match.table}")
	public String multiDs_matchTable = ""; //multiDsType=2

	//	支持同时使用多种类型数据库的数据源.support different type muli-Ds at same time.
	@SysValue("${bee.dosql.multi-DS.different.dbType}")
	public boolean multiDs_differentDbType;
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
		Logger.info("[Bee] ========= reset the dbName in HoneyConfig is :" + HoneyConfig.getHoneyConfig().dbName);
		BeeFactory.getHoneyFactory().setDbFeature(BeeFactory.getHoneyFactory()._getDbDialectFeature());
	}
	
	private static boolean alreadyPrintDbName = false;

	private static void checkAndInitDbName() {
		if (HoneyConfig.getHoneyConfig().dbName == null) {
			Connection conn = null;
			try {
				conn = SessionFactory.getConnection();
				if (conn != null) {
					HoneyConfig.getHoneyConfig().dbName = conn.getMetaData().getDatabaseProductName();
					Logger.info("[Bee] ========= get the dbName from the Connection is :" + HoneyConfig.getHoneyConfig().dbName);
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

	public boolean isNocache() {
		return nocache;
	}

}
