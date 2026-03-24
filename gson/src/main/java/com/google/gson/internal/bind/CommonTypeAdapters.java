/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.gson.internal.bind;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Currency;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.UUID;

/**
 * Type adapters for common non-numeric types (strings, booleans, and standard library types).
 * Extracted from {@link TypeAdapters} to reduce class size and improve cohesion.
 */
public final class CommonTypeAdapters {
  private CommonTypeAdapters() {
    throw new UnsupportedOperationException();
  }

  public static final TypeAdapter<Boolean> BOOLEAN =
      new TypeAdapter<Boolean>() {
        @Override
        public Boolean read(JsonReader in) throws IOException {
          JsonToken peek = in.peek();
          if (peek == JsonToken.NULL) {
            in.nextNull();
            return null;
          } else if (peek == JsonToken.STRING) {
            // support strings for compatibility with GSON 1.7
            return Boolean.parseBoolean(in.nextString());
          }
          return in.nextBoolean();
        }

        @Override
        public void write(JsonWriter out, Boolean value) throws IOException {
          out.value(value);
        }
      };

  /**
   * Writes a boolean as a string. Useful for map keys, where booleans aren't otherwise permitted.
   */
  public static final TypeAdapter<Boolean> BOOLEAN_AS_STRING =
      new TypeAdapter<Boolean>() {
        @Override
        public Boolean read(JsonReader in) throws IOException {
          if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
          }
          return Boolean.valueOf(in.nextString());
        }

        @Override
        public void write(JsonWriter out, Boolean value) throws IOException {
          out.value(value == null ? "null" : value.toString());
        }
      };

  public static final TypeAdapterFactory BOOLEAN_FACTORY =
      TypeAdapters.newFactory(boolean.class, Boolean.class, BOOLEAN);

  public static final TypeAdapter<Character> CHARACTER =
      new TypeAdapter<Character>() {
        @Override
        public Character read(JsonReader in) throws IOException {
          if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
          }
          String str = in.nextString();
          if (str.length() != 1) {
            throw new JsonSyntaxException(
                "Expecting character, got: " + str + "; at " + in.getPreviousPath());
          }
          return str.charAt(0);
        }

        @Override
        public void write(JsonWriter out, Character value) throws IOException {
          out.value(value == null ? null : String.valueOf(value));
        }
      };

  public static final TypeAdapterFactory CHARACTER_FACTORY =
      TypeAdapters.newFactory(char.class, Character.class, CHARACTER);

  public static final TypeAdapter<String> STRING =
      new TypeAdapter<String>() {
        @Override
        public String read(JsonReader in) throws IOException {
          JsonToken peek = in.peek();
          if (peek == JsonToken.NULL) {
            in.nextNull();
            return null;
          }
          /* coerce booleans to strings for backwards compatibility */
          if (peek == JsonToken.BOOLEAN) {
            return Boolean.toString(in.nextBoolean());
          }
          return in.nextString();
        }

        @Override
        public void write(JsonWriter out, String value) throws IOException {
          out.value(value);
        }
      };

  public static final TypeAdapterFactory STRING_FACTORY =
      TypeAdapters.newFactory(String.class, STRING);

  public static final TypeAdapter<StringBuilder> STRING_BUILDER =
      new TypeAdapter<StringBuilder>() {
        @Override
        public StringBuilder read(JsonReader in) throws IOException {
          if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
          }
          return new StringBuilder(in.nextString());
        }

        @Override
        public void write(JsonWriter out, StringBuilder value) throws IOException {
          out.value(value == null ? null : value.toString());
        }
      };

  public static final TypeAdapterFactory STRING_BUILDER_FACTORY =
      TypeAdapters.newFactory(StringBuilder.class, STRING_BUILDER);

  public static final TypeAdapter<StringBuffer> STRING_BUFFER =
      new TypeAdapter<StringBuffer>() {
        @Override
        public StringBuffer read(JsonReader in) throws IOException {
          if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
          }
          return new StringBuffer(in.nextString());
        }

        @Override
        public void write(JsonWriter out, StringBuffer value) throws IOException {
          out.value(value == null ? null : value.toString());
        }
      };

  public static final TypeAdapterFactory STRING_BUFFER_FACTORY =
      TypeAdapters.newFactory(StringBuffer.class, STRING_BUFFER);

  public static final TypeAdapter<URL> URL =
      new TypeAdapter<URL>() {
        @Override
        public URL read(JsonReader in) throws IOException {
          if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
          }
          String nextString = in.nextString();
          return nextString.equals("null") ? null : new URL(nextString);
        }

        @Override
        public void write(JsonWriter out, URL value) throws IOException {
          out.value(value == null ? null : value.toExternalForm());
        }
      };

  public static final TypeAdapterFactory URL_FACTORY =
      TypeAdapters.newFactory(java.net.URL.class, URL);

  public static final TypeAdapter<URI> URI =
      new TypeAdapter<URI>() {
        @Override
        public URI read(JsonReader in) throws IOException {
          if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
          }
          try {
            String nextString = in.nextString();
            return nextString.equals("null") ? null : new URI(nextString);
          } catch (URISyntaxException e) {
            throw new JsonIOException(e);
          }
        }

        @Override
        public void write(JsonWriter out, URI value) throws IOException {
          out.value(value == null ? null : value.toASCIIString());
        }
      };

  public static final TypeAdapterFactory URI_FACTORY =
      TypeAdapters.newFactory(java.net.URI.class, URI);

  public static final TypeAdapter<InetAddress> INET_ADDRESS =
      new TypeAdapter<InetAddress>() {
        @Override
        public InetAddress read(JsonReader in) throws IOException {
          if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
          }
          // regrettably, this should have included both the host name and the host address
          // For compatibility, we use InetAddress.getByName rather than the possibly-better
          // .getAllByName
          @SuppressWarnings("AddressSelection")
          InetAddress addr = InetAddress.getByName(in.nextString());
          return addr;
        }

        @Override
        public void write(JsonWriter out, InetAddress value) throws IOException {
          out.value(value == null ? null : value.getHostAddress());
        }
      };

  public static final TypeAdapterFactory INET_ADDRESS_FACTORY =
      TypeAdapters.newTypeHierarchyFactory(InetAddress.class, INET_ADDRESS);

  public static final TypeAdapter<UUID> UUID =
      new TypeAdapter<UUID>() {
        @Override
        public UUID read(JsonReader in) throws IOException {
          if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
          }
          String s = in.nextString();
          try {
            return java.util.UUID.fromString(s);
          } catch (IllegalArgumentException e) {
            throw new JsonSyntaxException(
                "Failed parsing '" + s + "' as UUID; at path " + in.getPreviousPath(), e);
          }
        }

        @Override
        public void write(JsonWriter out, UUID value) throws IOException {
          out.value(value == null ? null : value.toString());
        }
      };

  public static final TypeAdapterFactory UUID_FACTORY =
      TypeAdapters.newFactory(java.util.UUID.class, UUID);

  public static final TypeAdapter<Currency> CURRENCY =
      new TypeAdapter<Currency>() {
        @Override
        public Currency read(JsonReader in) throws IOException {
          String s = in.nextString();
          try {
            return Currency.getInstance(s);
          } catch (IllegalArgumentException e) {
            throw new JsonSyntaxException(
                "Failed parsing '" + s + "' as Currency; at path " + in.getPreviousPath(), e);
          }
        }

        @Override
        public void write(JsonWriter out, Currency value) throws IOException {
          out.value(value.getCurrencyCode());
        }
      }.nullSafe();

  public static final TypeAdapterFactory CURRENCY_FACTORY =
      TypeAdapters.newFactory(Currency.class, CURRENCY);

  public static final TypeAdapter<Locale> LOCALE =
      new TypeAdapter<Locale>() {
        @Override
        public Locale read(JsonReader in) throws IOException {
          if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
          }
          String locale = in.nextString();
          StringTokenizer tokenizer = new StringTokenizer(locale, "_");
          String language = null;
          String country = null;
          String variant = null;
          if (tokenizer.hasMoreElements()) {
            language = tokenizer.nextToken();
          }
          if (tokenizer.hasMoreElements()) {
            country = tokenizer.nextToken();
          }
          if (tokenizer.hasMoreElements()) {
            variant = tokenizer.nextToken();
          }
          if (country == null && variant == null) {
            return new Locale(language);
          } else if (variant == null) {
            return new Locale(language, country);
          } else {
            return new Locale(language, country, variant);
          }
        }

        @Override
        public void write(JsonWriter out, Locale value) throws IOException {
          out.value(value == null ? null : value.toString());
        }
      };

  public static final TypeAdapterFactory LOCALE_FACTORY =
      TypeAdapters.newFactory(Locale.class, LOCALE);
}
