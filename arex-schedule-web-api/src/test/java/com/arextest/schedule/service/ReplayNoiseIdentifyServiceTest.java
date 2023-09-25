package com.arextest.schedule.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@RunWith(JUnit4.class)
public class ReplayNoiseIdentifyServiceTest {

    boolean finalFlg = false;

    /**
     * CyclicBarrier 适用再多线程相互等待，直到到达一个屏障点。并且CyclicBarrier是可重用的。
     */

    @Test
    public void test() throws InterruptedException {

        Map<String, List<String>> map = new ConcurrentHashMap<>();

//        for (int i = 0; i < 10; i++) {
//            List<String> orDefault = map.getOrDefault("1", new ArrayList<>());
//            orDefault.add("c");
//            map.put("1", orDefault);
//        }


        CyclicBarrier cyclicBarrier = new CyclicBarrier(20);
        ExecutorService executorService = Executors.newFixedThreadPool(20);

        for (int i = 0; i < 20; i++) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        cyclicBarrier.await();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    } catch (BrokenBarrierException e) {
                        throw new RuntimeException(e);
                    }

                    map.compute("1", (k, v) -> {
                        if (v == null) {
                            v = new ArrayList<>();
                        }
                        v.add("c");
                        return v;
                    });
//                        map.computeIfAbsent("1", k -> new ArrayList<>()).add("c");
//                    List<String> orDefault = map.getOrDefault("1", new ArrayList<>());
//                    orDefault.add("c");
//                    map.put("1", orDefault);
                }
            });
            executorService.submit(thread);
        }
        Thread.sleep(5000);
        System.out.println();
    }
}
