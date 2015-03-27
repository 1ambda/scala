import org.scalatest._

class OptionTest extends FlatSpec with Matchers {
  // based on http://twitter.github.io/scala_school/ko/collections.html

  "null" should "be avoided. Use Option[T] instead" in {
    // base on http://twitter.github.io/effectivescala/#Functional programming-Options

    trait Peemptible[T] {
      def isAvailable(): Boolean
      def isNotAvailable(): Boolean = !isAvailable
      def occupy(locker: T): Option[T]
      def release(): Option[T]
    }

    case class Thread(name: String, priority: Int)

    class CPU(var core: Option[Thread]) extends Peemptible[Thread] {
      def isAvailable(): Boolean = {
        core match {
          case Some(t) if t.name == "Idle" => true
          case _ => false
        }
      }

      // if returned thread is the same one that was passed as parameter
      // occupying core failed because of priority.
      // otherwise, return None or a previous thread
      def occupy(thread: Thread): Option[Thread] = {

        core match {
          case Some(c) if c.priority >= thread.priority => {
            // case: current thread's priority is higher than the passed one
            Some(c)
          }
          case _ => {
            // case1: if core is idle
            // case2: current thread's priority is lower than the passed one
            val prev: Option[Thread] = core
            core = Some(thread)
            prev
          }
        }
      } 

      def release(): Option[Thread] = {
        val prev = core

        core match {
          // if current thread is idle thread,
          // keep the idle state
          case Some(t) if t.name != "Idle" => {
            core = Some(Thread("Idle", 0))
          }
          case _ => ;
        }

        prev
      }
    }

    // test preparation
    val idleThread = Thread("Idle", 0)
    val userThread = Thread("User", 3)
    val kernelThread = Thread("Kernel", 10)
    val cpu = new CPU(Some(idleThread))

    // idle state test
    assert(cpu.isAvailable == true)
    assert(cpu.release == Some(idleThread))
    assert(cpu.occupy(idleThread) == Some(idleThread))

    // occupy test
    assert(cpu.occupy(userThread) == Some(idleThread))
    assert(cpu.occupy(kernelThread) == Some(userThread))
    assert(cpu.release == Some(kernelThread))
  }
}
