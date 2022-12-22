package ticketingsystem;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;

class mReadWriteLock implements ReadWriteLock {
    private int readers;
    private int writers;
    private int threshold = 16;
    private int waitingReaders;
    private int waitingWriters;
    Lock lock;
    Condition condition;
    Lock readLock, writeLock;

    public mReadWriteLock() {
        readers = 0;
        writers = 0;
        lock = new ReentrantLock();
        readLock = new ReadLock();
        writeLock = new WriteLock();
        condition = lock.newCondition();
    }

    public Lock readLock() {
        return readLock;
    }

    public Lock writeLock() {
        return writeLock;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    class ReadLock implements Lock {
        public void lock() {
            lock.lock();
            try {
                while (writers > 0 || waitingWriters >= threshold) {
                    waitingReaders++;
                    condition.await();
                }
                waitingReaders--;
                readers++;
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }

        public void unlock() {
            lock.lock();
            try {
                readers--;
                if (readers == 0) {
                    condition.signalAll();
                }
            } finally {
                lock.unlock();
            }
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
            lock.lock();
            try {
                while (readers > 0 || waitingReaders >= threshold) {
                    waitingWriters++;
                    condition.await();
                }
                waitingWriters--;
                writers++;
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }

        public void unlock() {
            lock.lock();
            try {
                writers--;
                if (writers == 0)
                    condition.signalAll();
            } finally {
                lock.unlock();
            }
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