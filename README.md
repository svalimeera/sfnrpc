# sfnrpc
Simple fast nio rpc (SFNRPC) in Java. Tested on linux and windows platforms for high throughput.

<hr size="1"/>

Download
sfnrpc-nio-1.0.0.jar
https://code.google.com/p/sfnrpc/source/browse/download_release/

dependency jars
https://code.google.com/p/sfnrpc/source/browse/dependency_libs/

Build
Checkout and run maven install Code is at: https://code.google.com/p/sfnrpc/source/browse

Dependencies
a) commons-pool-1.6.jar b) logback-classic-1.0.13.jar c) logback-core-1.0.13.jar d) slf4j-api-1.7.5.jar

<hr size="1"/>

Sample Usage
Logging
private Slog sl = Slog.getSlog(this.getClass.getName());

Server
```
//create rpcserver inside your singleton 
RPCServer rpcServer = new RPCServer(sl); 
rpcServer.publish("#URN1",TestService.getInstance()); 
rpcServer.startServer("127.0.0.1",6878,rpcClient - or - null); 
//when you pass a rpcclient, sfnrpc bypasses tcp when client calls any urn that is present locally.
```

Client
``` 
 RPCClient rpcc = null; 
 private TestClient(){
  //singleton rpcc = new RPCClient(sl); rpcc.start(); 
 } 
 public void invoke(){ 
  Object [] objArr = new Object[1]; 
  TestObject to = new TestObject(); 
  to.data = "HELLO,"; objArr[0] = to; 
  Class [] classArr = new Class[1]; 
  classArr[0] = TestObject.class; 
  TestObject response = (TestObject)rpcc.invoke("#URN1","127.0.0.1:6878","echo",true,60,"", objArr,classArr); 
  sl.debug("The response was:"+response.data); 
}

```

Quick start test code at:
sfnrpc/source/browse/src/main/test/sfn/core/rpc/test

Issues

a) Ensure all objects transferred implement java.io.Serializable. 

b) Open a issue for support.
