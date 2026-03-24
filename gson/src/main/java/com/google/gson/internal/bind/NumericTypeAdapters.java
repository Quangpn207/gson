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

import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.LazilyParsedNumber;
import com.google.gson.internal.NumberLimits;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Type adapters for numeric types. Extracted from {@link TypeAdapters} to reduce class size and
 * improve cohesion.
 */
public final class NumericTypeAdapters {
  private NumericTypeAdapters() {
    throw new UnsupportedOperationException();
  }

  public static final TypeAdapter<Number> BYTE =
      new TypeAdapter<Number>() {
        @Override
        public Number read(JsonReader in) throws IOException {
          if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
          }

          int intValue;
          try {
            intValue = in.nextInt();
          } catch (NumberFormatException e) {
            throw new JsonSyntaxException(e);
          }
          // Allow up to 255 to support unsigned values
          if (intValue > 255 || intValue < Byte.MIN_VALUE) {
            throw new JsonSyntaxException(
                "Lossy conversion from " + intValue + " to byte; at path " + in.getPreviousPath());
          }
          return (byte) intValue;
        }

        @Override
        public void write(JsonWriter out, Number value) throws IOException {
          if (value == null) {
            out.nullValue();
          } else {
            out.value(value.byteValue());
          }
        }
      };

  public static final TypeAdapterFactory BYTE_FACTORY =
      TypeAdapters.newFactory(byte.class, Byte.class, BYTE);

  public static final TypeAdapter<Number> SHORT =
      new TypeAdapter<Number>() {
        @Override
        public Number read(JsonReader in) throws IOException {
          if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
          }

          int intValue;
          try {
            intValue = in.nextInt();
          } catch (NumberFormatException e) {
            throw new JsonSyntaxException(e);
          }
          // Allow up to 65535 to support unsigned values
          if (intValue > 65535 || intValue < Short.MIN_VALUE) {
            throw new JsonSyntaxException(
                "Lossy conversion from "
                    + intValue
                    + " to short; at path "
                    + in.getPreviousPath());
          }
          return (short) intValue;
        }

        @Override
        public void write(JsonWriter out, Number value) throws IOException {
          if (value == null) {
            out.nullValue();
          } else {
            out.value(value.shortValue());
          }
        }
      };

  public static final TypeAdapterFactory SHORT_FACTORY =
      TypeAdapters.newFactory(short.class, Short.class, SHORT);

  public static final TypeAdapter<Number> INTEGER =
      new TypeAdapter<Number>() {
        @Override
        public Number read(JsonReader in) throws IOException {
          if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
          }
          try {
            return in.nextInt();
          } catch (NumberFormatException e) {
            throw new JsonSyntaxException(e);
          }
        }

        @Override
        public void write(JsonWriter out, Number value) throws IOException {
          if (value == null) {
            out.nullValue();
          } else {
            out.value(value.intValue());
          }
        }
      };

  public static final TypeAdapterFactory INTEGER_FACTORY =
      TypeAdapters.newFactory(int.class, Integer.class, INTEGER);

  public static final TypeAdapter<Number> LONG =
      new TypeAdapter<Number>() {
        @Override
        public Number read(JsonReader in) throws IOException {
          if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
          }
          try {
            return in.nextLong();
          } catch (NumberFormatException e) {
            throw new JsonSyntaxException(e);
          }
        }

        @Override
        public void write(JsonWriter out, Number value) throws IOException {
          if (value == null) {
            out.nullValue();
          } else {
            out.value(value.longValue());
          }
        }
      };

  public static final TypeAdapter<Number> LONG_AS_STRING =
      new TypeAdapter<Number>() {
        @Override
        public Number read(JsonReader in) throws IOException {
          if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
          }
          return in.nextLong();
        }

        @Override
        public void write(JsonWriter out, Number value) throws IOException {
          if (value == null) {
            out.nullValue();
            return;
          }
          out.value(value.toString());
        }
      };

  private static class FloatAdapter extends TypeAdapter<Number> {
    private final boolean strict;

    FloatAdapter(boolean strict) {
      this.strict = strict;
    }

    @Override
    public Float read(JsonReader in) throws IOException {
      if (in.peek() == JsonToken.NULL) {
        in.nextNull();
        return null;
      }
      return (float) in.nextDouble();
    }

    @Override
    public void write(JsonWriter out, Number value) throws IOException {
      if (value == null) {
        out.nullValue();
        return;
      }
      float floatValue = value.floatValue();
      if (strict) {
        checkValidFloatingPoint(floatValue);
      }
      // For backward compatibility don't call `JsonWriter.value(float)` because that method has
      // been newly added and not all custom JsonWriter implementations might override it yet
      Number floatNumber = value instanceof Float ? value : floatValue;
      out.value(floatNumber);
    }
  }

  private static class DoubleAdapter extends TypeAdapter<Number> {
    private final boolean strict;

    DoubleAdapter(boolean strict) {
      this.strict = strict;
    }

    @Override
    public Double read(JsonReader in) throws IOException {
      if (in.peek() == JsonToken.NULL) {
        in.nextNull();
        return null;
      }
      return in.nextDouble();
    }

    @Override
    public void write(JsonWriter out, Number value) throws IOException {
      if (value == null) {
        out.nullValue();
        return;
      }
      double doubleValue = value.doubleValue();
      if (strict) {
        checkValidFloatingPoint(doubleValue);
      }
      out.value(doubleValue);
    }
  }

  private static void checkValidFloatingPoint(double value) {
    if (Double.isNaN(value) || Double.isInfinite(value)) {
      throw new IllegalArgumentException(
          value
              + " is not a valid double value as per JSON specification. To override this"
              + " behavior, use GsonBuilder.serializeSpecialFloatingPointValues() method.");
    }
  }

  public static final TypeAdapter<Number> FLOAT = new FloatAdapter(false);
  public static final TypeAdapter<Number> FLOAT_STRICT = new FloatAdapter(true);

  public static final TypeAdapter<Number> DOUBLE = new DoubleAdapter(false);
  public static final TypeAdapter<Number> DOUBLE_STRICT = new DoubleAdapter(true);

  public static final TypeAdapter<BigDecimal> BIG_DECIMAL =
      new TypeAdapter<BigDecimal>() {
        @Override
        public BigDecimal read(JsonReader in) throws IOException {
          if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
          }
          String s = in.nextString();
          try {
            return NumberLimits.parseBigDecimal(s);
          } catch (NumberFormatException e) {
            throw new JsonSyntaxException(
                "Failed parsing '" + s + "' as BigDecimal; at path " + in.getPreviousPath(), e);
          }
        }

        @Override
        public void write(JsonWriter out, BigDecimal value) throws IOException {
          out.value(value);
        }
      };

  public static final TypeAdapterFactory BIG_DECIMAL_FACTORY =
      TypeAdapters.newFactory(BigDecimal.class, BIG_DECIMAL);

  public static final TypeAdapter<BigInteger> BIG_INTEGER =
      new TypeAdapter<BigInteger>() {
        @Override
        public BigInteger read(JsonReader in) throws IOException {
          if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
          }
          String s = in.nextString();
          try {
            return NumberLimits.parseBigInteger(s);
          } catch (NumberFormatException e) {
            throw new JsonSyntaxException(
                "Failed parsing '" + s + "' as BigInteger; at path " + in.getPreviousPath(), e);
          }
        }

        @Override
        public void write(JsonWriter out, BigInteger value) throws IOException {
          out.value(value);
        }
      };

  public static final TypeAdapterFactory BIG_INTEGER_FACTORY =
      TypeAdapters.newFactory(BigInteger.class, BIG_INTEGER);

  public static final TypeAdapter<LazilyParsedNumber> LAZILY_PARSED_NUMBER =
      new TypeAdapter<LazilyParsedNumber>() {
        // Normally users should not be able to access and deserialize LazilyParsedNumber because
        // it is an internal type, but implement this nonetheless in case there are legit corner
        // cases where this is possible
        @Override
        public LazilyParsedNumber read(JsonReader in) throws IOException {
          if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
          }
          return new LazilyParsedNumber(in.nextString());
        }

        @Override
        public void write(JsonWriter out, LazilyParsedNumber value) throws IOException {
          out.value(value);
        }
      };

  public static final TypeAdapterFactory LAZILY_PARSED_NUMBER_FACTORY =
      TypeAdapters.newFactory(LazilyParsedNumber.class, LAZILY_PARSED_NUMBER);
}
