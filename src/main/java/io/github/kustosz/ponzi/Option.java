package io.github.kustosz.ponzi;

import java.util.function.Function;
import java.util.stream.Stream;

sealed public interface Option<T> {
  record None<T>() implements Option<T> {
    @Override
    public <U> None<U> map(Function<T, U> function) {
      return retype();
    }

    private <U> None<U> retype() {
      return (None<U>) this;
    }

    @Override
    public Stream<T> stream() {
      return Stream.empty();
    }

    @Override
    public T get() {
      throw new RuntimeException("No value.");
    }
  }

  record Some<T>(T value) implements Option<T> {
    @Override
    public <U> Some<U> map(Function<T, U> function) {
      return new Some<>(function.apply(value));
    }

    @Override
    public Stream<T> stream() {
      return Stream.of(value);
    }

    @Override
    public T get() {
      return value;
    }
  }

  static <T> Option<T> of(T value) {
    if (value == null) {
      return new None<>();
    } else {
      return new Some<>(value);
    }
  }

  Stream<T> stream();

  <U> Option<U> map(Function<T, U> function);

  T get();
}
