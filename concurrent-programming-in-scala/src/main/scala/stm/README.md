# Software Transactional Memory

> Reading multiple atomic variables is not an atomic operation and it can observe the program data in an inconsistent state.

## STM

The `atomic` statement is a fundamental abstraction at the core of every STM. When the program executes a block of code marked 
with `atomic`, it starts a **memory transaction**: a sequence of reads and writes to memory which occur atomically for other threads 
in the program. The `atomic` statement is similar to the `synchronized` statement, and ensure that a block of code executes in isolation, 
without the interference of other threads, thus avoiding race conditions. Unlike the `synchronized` statement, 
the `atomic` statement does not cause deadlocks.

In most STM implementations, the `atomic` statement maintains a log of a read and write operations. Every time a memory location is read 

during a memory transaction, the corresponding memory address is added to the log. 

Similarly, whenever a memory location is written during a memory transaction, the memory address and the proposed value are written to the log. 
Once the execution reaches the end of the `atomic` block, all the writes from the transaction log are written to the memory.

When this happens, we say that the transaction is **committed**. On the other hand, during the transaction, the STM might detect that another 
concurrent transaction performed by some other thread is concurrently reading or writing the same memory location. This situation is called a 
**transactional conflict**. When this occurs, one or both of the transactions are cancelled, and re-executed serially, on after another. 
We say that the STM **rolls back** these transactions. 

Such STMs are called **optimistic**. Optimistic STMs try to execute a transaction under the assumption that it will succeed, 
and roll back when they detect a conflict.

## The Interaction Between Transactions and Side-Effects

```scala
object CompositionSideEffects extends App with ThreadUtils {
  val myValue = Ref(0)

  def inc() = atomic { implicit tx =>
    log(s"Incrementing ${myValue()}")
    myValue() = myValue() + 1
  }

  Future { inc() }
  Future { inc() }

  Thread.sleep(5000)
}

// output
[info] Running stm.CompositionSideEffects 
[info] ForkJoinPool-1-worker-13: Incrementing 0
[info] ForkJoinPool-1-worker-11: Incrementing 0
[info] ForkJoinPool-1-worker-11: Incrementing 1
```

The two transactions commit, but the side-effecting `log` call is executed three times as a result of the rollback.

> Avoiding side effects in the transactions is a recommended practice

You might conclude that, if a side-effecting operation is idempotent, then it is safe to execute it in a transaction. 
After all, the worst thing that can happen is that the idempotent operation gets executed more than once. 

Unfortunately, this reasoning is flawed. After a transaction is rolled back and retired, the values of the transactional references 
might change. The second time a transaction is executed, the arguments to the idempotent operation might be different, or 
the idempotent operation might not be invoked at all. 

> Avoid external side effects inside the transactions, as the transactions can be re-executed multiple times.

We can use `Txn.afterCommit`

```scala
  def inc() = atomic { implicit txn =>
    val valueAtStart = myValue()
    Txn.afterCommit { _ =>
      log(s"Incrementing ${valueAtStart}")
    }

    myValue() = myValue() + 1
  }
  
  // don't do this
  def inc() = atomic { implicit txn =>
    Txn.afterCommit { _ =>
      log(s"Incrementing ${myValue()}")
    }

    myValue() = myValue() + 1
  }
```

Calling the last version of `inc` fails with an exception. Although the transactional context `txn` exists when 
the `afterCommit` method is called, the `afterCommit` block is executed later, the transaction is already over and the `txn` 
object is no longer valid. It is illegal to read or modify transactional references outside a transaction. 

Why does accessing a transactional reference inside the `afterCommit` block only fail at runtime?

The `afterCommit` method is in the static scope of the transaction, or, in other works, is statically nested within an `atomic` statement. 
For this reason, the compiler resolves the `txn` object of the transaction, and allows you to access the the transactional references. 

However, the `afterCommit` block is not executed in the **dynamic scope** of the transaction. In other words, 
the `afterCommit` block is run **after** the `atomic` block returns. By contrast, accessing a transactional reference outside of the `atomic` block is not in the 
static scope of a transaction, so the compiler detects this and reports an error.

<br/>

In general the `InTxn` objects must not escape the transaction block. For example, it is not legal to start an 
asynchronous operation from within the transaction and use the `InTxn` object to access the transactional references.

> Only use the transactional context within the thread that started the transaction.

<br/>

Not all side-effecting operations inside the transactions are bad. As long as the side effects are confined to mutating objects that are created inside the transaction, 
we are free to use them. In fact, such side effects are sometimes necessary. 


```scala
case class Node(elem: Int, next: Ref[Node])

object NodeOperations extends App with ThreadUtils {
  def nodeToString(n: Node): String = atomic { implicit txn =>
    val b = new StringBuilder
    var curr = n

    while (curr != null) {
      b ++= s"${curr.elem}"
      curr = curr.next()
    }

    b.toString()
  }


  def nodeToStringWrong(n: Node): String = {
    val b = new StringBuilder

    atomic { implicit txn =>
      var curr = n
      while (curr != null) {
        b ++= s"${curr.elem}"
        curr = curr.next()
      }
    }

    b.toString()
  }
}
```

If the transaction gets rolled back in the `nodeToStringWrong` example, the contents of the `StringBuilder` object are not cleared.
 
> When mutating an object inside a transaction, make sure that the object is created inside the transaction and the reference to it does not 
escape the scope of the transaction.


