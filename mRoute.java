package ticketingsystem;

import java.util.Random;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;

public class mRoute {

    // rwlock
    final mReadWriteLock rwLock[];
    final Lock readLock[];
    final Lock writeLock[];

    // magic const
    private static final int _INT_MAX = 0xffffffff;

    // route info
    int routeIndex;
    int routeNum;
    int coachNum;
    int seatNum;
    int stationNum;

    // record array
    int routeSize;
    AtomicIntegerArray routeRecord;

    // global tid for route
    AtomicLong globalRouteTid;

    /**
     * @param routeIndex 线路编号
     * @param routeNum   线路数量
     * @param coachNum   车厢数量
     * @param seatNum    座位数量
     * @param routeSize  车次容量
     * @param stationNum 车站数量
     * @param threadNum  线程数量
     */
    mRoute(int routeIndex, int routeNum,
            int coachNum, int seatNum, int routeSize, int stationNum, int threadNum) {
        
        rwLock = new mReadWriteLock[stationNum - 1];
        readLock = new Lock[stationNum - 1];
        writeLock = new Lock[stationNum - 1];
        for (int i = 0; i < stationNum - 1; i++) {
            rwLock[i] = new mReadWriteLock();
            rwLock[i].setThreshold(threadNum / 2);
            readLock[i] = rwLock[i].readLock();
            writeLock[i] = rwLock[i].writeLock();
        }

        this.routeIndex = routeIndex;
        this.routeNum = routeNum;
        this.coachNum = coachNum;
        this.seatNum = seatNum;
        this.stationNum = stationNum;
        this.routeSize = routeSize;

        globalRouteTid = new AtomicLong(routeIndex);
        routeRecord = new AtomicIntegerArray(routeSize);
    }

    /**
     * @param passenger 乘客姓名
     * @param departure 铁路出发
     * @param arrival   铁路到达
     * @return 火车票
     */
    public Ticket buyTicket(String passenger, int departure, int arrival) {
        for (int i = departure - 1; i < arrival - 1; i++) {
            writeLock[i].lock();
        }

        // get trip info
        int tourCover = getTourCoverInBit(departure, arrival);

        // pick random index to scan in order to avoid competition
        Random rand = new Random();

        for (int fakeSeatTag = rand.nextInt(routeSize),
                end = fakeSeatTag + routeSize; fakeSeatTag != end; fakeSeatTag++) {
            while (true) {
                int seatTag = fakeSeatTag % routeSize;
                int seatRecord = routeRecord.get(seatTag);
                if ((seatRecord & tourCover) != 0) {
                    // seat is already occupied
                    break;
                }

                if (routeRecord.compareAndSet(seatTag, seatRecord, seatRecord | tourCover)) {
                    // successfully add a ticket
                    Ticket t = getTicket(passenger, seatTag, departure, arrival);
                    for (int i = departure - 1; i < arrival - 1; i++) {
                        writeLock[i].unlock();
                    }
                    return t;
                }
            }
        }

        // fail to buy any ticket
        for (int i = departure - 1; i < arrival - 1; i++) {
            writeLock[i].unlock();
        }
        return null;
    }

    /**
     * @param departure 铁路出发
     * @param arrival   铁路到达
     * @return 有票数量
     */
    public int inquiry(int departure, int arrival) {
        for (int i = departure - 1; i < arrival - 1; i++) {
            readLock[i].lock();
        }

        // get trip info
        int tourCover = getTourCoverInBit(departure, arrival);

        // count available seats
        int count = 0, index = routeSize;
        while (index-- > 0) {
            count += (routeRecord.get(index) & tourCover) == 0 ? 1 : 0;
        }
        for (int i = departure - 1; i < arrival - 1; i++) {
            readLock[i].unlock();
        }
        return count;
    }

    /**
     * @param ticket 火车票
     * @return 退票状态
     */
    public boolean refundTicket(Ticket ticket) {
        for (int i = ticket.departure - 1; i < ticket.arrival - 1; i++) {
            writeLock[i].lock();
        }

        // get seat info
        int seatTag = (ticket.coach - 1) * seatNum + ticket.seat - 1;

        while (true) {
            int seatRecord = routeRecord.get(seatTag);
            if (routeRecord.compareAndSet(seatTag, seatRecord,
                    seatRecord ^ getTourCoverInBit(ticket.departure, ticket.arrival))) {
                // successfully refund the ticket
                for (int i = ticket.departure - 1; i < ticket.arrival - 1; i++) {
                    writeLock[i].unlock();
                }
                return true;
            }
        }
    }

    /**
     * @param departure 铁路出发
     * @param arrival   铁路到达
     * @return 描述行程情况的整数(bitmap)
     */
    private int getTourCoverInBit(int departure, int arrival) {
        return (_INT_MAX << (departure - 1)) & (_INT_MAX >>> (33 - arrival));
    }

    /**
     * @param passenger 乘客姓名
     * @param seatTag   座位绝对位置
     * @param departure 铁路出发
     * @param arrival   铁路到达
     * @return 火车票
     */
    private Ticket getTicket(
            String passenger,
            int seatTag,
            int departure,
            int arrival) {
        Ticket t = new Ticket();

        t.tid = globalRouteTid.getAndAdd(routeNum);
        t.passenger = passenger;
        t.route = routeIndex;
        t.coach = seatTag / seatNum + 1;
        t.seat = seatTag % seatNum + 1;
        t.departure = departure;
        t.arrival = arrival;

        return t;
    }

}
