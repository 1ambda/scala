package filesystem

import forkjoin.ExecutorUtils
import thread.ThreadUtils

object FileSystemWatcher extends App with ExecutorUtils {

  val fs = new FileSystem("/Users/1002471/Desktop/")

//  execute {
//    fs.copyFile("sy-2.png", "sy-44.png")
//  }

  execute {
    fs.deleteFile("sy-2.png")
    fs.logMessage("Testing Log!")
  }

  execute {
    fs.deleteFile("sy-2.png")
  }

  Thread.sleep(2000)
}

object FileSystemMonitorExample extends App with ThreadUtils {
  import scala.concurrent.ExecutionContext.Implicits.global

  FileSystemMonitor.fileCreated(".") foreach {
    case filename => log(s"Detected new file $filename`")
  }
}
