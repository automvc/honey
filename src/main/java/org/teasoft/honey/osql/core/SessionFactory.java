package org.teasoft.honey.osql.core;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.teasoft.bee.osql.exception.NoConfigException;
import org.teasoft.bee.osql.transaction.Transaction;
import org.teasoft.honey.osql.constant.DbConfigConst;
import org.teasoft.honey.osql.transaction.JdbcTransaction;
import org.teasoft.honey.util.StringUtils;

/**
 * @author Kingstar
 * @since  1.0
 */
public final class SessionFactory {

	private static BeeFactory beeFactory = null;
	private static boolean isFirst=true;

	public static BeeFactory getBeeFactory() {
		if (beeFactory == null) {
//			beeFactory = new BeeFactory();
			beeFactory = BeeFactory.getInstance();
		}
		return beeFactory;
	}
	
	public void setBeeFactory(BeeFactory beeFactory) {
		SessionFactory.beeFactory = beeFactory;
	}

	public SessionFactory() {}

	public static Connection getConnection() {
		Connection conn = null;
		try {
			DataSource ds = getBeeFactory().getDataSource();
			if (ds != null) {
				conn = ds.getConnection();
			} else {//do not set the dataSource
				conn = getOriginalConn();
			}
		} catch (SQLException e) {
			Logger.debug(e.getMessage());
			e.printStackTrace();
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
			tran = new JdbcTransaction();  //  put into context
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

	private static Connection getOriginalConn() throws ClassNotFoundException, SQLException {

		String driverName = HoneyConfig.getHoneyConfig().getDriverName();
		String url = HoneyConfig.getHoneyConfig().getUrl();
		String username = HoneyConfig.getHoneyConfig().getUsername();
		String password = HoneyConfig.getHoneyConfig().getPassword();

		String nullInfo = "";
		if (driverName == null) nullInfo += DbConfigConst.DB_DRIVERNAME + " do not config; ";
		if (url == null) nullInfo += DbConfigConst.DB_URL + " do not config; ";
		if (username == null) nullInfo += DbConfigConst.DB_USERNAM + " do not config; ";
		if (password == null) nullInfo += DbConfigConst.DB_PWORD + " do not config; ";

		if (!"".equals(nullInfo)) {
//			throw new NoConfigException("NoConfigException,Do not set the database info: " + nullInfo);
			if(isFirst){
			  Logger.warn("Do not set the database info: " + nullInfo); 
			  isFirst=false;
			}
		}
		Connection conn = null;
		if (StringUtils.isNotBlank(driverName)) Class.forName(driverName);  //some db,no need set the driverName //v1.8.15

		if (StringUtils.isNotBlank(username) && password != null)
			conn = DriverManager.getConnection(url, username, password);
		else
			conn = DriverManager.getConnection(url);  //v1.8.15

		return conn;
	}
}
