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

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.TroubleshootingGuide;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Type adapters for special types and factory helper methods. Basic numeric type adapters are in
 * {@link NumericTypeAdapters}, and common non-numeric type adapters are in {@link
 * CommonTypeAdapters}. Atomic type adapters are in {@link AtomicTypeAdapters}.
 */
public final class TypeAdapters {
  private TypeAdapters() {
    throw new UnsupportedOperationException();
  }

  @SuppressWarnings("rawtypes")
  public static final TypeAdapter<Class> CLASS =
      new TypeAdapter<Class>() {
        @Override
        public void write(JsonWriter out, Class value) throws IOException {
          throw new UnsupportedOperationException(
              "Attempted to serialize java.lang.Class: "
                  + value.getName()
                  + ". Forgot to register a type adapter?"
                  + "\nSee "
                  + TroubleshootingGuide.createUrl("java-lang-class-unsupported"));
        }

        @Override
        public Class read(JsonReader in) throws IOException {
          throw new UnsupportedOperationException(
              "Attempted to deserialize a java.lang.Class. Forgot to register a type adapter?"
                  + "\nSee "
                  + TroubleshootingGuide.createUrl("java-lang-class-unsupported"));
        }
      }.nullSafe();

  public static final TypeAdapterFactory CLASS_FACTORY = newFactory(Class.class, CLASS);

  public static final TypeAdapter<BitSet> BIT_SET =
      new TypeAdapter<BitSet>() {
        @Override
        public BitSet read(JsonReader in) throws IOException {
          BitSet bitset = new BitSet();
          in.beginArray();
          int i = 0;
          JsonToken tokenType = in.peek();
          while (tokenType != JsonToken.END_ARRAY) {
            boolean set;
            switch (tokenType) {
              case NUMBER:
              case STRING:
                int intValue = in.nextInt();
                if (intValue == 0) {
                  set = false;
                } else if (intValue == 1) {
                  set = true;
                } else {
                  throw new JsonSyntaxException(
                      "Invalid bitset value "
                          + intValue
                          + ", expected 0 or 1; at path "
                          + in.getPreviousPath());
                }
                break;
              case BOOLEAN:
                set = in.nextBoolean();
                break;
              default:
                throw new JsonSyntaxException(
                    "Invalid bitset value type: " + tokenType + "; at path " + in.getPath());
            }
            if (set) {
              bitset.set(i);
            }
            ++i;
            tokenType = in.peek();
          }
          in.endArray();
          return bitset;
        }

        @Override
        public void write(JsonWriter out, BitSet src) throws IOException {
          out.beginArray();
          for (int i = 0, length = src.length(); i < length; i++) {
            int value = src.get(i) ? 1 : 0;
            out.value(value);
          }
          out.endArray();
        }
      }.nullSafe();

  public static final TypeAdapterFactory BIT_SET_FACTORY = newFactory(BitSet.class, BIT_SET);

  /**
   * An abstract {@link TypeAdapter} for classes whose JSON serialization consists of a fixed set of
   * integer fields. That is the case for {@link Calendar} and the legacy serialization of various
   * {@code java.time} types.
   */
  abstract static class IntegerFieldsTypeAdapter<T> extends TypeAdapter<T> {
    private final List<String> fields;

    IntegerFieldsTypeAdapter(String... fields) {
      this.fields = Arrays.asList(fields);
    }

    abstract T create(long[] values);

    abstract long[] integerValues(T t);

    @Override
    public T read(JsonReader in) throws IOException {
      if (in.peek() == JsonToken.NULL) {
        in.nextNull();
        return null;
      }
      in.beginObject();
      long[] values = new long[fields.size()];
      while (in.peek() != JsonToken.END_OBJECT) {
        String name = in.nextName();
        int index = fields.indexOf(name);
        if (index >= 0) {
          values[index] = in.nextLong();
        } else {
          in.skipValue();
        }
      }
      in.endObject();
      return create(values);
    }

    @Override
    public void write(JsonWriter out, T value) throws IOException {
      if (value == null) {
        out.nullValue();
        return;
      }
      out.beginObject();
      long[] values = integerValues(value);
      for (int i = 0; i < fields.size(); i++) {
        out.name(fields.get(i));
        out.value(values[i]);
      }
      out.endObject();
    }
  }

  public static final TypeAdapter<Calendar> CALENDAR =
      new IntegerFieldsTypeAdapter<Calendar>(
          "year", "month", "dayOfMonth", "hourOfDay", "minute", "second") {

        @Override
        Calendar create(long[] values) {
          return new GregorianCalendar(
              toIntExact(values[0]),
              toIntExact(values[1]),
              toIntExact(values[2]),
              toIntExact(values[3]),
              toIntExact(values[4]),
              toIntExact(values[5]));
        }

        @Override
        long[] integerValues(Calendar calendar) {
          return new long[] {
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            calendar.get(Calendar.SECOND)
          };
        }
      };

  // TODO: update this when we are on at least Android API Level 24.
  private static int toIntExact(long x) {
    int i = (int) x;
    if (i != x) {
      throw new IllegalArgumentException("Too big for an int: " + x);
    }
    return i;
  }

  public static final TypeAdapterFactory CALENDAR_FACTORY =
      newFactoryForMultipleTypes(Calendar.class, GregorianCalendar.class, CALENDAR);

  public static final TypeAdapter<JsonElement> JSON_ELEMENT = JsonElementTypeAdapter.ADAPTER;

  public static final TypeAdapterFactory JSON_ELEMENT_FACTORY =
      newTypeHierarchyFactory(JsonElement.class, JSON_ELEMENT);

  public static final TypeAdapterFactory ENUM_FACTORY = EnumTypeAdapter.FACTORY;

  interface FactorySupplier {
    TypeAdapterFactory get();
  }

  public static TypeAdapterFactory javaTimeTypeAdapterFactory() {
    try {
      Class<?> javaTimeTypeAdapterFactoryClass =
          Class.forName("com.google.gson.internal.bind.JavaTimeTypeAdapters");
      FactorySupplier supplier =
          (FactorySupplier) javaTimeTypeAdapterFactoryClass.getDeclaredConstructor().newInstance();
      return supplier.get();
    } catch (ReflectiveOperationException | LinkageError e) {
      return null;
    }
  }

  @SuppressWarnings("TypeParameterNaming")
  public static <TT> TypeAdapterFactory newFactory(
      TypeToken<TT> type, TypeAdapter<TT> typeAdapter) {
    return new TypeAdapterFactory() {
      @SuppressWarnings("unchecked") // we use a runtime check to make sure the 'T's equal
      @Override
      public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
        return typeToken.equals(type) ? (TypeAdapter<T>) typeAdapter : null;
      }
    };
  }

  @SuppressWarnings("TypeParameterNaming")
  public static <TT> TypeAdapterFactory newFactory(Class<TT> type, TypeAdapter<TT> typeAdapter) {
    return new TypeAdapterFactory() {
      @SuppressWarnings("unchecked") // we use a runtime check to make sure the 'T's equal
      @Override
      public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
        return typeToken.getRawType() == type ? (TypeAdapter<T>) typeAdapter : null;
      }

      @Override
      public String toString() {
        return "Factory[type=" + type.getName() + ",adapter=" + typeAdapter + "]";
      }
    };
  }

  @SuppressWarnings("TypeParameterNaming")
  public static <TT> TypeAdapterFactory newFactory(
      Class<TT> unboxed, Class<TT> boxed, TypeAdapter<? super TT> typeAdapter) {
    return new TypeAdapterFactory() {
      @SuppressWarnings("unchecked") // we use a runtime check to make sure the 'T's equal
      @Override
      public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
        Class<? super T> rawType = typeToken.getRawType();
        return (rawType == unboxed || rawType == boxed) ? (TypeAdapter<T>) typeAdapter : null;
      }

      @Override
      public String toString() {
        return "Factory[type="
            + boxed.getName()
            + "+"
            + unboxed.getName()
            + ",adapter="
            + typeAdapter
            + "]";
      }
    };
  }

  @SuppressWarnings("TypeParameterNaming")
  public static <TT> TypeAdapterFactory newFactoryForMultipleTypes(
      Class<TT> base, Class<? extends TT> sub, TypeAdapter<? super TT> typeAdapter) {
    return new TypeAdapterFactory() {
      @SuppressWarnings("unchecked") // we use a runtime check to make sure the 'T's equal
      @Override
      public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
        Class<? super T> rawType = typeToken.getRawType();
        return (rawType == base || rawType == sub) ? (TypeAdapter<T>) typeAdapter : null;
      }

      @Override
      public String toString() {
        return "Factory[type="
            + base.getName()
            + "+"
            + sub.getName()
            + ",adapter="
            + typeAdapter
            + "]";
      }
    };
  }

  /**
   * Returns a factory for all subtypes of {@code typeAdapter}. We do a runtime check to confirm
   * that the deserialized type matches the type requested.
   */
  public static <T1> TypeAdapterFactory newTypeHierarchyFactory(
      Class<T1> clazz, TypeAdapter<T1> typeAdapter) {
    return new TypeAdapterFactory() {
      @SuppressWarnings("unchecked")
      @Override
      public <T2> TypeAdapter<T2> create(Gson gson, TypeToken<T2> typeToken) {
        Class<? super T2> requestedType = typeToken.getRawType();
        if (!clazz.isAssignableFrom(requestedType)) {
          return null;
        }
        return (TypeAdapter<T2>)
            new TypeAdapter<T1>() {
              @Override
              public void write(JsonWriter out, T1 value) throws IOException {
                typeAdapter.write(out, value);
              }

              @Override
              public T1 read(JsonReader in) throws IOException {
                T1 result = typeAdapter.read(in);
                if (result != null && !requestedType.isInstance(result)) {
                  throw new JsonSyntaxException(
                      "Expected a "
                          + requestedType.getName()
                          + " but was "
                          + result.getClass().getName()
                          + "; at path "
                          + in.getPreviousPath());
                }
                return result;
              }
            };
      }

      @Override
      public String toString() {
        return "Factory[typeHierarchy=" + clazz.getName() + ",adapter=" + typeAdapter + "]";
      }
    };
  }
}
