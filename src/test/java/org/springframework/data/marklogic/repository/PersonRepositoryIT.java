package org.springframework.data.marklogic.repository;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.marklogic.DatabaseConfiguration;
import org.springframework.data.marklogic.core.MarkLogicOperations;
import org.springframework.data.marklogic.core.Person;
import org.springframework.data.marklogic.core.Pet;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Instant;
import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextHierarchy({
        @ContextConfiguration("classpath:integration.xml"),
        @ContextConfiguration(classes = DatabaseConfiguration.class),
        @ContextConfiguration(classes = PersonRepositoryIntegrationConfiguration.class)
})
public class PersonRepositoryIT {

    @Autowired
    private PersonRepository repository;

    @Autowired
    MarkLogicOperations operations;

    private Person bobby, george, jane, jenny, andrea, henry, freddy;

    List<Person> all;

    @Before
    public void init() {
        cleanDb();

        andrea = new Person("Andrea", 17, "female", "food prep", "There isn't much to say", Instant.parse("2016-04-01T00:00:00Z"), asList("sewing", "karate"), asList(new Pet("Fluffy", "cat")));
        bobby = new Person("Bobby", 23, "male", "dentist", "", Instant.parse("2016-01-01T00:00:00Z"), asList("running", "origami"), asList(new Pet("Bowwow", "dog")));
        george = new Person("George", 12, "male", "engineer", "The guy wo works at the gas station, he is your friend", Instant.parse("2016-02-01T00:00:00Z"), asList("fishing", "hunting", "sewing"), asList(new Pet("Hazelnut", "snake"), new Pet("Snoopy", "dog")));
        henry = new Person("Henry", 32, "male", "construction", "He built my house", Instant.parse("2016-05-01T00:00:00Z"), asList("carpentry", "gardening"));
        jane = new Person("Jane", 52, "female", "doctor", "A nice lady that is a friend of george", Instant.parse("2016-03-01T00:00:00Z"), asList("fencing", "archery", "running"));
        jenny = new Person("Jenny", 41, "female", "dentist", "", Instant.parse("2016-06-01T00:00:00Z"), asList("gymnastics"), asList(new Pet("Powderkeg", "wolverine")));

        henry.setRankings(asList(1, 2, 3));

        all = repository.save(asList(jenny, bobby, george, jane, andrea, henry));

        freddy = new Person("Freddy", 27, "male", "policeman", "", Instant.parse("2016-08-01T00:00:00Z"), asList("gaming"));
        operations.write(freddy, "OtherPeople");
    }

    @After
    public void clean() {
        cleanDb();
    }

    private void cleanDb() {
        repository.deleteAll();
        operations.deleteAll("OtherPeople");
    }

    @Test
    public void testFindsPersonById() throws Exception {
        Person found = repository.findOne(bobby.getId());
        assertThat(found).isEqualTo(bobby);
    }

    @Test
    public void testFindsAllPeople() throws Exception {
        List<Person> people = repository.findAll();
        assertThat(people).containsAll(all);
    }

    @Test
    public void testFindsAllPeopleOrderedByName() throws Exception {
        List<Person> people = repository.findAll(new Sort("name"));
        assertThat(people).containsExactly(andrea, bobby, george, henry, jane, jenny);
    }

    @Test
    public void testFindsAllWithGivenIds() {
        Iterable<Person> people = repository.findAll(asList(george.getId(), bobby.getId()));
        assertThat(people).containsExactlyInAnyOrder(george, bobby);
    }

    @Test
    public void testDeletesPersonCorrectly() throws Exception {
        repository.delete(george);

        List<Person> people = repository.findAll();
        assertThat(people).hasSize(all.size() - 1);
        assertThat(people).doesNotContain(george);
    }

    @Test
    public void testDeletesPersonByIdCorrectly() {
        repository.delete(bobby.getId());

        List<Person> people = repository.findAll();
        assertThat(people).hasSize(all.size() - 1);
        assertThat(people).doesNotContain(bobby);
    }

    @Test
    public void testFindsPersonsOrderedByName() throws Exception {
        List<Person> people = repository.findAllByOrderByNameAsc();
        assertThat(people).containsExactly(andrea, bobby, george, henry, jane, jenny);
    }

    @Test
    public void testFindsPersonsByName() throws Exception {
        List<Person> people = repository.findByName("Jane");
        assertThat(people).containsExactly(jane);
    }

    @Test
    public void testFindsPersonsByNameOrderedByAge() throws Exception {
        List<Person> people = repository.findByGenderOrderByAge("female");
        assertThat(people).containsExactly(andrea, jenny, jane);
    }

    @Test
    public void testFindPersonsByOccupationOrderedByName() throws Exception {
        List<Person> people = repository.findByOccupationOrderByNameAsc("dentist");
        assertThat(people).containsExactly(bobby, jenny);
    }

    @Test
    public void testFindsPersonsByNameIn() throws Exception {
        List<Person> people = repository.findByNameIn("Jane", "George");
        assertThat(people).containsExactlyInAnyOrder(jane, george);
    }

    @Test
    public void findsPersonsByNameNull() throws Exception {
        List<Person> people = repository.findByName(null);
        assertThat(people).isNullOrEmpty();
    }

    @Test
    public void testFindsPersonsByNameInNull() throws Exception {
        List<Person> people = repository.findByNameIn(null);
        assertThat(people).isNullOrEmpty();
    }

    @Test
    public void testFindsPersonsByAge() throws Exception {
        List<Person> people = repository.findByAge(23);
        assertThat(people).containsExactly(bobby);
    }

    @Test
    public void testFindsPersonByBirthtime() throws Exception {
        Person person = repository.findByBirthtime(Instant.parse("2016-01-01T00:00:00Z"));
        assertThat(person).isEqualTo(bobby);
    }

    @Test
    public void testFindsPersonsByGenderLike() throws Exception {
        List<Person> people = repository.findByGenderLike("ma*");
        assertThat(people).containsExactlyInAnyOrder(bobby, george, henry);
    }

    @Test
    public void testFindsPersonsByNameNotLike() throws Exception {
        List<Person> people = repository.findByNameNotLike("Bo*");
        assertThat(people).doesNotContain(bobby);
    }

    @Test
    public void testFindsPagedPersonsOrderedByName() throws Exception {
        Page<Person> page = repository.findAll(new PageRequest(1, 2, Sort.Direction.ASC, "name"));
        assertThat(page.isFirst()).isFalse();
        assertThat(page.isLast()).isFalse();
        assertThat(page).containsExactly(george, henry);
    }

    @Test
    public void testExecutesPagedFinderCorrectly() throws Exception {
        Page<Person> page = repository.findByGenderLike("fem*",
                new PageRequest(0, 2, Sort.Direction.ASC, "name"));
        
        assertThat(page.isFirst()).isTrue();
        assertThat(page.isLast()).isFalse();
        assertThat(page.getNumberOfElements()).isEqualTo(2);
        assertThat(page).containsExactly(andrea, jane);

        // Wildcard index required for result total to be correct
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    public void testExistsWorksCorrectly() {
        assertThat(repository.exists(bobby.getId())).isTrue();
    }

    @Test
    public void testFindsPeopleUsingNotPredicate() {
        List<Person> people = repository.findByNameNot("Andrea");
        
        assertThat(people)
                .doesNotContain(andrea)
                .hasSize(all.size() - 1);
    }

    @Test
    public void tesetExecutesAndQueryCorrectly() {
        List<Person> people = repository.findByNameAndAge("Bobby", 23);

        assertThat(people).containsExactly(bobby);
    }

    @Test
    public void testExecutesOrQueryCorrectly() {
        List<Person> people = repository.findByNameOrAge("Bobby", 23);

        assertThat(people).containsExactly(bobby);
    }

    @Test
    public void testExecutesDerivedCountProjection() {
        assertThat(repository.countByName("George")).isEqualTo(1);
    }

    @Test
    public void testExecutesDerivedExistsProjectionToBoolean() {
        assertThat(repository.existsByName("Jane")).as("does exist").isTrue();
        assertThat(repository.existsByName("Brunhilda")).as("doesn't exist").isFalse();
    }

    @Test
    public void testExecutesDerivedStartsWithQueryCorrectly() {
        List<Person> people = repository.findByNameStartsWith("J");
        
        assertThat(people).containsExactlyInAnyOrder(jenny, jane);
    }

    @Test
    public void testFxecutesDerivedEndsWithQueryCorrectly() {
        List<Person> people = repository.findByNameEndsWith("nny");
        assertThat(people).containsExactly(jenny);
    }
    
    @Test
    public void testFindByNameIgnoreCase() {
        List<Person> people = repository.findByNameIgnoreCase("george");
        assertThat(people).containsExactly(george);
    }

    @Test
    public void testFindByNameNotIgnoreCase() {
        List<Person> people = repository.findByNameNotIgnoreCase("george");
        assertThat(people)
                .hasSize(all.size()-1)
                .doesNotContain(george);
    }

    @Test
    public void testFindByNameStartingWithIgnoreCase() {
        List<Person> people = repository.findByNameStartingWithIgnoreCase("ge");
        assertThat(people).containsExactly(george);
    }

    @Test
    public void testFindByHobbiesContains() throws Exception {
        List<Person> people = repository.findByHobbiesContains(asList("running"));
        assertThat(people).containsExactlyInAnyOrder(bobby, jane);
    }

    @Test
    public void testFindByHobbiesNotContains() throws Exception {
        List<Person> people = repository.findByHobbiesNotContaining(asList("running"));
        assertThat(people).doesNotContain(bobby, jane);
    }

    @Test
    public void testFindByPet() throws Exception {
        List<Person> people = repository.findByPets(new Pet("Powderkeg", "wolverine"));
        assertThat(people).containsExactly(jenny);
    }

    @Test
    public void testFindByNameQBE() throws Exception {
        Person person = repository.qbeFindByName("Bobby");
        assertThat(person).isEqualTo(bobby);
    }

    @Test
    public void testFindBobby() throws Exception {
        Person person = repository.qbeFindBobby();
        assertThat(person).isEqualTo(bobby);
    }

    @Test
    public void testFindByNameQBEQuoted() throws Exception {
        Person person = repository.qbeFindByNameQuoted("Bobby");
        assertThat(person).isEqualTo(bobby);
    }

    @Test
    public void testFindByPetQBE() throws Exception {
        List<Person> people = repository.qbeFindByPet(new Pet("Fluffy", "cat"));
        assertThat(people).containsExactly(andrea);
    }

    @Test
    public void testFindByGenderWithPageableQBE() throws Exception {
        Page<Person> people = repository.qbeFindByGenderWithPageable(
                "female",
                new PageRequest(0, 2, Sort.Direction.ASC, "name")
        );
        assertThat(people).containsExactly(andrea, jane);
    }

    @Test
    public void testFindByGenderQBEHonorsCollections() throws Exception {
        Page<Person> people = repository.qbeFindByGenderWithPageable(
                "male",
                new PageRequest(0, 20, Sort.Direction.ASC, "name")
        );
        assertThat(people).containsExactly(bobby, george, henry);
    }
}
