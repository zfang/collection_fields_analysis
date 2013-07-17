import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;

public class Test4 {
   public List f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, f15, f16;
   public static void main(String [] args) {
      Test4 t1 = new Test4();
      List l1 = getList(true);
      List l2 = l1;
      t1.f1 = l2;
      List l3 = t1.f2;
      test(l1, getList(false), l1, l2, t1.f1, l3);
   }
   public static void test(List p1, List p2, List p3, List p4, List p5, List p6) {
      Test4 t1 = new Test4();
      List l1 = getList(true);
      List l2 = getList(false);
      t1.f1 = getList(true);
      t1.f2 = l1;
      t1.f3 = l2;
      t1.f4 = l1;
      t1.f5 = p1;
      t1.f6 = p1;
      t1.f7 = p2;
      t1.f8 = p3;
      t1.f9 = getList2(true);
      t1.f10 = getList2(false);
      t1.f11 = getList3(false);
      t1.f12 = getList4(true);
      t1.f13 = p4;
      t1.f14 = p5;
      t1.f15 = p6;
      t1.f16 = (List)new LinkedList().clone();
   }

   public static List getList(boolean flag) {
      return flag ? new ArrayList() : new LinkedList();
   }

   public static List getList2(boolean flag) {
      Test4 t1 = new Test4();
      return flag ? new ArrayList() : t1.f1;
   }

   public static List getList3(boolean flag) {
      return getList2(flag);
   }

   public static List getList4(boolean flag) {
      return flag ? getList4(!flag) : getList(flag);
   }
}
