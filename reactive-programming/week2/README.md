# Week2

### Functions and States

Rewriting can be done anywhere in a term, and all rewritings which terminate lead to the same solution. 
This is an important result of the **lambda-calculus**, the theory behind functional programming.

An object **has a state** if its behavior is influenced by its history.   
For example, a bank account has a state because the answer to the question  

**"can I withdraw 100 CHF?"**
 
may vary over the course of the lifetime of the account

```scala
class BankAccount {
  private var balance = 0
  def deposit(amount: Int): Unit = {
    if (amount > 0) balance += amount
  }
  def withdraw(amount: Int): Int = {
    if (0 < amount && amount <= balance) {
      balance -= amount
      balance
    } else throw new Error("insufficient funds")
  }
}
val acc = new BankAccount
acc deposit 50
acc withdraw 20
acc withdraw 20
acc withdraw 15
```

