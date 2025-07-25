/*
 * Copyright 2013-2023 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.teasoft.bee.osql.DatabaseConst;
import org.teasoft.bee.osql.exception.NoConfigException;
import org.teasoft.bee.osql.transaction.Transaction;
import org.teasoft.honey.database.ClientDataSource;
import org.teasoft.honey.database.DatabaseClientConnection;
import org.teasoft.honey.logging.Logger;
import org.teasoft.honey.mongodb.MongodbConnection;
import org.teasoft.honey.osql.constant.DbConfigConst;
import org.teasoft.honey.osql.transaction.JdbcTransaction;
import org.teasoft.honey.util.StringUtils;

/**
 * 会话工厂类.Session Factory.
 * @author Kingstar
 * @since  1.0
 */
public final class SessionFactory {

	private static BeeFactory beeFactory = null;
	private static boolean isFirst = true;
	private static boolean isFirstWithOriginal = true;

	public static BeeFactory getBeeFactory() {
		if (beeFactory == null) {
			beeFactory = BeeFactory.getInstance();
		}
		return beeFactory;
	}

	public void setBeeFactory(BeeFactory beeFactory) {
		_setBeeFactory(beeFactory);
	}

	private static void _setBeeFactory(BeeFactory beeFactory0) {
		SessionFactory.beeFactory = beeFactory0;
	}

	public SessionFactory() {
		// empty
	}

	public static DatabaseClientConnection getDatabaseConnection() {
		DatabaseClientConnection dbConnection = null;
		try {
			DataSource ds = getBeeFactory().getDataSource();
			if (ds != null) {
				try (Connection conn = ds.getConnection()) {
					String dbName = conn.getMetaData().getDatabaseProductName();
					if (DatabaseConst.MongoDB.equalsIgnoreCase(dbName)) {
						dbConnection = new DatabaseClientConnection((ClientDataSource) ds);
					}
				}
			}
		} catch (SQLException e) {
			Logger.debug(e.getMessage());
			throw ExceptionHelper.convert(e);
		} catch (Exception e) {
			throw ExceptionHelper.convert(e);
		}

		return dbConnection;
	}

	public static Connection getConnection() {
		Connection conn = null;
		try {
			DataSource ds = getBeeFactory().getDataSource();

			if (ds == null) { // V1.11
				boolean isJndiType = HoneyConfig.getHoneyConfig().jndiType;
				if (isJndiType) {// Jndi type
					ds = new JndiDataSource().getDataSource();
					if (ds != null) {
						getBeeFactory().setDataSource(ds);
					}
				}
			}

			if (ds != null) {
				conn = ds.getConnection();
			} else {// do not set the dataSource
				conn = getOriginalConn();
				if (isFirstWithOriginal || HoneyConfig.getHoneyConfig().multiDS_enable) {
					isFirstWithOriginal = false;
					Logger.debug("Use OriginalConn!");
				}
			}
		} catch (SQLException e) {
			Logger.debug(e.getMessage());
			throw ExceptionHelper.convert(e);
		} catch (ClassNotFoundException e) {
			Logger.error("Can not find the Database driver!  " + e.getMessage());
			throw new NoConfigException("Can not find the Database driver(maybe miss the jar file).");
		} catch (Exception e) {
			Logger.error("Have Exception when getConnection: " + e.getMessage());
			throw ExceptionHelper.convert(e);
		}

		return conn;
	}

	public static Transaction getTransaction() {
		Transaction tran = null;
		if (getBeeFactory().getTransaction() == null) { // do not set the dataSource
			boolean isAndroid = HoneyConfig.getHoneyConfig().isAndroid;
			boolean isHarmony = HoneyConfig.getHoneyConfig().isHarmony;
			if (isAndroid || isHarmony) {
				String c = "";
				if (isAndroid)      c = "org.teasoft.beex.android.SQLiteTransaction";
				else if (isHarmony) c = "org.teasoft.beex.harmony.SQLiteTransaction";

				try {
					return (Transaction) Class.forName(c).newInstance();
				} catch (Exception e) {
					Logger.error(e.getMessage(), e);
				}
			} else if (HoneyUtil.isMongoDB()) {
				String c = "org.teasoft.beex.mongodb.MongodbTransaction";
				try {
					return (Transaction) Class.forName(c).newInstance();
				} catch (Exception e) {
					Logger.error(e.getMessage(), e);
				}
			}

			tran = new JdbcTransaction(); // put into context

//			tran=HoneyContext.getCurrentTransaction();
//			if(tran==null){
//				tran = new JdbcTransaction();
//				HoneyContext.setCurrentTransaction(tran);
//			}

		} else {
			tran = getBeeFactory().getTransaction();
		}

		return tran;
	}

	private static Connection getOriginalConn() throws ClassNotFoundException, SQLException, Exception {

		String driverName = HoneyConfig.getHoneyConfig().getDriverName();
		String url = HoneyConfig.getHoneyConfig().getUrl();
		String username = HoneyConfig.getHoneyConfig().getUsername();
		String p = HoneyConfig.getHoneyConfig().getPassword();

		if (StringUtils.isBlank(url)) { // check from application.properties
			BootApplicationProp prop = new BootApplicationProp();
			url = prop.getPropText(DbConfigConst.DB_URL);

			if (StringUtils.isBlank(url)) {// if null, use spring boot application.properties; easy for main class type
				url = prop.getPropText(BootApplicationProp.DATASOURCE_URL);
				username = prop.getPropText(BootApplicationProp.DATASOURCE_USERNAME);
				p = prop.getPropText(BootApplicationProp.DATASOURCE_PW);
				String driverClass1 = prop.getPropText(BootApplicationProp.DATASOURCE_DRIVER_CLASS_NAME);
				String driverClass2 = prop.getPropText(BootApplicationProp.DATASOURCE_DRIVER_CLASS_NAME2);
				driverName = (driverClass1 != null ? driverClass1 : driverClass2);
			} else {
				username = prop.getPropText(DbConfigConst.DB_USERNAM);
				p = prop.getPropText(DbConfigConst.DB_PWORD);
				driverName = prop.getPropText(DbConfigConst.DB_DRIVERNAME);
			}

		}

		return getOriginalConnForIntra(url, username, p, driverName);
	}

	public static Connection getOriginalConnForIntra(String url, String username, String p, String driverName)
			throws ClassNotFoundException, SQLException, Exception {

		String nullInfo = "";
		final String DO_NOT_CONFIG = " do not config; ";
		if (driverName == null) nullInfo += DbConfigConst.DB_DRIVERNAME + DO_NOT_CONFIG;
		if (url == null) nullInfo += DbConfigConst.DB_URL + DO_NOT_CONFIG;

		if (url == null) {
			if (HoneyConfig.getHoneyConfig().multiDS_enable)
				Logger.warn("Now is multi-DataSource model, please confirm whether set the config !!!");
			throw new Exception(
					"The url can not be null when get the Connection directly from DriverManager!  (" + nullInfo + ")");
		}

		if (username == null) nullInfo += DbConfigConst.DB_USERNAM + DO_NOT_CONFIG;
		if (p == null) nullInfo += DbConfigConst.DB_PWORD + DO_NOT_CONFIG;

		if (!"".equals(nullInfo) && HoneyConfig.getHoneyConfig().getDbs() == null) { // V2.1.8 add && getDbs
			if (isFirst) {
				Logger.warn("Do not set the database info: " + nullInfo);
				isFirst = false;
			}
		}
		Connection conn = null;
		if (StringUtils.isNotBlank(driverName)) Class.forName(driverName); // some db,no need set the driverName //v1.8.15

		if (username != null && p != null) {
			if (url.trim().startsWith("mongodb:")) return new MongodbConnection();
			conn = DriverManager.getConnection(url, username, p);
		} else {
			if (url.trim().startsWith("mongodb:")) return new MongodbConnection();
			conn = DriverManager.getConnection(url); // v1.8.15
		}
		return conn;
	}
}
