/*
 *    Copyright 2021 Andrea Peruffo
 *    Copyright 2021 Edoardo Vacchi
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

//JAVA 19
//JAVAC_OPTIONS --enable-preview --release 19
//JAVA_OPTIONS  --enable-preview
//SOURCES ChannelActors.java
//SOURCES ../actor/TypedActor.java

package io.github.evacchi.chat;

import io.github.evacchi.TypedActor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.util.ArrayList;

import static io.github.evacchi.TypedActor.*;
import static java.lang.System.*;

public interface ChatServer {
    record ClientConnected(Address addr) { }

    TypedActor.System system = new TypedActor.System();
    int PORT = 4444;

    static void main(String... args) throws IOException, InterruptedException {
        var serverSocket = new ServerSocket(PORT);
        out.printf("Server started at %s.\n", serverSocket.getLocalSocketAddress());

        var clientManager =
                system.actorOf(self -> clientManager());

        while (true) {
            var socket = serverSocket.accept();
            var channel = new ChannelActors(socket);
            system.actorOf(self -> channel.reader(self, clientManager));
            var writer = system.actorOf(self -> channel.writer());
            clientManager.tell(new ClientConnected(writer));
        }
    }

    static Behavior clientManager() {
        var clients = new ArrayList<Address>();
        return msg -> {
            switch (msg) {
                case ClientConnected(var address) -> clients.add(address);
                case ChannelActors.LineRead(var payload) ->
                    clients.forEach(client -> client.tell(new ChannelActors.WriteLine(payload)));
                default -> throw new RuntimeException("Unhandled message " + msg);
            }
            return Stay;
        };
    }

}
