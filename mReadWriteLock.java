package ticketingsystem;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

class mReadWriteLock implements ReadWriteLock {
    private AtomicInteger counter = new AtomicInteger(0);
    Lock readLock = new ReadLock(), writeLock = new WriteLock();

    public mReadWriteLock(int threadNum) {
    }

    public Lock readLock() {
        return readLock;
    }

    public Lock writeLock() {
        return writeLock;
    }

    class ReadLock implements Lock {
        public void lock() {
            while (true) {
                int res = counter.get();
                if (res >= 0) {
                    if (counter.compareAndSet(res, res + 1)) {
                        break;
                    }
                }
            }
        }

        public void unlock() {
            counter.getAndDecrement();
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            // TODO Auto-generated method stub

        }

        @Override
        public Condition newCondition() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean tryLock() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            // TODO Auto-generated method stub
            return false;
        }
    }

    class WriteLock implements Lock {
        public void lock() {
            while (true) {
                int res = counter.get();
                if (res <= 0) {
                    if (counter.compareAndSet(res, res - 1)) {
                        break;
                    }
                }
            }
        }

        public void unlock() {
            counter.getAndIncrement();
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            // TODO Auto-generated method stub

        }

        @Override
        public Condition newCondition() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean tryLock() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            // TODO Auto-generated method stub
            return false;
        }
    }
}