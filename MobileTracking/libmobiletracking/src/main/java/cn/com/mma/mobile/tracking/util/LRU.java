package cn.com.mma.mobile.tracking.util;

/**
 * Author:zhangqian
 * Time:2020/10/14
 * Version:
 * Description:MobileTracking
 */
import java.util.LinkedHashMap;

public class LRU<K,V> {

        private LinkedHashMap<K, V> map;
        private int cacheSize;

        public LRU(final int cacheSize)
        {
            this.cacheSize = cacheSize;
            map = new LinkedHashMap<K,V>(16,0.75F,true){
                @Override
                protected boolean removeEldestEntry(Entry eldest) {
                    if(cacheSize + 1 == map.size()){
                        return true;
                    }else{
                        return false;
                    }
                }
            };
        }
        public synchronized V get(K key) {
            return map.get(key);
        }
        public synchronized void put(K key,V value) {
            map.put(key, value);
        }
        public synchronized void clear() {
            map.clear();
        }
         public synchronized V remove(K key) {
            return map.remove(key);
         }
        public synchronized int usedSize() {
            return map.size();
        }

//        public void print() {
//            for (Map.Entry<K, V> entry : map.entrySet()) {
//                System.out.print(entry.getValue() + "--");
//            }
//            System.out.println();
//        }


    }




