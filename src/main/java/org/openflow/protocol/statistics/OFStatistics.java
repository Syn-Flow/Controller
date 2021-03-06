package org.openflow.protocol.statistics;

import java.nio.ByteBuffer;

/**
 * The base class for all OpenFlow multipart messages (primarily statistics).
 *
 * @author David Erickson (daviderickson@cs.stanford.edu) - Mar 11, 2010
 */
public interface OFStatistics {
    /**
     * Returns the wire length of this message in bytes
     * @return the length
     */
    public int getLength();

    /**
     * Read this message off the wire from the specified ByteBuffer
     * @param data
     */
    public void readFrom(ByteBuffer data);

    /**
     * Write this message's binary format to the specified ByteBuffer
     * @param data
     */
    public void writeTo(ByteBuffer data);

    /**
     * Computes, sets and returns the length of this OFStatistics message. The
     * value will be able to be retrieved from {@link #getLength()} without
     * recomputing after this method is called.
     * @return
     */
    public int computeLength();
}
