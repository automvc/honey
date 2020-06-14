package org.teasoft.honey.osql.core;

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
		setDbName(BeeProp.getBeeProp("bee.databaseName"));
//		setShowSQL(Boolean.parseBoolean(BeeProp.getBeeProp("bee.osql.showSQL")));
//		String t_batchSize = BeeProp.getBeeProp("bee.osql.select.batchSize");
//		if (t_batchSize != null) setBatchSize(Integer.parseInt(t_batchSize));
//		String t_maxResultSize = BeeProp.getBeeProp("bee.osql.select.maxResultSize");
//		if (t_maxResultSize != null) setMaxResultSize(Integer.parseInt(t_maxResultSize));

//		setUnderScoreAndCamelTransform(Boolean.parseBoolean(BeeProp.getBeeProp("bee.osql.underScoreAndCamelTransform")));
//		setDbNamingToLowerCaseBefore(Boolean.parseBoolean(BeeProp.getBeeProp("bee.osql.dbNaming.toLowerCaseBefore")));
		//		BeeProp.getBeeProp("bee.osql.delete.isAllowDeleteAllDataInOneTable");
		setIgnoreNullInSelectJson(Boolean.parseBoolean(BeeProp.getBeeProp("bee.osql.selectJson.ignoreNull"))); //2019-08-17
		setTimestampWithMillisecondInSelectJson(Boolean.parseBoolean(BeeProp.getBeeProp("bee.osql.selectJson.timestamp.withMillisecond")));
		setDateWithMillisecondInSelectJson(Boolean.parseBoolean(BeeProp.getBeeProp("bee.osql.selectJson.date.withMillisecond")));
		setTimeWithMillisecondInSelectJson(Boolean.parseBoolean(BeeProp.getBeeProp("bee.osql.selectJson.time.withMillisecond")));
		setNullToEmptyStringInReturnStringList(Boolean.parseBoolean(BeeProp.getBeeProp("bee.osql.select.returnStringList.nullToEmptyString"))); 
		
		setDriverName(BeeProp.getBeeProp(DbConfigConst.DB_DRIVERNAME));
		setUrl(BeeProp.getBeeProp(DbConfigConst.DB_URL));
		setUsername(BeeProp.getBeeProp(DbConfigConst.DB_USERNAM));
		setPassword(BeeProp.getBeeProp(DbConfigConst.DB_PASSWORD));
		
//		setCacheType(BeeProp.getBeeProp("bee.osql.cache.type"));  //暂时只有FIFO
		
		String t1 = BeeProp.getBeeProp("bee.osql.cache.map.size"); //缓存集数据量大小
		if (t1 != null) setCacheMapSize(Integer.parseInt(t1));
		
		String t2 = BeeProp.getBeeProp("bee.osql.cache.timeout");//缓存保存时间(毫秒 ms)
		if (t2 != null) setCacheTimeout(Integer.parseInt(t2));
		
//		String t3 = BeeProp.getBeeProp("bee.osql.cache.work.resultSet.size"); //resultset超过一定的值将不会放缓存
//		if (t3 != null) setCacheWorkResultSetSize(Integer.parseInt(t3));
		
		String t4 = BeeProp.getBeeProp("bee.osql.cache.startDeleteCache.rate"); 
		if (t4 != null) setStartDeleteCacheRate(Double.parseDouble(t4));
		
		String t5 = BeeProp.getBeeProp("bee.osql.cache.fullUsed.rate"); 
		if (t5 != null) setCachefullUsedRate(Double.parseDouble(t5));
		
		String t6 = BeeProp.getBeeProp("bee.osql.cache.fullClearCache.rate"); 
		if (t6 != null) setFullClearCacheRate(Double.parseDouble(t6));
		
		
		SysValueProcessor.process(honeyConfig);
	}

	// 启动时动态获取
	@SysValue("${bee.osql.showSQL}")
	private boolean showSQL;
	@SysValue("${bee.osql.showSQL.showType}")
	private boolean showSQLShowType;//v1.7.3
	@SysValue("${bee.osql.showSQL.showExecutableSql}")
	private boolean showExecutableSql;//v1.7.3
	
	@SysValue("${bee.osql.showSQL.donotPrint.currentDate}")
	private boolean showSQL_donotPrint_currentDate;  //v1.7.0
	
	@SysValue("${bee.osql.log.donotPrint.level}")
	private boolean log_donotPrint_level;  //v1.7.2
	
	@SysValue("${bee.osql.donot.allowed.deleteWholeRecords}")
	private boolean notDeleteWholeRecords;  //v1.7.2
	
	@SysValue("${bee.osql.donot.allowed.updateWholeRecords}")
	private boolean notUpdateWholeRecords;  //v1.7.2
	
	@SysValue("${bee.osql.date.format}")
	private String dateFormat;  //v1.7.2
	
	@SysValue("${bee.osql.moreTable.columnListWithStar}")
	private boolean moreTable_columnListWithStar;
	
	@SysValue("${bee.osql.sqlGenerate.moreTableSelect.2tablesWithJoinOnStyle}")
	private boolean tablesWithJoinOnStyle;
	
	private String dbName;
//	private boolean underScoreAndCamelTransform;//closed since v1.7
	
	@SysValue("${bee.osql.dbNaming.toLowerCaseBefore}")
	private boolean dbNamingToLowerCaseBefore=true;  //default : to LowerCase before
	
	@SysValue("${bee.osql.naming.translate.type}")
	private int namingTranslateType =1;
	
	private boolean ignoreNullInSelectJson;//2019-08-17
	private boolean timestampWithMillisecondInSelectJson;
	private boolean dateWithMillisecondInSelectJson;
	private boolean timeWithMillisecondInSelectJson;
	private boolean nullToEmptyStringInReturnStringList;

	private String driverName;
	private String url;
	private String username;
	private String password;

//	@SysValue("${aaa}")
	
	@SysValue("${bee.osql.name.mapping.entity2table}")
	private String entity2tableMappingList;
	
	private int cacheTimeout=10000;
	private int cacheMapSize=1000;
	private String cacheType="FIFO";
	
	private double startDeleteCacheRate=0.6;  //when timeout use
	private double cachefullUsedRate=0.8;      //when add element in cache use
	private double fullClearCacheRate=0.2;  //when add element in cache use
	
	@SysValue("${bee.osql.cache.nocache}")
	private boolean nocache;    //v1.7.2
	
	
	/////////////////
	@SysValue("${bee.osql.select.maxResultSize}")
	private int selectMaxResultSize;
	
	@SysValue("${bee.osql.select.batchSize}")
	private int batchSize = 100; //不设置,默认100
	
	
	@SysValue("${bee.osql.cache.work.resultSet.size}")
	private int cacheWorkResultSetSize=300;
	
	@SysValue("${bee.osql.cache.never}")
	private String neverCacheTableList ; 
	
	@SysValue("${bee.osql.cache.forever}")
	private String foreverCacheTableList ; 
	
	@SysValue("${bee.osql.cache.forever.modifySyn}")
	private String foreverCacheModifySynTableList ; 
	
	

//	private void setShowSQL(boolean showSQL) {
//		this.showSQL = showSQL;
//	}

//	private void setBatchSize(int batchSize) {
//		this.batchSize = batchSize;
//	}

	private void setDbName(String dbName) {
		this.dbName = dbName;
	}

//	private void setUnderScoreAndCamelTransform(boolean underScoreAndCamelTransform) {
//		this.underScoreAndCamelTransform = underScoreAndCamelTransform;
//	}

//	private void setDbNamingToLowerCaseBefore(boolean dbNamingToLowerCaseBefore) {
//		this.dbNamingToLowerCaseBefore = dbNamingToLowerCaseBefore;
//	}

	private void setDriverName(String driverName) {
		this.driverName = driverName;
	}

	private void setUrl(String url) {
		this.url = url;
	}

	private void setUsername(String username) {
		this.username = username;
	}

	private void setPassword(String password) {
		this.password = password;
	}
	
	public String getEntity2tableMappingList() {
		return entity2tableMappingList;
	}

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
		return dbName;
	}

//	public boolean isUnderScoreAndCamelTransform() {
//		return underScoreAndCamelTransform;
//	}

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

	public int getSelectMaxResultSize() {
		return selectMaxResultSize;
	}
	
	public boolean isIgnoreNullInSelectJson() {
		return ignoreNullInSelectJson;
	}

	private void setIgnoreNullInSelectJson(boolean ignoreNullInSelectJson) {
		this.ignoreNullInSelectJson = ignoreNullInSelectJson;
	}
	
	public boolean isTimestampWithMillisecondInSelectJson() {
		return timestampWithMillisecondInSelectJson;
	}

	private void setTimestampWithMillisecondInSelectJson(boolean timestampWithMillisecondInSelectJson) {
		this.timestampWithMillisecondInSelectJson = timestampWithMillisecondInSelectJson;
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

//	private void setCacheWorkResultSetSize(int cacheWorkResultSetSize) {
//		this.cacheWorkResultSetSize = cacheWorkResultSetSize;
//	}

	public String getCacheType() {
		return cacheType;
	}

	private void setCacheType(String cacheType) {
		this.cacheType = cacheType;
	}

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
}
