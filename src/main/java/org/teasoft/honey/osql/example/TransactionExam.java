package org.teasoft.honey.osql.example;

import java.math.BigDecimal;
import java.util.List;

import org.teasoft.bee.osql.BeeException;
import org.teasoft.bee.osql.Suid;
import org.teasoft.bee.osql.transaction.Transaction;
import org.teasoft.honey.osql.core.BeeFactory;
import org.teasoft.honey.osql.core.SessionFactory;
import org.teasoft.honey.osql.example.entity.Orderitem;
import org.teasoft.honey.osql.example.entity.Orders;

/**
 * @author Kingstar
 * @since  1.0
 */
public class TransactionExam {

	public static void main(String[] args) {
		Transaction transaction=SessionFactory.getTransaction();
		try {
			
			
			transaction.begin();
			
			Suid suid = BeeFactory.getHoneyFactory().getSuid();

			Orders orders = new Orders();
			orders.setId(101L);
			orders.setUserid("bee");
			orders.setName("Bee-ORM framework");
			orders.setTotal(new BigDecimal("91.99"));
			orders.setRemark(""); //empty String test

			suid.insert(orders); //insert

			Orderitem orderitem = new Orderitem();
			orderitem.setId(12L);
			orderitem.setOrderid(101L);
			orderitem.setUserid("bee");
			orderitem.setCategory("book");
			orderitem.setPrice(new BigDecimal("30"));

			suid.insert(orderitem);
			
			transaction.commit();

			List<Orders> list = suid.select(orders); //可任意组合条件查询
			for (int i = 0; i < list.size(); i++) {
				System.out.println(list.get(i).toString());
			}

		} catch (BeeException e) {
			e.printStackTrace();
			transaction.rollback();
		}
	}

}
