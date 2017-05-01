package org.corfudb.runtime.object.transactions;

import lombok.extern.slf4j.Slf4j;
import org.corfudb.util.Utils;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/** A class which allows access to transactional contexts, which manage
 * transactions. The static methods of this class provide access to the
 * thread's transaction stack, which is a stack of transaction contexts
 * active for a particular thread.
 *
 * Created by mwei on 1/11/16.
 */
@Slf4j
public class TransactionalContext {

    /** A thread local stack containing all transaction contexts
     * for a given thread.
     */
    private static final ThreadLocal<LinkedList<AbstractTransactionalContext>>
            threadTransactionStack = ThreadLocal.withInitial(
            LinkedList<AbstractTransactionalContext>::new);

    /** Whether or not the current thread is in a nested transaction.
     *
     * @return  True, if the current thread is in a nested transaction.
     */
    public static boolean isInNestedTransaction() {return threadTransactionStack.get().size() > 1;}

    /**
     * Returns the transaction stack for the calling thread.
     *
     * @return The transaction stack for the calling thread.
     */
    public static LinkedList<AbstractTransactionalContext> getTransactionStack() {
        return threadTransactionStack.get();
    }

    /**
     * Returns the current transactional context for the calling thread.
     *
     * @return The current transactional context for the calling thread.
     */
    public static AbstractTransactionalContext getCurrentContext() {
        return getTransactionStack().peekLast();
    }

    /**
     * Returns the last transactional context (parent/root) for the calling thread.
     *
     * @return The last transactional context for the calling thread.
     */
    public static AbstractTransactionalContext getRootContext() {
        return getTransactionStack().peekFirst();
    }

    /**
     * Returns whether or not the calling thread is in a transaction.
     *
     * @return True, if the calling thread is in a transaction.
     * False otherwise.
     */
    public static boolean isInTransaction() {
        return getTransactionStack().peekFirst() != null;
    }

    /** Add a new transactional context to the thread's transaction stack.
     *
     * @param context   The context to add to the transaction stack.
     * @return          The context which was added to the transaction stack.
     */
    public static AbstractTransactionalContext newContext(AbstractTransactionalContext context) {
        log.debug("TX begin[{}]", context);
        if (getRootContext() != null)
                getTransactionStack().addLast(context);
        else
            getTransactionStack().addLast(context);
        return context;
    }

    /** Remove the most recent transaction context from the transaction stack.
     *
     * @return          The context which was removed from the transaction stack.
     */
    public static AbstractTransactionalContext removeContext() {
        AbstractTransactionalContext r = getTransactionStack().pollLast();
        if (getTransactionStack().isEmpty()) {
            synchronized (getTransactionStack())
            {
                getTransactionStack().notifyAll();
            }
        }
        return r;
    }

    /**
     * Get the transaction stack as a list.
     * @return  The transaction stack as a list.
     */
    public static List<AbstractTransactionalContext> getTransactionStackAsList() {
        List<AbstractTransactionalContext> listReverse =
                getTransactionStack().stream().collect(Collectors.toList());
        Collections.reverse(listReverse);
        return listReverse;
    }
}
