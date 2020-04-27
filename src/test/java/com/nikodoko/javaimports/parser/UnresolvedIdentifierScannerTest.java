package com.nikodoko.javaimports.parser;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.nikodoko.javaimports.ImporterException;
import java.util.Collection;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class UnresolvedIdentifierScannerTest {
  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> parameters() {
    String[][][] inputOutputs = {
      {
        // Test that we handle scoping correctly in methods
        {
          "package com.pkg.test;",
          "class Test {",
          "  public void g() {",
          "    int c = f(b);",
          "  }",
          "  public int f(int a) {",
          "    int b = 2;",
          "    return a + b;",
          "  }",
          "}",
        },
        {"b"},
      },
      {
        {
          // Test that we handle scoping correctly in for loops
          "class Test {",
          "  public void f() {",
          "    for (int i = 0; i < 10; i ++) {",
          "      int b = 2;",
          "      staticFunction(i + b);",
          "    }",
          "    int var = i + b;",
          "    boolean[] c = {true, false};",
          "    for (boolean d : c) {",
          "      boolean e = d;",
          "    }",
          "    boolean f = e || d;",
          "  }",
          "}",
        },
        {"staticFunction", "i", "b", "e", "d"},
      },
      {
        {
          // Test that we handle scoping correctly in if blocks
          "class Test {",
          "  public void f() {",
          "    if (true) {",
          "      int a = 2;",
          "      int b = 3;",
          "    } else {",
          "      int c = a;",
          "    }",
          "    int var = b + c;",
          "  }",
          "}",
        },
        {"a", "b", "c"},
      },
      {
        {
          // Test that we handle scoping correctly in while loops
          "class Test {",
          "  public void f() {",
          "    while (true) {",
          "      int a = 2;",
          "    }",
          "    int var = a;",
          "  }",
          "}",
        },
        {"a"},
      },
      {
        {
          // Test that we handle scoping correctly in synchronized blocks
          "class Test {",
          "  public void f() {",
          "    synchronized (this) {",
          "      int a = 2;",
          "    }",
          "    int var = a;",
          "  }",
          "}",
        },
        // UnresolvedIdentifierScanner does not know about this
        {"this", "a"},
      },
      {
        {
          // Test that we handle scoping correctly in do-while loops
          "class Test {",
          "  public void f() {",
          "    do {",
          "      int a = 2;",
          "    } while (true);",
          "    int var = a;",
          "  }",
          "}",
        },
        {"a"},
      },
      {
        {
          // Test that we handle annotations properly
          "class Test {", //
          "  @SomeAnnotation",
          "  public void f() {",
          "    return;",
          "  }",
          "}",
        },
        {"SomeAnnotation"},
      },
      {
        {
          // Test that we handle lambdas properly
          "class Test {", //
          "  public void f() {",
          "    int a = 1;",
          "    BiFunction<Integer, Integer, Integer> f = (b, c) -> a + b + c;",
          "    int d = f.apply(2, 3) + b;",
          "  }",
          "}",
        },
        {"b", "Integer", "BiFunction"},
      },
      {
        {
          // Test that we handle scoping correctly in switch blocks
          "class Test {",
          "  public void f() {",
          "    int a = 2;",
          "    switch (a) {",
          "    case 1:",
          "      int b = 2;",
          "      break;",
          "    case 2:",
          "      int c = b;",
          "      break;",
          "    }",
          "    int var = c;",
          "  }",
          "}",
        },
        {"c"},
      },
      {
        {
          // Test that we handle scoping correctly in try-catch-finally
          "class Test {",
          "  public void f() {",
          "    try {",
          "      int a = 1;",
          "    } catch (SomeException e) {",
          "      int b = e.getErrorCode();",
          "    } catch (Exception e) {",
          "      int c = a;",
          "    } finally {",
          "      int d = b;",
          "    }",
          "    int var = c + e;",
          "  }",
          "}",
        },
        {"SomeException", "Exception", "a", "b", "c", "e"},
      },
      {
        {
          // Test that we handle scoping correctly in try-catch-finally (with resource)
          "class Test {",
          "  public void f() {",
          "    try (int r = 1) {",
          "      int a = 1 + r;",
          "    } catch (SomeException e) {",
          "      int b = e.getErrorCode();",
          "    } catch (Exception e) {",
          "      int c = a + r;",
          "    } finally {",
          "      int d = b + r;",
          "    }",
          "    int var = c + e + r;",
          "  }",
          "}",
        },
        {"SomeException", "Exception", "a", "b", "c", "e", "r"},
      },
      {
        {
          // Test we handle tricky inheritence cases correctly
          "class Test {",
          "  static class OtherChild extends Child {",
          "    public private void m() {",
          "      int c = n(f() + g(0));",
          "    }",
          "  }",
          "  static class Child extends Parent {",
          "    void f() {",
          "      int c = g(a) + h(b);",
          "    }",
          "  }",
          "  static class Parent {",
          "    protected int a = 0;",
          "    public int p(int x) {",
          "      return x;",
          "    }",
          "    public int g(int x) {",
          "      int b = 5;",
          "      return x;",
          "    }",
          "    int h(int x) {",
          "      return x;",
          "    }",
          "  }",
          "}",
        },
        {"b", "n"},
      },
      {
        {
          // Test that we handle annotations properly
          "@Annotation(a=\"value\")",
          "class Test {",
          "  @Function",
          "  public void f() {",
          "    return 0;",
          "  }",
          "}",
        },
        {"Annotation", "Function"},
      },
      {
        {
          // Test that we handle generic types properly
          "class Test<R> {",
          "  public static <T> T f(T t) {",
          "    R var = null;",
          "    return t;",
          "  }",
          "}",
        },
        {},
      },
      {
        // "Realistic" test using a real file, courtesy of Google Guava
        {
          "/*",
          " * Copyright (C) 2007 The Guava Authors",
          " *",
          " * Licensed under the Apache License, Version 2.0 (the \"License\");",
          " * you may not use this file except in compliance with the License.",
          " * You may obtain a copy of the License at",
          " *",
          " *",
          " * Unless required by applicable law or agreed to in writing, software",
          " * distributed under the License is distributed on an \"AS IS\" BASIS,",
          " * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.",
          " * See the License for the specific language governing permissions and",
          " * limitations under the License.",
          " */",
          "",
          "package com.google.common.collect;",
          "",
          "import static com.google.common.base.Preconditions.checkArgument;",
          "import static com.google.common.base.Preconditions.checkNotNull;",
          "import static com.google.common.collect.CollectPreconditions.checkNonnegative;",
          "",
          "import com.google.common.annotations.Beta;",
          "import com.google.common.annotations.GwtCompatible;",
          "import com.google.common.annotations.VisibleForTesting;",
          "import com.google.common.math.IntMath;",
          "import com.google.common.primitives.Ints;",
          "import com.google.errorprone.annotations.CanIgnoreReturnValue;",
          "import com.google.errorprone.annotations.concurrent.LazyInit;",
          "import com.google.j2objc.annotations.RetainedWith;",
          "import java.io.Serializable;",
          "import java.math.RoundingMode;",
          "import java.util.Arrays;",
          "import java.util.Collection;",
          "import java.util.Collections;",
          "import java.util.EnumSet;",
          "import java.util.Iterator;",
          "import java.util.Set;",
          "import java.util.SortedSet;",
          "import java.util.Spliterator;",
          "import java.util.function.Consumer;",
          "import java.util.stream.Collector;",
          "import org.checkerframework.checker.nullness.qual.Nullable;",
          "",
          "@GwtCompatible(serializable = true, emulated = true)",
          "public abstract class ImmutableSet<E> extends ImmutableCollection<E> implements Set<E> {",
          "  static final int SPLITERATOR_CHARACTERISTICS =",
          "      ImmutableCollection.SPLITERATOR_CHARACTERISTICS | Spliterator.DISTINCT;",
          "",
          "  public static <E> Collector<E, ?, ImmutableSet<E>> toImmutableSet() {",
          "    return CollectCollectors.toImmutableSet();",
          "  }",
          "",
          "  public static <E> ImmutableSet<E> of() {",
          "    return (ImmutableSet<E>) RegularImmutableSet.EMPTY;",
          "  }",
          "",
          "  public static <E> ImmutableSet<E> of(E element) {",
          "    return new SingletonImmutableSet<E>(element);",
          "  }",
          "",
          "  public static <E> ImmutableSet<E> of(E e1, E e2) {",
          "    return construct(2, 2, e1, e2);",
          "  }",
          "",
          "  public static <E> ImmutableSet<E> of(E e1, E e2, E e3) {",
          "    return construct(3, 3, e1, e2, e3);",
          "  }",
          "",
          "  public static <E> ImmutableSet<E> of(E e1, E e2, E e3, E e4) {",
          "    return construct(4, 4, e1, e2, e3, e4);",
          "  }",
          "",
          "  public static <E> ImmutableSet<E> of(E e1, E e2, E e3, E e4, E e5) {",
          "    return construct(5, 5, e1, e2, e3, e4, e5);",
          "  }",
          "",
          "  @SafeVarargs",
          "  public static <E> ImmutableSet<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E... others) {",
          "    checkArgument(",
          "        others.length <= Integer.MAX_VALUE - 6, \"the total number of elements must fit in an int\");",
          "    final int paramCount = 6;",
          "    Object[] elements = new Object[paramCount + others.length];",
          "    elements[0] = e1;",
          "    elements[1] = e2;",
          "    elements[2] = e3;",
          "    elements[3] = e4;",
          "    elements[4] = e5;",
          "    elements[5] = e6;",
          "    System.arraycopy(others, 0, elements, paramCount, others.length);",
          "    return construct(elements.length, elements.length, elements);",
          "  }",
          "",
          "  private static <E> ImmutableSet<E> constructUnknownDuplication(int n, Object... elements) {",
          "    return construct(",
          "        n,",
          "        Math.max(",
          "            ImmutableCollection.Builder.DEFAULT_INITIAL_CAPACITY,",
          "            IntMath.sqrt(n, RoundingMode.CEILING)),",
          "        elements);",
          "  }",
          "",
          "  private static <E> ImmutableSet<E> construct(int n, int expectedSize, Object... elements) {",
          "    switch (n) {",
          "      case 0:",
          "        return of();",
          "      case 1:",
          "        E elem = (E) elements[0];",
          "        return of(elem);",
          "      default:",
          "        SetBuilderImpl<E> builder = new RegularSetBuilderImpl<E>(expectedSize);",
          "        for (int i = 0; i < n; i++) {",
          "          @SuppressWarnings(\"unchecked\")",
          "          E e = (E) checkNotNull(elements[i]);",
          "          builder = builder.add(e);",
          "        }",
          "        return builder.review().build();",
          "    }",
          "  }",
          "",
          "  public static <E> ImmutableSet<E> copyOf(Collection<? extends E> elements) {",
          "    if (elements instanceof ImmutableSet && !(elements instanceof SortedSet)) {",
          "      ImmutableSet<E> set = (ImmutableSet<E>) elements;",
          "      if (!set.isPartialView()) {",
          "        return set;",
          "      }",
          "    } else if (elements instanceof EnumSet) {",
          "      return copyOfEnumSet((EnumSet) elements);",
          "    }",
          "    Object[] array = elements.toArray();",
          "    if (elements instanceof Set) {",
          "      return construct(array.length, array.length, array);",
          "    } else {",
          "      return constructUnknownDuplication(array.length, array);",
          "    }",
          "  }",
          "",
          "  public static <E> ImmutableSet<E> copyOf(Iterable<? extends E> elements) {",
          "    return (elements instanceof Collection)",
          "        ? copyOf((Collection<? extends E>) elements)",
          "        : copyOf(elements.iterator());",
          "  }",
          "",
          "  public static <E> ImmutableSet<E> copyOf(Iterator<? extends E> elements) {",
          "    if (!elements.hasNext()) {",
          "      return of();",
          "    }",
          "    E first = elements.next();",
          "    if (!elements.hasNext()) {",
          "      return of(first);",
          "    } else {",
          "      return new ImmutableSet.Builder<E>().add(first).addAll(elements).build();",
          "    }",
          "  }",
          "",
          "  public static <E> ImmutableSet<E> copyOf(E[] elements) {",
          "    switch (elements.length) {",
          "      case 0:",
          "        return of();",
          "      case 1:",
          "        return of(elements[0]);",
          "      default:",
          "        return constructUnknownDuplication(elements.length, elements.clone());",
          "    }",
          "  }",
          "",
          "  private static ImmutableSet copyOfEnumSet(EnumSet enumSet) {",
          "    return ImmutableEnumSet.asImmutable(EnumSet.copyOf(enumSet));",
          "  }",
          "",
          "  ImmutableSet() {}",
          "",
          "  boolean isHashCodeFast() {",
          "    return false;",
          "  }",
          "",
          "  @Override",
          "  public boolean equals(@Nullable Object object) {",
          "    if (object == this) {",
          "      return true;",
          "    } else if (object instanceof ImmutableSet",
          "        && isHashCodeFast()",
          "        && ((ImmutableSet<?>) object).isHashCodeFast()",
          "        && hashCode() != object.hashCode()) {",
          "      return false;",
          "    }",
          "    return Sets.equalsImpl(this, object);",
          "  }",
          "",
          "  @Override",
          "  public int hashCode() {",
          "    return Sets.hashCodeImpl(this);",
          "  }",
          "",
          "  @Override",
          "  public abstract UnmodifiableIterator<E> iterator();",
          "",
          "  @LazyInit @RetainedWith private transient @Nullable ImmutableList<E> asList;",
          "",
          "  @Override",
          "  public ImmutableList<E> asList() {",
          "    ImmutableList<E> result = asList;",
          "    return (result == null) ? asList = createAsList() : result;",
          "  }",
          "",
          "  ImmutableList<E> createAsList() {",
          "    return new RegularImmutableAsList<E>(this, toArray());",
          "  }",
          "",
          "  abstract static class Indexed<E> extends ImmutableSet<E> {",
          "    abstract E get(int index);",
          "",
          "    @Override",
          "    public UnmodifiableIterator<E> iterator() {",
          "      return asList().iterator();",
          "    }",
          "",
          "    @Override",
          "    public Spliterator<E> spliterator() {",
          "      return CollectSpliterators.indexed(size(), SPLITERATOR_CHARACTERISTICS, this::get);",
          "    }",
          "",
          "    @Override",
          "    public void forEach(Consumer<? super E> consumer) {",
          "      checkNotNull(consumer);",
          "      int n = size();",
          "      for (int i = 0; i < n; i++) {",
          "        consumer.accept(get(i));",
          "      }",
          "    }",
          "",
          "    @Override",
          "    int copyIntoArray(Object[] dst, int offset) {",
          "      return asList().copyIntoArray(dst, offset);",
          "    }",
          "",
          "    @Override",
          "    ImmutableList<E> createAsList() {",
          "      return new ImmutableAsList<E>() {",
          "        @Override",
          "        public E get(int index) {",
          "          return Indexed.this.get(index);",
          "        }",
          "",
          "        @Override",
          "        Indexed<E> delegateCollection() {",
          "          return Indexed.this;",
          "        }",
          "      };",
          "    }",
          "  }",
          "",
          "  private static class SerializedForm implements Serializable {",
          "    final Object[] elements;",
          "",
          "    SerializedForm(Object[] elements) {",
          "      this.elements = elements;",
          "    }",
          "",
          "    Object readResolve() {",
          "      return copyOf(elements);",
          "    }",
          "",
          "    private static final long serialVersionUID = 0;",
          "  }",
          "",
          "  @Override",
          "  Object writeReplace() {",
          "    return new SerializedForm(toArray());",
          "  }",
          "",
          "  public static <E> Builder<E> builder() {",
          "    return new Builder<E>();",
          "  }",
          "",
          "  @Beta",
          "  public static <E> Builder<E> builderWithExpectedSize(int expectedSize) {",
          "    checkNonnegative(expectedSize, \"expectedSize\");",
          "    return new Builder<E>(expectedSize);",
          "  }",
          "",
          "  static Object[] rebuildHashTable(int newTableSize, Object[] elements, int n) {",
          "    Object[] hashTable = new Object[newTableSize];",
          "    int mask = hashTable.length - 1;",
          "    for (int i = 0; i < n; i++) {",
          "      Object e = elements[i];",
          "      int j0 = Hashing.smear(e.hashCode());",
          "      for (int j = j0; ; j++) {",
          "        int index = j & mask;",
          "        if (hashTable[index] == null) {",
          "          hashTable[index] = e;",
          "          break;",
          "        }",
          "      }",
          "    }",
          "    return hashTable;",
          "  }",
          "",
          "  public static class Builder<E> extends ImmutableCollection.Builder<E> {",
          "    private SetBuilderImpl<E> impl;",
          "    boolean forceCopy;",
          "",
          "    public Builder() {",
          "      this(DEFAULT_INITIAL_CAPACITY);",
          "    }",
          "",
          "    Builder(int capacity) {",
          "      impl = new RegularSetBuilderImpl<E>(capacity);",
          "    }",
          "",
          "    Builder(@SuppressWarnings(\"unused\") boolean subclass) {",
          "    }",
          "",
          "    @VisibleForTesting",
          "    void forceJdk() {",
          "      this.impl = new JdkBackedSetBuilderImpl<E>(impl);",
          "    }",
          "",
          "    final void copyIfNecessary() {",
          "      if (forceCopy) {",
          "        copy();",
          "        forceCopy = false;",
          "      }",
          "    }",
          "",
          "    void copy() {",
          "      impl = impl.copy();",
          "    }",
          "",
          "    @Override",
          "    @CanIgnoreReturnValue",
          "    public Builder<E> add(E element) {",
          "      checkNotNull(element);",
          "      copyIfNecessary();",
          "      impl = impl.add(element);",
          "      return this;",
          "    }",
          "",
          "    @Override",
          "    @CanIgnoreReturnValue",
          "    public Builder<E> add(E... elements) {",
          "      super.add(elements);",
          "      return this;",
          "    }",
          "",
          "    @Override",
          "    @CanIgnoreReturnValue",
          "    public Builder<E> addAll(Iterable<? extends E> elements) {",
          "      super.addAll(elements);",
          "      return this;",
          "    }",
          "",
          "    @Override",
          "    @CanIgnoreReturnValue",
          "    public Builder<E> addAll(Iterator<? extends E> elements) {",
          "      super.addAll(elements);",
          "      return this;",
          "    }",
          "",
          "    Builder<E> combine(Builder<E> other) {",
          "      copyIfNecessary();",
          "      this.impl = this.impl.combine(other.impl);",
          "      return this;",
          "    }",
          "",
          "    @Override",
          "    public ImmutableSet<E> build() {",
          "      forceCopy = true;",
          "      impl = impl.review();",
          "      return impl.build();",
          "    }",
          "  }",
          "",
          "  private abstract static class SetBuilderImpl<E> {",
          "    E[] dedupedElements;",
          "    int distinct;",
          "",
          "    @SuppressWarnings(\"unchecked\")",
          "    SetBuilderImpl(int expectedCapacity) {",
          "      this.dedupedElements = (E[]) new Object[expectedCapacity];",
          "      this.distinct = 0;",
          "    }",
          "",
          "    SetBuilderImpl(SetBuilderImpl<E> toCopy) {",
          "      this.dedupedElements = Arrays.copyOf(toCopy.dedupedElements, toCopy.dedupedElements.length);",
          "      this.distinct = toCopy.distinct;",
          "    }",
          "",
          "    private void ensureCapacity(int minCapacity) {",
          "      if (minCapacity > dedupedElements.length) {",
          "        int newCapacity =",
          "            ImmutableCollection.Builder.expandedCapacity(dedupedElements.length, minCapacity);",
          "        dedupedElements = Arrays.copyOf(dedupedElements, newCapacity);",
          "      }",
          "    }",
          "",
          "    final void addDedupedElement(E e) {",
          "      ensureCapacity(distinct + 1);",
          "      dedupedElements[distinct++] = e;",
          "    }",
          "",
          "    abstract SetBuilderImpl<E> add(E e);",
          "",
          "    final SetBuilderImpl<E> combine(SetBuilderImpl<E> other) {",
          "      SetBuilderImpl<E> result = this;",
          "      for (int i = 0; i < other.distinct; i++) {",
          "        result = result.add(other.dedupedElements[i]);",
          "      }",
          "      return result;",
          "    }",
          "",
          "    abstract SetBuilderImpl<E> copy();",
          "",
          "    SetBuilderImpl<E> review() {",
          "      return this;",
          "    }",
          "",
          "    abstract ImmutableSet<E> build();",
          "  }",
          "",
          "  static final int MAX_TABLE_SIZE = Ints.MAX_POWER_OF_TWO;",
          "",
          "  private static final double DESIRED_LOAD_FACTOR = 0.7;",
          "",
          "  private static final int CUTOFF = (int) (MAX_TABLE_SIZE * DESIRED_LOAD_FACTOR);",
          "",
          "  @VisibleForTesting",
          "  static int chooseTableSize(int setSize) {",
          "    setSize = Math.max(setSize, 2);",
          "    if (setSize < CUTOFF) {",
          "      int tableSize = Integer.highestOneBit(setSize - 1) << 1;",
          "      while (tableSize * DESIRED_LOAD_FACTOR < setSize) {",
          "        tableSize <<= 1;",
          "      }",
          "      return tableSize;",
          "    }",
          "",
          "    checkArgument(setSize < MAX_TABLE_SIZE, \"collection too large\");",
          "    return MAX_TABLE_SIZE;",
          "  }",
          "",
          "  static final double HASH_FLOODING_FPP = 0.001;",
          "",
          "  static final int MAX_RUN_MULTIPLIER = 13;",
          "",
          "  static boolean hashFloodingDetected(Object[] hashTable) {",
          "    int maxRunBeforeFallback = maxRunBeforeFallback(hashTable.length);",
          "",
          "    int endOfStartRun;",
          "    for (endOfStartRun = 0; endOfStartRun < hashTable.length; ) {",
          "      if (hashTable[endOfStartRun] == null) {",
          "        break;",
          "      }",
          "      endOfStartRun++;",
          "      if (endOfStartRun > maxRunBeforeFallback) {",
          "        return true;",
          "      }",
          "    }",
          "    int startOfEndRun;",
          "    for (startOfEndRun = hashTable.length - 1; startOfEndRun > endOfStartRun; startOfEndRun--) {",
          "      if (hashTable[startOfEndRun] == null) {",
          "        break;",
          "      }",
          "      if (endOfStartRun + (hashTable.length - 1 - startOfEndRun) > maxRunBeforeFallback) {",
          "        return true;",
          "      }",
          "    }",
          "",
          "    int testBlockSize = maxRunBeforeFallback / 2;",
          "    blockLoop:",
          "    for (int i = endOfStartRun + 1; i + testBlockSize <= startOfEndRun; i += testBlockSize) {",
          "      for (int j = 0; j < testBlockSize; j++) {",
          "        if (hashTable[i + j] == null) {",
          "          continue blockLoop;",
          "        }",
          "      }",
          "      return true;",
          "    }",
          "    return false;",
          "  }",
          "",
          "  private static int maxRunBeforeFallback(int tableSize) {",
          "  }",
          "",
          "  private static final class RegularSetBuilderImpl<E> extends SetBuilderImpl<E> {",
          "    private Object[] hashTable;",
          "    private int maxRunBeforeFallback;",
          "    private int expandTableThreshold;",
          "    private int hashCode;",
          "",
          "    RegularSetBuilderImpl(int expectedCapacity) {",
          "      super(expectedCapacity);",
          "      int tableSize = chooseTableSize(expectedCapacity);",
          "      this.hashTable = new Object[tableSize];",
          "      this.maxRunBeforeFallback = maxRunBeforeFallback(tableSize);",
          "      this.expandTableThreshold = (int) (DESIRED_LOAD_FACTOR * tableSize);",
          "    }",
          "",
          "    RegularSetBuilderImpl(RegularSetBuilderImpl<E> toCopy) {",
          "      super(toCopy);",
          "      this.hashTable = Arrays.copyOf(toCopy.hashTable, toCopy.hashTable.length);",
          "      this.maxRunBeforeFallback = toCopy.maxRunBeforeFallback;",
          "      this.expandTableThreshold = toCopy.expandTableThreshold;",
          "      this.hashCode = toCopy.hashCode;",
          "    }",
          "",
          "    void ensureTableCapacity(int minCapacity) {",
          "      if (minCapacity > expandTableThreshold && hashTable.length < MAX_TABLE_SIZE) {",
          "        int newTableSize = hashTable.length * 2;",
          "        hashTable = rebuildHashTable(newTableSize, dedupedElements, distinct);",
          "        maxRunBeforeFallback = maxRunBeforeFallback(newTableSize);",
          "        expandTableThreshold = (int) (DESIRED_LOAD_FACTOR * newTableSize);",
          "      }",
          "    }",
          "",
          "    @Override",
          "    SetBuilderImpl<E> add(E e) {",
          "      checkNotNull(e);",
          "      int eHash = e.hashCode();",
          "      int i0 = Hashing.smear(eHash);",
          "      int mask = hashTable.length - 1;",
          "      for (int i = i0; i - i0 < maxRunBeforeFallback; i++) {",
          "        int index = i & mask;",
          "        Object tableEntry = hashTable[index];",
          "        if (tableEntry == null) {",
          "          addDedupedElement(e);",
          "          hashTable[index] = e;",
          "          hashCode += eHash;",
          "          return this;",
          "          return this;",
          "        }",
          "      }",
          "      return new JdkBackedSetBuilderImpl<E>(this).add(e);",
          "    }",
          "",
          "    @Override",
          "    SetBuilderImpl<E> copy() {",
          "      return new RegularSetBuilderImpl<E>(this);",
          "    }",
          "",
          "    @Override",
          "    SetBuilderImpl<E> review() {",
          "      int targetTableSize = chooseTableSize(distinct);",
          "      if (targetTableSize * 2 < hashTable.length) {",
          "        hashTable = rebuildHashTable(targetTableSize, dedupedElements, distinct);",
          "        maxRunBeforeFallback = maxRunBeforeFallback(targetTableSize);",
          "      }",
          "      return hashFloodingDetected(hashTable) ? new JdkBackedSetBuilderImpl<E>(this) : this;",
          "    }",
          "",
          "    @Override",
          "    ImmutableSet<E> build() {",
          "      switch (distinct) {",
          "        case 0:",
          "          return of();",
          "        case 1:",
          "          return of(dedupedElements[0]);",
          "        default:",
          "          Object[] elements =",
          "              (distinct == dedupedElements.length)",
          "                  ? dedupedElements",
          "                  : Arrays.copyOf(dedupedElements, distinct);",
          "          return new RegularImmutableSet<E>(elements, hashCode, hashTable, hashTable.length - 1);",
          "      }",
          "    }",
          "  }",
          "",
          "  private static final class JdkBackedSetBuilderImpl<E> extends SetBuilderImpl<E> {",
          "    private final Set<Object> delegate;",
          "",
          "    JdkBackedSetBuilderImpl(SetBuilderImpl<E> toCopy) {",
          "      delegate = Sets.newHashSetWithExpectedSize(distinct);",
          "      for (int i = 0; i < distinct; i++) {",
          "        delegate.add(dedupedElements[i]);",
          "      }",
          "    }",
          "",
          "    @Override",
          "    SetBuilderImpl<E> add(E e) {",
          "      checkNotNull(e);",
          "      if (delegate.add(e)) {",
          "        addDedupedElement(e);",
          "      }",
          "      return this;",
          "    }",
          "",
          "    @Override",
          "    SetBuilderImpl<E> copy() {",
          "      return new JdkBackedSetBuilderImpl<>(this);",
          "    }",
          "",
          "    @Override",
          "    ImmutableSet<E> build() {",
          "      switch (distinct) {",
          "        case 0:",
          "          return of();",
          "        case 1:",
          "          return of(dedupedElements[0]);",
          "        default:",
          "          return new JdkBackedImmutableSet<E>(",
          "              delegate, ImmutableList.asImmutableList(dedupedElements, distinct));",
          "      }",
          "    }",
          "  }",
          "}",
        },
        {
          // all the imports (what we are interested in finding out!)
          "checkArgument",
          "checkNotNull",
          "checkNonnegative",
          "Beta",
          "GwtCompatible",
          "VisibleForTesting",
          "IntMath",
          "Ints",
          "CanIgnoreReturnValue",
          "LazyInit",
          "RetainedWith",
          "Serializable",
          "RoundingMode",
          "Arrays",
          "Collection",
          "EnumSet",
          "Iterator",
          "Set",
          "SortedSet",
          "Spliterator",
          "Consumer",
          "Collector",
          "Nullable",
          // Identifiers that are in the package
          "RegularImmutableAsList",
          "ImmutableCollection",
          "toArray",
          "SingletonImmutableSet",
          "ImmutableAsList",
          "JdkBackedImmutableSet",
          "Hashing",
          "ImmutableList",
          "CollectSpliterators",
          "RegularImmutableSet",
          "CollectCollectors",
          "Sets",
          "ImmutableEnumSet",
          "UnmodifiableIterator",
          // Identifiers that are in the package and resolved through class extension
          "DEFAULT_INITIAL_CAPACITY",
          // Identifiers that are not in the package but resolved through class extension
          "size",
          // Constants
          "Override",
          "Iterable",
          "Math",
          "this",
          "Object",
          "SuppressWarnings",
          "System",
          "super",
          "Integer",
          "SafeVarargs",
        },
      },
    };
    ImmutableList.Builder<Object[]> builder = ImmutableList.builder();
    for (String[][] inputOutput : inputOutputs) {
      String input = String.join("\n", inputOutput[0]) + "\n";
      Set<String> output = Sets.newHashSet(inputOutput[1]);
      Object[] parameters = {input, output};
      builder.add(parameters);
    }
    return builder.build();
  }

  private final String input;
  private final Set<String> expected;

  public UnresolvedIdentifierScannerTest(String input, Set<String> expected) {
    this.input = input;
    this.expected = expected;
  }

  @Test
  public void scanTest() throws Exception {
    UnresolvedIdentifierScanner scanner = new UnresolvedIdentifierScanner();
    try {
      scanner.scan(Parser.getCompilationUnit("testfile", input), null);
    } catch (ImporterException e) {
      for (ImporterException.ImporterDiagnostic d : e.diagnostics()) {
        System.out.println(d);
      }
      fail();
    }

    assertThat(scanner.unresolved()).containsExactlyElementsIn(expected);
  }
}
