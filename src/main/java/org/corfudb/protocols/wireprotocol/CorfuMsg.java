package org.corfudb.protocols.wireprotocol;

import io.netty.buffer.ByteBuf;
import lombok.*;
import org.corfudb.infrastructure.*;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by mwei on 9/15/15.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CorfuMsg {
    /** The unique id of the client making the request */
    long clientID;

    /** The request id of this request/response */
    long requestID;

    /** The epoch of this request/response */
    long epoch;

    @RequiredArgsConstructor
    public enum CorfuMsgType {
        // Base Messages
        PING(0, CorfuMsg.class, BaseServer.class),
        PONG(1, CorfuMsg.class, BaseServer.class),
        RESET(2, CorfuResetMsg.class, BaseServer.class),
        SET_EPOCH(3, CorfuSetEpochMsg.class, BaseServer.class),
        ACK(4, CorfuMsg.class, BaseServer.class),
        WRONG_EPOCH(5, CorfuMsg.class, BaseServer.class),

        // Layout Messages
        LAYOUT_REQUEST(10, CorfuMsg.class, LayoutServer.class),
        LAYOUT_RESPONSE(11, LayoutResponseMsg.class, LayoutServer.class),

        // Sequencer Messages
        TOKEN_REQ(20, TokenRequestMsg.class, SequencerServer.class),
        TOKEN_RES(21, TokenResponseMsg.class, SequencerServer.class),

        // Logging Unit Messages
        WRITE(30, LogUnitWriteMsg.class, LogUnitServer.class),
        READ_REQUEST(31, LogUnitReadRequestMsg.class, LogUnitServer.class),
        READ_RESPONSE(32, LogUnitReadResponseMsg.class, LogUnitServer.class),
        TRIM(33, LogUnitTrimMsg.class, LogUnitServer.class),
        FILL_HOLE(34, LogUnitFillHoleMsg.class, LogUnitServer.class),
        FORCE_GC(35, CorfuMsg.class, LogUnitServer.class),
        GC_INTERVAL(36, LogUnitGCIntervalMsg.class, LogUnitServer.class),

        // Logging Unit Error Codes
        ERROR_OK(40, CorfuMsg.class, LogUnitServer.class),
        ERROR_TRIMMED(41, CorfuMsg.class, LogUnitServer.class),
        ERROR_OVERWRITE(42, CorfuMsg.class, LogUnitServer.class),
        ERROR_OOS(43, CorfuMsg.class, LogUnitServer.class),
        ERROR_RANK(44, CorfuMsg.class, LogUnitServer.class)
        ;

        public final int type;
        public final Class<? extends CorfuMsg> messageType;
        public final Class<? extends IServer> handler;

        public byte asByte() { return (byte)type; }
    };

    static Map<Byte, CorfuMsgType> typeMap =
            Arrays.<CorfuMsgType>stream(CorfuMsgType.values())
                    .collect(Collectors.toMap(CorfuMsgType::asByte, Function.identity()));

    /** The type of message */
    CorfuMsgType msgType;

        /* The wire format of the NettyCorfuMessage message is below:
        | client ID(8) | request ID(8) |  epoch(8)   |  type(1)  |
*/
    /** Serialize the message into the given bytebuffer.
     * @param buffer    The buffer to serialize to.
     * */
    public void serialize(ByteBuf buffer) {
        buffer.writeLong(clientID);
        buffer.writeLong(requestID);
        buffer.writeLong(epoch);
        buffer.writeByte(msgType.asByte());
    }

    /** Parse the rest of the message from the buffer. Classes that extend CorfuMsg
     * should parse their fields in this method.
     * @param buffer
     */
    public void fromBuffer(ByteBuf buffer) {
        // we don't do anything here since in the base message, no fields remain.
    }

    /** Copy the base fields over to this message */
    public void copyBaseFields(CorfuMsg msg)
    {
        this.clientID = msg.clientID;
        this.epoch = msg.epoch;
        this.requestID = msg.requestID;
    }

    /** Take the given bytebuffer and deserialize it into a message.
     *
     * @param buffer    The buffer to deserialize.
     * @return          The corresponding message.
     */
    @SneakyThrows
    public static CorfuMsg deserialize(ByteBuf buffer) {
        long clientID = buffer.readLong();
        long requestID = buffer.readLong();
        long epoch = buffer.readLong();
        CorfuMsgType message = typeMap.get(buffer.readByte());
        CorfuMsg msg = message.messageType.getConstructor().newInstance();
        msg.clientID = clientID;
        msg.requestID = requestID;
        msg.epoch = epoch;
        msg.msgType = message;
        msg.fromBuffer(buffer);
        return msg;
    }

    /** Constructor which generates a message based only the message type.
     * Typically used for generating error messages, since sendmessage will populate the rest of the fields.
     * @param type  The type of message to send.
     */
    public CorfuMsg(CorfuMsgType type)
    {
        this.msgType = type;
    }
}
