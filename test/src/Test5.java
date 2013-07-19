import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class Test5 extends HashMap {

   private Set _entrySet;

   public List f1, f2, f3, f4;

   public void setEntrySet() {
      _entrySet = super.entrySet();
   }

   public static void main(String [] args) {
      Test5 t1 = new Test5();
      t1.f1 = Collections.EMPTY_LIST;
      t1.f2 = Collections.emptyList();
      t1.f3 = getList1();
      t1.f4 = Collections.EMPTY_LIST;
      
      t1.setEntrySet();
   }

   public static List getList1() {
      return Collections.EMPTY_LIST;
   }
}
