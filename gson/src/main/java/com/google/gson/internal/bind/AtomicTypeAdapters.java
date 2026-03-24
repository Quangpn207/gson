package com.google.gson.internal.bind;

import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

public final class AtomicTypeAdapters {
  private AtomicTypeAdapters() {}

  public static final TypeAdapter<AtomicInteger> ATOMIC_INTEGER =
      new TypeAdapter<AtomicInteger>() {
        @Override
        public AtomicInteger read(JsonReader in) throws IOException {
          try {
            return new AtomicInteger(in.nextInt());
          } catch (NumberFormatException e) {
            throw new JsonSyntaxException(e);
          }
        }
        @Override
        public void write(JsonWriter out, AtomicInteger value) throws IOException {
          out.value(value.get());
        }
      }.nullSafe();

  public static final TypeAdapterFactory ATOMIC_INTEGER_FACTORY =
      TypeAdapters.newFactory(AtomicInteger.class, AtomicTypeAdapters.ATOMIC_INTEGER);

  public static TypeAdapter<AtomicLong> atomicLongAdapter(TypeAdapter<Number> longAdapter) {
    return new TypeAdapter<AtomicLong>() {
      @Override
      public AtomicLong read(JsonReader in) throws IOException {
        Number value = longAdapter.read(in);
        return new AtomicLong(value.longValue());
      }
      @Override
      public void write(JsonWriter out, AtomicLong value) throws IOException {
        longAdapter.write(out, value.get());
      }
    }.nullSafe();
  }

  public static final TypeAdapter<AtomicBoolean> ATOMIC_BOOLEAN =
      new TypeAdapter<AtomicBoolean>() {
        @Override
        public AtomicBoolean read(JsonReader in) throws IOException {
          return new AtomicBoolean(in.nextBoolean());
        }
        @Override
        public void write(JsonWriter out, AtomicBoolean value) throws IOException {
          out.value(value.get());
        }
      }.nullSafe();

  public static final TypeAdapterFactory ATOMIC_BOOLEAN_FACTORY =
      TypeAdapters.newFactory(AtomicBoolean.class, AtomicTypeAdapters.ATOMIC_BOOLEAN);

  public static final TypeAdapter<AtomicIntegerArray> ATOMIC_INTEGER_ARRAY =
      new TypeAdapter<AtomicIntegerArray>() {
        @Override
        public AtomicIntegerArray read(JsonReader in) throws IOException {
          List<Integer> list = new ArrayList<>();
          in.beginArray();
          while (in.hasNext()) {
            try {
              int integer = in.nextInt();
              list.add(integer);
            } catch (NumberFormatException e) {
              throw new JsonSyntaxException(e);
            }
          }
          in.endArray();
          int length = list.size();
          AtomicIntegerArray array = new AtomicIntegerArray(length);
          for (int i = 0; i < length; ++i) {
            array.set(i, list.get(i));
          }
          return array;
        }
        @Override
        public void write(JsonWriter out, AtomicIntegerArray value) throws IOException {
          out.beginArray();
          for (int i = 0, length = value.length(); i < length; i++) {
            out.value(value.get(i));
          }
          out.endArray();
        }
      }.nullSafe();

  public static final TypeAdapterFactory ATOMIC_INTEGER_ARRAY_FACTORY =
      TypeAdapters.newFactory(AtomicIntegerArray.class, AtomicTypeAdapters.ATOMIC_INTEGER_ARRAY);

  public static TypeAdapter<AtomicLongArray> atomicLongArrayAdapter(
      TypeAdapter<Number> longAdapter) {
    return new TypeAdapter<AtomicLongArray>() {
      @Override
      public AtomicLongArray read(JsonReader in) throws IOException {
        List<Long> list = new ArrayList<>();
        in.beginArray();
        while (in.hasNext()) {
          long value = longAdapter.read(in).longValue();
          list.add(value);
        }
        in.endArray();
        int length = list.size();
        AtomicLongArray array = new AtomicLongArray(length);
        for (int i = 0; i < length; ++i) {
          array.set(i, list.get(i));
        }
        return array;
      }
      @Override
      public void write(JsonWriter out, AtomicLongArray value) throws IOException {
        out.beginArray();
        for (int i = 0, length = value.length(); i < length; i++) {
          longAdapter.write(out, value.get(i));
        }
        out.endArray();
      }
    }.nullSafe();
  }
}
