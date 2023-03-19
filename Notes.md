1. Why use case classes for each operations (eg: FlatMap).
Ans: To make it stack safe. If you use functions, you'll soon run out of stack space and will get a stackOverFlow exception. By keeping it as a class, it becomes stack safe.

2. The Async class is a bit weird. You can manipulate the return type and make it do something else and it will still work at runtime.