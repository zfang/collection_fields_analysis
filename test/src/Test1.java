import java.util.List;
import java.util.LinkedList;

public class Test1 {
   public List f1, f2, f3, f4, f5, f6;
   public static void main(String [] args) {
      Test1 t1 = new Test1();
      List l1 = new LinkedList(); 
      List l2 = l1; 
      List l3 = null; 
      List l4 = l3; 
      t1.f1 = l2;
      t1.f2 = l3; 
      t1.f3 = (List)new LinkedList(); 
      t1.f4 = l4;
      t1.f5 = t1.getList();
      t1.f6 = null;
   }
   public List getList() {
      return new LinkedList();
   }
}
