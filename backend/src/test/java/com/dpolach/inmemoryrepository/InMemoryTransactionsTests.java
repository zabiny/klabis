package com.dpolach.inmemoryrepository;

import club.klabis.KlabisApplication;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "klabis.preset-data=false")
@ContextConfiguration(classes = KlabisApplication.class)
@EnableInMemoryRepositories(basePackageClasses = TestRepository.class)
class InMemoryTransactionsTests {

    @Autowired
    TestRepository testedSubject;

    @Autowired
    TransactionTemplate txTemplate;

    @Autowired
    InMemoryEntityStores entityStore;

    @AfterEach
    void cleanup() {
        entityStore.deleteAllData();
    }

    @DisplayName("Transaction tests")
    @Nested
    class TransactionTests {
        @Test
        void itShouldKeepCommitedData() {

            testedSubject.save(ExampleData.create(1, "forDelete (A)"));
            testedSubject.save(ExampleData.create(2, "before (A)"));

            txTemplate.executeWithoutResult(status -> {
                testedSubject.deleteById(1);
                testedSubject.save(ExampleData.create(2, "updated (A)"));
                testedSubject.save(ExampleData.create(3, "added (A)"));
            });

            assertThat(testedSubject.findAll())
                    .haveExactly(0, data(1))
                    .haveExactly(1, data(2, "updated (A)"))
                    .haveExactly(1, data(3, "added (A)"))
                    .hasSize(2);

        }

        @Test
        void itShouldRollbackData() {
            testedSubject.save(ExampleData.create(1, "forDelete (B)"));
            testedSubject.save(ExampleData.create(2, "before (B)"));

            txTemplate.executeWithoutResult(status -> {
                testedSubject.deleteById(1);
                ExampleData edited = testedSubject.findById(2).get();
                edited.setName("updated (B)");
                testedSubject.save(edited);
                testedSubject.save(ExampleData.create(3, "added (B)"));
                // rollback transaction
                status.setRollbackOnly();
            });

            assertThat(testedSubject.findAll())
                    .haveExactly(1, data(1, "forDelete (B)"))
                    .haveExactly(1, data(2, "before (B)"))
                    .hasSize(2);
        }
    }

    @DisplayName("Sorting tests")
    @Nested
    class SortingTests {
        @DisplayName("it should sort data by given attribute")
        @Test
        void itShouldSortData() {
            ExampleData item_1_Z = ExampleData.create(1, "Z");
            ExampleData item_2_Y = ExampleData.create(2, "Y");
            testedSubject.saveAll(List.of(item_1_Z, item_2_Y));

            Assertions.assertThat(testedSubject.findAll(Sort.by(Sort.Order.asc("name"))))
                    .describedAs("it should sort by name asc")
                    .containsExactly(item_2_Y, item_1_Z);
            Assertions.assertThat(testedSubject.findAll(Sort.by(Sort.Order.desc("name"))))
                    .describedAs("it should sort by name desc")
                    .containsExactly(item_1_Z, item_2_Y);

            Assertions.assertThat(testedSubject.findAll(Sort.by(Sort.Order.asc("id"))))
                    .describedAs("it should sort by id asc")
                    .containsExactly(item_1_Z, item_2_Y);
            Assertions.assertThat(testedSubject.findAll(Sort.by(Sort.Order.desc("id"))))
                    .describedAs("it should sort by id desc")
                    .containsExactly(item_2_Y, item_1_Z);
        }
    }

    @DisplayName("Name filtering method tests")
    @Nested
    class NameFilteringTests {
        @DisplayName("It should return expected results")
        @Test
        void itShouldReturnExpectedResults() {
            ExampleData item_1_Z = ExampleData.create(1, "text Z");
            ExampleData item_2_Y = ExampleData.create(2, "Something Y text contains");
            testedSubject.saveAll(List.of(item_1_Z, item_2_Y));

            Assertions.assertThat(testedSubject.findAllByNameContaining("Z"))
                    .describedAs("It should return expected results for filter 'Z'")
                    .containsExactly(item_1_Z);

            Assertions.assertThat(testedSubject.findAllByNameContaining("Y"))
                    .describedAs("It should return expected results for filter 'Y'")
                    .containsExactly(item_2_Y);

            Assertions.assertThat(testedSubject.findAllByNameContaining("text"))
                    .describedAs("It should return expected results for filter 'text'")
                    .containsExactlyInAnyOrder(item_2_Y, item_1_Z);

        }
    }

    static Condition<ExampleData> data(int id, String name) {
        return new Condition<>(id(id).and(name(name)), "data with id: " + id + " and name: " + name);
    }

    static Condition<ExampleData> data(int id) {
        return new Condition<>(id(id), "data with id: " + id);
    }

    static Predicate<ExampleData> id(Integer id) {
        return it -> it.getId() == id;
    }

    static Predicate<ExampleData> name(String name) {
        return it -> it.getName().equals(name);
    }

}

class ExampleData {
    @Id
    private int id;
    private String name;

    public static ExampleData create(int id, String name) {
        ExampleData data = new ExampleData();
        data.setId(id);
        data.setName(name);
        return data;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ExampleData that = (ExampleData) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "ExampleData{" +
               "id=" + id +
               ", name='" + name + '\'' +
               '}';
    }
}

@Repository
interface TestRepository extends InMemoryRepository<ExampleData, Integer> {
    List<ExampleData> findAllByNameContaining(String text);
}