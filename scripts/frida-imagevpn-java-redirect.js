// Diagnostic only: two local HTTP endpoints, no external destinations or image URLs.
Java.perform(function () {
  var Server = Java.use('java.net.ServerSocket');
  var Inet = Java.use('java.net.InetAddress');
  var JString = Java.use('java.lang.String');
  var H = Java.use('app.morphe.extension.chmate.HttpsTransport');
  var C = Java.use('app.morphe.extension.chmate.ImageVpnController');
  if (H.shouldUpgrade('http')) { console.log('SKIP: force-HTTPS enabled; configuration unchanged'); return; }
  var source = Server.$new(0, 1, Inet.getByName('127.0.0.1'));
  var destination = Server.$new(0, 1, Inet.getByName('127.0.0.1'));
  source.setSoTimeout(5000); destination.setSoTimeout(5000);
  var sourcePort = source.getLocalPort(), destinationPort = destination.getLocalPort();
  var routes = 0, direct = 0;
  var route = C.route.overload('java.net.URL');
  route.implementation = function (url) {
    if (url.getHost().toString() === '127.0.0.1'
        && (url.getPort() === sourcePort || url.getPort() === destinationPort)) {
      routes++;
      // Synthetic protected destination. Abort only this loopback fixture, before DNS/connect.
      if (url.getPort() === destinationPort)
        throw Java.use('java.io.IOException').$new('TEST_PROTECTED_DESTINATION_BLOCKED');
    }
    return route.call(this, url);
  };
  function serve(server, redirect) {
    var socket = null;
    try {
      socket = server.accept(); socket.setSoTimeout(2000);
      var reader = Java.use('java.io.BufferedReader').$new(
        Java.use('java.io.InputStreamReader').$new(socket.getInputStream(), 'US-ASCII'));
      for (var i = 0; i < 64; i++) { var line = reader.readLine(); if (line === null || line.toString() === '') break; }
      var response;
      if (redirect) response = 'HTTP/1.1 302 Found\r\nLocation: http://127.0.0.1:' + destinationPort
          + '/image\r\nContent-Length: 0\r\nConnection: close\r\n\r\n';
      else { direct++; response = 'HTTP/1.1 200 OK\r\nContent-Length: 2\r\nConnection: close\r\n\r\nok'; }
      socket.getOutputStream().write(JString.$new(response).getBytes('US-ASCII'));
      socket.getOutputStream().flush();
    } catch (e) { console.log('loopback server: ' + e); }
    finally { if (socket) socket.close(); server.close(); }
  }
  var Runnable = Java.use('java.lang.Runnable');
  var SourceWorker = Java.registerClass({ name: 'app.haiagaru.diagnostic.LocalRedirectSource',
    implements: [Runnable], methods: { run: function () { serve(source, true); } } });
  var DestinationWorker = Java.registerClass({ name: 'app.haiagaru.diagnostic.LocalRedirectDestination',
    implements: [Runnable], methods: { run: function () { serve(destination, false); } } });
  var ClientWorker = Java.registerClass({ name: 'app.haiagaru.diagnostic.LocalRedirectClient',
    implements: [Runnable], methods: { run: function () {
      var connection = null;
      try {
        connection = Java.cast(H.openConnection.overload('java.net.URL').call(H,
          Java.use('java.net.URL').$new('http://127.0.0.1:' + sourcePort + '/start')),
          Java.use('java.net.HttpURLConnection'));
        connection.setConnectTimeout(2000); connection.setReadTimeout(2000);
        connection.setInstanceFollowRedirects(true);
        var input = connection.getInputStream(); while (input.read() !== -1) {} input.close();
      } catch (e) { console.log('loopback client: ' + e); }
      finally {
        if (connection) connection.disconnect(); source.close(); destination.close();
        console.log('ANDROID loopback route callbacks=' + routes + ', direct destination requests=' + direct);
        console.log(direct === 0 && routes === 2 ? 'PASS: guarded redirect consulted routing and blocked synthetic target'
          : direct > 0 && routes === 1 ? 'FAIL: java.net automatic redirect bypasses route callback'
          : 'INCONCLUSIVE: inspect counters/errors before drawing conclusions');
      }
    } } });
  var Thread = Java.use('java.lang.Thread');
  Thread.$new(SourceWorker.$new()).start(); Thread.$new(DestinationWorker.$new()).start();
  Thread.$new(ClientWorker.$new()).start();
});
