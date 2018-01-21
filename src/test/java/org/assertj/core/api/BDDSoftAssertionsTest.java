/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * Copyright 2012-2018 the original author or authors.
 */
package org.assertj.core.api;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.assertj.core.test.Maps.mapOf;
import static org.assertj.core.test.Name.name;
import static org.assertj.core.util.Arrays.array;
import static org.assertj.core.util.DateUtil.parseDatetime;
import static org.assertj.core.util.Sets.newLinkedHashSet;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.DoublePredicate;
import java.util.function.IntPredicate;
import java.util.function.LongPredicate;
import java.util.function.Predicate;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.assertj.core.api.ClassAssertBaseTest.AnnotatedClass;
import org.assertj.core.api.ClassAssertBaseTest.AnotherAnnotation;
import org.assertj.core.api.ClassAssertBaseTest.MyAnnotation;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.assertj.core.api.test.ComparableExample;
import org.assertj.core.data.MapEntry;
import org.assertj.core.test.CartoonCharacter;
import org.assertj.core.test.Name;
import org.assertj.core.util.Lists;
import org.assertj.core.util.VisibleForTesting;
import org.junit.Before;
import org.junit.Test;

public class BDDSoftAssertionsTest extends BaseAssertionsTest {

  private BDDSoftAssertions softly;

  private CartoonCharacter homer;
  private CartoonCharacter fred;
  private CartoonCharacter lisa;
  private CartoonCharacter maggie;
  private CartoonCharacter bart;

  @Before
  public void setup() {
    softly = new BDDSoftAssertions();

    bart = new CartoonCharacter("Bart Simpson");
    lisa = new CartoonCharacter("Lisa Simpson");
    maggie = new CartoonCharacter("Maggie Simpson");

    homer = new CartoonCharacter("Homer Simpson");
    homer.getChildren().add(bart);
    homer.getChildren().add(lisa);
    homer.getChildren().add(maggie);

    CartoonCharacter pebbles = new CartoonCharacter("Pebbles Flintstone");
    fred = new CartoonCharacter("Fred Flintstone");
    fred.getChildren().add(pebbles);
  }

  @Test
  public void all_assertions_should_pass() {
    softly.then(1).isEqualTo(1);
    softly.then(Lists.newArrayList(1, 2)).containsOnly(1, 2);
    softly.assertAll();
  }

  @Test
  public void should_return_success_of_last_assertion() {
    softly.then(true).isFalse();
    softly.then(true).isEqualTo(true);
    assertThat(softly.wasSuccess()).isTrue();
  }

  @Test
  public void should_return_success_of_last_assertion_with_nested_calls() {
    softly.then(true).isFalse();
    softly.then(true).isTrue(); // isTrue() calls isEqualTo(true)
    assertThat(softly.wasSuccess()).isTrue();
  }

  @Test
  public void should_return_failure_of_last_assertion() {
    softly.then(true).isTrue();
    softly.then(true).isEqualTo(false);
    assertThat(softly.wasSuccess()).isFalse();
  }

  @Test
  public void should_return_failure_of_last_assertion_with_nested_calls() {
    softly.then(true).isTrue();
    softly.then(true).isFalse(); // isFalse() calls isEqualTo(false)
    assertThat(softly.wasSuccess()).isFalse();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void should_be_able_to_catch_exceptions_thrown_by_map_assertions() {
    // GIVEN
    Map<String, String> map = mapOf(MapEntry.entry("54", "55"));
    // WHEN
    softly.then(map).contains(MapEntry.entry("1", "2")).isEmpty();
    // THEN
    List<Throwable> errors = softly.errorsCollected();
    assertThat(errors).hasSize(2);
    assertThat(errors.get(0)).hasMessageStartingWith(format("%nExpecting:%n"
                                                            + " <{\"54\"=\"55\"}>%n"
                                                            + "to contain:%n"
                                                            + " <[MapEntry[key=\"1\", value=\"2\"]]>%n"
                                                            + "but could not find:%n"
                                                            + " <[MapEntry[key=\"1\", value=\"2\"]]>%n"));
    assertThat(errors.get(1)).hasMessageStartingWith(format("%nExpecting empty but was:<{\"54\"=\"55\"}>"));

  }

  @SuppressWarnings("unchecked")
  @Test
  public void should_be_able_to_catch_exceptions_thrown_by_all_proxied_methods() throws URISyntaxException {
    try {
      softly.then(BigDecimal.ZERO).isEqualTo(BigDecimal.ONE);

      softly.then(Boolean.FALSE).isTrue();
      softly.then(false).isTrue();
      softly.then(new boolean[] { false }).isEqualTo(new boolean[] { true });

      softly.then(new Byte((byte) 0)).isEqualTo((byte) 1);
      softly.then((byte) 2).inHexadecimal().isEqualTo((byte) 3);
      softly.then(new byte[] { 4 }).isEqualTo(new byte[] { 5 });

      softly.then(new Character((char) 65)).isEqualTo(new Character((char) 66));
      softly.then((char) 67).isEqualTo((char) 68);
      softly.then(new char[] { 69 }).isEqualTo(new char[] { 70 });

      softly.then(new StringBuilder("a")).isEqualTo(new StringBuilder("b"));

      softly.then(Object.class).isEqualTo(String.class);

      softly.then(parseDatetime("1999-12-31T23:59:59")).isEqualTo(parseDatetime("2000-01-01T00:00:01"));

      softly.then(new Double(6.0d)).isEqualTo(new Double(7.0d));
      softly.then(8.0d).isEqualTo(9.0d);
      softly.then(new double[] { 10.0d }).isEqualTo(new double[] { 11.0d });

      softly.then(new File("a"))
            .overridingErrorMessage("expected:<File(b)> but was:<File(a)>")
            .isEqualTo(new File("b"));

      softly.then(new Float(12f)).isEqualTo(new Float(13f));
      softly.then(14f).isEqualTo(15f);
      softly.then(new float[] { 16f }).isEqualTo(new float[] { 17f });

      softly.then(new ByteArrayInputStream(new byte[] { (byte) 65 }))
            .hasSameContentAs(new ByteArrayInputStream(new byte[] { (byte) 66 }));

      softly.then(new Integer(20)).isEqualTo(new Integer(21));
      softly.then(22).isEqualTo(23);
      softly.then(new int[] { 24 }).isEqualTo(new int[] { 25 });

      softly.then((Iterable<String>) Lists.newArrayList("26")).isEqualTo(Lists.newArrayList("27"));
      softly.then(Lists.newArrayList("28").iterator()).contains("29");
      softly.then(Lists.newArrayList("30")).isEqualTo(Lists.newArrayList("31"));

      softly.then(new Long(32L)).isEqualTo(new Long(33L));
      softly.then(34L).isEqualTo(35L);
      softly.then(new long[] { 36L }).isEqualTo(new long[] { 37L });

      softly.then(mapOf(MapEntry.entry("38", "39"))).isEqualTo(mapOf(MapEntry.entry("40", "41")));

      softly.then(new Short((short) 42)).isEqualTo(new Short((short) 43));
      softly.then((short) 44).isEqualTo((short) 45);
      softly.then(new short[] { (short) 46 }).isEqualTo(new short[] { (short) 47 });

      softly.then("48").isEqualTo("49");

      softly.then(new Object() {
        @Override
        public String toString() {
          return "50";
        }
      }).isEqualTo(new Object() {
        @Override
        public String toString() {
          return "51";
        }
      });

      softly.then(new Object[] { new Object() {
        @Override
        public String toString() {
          return "52";
        }
      } }).isEqualTo(new Object[] { new Object() {
        @Override
        public String toString() {
          return "53";
        }
      } });

      final IllegalArgumentException illegalArgumentException = new IllegalArgumentException("IllegalArgumentException message");
      softly.then(illegalArgumentException).hasMessage("NullPointerException message");
      softly.thenThrownBy(new ThrowingCallable() {

        @Override
        public void call() throws Exception {
          throw new Exception("something was wrong");
        }

      }).hasMessage("something was good");

      softly.then(mapOf(MapEntry.entry("54", "55"))).contains(MapEntry.entry("1", "2"));

      softly.then(LocalTime.of(12, 00)).isEqualTo(LocalTime.of(13, 00));
      softly.then(OffsetTime.of(12, 0, 0, 0, ZoneOffset.UTC))
            .isEqualTo(OffsetTime.of(13, 0, 0, 0, ZoneOffset.UTC));

      softly.then(Optional.of("not empty")).isEqualTo("empty");
      softly.then(OptionalInt.of(0)).isEqualTo(1);
      softly.then(OptionalDouble.of(0.0)).isEqualTo(1.0);
      softly.then(OptionalLong.of(0L)).isEqualTo(1L);
      softly.then(new URI("http://assertj.org")).hasPort(8888);
      softly.then(CompletableFuture.completedFuture("done")).hasFailed();
      softly.then((Predicate<String>) s -> s.equals("something")).accepts("something else");
      softly.then((IntPredicate) s -> s == 1).accepts(2);
      softly.then((LongPredicate) s -> s == 1).accepts(2);
      softly.then((DoublePredicate) s -> s == 1).accepts(2);

      softly.assertAll();
      fail("Should not reach here");
    } catch (SoftAssertionError e) {
      List<String> errors = e.getErrors();
      assertThat(errors).hasSize(52);
      assertThat(errors.get(0)).startsWith("expected:<[1]> but was:<[0]>");

      assertThat(errors.get(1)).startsWith("expected:<[tru]e> but was:<[fals]e>");
      assertThat(errors.get(2)).startsWith("expected:<[tru]e> but was:<[fals]e>");
      assertThat(errors.get(3)).startsWith("expected:<[[tru]e]> but was:<[[fals]e]>");

      assertThat(errors.get(4)).startsWith("expected:<[1]> but was:<[0]>");
      assertThat(errors.get(5)).startsWith("expected:<0x0[3]> but was:<0x0[2]>");
      assertThat(errors.get(6)).startsWith("expected:<[[5]]> but was:<[[4]]>");

      assertThat(errors.get(7)).startsWith("expected:<'[B]'> but was:<'[A]'>");
      assertThat(errors.get(8)).startsWith("expected:<'[D]'> but was:<'[C]'>");
      assertThat(errors.get(9)).startsWith("expected:<['[F]']> but was:<['[E]']>");

      assertThat(errors.get(10)).startsWith("expected:<[b]> but was:<[a]>");

      assertThat(errors.get(11)).startsWith("expected:<java.lang.[String]> but was:<java.lang.[Object]>");

      assertThat(errors.get(12)).startsWith("expected:<[2000-01-01T00:00:01].000> but was:<[1999-12-31T23:59:59].000>");

      assertThat(errors.get(13)).startsWith("expected:<[7].0> but was:<[6].0>");
      assertThat(errors.get(14)).startsWith("expected:<[9].0> but was:<[8].0>");
      assertThat(errors.get(15)).startsWith("expected:<[1[1].0]> but was:<[1[0].0]>");

      assertThat(errors.get(16)).startsWith("expected:<File(b)> but was:<File(a)>");

      assertThat(errors.get(17)).startsWith("expected:<1[3].0f> but was:<1[2].0f>");
      assertThat(errors.get(18)).startsWith("expected:<1[5].0f> but was:<1[4].0f>");
      assertThat(errors.get(19)).startsWith("expected:<[1[7].0f]> but was:<[1[6].0f]>");

      assertThat(errors.get(20)).startsWith(format("%nInputStreams do not have same content:%n%n"
                                                   + "Changed content at line 1:%n"
                                                   + "expecting:%n"
                                                   + "  [\"B\"]%n"
                                                   + "but was:%n"
                                                   + "  [\"A\"]%n"));

      assertThat(errors.get(21)).startsWith("expected:<2[1]> but was:<2[0]>");
      assertThat(errors.get(22)).startsWith("expected:<2[3]> but was:<2[2]>");
      assertThat(errors.get(23)).startsWith("expected:<[2[5]]> but was:<[2[4]]>");

      assertThat(errors.get(24)).startsWith("expected:<[\"2[7]\"]> but was:<[\"2[6]\"]>");
      assertThat(errors.get(25)).startsWith(format("%nExpecting:%n" +
                                                   " <[\"28\"]>%n" +
                                                   "to contain:%n" +
                                                   " <[\"29\"]>%n" +
                                                   "but could not find:%n" +
                                                   " <[\"29\"]>%n"));
      assertThat(errors.get(26)).startsWith("expected:<[\"3[1]\"]> but was:<[\"3[0]\"]>");

      assertThat(errors.get(27)).startsWith("expected:<3[3]L> but was:<3[2]L>");
      assertThat(errors.get(28)).startsWith("expected:<3[5]L> but was:<3[4]L>");
      assertThat(errors.get(29)).startsWith("expected:<[3[7]L]> but was:<[3[6]L]>");

      assertThat(errors.get(30)).startsWith("expected:<{\"[40\"=\"41]\"}> but was:<{\"[38\"=\"39]\"}>");

      assertThat(errors.get(31)).startsWith("expected:<4[3]> but was:<4[2]>");
      assertThat(errors.get(32)).startsWith("expected:<4[5]> but was:<4[4]>");
      assertThat(errors.get(33)).startsWith("expected:<[4[7]]> but was:<[4[6]]>");

      assertThat(errors.get(34)).startsWith("expected:<\"4[9]\"> but was:<\"4[8]\">");

      assertThat(errors.get(35)).startsWith("expected:<5[1]> but was:<5[0]>");
      assertThat(errors.get(36)).startsWith("expected:<[5[3]]> but was:<[5[2]]>");
      assertThat(errors.get(37)).startsWith(format("%nExpecting message:%n"
                                                   + " <\"NullPointerException message\">%n"
                                                   + "but was:%n"
                                                   + " <\"IllegalArgumentException message\">"));
      assertThat(errors.get(38)).startsWith(format("%nExpecting message:%n"
                                                   + " <\"something was good\">%n"
                                                   + "but was:%n"
                                                   + " <\"something was wrong\">"));
      assertThat(errors.get(39)).startsWith(format("%nExpecting:%n"
                                                   + " <{\"54\"=\"55\"}>%n"
                                                   + "to contain:%n"
                                                   + " <[MapEntry[key=\"1\", value=\"2\"]]>%n"
                                                   + "but could not find:%n"
                                                   + " <[MapEntry[key=\"1\", value=\"2\"]]>%n"));

      assertThat(errors.get(40)).startsWith("expected:<1[3]:00> but was:<1[2]:00>");
      assertThat(errors.get(41)).startsWith("expected:<1[3]:00Z> but was:<1[2]:00Z>");

      assertThat(errors.get(42)).startsWith("expected:<[\"empty\"]> but was:<[Optional[not empty]]>");
      assertThat(errors.get(43)).startsWith("expected:<[1]> but was:<[OptionalInt[0]]>");
      assertThat(errors.get(44)).startsWith("expected:<[1.0]> but was:<[OptionalDouble[0.0]]>");
      assertThat(errors.get(45)).startsWith("expected:<[1L]> but was:<[OptionalLong[0]]>");
      assertThat(errors.get(46)).contains("Expecting port of");
      assertThat(errors.get(47)).contains("to have failed");
      assertThat(errors.get(48)).startsWith(format("%nExpecting:%n  <given predicate>%n"
                                                   + "to accept <\"something else\"> but it did not."));

      assertThat(errors.get(49)).startsWith(format("%nExpecting:%n  <given predicate>%n"
                                                   + "to accept <2> but it did not."));

      assertThat(errors.get(50)).startsWith(format("%nExpecting:%n  <given predicate>%n"
                                                   + "to accept <2L> but it did not."));
      assertThat(errors.get(51)).startsWith(format("%nExpecting:%n  <given predicate>%n"
                                                   + "to accept <2.0> but it did not."));
    }
  }

  @Test
  public void should_pass_when_using_extracting_with_list() {
    // GIVEN
    List<Name> names = asList(Name.name("John", "Doe"), name("Jane", "Doe"));
    // WHEN
    softly.then(names)
          .extracting("first")
          .as("using extracting()")
          .contains("John")
          .contains("Jane")
          .contains("Foo1");

    softly.then(names)
          .extracting(Name::getFirst)
          .as("using extracting(Extractor)")
          .contains("John")
          .contains("Jane")
          .contains("Foo2");

    softly.then(names)
          .extracting("first", String.class)
          .as("using extracting(..., Class)")
          .contains("John")
          .contains("Jane")
          .contains("Foo3");

    softly.then(names)
          .extracting("first", "last")
          .as("using extracting(...)")
          .contains(tuple("John", "Doe"))
          .contains(tuple("Jane", "Doe"))
          .contains(tuple("Foo", "4"));

    softly.then(names)
          .extractingResultOf("getFirst", String.class)
          .as("using extractingResultOf(method, Class)")
          .contains("John")
          .contains("Jane")
          .contains("Foo5");

    softly.then(names)
          .extractingResultOf("getFirst")
          .as("using extractingResultOf(method)")
          .contains("John")
          .contains("Jane")
          .contains("Foo6");
    // THEN
    List<Throwable> errorsCollected = softly.errorsCollected();
    assertThat(errorsCollected).hasSize(6);
    assertThat(errorsCollected.get(0)).hasMessageContaining("Foo1");
    assertThat(errorsCollected.get(1)).hasMessageContaining("Foo2");
    assertThat(errorsCollected.get(2)).hasMessageContaining("Foo3");
    assertThat(errorsCollected.get(3)).hasMessageContaining("Foo")
                                      .hasMessageContaining("4");
    assertThat(errorsCollected.get(4)).hasMessageContaining("Foo5");
    assertThat(errorsCollected.get(5)).hasMessageContaining("Foo6");
  }

  @Test
  public void should_pass_when_using_extracting_with_iterable() {

    Iterable<Name> names = asList(name("John", "Doe"), name("Jane", "Doe"));

    try (AutoCloseableBDDSoftAssertions softly = new AutoCloseableBDDSoftAssertions()) {
      softly.then(names)
            .extracting("first")
            .as("using extracting()")
            .contains("John")
            .contains("Jane");

      softly.then(names)
            .extracting(Name::getFirst)
            .as("using extracting(Extractor)")
            .contains("John")
            .contains("Jane");

      softly.then(names)
            .extracting("first", String.class)
            .as("using extracting(..., Class)")
            .contains("John")
            .contains("Jane");

      softly.then(names)
            .extracting("first", "last")
            .as("using extracting(...)")
            .contains(tuple("John", "Doe"))
            .contains(tuple("Jane", "Doe"));

      softly.then(names)
            .extractingResultOf("getFirst", String.class)
            .as("using extractingResultOf(method, Class)")
            .contains("John")
            .contains("Jane");

      softly.then(names)
            .extractingResultOf("getFirst")
            .as("using extractingResultOf(method)")
            .contains("John")
            .contains("Jane");
    }
  }

  @Test
  public void should_work_when_using_extracting_with_array() {

    Name[] namesAsArray = { name("John", "Doe"), name("Jane", "Doe") };

    try (AutoCloseableBDDSoftAssertions softly = new AutoCloseableBDDSoftAssertions()) {
      softly.then(namesAsArray)
            .extracting("first")
            .as("using extracting()")
            .contains("John")
            .contains("Jane");

      softly.then(namesAsArray)
            .extracting(Name::getFirst)
            .as("using extracting(Extractor)")
            .contains("John")
            .contains("Jane");

      softly.then(namesAsArray)
            .extracting("first", String.class)
            .as("using extracting(..., Class)")
            .contains("John")
            .contains("Jane");

      softly.then(namesAsArray)
            .extracting("first", "last")
            .as("using extracting(...)")
            .contains(tuple("John", "Doe"))
            .contains(tuple("Jane", "Doe"));

      softly.then(namesAsArray)
            .extractingResultOf("getFirst", String.class)
            .as("using extractingResultOf(method, Class)")
            .contains("John")
            .contains("Jane");

      softly.then(namesAsArray)
            .extractingResultOf("getFirst")
            .as("using extractingResultOf(method)")
            .contains("John")
            .contains("Jane");
    }
  }

  @Test
  public void should_pass_when_using_extracting_with_iterator() {

    Iterator<Name> names = asList(name("John", "Doe"), name("Jane", "Doe")).iterator();

    try (AutoCloseableBDDSoftAssertions softly = new AutoCloseableBDDSoftAssertions()) {
      softly.then(names)
            .extracting("first")
            .as("using extracting()")
            .contains("John")
            .contains("Jane");
    }
  }

  @Test
  public void should_work_with_flat_extracting() {
    // GIVEN
    List<CartoonCharacter> characters = asList(homer, fred);
    CartoonCharacter[] charactersAsArray = characters.toArray(new CartoonCharacter[characters.size()]);
    // WHEN
    softly.then(characters)
          .flatExtracting(CartoonCharacter::getChildren)
          .as("using flatExtracting on Iterable")
          .containsAnyOf(homer, fred)
          .hasSize(123);
    softly.then(charactersAsArray)
          .flatExtracting(CartoonCharacter::getChildren)
          .as("using flatExtracting on array")
          .containsAnyOf(homer, fred)
          .hasSize(456);
    // THEN
    List<Throwable> errorsCollected = softly.errorsCollected();
    assertThat(errorsCollected).hasSize(4);
    assertThat(errorsCollected.get(0)).hasMessageContaining(homer.toString());
    assertThat(errorsCollected.get(1)).hasMessageContaining("123");
    assertThat(errorsCollected.get(2)).hasMessageContaining(fred.toString());
    assertThat(errorsCollected.get(3)).hasMessageContaining("456");
  }

  @Test
  public void should_collect_all_errors_when_using_extracting() {
    // GIVEN
    List<Name> names = asList(name("John", "Doe"), name("Jane", "Doe"));
    // WHEN
    softly.then(names)
          .extracting("first")
          .overridingErrorMessage("error 1")
          .contains("gandalf")
          .overridingErrorMessage("error 2")
          .contains("frodo");
    softly.then(names)
          .extracting("last")
          .overridingErrorMessage("error 3")
          .isEmpty();
    // THEN
    assertThat(softly.errorsCollected()).extracting(Throwable::getMessage)
                                        .containsExactly("error 1", "error 2", "error 3");
  }

  @Test
  public void should_collect_all_errors_when_using_flat_extracting() {
    // GIVEN
    List<CartoonCharacter> characters = asList(homer, fred);
    // WHEN
    softly.then(characters)
          .flatExtracting(CartoonCharacter::getChildren)
          .overridingErrorMessage("error 1")
          .hasSize(0)
          .overridingErrorMessage("error 2")
          .isEmpty();
    // THEN
    assertThat(softly.errorsCollected()).extracting(Throwable::getMessage)
                                        .containsExactly("error 1", "error 2");
  }

  @Test
  public void should_collect_all_errors_when_using_filtering() {
    // GIVEN
    LinkedHashSet<CartoonCharacter> dads = newLinkedHashSet(homer, fred);
    // WHEN
    softly.then(dads)
          .filteredOn("name", "Homer Simpson")
          .hasSize(10)
          .isEmpty();
    // THEN
    List<Throwable> errorsCollected = softly.errorsCollected();
    assertThat(errorsCollected).hasSize(2);
    assertThat(errorsCollected.get(0)).hasMessageStartingWith(format("%nExpected size:<10> but was:<1> in:%n<[CartoonCharacter [name=Homer Simpson]]>"));
    assertThat(errorsCollected.get(1)).hasMessageStartingWith(format("%nExpecting empty but was:<[CartoonCharacter [name=Homer Simpson]]>"));
  }

  @Test
  public void should_collect_all_errors_when_using_predicate_filtering() {
    // GIVEN
    LinkedHashSet<CartoonCharacter> dads = newLinkedHashSet(homer, fred);
    // WHEN
    softly.then(dads)
          .filteredOn(c -> c.getName().equals("Homer Simpson"))
          .hasSize(10)
          .isEmpty();
    // THEN
    List<Throwable> errorsCollected = softly.errorsCollected();
    assertThat(errorsCollected).hasSize(2);
    assertThat(errorsCollected.get(0)).hasMessageStartingWith(format("%nExpected size:<10> but was:<1> in:%n<[CartoonCharacter [name=Homer Simpson]]>"));
    assertThat(errorsCollected.get(1)).hasMessageStartingWith(format("%nExpecting empty but was:<[CartoonCharacter [name=Homer Simpson]]>"));
  }

  @Test
  public void should_work_with_comparable() {
    // GIVEN
    ComparableExample example1 = new ComparableExample(0);
    ComparableExample example2 = new ComparableExample(0);
    ComparableExample example3 = new ComparableExample(123);
    // WHEN
    softly.then(example1).isEqualByComparingTo(example2);
    softly.then(example1).isEqualByComparingTo(example3);
    // THEN
    List<Throwable> errorsCollected = softly.errorsCollected();
    assertThat(errorsCollected).hasSize(1);
    assertThat(errorsCollected.get(0)).hasMessageContaining("123");
  }

  @Test
  public void should_work_with_stream() {
    // WHEN
    softly.then(Stream.of("a", "b", "c")).contains("a", "foo");
    softly.then(IntStream.of(1, 2, 3)).contains(2, 4, 6);
    softly.then(LongStream.of(1, 2, 3)).contains(-1L, -2L, -3L);
    softly.then(DoubleStream.of(1, 2, 3)).contains(10.0, 20.0, 30.0);
    // THEN
    List<Throwable> errorsCollected = softly.errorsCollected();
    assertThat(errorsCollected).hasSize(4);
    assertThat(errorsCollected.get(0)).hasMessageContaining("foo");
    assertThat(errorsCollected.get(1)).hasMessageContaining("6");
    assertThat(errorsCollected.get(2)).hasMessageContaining("-3");
    assertThat(errorsCollected.get(3)).hasMessageContaining("30.0");
  }

  @Test
  public void should_work_with_CompletionStage() {
    // GIVEN
    CompletionStage<String> completionStage = completedFuture("done");
    // WHEN
    softly.then(completionStage).isDone();
    softly.then(completionStage).hasNotFailed();
    softly.then(completionStage).isCancelled();
    completionStage = null;
    softly.then(completionStage).isNull();
    // THEN
    assertThat(softly.errorsCollected()).hasSize(1);
    assertThat(softly.errorsCollected().get(0)).hasMessageContaining("cancelled");
  }

  @Test
  public void should_work_with_predicate() {
    // GIVEN
    Predicate<String> lowercasePredicate = s -> s.equals(s.toLowerCase());
    // WHEN
    softly.then(lowercasePredicate).accepts("a", "b", "c");
    softly.then(lowercasePredicate).accepts("a", "b", "C");
    // THEN
    assertThat(softly.errorsCollected()).hasSize(1);
    assertThat(softly.errorsCollected().get(0)).hasMessageContaining("C");
  }

  @Test
  public void should_work_with_optional() {
    // GIVEN
    Optional<String> optional = Optional.of("Gandalf");
    // WHEN
    softly.then(optional).contains("Gandalf");
    // THEN
    softly.assertAll();
  }

  @Test
  public void should_work_with_optional_chained_with_map() {
    // GIVEN
    Optional<String> optional = Optional.of("Gandalf");
    // WHEN
    softly.then(optional)
          .contains("Gandalf")
          .map(String::length)
          .contains(7);
    // THEN
    softly.assertAll();
  }

  @Test
  public void should_collect_all_errors_when_using_map() {
    // GIVEN
    Optional<String> optional = Optional.of("Gandalf");
    // WHEN
    softly.then(optional)
          .contains("Sauron");
    softly.then(optional)
          .contains("Gandalf")
          .map(String::length)
          .contains(1);
    // THEN
    assertThat(softly.errorsCollected()).hasSize(2);
  }

  @Test
  public void should_collect_all_errors_when_using_flatMap() {
    // GIVEN
    Optional<String> optional = Optional.of("Gandalf");
    // WHEN
    softly.then(optional)
          .contains("Sauron");
    softly.then(optional)
          .flatMap(s -> Optional.of(s.length()))
          .contains(1);
    // THEN
    assertThat(softly.errorsCollected()).hasSize(2);
  }

  @Test
  public void should_propagate_AssertionError_from_nested_proxied_calls() {
    // the nested proxied call to isNotEmpty() throw an Assertion error that must be propagated to the caller.
    softly.then(asList()).first();
    // nested proxied call to throwAssertionError when checking that is optional is present
    softly.then(Optional.empty()).contains("Foo");
    // nested proxied call to isNotNull
    softly.then((Predicate<String>) null).accepts("a", "b", "c");
    // nested proxied call to isCompleted
    softly.then(new CompletableFuture<String>()).isCompletedWithValue("done");
    // it must be caught by softly.assertAll()
    assertThat(softly.errorsCollected()).hasSize(4);
  }

  // bug #447

  public class TolkienCharacter {
    String name;
    int age;
  }

  @Test
  public void check_477_bugfix() {
    // GIVEN
    TolkienCharacter frodo = new TolkienCharacter();
    TolkienCharacter samnullGamgee = null;
    TolkienSoftAssertions softly = new TolkienSoftAssertions();
    // WHEN
    softly.then(frodo).hasAge(10); // Expect failure - age will be 0 due to not being initialized.
    softly.then(samnullGamgee).hasAge(11); // Expect failure - argument is null.
    // THEN
    assertThat(softly.errorsCollected()).hasSize(2);
  }

  public static class TolkienCharacterAssert extends AbstractAssert<TolkienCharacterAssert, TolkienCharacter> {

    public TolkienCharacterAssert(TolkienCharacter actual) {
      super(actual, TolkienCharacterAssert.class);
    }

    public static TolkienCharacterAssert assertThat(TolkienCharacter actual) {
      return new TolkienCharacterAssert(actual);
    }

    // 4 - a specific assertion !
    public TolkienCharacterAssert hasName(String name) {
      // check that actual TolkienCharacter we want to make assertions on is not null.
      isNotNull();

      // check condition
      if (!Objects.equals(actual.name, name)) {
        failWithMessage("Expected character's name to be <%s> but was <%s>", name, actual.name);
      }

      // return the current assertion for method chaining
      return this;
    }

    // 4 - another specific assertion !
    public TolkienCharacterAssert hasAge(int age) {
      // check that actual TolkienCharacter we want to make assertions on is not null.
      isNotNull();

      // check condition
      if (actual.age != age) {
        failWithMessage("Expected character's age to be <%s> but was <%s>", age, actual.age);
      }

      // return the current assertion for method chaining
      return this;
    }
  }

  public static class TolkienSoftAssertions extends SoftAssertions {

    public TolkienCharacterAssert then(TolkienCharacter actual) {
      return proxy(TolkienCharacterAssert.class, TolkienCharacter.class, actual);
    }
  }

  @Test
  public void should_return_failure_after_fail() {
    // GIVEN
    String failureMessage = "Should not reach here";
    // WHEN
    softly.fail(failureMessage);
    // THEN
    assertThat(softly.wasSuccess()).isFalse();
    assertThat(softly.errorsCollected()).hasSize(1);
    assertThat(softly.errorsCollected().get(0)).hasMessageStartingWith(failureMessage);
  }

  @Test
  public void should_return_failure_after_fail_with_parameters() {
    // GIVEN
    String failureMessage = "Should not reach %s or %s";
    // WHEN
    softly.fail(failureMessage, "here", "here");
    // THEN
    assertThat(softly.wasSuccess()).isFalse();
    assertThat(softly.errorsCollected()).hasSize(1);
    assertThat(softly.errorsCollected().get(0)).hasMessageStartingWith("Should not reach here or here");
  }

  @Test
  public void should_return_failure_after_fail_with_throwable() {
    // GIVEN
    String failureMessage = "Should not reach here";
    IllegalStateException realCause = new IllegalStateException();
    // WHEN
    softly.fail(failureMessage, realCause);
    // THEN
    assertThat(softly.wasSuccess()).isFalse();
    List<Throwable> errorsCollected = softly.errorsCollected();
    assertThat(errorsCollected).hasSize(1);
    assertThat(errorsCollected.get(0)).hasMessageStartingWith(failureMessage);
    assertThat(errorsCollected.get(0).getCause()).isEqualTo(realCause);
  }

  @Test
  public void should_return_failure_after_shouldHaveThrown() {
    // WHEN
    softly.shouldHaveThrown(IllegalArgumentException.class);
    // THEN
    assertThat(softly.wasSuccess()).isFalse();
    List<Throwable> errorsCollected = softly.errorsCollected();
    assertThat(errorsCollected).hasSize(1);
    assertThat(errorsCollected.get(0)).hasMessageStartingWith("IllegalArgumentException should have been thrown");
  }

  @Test
  public void should_assert_using_assertSoftly() {
    assertThatThrownBy(() -> {
      assertSoftly(assertions -> {
        assertions.assertThat(true).isFalse();
        assertions.assertThat(42).isEqualTo("meaning of life");
        assertions.assertThat("red").isEqualTo("blue");
      });
    }).as("it should call assertAll() and fail with multiple validation errors")
      .hasMessageContaining("meaning of life")
      .hasMessageContaining("blue");
  }

  @Test
  public void should_work_with_atomic() {
    // WHEN
    // simple atomic value
    softly.then(new AtomicBoolean(true)).isTrue().isFalse();
    softly.then(new AtomicInteger(1)).hasValueGreaterThan(0).hasNegativeValue();
    softly.then(new AtomicLong(1L)).hasValueGreaterThan(0L).hasNegativeValue();;
    softly.then(new AtomicReference<>("abc")).hasValue("abc").hasValue("def");
    // atomic array value
    softly.then(new AtomicIntegerArray(new int[] { 1, 2, 3 })).containsExactly(1, 2, 3).isEmpty();
    softly.then(new AtomicLongArray(new long[] { 1L, 2L, 3L })).containsExactly(1L, 2L, 3L).contains(0);
    softly.then(new AtomicReferenceArray<>(array("a", "b", "c"))).containsExactly("a", "b", "c").contains("123");
    // THEN
    List<Throwable> errorsCollected = softly.errorsCollected();
    assertThat(errorsCollected).hasSize(7);
    assertThat(errorsCollected.get(0)).hasMessageContaining("false");
    assertThat(errorsCollected.get(1)).hasMessageContaining("0");
    assertThat(errorsCollected.get(2)).hasMessageContaining("0L");
    assertThat(errorsCollected.get(3)).hasMessageContaining("def");
    assertThat(errorsCollected.get(4)).hasMessageContaining("empty");
    assertThat(errorsCollected.get(5)).hasMessageContaining("0");
    assertThat(errorsCollected.get(6)).hasMessageContaining("123");
  }

  @Test
  public void should_fix_bug_1146() {
    // GIVEN
    Map<String, String> numbers = mapOf(entry("one", "1"),
                                        entry("two", "2"),
                                        entry("three", "3"));
    // THEN
    try (final AutoCloseableBDDSoftAssertions softly = new AutoCloseableBDDSoftAssertions()) {
      softly.then(numbers)
            .extracting("one", "two")
            .containsExactly("1", "2");
      softly.then(numbers)
            .extracting("one")
            .containsExactly("1");
    }
  }

  // the test would if any method was not proxyable as the assertion error would not be softly caught
  @SuppressWarnings("unchecked")
  @Test
  public void all_iterable_assertion_final_methods_should_work_with_soft_assertions() {
    // GIVEN
    Iterable<Name> names = asList(name("John", "Doe"), name("Jane", "Doe"));
    Iterable<CartoonCharacter> characters = asList(homer, fred);
    // WHEN
    softly.then(names)
          .extracting(Name::getFirst)
          .contains("gandalf")
          .contains("frodo");
    softly.then(names)
          .extracting("last")
          .containsExactly("foo", "bar");
    softly.then(characters)
          .flatExtracting(CartoonCharacter::getChildren)
          .as("using flatExtracting on Iterable")
          .hasSize(1)
          .containsAnyOf(homer, fred);
    softly.then(characters)
          .flatExtracting(CartoonCharacter::getChildrenWithException)
          .as("using flatExtracting on Iterable with exception")
          .containsExactlyInAnyOrder(homer, fred);
    softly.then(characters)
          .containsOnly(bart);
    softly.then(characters)
          .containsOnlyOnce(maggie, bart);
    softly.then(characters)
          .containsSequence(homer, bart);
    softly.then(characters)
          .containsSubsequence(homer, maggie);
    softly.then(characters)
          .doesNotContain(homer, maggie);
    softly.then(characters)
          .doesNotContainSequence(fred);
    softly.then(characters)
          .doesNotContainSubsequence(homer, fred);
    softly.then(characters)
          .isSubsetOf(homer, bart);
    softly.then(characters)
          .startsWith(fred);
    softly.then(characters)
          .endsWith(bart);
    softly.then(names)
          .extracting(Name::getFirst, Name::getLast)
          .contains(tuple("John", "Doe"))
          .contains(tuple("Frodo", "Baggins"));
    // THEN
    List<Throwable> errorsCollected = softly.errorsCollected();
    assertThat(errorsCollected).hasSize(17);
    assertThat(errorsCollected.get(0)).hasMessageContaining("gandalf");
    assertThat(errorsCollected.get(1)).hasMessageContaining("frodo");
    assertThat(errorsCollected.get(2)).hasMessageContaining("foo")
                                      .hasMessageContaining("bar");
    assertThat(errorsCollected.get(3)).hasMessageContaining("size");
    assertThat(errorsCollected.get(4)).hasMessageContaining(fred.toString());
    assertThat(errorsCollected.get(5)).hasMessageContaining(homer.toString());
    assertThat(errorsCollected.get(6)).hasMessageContaining(bart.toString());
    assertThat(errorsCollected.get(7)).hasMessageContaining(maggie.toString());
    assertThat(errorsCollected.get(8)).hasMessageContaining(bart.toString());
    assertThat(errorsCollected.get(9)).hasMessageContaining(maggie.toString());
    assertThat(errorsCollected.get(10)).hasMessageContaining(homer.toString());
    assertThat(errorsCollected.get(11)).hasMessageContaining(fred.toString());
    assertThat(errorsCollected.get(12)).hasMessageContaining(homer.toString());
    assertThat(errorsCollected.get(13)).hasMessageContaining(bart.toString());
    assertThat(errorsCollected.get(14)).hasMessageContaining(fred.toString());
    assertThat(errorsCollected.get(15)).hasMessageContaining(bart.toString());
    assertThat(errorsCollected.get(16)).hasMessageContaining("Baggins");
  }

  // the test would if any method was not proxyable as the assertion error would not be softly caught
  @SuppressWarnings("unchecked")
  @Test
  public void all_list_assertion_final_methods_should_work_with_soft_assertions() {
    // GIVEN
    List<Name> names = asList(name("John", "Doe"), name("Jane", "Doe"));
    List<CartoonCharacter> characters = asList(homer, fred);
    // WHEN
    softly.then(names)
          .extracting(Name::getFirst)
          .contains("gandalf")
          .contains("frodo");
    softly.then(names)
          .extracting("last")
          .containsExactly("foo", "bar");
    softly.then(characters)
          .flatExtracting(CartoonCharacter::getChildren)
          .as("using flatExtracting on Iterable")
          .hasSize(1)
          .containsAnyOf(homer, fred);
    softly.then(characters)
          .flatExtracting(CartoonCharacter::getChildrenWithException)
          .as("using flatExtracting on Iterable with exception")
          .containsExactlyInAnyOrder(homer, fred);
    softly.then(characters)
          .containsOnly(bart);
    softly.then(characters)
          .containsOnlyOnce(maggie, bart);
    softly.then(characters)
          .containsSequence(homer, bart);
    softly.then(characters)
          .containsSubsequence(homer, maggie);
    softly.then(characters)
          .doesNotContain(homer, maggie);
    softly.then(characters)
          .doesNotContainSequence(fred);
    softly.then(characters)
          .doesNotContainSubsequence(homer, fred);
    softly.then(characters)
          .isSubsetOf(homer, bart);
    softly.then(characters)
          .startsWith(fred);
    softly.then(characters)
          .endsWith(bart);
    softly.then(names)
          .extracting(Name::getFirst, Name::getLast)
          .contains(tuple("John", "Doe"))
          .contains(tuple("Frodo", "Baggins"));
    // THEN
    List<Throwable> errorsCollected = softly.errorsCollected();
    assertThat(errorsCollected).hasSize(17);
    assertThat(errorsCollected.get(0)).hasMessageContaining("gandalf");
    assertThat(errorsCollected.get(1)).hasMessageContaining("frodo");
    assertThat(errorsCollected.get(2)).hasMessageContaining("foo")
                                      .hasMessageContaining("bar");
    assertThat(errorsCollected.get(3)).hasMessageContaining("size");
    assertThat(errorsCollected.get(4)).hasMessageContaining(fred.toString());
    assertThat(errorsCollected.get(5)).hasMessageContaining(homer.toString());
    assertThat(errorsCollected.get(6)).hasMessageContaining(bart.toString());
    assertThat(errorsCollected.get(7)).hasMessageContaining(maggie.toString());
    assertThat(errorsCollected.get(8)).hasMessageContaining(bart.toString());
    assertThat(errorsCollected.get(9)).hasMessageContaining(maggie.toString());
    assertThat(errorsCollected.get(10)).hasMessageContaining(homer.toString());
    assertThat(errorsCollected.get(11)).hasMessageContaining(fred.toString());
    assertThat(errorsCollected.get(12)).hasMessageContaining(homer.toString());
    assertThat(errorsCollected.get(13)).hasMessageContaining(bart.toString());
    assertThat(errorsCollected.get(14)).hasMessageContaining(fred.toString());
    assertThat(errorsCollected.get(15)).hasMessageContaining(bart.toString());
    assertThat(errorsCollected.get(16)).hasMessageContaining("Baggins");
  }

  // the test would if any method was not proxyable as the assertion error would not be softly caught
  @SuppressWarnings("unchecked")
  @Test
  public void all_object_array_assertion_final_methods_should_work_with_soft_assertions() {
    // GIVEN
    Name[] names = array(name("John", "Doe"), name("Jane", "Doe"));
    CartoonCharacter[] characters = array(homer, fred);
    // WHEN
    softly.then(names)
          .extracting(Name::getFirst)
          .contains("gandalf")
          .contains("frodo");
    softly.then(names)
          .extracting("last")
          .containsExactly("foo", "bar");
    softly.then(characters)
          .flatExtracting(CartoonCharacter::getChildren)
          .as("using flatExtracting on Iterable")
          .hasSize(1)
          .containsAnyOf(homer, fred);
    softly.then(characters)
          .flatExtracting(CartoonCharacter::getChildrenWithException)
          .as("using flatExtracting on Iterable with exception")
          .containsExactlyInAnyOrder(homer, fred);
    softly.then(characters)
          .containsOnly(bart);
    softly.then(characters)
          .containsOnlyOnce(maggie, bart);
    softly.then(characters)
          .containsSequence(homer, bart);
    softly.then(characters)
          .containsSubsequence(homer, maggie);
    softly.then(characters)
          .doesNotContain(homer, maggie);
    softly.then(characters)
          .doesNotContainSequence(fred);
    softly.then(characters)
          .doesNotContainSubsequence(homer, fred);
    softly.then(characters)
          .isSubsetOf(homer, bart);
    softly.then(characters)
          .startsWith(fred);
    softly.then(characters)
          .endsWith(bart);
    softly.then(names)
          .extracting(Name::getFirst, Name::getLast)
          .contains(tuple("John", "Doe"))
          .contains(tuple("Frodo", "Baggins"));
    // THEN
    List<Throwable> errorsCollected = softly.errorsCollected();
    assertThat(errorsCollected).hasSize(17);
    assertThat(errorsCollected.get(0)).hasMessageContaining("gandalf");
    assertThat(errorsCollected.get(1)).hasMessageContaining("frodo");
    assertThat(errorsCollected.get(2)).hasMessageContaining("foo")
                                      .hasMessageContaining("bar");
    assertThat(errorsCollected.get(3)).hasMessageContaining("size");
    assertThat(errorsCollected.get(4)).hasMessageContaining(fred.toString());
    assertThat(errorsCollected.get(5)).hasMessageContaining(homer.toString());
    assertThat(errorsCollected.get(6)).hasMessageContaining(bart.toString());
    assertThat(errorsCollected.get(7)).hasMessageContaining(maggie.toString());
    assertThat(errorsCollected.get(8)).hasMessageContaining(bart.toString());
    assertThat(errorsCollected.get(9)).hasMessageContaining(maggie.toString());
    assertThat(errorsCollected.get(10)).hasMessageContaining(homer.toString());
    assertThat(errorsCollected.get(11)).hasMessageContaining(fred.toString());
    assertThat(errorsCollected.get(12)).hasMessageContaining(homer.toString());
    assertThat(errorsCollected.get(13)).hasMessageContaining(bart.toString());
    assertThat(errorsCollected.get(14)).hasMessageContaining(fred.toString());
    assertThat(errorsCollected.get(15)).hasMessageContaining(bart.toString());
    assertThat(errorsCollected.get(16)).hasMessageContaining("Baggins");
  }

  // the test would if any method was not proxyable as the assertion error would not be softly caught
  @SuppressWarnings("unchecked")
  @Test
  public void all_class_assertion_final_methods_should_work_with_soft_assertions() {
    // GIVEN
    Class<AnnotatedClass> actual = AnnotatedClass.class;
    // WHEN
    softly.then(actual)
          .hasAnnotations(MyAnnotation.class, AnotherAnnotation.class)
          .hasAnnotations(SafeVarargs.class, VisibleForTesting.class);
    // THEN
    List<Throwable> errorsCollected = softly.errorsCollected();
    assertThat(errorsCollected).hasSize(1);
    assertThat(errorsCollected.get(0)).hasMessageContaining("SafeVarargs")
                                      .hasMessageContaining("VisibleForTesting");
  }

  // the test would if any method was not proxyable as the assertion error would not be softly caught
  @SuppressWarnings("unchecked")
  @Test
  public void all_object_assertion_final_methods_should_work_with_soft_assertions() {
    // GIVEN
    Name name = name("John", "Doe");
    // WHEN
    softly.then(name)
          .extracting(Name::getFirst, Name::getLast)
          .contains("John")
          .contains("frodo");
    // THEN
    List<Throwable> errorsCollected = softly.errorsCollected();
    assertThat(errorsCollected).hasSize(1);
    assertThat(errorsCollected.get(0)).hasMessageContaining("frodo");
  }

  // the test would if any method was not proxyable as the assertion error would not be softly caught
  @SuppressWarnings("unchecked")
  @Test
  public void all_map_assertion_final_methods_should_work_with_soft_assertions() {
    // GIVEN
    Map<String, String> map = mapOf(entry("a", "1"), entry("b", "2"), entry("c", "3"));
    // WHEN
    softly.then(map).contains(entry("abc", "ABC"), entry("def", "DEF")).isEmpty();
    softly.then(map).containsAnyOf(entry("gh", "GH"), entry("ij", "IJ"));
    softly.then(map).containsExactly(entry("kl", "KL"), entry("mn", "MN"));
    softly.then(map).containsKeys("K1", "K2");
    softly.then(map).containsOnly(entry("op", "OP"), entry("qr", "QR"));
    softly.then(map).containsOnlyKeys("K3", "K4");
    softly.then(map).containsValues("V1", "V2");
    softly.then(map).doesNotContain(entry("a", "1"), entry("abc", "ABC"));
    softly.then(map).doesNotContainKeys("a", "b");
    // THEN
    List<Throwable> errors = softly.errorsCollected();
    assertThat(errors).hasSize(10);
    assertThat(errors.get(0)).hasMessageContaining("MapEntry[key=\"abc\", value=\"ABC\"]");
    assertThat(errors.get(1)).hasMessageContaining("empty");
    assertThat(errors.get(2)).hasMessageContaining("gh")
                             .hasMessageContaining("IJ");
    assertThat(errors.get(3)).hasMessageContaining("\"a\"=\"1\"");
    assertThat(errors.get(4)).hasMessageContaining("K2");
    assertThat(errors.get(5)).hasMessageContaining("OP");
    assertThat(errors.get(6)).hasMessageContaining("K4");
    assertThat(errors.get(7)).hasMessageContaining("V2");
    assertThat(errors.get(8)).hasMessageContaining("ABC");
    assertThat(errors.get(9)).hasMessageContaining("b");
  }

  @SuppressWarnings("unchecked")
  @Test
  public void all_predicate_assertion_final_methods_should_work_with_soft_assertions() {
    // GIVEN
    Predicate<MapEntry<String, String>> ballSportPredicate = sport -> sport.value.contains("ball");
    // WHEN
    softly.then(ballSportPredicate)
          .accepts(entry("sport", "boxing"), entry("sport", "marathon"))
          .rejects(entry("sport", "football"), entry("sport", "basketball"));
    // THEN
    List<Throwable> errorsCollected = softly.errorsCollected();
    assertThat(errorsCollected).hasSize(2);
    assertThat(errorsCollected.get(0)).hasMessageContaining("boxing");
    assertThat(errorsCollected.get(1)).hasMessageContaining("basketball");
  }

}
