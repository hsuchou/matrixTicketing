package ticketingsystem;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class Test {
    private final static int _THREAD_MAX = 128;
    private final static int TEST_NUM = 1000000;

    private final static int refund = 10;
    private final static int buy = 40;
    private final static int query = 100;

    private final static int ROUTE_NUM = 50;
    private final static int COACH_NUM = 20;
    private final static int SEAT_NUM = 100;
    private final static int STATION_NUM = 30; 

    private final static long[] buyTicketTime = new long[_THREAD_MAX];
    private final static long[] refundTime = new long[_THREAD_MAX];
    private final static long[] inquiryTime = new long[_THREAD_MAX];

    private final static long[] buyTotal = new long[_THREAD_MAX];
    private final static long[] refundTotal = new long[_THREAD_MAX];
    private final static long[] inquiryTotal = new long[_THREAD_MAX];

    private final static AtomicInteger threadId = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException {
        final int[] threadNums = { 4, 8, 16, 32, 64 }; // 4, 8, 16, 32, 64
        for (int p = 0; p < threadNums.length; ++p) {
            final TicketingDS tds = new TicketingDS(ROUTE_NUM, COACH_NUM, SEAT_NUM, STATION_NUM, threadNums[p]);
            Thread[] threads = new Thread[threadNums[p]];
            for (int i = 0; i < threadNums[p]; i++) {
                threads[i] = new Thread(new Runnable() {
                    public void run() {
                        Random rand = new Random();
                        Ticket ticket = new Ticket();
                        int id = threadId.getAndIncrement();
                        ArrayList<Ticket> soldTicket = new ArrayList<>();
                        for (int i = 0; i < TEST_NUM; i++) {
                            int select = rand.nextInt(query);
                            if (0 <= select && select < refund && soldTicket.size() > 0) { // refund ticket 0-10
                                int selectTicketId = rand.nextInt(soldTicket.size());
                                if ((ticket = soldTicket.remove(selectTicketId)) != null) {
                                    long start = System.currentTimeMillis();
                                    tds.refundTicket(ticket);
                                    long end = System.currentTimeMillis();
                                    refundTime[id] += end - start;
                                    refundTotal[id] += 1;
                                } else {
                                    System.out.println("ErrOfRefund2");
                                }
                            } else if (refund <= select && select < buy) { // buy ticket 10-40
                                String passenger = "passenger" + rand.nextInt(TEST_NUM);
                                int route = rand.nextInt(ROUTE_NUM) + 1;
                                int departure = rand.nextInt(STATION_NUM - 1) + 1;
                                int arrival = departure + rand.nextInt(STATION_NUM - departure) + 1;
                                long start = System.currentTimeMillis();
                                ticket = tds.buyTicket(passenger, route, departure, arrival);
                                long end = System.currentTimeMillis();
                                buyTicketTime[id] += end - start;
                                buyTotal[id] += 1;
                                if (ticket != null) {
                                    soldTicket.add(ticket);
                                }
                            } else if (buy <= select && select < query) { // inquiry ticket 40-100
                                int route = rand.nextInt(ROUTE_NUM) + 1;
                                int departure = rand.nextInt(STATION_NUM - 1) + 1;
                                int arrival = departure + rand.nextInt(STATION_NUM - departure) + 1;
                                long start = System.currentTimeMillis();
                                tds.inquiry(route, departure, arrival);
                                long end = System.currentTimeMillis();
                                inquiryTime[id] += end - start;
                                inquiryTotal[id] += 1;
                            }
                        }
                    }
                });
            }
            long start = System.currentTimeMillis();
            for (int i = 0; i < threadNums[p]; ++i)
                threads[i].start();
            for (int i = 0; i < threadNums[p]; i++) {
                threads[i].join();
            }
            long end = System.currentTimeMillis();
            long buyTotalTime = calculateTotal(buyTicketTime, threadNums[p]);
            long refundTotalTime = calculateTotal(refundTime, threadNums[p]);
            long inquiryTotalTime = calculateTotal(inquiryTime, threadNums[p]);

            long bTotal = calculateTotal(buyTotal, threadNums[p]);
            long rTotal = calculateTotal(refundTotal, threadNums[p]);
            long iTotal = calculateTotal(inquiryTotal, threadNums[p]);

            double buyAvgTime = (double) (buyTotalTime) / bTotal;
            double refundAvgTime = (double) (refundTotalTime) / rTotal;
            double inquiryAvgTime = (double) (inquiryTotalTime) / iTotal;

            long time = end - start;

            long t = (long) (threadNums[p] * TEST_NUM / (double) time); // 1000??????ms?????????s

            System.out.println(String.format(
                    "ThreadNum: %d BuyAvgTime(ms): %.5f RefundAvgTime(ms): %.5f InquiryAvgTime(ms): %.5f ThroughOut(op/ms): %d",
                    threadNums[p], buyAvgTime, refundAvgTime, inquiryAvgTime, t));
            clear();
        }
    }

    private static long calculateTotal(long[] array, int threadNums) {
        long res = 0;
        for (int i = 0; i < threadNums; ++i)
            res += array[i];
        return res;
    }

    private static void clear() {
        threadId.set(0);
        long[][] arrays = { buyTicketTime, refundTime, inquiryTime, buyTotal, refundTotal, inquiryTotal };
        for (int i = 0; i < arrays.length; ++i)
            for (int j = 0; j < arrays[i].length; ++j)
                arrays[i][j] = 0;
    }

}
