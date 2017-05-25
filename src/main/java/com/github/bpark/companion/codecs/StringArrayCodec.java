/*
 * Copyright 2017 Kurt Sparber
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.bpark.companion.codecs;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.Json;

public class StringArrayCodec implements MessageCodec<String[], String[]> {

    @Override
    public void encodeToWire(Buffer buffer, String[] message) {

        String messageString = Json.encode(message);

        int length = messageString.getBytes().length;

        buffer.appendInt(length);
        buffer.appendString(messageString);
    }

    @Override
    public String[] decodeFromWire(int position, Buffer buffer) {
        int pos = position;

        int length = buffer.getInt(pos);

        // Get JSON string by it`s length
        // Jump 4 because getInt() == 4 bytes
        String jsonStr = buffer.getString(pos+=4, pos+length);

        return Json.decodeValue(jsonStr, String[].class);
    }

    @Override
    public String[] transform(String[] message) {
        return message;
    }

    @Override
    public String name() {
        return this.getClass().getSimpleName();
    }

    @Override
    public byte systemCodecID() {
        return -1;
    }
}
