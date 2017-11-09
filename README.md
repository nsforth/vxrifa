This library introduces concept of asynchronous object-oriented programming 'by contract' in Vert.X.<br>
Usually if you want to send message in Vert.X from one Actor(Verticle) to other you need to use <tt>eventBus.send</tt> or <tt>eventBus.publish</tt> with some object as payload.
Objects should be some sort of simple types like <tt>String</tt>,<tt>Integer</tt> or special objects like <tt>JsonObject</tt>.
Of course you can register own 'codec' and send anything you want but type checking on receiving side is a developer responsibility.
You should also write boiler plate for message handlers and handler registering.<br>
VxRifa library implements Java-idiomatic style for actors messaging based on Interfaces. 
## How to use library
Any interface can be annotated with <tt>@VxRifa</tt>. Methods should return void or <tt>io.vertx.core.Future</tt>.
Whenever java compiler processes annotations (when building project or on the fly in modern IDEs) VxRifa generates special classes that do all work.
For example:
```java
@VxRifa
public interface Calculator {
 
    Future<Double> sumTwoDoubles(Double one, Double two);
  
    void reset();

}
```
Implementation in Verticle that exporting such API would look like that:
```java
public class CalculatorImpl implements Calculator {
 
    @Override
    public Future<Double> sumTwoDoubles(Double one, Double two) {
        Future<Double> future = Future.future();
        future.complete(one + two);
        return future;
    }
   
    @Override
    public void reset() {
          
        // Some reset actions 
 
   }

}
```
Now you can get <tt>VxRifaReceiver</tt> with <tt>VxRifaUtil.getReceiverRegistrator</tt>
which creates any needed eventBus.consumer's and calls methods of <tt>Calculator</tt> whenever it receives messages from other actors.<br>
Other Verticles should use <tt>VxRifaUtil.getSenderByInterface</tt> that returns VxRifa-driven implementation of Calculator which send messages under the hood.
For example:
```java
.... CalculatorVerticle ....

Calculator calc_impl = new CalculatorImpl();
VxRifaReceiver<Calculator> registrator = VxRifaUtil.getReceiverRegistrator(vertx, Calculator.class);
Future<?> when_registered = registrator.registerReceiver(calc_impl);
 
.... Some other verticles that wants to use Calculator API ....

Calculator calc = VxRifaUtil.getSenderByInterface(vertx, Calculator.class);
calc.sumTwoDoubles(1.0, 2.0).setHandler(result -> {
    if (result.succeeded()) {
        Double sum = result.result();
    }
});
```
## Notes and limitations 
There is one small thing that should be done before using VxRifa. You must call <tt>VxRifaUtil.registerRIFACodec</tt> once for any instance of Vert.x.
VxRifa uses wrapper as container for methods parameters so that wrapper should be registered before any sending by eventBus.<br>
You should also remember that any parameters and returned objects should be immutable(effectively immutable) or at least thread-safe.
Currently messaging by VxRifa only possible for local non-clustered Vert.X instances because <tt>RIFAMessageCodec</tt> not supported network encoding/decoding of objects.
I hope to solve that in near future.
