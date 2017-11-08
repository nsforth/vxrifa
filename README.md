# vxrifa
<h2>VxRIFA - Vert.X Rich Interfaces For Actors</h2><p>
This library introduces concept of asynchronous object-oriented programming 'by contract'.<p>
Usually if you want to send message in Vert.X from one Actor(Verticle) to other you need to use eventBus.send or eventBus.publish with some object as payload.
Objects should be some sort of simple types like String,Integer or special objects like JsonObject.<br>
Of course you can register own 'codec' and send anything but on receiving side type checking is a developer responsibility.<br>
You should also write boiler plate for message handlers and handler registering.<p>
VxRifa library implements Java-ideomatic style for actors messaging based on Interfaces. 
<h3>How to use library</h3>
Any interface can be annotated with {@link VxRifa}. Methods should return void or {@link io.vertx.core.Future} typed with expected result.
Whenever java compiler processes annotations (on building project or on the fly by modern IDE) VxRifa generates special classes that do all work.
For example:
<pre><code>
@VxRifa
public interface Calculator {
 
    Future&lt;Double&gt; sumTwoDoubles(Double one, Double two);
  
    void reset();
 
}
</code></pre>
Implementation in Verticle that exporting such API should be like that:
<pre><code>
public class CalculatorImpl implements Calculator {
 
    @Override
    public Future&lt;Double&gt; sumTwoDoubles(Double one, Double two) {
        Future&lt;Double&gt; future = Future.future();
        future.complete(one + two);
        return future;
    }
   
    @Override
    public void reset() {
          
        // Some reset actions 
 
   }

}
</code></pre><p>
Now you can get {@link VxRifaReceiver} with {@link VxRifaUtil#getReceiverRegistrator}
which creates by calling {@link VxRifaReceiver#registerReceiver} any needed eventBus.consumer and calls methods of Calculator whenever receives messages from other actors.<br>
Other Verticles should use {@link VxRifaUtil#getSenderByInterface} that returns VxRifa-driven implementation of Calculator which send messages on methods calling under the hood.
For example:
<pre><code>
.... CalculatorVerticle ....

Calculator calc_impl = new CalculatorImpl();
VxRifaReceiver&lt;Calculator&gt; registrator = VxRifaUtil.getReceiverRegistrator(vertx, Calculator.class);
Future<?> when_registered = registrator.registerReceiver(calc_impl);
 
.... Some other verticles that wants to use Calculator API ....

Calculator calc = VxRifaUtil.getSenderByInterface(vertx, Calculator.class);
calc.sumTwoDoubles(1.0, 2.0).setHandler(result -> {
    if (result.succeeded()) {
        Double sum = result.result();
    }
});
</code></pre>
<h3>Notes and limitations</h3>
There is one small thing that should be done before using VxRifa. You must call {@link VxRifaUtil#registerRIFACodec(io.vertx.core.Vertx) } once for any instance of Vert.x.<br>
VxRifa uses wrapper for encapsulation of methods parameters in your interfaces so it should be registered before any sending by eventBus.
You should also remember that any parameters and returned objects should be immutable(effectively immutable) or thread-safe somehow.<br>
Currently messaging by VxRifa only possible for local non-clustered Vert.X instances because RIFAMessageCodec not supported network encoding/decoding of objects.
I hope to solve that in near future.
