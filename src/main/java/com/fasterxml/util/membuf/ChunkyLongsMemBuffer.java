package com.fasterxml.util.membuf;

import com.fasterxml.util.membuf.base.LongsSegment;
import com.fasterxml.util.membuf.base.ChunkyMemBufferBase;

public abstract class ChunkyLongsMemBuffer extends ChunkyMemBufferBase<LongsSegment>
{
    /**
     * Segment that was peeked, if any. When entries are peeked, a copy
     * of data is store in this property and actual contents are remove
     * as if entry was read normally.
     */
    protected long[] _peekedEntry;
    
    protected ChunkyLongsMemBuffer(SegmentAllocator<LongsSegment> allocator,
           int minSegmentsToAllocate, int maxSegmentsToAllocate,
           LongsSegment initialSegments)
    {
       super(allocator, minSegmentsToAllocate, maxSegmentsToAllocate, initialSegments);
    }

    protected ChunkyLongsMemBuffer(ChunkyLongsMemBuffer src) {
        super(src);
        _peekedEntry = src._peekedEntry;
    }

   /*
   /**********************************************************************
   /* Public API, simple statistics (not data) accessors
   /**********************************************************************
    */

   @Override
   public synchronized int getEntryCount() {
       return (_peekedEntry == null) ? _entryCount : (_entryCount+1);
   }

   @Override
   public synchronized boolean isEmpty() {
       return (_entryCount == 0) && (_peekedEntry == null);
   }

   @Override
   public synchronized long getTotalPayloadLength()
   {
       return (_peekedEntry == null) ? _totalPayloadLength
               : (_peekedEntry.length + _totalPayloadLength);
   }
   
   /*
   /**********************************************************************
   /* Public API, write (append)
   /**********************************************************************
    */

   /**
    * Method that tries to append an entry in buffer and returning;
    * if there is no room, a {@link IllegalStateException} is thrown.
    */
   public final void appendEntry(long[] data) {
       appendEntry(data, 0, data.length);
   }

   /**
    * Method that tries to append an entry in buffer and returning;
    * if there is no room, a {@link IllegalStateException} is thrown.
    */
   public final void appendEntry(long[] data, int dataOffset, int dataLength) {
       if (!tryAppendEntry(data, dataOffset, dataLength)) {
           throw new IllegalStateException("Not enough room in buffer to append entry of "+dataLength
                   +" (can't allocate enough new segments)");
       }
   }

   /**
    * Method that tries to append an entry in buffer if there is enough room;
    * if there is, entry is appended and 'true' returned; otherwise no changes
    * are made and 'false' is returned.
    */
   public final boolean tryAppendEntry(long[] data) {
       return tryAppendEntry(data, 0, data.length);
   }
   
   /**
    * Method that tries to append an entry in buffer if there is enough room;
    * if there is, entry is appended and 'true' returned; otherwise no changes
    * are made and 'false' is returned.
    */
   public abstract boolean tryAppendEntry(long[] data, int dataOffset, int dataLength);
   
   /*
   /**********************************************************************
   /* Public API, getting next entry
   /**********************************************************************
    */

   /**
    * Method that will check size of the next entry, if buffer has entries;
    * returns size in longs if there is at least one entry, or -1 if buffer
    * is empty.
    * Note that this method does not remove the entry and can be called multiple
    * times, that is, it is fully idempotent.
    */
   @Override
   public abstract int getNextEntryLength();

   /**
    * Method for reading and removing next available entry from buffer.
    * If no entry is available, will block to wait for more data.
    */
   public abstract long[] getNextEntry() throws InterruptedException;

   /**
    * Method that will read, remove and return next entry, if one is
    * available; or return null if not.
    */
   public abstract long[] getNextEntryIfAvailable();
   
   /**
    * Method to get (and remove) next entry from the buffer, if one
    * is available. If buffer is empty, may wait up to specified amount
    * of time for new data to arrive.
    * 
    * @param timeoutMsecs Amount of time to wait for more data if
    *   buffer is empty, if non-zero positive number; if zero or
    *   negative number, will return immediately
    *   
    * @return Next entry from buffer, if one was available either
    *   immediately or before waiting for full timeout; or null
    *   if no entry became available
    */
   public abstract long[] getNextEntry(long timeoutMsecs) throws InterruptedException;

   /*
   /**********************************************************************
   /* Public API, reading next entry in caller-provided array
   /**********************************************************************
    */
   
   /**
    * Method for reading and removing next available entry from buffer and
    * return length of the entry in longs, if succesful; or, if buffer does
    * not have enough space, return negative number as error code.
    * If no entry is available, will block to wait for more data.
    * 
    * @param buffer Buffer in which entry is to be read: must have enough space
    *  for read to succeed
    * @param offset Offset in buffer to use for storing results
    *
    * @return Length of the entry (non-negative) if read succeeds;
    *   or, negative number that indicates length of the entry in case
    *   of failures: for example, if buffer only had space for 4 longs,
    *   and entry length was 6, would return -6.
    */
   public abstract int readNextEntry(long[] buffer, int offset) throws InterruptedException;

   /**
    * Method for reading and removing next available entry from buffer and
    * return length of the entry in longs, if successful; or, if buffer does
    * not have enough space, return negative number as error code.
    * If no entry is available, will return {@link Integer#MIN_VALUE}.
    * 
    * @param buffer Buffer in which entry is to be read: must have enough space
    *  for read to succeed
    * @param offset Offset in buffer to use for storing results
    *
    * @return {@link Integer#MIN_VALUE} if no entry was available,
    *   length of the entry (non-negative) read if read succeeds,
    *   or negative number that indicates length of the entry in case
    *   of failures: for example, if buffer only had space for 4 longs,
    *   and entry length was 6, would return -6.
    */
   public abstract int readNextEntryIfAvailable(long[] buffer, int offset);
   
   /**
    * Method for reading and removing next entry from the buffer, if one
    * is available.
    * If buffer is empty, may wait up to specified amount of time for new data to arrive.
    * If no entry is available after timeout, will return {@link Integer#MIN_VALUE}.
    * If length of entry exceeds available buffer space, will return negative number
    * that indicates length of the entry that would have been copied.
    * 
    * @param timeoutMsecs Amount of time to wait for more data if
    *   buffer is empty, if non-zero positive number; if zero or
    *   negative number, will return immediately
    * @param buffer Buffer in which entry is to be read: must have enough space
    *  for read to succeed
    * @param offset Offset in buffer to use for storing results
    *
    * @return {@link Integer#MIN_VALUE} if no entry was available,
    *   length of the entry (non-negative) read if read succeeds,
    *   or negative number that indicates length of the entry in case
    *   of failures: for example, if buffer only had space for 4 longs,
    *   and entry length was 6, would return -6.
    */
   public abstract int readNextEntry(long timeoutMsecs, long[] buffer, int offset)
       throws InterruptedException;
   
   /*
   /**********************************************************************
   /* Public API, read-like access: skipping, wait-for-next
   /**********************************************************************
    */
   
   @Override
   public synchronized int skipNextEntry()
   {
       if (_head == null) {
           _reportClosed();
       }
       if (_entryCount < 1) {
           return -1;
       }
       if (_peekedEntry != null) {
           int len = _peekedEntry.length;
           _peekedEntry = null;
           return len;
       }
       
       final int segLen = getNextEntryLength();
       // ensure lengthh indicator gets reset for chunk after this one
       _nextEntryLength = -1;
       // and reduce entry count as well
       --_entryCount;
       _totalPayloadLength -= segLen;

       // a trivial case; marker entry (no payload)
       int remaining = segLen;
       String error = null;
       while (remaining > 0) {
           remaining -= _tail.skip(remaining);
           if (remaining == 0) { // all skipped?
               break;
           }
           error = _freeReadSegment(error);
       }
       if (error != null) {
           throw new IllegalStateException(error);
       }
       return segLen;
   }
   
   /**
    * Method that will read, and return (but NOT remove) the next entry,
    * if one is available; or return null if none available.
    * Method is idempotent.
    *<p>
    * Note that implementations may require additional storage
    * for keeping track of recently peeked entry; for example, they
    * may retain a long array copy of the contents separate from
    * physical storage.
    */
   public abstract long[] peekNextEntry();

   /*
   /**********************************************************************
   /* Abstract method implementations
   /**********************************************************************
    */

   @Override
   protected void _clearPeeked() {
       _peekedEntry = null;
   }

   @Override
   protected int _peekedLength() {
       return (_peekedEntry == null) ? 0 : _peekedEntry.length;
   }
}
