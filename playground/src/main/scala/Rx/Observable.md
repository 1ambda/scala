
### Hot and Cold Observable

http://www.introtorx.com/content/v1.0.10621.0/14_HotAndColdObservables.html

```cs
public void SimpleColdSample() {
  var period = TimeSpan.FromSeconds(1);
  var observable = Observable.Interval(period);
  observable.Subscribe(i => Console.WriteLine("first subscription : {0}", i));
  Thread.Sleep(period);
  observable.Subscribe(i => Console.WriteLine("second subscription : {0}", i));
  Console.ReadKey();
  /* Output:
  first subscription : 0
  first subscription : 1
  second subscription : 0
  first subscription : 2
  second subscription : 1
  first subscription : 3
  second subscription : 2
  */
}
```

If we want to be able to share the actual data values and not just the observable instance, we can use the `Publish` which return `IConnectableObservatle` that extends `Observable` by adding a single `Connect` method.

```cs
var period = TimeSpan.FromSeconds(1);
var observable = Observable.Interval(period).Publish();
observable.Connect();
observable.Subscribe(i => Console.WriteLine("first subscription : {0}", i));
Thread.Sleep(period);
observable.Subscribe(i => Console.WriteLine("second subscription : {0}", i));
```

By calling `Connect()`, `ConnectableObservable` will subsctibe to the underlying `Observable.Interval`.


### Connectable Observable Operators

```scala
def firstMillion  = Observable.range( 1, 1000000 ).sample(7, java.util.concurrent.TimeUnit.MILLISECONDS);

firstMillion.subscribe(
   { println("Subscriber #1:" + it); },       // onNext
   { println("Error: " + it.getMessage()); }, // onError
   { println("Sequence #1 complete"); }       // onCompleted
);

firstMillion.subscribe(
    { println("Subscriber #2:" + it); },       // onNext
    { println("Error: " + it.getMessage()); }, // onError
    { println("Sequence #2 complete"); }       // onCompleted
);

// output
Subscriber #1:211128
Subscriber #1:411633
Subscriber #1:629605
Subscriber #1:841903
Sequence #1 complete
Subscriber #2:244776
Subscriber #2:431416
Subscriber #2:621647
Subscriber #2:826996
Sequence #2 complete
```

```scala
def firstMillion  = Observable.range( 1, 1000000 ).sample(7, java.util.concurrent.TimeUnit.MILLISECONDS).publish();

firstMillion.subscribe(
   { println("Subscriber #1:" + it); },       // onNext
   { println("Error: " + it.getMessage()); }, // onError
   { println("Sequence #1 complete"); }       // onCompleted
);

firstMillion.subscribe(
   { println("Subscriber #2:" + it); },       // onNext
   { println("Error: " + it.getMessage()); }, // onError
   { println("Sequence #2 complete"); }       // onCompleted
);

firstMillion.connect();

// output
Subscriber #2:208683
Subscriber #1:208683
Subscriber #2:432509
Subscriber #1:432509
Subscriber #2:644270
Subscriber #1:644270
Subscriber #2:887885
Subscriber #1:887885
Sequence #2 complete
Sequence #1 complete
```

### Mixing cold and got

http://nullzzz.blogspot.kr/2012/01/things-you-should-know-about-rx.html

```cs
var hotness = $(document).toObservable("keyup")
var temperature = Observable.FromArray("coldness").Concat(hotness.Select(always("hotness")))
```

> You might expect to get an observable starting with "coldness" and producing "hotness" at each keyup. However, StartWith made your new observable a bit colder in the sense that any new subscriber will always get "coldness" first.

> No matter how you combine coldness with hotness, you won't get a hot Observable back. It won't be cold as in "tree falling in the woods", but inconsistent anyway. Lukewarm? No, more like Groundhog Day.

Using `StartWith` won't save you. It's the same thing as concatenating with a cold stream of one event.


### To Use Subject Or Not To Use Subject?

http://davesexton.com/blog/post/To-Use-Subject-Or-Not-To-Use-Subject.aspx
