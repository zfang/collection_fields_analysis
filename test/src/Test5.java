import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class Test5 extends HashMap {

   private Set _entrySet;

   public List f1, f2, f3, f4, f5;

   public void setEntrySet() {
      if (_entrySet == null)
         _entrySet = super.entrySet();
   }

   public static void main(String [] args) {
      test(Collections.EMPTY_LIST);
   }

   public static void test(List p1) {
      Test5 t1 = new Test5();
      t1.f1 = Collections.EMPTY_LIST;
      t1.f2 = Collections.emptyList();
      t1.f3 = getList1();
      t1.f4 = Collections.EMPTY_LIST;
      
      t1.setEntrySet();

      t1.f5 = p1;
   }

   public static List getList1() {
      return Collections.EMPTY_LIST;
   }
}
