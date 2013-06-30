import java.util.List;
import java.util.LinkedList;

public class Test4 {
   public List f1, f2, f3, f4, f5, f6;
   public static void main(String [] args) {
      Test4 t1 = new Test4();
      t1.f1 = t1.getList();
      boolean pl = t1.f1.isEmpty();
   }
   public List getList() {
      return new LinkedList();
   }
}
