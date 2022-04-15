package io.github.kustosz.ponzi;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

sealed public interface List<T> {
  Empty<?> EMPTY = new Empty<>();

  record Cons<T>(T head, List<T> tail) implements List<T> {
    @Override
    public List<T> prepend(T elem) {
      return new Cons<>(elem, this);
    }

    @Override
    public List<T> append(T elem) {
      return new Cons<>(head, tail.append(elem));
    }

    @Override
    public <U> List<U> map(Function<T, U> fn) {
      return new Cons<>(fn.apply(head()), tail().map(fn));
    }

    @Override
    public T get(int i) {
      if (i == 0) {
        return head;
      } else {
        return tail.get(i - 1);
      }
    }

    @Override
    public void forEach(Consumer<T> consumer) {
      List<T> current = this;
      while (current instanceof Cons<T> cons) {
        consumer.accept(cons.head);
        current = cons.tail;
      }
    }

    @Override
    public int size() {
      List<T> current = this;
      int i = 0;
      while (current instanceof Cons<T> cons) {
        current = cons.tail;
        i++;
      }
      return i;
    }
  }

  record Empty<T>() implements List<T> {
    @Override
    public List<T> prepend(T elem) {
      return new Cons<>(elem, this);
    }

    @Override
    public List<T> append(T elem) {
      return new Cons<>(elem, this);
    }

    @Override
    public <U> List<U> map(Function<T, U> fn) {
      return retype();
    }

    @Override
    public T get(int i) {
      throw new ArrayIndexOutOfBoundsException(i);
    }

    private <U> Empty<U> retype() {
      return (Empty<U>) this;
    }

    @Override
    public void forEach(Consumer<T> consumer) {
    }

    @Override
    public int size() {
      return 0;
    }
  }

  List<T> prepend(T elem);

  List<T> append(T elem);

  <U> List<U> map(Function<T, U> fn);

  T get(int i);

  int size();

  void forEach(Consumer<T> consumer);

  default Stream<T> stream() {
    Stream.Builder<T> bldr = Stream.builder();
    forEach(bldr::accept);
    return bldr.build();
  }

  default <S> List<Pair<T, S>> zip(List<S> that) {
    if (this instanceof Cons<T> lcons && that instanceof Cons<S> rcons) {
      return new Cons<>(new Pair<>(lcons.head, rcons.head), lcons.tail.zip(rcons.tail));
    } else {
      return empty();
    }
  }

  static <T> List<T> empty() {
    return EMPTY.retype();
  }

  static <T> List<T> of(T... values) {
    List<T> result = empty();
    for (int i = values.length - 1; i >= 0; i--) {
      result = new Cons<>(values[i], result);
    }
    return result;
  }

  static <T> List<T> of(java.util.List<T> values) {
    List<T> result = empty();
    for (int i = values.size() - 1; i >= 0; i--) {
      result = new Cons<>(values.get(i), result);
    }
    return result;
  }
}
