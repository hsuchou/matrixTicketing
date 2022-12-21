package ticketingsystem;

import java.util.concurrent.atomic.AtomicIntegerArray;

public class TicketingDS implements TicketingSystem {

    // magic const
    private static final int _INT_MAX = 0xffffffff;

    // system info
    int seatNum;

    // route instances
    mRoute[] routeObject;

    // pseudo hashmap for passengers and tickets
    volatile String[] ticketsPassengers;
    static AtomicIntegerArray ticketsMap;

    /**
     * @param routeNum   线路数量
     * @param coachNum   车厢数量
     * @param seatNum    座位数量
     * @param stationNum 车站数量
     * @param threadNum  线程数量
     */
    TicketingDS(int routeNum, int coachNum, int seatNum, int stationNum, int threadNum) {
        this.seatNum = seatNum;
        if (stationNum > Integer.SIZE) {
            throw new IllegalArgumentException();
        }

        routeObject = new mRoute[routeNum];
        for (int i = 0; i < routeNum; i++) {
            routeObject[i] = new mRoute(i + 1, routeNum, coachNum, seatNum, stationNum, threadNum);
        }

        int MAX_TID = threadNum * 102000 + routeNum * 30300;
        ticketsMap = new AtomicIntegerArray(MAX_TID);
        ticketsPassengers = new String[MAX_TID];
        for (int i = 0; i < MAX_TID; i++) {
            ticketsPassengers[i] = null;
            ticketsMap.set(i, _INT_MAX);
        }

    }

    /**
     * @param passenger 乘客姓名
     * @param route     线路
     * @param departure 铁路出发
     * @param arrival   铁路到达
     * @return 火车票
     */
    public Ticket buyTicket(String passenger, int route, int departure, int arrival) {
        Ticket t = routeObject[route - 1].buyTicket(passenger, departure, arrival);
        if (t != null) {
            ticketsMap.set((int) (t.tid), getHash(t));
            ticketsPassengers[(int) (t.tid)] = passenger;
        }
        return t;
    }

    /**
     * @param route     线路
     * @param departure 铁路出发
     * @param arrival   铁路到达
     * @return 有票数量
     */
    public int inquiry(int route, int departure, int arrival) {
        return routeObject[route - 1].inquiry(departure, arrival);
    }

    /**
     * @param ticket 火车票
     * @return 退票状态
     */
    public boolean refundTicket(Ticket ticket) {
        int tid = (int) ticket.tid;

        if (ticketsPassengers[tid] == null
                || !ticket.passenger.equals(ticketsPassengers[tid])
                || !ticketsMap.compareAndSet(tid, getHash(ticket), _INT_MAX)) {
            return false;
        }

        return routeObject[ticket.route - 1].refundTicket(ticket);
    }

    public boolean buyTicketReplay(Ticket ticket) {
        return false;
    }

    public boolean refundTicketReplay(Ticket ticket) {
        return false;
    }

    /**
     * @param t 火车票
     * @return 简易hash, 便于退票时查对信息
     */
    private int getHash(Ticket t) {
        return t.arrival
                | t.departure << 8
                | t.route << 12
                | (t.coach * seatNum + t.seat) << 18;
    }

}
