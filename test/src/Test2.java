import java.util.List;
import java.util.LinkedList;

public class Test2 {
   public List f1, f2, f3, f4, f5, f6;
   public static void main(String [] args) {
      Test2 t1 = new Test2();
      List l1 = new LinkedList(); 
      List l2 = l1; 
      List l3 = null; 
      List l4 = l3; 
      t1.f1 = l2;
      t1.f2 = l1;
      t1.f3 = l3; 
      t1.f4 = (List)new LinkedList(); 
      t1.f5 = l4;
      t1.f6 = t1.getList();

      Object o1 = new Object();
      Object o2 = new Object();
      Object o3 = new Object();
      Object o4 = new Object();

      t1.f1.add(o1);
      t1.f2.add(o2);
      t1.f4.add(o3);
      t1.f4.add(o4);
      t1.f6.add(o3);
   }
   public List getList() {
      return new LinkedList();
   }
}
