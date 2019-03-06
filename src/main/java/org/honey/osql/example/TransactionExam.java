package org.honey.osql.example;

import java.math.BigDecimal;
import java.util.List;

import org.bee.osql.BeeException;
import org.bee.osql.Suid;
import org.honey.osql.core.BeeFactory;
import org.honey.osql.example.entity.Orderitem;
import org.honey.osql.example.entity.Orders;
import org.honey.osql.example.entity.User;

/**
 * @author Kingstar
 * @since  1.0
 */
public class TransactionExam {

	public static void main(String[] args) {
		try {
			Suid suid = BeeFactory.getHoneyFactory().getSuid();

			User user = new User();
			user.setName("testName");
			user.setEmail("beeUser@163.com");
			suid.insert(user); //insert

			Orders orders = new Orders();
			orders.setUserid("bee");
			orders.setName("Bee-ORM framework");
			orders.setTotal(new BigDecimal("91.99"));
			orders.setRemark(""); //empty String test

			suid.insert(orders); //insert

			Orderitem orderitem = new Orderitem();
			orderitem.setOrderid(100001L);
			orderitem.setUserid("bee");
			orderitem.setCategory("book");
			orderitem.setPrice(new BigDecimal("30"));

			suid.insert(orderitem);

			List<Orders> list = suid.select(orders); //可任意组合条件查询
			for (int i = 0; i < list.size(); i++) {
				System.out.println(list.get(i).toString());
			}

		} catch (BeeException e) {
			e.printStackTrace();
		}
	}

}
