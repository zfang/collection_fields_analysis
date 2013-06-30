import java.util.List;
import java.util.LinkedList;

public class Test3 {
   public List f1, f2, f3, f4, f5;
   public static void main(String [] args) {
      Test3 t1 = new Test3();
      List l1 = new LinkedList(); 
      List l2 = l1; 
      List l3 = null; 
      List l4 = l3; 
      t1.f1 = l2;
      t1.f1 = l1;
      t1.f2 = l3; 
      t1.f2 = (List)new LinkedList(); 
      t1.f3 = l4;
      t1.f3 = t1.getList();
      t1.f4 = null;
      t1.f4 = t1.f2;
      t1.f5 = t1.f2;
      t1.f5 = l3;
   }
   public List getList() {
      return new LinkedList();
   }
}
