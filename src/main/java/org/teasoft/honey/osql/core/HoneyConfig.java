package org.teasoft.honey.osql.core;

import java.sql.Connection;

import org.teasoft.bee.osql.annotation.SysValue;
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
		setDateWithMillisecondInSelectJson(Boolean.parseBoolean(BeeProp.getBeeProp("bee.osql.selectJson.date.withMillisecond")));
		setTimeWithMillisecondInSelectJson(Boolean.parseBoolean(BeeProp.getBeeProp("bee.osql.selectJson.time.withMillisecond")));
		setNullToEmptyStringInReturnStringList(Boolean.parseBoolean(BeeProp.getBeeProp("bee.osql.select.returnStringList.nullToEmptyString"))); 
		
		String t1 = BeeProp.getBeeProp("bee.osql.cache.map.size"); //缓存集数据量大小
		if (t1 != null) setCacheMapSize(Integer.parseInt(t1));
		
		String t2 = BeeProp.getBeeProp("bee.osql.cache.timeout");//缓存保存时间(毫秒 ms)
		if (t2 != null) setCacheTimeout(Integer.parseInt(t2));
		
		String t4 = BeeProp.getBeeProp("bee.osql.cache.startDeleteCache.rate"); 
		if (t4 != null) setStartDeleteCacheRate(Double.parseDouble(t4));
		
		String t5 = BeeProp.getBeeProp("bee.osql.cache.fullUsed.rate"); 
		if (t5 != null) setCachefullUsedRate(Double.parseDouble(t5));
		
		String t6 = BeeProp.getBeeProp("bee.osql.cache.fullClearCache.rate"); 
		if (t6 != null) setFullClearCacheRate(Double.parseDouble(t6));
		
		
		SysValueProcessor.process(honeyConfig);
	}

	// 启动时动态获取
	@SysValue("${bee.log.loggerType}")
	public String loggerType; //v1.8
	
	@SysValue("${bee.osql.showSQL}")
	private boolean showSQL=false;
	@SysValue("${bee.osql.showSQL.showType}")
	private boolean showSQLShowType;//v1.8
	@SysValue("${bee.osql.showSQL.showExecutableSql}")
	private boolean showExecutableSql;//v1.8
	
	@SysValue("${bee.osql.showSQL.donotPrint.currentDate}")
	private boolean showSQL_donotPrint_currentDate;  //v1.7.0
	
	@SysValue("${bee.osql.log.donotPrint.level}")
	private boolean log_donotPrint_level=true;  //v1.7.2
	
	@SysValue("${bee.osql.donot.allowed.deleteWholeRecords}")
	private boolean notDeleteWholeRecords=true;  //v1.7.2
	
	@SysValue("${bee.osql.donot.allowed.updateWholeRecords}")
	private boolean notUpdateWholeRecords=true;  //v1.7.2
	
	@SysValue("${bee.osql.date.format}")
	private String dateFormat;  //v1.7.2
	
	@SysValue("${bee.osql.moreTable.columnListWithStar}")
	private boolean moreTable_columnListWithStar;
	
	@SysValue("${bee.osql.moreTable.select.2tablesWithJoinOnStyle}")
	private boolean tablesWithJoinOnStyle;
	
	@SysValue("${bee.databaseName}")
	public String dbName;
//	private boolean underScoreAndCamelTransform;//closed since v1.7
	
	@SysValue("${bee.osql.dbNaming.toLowerCaseBefore}")
	private boolean dbNamingToLowerCaseBefore=true;  //default : to LowerCase before
	
	@SysValue("${bee.osql.naming.translate.type}")
	private int namingTranslateType =1;
	
	@SysValue("${bee.osql.sql.keyword.case}")
	public String sqlKeyWordCase="";
	
	@SysValue("${bee.osql.selectJson.ignoreNull}")
	private boolean ignoreNullInSelectJson=true;
	@SysValue("${bee.osql.selectJson.timestamp.withMillisecond}")
	private boolean timestampWithMillisecondInSelectJson;
	private boolean dateWithMillisecondInSelectJson;
	private boolean timeWithMillisecondInSelectJson;
	private boolean nullToEmptyStringInReturnStringList;
	

	@SysValue("${"+DbConfigConst.DB_DRIVERNAME+"}")
	private String driverName;
	
	@SysValue("${"+DbConfigConst.DB_URL+"}")
	private String url;
	
	@SysValue("${"+DbConfigConst.DB_USERNAM+"}")
	private String username;
	
	@SysValue("${"+DbConfigConst.DB_PWORD+"}")
	private String password;

//	@SysValue("${aaa}")
	
	@SysValue("${bee.osql.name.mapping.entity2table}")
	public String entity2tableMappingList;
	
	private int cacheTimeout=10000;
	private int cacheMapSize=1000;
	private String cacheType="FIFO";
	
	private double startDeleteCacheRate=0.6;  //when timeout use
	private double cachefullUsedRate=0.8;      //when add element in cache use
	private double fullClearCacheRate=0.2;  //when add element in cache use
	
	@SysValue("${bee.osql.cache.nocache}")
	private boolean nocache;    //v1.7.2
	
	@SysValue("${bee.osql.cache.key.useMD5}")
	boolean cacheKeyUseMD5=true;
	
	/////////////////
//	@SysValue("${bee.osql.select.maxResultSize}")
//	private int selectMaxResultSize;
	
//	@SysValue("${bee.osql.select.batchSize}") //closed. the name is confused.  v1.9
	@SysValue("${bee.osql.insert.batchSize}")
	private int batchSize = 10000; //不设置,默认10000
	
	
	@SysValue("${bee.osql.cache.work.resultSet.size}")
	private int cacheWorkResultSetSize=300;
	
	@SysValue("${bee.osql.cache.never}")
	private String neverCacheTableList ; 
	
	@SysValue("${bee.osql.cache.forever}")
	private String foreverCacheTableList ; 
	
	@SysValue("${bee.osql.cache.forever.modifySyn}")
	private String foreverCacheModifySynTableList ; 
	
	//v1.8
	@SysValue("${bee.distribution.genid.workerid}")
	public int workerid=0 ;
	
	@SysValue("${bee.distribution.genid.idGeneratorType}")
	public int idGeneratorType=1 ;
	

	@SysValue("${bee.distribution.PearFlowerId.tolerateSecond}")
	public long tolerateSecond=10 ;
	@SysValue("${bee.distribution.PearFlowerId.useHalfWorkId}")
	public boolean useHalfWorkId; 
	@SysValue("${bee.distribution.PearFlowerId.switchWorkId.timeThreshold}")
	public long switchWorkIdTimeThreshold=120 ;
	@SysValue("${bee.distribution.PearFlowerId.randomNum.bound}")
	public int randomNumBound=2 ; //v1.8.15
	
	@SysValue("${bee.distribution.genid.forAllTableLongId}")
	public boolean genid_forAllTableLongId;
	@SysValue("${bee.distribution.genid.entityList.includes}")
	public String entityList_includes;
	@SysValue("${bee.distribution.genid.entityList.excludes}")
	public String entityList_excludes;
	
	@SysValue("${bee.dosql.multi-DS.enable}")
	public boolean enableMultiDs;
	@SysValue("${bee.dosql.multi-DS.type}")
	public int multiDsType ; //注意,系统会设初值0
	@SysValue("${bee.dosql.multi-DS.defalut-DS}")
	public String multiDsDefalutDS; 
	@SysValue("${bee.dosql.multi-DS.writeDB}")
	public String multiDs_writeDB;   //multiDsType=1
	@SysValue("${bee.dosql.multi-DS.readDB}")
	public String multiDs_readDB;  //multiDsType=1
	@SysValue("${bee.dosql.multi-DS.rDB.routeWay}")
	public int rDbRouteWay ; //注意,系统会设初值0  //multiDsType=1
			
//	@SysValue("${bee.dosql.multi-DS.db}")
//	public String multi_dbList;
	
	@SysValue("${bee.dosql.multi-DS.match.entityClassPath}")
	public String matchEntityClassPath="";  //multiDsType=2
	
	@SysValue("${bee.dosql.multi-DS.match.table}")
	public String matchTable="";  //multiDsType=2
	

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
	
//	public String getEntity2tableMappingList() {
//		return entity2tableMappingList;
//	}

	public boolean isShowSQL() {
		return showSQL;
	}
	
	public boolean isShowSQLShowType() {
		return showSQLShowType;
	}

	public boolean isShowExecutableSql() {
		return showExecutableSql;
	}

	public boolean isShowSQL_donotPrint_currentDate() {
		return showSQL_donotPrint_currentDate;
	}
	
	public boolean isLog_donotPrint_level() {
		return log_donotPrint_level;
	}

	public boolean isNotDeleteWholeRecords() {
		return notDeleteWholeRecords;
	}
	
	public boolean isNotUpdateWholeRecords() {
		return notUpdateWholeRecords;
	}

	public String getDateFormat() {
		return dateFormat;
	}

	public boolean isMoreTable_columnListWithStar() {
		return moreTable_columnListWithStar;
	}
	
	public boolean isTablesWithJoinOnStyle() {
		return tablesWithJoinOnStyle;
	}

	public int getBatchSize() {
		return batchSize;
	}

	public String getDbName() {
		checkAndInitDbName();
		return dbName;
	}

	public boolean isDbNamingToLowerCaseBefore() {
		return dbNamingToLowerCaseBefore;
	}
	
	public int getNamingTranslateType() {
		return namingTranslateType;
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

//	public int getSelectMaxResultSize() {
//		return selectMaxResultSize;
//	}
	
	public boolean isIgnoreNullInSelectJson() {
		return ignoreNullInSelectJson;
	}

	public boolean isTimestampWithMillisecondInSelectJson() {
		return timestampWithMillisecondInSelectJson;
	}

	public boolean isDateWithMillisecondInSelectJson() {
		return dateWithMillisecondInSelectJson;
	}

	private void setDateWithMillisecondInSelectJson(boolean dateWithMillisecondInSelectJson) {
		this.dateWithMillisecondInSelectJson = dateWithMillisecondInSelectJson;
	}

	public boolean isTimeWithMillisecondInSelectJson() {
		return timeWithMillisecondInSelectJson;
	}

	private void setTimeWithMillisecondInSelectJson(boolean timeWithMillisecondInSelectJson) {
		this.timeWithMillisecondInSelectJson = timeWithMillisecondInSelectJson;
	}

	public boolean isNullToEmptyStringInReturnStringList() {
		return nullToEmptyStringInReturnStringList;
	}

	private void setNullToEmptyStringInReturnStringList(boolean nullToEmptyStringInReturnStringList) {
		this.nullToEmptyStringInReturnStringList = nullToEmptyStringInReturnStringList;
	}

	public int getCacheTimeout() {
		return cacheTimeout;
	}

	public int getCacheMapSize() {
		return cacheMapSize;
	}

	private void setCacheTimeout(int cacheTimeout) {
		this.cacheTimeout = cacheTimeout;
	}

	private void setCacheMapSize(int cacheMapSize) {
		this.cacheMapSize = cacheMapSize;
	}

	public int getCacheWorkResultSetSize() {
		return cacheWorkResultSetSize;
	}

	public String getCacheType() {
		return cacheType;
	}

//	private void setCacheType(String cacheType) {
//		this.cacheType = cacheType;
//	}

	public double getStartDeleteCacheRate() {
		return startDeleteCacheRate;
	}

	private void setStartDeleteCacheRate(double startDeleteCacheRate) {
		this.startDeleteCacheRate = startDeleteCacheRate;
	}

	public double getCachefullUsedRate() {
		return cachefullUsedRate;
	}

	private void setCachefullUsedRate(double cachefullUsedRate) {
		this.cachefullUsedRate = cachefullUsedRate;
	}

	public double getFullClearCacheRate() {
		return fullClearCacheRate;
	}

	private void setFullClearCacheRate(double fullClearCacheRate) {
		this.fullClearCacheRate = fullClearCacheRate;
	}

	public String getNeverCacheTableList() {
		return neverCacheTableList;
	}

	public String getForeverCacheTableList() {
		return foreverCacheTableList;
	}

	public String getForeverCacheModifySynTableList() {
		return foreverCacheModifySynTableList;
	}

	public boolean isNocache() {
		return nocache;
	}
	
	private static boolean alreadyPrintDbName=false;
	
	private static void checkAndInitDbName() {
		if (HoneyConfig.getHoneyConfig().dbName == null) {
			Connection conn = null;
			try {
				conn = SessionFactory.getConnection();
				if (conn != null) {
					HoneyConfig.getHoneyConfig().dbName = conn.getMetaData().getDatabaseProductName();
					Logger.info("[Bee] ========= get the dbName from the Connection is :"+HoneyConfig.getHoneyConfig().dbName);
					alreadyPrintDbName=true;
				}
			} catch (Exception e) {
//				e.printStackTrace();
				Logger.error(e.getMessage());
			} finally {
				try {
					if (conn != null) conn.close();
				} catch (Exception e2) {
//					e2.printStackTrace();
					Logger.error(e2.getMessage());
				}
			}
		} else {
			if (!alreadyPrintDbName){
				Logger.info("[Bee] ========= get the dbName from HoneyConfig is :" + HoneyConfig.getHoneyConfig().dbName);
				alreadyPrintDbName=true;
			}
		}
	}
}
