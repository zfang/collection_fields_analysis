import java.util.List;
import java.util.LinkedList;

public class Test4 {
   public List f1, f2, f3, f4, f5, f6, f7, f8;
   public static void main(String [] args) {
      List l1 = getList();
      test(l1, getList(), l1);
   }
   public static void test(List p1, List p2, List p3) {
      Test4 t1 = new Test4();
      List l1 = getList();
      List l2 = getList();
      t1.f1 = getList();
      t1.f2 = l1;
      t1.f3 = l2;
      t1.f4 = l1;
      t1.f5 = p1;
      t1.f6 = p1;
      t1.f7 = p2;
      t1.f8 = p3;
      boolean pl = t1.f1.isEmpty();
   }
   public static List getList() {
      return new LinkedList();
   }
}
