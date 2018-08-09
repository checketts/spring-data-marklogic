package io.github.malteseduck.springframework.data.marklogic.core;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.document.ServerTransform;
import com.marklogic.client.query.StructuredQueryBuilder;
import io.github.malteseduck.springframework.data.marklogic.core.mapping.Document;
import io.github.malteseduck.springframework.data.marklogic.core.mapping.TypePersistenceStrategy;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import io.github.malteseduck.springframework.data.marklogic.DatabaseConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.annotation.Id;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Instant;
import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextHierarchy({
        @ContextConfiguration("classpath:integration.xml"),
        @ContextConfiguration(classes = DatabaseConfiguration.class)
})
public class TemplateCrudIT {

    private MarkLogicOperations ops;
    private StructuredQueryBuilder qb = new StructuredQueryBuilder();

    @Autowired
    public void setClient(DatabaseClient client) {
        ops = new MarkLogicTemplate(client);
    }

    @Before
    public void init() {
        cleanDb();
    }

    @After
    public void clean() {
        cleanDb();
    }

    private void cleanDb() {
        ops.delete(qb.directory(true, "/test/categories/"), Category.class);
        ops.dropCollection(Person.class);
        ops.dropCollection(InstantPerson.class);
        ops.dropCollection(IntPerson.class);
        ops.deleteByIds(asList("badfred"), BadPerson.class);
    }

    @Test
    public void testDeleteById() {
        Person bob = new Person("Bob");
        Person george = new Person("George");

        ops.write(asList(bob, george));

        ops.deleteByUri("/" + bob.getId() + ".json");
        assertThat(ops.exists(bob.getId())).as("deleted by uri").isFalse();

        ops.deleteById(george.getId(), Person.class);
        assertThat(ops.exists(george.getId())).as("deleted by id and type").isFalse();
    }

    @Test
    public void testDeleteByIds() throws Exception {
        Person bob = new Person("Bob");
        Person george = new Person("George");

        ops.write(asList(bob, george));

        ops.deleteByIds(asList(george.getId()), Person.class);
        assertThat(ops.exists(george.getId())).as("options type").isFalse();
    }

    @Test
    public void testDeleteEntities() throws Exception {
        Person bob = new Person("Bob");
        Person george = new Person("George");

        ops.write(asList(bob, george));
        ops.delete(asList(bob, george));
        assertThat(ops.exists(bob.getId(), Person.class)).isFalse();
    }

    @Test
    public void testSimpleWrite() {
        Person person = new Person("Bob");

        ops.write(person);

        Person saved = ops.read(person.getId(), Person.class);
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo(person.getId());
    }

    @Test
    @Ignore("not yet implemented because not supported in WriteSet")
    public void testSimpleWriteAutoId() {
        Person person = new Person("Bob");
        person.setId(null);

        ops.write(person);

        Person bob = ops.searchOne(null, Person.class);
        assertThat(bob.getId())
                .isNotNull()
                .isNotEqualTo("null");
    }

    @Test
    public void testBatchWrite() {
        Person bob = new Person("bob");
        Person fred = new Person("fred");

        ops.write(asList(bob, fred));

        Person saved = ops.read(fred.getId(), Person.class);
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo(fred.getId());
    }

    @Test
    public void testWriteWithTransform() {
        Person bob = new Person("bob");
        ops.write(bob, new ServerTransform("write-transform"));

        Person found = ops.read(bob.getId(), Person.class);

        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("Override Master Write");
    }

    @Test
    public void testBatchRead() {
        Person bob = new Person("bob");
        Person fred = new Person("fred");

        ops.write(asList(bob, fred));

        List<Person> saved = ops.read(asList(fred.getId(), bob.getId()), Person.class);
        assertThat(saved).extracting(Person::getName)
            .containsExactlyInAnyOrder("bob", "fred");
    }

    @Test
    public void testBatchReadByEntity() {
        Person bob = new Person("bob");
        Person fred = new Person("fred");

        ops.write(asList(bob, fred));

        List<Person> people = ops.search(Person.class);
        assertThat(people).extracting(Person::getName)
                .containsExactlyInAnyOrder("bob", "fred");
    }

    @Test
    public void testNoIdAnnotationFailure() throws Exception {
        Object person = new Object() {};

        Throwable thrown = catchThrowable(() -> ops.write(person));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not have a method or field annotated with org.springframework.data.annotation.Id");
    }

    @Document(typeStrategy = TypePersistenceStrategy.NONE)
    private static class BadPerson extends Person {
        public BadPerson(String name) { super(name); }
    }

    @Test
    public void testNoCollectionDeleteFailure() throws Exception {
        Throwable thrown = catchThrowable(() -> ops.dropCollection(BadPerson.class));

        assertThat(thrown).isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasMessage("Cannot determine deleteById scope for entity of type io.github.malteseduck.springframework.data.marklogic.core.TemplateCrudIT$BadPerson");
    }

    @Test
    public void testSearchConstrainedToCollection() {
        Person bob = new Person("bob");
        BadPerson fred = new BadPerson("fred");
        fred.setId("badfred");

        ops.write(asList(bob, fred));
        List<Person> people = ops.search(Person.class);
        assertThat(people).containsExactly(bob);
    }

    @Test
    public void testNoClassDeleteFailure() throws Exception {
        Throwable thrown = catchThrowable(() -> ops.dropCollection((Class<?>) null));

        assertThat(thrown).isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasMessage("Entity class is required to determine scope of deleteById");
    }

    public static class IntPerson {
        @Id
        private int id = 23;
        public int getId() { return id; }
    }

    @Test
    public void testIntIdWrite() throws Exception {
        IntPerson person = new IntPerson();

        ops.write(person);

        IntPerson saved = ops.read(23, IntPerson.class);
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo(23);
    }

    public static class InstantPerson {
        @Id
        private Instant id = Instant.parse("2007-07-07T07:07:07Z");
        public Instant getId() { return id; }
    }

    @Test
    public void testInstantIdWrite() throws Exception {
        InstantPerson person = new InstantPerson();

        ops.write(person);

        InstantPerson saved = ops.read(Instant.parse("2007-07-07T07:07:07Z"), InstantPerson.class);
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo(Instant.parse("2007-07-07T07:07:07Z"));
    }

    public static class ColPerson {
        @Id
        private List<String> id = asList("23");
        public List<String> getId() { return id; }
    }

    @Test
    public void testCollectionIdWriteFailure() throws Exception {
        ColPerson person = new ColPerson();

        Throwable thrown = catchThrowable(() -> ops.write(person));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Collection types not supported as entity id");
    }

    @Test
    public void testWriteWithCustomBaseUri() {
        Category category = new Category().setTitle("Test Category");

        ops.write(category);

        assertThat(ops.exists("/test/categories/" + category.getId() + ".json")).isTrue();
    }
}
