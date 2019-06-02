package org.teasoft.honey.osql.example.autogen;

import org.teasoft.bee.osql.BeeException;
import org.teasoft.honey.osql.atuogen.GenBean;
import org.teasoft.honey.osql.atuogen.GenConfig;
import org.teasoft.honey.osql.core.HoneyConfig;

/**
 * @author Kingstar
 * @since  1.0
 */
public class GenBeanExam {
		public static void main(String[] args) {

		 try{
			String dbName=HoneyConfig.getHoneyConfig().getDbName();
//			driverName,url,username,password config in bee.properties.

			GenConfig config = new GenConfig();

			config.setDbName(dbName);
			
			config.setGenToString(true);
			config.setGenSerializable(true);
			
			config.setBaseDir("D:\\JavaWeb\\workspaceGit\\Honey\\src\\main\\java\\");
			config.setPackagePath("org.teasoft.honey.osql.example.entity");

			GenBean genBean = new GenBean(config);
			
//			genBean.genAllBeanFile();
//			genBean.genSomeBeanFile("orders,items");//只创建部分表对应的JavaBean
//			genBean.genSomeBeanFile("orders");
			genBean.genSomeBeanFile("orderitem");
//			genBean.genSomeBeanFile("ordersView");
			
		  } catch (BeeException e) {
			 e.printStackTrace();
		  }
	}
}
