package com.jeysiva.booking.common;

// Two ways to stop two people booking the same seat, selected by the booking.locking-strategy property.
public enum LockingStrategy {

    // Both reads go through; the @Version column rejects whoever writes second. No locks held — best when
    // contention is rare, since the loser just retries.
    OPTIMISTIC,

    // SELECT ... FOR UPDATE locks the row at read time, so the second booker waits until the first commits
    // and then sees it RESERVED. No wasted work, but bookings serialise and waiters hold a DB connection.
    PESSIMISTIC
}
