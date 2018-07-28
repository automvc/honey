package org.honey.osql.core;

import org.honey.osql.constant.DbConfigConst;

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
		setShowSQL(Boolean.parseBoolean(BeeProp.getBeeProp("bee.osql.showSQL")));
		String t_batchSize = BeeProp.getBeeProp("bee.osql.select.batchSize");
		if (t_batchSize != null) setBatchSize(Integer.parseInt(t_batchSize));
		String t_maxResultSize = BeeProp.getBeeProp("bee.osql.select.maxResultSize");
		if (t_maxResultSize != null) setMaxResultSize(Integer.parseInt(t_maxResultSize));

		setUnderScoreAndCamelTransform(Boolean.parseBoolean(BeeProp.getBeeProp("bee.osql.underScoreAndCamelTransform")));
		setDbNamingToLowerCaseBefore(Boolean.parseBoolean(BeeProp.getBeeProp("bee.osql.dbNaming.toLowerCaseBefore")));
		//		BeeProp.getBeeProp("bee.osql.delete.isAllowDeleteAllDataInOneTable");

		setDriverName(BeeProp.getBeeProp(DbConfigConst.DB_DRIVERNAME));
		setUrl(BeeProp.getBeeProp(DbConfigConst.DB_URL));
		setUsername(BeeProp.getBeeProp(DbConfigConst.DB_USERNAM));
		setPassword(BeeProp.getBeeProp(DbConfigConst.DB_PASSWORD));

	}

	// 启动时动态获取
	private boolean showSQL;
	private int batchSize = 100; //不设置,默认100
	private String dbName;
	private boolean underScoreAndCamelTransform;
	private boolean dbNamingToLowerCaseBefore;

	private String driverName;
	private String url;
	private String username;
	private String password;
	private int maxResultSize;

	private void setShowSQL(boolean showSQL) {
		this.showSQL = showSQL;
	}

	private void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	private void setDbName(String dbName) {
		this.dbName = dbName;
	}

	private void setUnderScoreAndCamelTransform(boolean underScoreAndCamelTransform) {
		this.underScoreAndCamelTransform = underScoreAndCamelTransform;
	}

	private void setDbNamingToLowerCaseBefore(boolean dbNamingToLowerCaseBefore) {
		this.dbNamingToLowerCaseBefore = dbNamingToLowerCaseBefore;
	}

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

	private void setMaxResultSize(int maxResultSize) {
		this.maxResultSize = maxResultSize;
	}

	public boolean isShowSQL() {
		return showSQL;
	}

	public int getBatchSize() {
		return batchSize;
	}

	public String getDbName() {
		return dbName;
	}

	public boolean isUnderScoreAndCamelTransform() {
		return underScoreAndCamelTransform;
	}

	public boolean isDbNamingToLowerCaseBefore() {
		return dbNamingToLowerCaseBefore;
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

	public int getMaxResultSize() {
		return maxResultSize;
	}

}
